package com.shriaikyam.aikyam.pages;

import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;

/** Reached from PostPage by tapping an attached image/video thumbnail
 * (MediaGridThumbnailCell's onOpenEditor). Confirmed via MediaEditorScreen.kt source:
 * every string/contentDescription here is a hardcoded literal, not a string resource. */
public class MediaEditorPage extends BasePage {

    private final By stickerTool = text("Sticker");
    // Every sticker item in the tray AND the placed sticker on canvas share the exact
    // same contentDescription "Sticker" - individual stickers aren't distinguishable,
    // only counted by how many match at once.
    private final By stickerNode = accessibilityId("Sticker");
    private final By removeStickerAction = accessibilityId("Remove sticker");

    private final By textTool = text("Text");
    // "Type text..." is BasicTextField's placeholder child Text, rendered only while
    // empty - it's a real visibility signal for the dialog being open, but tapping/
    // typing into that exact node doesn't reach the actual editable field (confirmed
    // live: text landed nowhere). The real input is the EditText node itself.
    private final By textOverlayPlaceholder = textContains("Type text...");
    private final By textOverlayField = io.appium.java_client.AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\")"
    );
    private final By textEditorDone = text("Done");

    private final By backAction = accessibilityId("Back");

    // Toolbar. The primary action reads "Next" while earlier items in a multi-item batch
    // are still unedited and "Done" on the last one (MediaEditorScreen.kt), so both are
    // accepted wherever the intent is "commit and leave".
    private final By editorTitle = text("Edit");
    private final By doneAction = text("Done");
    private final By nextAction = text("Next");

    // Each image tool is an icon whose contentDescription is its own label - the tool list
    // in MediaEditorScreen.kt is exactly Crop, Filter, Adjust, Text, Sticker.
    private final By cropTool = accessibilityId("Crop");
    private final By filterTool = accessibilityId("Filter");
    private final By adjustTool = accessibilityId("Adjust");

    // ImageFilter's labels, in enum order. "Original" is the NONE default.
    private final By originalFilter = text("Original");
    private final By warmFilter = text("Warm");
    private final By monoFilter = text("Mono");

    private final By brightnessSlider = text("Brightness");
    private final By contrastSlider = text("Contrast");
    private final By saturationSlider = text("Saturation");
    private final By resetAdjustments = text("Reset");

    private final By textOverlayCancel = accessibilityId("Cancel");
    private final By removeAudioAction = accessibilityId("Remove Audio");

    public MediaEditorPage(IOSDriver driver) {
        super(driver);
    }

    public boolean isEditorVisible() {
        return isAnyVisible(stickerTool, textTool, backAction);
    }

    public boolean openStickerTray() {
        return tapFirstVisible(stickerTool);
    }

    public int visibleStickerCount() {
        return visibleElements(stickerNode).size();
    }

    /** Taps the first sticker in the tray - the tray itself is at the top of the
     * z-order until one is placed, so this always hits a tray item, not the canvas. */
    public boolean tapFirstStickerInTray() {
        return tapFirstVisible(stickerNode);
    }

    public boolean tapRemoveSticker() {
        return tapFirstVisible(removeStickerAction);
    }

    public boolean isRemoveStickerActionVisible() {
        return isAnyVisible(removeStickerAction);
    }

    public boolean openTextTool() {
        return tapFirstVisible(textTool);
    }

    public boolean isTextOverlayInputVisible() {
        return isAnyVisible(textOverlayPlaceholder, textOverlayField);
    }

    public boolean typeOverlayText(String value) {
        return typeIntoFirstVisible(value, textOverlayField, textOverlayPlaceholder);
    }

    public boolean confirmTextOverlay() {
        return tapFirstVisible(textEditorDone);
    }

    public boolean isOverlayTextVisible(String value) {
        return isAnyVisible(textContains(value));
    }

    public boolean navigateBack() {
        return tapFirstVisible(backAction);
    }

    // ── Crop ─────────────────────────────────────────────────────────────────────────
    public boolean openCropTool() {
        return tapFirstVisible(cropTool);
    }

    /** The crop and adjust panels own the "Reset" control, so its appearance is what
     * distinguishes a genuinely open panel from "the tool icon was tappable". */
    public boolean isCropPanelOpen() {
        return isAnyVisible(resetAdjustments);
    }

    // ── Filters ──────────────────────────────────────────────────────────────────────
    public boolean openFilterTray() {
        return tapFirstVisible(filterTool);
    }

    public boolean isFilterTrayOpen() {
        return isAnyVisible(originalFilter, warmFilter, monoFilter);
    }

    public boolean selectFilter(String label) {
        return tapFirstVisible(text(label));
    }

    public boolean isFilterVisible(String label) {
        return isAnyVisible(text(label));
    }

    // ── Adjust ───────────────────────────────────────────────────────────────────────
    public boolean openAdjustPanel() {
        return tapFirstVisible(adjustTool);
    }

    /** All three sliders, not any-of: the panel is only really open once the whole
     * Brightness/Contrast/Saturation set has composed. */
    public boolean isAdjustPanelOpen() {
        return isAnyVisible(brightnessSlider) && isAnyVisible(contrastSlider)
                && isAnyVisible(saturationSlider);
    }

    public boolean tapResetAdjustments() {
        return tapFirstVisible(resetAdjustments);
    }

    // ── Text overlay ─────────────────────────────────────────────────────────────────
    public boolean cancelTextOverlay() {
        return tapFirstVisible(textOverlayCancel);
    }

    // ── Video ────────────────────────────────────────────────────────────────────────
    /** Only rendered for a video item, so absence means "this item is not a video"
     * rather than a failure. */
    public boolean isRemoveAudioVisible() {
        return isAnyVisible(removeAudioAction);
    }

    // ── Commit ───────────────────────────────────────────────────────────────────────
    /** The primary action reads "Next" while earlier items in a batch are unedited and
     * "Done" on the last, so both are accepted for "commit and leave". */
    public boolean commitAndLeave() {
        return tapFirstVisible(doneAction, nextAction);
    }

    public boolean isStillInEditor() {
        return isAnyVisible(editorTitle);
    }

    /** Polls for the editor to close rather than checking once. commitAndLeave() returns
     * as soon as the tap is dispatched, not once the edit has been applied and the screen
     * has popped - confirmed live: TC137 reported it was still in the editor while the
     * captured page source showed Create Post, with no "Edit" node anywhere. */
    public boolean hasLeftEditor() {
        return waitUntilGone(editorTitle);
    }
}
