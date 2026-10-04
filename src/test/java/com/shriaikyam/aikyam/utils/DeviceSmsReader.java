package com.shriaikyam.aikyam.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the AIKYAM login OTP straight off the connected device's SMS inbox via adb, so the
 * Authentication tests can complete a real OTP login without a human relaying the code.
 *
 * The query itself is intentionally unfiltered (no --where/--sort): a value containing a
 * quote character reliably gets stripped somewhere between here and the device - confirmed
 * across adb's own argv-rejoining, Java ProcessBuilder's Windows command-line escaping (a
 * known JDK/Windows limitation with embedded quotes), and a cmd.exe batch-file wrapper, none
 * of which reproduced the escaping that a manually-typed shell command used successfully.
 * Every candidate row is scanned in memory for the AIKYAM OTP pattern and discarded
 * immediately after - unrelated message bodies (bank alerts, other OTPs, etc.) are never
 * retained or logged, including in the failure diagnostic below.
 */
public final class DeviceSmsReader {

    private static final Pattern OTP_PATTERN = Pattern.compile("Use (\\d{6}) to login to AIKYAM app");
    private static final String ADB_EXECUTABLE = AdbBridge.adbExecutable();
    private static volatile String lastDiagnostic = "no attempt made yet";

    private DeviceSmsReader() {
    }

    public static String readLatestAikyamOtp(Duration timeout) {
        long deadlineMillis = System.currentTimeMillis() + timeout.toMillis();

        while (System.currentTimeMillis() < deadlineMillis) {
            for (String body : queryRecentSmsBodies()) {
                Matcher matcher = OTP_PATTERN.matcher(body);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
            sleepQuietly();
        }

        throw new IllegalStateException(
                "AIKYAM OTP SMS not received within " + timeout.getSeconds() + "s; diagnostic: " + lastDiagnostic
        );
    }

    /** Every row's body is scanned for the OTP pattern by the caller and discarded right
     * after - none of them are retained or logged beyond this method. */
    private static List<String> queryRecentSmsBodies() {
        List<String> bodies = new ArrayList<>();
        try {
            // No --where/--sort: any value containing a quote character does not survive
            // the trip to the device intact (see class javadoc). Values with no special
            // characters at all - like these - pass through cleanly as separate argv
            // elements, so filtering/sorting happens here in Java instead.
            Process process = new ProcessBuilder(
                    ADB_EXECUTABLE, "shell", "content", "query",
                    "--uri", "content://sms/inbox", "--projection", "body"
            ).redirectErrorStream(true).start();

            int rowCount = 0;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Row:")) {
                        rowCount++;
                        int bodyIndex = line.indexOf("body=");
                        if (bodyIndex >= 0) {
                            bodies.add(line.substring(bodyIndex + "body=".length()));
                        }
                    }
                }
            }
            int exitCode = process.waitFor();
            lastDiagnostic = "adbExecutable=" + ADB_EXECUTABLE + " exitCode=" + exitCode + " rowsReturned=" + rowCount;
        } catch (IOException | InterruptedException exception) {
            lastDiagnostic = "adbExecutable=" + ADB_EXECUTABLE + " threw "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }
        return bodies;
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
