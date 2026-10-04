package com.shriaikyam.aikyam.pages;

import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class PostPage extends BasePage {

    private final By createPostAction = accessibilityId("Create Post");
    private final By createPostScreen = accessibilityId("Create post screen");
    private final By addMediaAction = accessibilityId("Add image or video");
    private final By imageOption = accessibilityId("Image");
    private final By videoOption = accessibilityId("Video");
    private final By galleryOption = textContains("Gallery");
    private final By cameraOption = textContains("Camera");
    private final By captionEditor = accessibilityId("Caption editor");
    private final By captionField = io.appium.java_client.AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\")"
    );
    private final By captionPlaceholder = textContains("caption");
    // Bold/Mention/Hashtag render as plain Text nodes ("B", "@", "#") with no
    // contentDescription, so accessibility-id locators never match them.
    private final By boldAction = text("B");
    private final By emojiAction = accessibilityId("Emoji");
    // The picker is a Compose AlertDialog; dismissing without picking an emoji means
    // tapping its "Close" TextButton.
    private final By emojiCloseAction = text("Close");
    // Category tab-strip icons carry the section's own title as contentDescription
    // (e.g. "Devotional", "Hearts") - confirmed via EmojiCatalog.kt/EmojiPicker.kt.
    private final By emojiSearchPlaceholder = textContains("Search with text or emoji");
    private final By emojiNoResultsText = text("No emojis found");
    private final By mentionAction = text("@");
    private final By hashtagAction = text("#");
    private final By visibilityPrompt = textContains("Who can see this?");
    private final By publicOption = text("Public");
    private final By followingOption = text("Following");
    private final By privateOption = text("Private");
    private final By postAction = text("Post");
    // The "Post" Text node always reports enabled=true even when the button is
    // disabled - the real enabled/disabled state lives on its clickable parent.
    private final By postButtonContainer = By.xpath("(//*[@text='Post'])[1]/..");
    // The Android system Photo Picker (com.google.android.photopicker) exposes each
    // still image with this exact description prefix, distinguishing it from videos
    // ("Video taken on ... with duration ...") and broken/unloaded thumbnails.
    private final By pickerFirstPhoto = io.appium.java_client.AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Photo taken on\")"
    );
    // Same system Photo Picker, videos are described with "Video taken on ... with duration ...".
    private final By pickerFirstVideo = io.appium.java_client.AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionStartsWith(\"Video taken on\")"
    );
    private final By pickerDoneAction = text("Done");
    // DocumentsUI (the picker used below Android 13, where com.google.android.photopicker
    // does not exist) carries no per-photo description at all - each entry is labelled
    // with its filename. One locator per extension rather than one textMatches regex:
    // verified live against this build, textMatches("(?i).*\\.(png|jpg)$") returns an
    // EMPTY list because the backslash escape does not survive the UiAutomator expression
    // string, while textContains(".png") matches and its click attaches the image.
    // Extensions are listed explicitly so the .mp4 test assets in /sdcard/Download are
    // never selected by an image test.
    private final By[] documentsUiImageLabels = new By[] {
            textContains(".png"), textContains(".jpg"),
            textContains(".jpeg"), textContains(".webp")
    };
    private final By scheduleToggle = text("Schedule a Post?");
    private final By pickDateAction = text("Pick date");
    private final By scheduleDateTimeRequiredError = textContains("Pick a date and time to schedule this post");
    private final By scheduleSubmitAction = text("Schedule");
    private final By backAction = accessibilityId("Back");
    // DiscardPostDialog.kt - confirmed via source, no contentDescription/testTag on
    // either button, locate by exact text.
    private final By discardDialogTitle = text("Discard post?");
    private final By discardConfirmAction = text("Discard");
    private final By keepEditingAction = text("Keep Editing");
    private final By locationField = textContains("Enter your location");
    // The mic hands off to the system's own speech-recognition dialog (a separate
    // "Google" activity, not part of this app's Compose tree) - confirmed live via
    // "Speak your caption..." rendering full-screen over Create Post. There is
    // nothing further to assert inside it from here; isMicDialogVisible() only
    // proves the hand-off happened.
    private final By micAction = accessibilityId("Voice input");
    private final By micDialogPrompt = textContains("Speak your caption");
    private final By saveDraftAction = text("Save Draft");

    public PostPage(IOSDriver driver) {
        super(driver);
    }

    public boolean openCreatePost() {
        return tapFirstVisible(createPostAction, textContains("Create Post"), textContains("Create"));
    }

    public void waitUntilLoaded() {
        waitForAnyVisible(createPostScreen, addMediaAction, captionEditor, captionField, captionPlaceholder, postAction);
    }

    public boolean isCreatePostVisible() {
        // captionField (a bare EditText class matcher) is deliberately excluded here:
        // it also matches unrelated EditTexts on other screens (e.g. feed search/comment
        // boxes), which previously caused this check to falsely report "already on
        // Create Post" when the app was actually elsewhere.
        return isAnyVisible(createPostScreen, addMediaAction, captionEditor, captionPlaceholder, postAction);
    }

    public boolean isCaptionEditorVisible() {
        return isAnyVisible(captionEditor, captionField, captionPlaceholder);
    }

    public boolean isCaptionToolbarVisible() {
        return isAnyVisible(boldAction, emojiAction, mentionAction, hashtagAction);
    }

    public boolean isVisibilityOptionsVisible() {
        // The visibility section sits below the caption box; it drops out of the
        // accessibility tree entirely while the on-screen keyboard covers it.
        hideKeyboardIfShown();
        return isAnyVisible(visibilityPrompt, publicOption, followingOption, privateOption);
    }

    public boolean isPostButtonVisible() {
        return isAnyVisible(postAction);
    }

    public boolean isPostButtonEnabled() {
        // Enabling Post after a link-embed (e.g. a YouTube URL in the caption) depends
        // on the app fetching link-preview metadata, so this must poll rather than
        // check once - an immediate check can race the embed and read stale state.
        try {
            return new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ignored -> isAnyPostButtonEnabled());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean startImagePostFlow() {
        return tapFirstVisible(imageOption, textContains("Image"), addMediaAction, galleryOption, cameraOption);
    }

    public boolean startVideoPostFlow() {
        return tapFirstVisible(videoOption, textContains("Video"), addMediaAction, galleryOption, cameraOption);
    }

    /**
     * Opens the media picker and attaches the first still image found in the device
     * gallery (skipping videos and any broken/unloaded thumbnails). Requires the
     * device gallery to contain at least one photo.
     */
    public boolean attachFirstGalleryImage() {
        if (!tapFirstVisible(addMediaAction)) {
            return false;
        }
        // Which picker opens depends entirely on the OS version, so try both rather than
        // assume. Confirmed live: on Android 15 the system Photo Picker opens and labels
        // stills "Photo taken on ..."; on Android 10 that package does not exist at all
        // (com.google.android.photopicker arrived in Android 13), so the app falls back to
        // DocumentsUI, which labels entries by FILENAME. The "Photo taken on" selector can
        // never match there, which stranded every image test on the API 29 emulator with
        // the picker left open on screen.
        if (!tapFirstVisible(pickerFirstPhoto) && !tapFirstVisible(documentsUiImageLabels)) {
            return false;
        }
        // DocumentsUI returns to the caller the moment a file is chosen - it has no Done
        // button - while the Photo Picker requires one. Absent is success, not failure.
        if (!isVisibleNow(pickerDoneAction)) {
            return true;
        }
        return tapFirstVisible(pickerDoneAction);
    }


    /**
     * Opens the media picker and attaches the first video found in the device gallery.
     * Requires the device gallery to contain at least one video.
     */
    public boolean attachFirstGalleryVideo() {
        if (!tapFirstVisible(addMediaAction)) {
            return false;
        }
        if (!scrollUntilVisible(pickerFirstVideo)) {
            return false;
        }
        if (!tapFirstVisible(pickerFirstVideo)) {
            return false;
        }
        return tapFirstVisible(pickerDoneAction);
    }

    /** Tapping Back with unsaved content (caption or media present) shows the Discard
     * confirmation dialog (DiscardPostDialog.kt) rather than navigating away directly. */
    public boolean tapBack() {
        return tapFirstVisible(backAction);
    }

    /** MediaGridThumbnailCell (CreatePostScreen.kt) gives the first attached image the
     * contentDescription "Selected image 1" (ui_selected_image, 1-indexed) - tapping it
     * opens the MediaEditorScreen overlay for that item. */
    public boolean tapFirstMediaThumbnail() {
        return tapFirstVisible(accessibilityId("Selected image 1"), accessibilityId("Video thumbnail 1"));
    }

    /** A keyboard left open from a leftover caption/search field can occlude the Back
     * icon and other toolbar controls - dismiss it before interacting further. */
    public void dismissKeyboardIfShown() {
        hideKeyboardIfShown();
    }

    public boolean isDiscardDialogVisible() {
        return isAnyVisible(discardDialogTitle);
    }

    public boolean tapKeepEditing() {
        return tapFirstVisible(keepEditingAction);
    }

    public boolean tapDiscardConfirm() {
        return tapFirstVisible(discardConfirmAction);
    }

    public boolean addCaption(String caption) {
        if (!typeIntoFirstVisible(caption, captionEditor, captionField, captionPlaceholder)) {
            return false;
        }
        if (isCaptionPresent(caption)) {
            return true;
        }
        // Typing a caption containing a URL can race the app's link-preview/auto-embed
        // recompose mid-keystroke (observed: duplicated/truncated text, e.g. "...
        // https://www.youtube.Regression post ... https://www.youtube.com/watch?v=").
        // Retry once now that any recompose triggered by the first attempt has settled.
        return typeIntoFirstVisible(caption, captionEditor, captionField, captionPlaceholder)
                && isCaptionPresent(caption);
    }

    public boolean isCaptionPresent(String caption) {
        return isAnyVisible(textContains(caption)) || driver.getPageSource().contains(caption);
    }

    public boolean toggleBold() {
        return tapFirstVisible(boldAction);
    }

    /** Idempotent: if a prior test left the picker open (e.g. an unverified
     * closeEmojiPicker() call), tapping the underlying "Emoji" icon again is a stray
     * tap on the dialog now covering it - confirmed live, Compose keeps occluded nodes
     * reporting visible/clickable in the accessibility tree (same quirk documented on
     * MusicPage). Check for the picker's own search field first instead. */
    public boolean openEmojiPicker() {
        if (isAnyVisible(emojiSearchPlaceholder)) {
            return true;
        }
        return tapFirstVisible(emojiAction);
    }

    public boolean closeEmojiPicker() {
        return tapFirstVisible(emojiCloseAction);
    }

    public boolean isEmojiCategoryVisible(String categoryLabel) {
        return isAnyVisible(accessibilityId(categoryLabel), text(categoryLabel));
    }

    public boolean searchEmoji(String query) {
        return typeIntoFirstVisible(query, emojiSearchPlaceholder);
    }

    public boolean isNoEmojisFoundVisible() {
        return isAnyVisible(emojiNoResultsText);
    }

    /** Taps an emoji cell by its exact glyph (EmojiCell renders the raw string, no
     * contentDescription) and returns whether it landed in the caption. */
    public boolean selectEmojiAndConfirmInserted(String emojiGlyph) {
        if (!tapFirstVisible(text(emojiGlyph))) {
            return false;
        }
        return isCaptionPresent(emojiGlyph);
    }

    public boolean insertMentionSymbol() {
        return tapFirstVisible(mentionAction);
    }

    public boolean insertHashtagSymbol() {
        return tapFirstVisible(hashtagAction);
    }

    public boolean tapPost() {
        return tapFirstVisible(postAction);
    }

    /**
     * Sends the app to background (simulating the user switching away mid-upload) and
     * waits for it to be automatically foregrounded again after the given duration.
     */
    public void backgroundAppFor(Duration duration) {
        driver.runAppInBackground(duration);
    }

    /** Negative complement to isPostButtonEnabled(): true only if every matching
     * container is confirmed disabled (not merely "not yet confirmed enabled"), so a
     * transient/unfound element can't be mistaken for a real disabled state. */
    public boolean isPostButtonDisabled() {
        java.util.List<WebElement> containers = driver.findElements(postButtonContainer);
        if (containers.isEmpty()) {
            return false;
        }
        for (WebElement element : containers) {
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
        return isAnyVisible(scheduleToggle);
    }

    /** Toggles scheduling on and confirms via a real state signal - the date/time
     * picker rows only render once scheduling is actually enabled, not just because
     * the toggle Text was tappable. No-ops (returns true) if already enabled. */
    public boolean enableSchedulePost() {
        if (isAnyVisible(pickDateAction)) {
            return true;
        }
        tapFirstVisible(scheduleToggle);
        return isAnyVisible(pickDateAction);
    }

    public boolean tapScheduleSubmit() {
        return tapFirstVisible(scheduleSubmitAction);
    }

    public boolean isScheduleDateTimeRequiredErrorVisible() {
        return isAnyVisible(scheduleDateTimeRequiredError);
    }

    public boolean enterLocation(String location) {
        return typeIntoFirstVisible(location, locationField);
    }

    public boolean isLocationPresent(String location) {
        return isAnyVisible(textContains(location));
    }

    /** Taps the caption toolbar's mic icon and confirms the hand-off to the system
     * speech-recognition dialog ("Speak your caption...") actually happened - this is
     * as far as automation can verify without a real microphone input on the test
     * device/emulator; what the recognizer transcribes is outside this app's own UI. */
    public boolean tapMicAndConfirmDialogOpens() {
        if (!tapFirstVisible(micAction)) {
            return false;
        }
        try {
            waitForAnyVisible(micDialogPrompt);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Dismisses the system speech dialog via device Back, returning to Create Post. */
    public void dismissMicDialog() {
        try {
            driver.navigate().back();
        } catch (RuntimeException ignored) {
            // Best-effort.
        }
    }

    public boolean isSaveDraftVisible() {
        return isAnyVisible(saveDraftAction);
    }

    public boolean tapSaveDraft() {
        return tapFirstVisible(saveDraftAction);
    }

    private boolean isAnyPostButtonEnabled() {
        for (WebElement element : driver.findElements(postButtonContainer)) {
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
