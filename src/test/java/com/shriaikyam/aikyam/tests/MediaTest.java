package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.FeedPage;
import com.shriaikyam.aikyam.pages.MediaPage;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class MediaTest extends BaseTest {

    @Test
    public void TC026_Verify_Video_Scroll_Behaviour() {
        FeedPage feedPage = new FeedPage(driver);
        MediaPage mediaPage = new MediaPage(driver);

        feedPage.waitUntilLoaded();
        String initialMediaState = mediaPage.captureMediaState();
        mediaPage.scrollMediaFeed();

        Assert.assertTrue("Media feed should scroll with video content", mediaPage.didMediaScroll(initialMediaState));
    }

    @Test
    public void TC027_Verify_Media_Content_Available_After_Feed_Load() {
        FeedPage feedPage = new FeedPage(driver);
        MediaPage mediaPage = new MediaPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue("Media or feed content should be available after feed load", mediaPage.isMediaContentAvailable());
    }

    @Test
    public void TC029_Verify_Media_State_Capture() {
        FeedPage feedPage = new FeedPage(driver);
        MediaPage mediaPage = new MediaPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue("Media page source/state should be captured", mediaPage.isMediaStateCaptured());
    }

    @Test
    public void TC030_Verify_Media_Available_After_Scroll() {
        FeedPage feedPage = new FeedPage(driver);
        MediaPage mediaPage = new MediaPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue("Media or feed content should remain available after scroll", mediaPage.isMediaAvailableAfterScroll());
    }

    @Test
    public void TC031_Verify_Media_Feed_Usable_After_Playback_Attempt() {
        FeedPage feedPage = new FeedPage(driver);
        MediaPage mediaPage = new MediaPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue("Media feed should remain usable after playback attempt", mediaPage.isMediaFeedUsableAfterPlaybackAttempt());
    }

    @Test
    public void TC032_Verify_Media_Feed_Recovers_After_Multiple_Scrolls() {
        FeedPage feedPage = new FeedPage(driver);
        MediaPage mediaPage = new MediaPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue("Media feed should remain usable after multiple scrolls", mediaPage.isMediaFeedUsableAfterMultipleScrolls(3));
    }

    @Test
    public void TC033_Verify_Video_State_Recoverable_After_Scroll() {
        FeedPage feedPage = new FeedPage(driver);
        MediaPage mediaPage = new MediaPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue("Video state should remain recoverable after scrolling away", mediaPage.isVideoStateRecoverableAfterScroll());
    }

    @Test
    public void TC034_Verify_Video_State_Still_Recognized_After_Second_Lookup() {
        FeedPage feedPage = new FeedPage(driver);
        MediaPage mediaPage = new MediaPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue("Video state should still be recognized on a second lookup", mediaPage.isVideoStateStillRecognizedAfterSecondLookup());
    }

    @Test
    public void TC035_Verify_Home_Navigation_Visible_On_Media() {
        FeedPage feedPage = new FeedPage(driver);
        MediaPage mediaPage = new MediaPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue("Home navigation should be visible on media feed", mediaPage.isHomeNavigationVisible());
    }

    @Test
    public void TC036_Verify_Save_Action_Visible_On_Media() {
        FeedPage feedPage = new FeedPage(driver);
        MediaPage mediaPage = new MediaPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue("Save action should be visible on media feed", mediaPage.isSaveActionVisible());
    }

    @Test
    public void TC037_Verify_Comment_Action_Visible_On_Media() {
        FeedPage feedPage = new FeedPage(driver);
        MediaPage mediaPage = new MediaPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue("Comment action should be visible on media feed", mediaPage.isCommentActionVisible());
    }

    @Test
    public void TC038_Verify_Namaskara_Action_Visible_On_Media() {
        FeedPage feedPage = new FeedPage(driver);
        MediaPage mediaPage = new MediaPage(driver);

        feedPage.waitUntilLoaded();

        Assert.assertTrue("Namaskara action should be visible on media feed", mediaPage.isNamaskaraActionVisible());
    }

    @Test
    public void TC039_Verify_Media_Remains_Usable_After_Playback_And_Scroll() {
        FeedPage feedPage = new FeedPage(driver);
        MediaPage mediaPage = new MediaPage(driver);

        feedPage.waitUntilLoaded();
        mediaPage.startVideoPlayback();
        mediaPage.scrollMediaFeed();

        Assert.assertTrue("Media feed should remain usable after playback and scroll", mediaPage.isMediaFeedUsable());
    }

}
