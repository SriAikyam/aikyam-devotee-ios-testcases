package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.FeedPage;
import com.shriaikyam.aikyam.pages.HomePage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Post options (•••) on someone else's post in the home feed.
 *
 * TC362/TC363 cover iOS PR #695 (Show less / Show more now send the affinity signal and keep
 * the post on screen). They run in name order so the Show more of TC363 offsets the Show less
 * signal TC362 sent for the same top post.
 *
 * TC364 covers iOS PR #704 (Hide all posts from this account). It only checks the option is
 * offered: hiding an account cannot be undone from the app (no unhide, as on Android), and
 * these tests never leave the account changed.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FeedPostOptionsJourneyTest extends BaseTest {

    private static final String SHOW_LESS = "Show less of these posts";
    private static final String SHOW_MORE = "Show more of such posts";
    private static final String HIDE_ACCOUNT = "Hide all posts from this account";

    private FeedPage feed;

    @Before
    public void openFeed() {
        new HomePage(driver).tapHomeTab();
        feed = new FeedPage(driver);
        feed.waitUntilLoaded();
    }

    @After
    public void closeMenu() {
        if (feed != null && feed.isPostOptionsSheetVisible()) {
            feed.dismissPostOptionsSheet();
        }
    }

    private void openVisitorMenu() {
        Assert.assertEquals("Post options should open on someone else's post with Report in it",
                "Report", feed.openFirstPostOptionsSheet());
    }

    @Test
    public void TC362_Show_Less_Keeps_Post_And_Shows_Toast() {
        // Taken before the menu opens: while it is open the feed's buttons are not exposed.
        String before = feed.captureFeedState();
        openVisitorMenu();

        Assert.assertTrue("'" + SHOW_LESS + "' should be in the menu", feed.tapPostOption(SHOW_LESS));

        Assert.assertTrue("Toast 'You'll see fewer posts like this' should appear",
                feed.waitForText("see fewer posts like this", 6));
        // Before #695 Show less removed the post locally; now it must stay.
        Assert.assertEquals("The post should stay in the feed after Show less (nothing shifts up)",
                before, feed.captureFeedState());
    }

    @Test
    public void TC363_Show_More_Shows_Toast() {
        String before = feed.captureFeedState();
        openVisitorMenu();

        Assert.assertTrue("'" + SHOW_MORE + "' should be in the menu", feed.tapPostOption(SHOW_MORE));

        Assert.assertTrue("Toast 'You'll see more posts like this' should appear",
                feed.waitForText("see more posts like this", 6));
        Assert.assertEquals("The post should stay in the feed after Show more (nothing shifts up)",
                before, feed.captureFeedState());
    }

    @Test
    public void TC364_Hide_Account_Option_Offered_On_Others_Post() {
        openVisitorMenu();

        Assert.assertTrue("'" + HIDE_ACCOUNT + "' should be offered on someone else's post",
                feed.isPostOptionVisible(HIDE_ACCOUNT));
        Assert.assertTrue("The menu should close without acting", feed.dismissPostOptionsSheet());
    }
}
