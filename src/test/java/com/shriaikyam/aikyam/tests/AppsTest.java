package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.AppsPage;
import com.shriaikyam.aikyam.pages.HomePage;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.shriaikyam.aikyam.categories.P0;

public class AppsTest extends BaseTest {

    /** Land on the Apps screen itself before each case. A mini-app left open by an
     * earlier case is a full-screen push that re-tapping the Apps tab does not pop, so
     * back out first, then enter from Home. */
    @Before
    public void returnToAppsScreen() {
        AppsPage appsPage = new AppsPage(driver);
        HomePage homePage = new HomePage(driver);

        for (int attempt = 0; attempt < 4 && appsPage.isMiniAppWebViewVisible(); attempt++) {
            appsPage.navigateBack();
        }
        homePage.tapHomeTab();
        homePage.tapAppsTab();
        appsPage.waitUntilLoaded();
    }

    @Category(P0.class)
    @Test
    public void TC081_Verify_Back_Navigation_Returns_To_Apps_From_Stotragalu() {
        HomePage homePage = new HomePage(driver);
        AppsPage appsPage = new AppsPage(driver);

        homePage.tapAppsTab();
        appsPage.waitUntilLoaded();
        appsPage.openStotragalu();
        Assert.assertTrue("Stotragalu should open a mini-app WebView", appsPage.waitForMiniAppWebViewToLoad());

        Assert.assertTrue("Back should be dispatchable from the mini-app", appsPage.navigateBack());
        Assert.assertTrue("Apps screen should be visible again after navigating back", appsPage.isAppsScreenVisible());
    }

    @Category(P0.class)
    @Test
    public void TC149_Verify_Both_Mini_Apps_Launch_In_Sequence() {
        HomePage homePage = new HomePage(driver);
        AppsPage appsPage = new AppsPage(driver);

        homePage.tapAppsTab();
        appsPage.waitUntilLoaded();

        Assert.assertTrue("Stotragalu should be tappable", appsPage.openStotragalu());
        Assert.assertTrue("Stotragalu should open a mini-app WebView",
                appsPage.waitForMiniAppWebViewToLoad());
        Assert.assertTrue("Back should return to Apps", appsPage.navigateBack());
        Assert.assertTrue("Apps should be visible between launches", appsPage.isAppsScreenVisible());

        // The second launch is the point: a mini-app host that keeps the first app's state
        // would fail to load the second, and one launch alone would never show it.
        Assert.assertTrue("Panchang should be tappable after Stotragalu", appsPage.openPanchang());
        Assert.assertTrue("Panchang should open its own WebView after Stotragalu",
                appsPage.waitForMiniAppWebViewToLoad());

        Assert.assertTrue("Back should return to Apps again", appsPage.navigateBack());
        Assert.assertTrue("Apps should be visible after the second mini-app",
                appsPage.isAppsScreenVisible());
    }
}
