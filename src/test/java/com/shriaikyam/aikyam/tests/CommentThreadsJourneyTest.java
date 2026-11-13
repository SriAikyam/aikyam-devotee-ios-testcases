package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.CommentsPage;
import com.shriaikyam.aikyam.pages.FeedPage;
import com.shriaikyam.aikyam.pages.HomePage;
import io.appium.java_client.AppiumBy;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Reply threads and thread delete on the comments sheet (app issues #663, #665). Every
 * test posts its own uniquely tagged comments on the first feed post and deletes them in a
 * finally block; deleting a top-level comment removes its whole thread.
 *
 * Needs a Dev build with comments turned on (AppFeatures.commentsEnabled; off for release
 * 0.1) and the session on the devotee account (Home feed).
 */
public class CommentThreadsJourneyTest extends BaseTest {

    /** Rows of the same thread share one indent; allow a point or two of layout rounding. */
    private static final int SAME_INDENT_TOLERANCE = 2;

    private FeedPage feed;
    private CommentsPage comments;

    @Before
    public void openFeed() {
        HomePage home = new HomePage(driver);
        feed = new FeedPage(driver);
        comments = new CommentsPage(driver);
        home.tapHomeTab();
        feed.waitUntilLoaded();
        feed.tapAllPostsFilter();
        feed.waitUntilLoaded();
        // In the tree (not necessarily on screen) only when the build has comments turned on.
        Assume.assumeFalse("Needs a build with comments turned on (AppFeatures.commentsEnabled)",
                driver.findElements(AppiumBy.accessibilityId("feed.post.comment")).isEmpty());
    }

    private void openComments() {
        Assert.assertTrue("Comment action should be tappable", feed.tapFirstCommentAction());
        comments.waitUntilLoaded();
    }

    private void reopenComments() {
        Assert.assertTrue("Comments sheet should close", comments.navigateBack());
        feed.waitUntilLoaded();
        openComments();
    }

    private void post(String text) {
        Assert.assertTrue("comment should be typeable", comments.typeComment(text));
        Assert.assertTrue("Send should be tappable", comments.tapSend());
        Assert.assertTrue("posted comment should appear: " + text, comments.waitForCommentToAppear(text));
    }

    /** Reply keeps the "@Name " prefill: the text is appended, not typed over it. */
    private void reply(String parent, String text) {
        Assert.assertTrue("Reply should be tappable under " + parent, comments.tapReplyUnder(parent));
        Assert.assertTrue("Reply should prefill an @mention", comments.isAnyMentionPrefillVisible());
        Assert.assertTrue(comments.appendToCommentInput(text));
        Assert.assertTrue("Send should be tappable", comments.tapSend());
        Assert.assertTrue("Reply should appear: " + text, comments.waitForCommentToAppear(text));
    }

    /** Deletes what a test left behind, top-level comments first (each takes its replies). */
    private void cleanUp(String... texts) {
        if (!comments.isCommentsScreenVisible()) {
            if (!feed.tapFirstCommentAction()) {
                return;
            }
            comments.waitUntilLoaded();
        }
        for (String text : texts) {
            if (comments.isCommentVisible(text)) {
                comments.deleteCommentAnywhere(text);
            }
        }
    }

    private static int countOf(String text, char c) {
        return (int) text.chars().filter(ch -> ch == c).count();
    }

    /** #663: the reply is threaded by the server's parentCommentId, so it is still nested
     * after the sheet reloads from the server - not only straight after sending. */
    @Test
    public void TC371_Verify_Reply_Stays_Nested_After_Reopening_Comments() {
        long now = System.currentTimeMillis();
        String parent = "QA thread parent " + now;
        String replyText = "QA thread reply " + now;
        openComments();
        try {
            post(parent);
            reply(parent, replyText);

            reopenComments();
            Assert.assertTrue("Parent should load again", comments.waitForCommentToAppear(parent));
            Assert.assertTrue("Reply should load again", comments.waitForCommentToAppear(replyText));
            int parentLeft = comments.commentTextLeft(parent);
            int replyLeft = comments.commentTextLeft(replyText);
            Assert.assertTrue("Reply should still be indented under its parent after reopening (parent x="
                    + parentLeft + ", reply x=" + replyLeft + ")", replyLeft > parentLeft);
            Assert.assertTrue("Reply should still be listed below its parent",
                    comments.commentTextTop(replyText) > comments.commentTextTop(parent));
        } finally {
            cleanUp(parent, replyText);
        }
    }

    /** #663: replies to one comment share one indent under it, and each shows the "@Name"
     * it answers once - never "@Name @Name". (iOS offers Reply on top-level comments only.) */
    @Test
    public void TC372_Verify_Replies_Share_Indent_With_One_Mention() {
        long now = System.currentTimeMillis();
        String root = "QA r2r root " + now;
        String first = "QA r2r first " + now;
        String second = "QA r2r second " + now;
        openComments();
        try {
            post(root);
            reply(root, first);
            reply(root, second);

            reopenComments();
            Assert.assertTrue(comments.waitForCommentToAppear(root));
            Assert.assertTrue(comments.waitForCommentToAppear(first));
            Assert.assertTrue(comments.waitForCommentToAppear(second));
            int rootLeft = comments.commentTextLeft(root);
            int firstLeft = comments.commentTextLeft(first);
            int secondLeft = comments.commentTextLeft(second);
            Assert.assertTrue("Replies should be indented under the root", firstLeft > rootLeft);
            Assert.assertTrue("Both replies should share one indent (first x="
                    + firstLeft + ", second x=" + secondLeft + ")",
                    Math.abs(secondLeft - firstLeft) <= SAME_INDENT_TOLERANCE);
            Assert.assertTrue("Replies should be listed below the root",
                    comments.commentTextTop(second) > comments.commentTextTop(root));

            for (String r : new String[] {first, second}) {
                String label = comments.commentLabel(r);
                Assert.assertNotNull("Reply should have text", label);
                Assert.assertTrue("Reply should start with the @mention it answers: " + label,
                        label.startsWith("@"));
                Assert.assertEquals("The @mention should show once, not repeated: " + label,
                        1, countOf(label, '@'));
            }
        } finally {
            cleanUp(root, first, second);
        }
    }

    /** #663: removing the "@Name " Reply put in the composer turns the message back into a
     * normal top-level comment. */
    @Test
    public void TC373_Verify_Removing_Mention_Posts_Top_Level_Comment() {
        long now = System.currentTimeMillis();
        String parent = "QA mention parent " + now;
        String plain = "QA mention removed " + now;
        openComments();
        try {
            post(parent);
            Assert.assertTrue(comments.tapReplyUnder(parent));
            Assert.assertTrue("Reply should prefill an @mention", comments.isAnyMentionPrefillVisible());
            // typeComment clears the field first, which removes the "@Name " prefill.
            post(plain);

            reopenComments();
            Assert.assertTrue(comments.waitForCommentToAppear(parent));
            Assert.assertTrue(comments.waitForCommentToAppear(plain));
            Assert.assertTrue("Comment without the @mention should be top level, at the parent's indent",
                    Math.abs(comments.commentTextLeft(plain) - comments.commentTextLeft(parent))
                            <= SAME_INDENT_TOLERANCE);
            String label = comments.commentLabel(plain);
            Assert.assertFalse("Top-level comment should not show an @mention: " + label,
                    label != null && label.startsWith("@"));
        } finally {
            cleanUp(parent, plain);
        }
    }

    /** #665: deleting a comment removes all its replies with it, on screen and on the
     * server. */
    @Test
    public void TC374_Verify_Deleting_Comment_Removes_Whole_Thread() {
        long now = System.currentTimeMillis();
        String root = "QA del root " + now;
        String first = "QA del reply " + now;
        String second = "QA del reply2 " + now;
        openComments();
        try {
            post(root);
            reply(root, first);
            reply(root, second);

            Assert.assertTrue("Delete should be performed on the root", comments.deleteComment(root));
            Assert.assertTrue("Root should disappear", comments.waitForCommentToDisappear(root));
            Assert.assertTrue("Its reply should disappear with it", comments.waitForCommentToDisappear(first));
            Assert.assertTrue("Its second reply should disappear with it",
                    comments.waitForCommentToDisappear(second));

            reopenComments();
            Assert.assertFalse("Root should stay deleted after reopening", comments.isCommentVisible(root));
            Assert.assertFalse("Reply should stay deleted after reopening", comments.isCommentVisible(first));
            Assert.assertFalse("Second reply should stay deleted after reopening",
                    comments.isCommentVisible(second));
        } finally {
            cleanUp(root, first, second);
        }
    }

    /** #665: the feed card's comment count drops by the whole thread, not by one. */
    @Test
    public void TC375_Verify_Feed_Count_Drops_By_Thread_Size_After_Delete() {
        long now = System.currentTimeMillis();
        String root = "QA count root " + now;
        String first = "QA count reply " + now;
        String second = "QA count reply2 " + now;
        int before = feed.firstVisibleCommentCount();
        openComments();
        try {
            post(root);
            reply(root, first);
            reply(root, second);
            Assert.assertTrue(comments.navigateBack());
            feed.waitUntilLoaded();
            Assert.assertEquals("Feed count should rise by the 3 new comments",
                    before + 3, feed.firstVisibleCommentCount());

            openComments();
            Assert.assertTrue(comments.deleteComment(root));
            Assert.assertTrue(comments.waitForCommentToDisappear(second));
            Assert.assertTrue(comments.navigateBack());
            feed.waitUntilLoaded();
            Assert.assertEquals("Feed count should drop by the whole thread (3), back to where it was",
                    before, feed.firstVisibleCommentCount());
        } finally {
            cleanUp(root, first, second);
        }
    }
}
