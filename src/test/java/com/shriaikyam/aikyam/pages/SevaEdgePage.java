package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks shared by the seva corner cases (groups 5-9): error and empty states, layout fit,
 * prices, media, sharing, app relaunch with launch arguments, and the temple-side Perform Seva
 * screen. Strings are the app's en.lproj values (AikyamDevotee/en.lproj/Localizable.strings).
 */
public class SevaEdgePage extends BasePage {

    public static final String SEVAS_NETWORK_ERROR = "Unable to load sevas. Check your network connection.";
    public static final String SEVA_DETAILS_ERROR = "Unable to load seva details";
    public static final String NO_SEVAS = "No sevas available";
    public static final String NO_UPCOMING = "No upcoming sevas";
    public static final String NO_PAST = "No past sevas";
    public static final String NO_ORDERS = "No orders found";
    public static final String CONNECTION_DROPPED = "The connection dropped before we could confirm your booking";
    public static final String SESSION_EXPIRED = "Session expired. Please sign in again.";
    public static final String FREQUENT_SEVAS = "Your frequent sevas";
    public static final String CAMERA_PERMISSION = "Camera permission is required to capture photos or videos.";
    public static final String MIC_PERMISSION = "Microphone permission is required to record audio.";
    public static final String DETAILS_UPDATED = "Seva details updated successfully";
    public static final String ORDER_COMPLETED = "Seva order completed successfully";
    public static final String NO_TEMPLE_CONTEXT = "No active temple context";

    private static final Pattern RUPEE = Pattern.compile("₹\\s?([0-9][0-9,]*(?:\\.[0-9]+)?)");
    // Indian grouping: 999, 1,000, 12,345, 1,23,456. No decimals.
    private static final Pattern GROUPED = Pattern.compile("\\d{1,3}|\\d{1,2}(,\\d{2})*,\\d{3}");
    private static final Pattern KANNADA = Pattern.compile("[\\u0C80-\\u0CFF]");

    private final By retry = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND (label ==[c] 'Retry' OR label ==[c] 'Try again')");
    private final By spinner = AppiumBy.className("XCUIElementTypeActivityIndicator");
    private final By staticTexts = AppiumBy.className("XCUIElementTypeStaticText");
    private final By interactive = AppiumBy.iOSNsPredicateString(
            "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeTextField' OR type == 'XCUIElementTypeSwitch')"
                    + " AND visible == 1");
    private final By media = AppiumBy.iOSNsPredicateString(
            "(type == 'XCUIElementTypeImage' OR type == 'XCUIElementTypeOther') AND (name CONTAINS[c] 'media'"
                    + " OR name CONTAINS[c] 'photo' OR name CONTAINS[c] 'video' OR name CONTAINS[c] 'audio')");
    private final By shareOrCopy = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND (label ==[c] 'Share' OR label ==[c] 'Copy'"
                    + " OR name CONTAINS[c] 'share' OR name CONTAINS[c] 'copy')");
    private final By closeButton = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND (label ==[c] 'Close' OR label ==[c] 'Done' OR name == 'xmark')");
    private final By playButton = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND (label CONTAINS[c] 'Play' OR name CONTAINS[c] 'play')");
    private final By performSeva = text("Perform Seva");
    private final By takePhoto = AppiumBy.iOSNsPredicateString("label BEGINSWITH 'Take'");
    private final By recordAudio = AppiumBy.iOSNsPredicateString("label BEGINSWITH 'Record' AND label CONTAINS 'Audio'");
    private final By complete = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND label == 'Complete'");
    private final By update = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND label == 'Update'");
    private final By message = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField'");

    public SevaEdgePage(IOSDriver driver) {
        super(driver);
    }

    // ---- text on screen ----

    public boolean isTextShown(String exactOrPrefix) {
        return isAnyVisible(text(exactOrPrefix), textContains(exactOrPrefix));
    }

    public boolean isTextShownNow(String part) {
        return isVisibleNow(textContains(part));
    }

    public boolean scrollToText(String part) {
        return scrollUntilVisible(8, textContains(part));
    }

    public boolean isRetryVisible() {
        return isAnyVisible(retry);
    }

    public boolean tapRetry() {
        return tapFirstVisible(retry);
    }

    public boolean isSpinnerVisibleNow() {
        return isVisibleNow(spinner);
    }

    /** True once no activity indicator is left on screen within the timeout. */
    public boolean waitUntilSettled(java.time.Duration timeout) {
        return waitUntilGone(timeout, spinner);
    }

    /** Labels of every static text on screen, top to bottom. */
    public List<String> visibleTexts() {
        List<String> out = new ArrayList<>();
        for (WebElement e : visibleElements(staticTexts)) {
            String l = e.getAttribute("label");
            if (l != null && !l.isBlank()) {
                out.add(l.trim());
            }
        }
        return out;
    }

    public boolean scrollDown() {
        swipeUp();
        return true;
    }

    public boolean refresh() {
        pullToRefresh();
        return true;
    }

    // ---- prices ----

    /** Every ₹ amount on screen whose digits are not grouped the Indian way, or that carry decimals. */
    public List<String> badlyFormattedPrices() {
        List<String> bad = new ArrayList<>();
        for (String label : visibleTexts()) {
            Matcher m = RUPEE.matcher(label);
            while (m.find()) {
                if (!GROUPED.matcher(m.group(1)).matches()) {
                    bad.add(m.group());
                }
            }
        }
        return bad;
    }

    public int priceCount() {
        int n = 0;
        for (String label : visibleTexts()) {
            Matcher m = RUPEE.matcher(label);
            while (m.find()) {
                n++;
            }
        }
        return n;
    }

    // ---- layout ----

    /** Static texts, buttons and fields that stick out past the screen edge (clipped). */
    public List<String> elementsOffScreenHorizontally() {
        Dimension screen = driver.manage().window().getSize();
        List<String> out = new ArrayList<>();
        List<WebElement> all = new ArrayList<>(visibleElements(staticTexts));
        all.addAll(visibleElements(interactive));
        for (WebElement e : all) {
            Rectangle r = e.getRect();
            if (r.getWidth() > 0 && (r.getX() < -1 || r.getX() + r.getWidth() > screen.getWidth() + 1)) {
                out.add(e.getAttribute("label") + " @x=" + r.getX() + " w=" + r.getWidth());
            }
        }
        return out;
    }

    /** Buttons, fields and switches that VoiceOver would read as nothing. */
    public List<String> unlabelledControls() {
        List<String> out = new ArrayList<>();
        for (WebElement e : visibleElements(interactive)) {
            String label = e.getAttribute("label");
            String name = e.getAttribute("name");
            if ((label == null || label.isBlank()) && (name == null || name.isBlank())) {
                out.add(e.getAttribute("type") + " " + e.getRect());
            }
        }
        return out;
    }

    public boolean isScreenWiderThanTall() {
        Dimension d = driver.manage().window().getSize();
        return d.getWidth() > d.getHeight();
    }

    public int screenWidthPoints() {
        return driver.manage().window().getSize().getWidth();
    }

    // ---- language ----

    public boolean isAnyKannadaTextShown() {
        for (String t : visibleTexts()) {
            if (KANNADA.matcher(t).find()) {
                return true;
            }
        }
        return false;
    }

    /** English UI strings still showing; brand names and numbers are ignored. */
    public List<String> untranslatedEnglish(String... englishLabels) {
        List<String> left = new ArrayList<>();
        for (String s : englishLabels) {
            if (isVisibleNow(text(s))) {
                left.add(s);
            }
        }
        return left;
    }

    /** Relaunches the dev app with launch arguments (e.g. -AppleLanguages (kn)); none restores defaults. */
    public void relaunchWithArguments(String... args) {
        String bundle = DeepLinkPage.DEV_BUNDLE_ID;
        driver.executeScript("mobile: terminateApp", Map.of("bundleId", bundle));
        driver.executeScript("mobile: launchApp", Map.of("bundleId", bundle, "arguments", List.of(args)));
    }

    // ---- media and share ----

    public boolean isAnyMediaShown() {
        return scrollUntilVisible(6, media);
    }

    public int mediaCount() {
        return visibleElements(media).size();
    }

    public boolean openFirstMedia() {
        return tapFirstVisible(media);
    }

    public boolean isPlayControlShown() {
        return isAnyVisible(playButton);
    }

    public boolean closeViewer() {
        return tapFirstVisible(closeButton) || navigateBack();
    }

    public boolean tapShareOrCopy() {
        return scrollUntilVisible(4, shareOrCopy) && tapFirstVisible(shareOrCopy);
    }

    public boolean isShareOrCopyShown() {
        return scrollUntilVisible(4, shareOrCopy);
    }

    public String clipboardText() {
        return driver.getClipboardText();
    }

    // ---- temple side: Perform Seva ----

    public boolean tapPerformSeva() {
        return scrollUntilVisible(6, performSeva) && tapFirstVisible(performSeva);
    }

    public boolean tapTakePhoto() {
        return scrollUntilVisible(4, takePhoto) && tapFirstVisible(takePhoto);
    }

    public boolean tapRecordAudio() {
        return scrollUntilVisible(4, recordAudio) && tapFirstVisible(recordAudio);
    }

    public boolean typeMessage(String value) {
        return scrollUntilVisible(4, message) && typeIntoFirstVisible(value, message);
    }

    public boolean tapComplete() {
        return scrollUntilVisible(4, complete) && tapFirstVisible(complete);
    }

    public boolean tapUpdate() {
        return scrollUntilVisible(4, update) && tapFirstVisible(update);
    }

    public boolean isUpdateShown() {
        return scrollUntilVisible(4, update);
    }

    /** Dismisses a system permission prompt with "Don't Allow". */
    public boolean denySystemPrompt() {
        try {
            driver.executeScript("mobile: alert", Map.of("action", "dismiss"));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean tapText(String label) {
        return scrollUntilVisible(8, textContains(label)) && tapFirstVisible(text(label), textContains(label));
    }

    public boolean back() {
        return navigateBack();
    }
}
