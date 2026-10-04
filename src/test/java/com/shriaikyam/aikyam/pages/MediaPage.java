package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class MediaPage extends BasePage {

    private static final Duration SCROLL_TIMEOUT = Duration.ofSeconds(10);
    private static final int VIDEO_SEARCH_SCROLL_ATTEMPTS = 5;

    // VideoDurationBadge's play-icon fallback is labelled "Video"; the inline feed player's
    // mute toggle (Core/Media/VideoPlayerView) reads "Mute"/"Unmute".
    private final By videoIndicator = accessibilityId("Video");
    private final By videoThumbnail = id("feed.post.video");
    private final By playAction = accessibilityId("Play");
    private final By muteAction = accessibilityId("Mute");
    private final By unmuteAction = accessibilityId("Unmute");
    private final By homeBottomNav = id("tab.home");
    // FeedPostRow's engagement buttons are icon-only; until they carry identifiers, the
    // button label falls back to the asset name of its FeedTemplateIcon (plus any count).
    private final By saveAction = id("feed.post.save");
    private final By saveIcon = feedButtonStartingWith("bookmark");
    private final By commentAction = id("feed.post.comment");
    private final By commentIcon = feedButtonStartingWith("comment");
    private final By namaskaraAction = id("feed.post.namaskara");
    private final By namaskaraIcon = feedButtonStartingWith("Namaskara3");

    public MediaPage(IOSDriver driver) {
        super(driver);
    }

    private static By feedButtonStartingWith(String assetName) {
        String v = predicateLiteral(assetName);
        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND (label BEGINSWITH " + v + " OR name BEGINSWITH " + v + ")");
    }

    public boolean isVideoVisible() {
        return findVideoInFeed();
    }

    public boolean startVideoPlayback() {
        if (!findVideoInFeed()) {
            return false;
        }
        if (isAnyVideoLocatorVisible(muteAction, unmuteAction, videoThumbnail, textContains("Processing video"))) {
            return true;
        }
        return tapFirstVisible(playAction, videoThumbnail);
    }

    public boolean isMediaContentAvailable() {
        return isAnyVideoLocatorVisible(videoLocators()) || isMediaFeedUsable();
    }

    public boolean isPlaybackControlOrStateVisible() {
        return findVideoInFeed() && isAnyVideoLocatorVisible(videoLocators());
    }

    public boolean isMediaStateCaptured() {
        try {
            String state = captureMediaState();
            return state != null && !state.isBlank();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isMediaFeedUsable() {
        return isAnyVideoLocatorVisible(homeBottomNav, saveAction, saveIcon, commentAction, commentIcon,
                namaskaraAction, namaskaraIcon);
    }

    public boolean isHomeNavigationVisible() {
        return isAnyVideoLocatorVisible(homeBottomNav);
    }

    public boolean isSaveActionVisible() {
        return isAnyVideoLocatorVisible(saveAction, saveIcon);
    }

    public boolean isCommentActionVisible() {
        return isAnyVideoLocatorVisible(commentAction, commentIcon);
    }

    public boolean isNamaskaraActionVisible() {
        return isAnyVideoLocatorVisible(namaskaraAction, namaskaraIcon);
    }

    public boolean isMediaAvailableAfterScroll() {
        scrollMediaFeed();
        return isMediaContentAvailable();
    }

    public boolean isMediaFeedUsableAfterPlaybackAttempt() {
        startVideoPlayback();
        return isMediaFeedUsable() || isAnyVideoLocatorVisible(videoLocators());
    }

    public boolean isMediaFeedUsableAfterMultipleScrolls(int count) {
        for (int index = 0; index < count; index++) {
            try {
                scrollMediaFeed();
            } catch (RuntimeException exception) {
                return false;
            }
        }
        return isMediaFeedUsable() || isAnyVideoLocatorVisible(videoLocators());
    }

    public boolean isVideoStateRecoverableAfterScroll() {
        boolean hadVideo = findVideoInFeed();
        boolean wasFeedUsable = isMediaFeedUsable();

        if (!hadVideo) {
            return wasFeedUsable;
        }

        try {
            scrollMediaFeed();
        } catch (RuntimeException exception) {
            return wasFeedUsable;
        }

        return isMediaFeedUsable() || isAnyVideoLocatorVisible(videoLocators()) || isMediaStateCaptured();
    }

    public boolean isVideoStateStillRecognizedAfterSecondLookup() {
        String firstState = captureMediaState();
        String secondState = captureMediaState();
        return firstState != null && !firstState.isBlank()
                && secondState != null && !secondState.isBlank()
                && (isMediaFeedUsable() || firstState.contains("Video") || secondState.contains("Video"));
    }

    public String captureMediaState() {
        return driver.getPageSource();
    }

    public void scrollMediaFeed() {
        swipeUp();
    }

    public boolean didMediaScroll(String previousState) {
        return new WebDriverWait(driver, SCROLL_TIMEOUT)
                .until(ignored -> !driver.getPageSource().equals(previousState));
    }

    private boolean findVideoInFeed() {
        for (int attempt = 0; attempt <= VIDEO_SEARCH_SCROLL_ATTEMPTS; attempt++) {
            if (isAnyVideoLocatorVisible(videoLocators())) {
                return true;
            }
            if (attempt < VIDEO_SEARCH_SCROLL_ATTEMPTS) {
                try {
                    swipeUp();
                } catch (RuntimeException exception) {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean isAnyVideoLocatorVisible(List<By> locators) {
        try {
            return locators.stream().anyMatch(locator -> !visibleElements(locator).isEmpty());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean isAnyVideoLocatorVisible(By... locators) {
        return isAnyVideoLocatorVisible(List.of(locators));
    }

    private List<By> videoLocators() {
        return List.of(
                videoIndicator,
                videoThumbnail,
                playAction,
                muteAction,
                unmuteAction,
                textContains("Processing video")
        );
    }
}
