package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.ExplorePage;
import com.shriaikyam.aikyam.pages.HomePage;
import org.junit.After;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.shriaikyam.aikyam.categories.P0;
import org.junit.runners.MethodSorters;
import org.junit.Assume;

// JUnit4's default order is hash-based, not declaration order - confirmed live this
// let TC071 (which types a search query and never clears it) run before TC065,
// stranding TC065's "Map should be the default view" check behind the leftover query
// (noReset=true keeps app state across tests). Pin to TC-number order, same fix
// already applied to PostTest/SettingsTest for the same class of issue.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExploreTest extends BaseTest {

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

    @Category(P0.class)
    @Test
    public void TC065_Verify_Explore_Screen_Opens_With_Map_Default() {
        HomePage homePage = new HomePage(driver);
        ExplorePage explorePage = new ExplorePage(driver);

        homePage.tapExploreTab();
        explorePage.waitUntilLoaded();

        Assert.assertTrue("Explore screen (Map/List toggle) should be visible", explorePage.isExploreVisible());
        Assert.assertTrue("Map should be the default view on entering Explore", explorePage.isMapViewActive());
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

    // Deliberately not @Category(P0): it is the return half of TC065's toggle, so TC065 failing already catches a broken toggle.
    @Test
    public void TC067_Verify_Toggle_Back_To_Map_View() {
        HomePage homePage = new HomePage(driver);
        ExplorePage explorePage = new ExplorePage(driver);

        homePage.tapExploreTab();
        explorePage.waitUntilLoaded();

        explorePage.switchToListView();
        explorePage.waitForListContent();

        explorePage.switchToMapView();
        Assert.assertTrue("Map view should be active again after toggling back", explorePage.isMapViewActive());
    }

    /** iOS copy is "No matches found" (Android: "No locations found"); both hang off
     * explore.location.empty, so the assertion is identifier-based and reads the same. */
    // Deliberately not @Category(P0): it skips on a device - the location it needs can only be seeded on a simulator - and a gate case must run everywhere.
    @Test
    public void TC069_Verify_Invalid_Location_Search_Shows_No_Locations_Found() {
        HomePage homePage = new HomePage(driver);
        ExplorePage explorePage = new ExplorePage(driver);

        homePage.tapExploreTab();
        explorePage.waitUntilLoaded();

        // The chip only renders once the app has a location to show. The suite can seed one
        // with `simctl location` on a simulator, but not on a physical device, where it is
        // the real GPS - so on hardware away from the seeded area there is no chip to tap.
        // Verified on the test iPhone: explore.toggle.map/list resolve (the screen is fine)
        // while explore.location.change and explore.location.label return 0 matches.
        // Skipped rather than failed: nothing about this case is broken, its precondition
        // is simply absent on this host.
        Assume.assumeTrue("Skipping: Explore shows no change-location chip on this host - "
                        + "the suite can only seed a location on a simulator (simctl location); "
                        + "on a device the phone has to actually be where the data is seeded",
                explorePage.isLocationChipVisible());
        explorePage.openChangeLocationDialog();
        Assert.assertTrue("Change location dialog should be visible", explorePage.isChangeLocationDialogVisible());

        Assert.assertTrue("Nonsense query should be accepted by the field",
                explorePage.searchLocation("zzxxqqinvalidplace123"));
        explorePage.submitSearch();

        // Wait for the search to actually resolve first. "No matches found" is also what
        // the dialog shows in the gap BEFORE completions arrive, so asserting it straight
        // after submitSearch() could pass before the query ran at all - and could not tell
        // a genuinely invalid query from MapKit throttling, the same flakiness that got
        // TC070 excluded (review finding 2).
        // Two different waits, deliberately: this one answers "has the query produced a
        // result yet?" and is the only guard on the INVALID-query path, where no row ever
        // arrives. selectLocationSuggestion's own waitForSuggestionsToSettle() answers
        // "has the list stopped reordering?" and only runs on the valid path. They are not
        // redundant (Vinod's review).
        Assert.assertTrue("The location search should resolve before it is judged",
                explorePage.waitForLocationSearchToResolve());

        Assert.assertTrue("'No matches found' should be shown for an invalid query",
                explorePage.isNoLocationsFoundVisible());
    }

    // Deliberately not @Category(P0): it only types into a field; nothing downstream depends on it.
    @Test
    public void TC071_Verify_Temple_Search_Field_Accepts_Input() {
        HomePage homePage = new HomePage(driver);
        ExplorePage explorePage = new ExplorePage(driver);

        homePage.tapExploreTab();
        explorePage.waitUntilLoaded();

        try {
            Assert.assertTrue("Temple search field should accept a query",
                    explorePage.searchTemple("Ganesha"));
        } finally {
            // In a finally: a failed assertion used to skip this and leak the query into
            // every later test on this screen (review finding 8).
            explorePage.clearTempleSearch();
        }
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
