package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.Collections;

public class AppsPage extends BasePage {

    // AppsView: the two live hero cards and the static "coming soon" catalogue.
    private final By stotragaluCard = id("apps.card.stotragalu");
    private final By panchangCard = id("apps.card.panchang");
    private final By exploreMoreDivider = id("apps.divider");
    // Every ComingSoonRow shares one id; the row is a single combined element.
    private final By comingSoonRow = id("apps.comingsoon");
    // MiniAppScreen hosts a WKWebView, surfaced by XCUITest as XCUIElementTypeWebView.
    private final By webView = AppiumBy.iOSClassChain("**/XCUIElementTypeWebView");
    private final By webViewById = id("miniapp.webview");

    public AppsPage(IOSDriver driver) {
        super(driver);
    }

    public void waitUntilLoaded() {
        waitForAnyVisible(stotragaluCard, panchangCard);
    }

    public boolean isAppsScreenVisible() {
        return isAnyVisible(stotragaluCard) && isAnyVisible(panchangCard);
    }

    public boolean isExploreMoreDividerVisible() {
        return isAnyVisible(exploreMoreDivider);
    }

    public boolean isComingSoonSectionVisible() {
        return isAnyVisible(comingSoonRow);
    }

    public boolean openStotragalu() {
        return tapFirstVisible(stotragaluCard);
    }

    public boolean openPanchang() {
        return tapFirstVisible(panchangCard);
    }

    public boolean waitForMiniAppWebViewToLoad() {
        try {
            waitForAnyVisible(webView, webViewById);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** ComingSoonRow (AppsView.swift) has no tap action at all, so tapping one must be
     * inert. Named here so a test can drive it and assert nothing happened, rather than
     * assuming. "Spiritual Books" is the first row in AppsView's catalogue. */
    public boolean isComingSoonCardVisible() {
        return isAnyVisible(comingSoonRow);
    }

    public boolean tapComingSoonCard() {
        return tapFirstVisible(comingSoonRow);
    }

    public boolean isMiniAppWebViewVisible() {
        return isVisibleNow(webView, webViewById);
    }

    public boolean isStotragaluVisible() {
        return isAnyVisible(stotragaluCard);
    }

    public boolean isPanchangVisible() {
        return isAnyVisible(panchangCard);
    }

    /** MiniAppScreen hides the navigation bar and has no native back control; the root
     * stack re-enables the left-edge swipe-back (aikyamEnableSwipeBack), and Stotragalu's
     * own web header also has a "Back" button that exits through the native bridge. Try
     * the edge swipe first, then BasePage's back-control lookup. */
    @Override
    public boolean navigateBack() {
        if (!isMiniAppWebViewVisible()) {
            return super.navigateBack();
        }
        swipeFromLeftEdge();
        if (waitUntilGone(DEFAULT_TIMEOUT, webView, webViewById)) {
            return true;
        }
        super.navigateBack();
        return waitUntilGone(DEFAULT_TIMEOUT, webView, webViewById);
    }

    private void swipeFromLeftEdge() {
        Dimension size = driver.manage().window().getSize();
        int y = size.getHeight() / 2;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 2, y));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(400), PointerInput.Origin.viewport(),
                (int) (size.getWidth() * 0.75), y));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(swipe));
    }
}
