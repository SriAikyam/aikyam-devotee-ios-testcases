package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.FeedPage;
import com.shriaikyam.aikyam.pages.HomePage;
import com.shriaikyam.aikyam.pages.StoriesPage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Feed circle strip journeys. FINDING: in this build a person circle does not open a status
 * viewer (StatusViewerScreen is routed but nothing navigates to it); tapping a circle scopes the
 * feed to that person, shows a ring on it and clears its blue unseen dot (server-side
 * markSubscriberSeen - viewing counts as seen on the shared dev account). Nothing is created,
 * deleted or modified.
 */
public class StoriesJourneyTest extends BaseTest {

    private FeedPage feed;
    private StoriesPage strip;

    @Before
    public void startFromCleanFeed() {
        new HomePage(driver).tapHomeTab();
        feed = new FeedPage(driver);
        strip = new StoriesPage(driver);
        feed.waitUntilLoaded();
        feed.tapAllPostsFilter();
        strip.scrollStripToStart();
    }

    @After
    public void leaveFeedClean() {
        try {
            feed.tapAllPostsFilter();
            strip.scrollStripToStart();
        } catch (RuntimeException ignored) {
            // best effort; the next test's @Before resets as well
        }
    }

    private List<String> circles() {
        List<String> names = new ArrayList<>(strip.visibleCircleNames());
        Assert.assertFalse("Precondition: the strip must show at least one person circle", names.isEmpty());
        return names;
    }

    @Test
    public void TC250_Verify_Tapping_A_Person_Circle_Scopes_Feed_To_That_Person_And_Opens_No_Viewer() {
        String name = circles().get(0);
        Assert.assertTrue("Circle should be tappable: " + name, strip.tapCircle(name));

        Assert.assertTrue("Feed should be scoped to only " + name + " posts, but authors were "
                + strip.visiblePostAuthors(), strip.waitForOnlyAuthor(name));
        Assert.assertFalse("Tapping a circle must not open a status viewer", strip.isStatusViewerOpen());
        Assert.assertTrue("The tapped circle should wear the selection ring", strip.hasSelectedRing(name));
        Assert.assertFalse("All Posts should lose its ring once a person is selected", strip.hasSelectedRing("All Posts"));
        Assert.assertTrue("The strip must stay visible while scoped", strip.visibleCircleNames().contains(name));
    }

    @Test
    public void TC251_Verify_Switching_Between_Circles_Changes_The_Scoped_Author() {
        List<String> names = circles();
        Assert.assertTrue("Need two person circles in the strip, saw " + names, names.size() >= 2);
        String first = names.get(0);
        String second = names.get(1);

        Assert.assertTrue(strip.tapCircle(first));
        Assert.assertTrue("Feed should scope to " + first, strip.waitForOnlyAuthor(first));
        Assert.assertTrue(strip.tapCircle(second));
        Assert.assertTrue("Feed should switch to only " + second + ", authors were " + strip.visiblePostAuthors(),
                strip.waitForOnlyAuthor(second));
        Assert.assertTrue("Ring should move to " + second, strip.hasSelectedRing(second));
        Assert.assertFalse("Ring should leave " + first, strip.hasSelectedRing(first));
    }

    @Test
    public void TC252_Verify_Selecting_A_Circle_Clears_Its_Unseen_Dot_And_It_Stays_Cleared() {
        String name = null;
        for (String candidate : circles()) {
            if (strip.hasUnseenDot(candidate)) {
                name = candidate;
                break;
            }
        }
        // If every visible circle has already been viewed (this test consumes dots), still prove
        // the invariant on the first circle: selecting never leaves an unseen dot.
        boolean hadDot = name != null;
        if (name == null) {
            name = circles().get(0);
        }
        Assert.assertTrue(strip.tapCircle(name));
        Assert.assertTrue(strip.waitForOnlyAuthor(name));
        Assert.assertFalse(name + (hadDot ? " (had a blue dot)" : "") + " must show no unseen dot after selection",
                strip.hasUnseenDot(name));

        feed.tapAllPostsFilter();
        Assert.assertFalse(name + " dot must stay cleared after returning to All Posts", strip.hasUnseenDot(name));
    }

    @Test
    public void TC253_Verify_Scroll_Right_Reveals_Further_Circles() {
        Set<String> before = strip.visibleCircleNames();
        Assert.assertTrue("Strip should offer a Scroll right control", strip.tapScrollRight());
        Assert.assertTrue("Scrolling right should change the visible circles, still " + before,
                strip.waitForCircleSetToChangeFrom(before));
        Set<String> after = strip.visibleCircleNames();
        Assert.assertFalse("A circle not shown before should appear", before.containsAll(after));
        Assert.assertTrue("A Scroll left control should now be offered", strip.isScrollLeftVisible());
    }

    @Test
    public void TC254_Verify_Temples_And_Live_Filters_Keep_Strip_And_Return_To_All_Posts() {
        Assert.assertTrue("All Posts starts selected", strip.hasSelectedRing("All Posts"));
        Assert.assertTrue(feed.tapTemplesFilter());
        Assert.assertTrue("Temples ring after tap", waitRing("Temples", true));
        Assert.assertFalse("All Posts ring cleared by Temples", strip.hasSelectedRing("All Posts"));
        Assert.assertTrue(strip.tapLiveCircle());
        Assert.assertTrue("Live ring after tap", waitRing("Live", true));
        Assert.assertFalse("Temples ring cleared by Live", strip.hasSelectedRing("Temples"));
        Assert.assertTrue(feed.tapAllPostsFilter());
        Assert.assertTrue("All Posts ring restored", waitRing("All Posts", true));
        Assert.assertFalse("Live ring cleared", strip.hasSelectedRing("Live"));
        feed.waitUntilLoaded();
        Assert.assertTrue("Feed content should be back under All Posts", feed.isAnyFeedItemVisible());
    }

    private boolean waitRing(String name, boolean expected) {
        for (int i = 0; i < 20; i++) {
            if (strip.hasSelectedRing(name) == expected) {
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }
}
