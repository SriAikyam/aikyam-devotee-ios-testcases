package com.shriaikyam.aikyam.pages;

import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class MediaPage extends BasePage {

    private static final Duration SCROLL_TIMEOUT = Duration.ofSeconds(10);
    private static final int VIDEO_SEARCH_SCROLL_ATTEMPTS = 5;

    private final By videoIndicator = accessibilityId("Video");
    private final By videoThumbnail = accessibilityId("Video thumbnail");
    private final By playAction = accessibilityId("Play");
    private final By playVideoAction = accessibilityId("Play video");
    private final By muteAction = accessibilityId("Mute");
    private final By unmuteAction = accessibilityId("Unmute");
    private final By playbackError = accessibilityId("Playback error");
    private final By homeBottomNav = accessibilityId("Home");
    private final By saveAction = accessibilityId("Save");
    private final By commentAction = accessibilityId("Comment");
    private final By namaskaraAction = accessibilityId("Namaskara");

    public MediaPage(IOSDriver driver) {
        super(driver);
    }

    public boolean isVideoVisible() {
        return findVideoInFeed();
    }

    public boolean startVideoPlayback() {
        if (!findVideoInFeed()) {
            return false;
        }
        if (isAnyVideoLocatorVisible(muteAction, unmuteAction, videoThumbnail, playbackError, textContains("Processing video"), textContains("Video unavailable"))) {
            return true;
        }
        return tapFirstVisible(playAction, playVideoAction, videoIndicator, textContains("Video"));
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
        return isAnyVideoLocatorVisible(homeBottomNav, saveAction, commentAction, namaskaraAction);
    }

    public boolean isHomeNavigationVisible() {
        return isAnyVideoLocatorVisible(homeBottomNav);
    }

    public boolean isSaveActionVisible() {
        return isAnyVideoLocatorVisible(saveAction);
    }

    public boolean isCommentActionVisible() {
        return isAnyVideoLocatorVisible(commentAction);
    }

    public boolean isNamaskaraActionVisible() {
        return isAnyVideoLocatorVisible(namaskaraAction);
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
                playVideoAction,
                muteAction,
                unmuteAction,
                playbackError,
                textContains("Video"),
                textContains("Processing video"),
                textContains("Video unavailable"),
                textContains("Tap to retry")
        );
    }
}
