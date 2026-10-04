package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.ExplorePage;
import com.shriaikyam.aikyam.pages.HomePage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.List;

/**
 * Search inside the Explore tab, asserted on real outcomes: a result row whose text
 * contains the query, the screen a tap lands on, the definite empty-state strings. A tab
 * label or the container being on screen is never accepted as a result.
 *
 * Observed live (dev build, Android 15): temple suggestions appear a few seconds after
 * typing (not instantly); submitting turns them into a list; the Posts tab renders
 * results as a grid of text-less tiles, so presence is counted by tile and absence is the
 * "No posts found" string.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SearchJourneyTest extends BaseTest {

    private static final String TEMPLE_QUERY = "Gangadhareshwara";
    private static final String DEVOTEE_QUERY = "Santhosh";
    private static final String POST_QUERY = "temple";
    private static final String GIBBERISH = "zzqxjvwk";

    private ExplorePage explore;

    @Before
    public void openExploreOnTemples() {
        explore = new ExplorePage(driver);
        new HomePage(driver).tapExploreTab();
        // waitUntilTabStripLoaded also sweeps the strip back to its start, so the offset
        // another test leaked is irrelevant.
        explore.waitUntilTabStripLoaded();
        resetSearch();
        Assert.assertTrue("Temples tab should be selectable", explore.selectEntityTab("Temples"));
        resetTempleSearch();
    }

    @After
    public void restoreExplore() {
        explore = new ExplorePage(driver);
        resetSearch();
        explore.selectEntityTab("Temples");
        resetTempleSearch();
    }

    /** Clears a leftover query on whichever tab is showing, without any blind coordinate tap
     * (see ExplorePage.tapClearSearchIcon). Temples additionally need the suggestions sheet
     * dismissed, which clearTempleSearch handles. */
    private void resetSearch() {
        try {
            explore.tapClearSearchIcon();
        } catch (RuntimeException ignored) {
            // no query to clear
        }
    }

    private void resetTempleSearch() {
        try {
            explore.clearTempleSearch();
        } catch (RuntimeException ignored) {
            // no query to clear
        }
    }

    @Test
    public void TC210_Temple_Search_Lists_Matching_Temples() {
        Assert.assertTrue("Query should land in the field", explore.typeQuery(TEMPLE_QUERY));
        List<String> results = explore.waitForResultsContaining(TEMPLE_QUERY, 25);
        Assert.assertFalse("A temple whose name contains '" + TEMPLE_QUERY + "' should be listed", results.isEmpty());
        Assert.assertTrue("Expected the Gavi Gangadhareshwara temple among " + results,
                results.stream().anyMatch(t -> t.toLowerCase().contains("gavi gangadhareshwara")));
    }

    @Test
    public void TC211_Temple_Search_Result_Opens_Temple_Profile_And_Back_Returns() {
        Assert.assertTrue(explore.typeQuery(TEMPLE_QUERY));
        Assert.assertFalse("Result should appear", explore.waitForResultsContaining(TEMPLE_QUERY, 25).isEmpty());
        Assert.assertTrue("Matching result should be tappable", explore.tapResultContaining("gavi gangadhareshwara"));

        Assert.assertTrue("Temple profile (About tab / Follow action) should open", explore.isTempleProfileVisible());
        Assert.assertFalse("Profile should show the temple's name",
                explore.waitForResultsContaining("gangadhareshwara", 10).isEmpty());

        Assert.assertTrue(explore.navigateBack());
        Assert.assertTrue("Back should return to Explore", explore.isEntityTabStripVisible());
        Assert.assertFalse("Temple profile should be gone after Back", explore.isTempleProfileVisible());
    }

    @Test
    public void TC213_Devotee_Search_Finds_Named_Devotee_And_Opens_Profile() {
        Assert.assertTrue(explore.selectEntityTab("Devotees"));
        Assert.assertTrue("Query should land intact", explore.typeQuery(DEVOTEE_QUERY));
        List<String> results = explore.waitForResultsContaining(DEVOTEE_QUERY, 25);
        Assert.assertFalse("A devotee named '" + DEVOTEE_QUERY + "' should be listed", results.isEmpty());
        Assert.assertTrue(explore.tapResultContaining(DEVOTEE_QUERY));

        Assert.assertFalse("User profile should show the devotee's name",
                explore.waitForResultsContaining(DEVOTEE_QUERY, 15).isEmpty());
        Assert.assertFalse("Explore tab strip should be replaced by the profile", explore.isEntityTabStripVisible());
        Assert.assertTrue(explore.navigateBack());
        Assert.assertTrue("Back should return to Explore", explore.isEntityTabStripVisible());
    }

    @Test
    public void TC214_Devotee_Search_No_Match_Shows_No_Results_Found() {
        Assert.assertTrue(explore.selectEntityTab("Devotees"));
        Assert.assertTrue(explore.typeQuery(GIBBERISH));
        Assert.assertTrue("'No results found' should show", explore.waitForExactText("No results found", 25));
    }

    @Test
    public void TC216_Recent_Search_Is_Listed_Rerunnable_And_Clearable() {
        Assert.assertTrue(explore.typeQuery(TEMPLE_QUERY));
        Assert.assertFalse(explore.waitForResultsContaining(TEMPLE_QUERY, 25).isEmpty());
        explore.submitQuery();
        Assert.assertTrue("Submit should show the Map/List results screen", explore.waitForExactText("List", 20));
        java.util.List<String> firstRun = settledVisibleTexts();
        explore.clearTempleSearch();

        // Reopen the empty field: the history sheet lists the query.
        Assert.assertTrue(explore.focusSearchField());
        Assert.assertTrue("'Search History' section should show", explore.waitForExactText("Search History", 15));
        Assert.assertTrue("Recents should list '" + TEMPLE_QUERY + "'", explore.waitForExactText(TEMPLE_QUERY, 10));

        // Re-run it.
        Assert.assertTrue(explore.tapResultContaining(TEMPLE_QUERY));
        Assert.assertEquals("Tapping the recent should refill the field", TEMPLE_QUERY, explore.searchFieldText());
        Assert.assertTrue("Re-run should land on the results screen", explore.waitForExactText("List", 20));
        Assert.assertFalse("History sheet should be closed after re-run", explore.isExactTextVisibleNow("Search History"));
        Assert.assertEquals("Re-running the recent should reproduce the original results", firstRun, settledVisibleTexts());

        // Clean up local history.
        explore.clearTempleSearch();
        Assert.assertTrue(explore.focusSearchField());
        Assert.assertTrue("'Clear all' should be offered", explore.tapClearAllRecents());
        Assert.assertTrue("History should be empty afterwards",
                explore.waitUntilExactTextGone(TEMPLE_QUERY, 10));
    }

    /** Results load after the Map/List screen appears, so a snapshot taken straight away can
     * be half-empty. Waits for two snapshots two seconds apart to match (and for real
     * content beyond the tab strip) before returning it. */
    private java.util.List<String> settledVisibleTexts() {
        java.util.List<String> previous = explore.visibleTexts();
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
            java.util.List<String> current = explore.visibleTexts();
            if (current.equals(previous) && current.size() > 20) {
                return current;
            }
            previous = current;
        }
        return previous;
    }
}
