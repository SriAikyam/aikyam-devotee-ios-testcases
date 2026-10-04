package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.junit.AssumptionViolatedException;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Home feed (SmoothFeedView + FeedPostRow). Standalone rather than a BasePage subclass:
 * its private helpers predate BasePage and collide with BasePage's protected ones, so it
 * reuses BasePage's public static gestures instead. */
public class FeedPage {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration SCROLL_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration IMPLICIT_TIMEOUT = Duration.ofSeconds(5);
    private static final Pattern COUNT = Pattern.compile("^[0-9][0-9,.]*[KkMm]?$");
    private static final Pattern CAROUSEL_COUNTER = Pattern.compile("\\d+/\\d+");

    private final IOSDriver driver;

    private final By homeAction = AppiumBy.accessibilityId("tab.home");
    // Namaskara IS the like action; there is no separate Like control.
    private final By namaskaraAction = AppiumBy.accessibilityId("feed.post.like");
    private final By commentAction = AppiumBy.accessibilityId("feed.post.comment");
    private final By shareAction = AppiumBy.accessibilityId("feed.post.share");
    private final By saveAction = AppiumBy.accessibilityId("feed.post.save");
    // Only rendered on someone else's post (PostOptionsPolicy hides it on own posts in the feed).
    private final By moreOptionsAction = AppiumBy.accessibilityId("feed.post.more");
    private final By authorHeader = AppiumBy.accessibilityId("feed.post.author");
    private final By profileAvatarAction = AppiumBy.accessibilityId("header.account");
    private final By createPostAction = AppiumBy.accessibilityId("feed.createPost");
    private final By cancelAction = BasePage.byExactText("Cancel");
    private final By unsaveDialogAction = AppiumBy.iOSNsPredicateString("label BEGINSWITH 'Unsave'");

    // Post options is a SwiftUI Menu, not a titled sheet: visitors get Save/Follow/.../Report,
    // owners (where the screen allows it) get Delete only.
    private final By postOptionsDeleteAction = AppiumBy.accessibilityId("postOptions.delete");
    private final By postOptionsDeleteText = BasePage.byExactText("Delete");
    private final By postOptionsReportAction = AppiumBy.accessibilityId("postOptions.report");
    private final By postOptionsReportText = BasePage.byExactText("Report");
    private final By menuDismissRegion = AppiumBy.accessibilityId("PopoverDismissRegion");

    // FeedSubscriberStrip renders nothing at all when the account follows no one.
    private final By allPostsFilterTab = AppiumBy.accessibilityId("feed.filter.allPosts");
    private final By templesFilterTab = AppiumBy.accessibilityId("feed.filter.temples");
    private final By liveFilterTab = AppiumBy.accessibilityId("feed.filter.live");

    // The video volume toggle's identifier names the action a tap would perform, so it
    // reports the current state inverted: "video.mute" while audible, "video.unmute" while muted.
    private final By muteAction = AppiumBy.accessibilityId("video.mute");
    private final By unmuteAction = AppiumBy.accessibilityId("video.unmute");

    public FeedPage(IOSDriver driver) {
        this.driver = driver;
    }

    private String bundleId() {
        Object bundleId = driver.getCapabilities().getCapability("appium:bundleId");
        if (bundleId == null) {
            bundleId = driver.getCapabilities().getCapability("bundleId");
        }
        return bundleId != null ? bundleId.toString() : DeepLinkPage.DEV_BUNDLE_ID;
    }

    public void waitUntilLoaded() {
        dismissUnsaveDialogIfVisible();
        if (isAnyFeedItemVisible()) {
            return;
        }

        if (tapHomeIfVisible() && searchFeedItemInViewport()) {
            return;
        }

        for (int attempt = 0; attempt < 4; attempt++) {
            pressBackQuietly();
            dismissUnsaveDialogIfVisible();
            if (isAnyFeedItemVisible()) {
                return;
            }
            if (tapHomeIfVisible() && searchFeedItemInViewport()) {
                return;
            }
        }

        waitForFeedItemInViewport();
    }

    public boolean isAnyFeedItemVisible() {
        return feedItemLocators().stream()
                .anyMatch(locator -> !visibleElements(locator).isEmpty());
    }

    public boolean isLikeActionVisible() {
        return findAnyAction(likeActionLocators());
    }

    public boolean isCommentActionVisible() {
        return findAnyAction(commentActionLocators());
    }

    public boolean isShareActionVisible() {
        return findAnyAction(shareActionLocators());
    }

    public boolean isNamaskaraActionVisible() {
        return findAnyAction(namaskaraActionLocators());
    }

    public boolean isSaveActionVisible() {
        return findAnyAction(saveActionLocators());
    }

    public boolean isMoreOptionsActionVisible() {
        return findAnyAction(moreOptionsActionLocators());
    }

    public int visibleFeedActionCount() {
        int maxVisibleCount = countVisibleFeedActions();
        for (int attempt = 0; maxVisibleCount < 5 && attempt < 4; attempt++) {
            swipeUp();
            maxVisibleCount = Math.max(maxVisibleCount, countVisibleFeedActions());
        }
        return maxVisibleCount;
    }

    public boolean hasNonBlankPageSource() {
        String pageSource = driver.getPageSource();
        return pageSource != null && !pageSource.isBlank();
    }

    /**
     * Scrolls repeatedly to reach the tail of available feed content. The app has no
     * dedicated "end of feed" marker, so this asserts the feed stays responsive
     * (still showing content, not crashed or stuck on a blank/loading screen) rather
     * than a specific end-of-list indicator.
     */
    public boolean isFeedUsableAfterExtensiveScrolling(int scrollAttempts) {
        for (int i = 0; i < scrollAttempts; i++) {
            swipeUp();
        }
        return isAnyFeedItemVisible();
    }

    /**
     * Rapidly alternates Like/Namaskara taps on whatever post is currently visible, to
     * check the toggle state doesn't corrupt or crash the feed under fast repeated input.
     */
    public boolean isFeedStableAfterRapidLikeNamaskaraTaps(int repetitions) {
        for (int i = 0; i < repetitions; i++) {
            tapIfPresent(likeActionLocators());
            tapIfPresent(namaskaraActionLocators());
        }
        return isAnyFeedItemVisible();
    }

    private void tapIfPresent(List<By> locators) {
        WebElement element = firstVisibleAction(locators);
        if (element != null) {
            try {
                element.click();
            } catch (RuntimeException ignored) {
                // A rapid second tap can race the first one's re-render; skip and continue.
            }
        }
    }

    /** iOS: XCUITest has no airplane-mode/network control for the simulator, so an offline
     * scenario cannot be driven from here. Enabling skips the calling test instead of letting
     * it pass against a still-online app; disabling is a no-op so teardown stays safe. */
    public void setAirplaneMode(boolean enabled) {
        if (enabled) {
            throw new AssumptionViolatedException(
                    "iOS: network cannot be toggled through XCUITest; offline scenario not runnable");
        }
    }

    /**
     * True once the app is confirmed not to have crashed or gone blank while offline -
     * cached content or a graceful error/retry state both count as "handled gracefully".
     */
    public boolean isAppResponsiveOffline() {
        swipeUp();
        return hasNonBlankPageSource();
    }

    /**
     * iOS has no system Back: repeats the in-app back gesture/controls across whatever screens
     * that reaches, then re-activates the app to confirm it recovers rather than staying
     * stuck or corrupted.
     */
    public boolean isAppUsableAfterBackButtonSpam(int presses) {
        for (int i = 0; i < presses; i++) {
            try {
                BasePage.navigateBack(driver);
            } catch (RuntimeException ignored) {
                break;
            }
        }
        driver.activateApp(bundleId());
        waitUntilLoaded();
        return isAnyFeedItemVisible();
    }

    /** Searches currently-rendered feed content for the given caption, scrolling to look for it. */
    public boolean isCaptionVisibleInFeed(String caption) {
        if (pageSourceContains(caption)) {
            return true;
        }
        for (int attempt = 0; attempt < 4; attempt++) {
            swipeUp();
            if (pageSourceContains(caption)) {
                return true;
            }
        }
        return false;
    }

    private boolean pageSourceContains(String value) {
        String pageSource = driver.getPageSource();
        return pageSource != null && pageSource.contains(value);
    }

    /** Taps the top-bar avatar, which opens the account/settings hub. */
    public boolean openOwnProfile() {
        return tapFirstVisible(profileAvatarAction);
    }

    public boolean openCreatePost() {
        if (tapFirstVisible(createPostAction)) {
            return true;
        }

        // A prior test can leave a detail screen pushed onto the Home tab's own stack;
        // re-tapping the active Home tab only scrolls to top, it does not pop. Back out first,
        // until the tab bar (and with it the feed root) is reachable again.
        for (int attempt = 0; attempt < 6 && visibleElements(homeAction).isEmpty(); attempt++) {
            try {
                BasePage.navigateBack(driver);
            } catch (RuntimeException ignored) {
                break;
            }
        }

        tapHomeIfVisible();
        for (int attempt = 0; attempt < 4; attempt++) {
            if (tapFirstVisible(createPostAction)) {
                return true;
            }
            swipeDown();
        }

        return false;
    }

    public void tapFirstLikeAction() {
        tapFirstAction(likeActionLocators());
    }

    public void tapFirstNamaskaraAction() {
        tapFirstAction(namaskaraActionLocators());
    }

    public void tapFirstSaveAction() {
        tapFirstAction(saveActionLocators());
        dismissUnsaveDialogIfVisible();
        new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ignored -> {
                    dismissUnsaveDialogIfVisible();
                    return isAnyFeedItemVisible();
                });
    }

    public void tapFirstMoreOptionsAction() {
        tapFirstAction(moreOptionsActionLocators());
    }

    public boolean tapFirstCommentAction() {
        WebElement action = firstVisibleAction(commentActionLocators());
        for (int attempt = 0; action == null && attempt < 4; attempt++) {
            swipeUp();
            action = firstVisibleAction(commentActionLocators());
        }
        if (action == null) {
            return false;
        }
        action.click();
        return true;
    }

    /** Signature of the counts on every visible Namaskara/Comment/Share button. The count is
     * a Text inside the SwiftUI Button, so it surfaces only as the last ", "-separated part of
     * the button's label (absent when zero). Used to prove a tap actually changed something
     * real, not just that the feed didn't crash. */
    public String captureNumericCountsSignature() {
        List<String> counts = new ArrayList<>();
        for (By locator : List.of(namaskaraAction, commentAction, shareAction)) {
            for (WebElement element : visibleElements(locator)) {
                try {
                    counts.add(countFromLabel(element.getAttribute("label")));
                } catch (StaleElementReferenceException ignored) {
                    // Re-rendered mid-read; the caller's wait retries.
                }
            }
            counts.add("/");
        }
        return String.join("|", counts);
    }

    /** Count beside the first visible Comment icon; 0 when the app hides it (zero comments). */
    public int firstVisibleCommentCount() {
        WebElement comment = firstVisibleAction(commentActionLocators());
        if (comment == null) {
            throw new IllegalStateException("No Comment action visible on the feed");
        }
        String count = countFromLabel(comment.getAttribute("label"));
        String digits = count.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    static String countFromLabel(String label) {
        if (label == null) {
            return "";
        }
        String[] parts = label.split(", ");
        String last = parts[parts.length - 1].trim();
        return COUNT.matcher(last).matches() ? last : "";
    }

    /** A FeedPostRow header button's label is the avatar initial, the display name, then the
     * "@handle · time" (or location) line, joined by ", ". Returns the display name. */
    public static String displayNameFromLabel(String label) {
        if (label == null) {
            return null;
        }
        String fallback = null;
        for (String part : label.split(", ")) {
            String p = part.trim();
            if (p.isEmpty() || p.startsWith("@") || p.contains(" · ")) {
                continue;
            }
            if (p.length() > 2) {
                return p;
            }
            if (fallback == null) {
                fallback = p;
            }
        }
        return fallback != null ? fallback : label.trim();
    }

    /** Opens the post options menu from the first available "More options" icon and returns
     * which owner/visitor action it shows ("Delete" or "Report"), or null if neither appeared. */
    public String openFirstPostOptionsSheet() {
        if (!findAnyAction(moreOptionsActionLocators())) {
            return null;
        }
        WebElement moreOptions = firstVisibleAction(moreOptionsActionLocators());
        if (moreOptions == null) {
            return null;
        }
        // Keep the icon clear of the status bar/floating top bar before tapping it.
        if (moreOptions.getLocation().getY() < driver.manage().window().getSize().getHeight() * 0.15) {
            swipeDown();
            moreOptions = firstVisibleAction(moreOptionsActionLocators());
            if (moreOptions == null) {
                return null;
            }
        }
        moreOptions.click();
        try {
            waitForFirstVisible(postOptionsDeleteAction, postOptionsReportAction, postOptionsDeleteText, postOptionsReportText);
        } catch (RuntimeException exception) {
            return null;
        }
        if (!visibleElements(postOptionsDeleteAction).isEmpty() || !visibleElements(postOptionsDeleteText).isEmpty()) {
            return "Delete";
        }
        if (!visibleElements(postOptionsReportAction).isEmpty() || !visibleElements(postOptionsReportText).isEmpty()) {
            return "Report";
        }
        return null;
    }

    public boolean isPostOptionsSheetVisible() {
        return !visibleElements(postOptionsDeleteAction).isEmpty()
                || !visibleElements(postOptionsReportAction).isEmpty()
                || !visibleElements(postOptionsDeleteText).isEmpty()
                || !visibleElements(postOptionsReportText).isEmpty();
    }

    /** Dismisses the menu without acting on any option: a SwiftUI Menu closes on a tap
     * outside it (XCUITest exposes that area as PopoverDismissRegion when present). */
    public boolean dismissPostOptionsSheet() {
        try {
            List<WebElement> dismiss = visibleElements(menuDismissRegion);
            if (!dismiss.isEmpty()) {
                dismiss.get(0).click();
                return true;
            }
            Dimension screen = driver.manage().window().getSize();
            BasePage.tapPoint(driver, screen.getWidth() / 2, (int) (screen.getHeight() * 0.06));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** The subscriber strip sits at the very top of the feed, so recovering it needs scrolling
     * back UP if an earlier test left the feed scrolled down. */
    public boolean isAllPostsFilterVisible() {
        return scrollToTopThenFind(allPostsFilterTab);
    }

    public boolean isTemplesFilterVisible() {
        return scrollToTopThenFind(templesFilterTab);
    }

    private boolean scrollToTopThenFind(By locator) {
        if (!visibleElements(locator).isEmpty()) {
            return true;
        }
        // Re-tapping the already-selected Home tab scrolls the feed to the top
        // (SmoothFeedView's tabReselectGeneration handler) - try it before swiping.
        tapHomeIfVisible();
        if (!visibleElements(locator).isEmpty()) {
            return true;
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            swipeDown();
            if (!visibleElements(locator).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean tapAllPostsFilter() {
        scrollToTopThenFind(allPostsFilterTab);
        return tapFirstVisible(allPostsFilterTab);
    }

    public boolean tapTemplesFilter() {
        scrollToTopThenFind(templesFilterTab);
        return tapFirstVisible(templesFilterTab);
    }

    public boolean tapLiveFilter() {
        return tapFirstVisible(liveFilterTab);
    }

    public String captureFeedState() {
        List<String> visibleFeedParts = new ArrayList<>();

        for (By locator : feedItemLocators()) {
            visibleFeedParts.addAll(
                    visibleElements(locator).stream()
                            .map(this::elementState)
                            .collect(Collectors.toList())
            );
        }

        if (visibleFeedParts.isEmpty()) {
            return driver.getPageSource();
        }

        Collections.sort(visibleFeedParts);
        return String.join("|", visibleFeedParts);
    }

    // ---- Feed journeys: share, fullscreen media, carousel ----

    private final By postMedia = AppiumBy.accessibilityId("feed.post.media");
    private final By viewerBackAction = AppiumBy.accessibilityId("immersive.back");
    private final By carouselCounter = AppiumBy.accessibilityId("feed.post.carouselCounter");

    public boolean tapFirstShareAction() {
        return tapFirstVisible(shareAction);
    }

    /** Centre of the visible part of the first post image that is clear of the top bar/strip
     * and the tab bar. Falls back to a point just above the first action row - the media sits
     * directly on top of it - while the image is still a shimmer placeholder with no element. */
    private Point firstPostMediaCenter() {
        Dimension screen = driver.manage().window().getSize();
        int minY = (int) (screen.getHeight() * 0.20);
        int maxY = (int) (screen.getHeight() * 0.85);
        for (WebElement media : visibleElements(postMedia)) {
            try {
                Rectangle rect = media.getRect();
                int top = Math.max(rect.getY(), minY);
                int bottom = Math.min(rect.getY() + rect.getHeight(), maxY);
                if (bottom - top > 60) {
                    return new Point(rect.getX() + rect.getWidth() / 2, (top + bottom) / 2);
                }
            } catch (StaleElementReferenceException ignored) {
                // Recycled mid-read; try the next one.
            }
        }
        WebElement save = firstVisibleAction(saveActionLocators());
        if (save == null) {
            return null;
        }
        int y = save.getRect().getY() - 80;
        if (y < minY) {
            return null;
        }
        return new Point(screen.getWidth() / 2, y);
    }

    public boolean tapFirstPostMedia() {
        Point center = null;
        for (int attempt = 0; attempt < 20 && center == null; attempt++) {
            center = firstPostMediaCenter();
            if (center == null) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        if (center == null) {
            return false;
        }
        BasePage.tapPoint(driver, center.getX(), center.getY());
        return true;
    }

    /** The immersive viewer is the only feed surface with its own Back control. */
    public boolean isPostFullscreenViewerOpen() {
        return !visibleElements(viewerBackAction).isEmpty();
    }

    public boolean waitForPostFullscreenViewer() {
        try {
            return new WebDriverWait(driver, SCROLL_TIMEOUT).ignoring(StaleElementReferenceException.class).until(ignored -> isPostFullscreenViewerOpen());
        } catch (RuntimeException timeout) {
            return false;
        }
    }

    public boolean closePostFullscreenViewer() {
        return tapFirstVisible(viewerBackAction);
    }

    /** Visible "n/m" carousel counter of the first multi-image post, scrolling for it. */
    public String findCarouselCounter(int scrollAttempts) {
        for (int attempt = 0; attempt <= scrollAttempts; attempt++) {
            String counter = visibleCarouselCounter();
            if (counter != null) {
                return counter;
            }
            swipeUp();
        }
        return null;
    }

    private String visibleCarouselCounter() {
        WebElement element = visibleCarouselCounterElement();
        return element == null ? null : counterText(element);
    }

    private static String counterText(WebElement element) {
        String text = element.getAttribute("label");
        if (text == null) {
            text = element.getText();
        }
        return text == null ? null : text.trim();
    }

    /** Swipes the carousel whose counter is on screen, first scrolling it up if its image
     * is still half under the tab bar. Returns false if no carousel is visible. */
    public boolean swipeVisibleCarousel(boolean toNext) {
        Dimension screen = driver.manage().window().getSize();
        for (int attempt = 0; attempt < 4; attempt++) {
            WebElement counter = visibleCarouselCounterElement();
            if (counter == null) {
                return false;
            }
            int y = counter.getRect().getY();
            if (y > screen.getHeight() / 2) {
                int from = screen.getHeight() * 3 / 4;
                dragFinger(screen.getWidth() / 2, from, screen.getWidth() / 2, from - (y - screen.getHeight() / 3), 600);
                continue;
            }
            int width = screen.getWidth();
            // The counter badge sits in the image's top-right corner; swipe across the image below it.
            int swipeY = y + 120;
            dragFinger(toNext ? width * 4 / 5 : width / 5, swipeY, toNext ? width / 5 : width * 4 / 5, swipeY, 400);
            return true;
        }
        return false;
    }

    private WebElement visibleCarouselCounterElement() {
        for (WebElement element : visibleElements(carouselCounter)) {
            try {
                String text = counterText(element);
                if (text != null && CAROUSEL_COUNTER.matcher(text).matches()) {
                    return element;
                }
            } catch (StaleElementReferenceException ignored) {
                // Re-rendered mid-read; try the next one.
            }
        }
        return null;
    }

    public boolean waitForCarouselCounterToChange(String previous) {
        try {
            return new WebDriverWait(driver, DEFAULT_TIMEOUT).ignoring(StaleElementReferenceException.class).until(ignored -> {
                String now = visibleCarouselCounter();
                return now != null && !now.equals(previous);
            });
        } catch (RuntimeException timeout) {
            return false;
        }
    }

    public boolean waitForCountsSignature(String expected) {
        try {
            return new WebDriverWait(driver, DEFAULT_TIMEOUT).ignoring(StaleElementReferenceException.class)
                    .until(ignored -> expected.equals(captureNumericCountsSignature()));
        } catch (RuntimeException timeout) {
            return false;
        }
    }

    private void dragFinger(int fromX, int fromY, int toX, int toY, int millis) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence gesture = new Sequence(finger, 1);
        gesture.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), fromX, fromY));
        gesture.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        gesture.addAction(finger.createPointerMove(Duration.ofMillis(millis), PointerInput.Origin.viewport(), toX, toY));
        gesture.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(gesture));
    }

    // ---- Feed journeys: follow a temple, filter author ----

    private final By feedFollowButton = AppiumBy.accessibilityId("feed.post.follow");
    // TempleProfileView ("temple.follow") / UserProfileScreen ("profile.follow") use one toggle
    // for both states; its label also carries the icon's, so the state is read with CONTAINS.
    private static final String PROFILE_TOGGLE = "name IN {'temple.follow', 'profile.follow'}";
    private final By profileFollowButton = AppiumBy.iOSNsPredicateString(
            PROFILE_TOGGLE + " AND label CONTAINS 'Follow' AND NOT (label CONTAINS 'Following')");
    private final By followingButton = AppiumBy.iOSNsPredicateString(
            PROFILE_TOGGLE + " AND label CONTAINS 'Following'");
    private final By unfollowConfirm = BasePage.byExactText("Unfollow");

    public boolean scrollToFollowButton(int scrollAttempts) {
        for (int attempt = 0; attempt <= scrollAttempts; attempt++) {
            if (!visibleElements(feedFollowButton).isEmpty()) {
                return true;
            }
            swipeUp();
        }
        return false;
    }

    /** Header author button on the same row as the given element, or null. */
    private WebElement authorOnRowOf(WebElement anchor) {
        Rectangle a = anchor.getRect();
        int rowCenter = a.getY() + a.getHeight() / 2;
        for (WebElement author : visibleElements(authorHeader)) {
            Rectangle r = author.getRect();
            if (Math.abs(r.getY() + r.getHeight() / 2 - rowCenter) < 30) {
                return author;
            }
        }
        return null;
    }

    /** Name of the post author on the same header row as the first visible Follow button. */
    public String authorBesideFollowButton() {
        List<WebElement> buttons = visibleElements(feedFollowButton);
        if (buttons.isEmpty()) {
            return null;
        }
        WebElement author = authorOnRowOf(buttons.get(0));
        return author == null ? null : displayNameFromLabel(author.getAttribute("label"));
    }

    public boolean tapFollow() {
        return tapFirstVisible(feedFollowButton);
    }

    /** True once no Follow button remains on the header row of the given author. */
    public boolean waitForFollowButtonGone(String author) {
        try {
            return new WebDriverWait(driver, SCROLL_TIMEOUT).ignoring(StaleElementReferenceException.class).until(ignored -> {
                for (WebElement button : visibleElements(feedFollowButton)) {
                    WebElement rowAuthor = authorOnRowOf(button);
                    String label = rowAuthor == null ? null : rowAuthor.getAttribute("label");
                    if (label != null && label.contains(author)) {
                        return false;
                    }
                }
                return true;
            });
        } catch (RuntimeException timeout) {
            return false;
        }
    }

    public boolean openAuthorProfile(String author) {
        for (WebElement header : visibleElements(authorHeader)) {
            try {
                String label = header.getAttribute("label");
                if (label != null && label.contains(author)) {
                    header.click();
                    return true;
                }
            } catch (RuntimeException ignored) {
                // Recycled mid-read; try the next one.
            }
        }
        return tapFirstVisible(BasePage.byExactText(author));
    }

    public boolean tapFollowing() {
        return tapFirstVisible(followingButton);
    }

    public boolean waitForFollowing() {
        return waitForVisible(followingButton);
    }

    public boolean waitForFollowButton() {
        return waitForVisible(profileFollowButton) || waitForVisible(feedFollowButton);
    }

    public boolean isUnfollowSheetVisible() {
        return waitForVisible(unfollowConfirm);
    }

    public boolean confirmUnfollow() {
        return tapFirstVisible(unfollowConfirm);
    }

    private boolean waitForVisible(By locator) {
        try {
            return new WebDriverWait(driver, SCROLL_TIMEOUT).ignoring(StaleElementReferenceException.class).until(ignored -> !visibleElements(locator).isEmpty());
        } catch (RuntimeException timeout) {
            return false;
        }
    }

    /** Display name of the topmost post whose header is on screen. */
    public String firstPostAuthor() {
        try {
            return readFirstPostAuthor();
        } catch (StaleElementReferenceException recomposed) {
            return null;
        }
    }

    private String readFirstPostAuthor() {
        WebElement best = null;
        for (WebElement element : visibleElements(authorHeader)) {
            if (element.getRect().getY() >= 0 && (best == null || element.getRect().getY() < best.getRect().getY())) {
                best = element;
            }
        }
        return best == null ? null : displayNameFromLabel(best.getAttribute("label"));
    }

    public boolean waitForFirstPostAuthorToChangeFrom(String previous) {
        try {
            return new WebDriverWait(driver, SCROLL_TIMEOUT).ignoring(StaleElementReferenceException.class).until(ignored -> {
                String now = firstPostAuthor();
                return now != null && !now.equals(previous);
            });
        } catch (RuntimeException timeout) {
            return false;
        }
    }

    public void swipeUp() {
        swipeVertical(0.75, 0.30);
    }

    public void swipeDown() {
        swipeVertical(0.30, 0.75);
    }

    private void swipeVertical(double startHeightPercent, double endHeightPercent) {
        Dimension size = driver.manage().window().getSize();
        int centerX = size.getWidth() / 2;
        int startY = (int) (size.getHeight() * startHeightPercent);
        int endY = (int) (size.getHeight() * endHeightPercent);
        dragFinger(centerX, startY, centerX, endY, 700);
    }

    public boolean didFeedScroll(String previousFeedState) {
        return new WebDriverWait(driver, SCROLL_TIMEOUT)
                .until(ignored -> !captureFeedState().equals(previousFeedState));
    }

    private List<By> feedItemLocators() {
        return List.of(
                namaskaraAction,
                commentAction,
                shareAction,
                saveAction,
                moreOptionsAction
        );
    }

    private List<WebElement> visibleElements(By locator) {
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
            try {
                return driver.findElements(locator).stream()
                        .filter(WebElement::isDisplayed)
                        .collect(Collectors.toList());
            } finally {
                driver.manage().timeouts().implicitlyWait(IMPLICIT_TIMEOUT);
            }
        } catch (RuntimeException exception) {
            return Collections.emptyList();
        }
    }

    private boolean tapFirstVisible(By... locators) {
        return tapFirstVisible(List.of(locators));
    }

    private boolean tapFirstVisible(List<By> locators) {
        try {
            WebElement element = new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ignored -> firstVisibleAction(locators));
            element.click();
            return true;
        } catch (RuntimeException exception) {
            for (By locator : locators) {
                for (WebElement element : visibleElements(locator)) {
                    try {
                        element.click();
                        return true;
                    } catch (RuntimeException ignored) {
                        // Keep scanning fallback locators.
                    }
                }
            }
            return false;
        }
    }

    /** True when a video row is currently audible - the toggle offers "mute". */
    public boolean isVideoAudible() {
        return findAnyAction(List.of(muteAction));
    }

    public boolean isVideoMuted() {
        return findAnyAction(List.of(unmuteAction));
    }

    public boolean isVolumeToggleVisible() {
        return findAnyAction(List.of(muteAction, unmuteAction));
    }

    /** Video rows are interleaved with image posts, and the toggle only shows on the focused
     * (playing) video, so the first screenful often has none - scroll for one first. */
    public boolean scrollUntilVolumeToggleVisible() {
        if (isVolumeToggleVisible()) {
            return true;
        }
        for (int attempt = 0; attempt < 6; attempt++) {
            swipeUp();
            if (isVolumeToggleVisible()) {
                return true;
            }
        }
        return false;
    }

    public boolean tapVolumeToggle() {
        WebElement toggle = firstVisibleAction(List.of(muteAction, unmuteAction));
        if (toggle == null) {
            return false;
        }
        BasePage.tapElementCenter(driver, toggle);
        return true;
    }

    /** Polls for the toggle to flip: the tap is dispatched to the player and the icon
     * re-renders a moment later, so a single check races it. */
    public boolean waitForVideoMuted() {
        return waitForActionGone(muteAction) && findAnyAction(List.of(unmuteAction));
    }

    public boolean waitForVideoAudible() {
        return waitForActionGone(unmuteAction) && findAnyAction(List.of(muteAction));
    }

    private boolean tapHomeIfVisible() {
        try {
            WebElement homeElement = firstVisibleAction(List.of(homeAction));
            if (homeElement != null) {
                homeElement.click();
                new WebDriverWait(driver, DEFAULT_TIMEOUT)
                        .until(ignored -> isAnyFeedItemVisible() || firstVisibleAction(List.of(createPostAction)) != null);
                return true;
            }
        } catch (RuntimeException ignored) {
            // The fallback swipes below can still recover if Home is already selected or temporarily unavailable.
        }
        return false;
    }

    private void waitForFeedItemInViewport() {
        if (searchFeedItemInViewport()) {
            return;
        }

        new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ignored -> isAnyFeedItemVisible());
    }

    private boolean searchFeedItemInViewport() {
        if (waitBrieflyForFeedItem()) {
            return true;
        }

        for (int attempt = 0; attempt < 4; attempt++) {
            swipeUp();
            if (waitBrieflyForFeedItem()) {
                return true;
            }
        }

        return false;
    }

    private boolean waitBrieflyForFeedItem() {
        try {
            return new WebDriverWait(driver, SCROLL_TIMEOUT)
                    .until(ignored -> isAnyFeedItemVisible());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Only Saved Posts asks to confirm an unsave; the Home feed toggles straight away. */
    private void dismissUnsaveDialogIfVisible() {
        try {
            if (!visibleElements(unsaveDialogAction).isEmpty()) {
                tapFirstVisible(cancelAction);
            }
        } catch (RuntimeException ignored) {
            // No confirmation shown.
        }
    }

    private void pressBackQuietly() {
        try {
            BasePage.navigateBack(driver);
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException ignored) {
            // If back is not available from the current state, the next recovery path can still run.
        }
    }

    private void tapFirstAction(List<By> locators) {
        WebElement action = firstVisibleAction(locators);
        for (int attempt = 0; action == null && attempt < 4; attempt++) {
            swipeUp();
            action = firstVisibleAction(locators);
        }
        if (action == null) {
            action = waitForFirstVisible(locators);
        }
        action.click();
        new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ignored -> isAnyFeedItemVisible());
    }

    /** Pull-to-refresh: a downward drag from near the top of the list. Slower than a
     * fling on purpose - a quick swipe scrolls the list instead of arming the refresh. */
    public void pullToRefreshFeed() {
        Dimension screen = driver.manage().window().getSize();
        int x = screen.getWidth() / 2;
        dragFinger(x, (int) (screen.getHeight() * 0.30), x, (int) (screen.getHeight() * 0.80), 800);

        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ignored -> isAnyFeedItemVisible());
        } catch (RuntimeException stillEmpty) {
            // Let the caller's assertion report the empty feed rather than throwing here.
        }
    }

    private boolean waitForActionGone(By locator) {
        try {
            return new WebDriverWait(driver, SCROLL_TIMEOUT)
                    .until(ignored -> visibleElements(locator).isEmpty());
        } catch (RuntimeException timedOut) {
            return false;
        }
    }

    private boolean findAnyAction(List<By> locators) {
        if (firstVisibleAction(locators) != null) {
            return true;
        }
        // "More options" only renders on someone else's post, and the QA account's feed has
        // accumulated many self-created posts from earlier runs, so allow generous scrolling.
        for (int attempt = 0; attempt < 10; attempt++) {
            swipeUp();
            if (firstVisibleAction(locators) != null) {
                return true;
            }
        }
        return false;
    }

    private WebElement firstVisibleAction(List<By> locators) {
        for (By locator : locators) {
            for (WebElement element : visibleElements(locator)) {
                return element;
            }
        }
        return null;
    }

    private WebElement waitForFirstVisible(By... locators) {
        return waitForFirstVisible(List.of(locators));
    }

    private WebElement waitForFirstVisible(List<By> locators) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .until(ignored -> firstVisibleAction(locators));
    }

    private int countVisibleFeedActions() {
        int visibleCount = 0;
        for (By locator : feedItemLocators()) {
            visibleCount += visibleElements(locator).size();
        }
        return visibleCount;
    }

    private List<By> likeActionLocators() {
        return List.of(namaskaraAction);
    }

    private List<By> commentActionLocators() {
        return List.of(commentAction);
    }

    private List<By> shareActionLocators() {
        return List.of(shareAction);
    }

    private List<By> namaskaraActionLocators() {
        return List.of(namaskaraAction);
    }

    private List<By> saveActionLocators() {
        return List.of(saveAction);
    }

    private List<By> moreOptionsActionLocators() {
        return List.of(moreOptionsAction);
    }

    private String elementState(WebElement element) {
        Point location = element.getLocation();
        Dimension size = element.getSize();
        return element.getAttribute("name") + ":"
                + element.getAttribute("label") + ":"
                + location.getX() + ","
                + location.getY() + ","
                + size.getWidth() + "x"
                + size.getHeight();
    }
}
