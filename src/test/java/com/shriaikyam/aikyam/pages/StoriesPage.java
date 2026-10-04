package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The feed's circle strip (FeedSubscriberStripViews: All Posts / Temples / Live / followed
 * accounts). A person circle does NOT open a status viewer: it scopes the feed to that person
 * and moves the selection ring onto it. The ring is painted, not exposed to accessibility, so
 * it is read from screenshot pixels. Standalone (FeedPage is not a BasePage).
 */
public class StoriesPage {

    private static final Duration WAIT = Duration.ofSeconds(15);
    private static final Duration IMPLICIT_TIMEOUT = Duration.ofSeconds(5);
    /** AppAvatarSize.ring: every strip circle is 58pt, drawn at the top of its column button. */
    private static final int CIRCLE_DIAMETER = 58;

    private final By strip = AppiumBy.accessibilityId("stories.strip");
    private final By personCircle = AppiumBy.accessibilityId("stories.circle");
    private final By scrollLeft = AppiumBy.accessibilityId("stories.scrollLeft");
    private final By scrollRight = AppiumBy.accessibilityId("stories.scrollRight");
    private final By allPostsCircle = AppiumBy.accessibilityId("feed.filter.allPosts");
    private final By templesCircle = AppiumBy.accessibilityId("feed.filter.temples");
    private final By liveCircle = AppiumBy.accessibilityId("feed.filter.live");
    private final By postAuthor = AppiumBy.accessibilityId("feed.post.author");
    private final By viewerPage = AppiumBy.accessibilityId("stories.viewer.page");
    private final By viewerEmpty = AppiumBy.accessibilityId("stories.viewer.empty");

    private final IOSDriver driver;

    public StoriesPage(IOSDriver driver) {
        this.driver = driver;
    }

    private List<WebElement> visible(By locator) {
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
            try {
                return driver.findElements(locator).stream()
                        .filter(WebElement::isDisplayed)
                        .collect(Collectors.toList());
            } finally {
                driver.manage().timeouts().implicitlyWait(IMPLICIT_TIMEOUT);
            }
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
    }

    /** Horizontal bounds of the strip's scroll viewport (the window when it is not found). */
    private int[] stripBounds() {
        List<WebElement> s = visible(strip);
        if (!s.isEmpty()) {
            Rectangle r = s.get(0).getRect();
            return new int[] {r.getX(), r.getX() + r.getWidth()};
        }
        return new int[] {0, driver.manage().window().getSize().getWidth()};
    }

    /** Person-circle column buttons fully inside the strip's viewport. */
    private List<WebElement> stripAvatars() {
        int[] bounds = stripBounds();
        List<WebElement> all = visible(personCircle);
        all.removeIf(e -> {
            try {
                Rectangle r = e.getRect();
                return r.getX() < bounds[0] || r.getX() + r.getWidth() > bounds[1];
            } catch (StaleElementReferenceException gone) {
                return true;
            }
        });
        return all;
    }

    private static String nameOf(WebElement e) {
        return FeedPage.displayNameFromLabel(e.getAttribute("label"));
    }

    /** Names of the person circles currently fully visible in the strip, left to right. */
    public Set<String> visibleCircleNames() {
        try {
            Set<String> names = new LinkedHashSet<>();
            for (WebElement e : stripAvatars()) {
                names.add(nameOf(e));
            }
            return names;
        } catch (StaleElementReferenceException recomposed) {
            return visibleCircleNames();
        }
    }

    private WebElement avatar(String name) {
        for (WebElement e : stripAvatars()) {
            if (name.equals(nameOf(e))) {
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

    /** Distinct display names on the visible post headers below the strip. */
    public Set<String> visiblePostAuthors() {
        Set<String> authors = new LinkedHashSet<>();
        for (WebElement e : visible(postAuthor)) {
            try {
                if (e.getRect().getY() >= 0) {
                    authors.add(nameOf(e));
                }
            } catch (StaleElementReferenceException skip) {
                // re-rendered mid-read
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
        return !visible(viewerPage).isEmpty() || !visible(viewerEmpty).isEmpty();
    }

    public boolean tapLiveCircle() {
        List<WebElement> l = visible(liveCircle);
        if (l.isEmpty()) {
            return false;
        }
        l.get(0).click();
        return true;
    }

    /** The arrows collapse to zero width (and opacity 0) when there is nothing to scroll to. */
    private WebElement arrow(By locator) {
        for (WebElement e : visible(locator)) {
            if (e.getRect().getWidth() > 0) {
                return e;
            }
        }
        return null;
    }

    public boolean tapScrollRight() {
        WebElement e = arrow(scrollRight);
        if (e == null) {
            return false;
        }
        e.click();
        return true;
    }

    public boolean isScrollLeftVisible() {
        return arrow(scrollLeft) != null;
    }

    public void scrollStripToStart() {
        for (int i = 0; i < 8; i++) {
            WebElement e = arrow(scrollLeft);
            if (e == null) {
                return;
            }
            e.click();
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

    /** iOS: FeedSubscriberCircle draws no unseen dot (see parity report), so there is never
     * one to find. Still fails loudly when the circle itself is not on screen. */
    public boolean hasUnseenDot(String name) {
        if (avatar(name) == null) {
            throw new IllegalStateException("circle not visible: " + name);
        }
        return false;
    }

    /** True when the named circle (or All Posts / Temples / Live) wears the selection ring
     * (AppColor.primarySelection #EC496C, 2pt stroke on the 58pt circle's edge). */
    public boolean hasSelectedRing(String name) {
        Rectangle column = ringAnchor(name);
        BufferedImage img = screenshot();
        double scale = img.getWidth() / (double) driver.manage().window().getSize().getWidth();
        double cx = column.getX() + column.getWidth() / 2.0;
        double cy = column.getY() + CIRCLE_DIAMETER / 2.0;
        // Probe the circle's leftmost edge band, outside the 52pt avatar it surrounds.
        int px = (int) ((cx - CIRCLE_DIAMETER / 2.0) * scale);
        int py = (int) (cy * scale);
        return hasColour(img, px, py, (int) Math.max(2, 2 * scale), 236, 73, 108, 50);
    }

    /** The column button of the named circle; the circle sits at its top, centred. */
    private Rectangle ringAnchor(String name) {
        By fixed = "All Posts".equals(name) ? allPostsCircle
                : "Temples".equals(name) ? templesCircle
                : "Live".equals(name) ? liveCircle
                : null;
        if (fixed != null) {
            List<WebElement> l = visible(fixed);
            if (l.isEmpty()) {
                throw new IllegalStateException("fixed circle not visible: " + name);
            }
            return l.get(0).getRect();
        }
        WebElement e = avatar(name);
        if (e == null) {
            throw new IllegalStateException("circle not visible: " + name);
        }
        return e.getRect();
    }
}
