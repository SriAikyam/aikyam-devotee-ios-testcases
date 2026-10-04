package com.shriaikyam.aikyam.utils;

import java.io.IOException;

/**
 * Small shared helper for shelling out to adb from test code. Gradle's test worker JVM
 * does not necessarily inherit the same PATH as an interactive shell, so "adb" alone can
 * silently fail to launch there even though it resolves fine from the command line -
 * resolve an absolute path via the SDK env vars instead, falling back to bare "adb" only
 * if neither is set.
 */
public final class AdbBridge {

    private static final String ADB_EXECUTABLE = resolveAdbExecutable();

    private AdbBridge() {
    }

    static String resolveAdbExecutable() {
        String sdkHome = System.getenv("ANDROID_HOME");
        if (sdkHome == null || sdkHome.isBlank()) {
            sdkHome = System.getenv("ANDROID_SDK_ROOT");
        }
        if (sdkHome == null || sdkHome.isBlank()) {
            return "adb";
        }
        java.io.File adbExe = new java.io.File(sdkHome, "platform-tools/adb.exe");
        if (adbExe.isFile()) {
            return adbExe.getAbsolutePath();
        }
        java.io.File adbUnix = new java.io.File(sdkHome, "platform-tools/adb");
        return adbUnix.isFile() ? adbUnix.getAbsolutePath() : "adb";
    }

    public static String adbExecutable() {
        return ADB_EXECUTABLE;
    }

    /**
     * Injects real keystrokes into whatever field currently has focus, via Android's
     * "input text" shell command - used as a fallback when a Compose text field
     * doesn't support the accessibility ACTION_SET_TEXT that Appium's WebElement
     * .sendKeys() relies on internally (sendKeys silently no-ops in that case, rather
     * than throwing, so callers must verify the value actually landed).
     * "input text" does not handle literal spaces well - they must be escaped as %s.
     */
    public static boolean typeText(String value) {
        try {
            String escaped = value.replace(" ", "%s");
            Process process = new ProcessBuilder(ADB_EXECUTABLE, "shell", "input text " + escaped)
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException exception) {
            return false;
        }
    }

    /** Presses the keyboard's IME "Done" action (the checkmark button, keycode 66/
     * ENTER) - needed after typeText() on some fields for the typed value to actually
     * commit to the field's real state, rather than sitting uncommitted. */
    public static boolean pressImeDone() {
        try {
            Process process = new ProcessBuilder(ADB_EXECUTABLE, "shell", "input keyevent 66")
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException exception) {
            return false;
        }
    }

    /** Selects all existing text (Ctrl+A, keycode 29 with the meta-key modifier) then
     * deletes it (Backspace, keycode 67) - a real "clear the field" for the adb-typing
     * fallback path. Confirmed live: this fallback is only reached after
     * WebElement.clear()/sendKeys() already ran and may have partially inserted text
     * before being judged to have failed - calling typeText() on top of that without
     * clearing again concatenates old and new content instead of replacing it. */
    public static boolean clearField() {
        try {
            Process selectAll = new ProcessBuilder(
                    ADB_EXECUTABLE, "shell", "input keyevent --longpress KEYCODE_MOVE_END"
            ).redirectErrorStream(true).start();
            selectAll.waitFor();
            // Bounded, not exact: repeated backspace covers any realistic field length
            // without needing to know the current text's true size.
            StringBuilder deletes = new StringBuilder();
            for (int i = 0; i < 80; i++) {
                deletes.append("KEYCODE_DEL ");
            }
            Process delete = new ProcessBuilder(
                    ADB_EXECUTABLE, "shell", "input keyevent " + deletes.toString().trim()
            ).redirectErrorStream(true).start();
            return delete.waitFor() == 0;
        } catch (IOException | InterruptedException exception) {
            return false;
        }
    }
}
