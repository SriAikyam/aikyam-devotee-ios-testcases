package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The feed's circle strip (All Posts / Temples / Live / people). In this build a person circle
 * does NOT open a status viewer: it scopes the feed to that person (ring on the circle, unseen
 * blue dot cleared). The ring and dot are painted, not exposed to accessibility, so they are read
 * from screenshot pixels. Standalone (FeedPage is not a BasePage).
 */
public class StoriesPage {

    private static final Duration WAIT = Duration.ofSeconds(15);
    private static final Set<String> FIXED = Set.of("All Posts", "Temples", "Live");

    private final AndroidDriver driver;

    public StoriesPage(AndroidDriver driver) {
        this.driver = driver;
    }

    private int height() {
        return driver.manage().window().getSize().getHeight();
    }

    /** Fully visible person-circle avatar nodes (content-desc = name) inside the strip band. */
    private List<WebElement> stripAvatars() {
        int limit = (int) (height() * 0.20);
        List<WebElement> all = driver.findElements(By.xpath("//android.view.View[@content-desc!='']"));
        all.removeIf(e -> {
            try {
                Rectangle r = e.getRect();
                String d = e.getAttribute("content-desc");
                return r.getY() < height() * 0.10 || r.getY() + r.getHeight() > limit
                        || FIXED.contains(d) || d.startsWith("Scroll") || d.equals("Create Post")
                        || d.equals("Profile") || d.equals("AIKYAM") || r.getWidth() < 100;
            } catch (StaleElementReferenceException gone) {
                return true;
            }
        });
        return all;
    }

    /** Names of the person circles currently fully visible in the strip, left to right. */
    public Set<String> visibleCircleNames() {
        try {
            Set<String> names = new LinkedHashSet<>();
            for (WebElement e : stripAvatars()) {
                names.add(e.getAttribute("content-desc").trim());
            }
            return names;
        } catch (StaleElementReferenceException recomposed) {
            return visibleCircleNames();
        }
    }

    private WebElement avatar(String name) {
        for (WebElement e : stripAvatars()) {
            if (name.equals(e.getAttribute("content-desc").trim())) {
                return e;
            }
        }
        return null;
    }

    public boolean tapCircle(String name) {
        WebElement e = avatar(name);
        if (e == null) {
            return false;
        }
        e.click();
        return true;
    }

    /** Distinct authors on the visible post headers below the strip (clickable avatar nodes). */
    public Set<String> visiblePostAuthors() {
        Set<String> authors = new LinkedHashSet<>();
        int top = (int) (height() * 0.20);
        for (WebElement e : driver.findElements(By.xpath("//*[@clickable='true' and @content-desc!='']"))) {
            try {
                Rectangle r = e.getRect();
                String d = e.getAttribute("content-desc");
                if (r.getY() > top && r.getX() < 150 && r.getWidth() < 200
                        && !d.equals("More options") && !d.equals("Namaskara") && !d.equals("Save")
                        && !d.equals("Share") && !d.equals("Comment")) {
                    authors.add(d.trim());
                }
            } catch (StaleElementReferenceException skip) {
                // recomposed mid-read
            }
        }
        return authors;
    }

    public boolean waitForOnlyAuthor(String name) {
        try {
            return new WebDriverWait(driver, WAIT).ignoring(StaleElementReferenceException.class)
                    .until(ignored -> visiblePostAuthors().equals(Set.of(name)));
        } catch (RuntimeException timeout) {
            return false;
        }
    }

    public boolean isStatusViewerOpen() {
        return !driver.findElements(AppiumBy.accessibilityId("Close")).isEmpty()
                && !driver.findElements(AppiumBy.androidUIAutomator("new UiSelector().text(\"Reply\")")).isEmpty();
    }

    /** Taps the Live circle (its icon node carries the "Live" content-desc; the text label is not clickable). */
    public boolean tapLiveCircle() {
        for (WebElement e : driver.findElements(AppiumBy.accessibilityId("Live"))) {
            if (e.getRect().getY() < height() * 0.20) {
                e.click();
                return true;
            }
        }
        return false;
    }

    public boolean tapScrollRight() {
        List<WebElement> l = driver.findElements(AppiumBy.accessibilityId("Scroll right"));
        if (l.isEmpty()) {
            return false;
        }
        l.get(0).click();
        return true;
    }

    public boolean isScrollLeftVisible() {
        return !driver.findElements(AppiumBy.accessibilityId("Scroll left")).isEmpty();
    }

    public void scrollStripToStart() {
        for (int i = 0; i < 8 && isScrollLeftVisible(); i++) {
            driver.findElement(AppiumBy.accessibilityId("Scroll left")).click();
            try {
                Thread.sleep(700);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Waits for the visible circle set to differ from {@code before}. */
    public boolean waitForCircleSetToChangeFrom(Set<String> before) {
        try {
            return new WebDriverWait(driver, WAIT).ignoring(StaleElementReferenceException.class)
                    .until(ignored -> !visibleCircleNames().equals(before));
        } catch (RuntimeException timeout) {
            return false;
        }
    }

    // ---- painted state, read from pixels ----

    private BufferedImage screenshot() {
        try {
            return ImageIO.read(new ByteArrayInputStream(driver.getScreenshotAs(OutputType.BYTES)));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean near(int rgb, int r, int g, int b, int tol) {
        return Math.abs(((rgb >> 16) & 255) - r) < tol && Math.abs(((rgb >> 8) & 255) - g) < tol
                && Math.abs((rgb & 255) - b) < tol;
    }

    /** Any pixel in the box around (cx,cy) matching colour (r,g,b). */
    private static boolean hasColour(BufferedImage img, int cx, int cy, int half, int r, int g, int b, int tol) {
        for (int x = Math.max(0, cx - half); x < Math.min(img.getWidth(), cx + half); x++) {
            for (int y = Math.max(0, cy - half); y < Math.min(img.getHeight(), cy + half); y++) {
                if (near(img.getRGB(x, y), r, g, b, tol)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** True when the named circle shows the blue unseen dot at its bottom-right corner. */
    public boolean hasUnseenDot(String name) {
        WebElement e = avatar(name);
        if (e == null) {
            throw new IllegalStateException("circle not visible: " + name);
        }
        Rectangle r = e.getRect();
        BufferedImage img = screenshot();
        double scale = img.getWidth() / (double) driver.manage().window().getSize().getWidth();
        int cx = (int) ((r.getX() + r.getWidth() / 2.0 + 47) * scale);
        int cy = (int) ((r.getY() + r.getHeight() / 2.0 + 48) * scale);
        return hasColour(img, cx, cy, 12, 33, 150, 243, 60);
    }

    /** True when the named circle (or All Posts / Temples / Live via its label) wears the brand ring. */
    public boolean hasSelectedRing(String name) {
        Rectangle r = ringAnchor(name);
        BufferedImage img = screenshot();
        double scale = img.getWidth() / (double) driver.manage().window().getSize().getWidth();
        int cx = (int) ((r.getX() + r.getWidth() / 2.0) * scale);
        int cy = (int) ((r.getY() + r.getHeight() / 2.0) * scale);
        int radius = (int) (r.getWidth() / 2.0 * 1.0 * scale);
        // ring colour is the brand red/pink; probe the leftmost edge band of the circle
        return hasColour(img, cx - radius + 6, cy, 8, 226, 60, 90, 60);
    }

    private Rectangle ringAnchor(String name) {
        if (FIXED.contains(name)) {
            // fixed circles: the labelled icon node sits at the circle centre; the circle is 58dp
            List<WebElement> l = driver.findElements(AppiumBy.accessibilityId(name));
            for (WebElement e : l) {
                Rectangle r = e.getRect();
                if (r.getY() < height() * 0.20) {
                    return new Rectangle(r.getX() + r.getWidth() / 2 - 76, r.getY() + r.getHeight() / 2 - 76, 152, 152);
                }
            }
            throw new IllegalStateException("fixed circle not visible: " + name);
        }
        WebElement e = avatar(name);
        if (e == null) {
            throw new IllegalStateException("circle not visible: " + name);
        }
        Rectangle r = e.getRect();
        return new Rectangle(r.getX() + r.getWidth() / 2 - 76, r.getY() + r.getHeight() / 2 - 76, 152, 152);
    }
}
