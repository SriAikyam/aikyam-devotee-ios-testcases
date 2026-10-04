package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.ExplorePage;
import com.shriaikyam.aikyam.pages.HomePage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.shriaikyam.aikyam.categories.P0;
import org.junit.runners.MethodSorters;

// JUnit4's default order is hash-based, not declaration order - confirmed live this
// let TC071 (which types a search query and never clears it) run before TC065,
// stranding TC065's "Map should be the default view" check behind the leftover query
// (noReset=true keeps app state across tests). Pin to TC-number order, same fix
// already applied to PostTest/SettingsTest for the same class of issue.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExploreTest extends BaseTest {

    /** ExploreScreen's location chip ("Based on your current location" + edit icon, the
     * only way into the change-location dialog) is present in the app source on both main
     * and the current feature branch, but is absent from the build installed on the test
     * device - confirmed live in Map view AND List view, with location permission granted.
     * Skip rather than fail: these three tests are valid again the moment the device gets
     * a build that renders it, which an @Ignore would silently keep them from doing. */
    private static final String SKIP_NO_LOCATION_CHIP =
            "Skipping: this build's Explore screen renders no change-location chip";

    /** Explore remembers the last-used Map/List view across app sessions (noReset=true),
     * so any run left in List view makes TC065's "Map is the default" check fail on the
     * NEXT run - confirmed live. Restoring after every test, rather than only at the end
     * of the class, means an interrupted run leaves the same state a clean one does.
     * BaseTest quits the driver from a @Rule, not an @After, so the driver is still alive
     * here.
     *
     * The entity tab strip (TC171-TC176) is just as sticky: leaving Explore on
     * Devotees/Posts/Deities hides the Map/List toggle entirely, which would strand every
     * pre-existing test in this class on the NEXT run. Restore Temples first, then the map
     * view - order matters, since the toggle only exists under the Temples tab. */
    @After
    public void restoreMapView() {
        ExplorePage explorePage = new ExplorePage(driver);
        // Guarding on isEntityTabStripVisible() would defeat the point: once the strip has
        // scrolled, Temples is exactly the tab that is NOT visible. selectEntityTab scrolls
        // the strip itself and is a no-op off Explore, so call it unconditionally.
        if (!explorePage.isExploreVisible()) {
            explorePage.selectEntityTab("Temples");
        }
        if (explorePage.isExploreVisible()) {
            explorePage.switchToMapView();
        }
    }

    @Test
    public void TC066_Verify_Toggle_To_List_View() {
        HomePage homePage = new HomePage(driver);
        ExplorePage explorePage = new ExplorePage(driver);

        homePage.tapExploreTab();
        explorePage.waitUntilLoaded();

        explorePage.switchToListView();
        Assert.assertTrue("List content should load after switching to List view", explorePage.waitForListContent());
    }

    @Category(P0.class)
    @Test
    public void TC172_Verify_Devotees_Tab_Shows_Devotee_Search() {
        HomePage homePage = new HomePage(driver);
        ExplorePage explorePage = new ExplorePage(driver);

        homePage.tapExploreTab();
        explorePage.waitUntilTabStripLoaded();

        Assert.assertTrue("Devotees tab should be selectable",
                explorePage.selectEntityTab("Devotees"));
        Assert.assertTrue(
                "Devotees tab should show its own devotee search field",
                explorePage.isDevoteeSearchVisible()
        );
    }

    @Test
    public void TC173_Verify_Posts_Tab_Shows_Post_Search() {
        HomePage homePage = new HomePage(driver);
        ExplorePage explorePage = new ExplorePage(driver);

        homePage.tapExploreTab();
        explorePage.waitUntilTabStripLoaded();

        Assert.assertTrue("Posts tab should be selectable",
                explorePage.selectEntityTab("Posts"));
        Assert.assertTrue(
                "Posts tab should show its own post search field",
                explorePage.isPostSearchVisible()
        );
    }

    /** Switching away and back must restore the Temples tab's own surface - the temple
     * search field and the Map/List toggle, neither of which exists on the other tabs. */
    @Test
    public void TC176_Verify_Returning_To_Temples_Restores_Map_List_Toggle() {
        HomePage homePage = new HomePage(driver);
        ExplorePage explorePage = new ExplorePage(driver);

        homePage.tapExploreTab();
        explorePage.waitUntilTabStripLoaded();

        Assert.assertTrue("Devotees tab should be selectable",
                explorePage.selectEntityTab("Devotees"));
        Assert.assertFalse("Map/List toggle should not survive on the Devotees tab",
                explorePage.isExploreVisible());

        Assert.assertTrue("Temples tab should be selectable again",
                explorePage.selectEntityTab("Temples"));
        Assert.assertTrue("Temple search should be back on the Temples tab",
                explorePage.isTempleSearchVisible());
        Assert.assertTrue("Map/List toggle should be back on the Temples tab",
                explorePage.isExploreVisible());
    }

    private boolean retryBackToExplore(ExplorePage explorePage) {
        for (int attempt = 0; attempt < 3; attempt++) {
            explorePage.navigateBack();
            if (explorePage.isExploreVisible()) {
                return true;
            }
        }
        return false;
    }
}
