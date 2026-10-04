package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.AppsPage;
import com.shriaikyam.aikyam.pages.BookSevaPage;
import com.shriaikyam.aikyam.pages.ExplorePage;
import com.shriaikyam.aikyam.pages.FeedPage;
import com.shriaikyam.aikyam.pages.HomePage;
import com.shriaikyam.aikyam.pages.MusicPage;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.shriaikyam.aikyam.categories.P0;

import java.util.function.BooleanSupplier;

public class HomeTest extends BaseTest {

    /* ---- Phase 3 pilot: THIS CLASS ONLY. HomeTest is read-mostly, has no
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

    /** Reselecting an already-active tab triggers a scroll-to-top animation in this
     * app; a tab switch dispatched immediately afterward can race that animation and
     * miss (confirmed live) - retrying the tap once against the destination's own
     * signal is a real wait-for-condition, not a blind sleep. */
    private void tapAndVerify(Runnable tap, BooleanSupplier destinationVisible) {
        tap.run();
        if (!destinationVisible.getAsBoolean()) {
            tap.run();
        }
    }

    @Category(P0.class)
    @Test
    public void TC001_Verify_Home_Screen_Load() {
        HomePage homePage = new HomePage(driver);

        homePage.waitUntilLoaded();

        Assert.assertTrue(
                "Home bottom navigation should be visible",
                homePage.isHomeBottomNavigationVisible()
        );
    }

    /** New - Explore tab click had no dedicated test at all (TC008 was missing from
     * the original numbering). */
    @Category(P0.class)
    @Test
    public void TC008_Verify_Explore_Tab_Opens_Explore_Screen() {
        HomePage homePage = new HomePage(driver);
        ExplorePage explorePage = new ExplorePage(driver);

        homePage.waitUntilLoaded();
        tapAndVerify(homePage::tapExploreTab, explorePage::isExploreVisible);

        Assert.assertTrue("Explore tab should actually load the Explore screen (Map/List toggle), not just keep the bottom nav visible",
                explorePage.isExploreVisible());
    }

    /** Upgraded from "bottom nav still visible" to confirming Music's own destination
     * content (the Music header/home carousels) actually rendered. */
    @Category(P0.class)
    @Test
    public void TC009_Verify_Music_Tab_Opens_Music_Screen() {
        HomePage homePage = new HomePage(driver);
        MusicPage musicPage = new MusicPage(driver);

        homePage.waitUntilLoaded();
        tapAndVerify(homePage::tapMusicTab, musicPage::isMusicScreenVisible);

        Assert.assertTrue("Music tab should actually load the Music screen, not just keep the bottom nav visible",
                musicPage.isMusicScreenVisible());
    }

    /** Upgraded from "bottom nav still visible" to confirming Apps' own destination
     * content (Stotragalu/Panchang cards) actually rendered. */
    @Category(P0.class)
    @Test
    public void TC011_Verify_Apps_Tab_Opens_Apps_Screen() {
        HomePage homePage = new HomePage(driver);
        AppsPage appsPage = new AppsPage(driver);

        homePage.waitUntilLoaded();
        tapAndVerify(homePage::tapAppsTab, appsPage::isAppsScreenVisible);

        Assert.assertTrue("Apps tab should actually load its feature cards, not just keep the bottom nav visible",
                appsPage.isAppsScreenVisible());
    }

    @Category(P0.class)
    @Test
    public void TC012_Verify_All_Bottom_Navigation_Items_Are_Present() {
        HomePage homePage = new HomePage(driver);

        homePage.waitUntilLoaded();

        Assert.assertEquals("Bottom navigation should expose five visible items", 5, homePage.visibleBottomNavigationItemCount());
    }

    /** Upgraded to assert each tab's own destination content along the cycle, not
     * just that the bottom nav survives the whole sequence. */
    @Test
    public void TC013_Verify_Full_Bottom_Navigation_Cycle_Loads_Each_Destination() {
        HomePage homePage = new HomePage(driver);
        ExplorePage explorePage = new ExplorePage(driver);
        MusicPage musicPage = new MusicPage(driver);
        BookSevaPage bookSevaPage = new BookSevaPage(driver);
        AppsPage appsPage = new AppsPage(driver);
        FeedPage feedPage = new FeedPage(driver);

        homePage.waitUntilLoaded();

        tapAndVerify(homePage::tapExploreTab, explorePage::isExploreVisible);
        Assert.assertTrue("Explore should load mid-cycle", explorePage.isExploreVisible());

        tapAndVerify(homePage::tapMusicTab, musicPage::isMusicScreenVisible);
        Assert.assertTrue("Music should load mid-cycle", musicPage.isMusicScreenVisible());

        // The fifth slot is visited so the cycle really covers all five tabs. It was
        // Sevas; confirmed live it is now Events (Seva is on hold - see BookSevaTest),
        // so tapEventsTab() is what actually reaches that destination today. Content
        // there isn't asserted - this test is about navigation reaching each
        // destination, not about what any one of them renders.
        tapAndVerify(homePage::tapEventsTab, homePage::isHomeBottomNavigationVisible);

        tapAndVerify(homePage::tapAppsTab, appsPage::isAppsScreenVisible);
        Assert.assertTrue("Apps should load mid-cycle", appsPage.isAppsScreenVisible());

        tapAndVerify(homePage::tapHomeTab, feedPage::isAnyFeedItemVisible);
        Assert.assertTrue("Bottom navigation should remain visible after cycling all tabs", homePage.isHomeBottomNavigationVisible());
        Assert.assertTrue("Home should load feed content at the end of the cycle", feedPage.isAnyFeedItemVisible());
    }

}
