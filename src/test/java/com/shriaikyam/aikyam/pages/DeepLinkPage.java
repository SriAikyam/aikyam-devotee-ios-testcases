package com.shriaikyam.aikyam.pages;

import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
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
     * equivalent aikyam:// URL, which ShareLinkRouter routes identically. */
    public boolean fire(String url) {
        String link = url.replaceFirst("^https?://[^/]+/", "aikyam://");
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

    /** Backs out of anything left open and ends on the Home tab. */
    public void returnHome() {
        for (int i = 0; i < 6; i++) {
            if (isHomeFeedShown(Duration.ofSeconds(1))) {
                return;
            }
            if (isShown(id("tab.home"))) {
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
