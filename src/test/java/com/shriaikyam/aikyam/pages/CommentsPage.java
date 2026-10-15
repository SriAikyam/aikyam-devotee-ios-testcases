package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

/** CommentsView, reached via FeedPage.tapFirstCommentAction() (presented as a sheet from the
 * Home feed). Send only renders once the composer text is non-blank - its absence with empty
 * input is a genuine negative check, not a missing locator. */
public class CommentsPage extends BasePage {

    // Title is "Comments" while loading and "Comments (N)" once the total is known.
    private final By titleLabel = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND label BEGINSWITH 'Comments' AND name != 'comments.text'");
    private final By commentInputField = id("comments.input");
    private final By sendAction = id("comments.send");
    private final By noCommentsYetText = id("comments.empty");
    private final By replyAction = id("comments.reply");

    public CommentsPage(IOSDriver driver) {
        super(driver);
    }

    public void waitUntilLoaded() {
        waitForAnyVisible(titleLabel, commentInputField, noCommentsYetText);
    }

    public boolean isCommentsScreenVisible() {
        return isAnyVisible(titleLabel, commentInputField);
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
        return typeIntoFirstVisible(text, commentInputField);
    }

    public boolean isSendActionVisible() {
        return isAnyVisible(sendAction);
    }

    public boolean tapSend() {
        return tapFirstVisible(sendAction);
    }

    /** The new comment lands after the backend round-trip - poll rather than check once
     * immediately after tapSend(). */
    public boolean waitForCommentToAppear(String commentText) {
        By locator = commentText(commentText);
        // Waiting alone is not enough. A newly posted comment is appended to the END of the
        // thread, and that thread grows on every suite run (TC090 posted without cleaning up
        // for a long time), so the new row is routinely below the fold - waitForAnyVisible
        // would time out on a comment that posted perfectly well. Wait first for the render,
        // then scroll for it.
        if (isAnyVisible(locator)) {
            return true;
        }
        return scrollUntilVisible(COMMENT_SCROLL_BUDGET, locator);
    }

    /** Threads on the shared dev account can be long; 4 swipes did not reach the end. */
    private static final int COMMENT_SCROLL_BUDGET = 10;

    /** FlatBackScreenHeader's back button carries no label or identifier of its own (shared
     * component), so take the button on the title's row, left of it. Falls back to the
     * generic back/dismiss (the sheet swipes down). */
    @Override
    public boolean navigateBack() {
        hideKeyboardIfShown();
        WebElement title = firstDisplayed(titleLabel);
        if (title != null) {
            Rectangle t = title.getRect();
            int rowCenter = t.getY() + t.getHeight() / 2;
            WebElement best = null;
            for (WebElement b : driver.findElements(By.className("XCUIElementTypeButton"))) {
                try {
                    Rectangle r = b.getRect();
                    if (b.isDisplayed() && r.getX() < t.getX()
                            && Math.abs(r.getY() + r.getHeight() / 2 - rowCenter) < 25
                            && (best == null || r.getX() < best.getRect().getX())) {
                        best = b;
                    }
                } catch (RuntimeException ignored) {
                    // re-rendered mid-scan
                }
            }
            if (best != null) {
                best.click();
                return waitUntilGone(titleLabel);
            }
        }
        BasePage.navigateBack(driver);
        return waitUntilGone(titleLabel);
    }

    public boolean isReplyActionVisible() {
        return isAnyVisible(replyAction);
    }

    public boolean tapFirstReply() {
        return tapFirstVisible(replyAction);
    }

    private By inputWhere(String condition) {
        return AppiumBy.iOSNsPredicateString("name == 'comments.input' AND " + condition);
    }

    /** Reply sets the composer text to "@name " directly (CommentsView onReply), so the field
     * must show that exact prefix afterward. */
    public boolean isCommentInputPrefilledWithMention(String expectedName) {
        return isAnyVisible(inputWhere("value BEGINSWITH " + predicateLiteral("@" + expectedName + " ")));
    }

    /** Same check without requiring the exact display name up front - reply targets
     * whichever comment happens to render first, so the name isn't known ahead of time. */
    public boolean isAnyMentionPrefillVisible() {
        return isAnyVisible(inputWhere("value BEGINSWITH '@'"));
    }

    // iOS: CommentsView has no quick-pick emoji bar, only an emoji-picker sheet behind the
    // composer's Emoji button (see parity report).
    public boolean isEmojiBarItemVisible() {
        return false;
    }

    // iOS: no emoji bar to tap (see isEmojiBarItemVisible).
    public boolean tapFirstEmojiBarItem() {
        return false;
    }

    public boolean doesCommentInputContainEmoji(String emoji) {
        return isAnyVisible(inputWhere("value CONTAINS " + predicateLiteral(emoji)));
    }

    // ---- Journey helpers (TC190+): row-relative locators for one specific comment ----

    private final By moreAction = id("comments.more");
    private final By deleteMenuItem = id("comments.delete");
    private final By deleteMenuText = text("Delete");
    private final By showMoreAction = expandToggle("Show more");
    private final By showLessAction = expandToggle("Show less");

    private static By expandToggle(String label) {
        return AppiumBy.iOSNsPredicateString(
                "name == 'comments.expand' AND label == " + predicateLiteral(label));
    }

    /** Comment body text, not the composer (which may still hold the same words). */
    private static By commentText(String value) {
        return AppiumBy.iOSNsPredicateString(
                "name == 'comments.text' AND label CONTAINS " + predicateLiteral(value));
    }

    private WebElement firstDisplayed(By locator) {
        for (WebElement e : driver.findElements(locator)) {
            try {
                if (e.isDisplayed()) {
                    return e;
                }
            } catch (RuntimeException ignored) {
                // stale during re-render
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
        return firstDisplayed(commentText(commentText)) != null;
    }

    /** Left edge x of the comment text element, or -1 if absent. */
    public int commentTextLeft(String commentText) {
        WebElement e = firstDisplayed(commentText(commentText));
        return e == null ? -1 : e.getRect().getX();
    }

    public int commentTextTop(String commentText) {
        WebElement e = firstDisplayed(commentText(commentText));
        return e == null ? -1 : e.getRect().getY();
    }

    public boolean waitForCommentToDisappear(String commentText) {
        return waitUntilGone(commentText(commentText));
    }

    /** Taps the Reply button directly below the given comment's text. */
    public boolean tapReplyUnder(String commentText) {
        WebElement c = firstDisplayed(commentText(commentText));
        if (c == null) {
            return false;
        }
        int bottom = c.getRect().getY() + 1;
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
     * nearest More icon at or above the comment text, on the name row) and taps Delete. */
    public boolean deleteComment(String commentText) {
        hideKeyboardIfShown();
        WebElement c = firstDisplayed(commentText(commentText));
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
            waitForAnyVisible(deleteMenuItem, deleteMenuText);
        } catch (RuntimeException e) {
            return false;
        }
        return tapFirstVisible(deleteMenuItem, deleteMenuText);
    }

    /* Show more / Show less sit at the bottom of the comment they belong to, and TC193
     * deliberately posts a ~700-character one. Expanding it makes the row taller than the
     * viewport, so the control the next assertion looks for is off-screen - a bare
     * visibility probe then reports false for a control that is present and working.
     * Scrolling for it matches how the rest of the suite treats "visible" (see
     * FamilyMembersPage scrolling to its member-count chip before asserting). */
    public boolean isShowMoreVisible() {
        return isAnyVisible(showMoreAction) || scrollUntilVisible(COMMENT_SCROLL_BUDGET, showMoreAction);
    }

    public boolean tapShowMore() {
        return isShowMoreVisible() && tapFirstVisible(showMoreAction);
    }

    public boolean isShowLessVisible() {
        return isAnyVisible(showLessAction) || scrollUntilVisible(COMMENT_SCROLL_BUDGET, showLessAction);
    }

    public boolean tapShowLess() {
        return isShowLessVisible() && tapFirstVisible(showLessAction);
    }
}
