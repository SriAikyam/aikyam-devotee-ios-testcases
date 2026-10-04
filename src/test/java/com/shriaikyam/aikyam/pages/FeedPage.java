package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.connection.ConnectionStateBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FeedPage {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration SCROLL_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration IMPLICIT_TIMEOUT = Duration.ofSeconds(5);

    private final AndroidDriver driver;

    // Bottom-nav labels are plain Text nodes with no contentDescription, so an
    // accessibility-id locator never matches them; match on visible label text.
    private final By homeAction = AppiumBy.androidUIAutomator("new UiSelector().text(\"Home\")");
    private final By homeActionKn = AppiumBy.androidUIAutomator("new UiSelector().text(\"ಮುಖಪುಟ\")");
    // No "Like" control exists in this app - Namaskara IS the like action, and it is the
    // only one of the two ever present in the tree (confirmed live: every feed page source
    // carries contentDescription "Namaskara" and none carries "Like"). The English and
    // Kannada "Like" ids below matched nothing and are kept out of the locator lists.
    private final By commentAction = AppiumBy.accessibilityId("Comment");
    private final By commentActionKn = AppiumBy.accessibilityId("ಕಾಮೆಂಟ್");
    private final By shareAction = AppiumBy.accessibilityId("Share");
    private final By shareActionKn = AppiumBy.accessibilityId("ಹಂಚಿಕೊಳ್ಳಿ");
    private final By sendAction = AppiumBy.accessibilityId("Send");
    private final By sendActionKn = AppiumBy.accessibilityId("ಕಳುಹಿಸಿ");
    private final By namaskaraAction = AppiumBy.accessibilityId("Namaskara");
    private final By saveAction = AppiumBy.accessibilityId("Save");
    private final By saveActionKn = AppiumBy.accessibilityId("ಉಳಿಸಿ");
    private final By moreOptionsAction = AppiumBy.accessibilityId("More options");
    private final By moreOptionsActionKn = AppiumBy.accessibilityId("ಹೆಚ್ಚಿನ ಆಯ್ಕೆಗಳು");
    private final By profileAvatarAction = AppiumBy.accessibilityId("Profile");
    private final By collapseAction = AppiumBy.accessibilityId("Collapse");
    private final By exportAction = AppiumBy.accessibilityId("Export");
    private final By createPostAction = AppiumBy.accessibilityId("Create Post");
    private final By createPostTextAction = AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Create Post\")");
    private final By createTextAction = AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Create\")");
    private final By cancelAction = AppiumBy.androidUIAutomator("new UiSelector().text(\"Cancel\")");
    private final By cancelActionKn = AppiumBy.androidUIAutomator("new UiSelector().text(\"ರದ್ದುಮಾಡಿ\")");
    private final By unsaveDialogAction = AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Unsave Post\")");
    private final By unsaveDialogActionKn = AppiumBy.androidUIAutomator("new UiSelector().textContains(\"ಉಳಿಸದೇ\")");

    // Namaskara/Save icons never change their contentDescription on toggle (confirmed
    // via source read: tint is the only visual delta, which Appium/UiAutomator2 cannot
    // read from the accessibility tree) - the like-count text next to Namaskara
    // (FeedEngagementRow's `likeLabel`) is the only assertable real-outcome signal.
    // Matches any short numeric/compact-count text (e.g. "1", "12", "1.2K") anywhere
    // visible, not scoped to a specific post - a signature-diff (same pattern as
    // captureFeedState()/didFeedScroll()) is enough to prove a tap actually registered.
    private final By numericCountText = AppiumBy.androidUIAutomator(
            "new UiSelector().textMatches(\"^[0-9][0-9,.]*[KkMm]?$\")"
    );

    // Post Options sheet (opened via "More options") is a binary Delete/Report policy -
    // exactly one is ever shown, never both. Both are plain Text nodes.
    private final By postOptionsDeleteAction = AppiumBy.androidUIAutomator("new UiSelector().text(\"Delete\")");
    private final By postOptionsReportAction = AppiumBy.androidUIAutomator("new UiSelector().text(\"Report\")");
    private final By postOptionsTitle = AppiumBy.androidUIAutomator("new UiSelector().text(\"Options\")");

    // FeedSubscriberStrip - "All Posts" and "Temples" carry real contentDescriptions;
    // "Live" is the same filter tab LivePlayerPage already uses (a text label, not a
    // static content-desc, since it also appears elsewhere on live cards).
    private final By allPostsFilterTab = AppiumBy.accessibilityId("All Posts");
    private final By templesFilterTab = AppiumBy.accessibilityId("Temples");
    private final By liveFilterTab = AppiumBy.androidUIAutomator("new UiSelector().text(\"Live\")");

    // A video row's volume toggle. Its contentDescription names the action the tap would
    // perform, so it reports the CURRENT state inverted: "Mute" while audible, "Unmute"
    // while muted (FeedScreen.kt). That inversion makes it a genuine playback-state
    // signal - the only labelled video control in the feed. Play/pause and fullscreen
    // expose no label at all, so they cannot be driven by a locator.
    private final By muteAction = AppiumBy.accessibilityId("Mute");
    private final By unmuteAction = AppiumBy.accessibilityId("Unmute");

    public FeedPage(AndroidDriver driver) {
        this.driver = driver;
    }

    // Reads whichever appPackage BaseTest configured the session with (dev or prod,
    // controlled by -Dappium.appPackage) so back-press recovery guards below always
    // compare against the app actually under test, not a hardcoded flavor.
    private String appPackage() {
        Object appPackage = driver.getCapabilities().getCapability("appium:appPackage");
        return appPackage != null ? appPackage.toString() : "com.shriaikyam.aikyam.dev";
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

    /** Toggles airplane mode (and Wi-Fi/data with it) on or off. */
    public void setAirplaneMode(boolean enabled) {
        ConnectionStateBuilder builder = new ConnectionStateBuilder(driver.getConnection());
        if (enabled) {
            builder.withAirplaneModeEnabled().withWiFiDisabled().withDataDisabled();
        } else {
            builder.withAirplaneModeDisabled().withWiFiEnabled().withDataEnabled();
        }
        driver.setConnection(builder.build());
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
     * Presses back repeatedly across whatever screens that reaches (this can exit the
     * app entirely), then relaunches it to confirm it recovers rather than staying
     * crashed or corrupted.
     */
    public boolean isAppUsableAfterBackButtonSpam(int presses) {
        for (int i = 0; i < presses; i++) {
            try {
                driver.navigate().back();
            } catch (RuntimeException ignored) {
                break;
            }
        }
        driver.activateApp(appPackage());
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

    /** Taps the top-bar avatar, which navigates to the Settings screen hosting "Personal Profile". */
    public boolean openOwnProfile() {
        return tapFirstVisible(profileAvatarAction);
    }

    public boolean openCreatePost() {
        if (tapFirstVisible(createPostAction, createPostTextAction, createTextAction)) {
            return true;
        }

        // Confirmed live: a prior test can leave the app stranded deep in Settings/
        // Personal Profile/a single-post detail view (e.g. after ProfilePage's
        // deleteOpenPost() auto-advances to an adjacent post) - all pushed onto the Home
        // tab's own internal nav stack, so tapping the already-active Home tab does NOT
        // pop back to the Feed root (it's a no-op reselect, not a "go to tab root"
        // action). Unwind with real back-presses first so tapHomeIfVisible()'s tap
        // actually has a Feed root to land on.
        // isAnyFeedItemVisible() is NOT a safe stopping condition here - confirmed live,
        // the single-post detail viewer renders the exact same Like/Comment/Share/More
        // icons as a Feed row, so it reports "feed item visible" even when nowhere near
        // Feed, and the loop never presses back at all. profileAvatarAction is scoped
        // correctly (Feed's own top bar only, per ProfilePage's openProfile() javadoc).
        // Confirmed live (same failure mode already fixed once in ProfilePage): once
        // back-presses reach the Feed root, one more press exits the app entirely to the
        // Android home screen, after which nothing here ever matches again. Stop the
        // moment we've left the app's own package and relaunch it instead of pressing
        // back further.
        for (int attempt = 0; attempt < 6 && visibleElements(profileAvatarAction).isEmpty(); attempt++) {
            try {
                String currentPackage = driver.getCurrentPackage();
                if (currentPackage != null && !currentPackage.equals(appPackage())) {
                    driver.activateApp(appPackage());
                    break;
                }
                driver.navigate().back();
            } catch (RuntimeException ignored) {
                break;
            }
        }

        tapHomeIfVisible();
        for (int attempt = 0; attempt < 4; attempt++) {
            swipeDown();
            if (tapFirstVisible(createPostAction, createPostTextAction, createTextAction)) {
                return true;
            }
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

    /** A content-agnostic signature of every visible short numeric/compact-count text
     * on screen (e.g. Namaskara's like-count) - used to prove a tap actually changed
     * something real, not just that the feed didn't crash. Same diff pattern as
     * captureFeedState()/didFeedScroll(). */
    public String captureNumericCountsSignature() {
        List<String> counts = visibleElements(numericCountText).stream()
                .map(WebElement::getText)
                .filter(value -> value != null && !value.isBlank())
                .sorted()
                .collect(Collectors.toList());
        return String.join("|", counts);
    }

    /** Opens the Post Options sheet from the first available "More options" icon and
     * returns which single action it shows ("Delete" or "Report" - the policy is
     * binary, never both), or null if neither appeared. */
    public String openFirstPostOptionsSheet() {
        if (!findAnyAction(moreOptionsActionLocators())) {
            return null;
        }
        // Deliberately not tapFirstMoreOptionsAction() - that helper waits afterward for
        // isAnyFeedItemVisible(), which is correct for Like/Save (they stay on the feed)
        // but wrong here: the Post Options sheet covers the feed's action icons, so that
        // wait would time out even though the sheet opened successfully (confirmed live -
        // the sheet was visible on screen while this method still threw).
        WebElement moreOptions = firstVisibleAction(moreOptionsActionLocators());
        if (moreOptions == null) {
            return null;
        }
        // findAnyAction()'s own scroll-to-find can land the icon right at the very top
        // edge of the viewport (confirmed live: y~50px, against an ~83px status bar) -
        // a tap that close to system chrome was unreliable. Nudge it toward the middle
        // of the screen first.
        if (moreOptions.getLocation().getY() < 150) {
            swipeDown();
            moreOptions = firstVisibleAction(moreOptionsActionLocators());
            if (moreOptions == null) {
                return null;
            }
        }
        moreOptions.click();
        try {
            waitForFirstVisible(postOptionsTitle, postOptionsDeleteAction, postOptionsReportAction);
        } catch (RuntimeException exception) {
            return null;
        }
        if (!visibleElements(postOptionsDeleteAction).isEmpty()) {
            return "Delete";
        }
        if (!visibleElements(postOptionsReportAction).isEmpty()) {
            return "Report";
        }
        return null;
    }

    public boolean isPostOptionsSheetVisible() {
        return !visibleElements(postOptionsTitle).isEmpty()
                || !visibleElements(postOptionsDeleteAction).isEmpty()
                || !visibleElements(postOptionsReportAction).isEmpty();
    }

    /** Dismisses the sheet without acting on either option - real device back
     * pops Screen.PostOptions off the nav graph per source (no dedicated close icon). */
    public boolean dismissPostOptionsSheet() {
        try {
            driver.navigate().back();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** The subscriber strip is pinned to the very top of the feed - unlike
     * findAnyAction()'s scroll-forward strategy (built for "More options", which needs
     * to reach LATER posts), recovering this needs scrolling back UP first if an
     * earlier test left the feed scrolled down. */
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
        // Re-tapping the already-selected Home tab is a common "scroll to top" gesture
        // in this app's own recovery (see tapHomeIfVisible()'s use elsewhere) - try it
        // before falling back to repeated swipes, which alone were not enough to climb
        // back out of a long video reel scrolled deep into the feed (confirmed live).
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

    // Double-tap-to-like was investigated and deliberately dropped: a hand-rolled W3C
    // Actions sequence is rejected outright by this UiAutomator2 version ("Unable to
    // perform W3C actions ... make sure your input actions chain is valid"), and the
    // driver's own "mobile: doubleClickGesture" command executes without error but
    // isn't fast enough to register as a genuine double-tap to the app's own gesture
    // detector - it falls through to two separate single-taps instead (confirmed live:
    // opened the fullscreen viewer, then toggled its sound icon). No tunable timing
    // parameter is exposed on that command to fix this - not reliably automatable with
    // the current driver/device combination.

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

    private final By viewerBackAction = AppiumBy.accessibilityId("Back");
    private final By slashText = AppiumBy.androidUIAutomator("new UiSelector().textContains(\"/\")");
    private static final java.util.regex.Pattern CAROUSEL_COUNTER = java.util.regex.Pattern.compile("\\d+/\\d+");

    public boolean tapFirstShareAction() {
        return tapFirstVisible(shareAction, shareActionKn);
    }

    /** The post's media is an unlabelled, non-clickable view that sits directly above its
     * action row, so a point just above the first Save icon is inside it. (An earlier version
     * measured from the header's More options button, which a user's own post does not show.) */
    private Point firstPostMediaCenter() {
        WebElement save = firstVisibleAction(saveActionLocators());
        if (save == null) {
            return null;
        }
        Dimension screen = driver.manage().window().getSize();
        int y = save.getRect().getY() - 300;
        if (y < screen.getHeight() / 4) {
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
        dragFinger(center.getX(), center.getY(), center.getX(), center.getY(), 50);
        return true;
    }

    /** Fullscreen viewer is the only feed surface that exposes a Back control. */
    public boolean isPostFullscreenViewerOpen() {
        return !visibleElements(viewerBackAction).isEmpty();
    }

    /** The viewer opens after the tap-vs-double-tap delay, so poll instead of checking once. */
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
        for (WebElement element : visibleElements(slashText)) {
            String text = element.getText();
            if (text != null && CAROUSEL_COUNTER.matcher(text.trim()).matches()) {
                return text.trim();
            }
        }
        return null;
    }

    /** Swipes the carousel whose counter is on screen, first scrolling it up if its image
     * is still half under the bottom navigation. Returns false if no carousel is visible. */
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
            int swipeY = y + 300;
            dragFinger(toNext ? width * 4 / 5 : width / 5, swipeY, toNext ? width / 5 : width * 4 / 5, swipeY, 400);
            return true;
        }
        return false;
    }

    private WebElement visibleCarouselCounterElement() {
        for (WebElement element : visibleElements(slashText)) {
            String text = element.getText();
            if (text != null && CAROUSEL_COUNTER.matcher(text.trim()).matches()) {
                return element;
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

    private final By followButton = AppiumBy.androidUIAutomator("new UiSelector().text(\"Follow\")");
    private final By followingButton = AppiumBy.androidUIAutomator("new UiSelector().text(\"Following\")");
    private final By unfollowConfirm = AppiumBy.androidUIAutomator("new UiSelector().text(\"Unfollow\")");
    private final By anyTextView = AppiumBy.className("android.widget.TextView");

    public boolean scrollToFollowButton(int scrollAttempts) {
        for (int attempt = 0; attempt <= scrollAttempts; attempt++) {
            if (!visibleElements(followButton).isEmpty()) {
                return true;
            }
            swipeUp();
        }
        return false;
    }

    /** Name of the post author on the same header row as the first visible Follow button. */
    public String authorBesideFollowButton() {
        List<WebElement> buttons = visibleElements(followButton);
        if (buttons.isEmpty()) {
            return null;
        }
        int rowY = buttons.get(0).getRect().getY();
        WebElement name = null;
        for (WebElement element : visibleElements(anyTextView)) {
            Rectangle rect = element.getRect();
            String text = element.getText();
            if (text == null || text.isBlank() || text.startsWith("@") || "Follow".equals(text)) {
                continue;
            }
            if (Math.abs(rect.getY() - rowY) < 120 && rect.getX() < 700
                    && (name == null || rect.getY() < name.getRect().getY())) {
                name = element;
            }
        }
        return name == null ? null : name.getText().trim();
    }

    public boolean tapFollow() {
        return tapFirstVisible(followButton);
    }

    /** True once no Follow button remains on the header row of the given author. */
    public boolean waitForFollowButtonGone(String author) {
        try {
            return new WebDriverWait(driver, SCROLL_TIMEOUT).ignoring(StaleElementReferenceException.class).until(ignored -> {
                for (WebElement button : visibleElements(followButton)) {
                    int rowY = button.getRect().getY();
                    for (WebElement element : visibleElements(AppiumBy.androidUIAutomator(
                            "new UiSelector().text(\"" + author + "\")"))) {
                        if (Math.abs(element.getRect().getY() - rowY) < 120) {
                            return false;
                        }
                    }
                }
                return true;
            });
        } catch (RuntimeException timeout) {
            return false;
        }
    }

    public boolean openAuthorProfile(String author) {
        return tapFirstVisible(AppiumBy.androidUIAutomator("new UiSelector().text(\"" + author + "\")"));
    }

    public boolean tapFollowing() {
        return tapFirstVisible(followingButton);
    }

    public boolean waitForFollowing() {
        return waitForVisible(followingButton);
    }

    public boolean waitForFollowButton() {
        return waitForVisible(followButton);
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

    /** Author name of the first post, read from the header band just under the filter strip
     * (a fraction of the screen height, so it does not depend on the device's resolution). */
    public String firstPostAuthor() {
        try {
            return readFirstPostAuthor();
        } catch (StaleElementReferenceException recomposed) {
            return null;
        }
    }

    private String readFirstPostAuthor() {
        int height = driver.manage().window().getSize().getHeight();
        WebElement best = null;
        for (WebElement element : visibleElements(anyTextView)) {
            Rectangle rect = element.getRect();
            String text = element.getText();
            if (rect.getY() > height * 0.21 && rect.getY() < height * 0.27 && rect.getX() < 700
                    && text != null && !text.isBlank() && (best == null || rect.getY() < best.getRect().getY())) {
                best = element;
            }
        }
        return best == null ? null : best.getText().trim();
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

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(700), PointerInput.Origin.viewport(), centerX, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(swipe));
    }

    public boolean didFeedScroll(String previousFeedState) {
        return new WebDriverWait(driver, SCROLL_TIMEOUT)
                .until(ignored -> !captureFeedState().equals(previousFeedState));
    }

    private List<By> feedItemLocators() {
        return List.of(
                namaskaraAction,
                commentAction,
                commentActionKn,
                shareAction,
                shareActionKn,
                sendAction,
                sendActionKn,
                namaskaraAction,
                saveAction,
                saveActionKn,
                moreOptionsAction,
                moreOptionsActionKn,
                collapseAction,
                exportAction
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

    /** True when a video row is currently audible - the toggle offers "Mute", meaning
     * tapping it would mute. */
    public boolean isVideoAudible() {
        return findAnyAction(List.of(muteAction));
    }

    public boolean isVideoMuted() {
        return findAnyAction(List.of(unmuteAction));
    }

    public boolean isVolumeToggleVisible() {
        return findAnyAction(List.of(muteAction, unmuteAction));
    }

    /** Video rows are interleaved with image posts, so the first screenful often has no
     * volume toggle at all - scroll for one before deciding it is absent. */
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

    /** Real touch on the toggle's clickable ancestor, not click() on the icon. The icon is
     * an Icon inside an IconButton, so the icon node itself reports clickable=false and an
     * accessibility click on it is not reliably routed to the handler - the same dispatch
     * problem that silently broke the bottom-nav taps. */
    public boolean tapVolumeToggle() {
        WebElement toggle = firstVisibleAction(List.of(muteAction, unmuteAction));
        if (toggle == null) {
            return false;
        }
        BasePage.tapElementCenter(driver, BasePage.clickableAncestorOf(toggle));
        return true;
    }

    /** Polls for the toggle to flip: the tap is dispatched to the player and the icon
     * recomposes a moment later, so a single check races it. */
    public boolean waitForVideoMuted() {
        return waitForActionGone(muteAction) && findAnyAction(List.of(unmuteAction));
    }

    public boolean waitForVideoAudible() {
        return waitForActionGone(unmuteAction) && findAnyAction(List.of(muteAction));
    }

    private boolean tapHomeIfVisible() {
        try {
            WebElement homeElement = firstVisibleAction(homeActionLocators());
            if (homeElement != null) {
                // Real touch on the clickable nav item, not click() on its label - see
                // BasePage.clickableAncestorOf. Confirmed live: TC090 sat on a temple
                // profile left over from the previous class while this recovery path
                // silently did nothing. Same fix as HomePage.tap.
                BasePage.tapElementCenter(driver, BasePage.clickableAncestorOf(homeElement));
                new WebDriverWait(driver, DEFAULT_TIMEOUT)
                        .until(ignored -> isAnyFeedItemVisible() || firstVisibleAction(createPostLocators()) != null);
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

    private void dismissUnsaveDialogIfVisible() {
        try {
            if (firstVisibleAction(unsaveDialogLocators()) != null) {
                tapFirstVisible(cancelActionLocators());
            }
        } catch (RuntimeException ignored) {
            // The save action may not show a confirmation dialog when saving an unsaved post.
        }
    }

    private void pressBackQuietly() {
        try {
            driver.navigate().back();
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
        int from = (int) (screen.getHeight() * 0.30);
        int to = (int) (screen.getHeight() * 0.80);

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence drag = new Sequence(finger, 1);
        drag.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, from));
        drag.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        drag.addAction(finger.createPointerMove(Duration.ofMillis(800), PointerInput.Origin.viewport(), x, to));
        drag.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(drag));

        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ignored -> isAnyFeedItemVisible());
        } catch (RuntimeException stillEmpty) {
            // Let the caller's assertion report the empty feed rather than throwing here.
        }
    }

    /** Polls until the locator stops matching. FeedPage predates BasePage and carries its
     * own helpers, so it has no waitUntilGone of its own. */
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
        // "More options" only renders on someone else's post (own posts hide it by design -
        // see FeedPostUiModel.showOptions), and the QA account's feed has accumulated many
        // self-created posts from earlier test runs, so this needs more scroll headroom than
        // the other action checks to reach a non-own post.
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

    private List<By> homeActionLocators() {
        return List.of(homeAction, homeActionKn);
    }

    private List<By> createPostLocators() {
        return List.of(createPostAction, createPostTextAction, createTextAction);
    }

    private List<By> cancelActionLocators() {
        return List.of(cancelAction, cancelActionKn);
    }

    private List<By> unsaveDialogLocators() {
        return List.of(unsaveDialogAction, unsaveDialogActionKn);
    }

    private List<By> likeActionLocators() {
        return List.of(namaskaraAction);
    }

    private List<By> commentActionLocators() {
        return List.of(commentAction, commentActionKn);
    }

    private List<By> shareActionLocators() {
        return List.of(shareAction, shareActionKn, sendAction, sendActionKn);
    }

    private List<By> namaskaraActionLocators() {
        return List.of(namaskaraAction);
    }

    private List<By> saveActionLocators() {
        return List.of(saveAction, saveActionKn);
    }

    private List<By> moreOptionsActionLocators() {
        return List.of(moreOptionsAction, moreOptionsActionKn);
    }

    private String elementState(WebElement element) {
        Point location = element.getLocation();
        Dimension size = element.getSize();
        return element.getText() + ":"
                + element.getAttribute("content-desc") + ":"
                + location.getX() + ","
                + location.getY() + ","
                + size.getWidth() + "x"
                + size.getHeight();
    }
}
