package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.BasePage;
import com.shriaikyam.aikyam.pages.FeedPage;
import com.shriaikyam.aikyam.pages.HomePage;
import com.shriaikyam.aikyam.pages.ProfilePage;
import com.shriaikyam.aikyam.pages.SettingsPage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.shriaikyam.aikyam.categories.P0;

/**
 * Feed end-to-end journeys: each test follows one user action through to its visible
 * outcome (not just "the control is on screen"). The ones that change server state undo
 * their own change in a finally block so the shared dev account is left as found.
 */
public class FeedJourneyTest extends BaseTest {

    private static final String SAVED_POSTS_EMPTY = "No saved posts";
    /** Enough to clear litter from interrupted runs without spinning on a broken unsave. */
    private static final int MAX_SAVED_POSTS_DRAIN = 10;

    @Before
    public void returnToFeed() {
        new HomePage(driver).tapHomeTab();
        FeedPage feedPage = new FeedPage(driver);
        feedPage.waitUntilLoaded();
        // The Temples/Live filter selection survives between tests, so start from All Posts.
        feedPage.tapAllPostsFilter();
    }

    /** The bookmark's saved state is only a tint/icon swap, so the proof that a save took
     * effect is the post appearing under Settings > Saved Posts. */
    @Category(P0.class)
    @Test
    public void TC180_Verify_Saved_Post_Appears_In_Saved_Posts_And_Unsave_Removes_It() {
        FeedPage feedPage = new FeedPage(driver);

        ensureSavedPostsEmpty();

        feedPage.tapFirstSaveAction();
        boolean stillSaved = true;
        try {
            SettingsPage saved = openSavedPosts();
            Assert.assertFalse("Saved Posts should no longer be empty after saving a post",
                    saved.isEmptyStateShown(SAVED_POSTS_EMPTY));
            Assert.assertTrue("Saved Posts should list the saved post",
                    saved.isPostListContentPresent(SAVED_POSTS_EMPTY));
            backToFeed();

            feedPage.tapFirstSaveAction();
            stillSaved = false;
            Assert.assertTrue("Saved Posts should be empty again after unsaving",
                    openSavedPosts().isEmptyStateShown(SAVED_POSTS_EMPTY));
        } finally {
            if (stillSaved) {
                backToFeed();
                feedPage.tapFirstSaveAction();
            }
        }
    }

    @Category(P0.class)
    @Test
    public void TC181_Verify_Namaskara_Then_Unnamaskara_Restores_The_Counts() {
        FeedPage feedPage = new FeedPage(driver);
        String before = feedPage.captureNumericCountsSignature();

        feedPage.tapFirstNamaskaraAction();
        boolean toggled = true;
        try {
            Assert.assertTrue("Namaskara should change the visible counts (was " + before + ")",
                    feedPage.waitForCountsSignatureToChangeFrom(before));

            // A second tap while the first is still being saved is ignored by the app.
            pauseForServer();
            feedPage.tapFirstNamaskaraAction();
            toggled = false;
            Assert.assertTrue("Tapping Namaskara again should restore the original counts",
                    feedPage.waitForCountsSignature(before));
        } finally {
            if (toggled) {
                pauseForServer();
                feedPage.tapFirstNamaskaraAction();
            }
        }
    }

    @Category(P0.class)
    @Test
    public void TC182_Verify_Share_Opens_Native_Share_Sheet_And_Returns_To_Feed() {
        FeedPage feedPage = new FeedPage(driver);
        ProfilePage profilePage = new ProfilePage(driver);

        Assert.assertTrue("Share should be tappable", feedPage.tapFirstShareAction());
        Assert.assertTrue("Native share sheet should open", profilePage.isNativeShareSheetVisible());
        Assert.assertTrue("Share sheet should be dismissible", profilePage.dismissShareSheet());
        Assert.assertTrue("Feed should be showing again after the share sheet closes",
                feedPage.isAnyFeedItemVisible());
    }

    @Category(P0.class)
    @Test
    public void TC183_Verify_Post_Image_Opens_Fullscreen_And_Back_Returns_To_Feed() {
        FeedPage feedPage = new FeedPage(driver);

        Assert.assertTrue("The first post's image should be tappable", feedPage.tapFirstPostMedia());
        Assert.assertTrue("Tapping the image should open the fullscreen viewer",
                feedPage.waitForPostFullscreenViewer());
        Assert.assertTrue("Back should close the fullscreen viewer", feedPage.closePostFullscreenViewer());
        Assert.assertFalse("The fullscreen viewer should be gone", feedPage.isPostFullscreenViewerOpen());
        Assert.assertTrue("Feed should be showing again", feedPage.isAnyFeedItemVisible());
    }

    /** Needs a multi-image post near the top of the feed - data-dependent. */
    @Test
    public void TC184_Verify_Swiping_A_Multi_Image_Post_Advances_Its_Counter() {
        FeedPage feedPage = new FeedPage(driver);

        String before = feedPage.findCarouselCounter(6);
        Assert.assertNotNull("A multi-image post (n/m counter) should be present in the first feed screens", before);
        Assert.assertTrue("The carousel should be swipeable", feedPage.swipeVisibleCarousel(true));
        Assert.assertTrue("Swiping should move to the next image (counter changes from " + before + ")",
                feedPage.waitForCarouselCounterToChange(before));
    }

    /** Follows a temple from its post. In the feed the Follow button just disappears (there
     * is no "Following" label on the post), so the follow state is proved on the temple's own
     * page, and the test unfollows there so the account is left as found. */
    @Test
    public void TC185_Verify_Follow_Then_Unfollow_A_Temple_From_A_Post() {
        FeedPage feedPage = new FeedPage(driver);

        Assert.assertTrue("A post with a Follow button should be in the first feed screens",
                feedPage.scrollToFollowButton(8));
        String author = feedPage.authorBesideFollowButton();
        Assert.assertNotNull("The author next to the Follow button should be readable", author);

        Assert.assertTrue("Follow should be tappable", feedPage.tapFollow());
        boolean following = true;
        try {
            Assert.assertTrue("The Follow button should disappear from " + author + "'s post",
                    feedPage.waitForFollowButtonGone(author));

            Assert.assertTrue("The author's name should open their profile", feedPage.openAuthorProfile(author));
            Assert.assertTrue("The profile should show Following", feedPage.waitForFollowing());

            Assert.assertTrue("Following should be tappable", feedPage.tapFollowing());
            Assert.assertTrue("Tapping Following should open the Unfollow confirmation",
                    feedPage.isUnfollowSheetVisible());
            Assert.assertTrue("Unfollow should be tappable", feedPage.confirmUnfollow());
            following = false;
            Assert.assertTrue("The profile should return to Follow", feedPage.waitForFollowButton());
        } finally {
            if (following && feedPage.waitForFollowing() || following && feedPage.openAuthorProfile(author)) {
                if (feedPage.tapFollowing() && feedPage.isUnfollowSheetVisible()) {
                    feedPage.confirmUnfollow();
                }
            }
        }
    }

    @Category(P0.class)
    @Test
    public void TC186_Verify_Temples_Filter_Changes_The_Feed_And_All_Posts_Restores_It() {
        FeedPage feedPage = new FeedPage(driver);

        String allPostsFirst = feedPage.firstPostAuthor();
        Assert.assertNotNull("The first post's author should be readable under All Posts", allPostsFirst);

        Assert.assertTrue("Temples filter should be tappable", feedPage.tapTemplesFilter());
        Assert.assertTrue("The Temples filter should change which post is first (was " + allPostsFirst + ")",
                feedPage.waitForFirstPostAuthorToChangeFrom(allPostsFirst));
        String templesFirst = feedPage.firstPostAuthor();

        Assert.assertTrue("All Posts filter should be tappable", feedPage.tapAllPostsFilter());
        feedPage.waitUntilLoaded();
        Assert.assertEquals("All Posts should restore the original first post", allPostsFirst, feedPage.firstPostAuthor());
    }

    private static void pauseForServer() {
        try {
            Thread.sleep(2500);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /** Establish the empty Saved Posts state this test needs, rather than assert it.
     *
     * This was an assertion ("Precondition: Saved Posts must start empty on this
     * account"), which left the test unable to recover from its own interruption: the
     * cleanup that unsaves lives in a finally block, so a run killed mid-test - or one
     * that failed on the precondition itself, before any cleanup could run - left a post
     * saved, and every later run then failed on the precondition instead of on anything
     * the product did. A smoke gate that a single interrupted run can disable permanently
     * is not a gate. Draining is idempotent: already-empty costs one check.
     *
     * The loop stays bounded so a genuinely broken unsave surfaces as a failure instead
     * of spinning, and the assertion below still fails loudly in that case - but now it
     * means "unsave does not work", not "a previous run left litter". */
    private void ensureSavedPostsEmpty() {
        SettingsPage saved = openSavedPosts();
        for (int attempt = 0; attempt < MAX_SAVED_POSTS_DRAIN
                && !saved.isEmptyStateShown(SAVED_POSTS_EMPTY); attempt++) {
            if (!saved.unsaveFirstListedPost(SAVED_POSTS_EMPTY)) {
                break;
            }
        }
        Assert.assertTrue("Saved Posts should be drained to empty before the test body "
                        + "(unsave appears broken, or the list is longer than "
                        + MAX_SAVED_POSTS_DRAIN + " posts)",
                saved.isEmptyStateShown(SAVED_POSTS_EMPTY));
        backToFeed();
    }

    private SettingsPage openSavedPosts() {
        Assert.assertTrue("Settings hub should open", new ProfilePage(driver).openSettingsHub());
        SettingsPage settingsPage = new SettingsPage(driver);
        Assert.assertTrue("Saved Posts row should be tappable", settingsPage.openRow("Saved Posts"));
        Assert.assertTrue("Saved Posts screen should open", settingsPage.isDestinationTitleVisible("Saved Posts"));
        return settingsPage;
    }

    private void backToFeed() {
        BasePage.navigateBack(driver);
        BasePage.navigateBack(driver);
        new HomePage(driver).tapHomeTab();
        new FeedPage(driver).waitUntilLoaded();
    }
}
