package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.CommentsPage;
import com.shriaikyam.aikyam.pages.FeedPage;
import com.shriaikyam.aikyam.pages.HomePage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.shriaikyam.aikyam.categories.P0;

/** Comment lifecycle journeys. Every test creates its own uniquely-tagged comment and
 * deletes it in a finally block, verifying the deletion. */
public class CommentsJourneyTest extends BaseTest {

    private FeedPage feed;
    private CommentsPage comments;

    @Before
    public void resetFeed() {
        HomePage home = new HomePage(driver);
        feed = new FeedPage(driver);
        comments = new CommentsPage(driver);
        home.tapHomeTab();
        feed.waitUntilLoaded();
        feed.tapAllPostsFilter();
        feed.waitUntilLoaded();
    }

    private void openComments() {
        Assert.assertTrue("Comment action should be tappable", feed.tapFirstCommentAction());
        comments.waitUntilLoaded();
    }

    private void post(String text) {
        post(text, text);
    }

    /** Waits on a short unique token: matching a ~700-char body is brittle. */
    private void post(String text, String token) {
        Assert.assertTrue("comment should be typeable", comments.typeComment(text));
        Assert.assertTrue("Send should be tappable", comments.tapSend());
        Assert.assertTrue("posted comment should appear: " + token, comments.waitForCommentToAppear(token));
    }

    private void deleteAndVerify(String text) {
        if (!comments.isCommentVisible(text)) {
            return;
        }
        Assert.assertTrue("Delete flow should be reachable for " + text, comments.deleteComment(text));
        Assert.assertTrue("Comment should be gone after delete: " + text,
                comments.waitForCommentToDisappear(text));
    }

    /** Count shown next to the first visible Comment icon; 0 when the app hides it (zero). */
    private int firstCommentCount() {
        return feed.firstVisibleCommentCount();
    }

    @Category(P0.class)
    @Test
    public void TC190_Verify_Own_Comment_Can_Be_Deleted() {
        String text = "QA comment " + System.currentTimeMillis();
        openComments();
        try {
            post(text);
            Assert.assertTrue("Delete should be performed", comments.deleteComment(text));
            Assert.assertTrue("Deleted comment should disappear from the list",
                    comments.waitForCommentToDisappear(text));
        } finally {
            deleteAndVerify(text);
        }
    }

    @Test
    public void TC191_Verify_Feed_Comment_Count_Syncs_After_Add_And_Delete() {
        String text = "QA count " + System.currentTimeMillis();
        int before = firstCommentCount();
        openComments();
        boolean deleted = false;
        try {
            post(text);
            Assert.assertTrue(comments.navigateBack());
            feed.waitUntilLoaded();
            Assert.assertEquals("Feed comment count should increment after adding",
                    before + 1, firstCommentCount());
            openComments();
            Assert.assertTrue(comments.deleteComment(text));
            Assert.assertTrue(comments.waitForCommentToDisappear(text));
            deleted = true;
            Assert.assertTrue(comments.navigateBack());
            feed.waitUntilLoaded();
            Assert.assertEquals("Feed comment count should return after delete",
                    before, firstCommentCount());
        } finally {
            if (!deleted) {
                if (!comments.isCommentsScreenVisible()) {
                    feed.tapFirstCommentAction();
                    comments.waitUntilLoaded();
                }
                deleteAndVerify(text);
            }
        }
    }

    @Test
    public void TC192_Verify_Reply_Is_Nested_Under_Parent() {
        long now = System.currentTimeMillis();
        String parent = "QA parent " + now;
        String reply = "QA reply " + now;
        openComments();
        try {
            post(parent);
            Assert.assertTrue("Reply should be tappable under the parent", comments.tapReplyUnder(parent));
            Assert.assertTrue("Reply should prefill an @mention", comments.isAnyMentionPrefillVisible());
            Assert.assertTrue(comments.appendToCommentInput(reply));
            Assert.assertTrue("Send should be tappable", comments.tapSend());
            Assert.assertTrue("Reply should appear", comments.waitForCommentToAppear(reply));
            int parentLeft = comments.commentTextLeft(parent);
            int replyLeft = comments.commentTextLeft(reply);
            Assert.assertTrue("Reply should be indented relative to parent (parent x=" + parentLeft
                    + ", reply x=" + replyLeft + ")", replyLeft > parentLeft);
            Assert.assertTrue("Reply should be listed below its parent",
                    comments.commentTextTop(reply) > comments.commentTextTop(parent));
        } finally {
            deleteAndVerify(parent);
            if (comments.isCommentVisible(reply)) {
                deleteAndVerify(reply);
            }
        }
    }

    @Test
    public void TC193_Verify_Long_Comment_Show_More_And_Show_Less() {
        String tag = "QA long " + System.currentTimeMillis();
        StringBuilder sb = new StringBuilder(tag);
        for (int i = 0; i < 40; i++) {
            sb.append(" lorem ipsum dolor");
        }
        openComments();
        try {
            post(sb.toString(), tag);
            Assert.assertTrue("Long comment should be collapsed with Show more", comments.isShowMoreVisible());
            Assert.assertTrue(comments.tapShowMore());
            Assert.assertTrue("After expanding, Show less should be offered", comments.isShowLessVisible());
            Assert.assertTrue(comments.tapShowLess());
            Assert.assertTrue("After collapsing, Show more should return", comments.isShowMoreVisible());
        } finally {
            deleteAndVerify(tag);
        }
    }
}
