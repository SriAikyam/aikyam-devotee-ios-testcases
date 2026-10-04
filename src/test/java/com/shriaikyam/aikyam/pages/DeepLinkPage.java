package com.shriaikyam.aikyam.pages;

import com.shriaikyam.aikyam.utils.AdbBridge;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fires deep links at the DEV app only and reads the screen they land on. */
public class DeepLinkPage extends BasePage {

    public static final String DEV_PACKAGE = "com.shriaikyam.aikyam.dev";
    private static final Pattern SHORT_LINK = Pattern.compile("https://\\S+/p/[A-Za-z0-9_-]+");

    public DeepLinkPage(AndroidDriver driver) {
        super(driver);
    }

    private static boolean adb(String... args) {
        String[] cmd = new String[args.length + 1];
        cmd[0] = AdbBridge.adbExecutable();
        System.arraycopy(args, 0, cmd, 1, args.length);
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            p.getInputStream().readAllBytes();
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private static String device() {
        String udid = System.getProperty("appium.udid");
        return udid == null || udid.isBlank() ? "emulator-5554" : udid;
    }

    /** VIEW intent pinned to the dev package so it can never reach another app or prod. */
    public boolean fire(String url) {
        return adb("-s", device(), "shell", "am", "start", "-W", "-a", "android.intent.action.VIEW",
                "-d", "'" + url + "'", DEV_PACKAGE);
    }

    /** Dev package only; app data (and so the login) is kept. */
    public boolean forceStopApp() {
        return adb("-s", device(), "shell", "am", "force-stop", DEV_PACKAGE);
    }

    /** Reads the share link out of the open native chooser's preview, or null. */
    public String readLinkFromShareSheet() {
        try {
            WebElement el = new WebDriverWait(driver, SHORT_TIMEOUT).ignoring(StaleElementReferenceException.class)
                    .until(d -> d.findElements(textContains("shriaikyam.com/p/")).stream().findFirst().orElse(null));
            Matcher m = SHORT_LINK.matcher(el.getText());
            return m.find() ? m.group() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public boolean closeShareSheet() {
        try {
            driver.navigate().back();
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
                    .until(d -> d.findElements(By.className("android.widget.TextView")).stream()
                            .filter(WebElement::isDisplayed).map(WebElement::getText)
                            .filter(t -> t != null && t.startsWith("@")).findFirst().orElse(null));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** True when the Home feed (stories strip with the "All Posts" filter) is what is showing. */
    public boolean isHomeFeedShown(Duration timeout) {
        return waitFor(text("All Posts"), timeout) && waitFor(accessibilityId("Create Post"), timeout);
    }

    /** Backs out of anything left open and ends on the Home tab. */
    public void returnHome() {
        for (int i = 0; i < 6; i++) {
            if (isHomeFeedShown(Duration.ofSeconds(1))) {
                return;
            }
            if (isShown(text("Home"))) {
                new HomePage(driver).tapHomeTab();
                if (isHomeFeedShown(Duration.ofSeconds(3))) {
                    return;
                }
            } else {
                try {
                    driver.navigate().back();
                } catch (RuntimeException ignored) {
                    return;
                }
            }
        }
    }
}
