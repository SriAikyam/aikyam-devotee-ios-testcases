package com.shriaikyam.aikyam.pages;

import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import java.util.List;

/** Reached via FeedPage.tapFirstCommentAction(). Send only renders once the comment
 * text is non-blank (confirmed via source read) - its absence with empty input is a
 * genuine negative check, not a missing locator. Adding a comment does not
 * optimistically insert it; the screen refetches the whole list once the backend call
 * completes, so waitForCommentToAppear() must poll rather than check once. */
public class CommentsPage extends BasePage {

    private final By titleLabel = text("Comments");
    private final By commentInputPlaceholder = textContains("What do you think of this?");
    private final By commentInputField = io.appium.java_client.AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\")"
    );
    private final By sendAction = accessibilityId("Send");
    private final By noCommentsYetText = text("No comments yet");
    private final By backAction = accessibilityId("Back");
    // First default EmojiBar item (CommentsCommon.kt's defaultEmojis list) - guaranteed
    // first unless the account has "recent" emojis prepended ahead of it.
    private final By firstEmojiBarItem = text("🙏");
    // "Reply" is a plain Text node under each comment (CommentsCommon.kt) - tapping the
    // first one on screen replies to whichever comment happens to render first.
    private final By replyAction = text("Reply");

    public CommentsPage(IOSDriver driver) {
        super(driver);
    }

    public void waitUntilLoaded() {
        waitForAnyVisible(titleLabel, commentInputPlaceholder, commentInputField, noCommentsYetText);
    }

    public boolean isCommentsScreenVisible() {
        return isAnyVisible(titleLabel, commentInputPlaceholder, commentInputField);
    }

    public boolean isNoCommentsEmptyStateVisible() {
        return isAnyVisible(noCommentsYetText);
    }

    /** Send is only rendered once the input is non-blank - this is the real negative
     * check: no text typed means no Send action to find at all. */
    public boolean isSendActionAbsentWithEmptyInput() {
        return !isAnyVisible(sendAction);
    }

    public boolean typeComment(String text) {
        return typeIntoFirstVisible(text, commentInputField, commentInputPlaceholder);
    }

    public boolean isSendActionVisible() {
        return isAnyVisible(sendAction);
    }

    public boolean tapSend() {
        return tapFirstVisible(sendAction);
    }

    /** Adding a comment triggers a full list refetch (not an optimistic insert) before
     * it appears - poll rather than check once immediately after tapSend(). */
    public boolean waitForCommentToAppear(String commentText) {
        try {
            waitForAnyVisible(textContains(commentText));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean navigateBack() {
        return tapFirstVisible(backAction);
    }

    public boolean isReplyActionVisible() {
        return isAnyVisible(replyAction);
    }

    public boolean tapFirstReply() {
        return tapFirstVisible(replyAction);
    }

    /** Reply prefill is a real state assertion, not "tap didn't crash" - CommentsScreen.kt's
     * onReplyClick sets the input's TextFieldValue to "@name " directly, so the field must
     * show that exact prefix afterward. */
    public boolean isCommentInputPrefilledWithMention(String expectedName) {
        return isAnyVisible(io.appium.java_client.AppiumBy.androidUIAutomator(
                "new UiSelector().className(\"android.widget.EditText\").textStartsWith(\"@" + expectedName + " \")"
        ));
    }

    /** Same check without requiring the exact display name up front - reply targets
     * whichever comment happens to render first, so the name isn't known ahead of time. */
    public boolean isAnyMentionPrefillVisible() {
        return isAnyVisible(io.appium.java_client.AppiumBy.androidUIAutomator(
                "new UiSelector().className(\"android.widget.EditText\").textStartsWith(\"@\")"
        ));
    }

    public boolean isEmojiBarItemVisible() {
        return isAnyVisible(firstEmojiBarItem);
    }

    public boolean tapFirstEmojiBarItem() {
        return tapFirstVisible(firstEmojiBarItem);
    }

    /** EmojiBar's onEmojiClick appends the glyph to whatever text is already in the
     * input (CommentsCommon.kt) - checking the field now contains it is the real signal,
     * not just "the emoji chip was tappable". */
    public boolean doesCommentInputContainEmoji(String emoji) {
        return isAnyVisible(io.appium.java_client.AppiumBy.androidUIAutomator(
                "new UiSelector().className(\"android.widget.EditText\").textContains(\"" + emoji + "\")"
        ));
    }

    // ---- Journey helpers (TC190+): row-relative locators for one specific comment ----

    private final By moreAction = accessibilityId("More");
    private final By deleteMenuItem = text("Delete");
    private final By showMoreAction = text("Show more");
    private final By showLessAction = text("Show less");

    private WebElement firstDisplayed(By locator) {
        for (WebElement e : driver.findElements(locator)) {
            try {
                if (e.isDisplayed()) {
                    return e;
                }
            } catch (RuntimeException ignored) {
                // stale during recomposition
            }
        }
        return null;
    }

    /** Appends to the input without clearing it, so a Reply "@name " prefill survives. */
    public boolean appendToCommentInput(String value) {
        WebElement field = firstDisplayed(commentInputField);
        if (field == null) {
            return false;
        }
        field.sendKeys(value);
        return true;
    }

    public boolean isCommentVisible(String commentText) {
        return firstDisplayed(textContains(commentText)) != null;
    }

    /** Left edge x of the comment text element, or -1 if absent. */
    public int commentTextLeft(String commentText) {
        WebElement e = firstDisplayed(textContains(commentText));
        return e == null ? -1 : e.getRect().getX();
    }

    public int commentTextTop(String commentText) {
        WebElement e = firstDisplayed(textContains(commentText));
        return e == null ? -1 : e.getRect().getY();
    }

    public boolean waitForCommentToDisappear(String commentText) {
        return waitUntilGone(textContains(commentText));
    }

    /** Taps the Reply label directly below the given comment's text. */
    public boolean tapReplyUnder(String commentText) {
        WebElement c = firstDisplayed(textContains(commentText));
        if (c == null) {
            return false;
        }
        int bottom = c.getRect().getY() + 1; // Reply's touch target overlaps the text bottom
        WebElement best = null;
        for (WebElement r : driver.findElements(replyAction)) {
            if (r.getRect().getY() >= bottom
                    && (best == null || r.getRect().getY() < best.getRect().getY())) {
                best = r;
            }
        }
        if (best == null) {
            return false;
        }
        best.click();
        return true;
    }

    /** Opens the owner "More" menu belonging to the comment row of the given text (the
     * nearest More icon at or above the comment text) and taps Delete. */
    public boolean deleteComment(String commentText) {
        hideKeyboardIfShown();
        WebElement c = firstDisplayed(textContains(commentText));
        if (c == null) {
            return false;
        }
        int textY = c.getRect().getY();
        WebElement best = null;
        for (WebElement m : driver.findElements(moreAction)) {
            int y = m.getRect().getY();
            if (y <= textY && (best == null || y > best.getRect().getY())) {
                best = m;
            }
        }
        if (best == null) {
            return false;
        }
        best.click();
        try {
            waitForAnyVisible(deleteMenuItem);
        } catch (RuntimeException e) {
            return false;
        }
        return tapFirstVisible(deleteMenuItem);
    }

    public boolean isShowMoreVisible() {
        return isAnyVisible(showMoreAction);
    }

    public boolean tapShowMore() {
        return tapFirstVisible(showMoreAction);
    }

    public boolean isShowLessVisible() {
        return isAnyVisible(showLessAction);
    }

    public boolean tapShowLess() {
        return tapFirstVisible(showLessAction);
    }
}
