package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.FeedPage;
import com.shriaikyam.aikyam.pages.HomePage;
import org.junit.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.shriaikyam.aikyam.categories.P0;

public class FeedTest extends BaseTest {

    /* ---- Phase 3 pilot: THIS CLASS ONLY. FeedTest is read-mostly, has no
       @FixMethodOrder and performs no authentication or account mutation, so a shared
       session cannot carry auth state or ordered data between tests.
       BaseTest.resetToHome() still runs before EVERY test - only the session is reused. */
    @Override
    protected boolean reuseDriverAcrossTests() {
        return true;
    }

    /** Scopes the shared session to this class; without it the session would leak into
     * whichever class runs next. */
    @AfterClass
    public static void quitSharedDriver() {
        releaseSharedDriver();
    }

    /** Land on the Home feed before each case. FeedPage.waitUntilLoaded has its own
     * back-press recovery, but it could not pop a Profile screen left behind by an
     * earlier class - confirmed live: six cases failed in one batch with the profile in
     * the page source, TC004 among them. HomePage.tapHomeTab goes through
     * ensureBottomNavigationReachable and the real-touch nav fix, which is the path that
     * actually works (HomeTest 20/21). BaseTest's own @Before builds the driver first,
     * superclass before subclass. */
    @Before
    public void returnToFeed() {
        new HomePage(driver).tapHomeTab();
        new FeedPage(driver).waitUntilLoaded();
    }

    @Category(P0.class)
    @Test
    public void TC002_Verify_Feed_Load() {
        FeedPage feedPage = new FeedPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue(
                "At least one feed item should be visible",
                feedPage.isAnyFeedItemVisible()
        );
    }

    @Category(P0.class)
    @Test
    public void TC003_Verify_Feed_Scroll() {
        FeedPage feedPage = new FeedPage(driver);

        feedPage.waitUntilLoaded();
        String initialFeedState = feedPage.captureFeedState();
        feedPage.swipeUp();

        Assert.assertTrue(
                "Feed should scroll after swipe up",
                feedPage.didFeedScroll(initialFeedState)
        );
    }

    // TC004_Verify_Like_Action_Visible removed: Namaskara IS this app's like action, so it
    // was TC007 under another name, driving the same node through the same locator list.
    // No "Like" contentDescription exists in the build at all.

    @Category(P0.class)
    @Test
    public void TC005_Verify_Comment_Action_Visible() {
        FeedPage feedPage = new FeedPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue("Comment action should be visible", feedPage.isCommentActionVisible());
    }

    @Category(P0.class)
    @Test
    public void TC006_Verify_Share_Action_Visible() {
        FeedPage feedPage = new FeedPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue("Share action should be visible", feedPage.isShareActionVisible());
    }

    @Category(P0.class)
    @Test
    public void TC007_Verify_Namaskara_Action_Visible() {
        FeedPage feedPage = new FeedPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue("Namaskara action should be visible", feedPage.isNamaskaraActionVisible());
    }

    @Category(P0.class)
    @Test
    public void TC008_Verify_Save_Action_Visible() {
        FeedPage feedPage = new FeedPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue("Save action should be visible", feedPage.isSaveActionVisible());
    }

    // TC015 (Save tap, "feed did not vanish") removed: it saved the first post on every run
    // and never undid it. FeedJourneyTest.TC180 checks Save end to end and restores the account.
    @Test
    public void TC060_Verify_Feed_Remains_Usable_At_Pagination_End() {
        FeedPage feedPage = new FeedPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue(
                "Feed should stay usable after scrolling through many pages, not crash or blank out",
                feedPage.isFeedUsableAfterExtensiveScrolling(20)
        );
    }

    @Test
    public void TC061_Verify_Feed_Stable_After_Rapid_Like_Namaskara_Taps() {
        FeedPage feedPage = new FeedPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue(
                "Feed should remain visible and stable after rapid repeated Like/Namaskara taps",
                feedPage.isFeedStableAfterRapidLikeNamaskaraTaps(8)
        );
    }

    /** The recovery half of TC062. Surviving an outage is only half the guarantee - a feed
     * that stays empty or stuck after the network returns is still broken, and TC062
     * cannot see that because it only asserts the offline state. Pulls to refresh once
     * connectivity is back rather than waiting on whatever background retry may exist, so
     * the assertion is about the feed reloading, not about retry timing. */
    @Test
    public void TC151_Verify_Feed_Recovers_After_Network_Restored() {
        FeedPage feedPage = new FeedPage(driver);

        feedPage.waitUntilLoaded();
        feedPage.setAirplaneMode(true);
        try {
            Assert.assertTrue("App should stay responsive while offline",
                    feedPage.isAppResponsiveOffline());
        } finally {
            feedPage.setAirplaneMode(false);
        }

        feedPage.pullToRefreshFeed();
        Assert.assertTrue("Feed should show real content again once the network is back",
                feedPage.isAnyFeedItemVisible());
        Assert.assertTrue("Feed actions should work again after recovery, not just render",
                feedPage.isNamaskaraActionVisible());
    }

    /** Both airplane-mode cases above leave the radio re-associating. Refresh once more
     * after the whole class so the next class does not start against a half-connected
     * device - the failure that looks like a broken feature and is really this. */
    @After
    public void ensureConnectivityRestored() {
        // Teardown has to tolerate setUp() having failed. JUnit still runs @After when
        // @Before threw, and if the session was never created driver is null - touching it
        // here raised a NullPointerException that REPLACED the real failure in the report.
        // Confirmed live: an adb outage produced 17 SessionNotCreatedExceptions, and every
        // one of them was reported as "Cannot invoke IOSDriver.getConnection() because
        // this.driver is null" instead, hiding the actual cause. There is no radio to
        // restore when no session exists, so return and let the original exception stand.
        if (driver == null) {
            return;
        }
        FeedPage feedPage = new FeedPage(driver);
        feedPage.setAirplaneMode(false);
    }

    @Test
    public void TC086_Verify_Namaskara_Tap_Changes_Like_Count_Signature() {
        FeedPage feedPage = new FeedPage(driver);

        feedPage.waitUntilLoaded();
        String before = feedPage.captureNumericCountsSignature();
        feedPage.tapFirstNamaskaraAction();
        String after = feedPage.captureNumericCountsSignature();

        Assert.assertNotEquals(
                "Tapping Namaskara should change the visible like-count signature, not just leave the feed on screen",
                before,
                after
        );
    }

    @Test
    public void TC087_Verify_Post_Options_Sheet_Shows_Exactly_One_Of_Delete_Or_Report() {
        FeedPage feedPage = new FeedPage(driver);

        feedPage.waitUntilLoaded();
        String action = feedPage.openFirstPostOptionsSheet();

        Assert.assertTrue("Post Options sheet should show either Delete or Report",
                "Delete".equals(action) || "Report".equals(action));

        feedPage.dismissPostOptionsSheet();
    }

    @Test
    public void TC114_Verify_Temples_Filter_Then_All_Posts_Restores_Feed() {
        FeedPage feedPage = new FeedPage(driver);

        feedPage.waitUntilLoaded();
        Assert.assertTrue("Temples filter should be tappable", feedPage.tapTemplesFilter());
        Assert.assertTrue("Feed should stay usable after switching to Temples filter",
                feedPage.hasNonBlankPageSource());

        Assert.assertTrue("All Posts filter should be tappable", feedPage.tapAllPostsFilter());
        Assert.assertTrue("Feed should show posts again after returning to All Posts",
                feedPage.isAnyFeedItemVisible());
    }

    @Test
    public void TC064_Verify_Feed_Usable_After_Rapid_Back_Button_Spam() {
        FeedPage feedPage = new FeedPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue(
                "App should recover and still be usable after rapid back-button presses across screens",
                feedPage.isAppUsableAfterBackButtonSpam(10)
        );
    }
}
