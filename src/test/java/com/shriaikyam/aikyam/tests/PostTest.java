package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.FeedPage;
import com.shriaikyam.aikyam.pages.MediaEditorPage;
import com.shriaikyam.aikyam.pages.PostPage;
import com.shriaikyam.aikyam.pages.ProfilePage;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.time.Duration;

// JUnit4's default order (MethodSorters.DEFAULT) is a hash-based order, not declaration
// order - it varies from run to run and previously caused later tests to inherit whatever
// screen an earlier, differently-ordered test left the app on (e.g. a still-open dialog).
// Pinning to TC-number order keeps the suite in its intended flow: create -> post -> appears
// in feed -> delete.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PostTest extends BaseTest {
    @Test
    public void TC024_Verify_Add_Caption() {
        PostPage postPage = openCreatePostPage();
        String caption = "Regression caption " + System.currentTimeMillis();

        Assert.assertTrue("Caption should be entered", postPage.addCaption(caption));
        Assert.assertTrue("Caption text should be visible", postPage.isCaptionPresent(caption));
    }

    @Test
    public void TC031_Verify_Mention_Symbol_Insertion() {
        PostPage postPage = openCreatePostPage();

        Assert.assertTrue("Mention symbol should be tappable", postPage.insertMentionSymbol());
        Assert.assertTrue("Create post screen should remain visible", postPage.isCreatePostVisible());
    }

    @Test
    public void TC032_Verify_Hashtag_Symbol_Insertion() {
        PostPage postPage = openCreatePostPage();

        Assert.assertTrue("Hashtag symbol should be tappable", postPage.insertHashtagSymbol());
        Assert.assertTrue("Create post screen should remain visible", postPage.isCreatePostVisible());
    }

    /** Negative complement to TC035 - the Post button must NOT be enabled before any
     * caption or media has been attached, matching the "verify actual state" mandate
     * rather than only checking the positive/enabled case. */
    @Test
    public void TC112_Verify_Post_Button_Disabled_Without_Caption_Or_Media() {
        PostPage postPage = openCreatePostPage();

        Assert.assertTrue("Post button should be disabled with no caption or media attached",
                postPage.isPostButtonDisabled());
    }

    /** Attaches an image and opens its editor, asserting each step so a failure names the
     * step that broke rather than surfacing later as a confusing tool-not-found. */
    private MediaEditorPage openEditorOnFreshImage() {
        PostPage postPage = openCreatePostPage();
        MediaEditorPage editorPage = new MediaEditorPage(driver);

        Assert.assertTrue("Image should attach from gallery", postPage.attachFirstGalleryImage());
        Assert.assertTrue("Media thumbnail should open the editor", postPage.tapFirstMediaThumbnail());
        Assert.assertTrue("Media editor should be visible", editorPage.isEditorVisible());
        return editorPage;
    }

    private PostPage openCreatePostPage() {
        PostPage postPage = new PostPage(driver);
        if (postPage.isCreatePostVisible()) {
            discardLeftoverDraft(postPage);
            return postPage;
        }

        FeedPage feedPage = new FeedPage(driver);
        Assert.assertTrue("Create post should open", feedPage.openCreatePost());

        postPage.waitUntilLoaded();
        return postPage;
    }

    /** Backs out of any leftover draft (media/caption from an earlier test) via the
     * real Discard flow, then reopens a fresh Create Post screen. No-ops if the screen
     * is already clean. */
    private void discardLeftoverDraft(PostPage postPage) {
        postPage.dismissKeyboardIfShown();
        if (!postPage.tapBack()) {
            return;
        }
        if (postPage.isDiscardDialogVisible()) {
            postPage.tapDiscardConfirm();
        }
        if (!postPage.isCreatePostVisible()) {
            FeedPage feedPage = new FeedPage(driver);
            feedPage.openCreatePost();
            postPage.waitUntilLoaded();
        }
    }

    private boolean waitForFeedReturn(FeedPage feedPage) {
        try {
            return feedPage.isAnyFeedItemVisible() || waitForFeed(feedPage);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean waitForFeed(FeedPage feedPage) {
        try {
            feedPage.waitUntilLoaded();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
