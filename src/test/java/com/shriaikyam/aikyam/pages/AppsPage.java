package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;

public class AppsPage extends BasePage {

    // Unlike Music, both live feature cards and every "coming soon" card render fixed
    // labels straight from AppsScreen's own catalogue (not backend-driven), so this
    // screen's static content can be matched directly by text.
    private final By stotragaluCard = text("Stotragalu");
    private final By panchangCard = text("Panchang");
    private final By exploreMoreDivider = text("Explore more (Coming soon)");
    private final By comingSoonBadge = text("Coming soon");
    private final By webView = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.webkit.WebView\")"
    );

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
        return isAnyVisible(comingSoonBadge);
    }

    public boolean openStotragalu() {
        return tapFirstVisible(stotragaluCard);
    }

    public boolean openPanchang() {
        return tapFirstVisible(panchangCard);
    }

    public boolean waitForMiniAppWebViewToLoad() {
        try {
            waitForAnyVisible(webView);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** ComingSoonFeatureCard (AppsScreen.kt) takes no onTap and carries no clickable
     * modifier at all, so tapping one must be inert. Named here so a test can drive it
     * and assert nothing happened, rather than assuming. */
    private final By spiritualBooksCard = text("Spiritual Books");

    public boolean isComingSoonCardVisible() {
        return isAnyVisible(spiritualBooksCard);
    }

    public boolean tapComingSoonCard() {
        return tapFirstVisible(spiritualBooksCard);
    }

    public boolean isMiniAppWebViewVisible() {
        return isAnyVisible(webView);
    }

    public boolean isStotragaluVisible() {
        return isAnyVisible(stotragaluCard);
    }

    public boolean isPanchangVisible() {
        return isAnyVisible(panchangCard);
    }

    public boolean navigateBack() {
        try {
            driver.navigate().back();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
