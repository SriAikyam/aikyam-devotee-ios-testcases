package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.appmanagement.ApplicationState;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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
     * `mobile: deepLink` works on simulators and physical devices alike. It is not wrapped:
     * a rejected command (dead session, unsupported driver) must fail the test with its real
     * cause instead of falling back and reporting success. Returns true once the OS has
     * accepted the URL - WHERE it routed is for the caller's assertions to check. */
    public boolean fire(String url) {
        String link = url.replaceFirst("^https?://[^/]+/", "aikyam://");
        driver.executeScript("mobile: deepLink", Map.of("url", link, "bundleId", DEV_BUNDLE_ID));
        return true;
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

    // -------------------------------------------------------------------------
    // Destination probes. Each matches the identifier when the build has one and otherwise
    // what iOS main renders (English copy, as for idOrLabel - see BasePage).
    // -------------------------------------------------------------------------

    /** DevoteeOrdersHistoryView's navigation title (the bar exposes it as its name). */
    public static final String ORDER_HISTORY_TITLE = "Order History";

    private static final Map<String, String> TAB_LABELS = Map.of(
            "tab.home", "Home", "tab.explore", "Explore", "tab.music", "Music",
            "tab.events", "Events", "tab.apps", "Apps", "tab.temple", "Temple", "tab.orders", "Orders");

    /** The tab button - custom bar or iOS 26's native one - carries the selected trait, which
     * XCUITest reports as selected=true. A single read, no wait: as cheap for a negative check
     * as for a positive one. */
    public boolean isTabSelected(String tabId) {
        return driver.findElements(idOrLabel(tabId, TAB_LABELS.get(tabId))).stream()
                .anyMatch(e -> "true".equals(e.getAttribute("selected")));
    }

    public boolean waitForTabSelected(String tabId, Duration timeout) {
        return poll(() -> isTabSelected(tabId), timeout);
    }

    public boolean isBackShownNow() {
        return isDisplayedNow(driver, byDesc("Back"));
    }

    /** All Posts is the strip's first column; @Before starts every case from it. */
    public void selectAllPosts() {
        tapFirstVisible(idOrLabel("feed.filter.allPosts", "All Posts"));
    }

    /** The Home feed: Home is the selected tab and the subscriber strip is showing. */
    public boolean isHomeFeedShown(Duration timeout) {
        return waitFor(idOrLabel("feed.filter.allPosts", "All Posts"), timeout) && isTabSelected("tab.home");
    }

    /** SmoothFeedView's Live mode: its filter row ("LIVE NOW") or, with nothing live, the
     * empty state. Neither exists in All Posts, so this is what a Live-tab route changes. */
    public boolean isLiveFeedShown(Duration timeout) {
        return waitFor(AppiumBy.iOSNsPredicateString(
                "label == 'LIVE NOW' OR label == 'No live events'"), timeout);
    }

    /** PostDetailView's error state - 404 reads "no longer available", other failures
     * "Failed to load post". */
    public boolean isPostErrorShown(Duration timeout) {
        return waitFor(AppiumBy.iOSNsPredicateString("name == 'postDetail.error'"
                + " OR label == 'This post is no longer available' OR label == 'Failed to load post'"), timeout);
    }

    /** YoutubeLivePlayerView: the YouTube embed (a web view) under a floating Back button. Post
     * detail has no web view and a mini-app has no native Back, so the pair is the player. */
    public boolean isLivePlayerShown(Duration timeout) {
        By webView = AppiumBy.iOSClassChain("**/XCUIElementTypeWebView");
        return poll(() -> isDisplayedNow(driver, id("livePlayer.screen"))
                || (isDisplayedNow(driver, webView) && isBackShownNow()), timeout);
    }

    /** A bundled mini-app's web view, recognised by its page: the web view takes the document
     * title (<title>Panchang</title>) and the page renders its name. */
    public boolean isMiniAppShown(String title, Duration timeout) {
        String t = predicateLiteral(title);
        By byTitle = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeWebView' AND label == " + t);
        By byText = AppiumBy.iOSClassChain(
                "**/XCUIElementTypeWebView/**/XCUIElementTypeStaticText[`label CONTAINS " + t + "`]");
        return poll(() -> isDisplayedNow(driver, byTitle) || isDisplayedNow(driver, byText), timeout);
    }

    private boolean poll(java.util.function.BooleanSupplier condition, Duration timeout) {
        try {
            return new WebDriverWait(driver, timeout).ignoring(StaleElementReferenceException.class)
                    .until(d -> condition.getAsBoolean());
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** A link the router rejects leaves no signal of its own, and checking the screen right
     * after firing passes before the app has even processed it. So "nothing happened" is
     * asserted over a window: for all of it {@code tabId} stays selected and nothing is pushed. */
    public boolean staysOnTab(String tabId, Duration window) {
        long deadline = System.nanoTime() + window.toNanos();
        do {
            if (!isTabSelected(tabId) || isBackShownNow()) {
                return false;
            }
        } while (System.nanoTime() < deadline);
        return true;
    }

    public boolean staysOnHome(Duration window) {
        return staysOnTab("tab.home", window) && isHomeFeedShown(SHORT_TIMEOUT);
    }

    // -------------------------------------------------------------------------
    // Push notifications (simulator only - `xcrun simctl push`)
    // -------------------------------------------------------------------------

    private static final String NOTIFICATION_TITLE = "Aikyam";
    private static final Duration BANNER_TIMEOUT = Duration.ofSeconds(10);

    /** The app asks for notification permission contextually (after a follow or a post),
     * not at login - so a fresh simulator never shows a banner. Grants it up front.
     * `mobile: setPermission` needs AppleSimulatorUtils (`brew tap wix/brew && brew install
     * applesimutils`); without it the grant is skipped, which is fine only if permission
     * was already given on this simulator. The grant may relaunch the app, hence the
     * activateApp. */
    public void allowNotifications() {
        try {
            driver.executeScript("mobile: setPermission", Map.of(
                    "bundleId", DEV_BUNDLE_ID, "access", Map.of("notifications", "yes")));
        } catch (RuntimeException applesimutilsMissing) {
            System.out.println("[notifications] could not grant permission: " + applesimutilsMissing.getMessage());
        }
        driver.activateApp(DEV_BUNDLE_ID);
    }

    /** Backgrounds the app, pushes {@code payload} with {@code body} as the alert text, and
     * taps the banner. {@code body} gets a unique suffix so the tap can never hit a banner
     * left over from an earlier test. False when the banner never appeared. */
    public boolean notifyAndTap(String body, Map<String, Object> payload) {
        String unique = body + " " + Long.toString(System.nanoTime(), 36);
        return sendAppToBackground()
                && pushNotification(unique, payload)
                && tapNotificationBanner(unique, BANNER_TIMEOUT);
    }

    /** Ordered payload map from key/value pairs. */
    public static Map<String, Object> payload(Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("payload needs key/value pairs");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put((String) keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return map;
    }

    /** Home button, then waits until the OS reports the app is no longer in the foreground -
     * a banner only shows once it is. */
    private boolean sendAppToBackground() {
        driver.executeScript("mobile: pressButton", Map.of("name", "home"));
        try {
            return new WebDriverWait(driver, SHORT_TIMEOUT).until(
                    d -> driver.queryAppState(DEV_BUNDLE_ID) != ApplicationState.RUNNING_IN_FOREGROUND);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** {@code xcrun simctl push} with the APNS JSON written to a temp file. Payload keys sit at
     * the top level next to {@code aps}, where NotificationHandler.extractRoutingPayload reads them. */
    private boolean pushNotification(String body, Map<String, Object> payload) {
        StringBuilder json = new StringBuilder("{\"aps\":{\"alert\":{\"title\":")
                .append(jsonString(NOTIFICATION_TITLE)).append(",\"body\":").append(jsonString(body))
                .append("},\"sound\":\"default\"}");
        payload.forEach((k, v) -> json.append(',').append(jsonString(k)).append(':')
                .append(v instanceof String ? jsonString((String) v) : String.valueOf(v)));
        json.append('}');

        Path tmp = null;
        try {
            tmp = Files.createTempFile("aikyam-notif-", ".json");
            Files.writeString(tmp, json.toString());
            String udid = System.getProperty("appium.udid", "booted");
            Process p = new ProcessBuilder("xcrun", "simctl", "push", udid, DEV_BUNDLE_ID, tmp.toString())
                    .redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                    // A leftover temp file is harmless.
                }
            }
        }
    }

    /** SpringBoard renders the banner; with the app backgrounded the driver queries the active
     * app (SpringBoard), so the banner is reachable by its text. No coordinate fallback: a
     * blind tap would report success for a banner that never appeared. */
    private boolean tapNotificationBanner(String text, Duration timeout) {
        By banner = AppiumBy.iOSNsPredicateString("label CONTAINS " + predicateLiteral(text));
        try {
            return new WebDriverWait(driver, timeout).ignoring(StaleElementReferenceException.class)
                    .until(d -> {
                        WebElement el = d.findElements(banner).stream()
                                .filter(WebElement::isDisplayed).findFirst().orElse(null);
                        if (el == null) {
                            return false;
                        }
                        el.click();
                        return true;
                    });
        } catch (RuntimeException e) {
            return false;
        }
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
