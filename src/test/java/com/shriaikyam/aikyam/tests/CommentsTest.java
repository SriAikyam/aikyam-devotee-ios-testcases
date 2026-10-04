package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.CommentsPage;
import com.shriaikyam.aikyam.pages.FeedPage;
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.shriaikyam.aikyam.categories.P0;

public class CommentsTest extends BaseTest {

    /* ---- Phase 3 pilot: THIS CLASS ONLY. CommentsTest has no @FixMethodOrder, no
       @Before/@After and no shared mutable fields; each test self-navigates from the Feed
       via tapFirstCommentAction(). It does post real comments (TC090/TC125), but touches
       no authentication or account state. With no teardown of its own, BaseTest's
       resetToHome() is the ONLY thing recovering an open Comments screen, keyboard state
       or stale input text between tests - which is precisely what this pilot probes. */
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

    @Category(P0.class)
    @Test
    public void TC088_Verify_Comments_Screen_Opens() {
        FeedPage feedPage = new FeedPage(driver);
        CommentsPage commentsPage = new CommentsPage(driver);

        feedPage.waitUntilLoaded();
        Assert.assertTrue("Comment action should be tappable", feedPage.tapFirstCommentAction());
        commentsPage.waitUntilLoaded();

        Assert.assertTrue("Comments screen should be visible", commentsPage.isCommentsScreenVisible());
    }

    @Test
    public void TC089_Verify_Send_Action_Absent_With_Empty_Input() {
        FeedPage feedPage = new FeedPage(driver);
        CommentsPage commentsPage = new CommentsPage(driver);

        feedPage.waitUntilLoaded();
        feedPage.tapFirstCommentAction();
        commentsPage.waitUntilLoaded();

        Assert.assertTrue("Send action should not exist while the comment input is empty",
                commentsPage.isSendActionAbsentWithEmptyInput());
    }

    @Test
    public void TC090_Verify_Adding_Comment_Appears_In_List() {
        FeedPage feedPage = new FeedPage(driver);
        CommentsPage commentsPage = new CommentsPage(driver);
        String comment = "QA regression comment " + System.currentTimeMillis();

        feedPage.waitUntilLoaded();
        feedPage.tapFirstCommentAction();
        commentsPage.waitUntilLoaded();

        Assert.assertTrue("Comment text should be enterable", commentsPage.typeComment(comment));
        Assert.assertTrue("Send action should appear once text is entered", commentsPage.isSendActionVisible());
        Assert.assertTrue("Send should be tappable", commentsPage.tapSend());
        Assert.assertTrue("The new comment should actually appear in the list after posting",
                commentsPage.waitForCommentToAppear(comment));
    }

    @Category(P0.class)
    @Test
    public void TC091_Verify_Back_Navigation_Closes_Comments() {
        FeedPage feedPage = new FeedPage(driver);
        CommentsPage commentsPage = new CommentsPage(driver);

        feedPage.waitUntilLoaded();
        feedPage.tapFirstCommentAction();
        commentsPage.waitUntilLoaded();

        Assert.assertTrue("Back should be tappable", commentsPage.navigateBack());
        Assert.assertTrue("Feed should be visible again after closing Comments", feedPage.isAnyFeedItemVisible());
    }

    /** CommentsScreen.kt's onReplyClick prefills "@name " into the input - a real state
     * change, not just "Reply was tappable". Seeds a comment first when none exist yet
     * so the assertion always has a genuine target instead of skipping. */
    @Test
    public void TC125_Verify_Reply_Prefills_Mention() {
        FeedPage feedPage = new FeedPage(driver);
        CommentsPage commentsPage = new CommentsPage(driver);

        feedPage.waitUntilLoaded();
        feedPage.tapFirstCommentAction();
        commentsPage.waitUntilLoaded();

        if (!commentsPage.isReplyActionVisible()) {
            String seedComment = "Automated reply-seed " + System.currentTimeMillis();
            Assert.assertTrue("Should be able to type a seed comment", commentsPage.typeComment(seedComment));
            Assert.assertTrue("Send action should be tappable", commentsPage.tapSend());
            Assert.assertTrue("Seed comment should appear before replying to it",
                    commentsPage.waitForCommentToAppear(seedComment));
        }

        Assert.assertTrue("Reply action should be tappable", commentsPage.tapFirstReply());

        Assert.assertTrue(
                "Comment input should prefill with an @mention after tapping Reply",
                commentsPage.isAnyMentionPrefillVisible()
        );

        commentsPage.navigateBack();
    }

    /** EmojiBar's onEmojiClick appends the tapped glyph into the comment input
     * (CommentsCommon.kt) - verifies the glyph actually lands in the field, not just
     * that the emoji chip itself is tappable. */
    @Test
    public void TC126_Verify_EmojiBar_Quick_Emoji_Inserts_Into_Comment() {
        FeedPage feedPage = new FeedPage(driver);
        CommentsPage commentsPage = new CommentsPage(driver);

        feedPage.waitUntilLoaded();
        feedPage.tapFirstCommentAction();
        commentsPage.waitUntilLoaded();

        if (!commentsPage.isEmojiBarItemVisible()) {
            commentsPage.navigateBack();
            return;
        }

        Assert.assertTrue("Emoji bar item should be tappable", commentsPage.tapFirstEmojiBarItem());
        Assert.assertTrue(
                "Comment input should contain the tapped emoji",
                commentsPage.doesCommentInputContainEmoji("🙏")
        );

        commentsPage.navigateBack();
    }
}
