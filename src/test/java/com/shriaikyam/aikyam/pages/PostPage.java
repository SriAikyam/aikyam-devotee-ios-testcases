package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/** Create Post: CreatePostView + CreatePostCaptionEditor + EmojiPickerView (all under
 * FeatureFeed/Presentation). Every `id(...)` below is paired with a label/type fallback so
 * the page still resolves before the identifiers land in those views. */
public class PostPage extends BasePage {

    // The Home feed's own entry point; its identifier belongs to the feed screen.
    private final By createPostAction = accessibilityId("Create Post");

    private final By addMediaAction = id("createpost.addmedia");
    private final By addMediaPlaceholder = textContains("Tap to upload");
    private final By addMoreMediaAction = id("createpost.media.add");
    private final By mediaEditAction = id("createpost.media.edit");
    private final By mediaEditButton = button("label == 'Edit'");

    // CreatePostCaptionTextView is a UITextView - the only TextView on this screen (location
    // is a TextField). iOS shows no caption placeholder (see parity report).
    private final By captionEditor = id("createpost.caption");
    private final By captionField = By.className("XCUIElementTypeTextView");

    private final By boldAction = id("createpost.bold");
    private final By boldLabel = accessibilityId("Bold");
    private final By emojiAction = id("createpost.emoji");
    private final By emojiSymbol = button("name == 'face.smiling' OR label == 'Emoji' OR label == 'Smiling face'");
    private final By mentionAction = id("createpost.mention");
    private final By mentionSymbol = button("name == 'at' OR label == 'at' OR label == '@'");
    private final By hashtagAction = id("createpost.hashtag");
    private final By hashtagSymbol = button("name == 'number' OR label == 'number' OR label == '#'");
    private final By micAction = id("createpost.mic");
    private final By micLabel = accessibilityId("Voice input");
    // iOS has no system speech dialog: the editor shows this prompt inline while listening.
    private final By micDialogPrompt = textContains("Speak your caption");

    // EmojiPickerView, shown inline where the keyboard would be.
    private final By emojiSearchField = id("emojipicker.search");
    private final By emojiSearchPlaceholder = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeTextField' AND (placeholderValue == 'Search with text or emoji'"
                    + " OR value == 'Search with text or emoji')");
    private final By emojiCloseAction = id("emojipicker.close");
    private final By emojiCloseLabel = button("label == 'Close'");
    private final By emojiNoResultsText = text("No emojis found");

    // The bottom bar button reads "Post", or "Schedule" while scheduling is on, and is
    // disabled until at least one media item is attached.
    private final By postAction = id("createpost.publish");
    private final By postButton = button("label BEGINSWITH 'Post' OR label BEGINSWITH 'Schedule'");

    private final By scheduleToggle = id("createpost.schedule");
    private final By scheduleSwitch = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeSwitch' AND label CONTAINS 'Schedule a Post?'");
    private final By scheduleDatePicker = id("createpost.schedule.date");
    private final By scheduleDateLabel = textContains("Publish at");
    // iOS pre-fills the date one hour ahead, so its only schedule validation is a past time.
    private final By scheduleDateTimeRequiredError = textContains("Pick a time in the future to schedule this post");

    private final By locationField = id("createpost.location");
    private final By locationPlaceholder = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeTextField' AND (placeholderValue == 'Enter your location'"
                    + " OR value == 'Enter your location')");

    // PHPicker runs out of process; its cells are Images labelled "Photo, <date>" / "Video, <date>".
    private final By pickerFirstPhoto = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeImage' AND label BEGINSWITH 'Photo'");
    private final By pickerFirstVideo = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeImage' AND label BEGINSWITH 'Video'");
    // Multi-select PHPicker (maxSelectionCount > 1) needs an explicit "Add".
    private final By pickerAddAction = button("label == 'Add' OR label == 'Done'");

    public PostPage(IOSDriver driver) {
        super(driver);
    }

    private static By button(String condition) {
        return AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (" + condition + ")");
    }

    public boolean openCreatePost() {
        return tapFirstVisible(createPostAction, textContains("Create Post"));
    }

    public void waitUntilLoaded() {
        waitForAnyVisible(addMediaAction, addMediaPlaceholder, captionEditor, postAction, scheduleSwitch);
    }

    public boolean isCreatePostVisible() {
        // captionField (a bare TextView class matcher) is excluded: it also matches text views
        // on other screens and would report "already on Create Post" from elsewhere.
        return isAnyVisible(addMediaAction, addMediaPlaceholder, captionEditor, postAction, scheduleSwitch);
    }

    public boolean isCaptionEditorVisible() {
        return isAnyVisible(captionEditor, captionField);
    }

    public boolean isCaptionToolbarVisible() {
        return isAnyVisible(boldAction, boldLabel, emojiAction, emojiSymbol, mentionAction, mentionSymbol,
                hashtagAction, hashtagSymbol);
    }

    // iOS: Create Post has no audience/visibility picker; posts always go out Public (see parity report).
    public boolean isVisibilityOptionsVisible() {
        return false;
    }

    public boolean isPostButtonVisible() {
        return isAnyVisible(postAction, postButton);
    }

    public boolean isPostButtonEnabled() {
        // Polls: the button enables only once picked media has loaded into the grid.
        try {
            return new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ignored -> isAnyPostButtonEnabled());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** iOS has one PhotosPicker for images and videos (no Image/Video/Camera chooser), so
     * both flows just open it. */
    public boolean startImagePostFlow() {
        return tapFirstVisible(addMediaAction, addMediaPlaceholder, addMoreMediaAction);
    }

    public boolean startVideoPostFlow() {
        return tapFirstVisible(addMediaAction, addMediaPlaceholder, addMoreMediaAction);
    }

    /** Opens the PhotosPicker and attaches the first still image in the simulator's library. */
    public boolean attachFirstGalleryImage() {
        if (!tapFirstVisible(addMediaAction, addMediaPlaceholder, addMoreMediaAction)) {
            return false;
        }
        if (!tapFirstVisible(pickerFirstPhoto)) {
            return false;
        }
        return confirmPickerSelection();
    }

    /** Opens the PhotosPicker and attaches the first video. Requires a video in the
     * simulator's photo library (`xcrun simctl addmedia booted <file.mp4>`). */
    public boolean attachFirstGalleryVideo() {
        if (!tapFirstVisible(addMediaAction, addMediaPlaceholder, addMoreMediaAction)) {
            return false;
        }
        if (!scrollUntilVisible(pickerFirstVideo)) {
            return false;
        }
        if (!tapFirstVisible(pickerFirstVideo)) {
            return false;
        }
        return confirmPickerSelection();
    }

    private boolean confirmPickerSelection() {
        if (!isVisibleNow(pickerAddAction)) {
            // Single-select pickers return on the tap itself.
            return true;
        }
        return tapFirstVisible(pickerAddAction);
    }

    /** iOS dismisses Create Post straight away - there is no Discard dialog (see parity report). */
    public boolean tapBack() {
        return navigateBack();
    }

    /** The pencil on a thumbnail opens CreatePostMediaEditorView for images and videos
     * alike; tapping a video thumbnail itself only toggles its inline preview. */
    public boolean tapFirstMediaThumbnail() {
        return tapFirstVisible(mediaEditAction, mediaEditButton);
    }

    public void dismissKeyboardIfShown() {
        hideKeyboardIfShown();
    }

    // iOS: Create Post has no Discard/Keep Editing dialog; Back dismisses directly (see parity report).
    public boolean isDiscardDialogVisible() {
        return false;
    }

    // iOS: Create Post has no Discard/Keep Editing dialog (see parity report).
    public boolean tapKeepEditing() {
        return false;
    }

    // iOS: Create Post has no Discard/Keep Editing dialog (see parity report).
    public boolean tapDiscardConfirm() {
        return false;
    }

    public boolean addCaption(String caption) {
        if (!typeIntoFirstVisible(caption, captionEditor, captionField)) {
            return false;
        }
        if (isCaptionPresent(caption)) {
            return true;
        }
        // One retry in case the first keystrokes landed while the field was still taking focus.
        return typeIntoFirstVisible(caption, captionEditor, captionField)
                && isCaptionPresent(caption);
    }

    public boolean isCaptionPresent(String caption) {
        return isAnyVisible(textContains(caption)) || driver.getPageSource().contains(caption);
    }

    public boolean toggleBold() {
        return tapFirstVisible(boldAction, boldLabel);
    }

    /** Idempotent: if the panel is already open its search field is showing. */
    public boolean openEmojiPicker() {
        if (isVisibleNow(emojiSearchField, emojiSearchPlaceholder)) {
            return true;
        }
        return tapFirstVisible(emojiAction, emojiSymbol);
    }

    public boolean closeEmojiPicker() {
        return tapFirstVisible(emojiCloseAction, emojiCloseLabel);
    }

    /** The tab strip is icon-only on iOS, so this matches the category's section header in
     * the emoji grid (visible once that section is on screen). */
    public boolean isEmojiCategoryVisible(String categoryLabel) {
        return isAnyVisible(accessibilityId(categoryLabel), text(categoryLabel));
    }

    public boolean searchEmoji(String query) {
        return typeIntoFirstVisible(query, emojiSearchField, emojiSearchPlaceholder);
    }

    public boolean isNoEmojisFoundVisible() {
        return isAnyVisible(emojiNoResultsText);
    }

    /** Emoji cells are Buttons labelled with the raw glyph. */
    public boolean selectEmojiAndConfirmInserted(String emojiGlyph) {
        if (!tapFirstVisible(button("label == " + predicateLiteral(emojiGlyph)), text(emojiGlyph))) {
            return false;
        }
        return isCaptionPresent(emojiGlyph);
    }

    public boolean insertMentionSymbol() {
        return tapFirstVisible(mentionAction, mentionSymbol);
    }

    public boolean insertHashtagSymbol() {
        return tapFirstVisible(hashtagAction, hashtagSymbol);
    }

    public boolean tapPost() {
        return tapFirstVisible(postAction, postButton);
    }

    public void backgroundAppFor(Duration duration) {
        driver.runAppInBackground(duration);
    }

    /** True only if every matching button is confirmed disabled, so an element that was not
     * found can't be mistaken for a disabled one. */
    public boolean isPostButtonDisabled() {
        List<WebElement> buttons = postButtons();
        if (buttons.isEmpty()) {
            return false;
        }
        for (WebElement element : buttons) {
            try {
                if (element.isEnabled()) {
                    return false;
                }
            } catch (RuntimeException ignored) {
                return false;
            }
        }
        return true;
    }

    public boolean isScheduleToggleVisible() {
        hideKeyboardIfShown();
        return scrollUntilVisible(scheduleToggle, scheduleSwitch);
    }

    /** Turns scheduling on and confirms it via the "Publish at" date picker that only
     * renders once it is on. No-ops (returns true) if already enabled. */
    public boolean enableSchedulePost() {
        hideKeyboardIfShown();
        if (!scrollUntilVisible(scheduleToggle, scheduleSwitch)) {
            return false;
        }
        if (isVisibleNow(scheduleDatePicker, scheduleDateLabel)) {
            return true;
        }
        try {
            WebElement toggle = waitForAnyVisible(scheduleToggle, scheduleSwitch);
            // A SwiftUI Toggle spans the row, and a tap on its centre lands on the label,
            // which does not flip it - tap the switch at the trailing edge instead.
            Point location = toggle.getLocation();
            Dimension size = toggle.getSize();
            tapPoint(location.getX() + size.getWidth() - 30, location.getY() + size.getHeight() / 2);
        } catch (RuntimeException exception) {
            return false;
        }
        return isAnyVisible(scheduleDatePicker, scheduleDateLabel);
    }

    public boolean tapScheduleSubmit() {
        return tapFirstVisible(postAction, button("label BEGINSWITH 'Schedule'"));
    }

    /** iOS validates only a past time (alert "Pick a time in the future..."); it never
     * asks for a missing date because the picker is pre-filled. */
    public boolean isScheduleDateTimeRequiredErrorVisible() {
        return isAnyVisible(scheduleDateTimeRequiredError);
    }

    public boolean enterLocation(String location) {
        hideKeyboardIfShown();
        scrollBackUntilVisible(locationField, locationPlaceholder);
        return typeIntoFirstVisible(location, locationField, locationPlaceholder);
    }

    public boolean isLocationPresent(String location) {
        return isAnyVisible(textContains(location));
    }

    /** Taps the caption toolbar mic and confirms listening started - iOS shows the
     * "Speak your caption..." prompt inline in the toolbar instead of a system dialog. The
     * first run on a simulator raises the Speech/Microphone permission alerts first. */
    public boolean tapMicAndConfirmDialogOpens() {
        if (!tapFirstVisible(micAction, micLabel)) {
            return false;
        }
        try {
            waitForAnyVisible(micDialogPrompt);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** The mic button toggles listening off again; there is no dialog to back out of. */
    public void dismissMicDialog() {
        try {
            if (isVisibleNow(micDialogPrompt)) {
                tapFirstVisible(micAction, micLabel);
            }
        } catch (RuntimeException ignored) {
            // Best-effort.
        }
    }

    // iOS: Create Post has no Save Draft action (no draft persistence, see parity report).
    public boolean isSaveDraftVisible() {
        return false;
    }

    // iOS: Create Post has no Save Draft action (no draft persistence, see parity report).
    public boolean tapSaveDraft() {
        return false;
    }

    private List<WebElement> postButtons() {
        List<WebElement> byId = driver.findElements(postAction);
        return byId.isEmpty() ? driver.findElements(postButton) : byId;
    }

    private boolean isAnyPostButtonEnabled() {
        for (WebElement element : postButtons()) {
            try {
                if (element.isEnabled()) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // Keep scanning other matches.
            }
        }
        return false;
    }
}
