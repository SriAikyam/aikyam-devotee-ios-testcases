package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.ExplorePage;
import com.shriaikyam.aikyam.pages.HomePage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * iOS PR #705 (#669): on Explore's Temples tab, pressing Search on a temple's exact name opens
 * that temple (temples/suggest exact match, ignoring case and leading/trailing spaces; spaces
 * inside the name are not collapsed). A partial name keeps the normal search.
 *
 * Kept apart from SearchJourneyTest, whose setup drives the entity tab strip through
 * identifiers that ship only on the app branch qa/appium-ios-tests. This class needs only the
 * explore.search field, so it also runs on a plain main/PR build. The temple is the one the
 * PR was verified with on dev.
 */
public class TempleSuggestSearchTest extends BaseTest {

    private ExplorePage explore;

    @Before
    public void openExploreTemples() {
        explore = new ExplorePage(driver);
        new HomePage(driver).tapExploreTab();
        // Temples is Explore's first and default tab; its placeholder proves it is selected.
        Assert.assertTrue("Explore should open on the Temples tab", explore.isTempleSearchVisible());
        // Same path the PR was verified on: Temples -> List, then search.
        explore.switchToListView();
    }

    @After
    public void leaveExplore() {
        if (!explore.isTempleSearchVisible()) {
            explore.navigateBack();
        }
        explore.clearTempleSearch();
    }

    @Test
    public void TC366_Exact_Temple_Name_Submit_Opens_Temple_Profile() {
        Assert.assertTrue("Query should land in the field",
                explore.typeQuery("  veerananjanaswamy TEMPLE mulbagal  "));
        waitForSuggestResponse();
        explore.submitQuery();

        // The profile's Follow button, with the search capsule gone, is the temple screen.
        Assert.assertTrue("Search on an exact temple name should open that temple's profile",
                explore.waitForExactText("Follow", 15) && !explore.isTempleSearchVisible());
    }

    @Test
    public void TC367_Partial_Temple_Name_Submit_Stays_On_Explore() {
        Assert.assertTrue("Query should land in the field", explore.typeQuery("Veera"));
        waitForSuggestResponse();
        explore.submitQuery();

        Assert.assertEquals("Search on a partial name should keep the query on Explore",
                "Veera", explore.searchFieldText());
    }

    /** The suggest call fires 150 ms after typing stops and has no on-screen result (there is
     * no dropdown), so there is nothing to wait on but time. */
    private static void waitForSuggestResponse() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
