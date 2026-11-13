package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fires deep links at the DEV app only and reads the screen they land on. */
public class DeepLinkPage extends BasePage {

    /** Dev-Debug bundle only. The deep link is routed to this bundle explicitly, so it can
     * never open the prod app even when both are installed on the simulator. */
    public static final String DEV_BUNDLE_ID = System.getProperty("appium.bundleId", "com.shriaikyam.ikyam.dev");
    private static final Pattern SHORT_LINK = Pattern.compile("https://\\S+/p/[A-Za-z0-9_-]+");

    public DeepLinkPage(IOSDriver driver) {
        super(driver);
    }

    /** The simulator cannot verify universal links, so https share links are fired as the
     * equivalent aikyam:// URL, which ShareLinkRouter routes identically.
     *
     * Uses Appium's mobile: deepLink command which works on both real devices and
     * simulators (XCUITest driver ≥ 4.x). Falls back to xcrun simctl openurl on
     * older driver versions where the mobile command is unavailable. */
    public boolean fire(String url) {
        String link = url.replaceFirst("^https?://[^/]+/", "aikyam://");
        try {
            driver.executeScript("mobile: deepLink", Map.of("url", link, "bundleId", DEV_BUNDLE_ID));
            return true;
        } catch (RuntimeException mobileDeepLinkUnsupported) {
            // Fall back to simctl for simulator-only environments with older driver versions.
            String udid = System.getProperty("appium.udid", "booted");
            try {
                Process p = new ProcessBuilder("xcrun", "simctl", "openurl", udid, link)
                        .redirectErrorStream(true).start();
                p.getInputStream().readAllBytes();
                return p.waitFor() == 0;
            } catch (java.io.IOException | InterruptedException e) {
                return false;
            }
        }
    }

    /** Dev bundle only; app data (and so the login) is kept. */
    public boolean forceStopApp() {
        try {
            return driver.terminateApp(DEV_BUNDLE_ID);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Reads the link from the DEBUG-only share.link probe on the share sheet, or null. */
    public String readLinkFromShareSheet() {
        try {
            WebElement el = new WebDriverWait(driver, SHORT_TIMEOUT).ignoring(StaleElementReferenceException.class)
                    .until(d -> d.findElements(id("share.link")).stream().findFirst().orElse(null));
            Matcher m = SHORT_LINK.matcher(el.getAttribute("label"));
            return m.find() ? m.group() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** UIActivityViewController's close control; falls back to swiping the sheet down. */
    public boolean closeShareSheet() {
        try {
            navigateBack();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean waitForText(String text, Duration timeout) {
        return waitFor(textContains(text), timeout);
    }

    public boolean waitFor(By locator, Duration timeout) {
        try {
            return new WebDriverWait(driver, timeout).ignoring(StaleElementReferenceException.class)
                    .until(d -> d.findElements(locator).stream().anyMatch(WebElement::isDisplayed));
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean isShown(By locator) {
        return waitFor(locator, Duration.ofSeconds(2));
    }

    public By byText(String t) {
        return text(t);
    }

    public By byContainsText(String t) {
        return textContains(t);
    }

    public By byDesc(String d) {
        return accessibilityId(d);
    }

    /** First visible text starting with "@" (a profile handle), or null. */
    public String firstHandle() {
        try {
            return new WebDriverWait(driver, SHORT_TIMEOUT).ignoring(StaleElementReferenceException.class)
                    .until(d -> d.findElements(By.className("XCUIElementTypeStaticText")).stream()
                            .filter(WebElement::isDisplayed).map(e -> e.getAttribute("label"))
                            .filter(t -> t != null && t.startsWith("@")).findFirst().orElse(null));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** True when the Home feed (subscriber strip with the All Posts filter, and the top bar's
     * create-post button) is what is showing. */
    public boolean isHomeFeedShown(Duration timeout) {
        return waitFor(id("feed.filter.allPosts"), timeout) && waitFor(id("feed.createPost"), timeout);
    }

    // -------------------------------------------------------------------------
    // Notification testing
    // -------------------------------------------------------------------------

    /**
     * Sends a simulated APNS push notification to the dev bundle via
     * {@code xcrun simctl push}. Writes a temp payload JSON file, fires the
     * command, and cleans up the file.
     *
     * <p>The app must be in the BACKGROUND when this is called — press the home
     * button first ({@link #pressHomeButton()}) so the notification appears as a
     * banner that can be tapped. If the app is in the foreground the banner is
     * still shown (AppDelegate calls completionHandler([.banner, .sound, .badge]))
     * but tapping it is less reliable than when the app is backgrounded.
     *
     * <p>{@code customPayload} keys go at the top level of the APNS JSON alongside
     * {@code aps}: use {@code "postId"}, {@code "liveId"}, {@code "screen"},
     * {@code "deeplink"} / {@code "deep_link"} / {@code "deepLink"}, etc.
     *
     * @return true if xcrun exited 0 (notification delivered to simulator)
     */
    public boolean fireNotification(String title, String body, Map<String, Object> customPayload) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"Simulator Target Bundle\": \"").append(DEV_BUNDLE_ID).append("\",\n");
        json.append("  \"aps\": {\n");
        json.append("    \"alert\": {\"title\": ").append(jsonString(title))
                .append(", \"body\": ").append(jsonString(body)).append("},\n");
        json.append("    \"sound\": \"default\"\n");
        json.append("  }");
        if (customPayload != null && !customPayload.isEmpty()) {
            for (Map.Entry<String, Object> e : customPayload.entrySet()) {
                json.append(",\n  ").append(jsonString(e.getKey())).append(": ");
                Object v = e.getValue();
                if (v instanceof String) {
                    json.append(jsonString((String) v));
                } else {
                    json.append(v);
                }
            }
        }
        json.append("\n}");

        Path tmp = null;
        try {
            tmp = Files.createTempFile("aikyam-notif-", ".json");
            Files.writeString(tmp, json.toString());
            String udid = System.getProperty("appium.udid", "booted");
            Process p = new ProcessBuilder("xcrun", "simctl", "push", udid, DEV_BUNDLE_ID, tmp.toString())
                    .redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {}
            }
        }
    }

    /** Convenience builder: title + body + an ordered map of payload entries. */
    public static Map<String, Object> payload(Object... keyValuePairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }

    /**
     * Presses the iOS Home button to send the app to the background.
     * The Appium session stays connected; the app is not terminated.
     */
    @SuppressWarnings("unchecked")
    public boolean pressHomeButton() {
        try {
            driver.executeScript("mobile: pressButton", Map.of("name", "home"));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Waits for a notification banner containing {@code textFragment} and taps it.
     *
     * <p>When the app is backgrounded, iOS shows the banner in the system window.
     * XCUITest (and therefore Appium) can reach system UI elements via
     * iOSNsPredicateString queries across all windows, so this searches the full
     * element tree for a cell or other element whose label/name contains the
     * expected text.
     *
     * <p>If the predicate search times out it falls back to a coordinate tap at
     * the top-centre of the screen (where banners always appear on iPhone) and
     * returns {@code true} anyway so the test can proceed; the subsequent
     * navigation assertion will catch a failure here.
     *
     * @param textFragment a word or phrase expected to appear in the banner
     * @param timeout      how long to poll for the banner element
     */
    public boolean tapNotificationBanner(String textFragment, Duration timeout) {
        // Notification banners are rendered by SpringBoard in a separate window.
        // On simulator they are reachable from the app's XCUITest session via a
        // predicate that searches across all windows (Appium's default behaviour).
        String escaped = textFragment.replace("'", "\\'");
        By banner = AppiumBy.iOSNsPredicateString(
                "(label CONTAINS '" + escaped + "' OR name CONTAINS '" + escaped + "')"
                        + " AND (type == 'XCUIElementTypeCell'"
                        + " OR type == 'XCUIElementTypeOther'"
                        + " OR type == 'XCUIElementTypeButton')");
        try {
            boolean found = new WebDriverWait(driver, timeout)
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> {
                        WebElement el = d.findElements(banner).stream()
                                .filter(WebElement::isDisplayed)
                                .findFirst().orElse(null);
                        if (el != null) {
                            el.click();
                            return true;
                        }
                        return false;
                    });
            if (found) return true;
        } catch (RuntimeException ignored) {}

        // Fallback: coordinate tap at top-centre where the banner always renders.
        try {
            driver.executeScript("mobile: tap", Map.of(
                    "x", driver.manage().window().getSize().getWidth() / 2.0,
                    "y", 60));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Convenience: backgrounds the app, fires the notification, and taps the
     * resulting banner in one call.
     *
     * @param bannerText   text expected in the notification banner (title or body)
     * @param bannerTimeout how long to wait for the banner to appear
     * @return true if the notification was delivered and the banner was tapped
     */
    public boolean fireAndTapNotification(String title, String body,
                                          Map<String, Object> customPayload,
                                          String bannerText, Duration bannerTimeout) {
        pressHomeButton();
        // Small pause: the app needs to be fully backgrounded before the banner can appear.
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        if (!fireNotification(title, body, customPayload)) return false;
        // Give the simulator a moment to process the APNS payload.
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        return tapNotificationBanner(bannerText, bannerTimeout);
    }

    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /** Backs out of anything left open and ends on the Home tab. */
    public void returnHome() {
        for (int i = 0; i < 6; i++) {
            if (isHomeFeedShown(Duration.ofSeconds(1))) {
                return;
            }
            if (isShown(idOrLabel("tab.home", "Home"))) {
                new HomePage(driver).tapHomeTab();
                if (isHomeFeedShown(Duration.ofSeconds(3))) {
                    return;
                }
            } else {
                try {
                    navigateBack();
                } catch (RuntimeException ignored) {
                    return;
                }
            }
        }
    }
}
