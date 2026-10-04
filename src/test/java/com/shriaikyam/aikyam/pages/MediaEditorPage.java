package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/** CreatePostMediaEditorView (full-screen cover), reached from PostPage via a thumbnail's
 * pencil button. Its text dialog is CreatePostTextEditorDialog and its crop tool is the
 * CreatePostCropSheet sheet. Every `id(...)` is paired with a label/type fallback. */
public class MediaEditorPage extends BasePage {

    // Tool buttons combine the SF Symbol and the caption Text into one label, so the
    // fallbacks match on CONTAINS rather than an exact label.
    private final By cropTool = id("mediaeditor.tool.crop");
    private final By cropToolLabel = button("label CONTAINS 'Crop'");
    private final By filterTool = id("mediaeditor.tool.filter");
    private final By filterToolLabel = button("label CONTAINS 'Filter'");
    private final By adjustTool = id("mediaeditor.tool.adjust");
    private final By adjustToolLabel = button("label CONTAINS 'Adjust'");
    private final By textTool = id("mediaeditor.tool.text");
    private final By textToolLabel = button("label CONTAINS 'Text'");
    private final By stickerTool = id("mediaeditor.tool.sticker");
    private final By stickerToolLabel = button("label CONTAINS 'Sticker'");

    // Tray items and placed overlays carry no shared label on iOS (each is its raw glyph),
    // so they are only countable through their identifiers.
    private final By stickerTrayItem = id("mediaeditor.sticker");
    private final By placedOverlay = id("mediaeditor.overlay");

    private final By textOverlayPlaceholder = textContains("Type text...");
    private final By textOverlayField = id("mediaeditor.text.field");
    // TextField(axis: .vertical) is backed by a UITextView.
    private final By textOverlayFieldByType = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeTextView' OR type == 'XCUIElementTypeTextField'");
    private final By textEditorDone = id("mediaeditor.text.done");
    private final By textOverlayCancel = id("mediaeditor.text.close");
    private final By textOverlayCancelLabel = button("label == 'Close'");

    // iOS top bar: Skip (leave without applying) | "Edit" title | Done.
    private final By skipAction = id("mediaeditor.skip");
    private final By skipLabel = button("label == 'Skip'");
    private final By editorTitle = id("mediaeditor.title");
    // StaticText only: Create Post's thumbnail pencil is a Button also labelled "Edit".
    private final By editorTitleText = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeStaticText' AND label == 'Edit'");
    private final By doneAction = id("mediaeditor.done");
    private final By doneLabel = button("label == 'Done'");

    private final By originalFilter = text("Original");
    private final By warmFilter = text("Warm");
    private final By monoFilter = text("Mono");

    private final By brightnessSlider = text("Brightness");
    private final By contrastSlider = text("Contrast");
    private final By saturationSlider = text("Saturation");
    private final By resetAdjustments = id("mediaeditor.adjust.reset");
    private final By cropReset = id("mediaeditor.crop.reset");
    private final By resetLabel = button("label == 'Reset'");

    private final By removeAudioAction = id("mediaeditor.video.removeaudio");
    private final By removeAudioLabel = button("label CONTAINS 'Remove audio'");

    public MediaEditorPage(IOSDriver driver) {
        super(driver);
    }

    private static By button(String condition) {
        return AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (" + condition + ")");
    }

    /** Skip/Done are on both the image and the video editor; the tool bar is image-only. */
    public boolean isEditorVisible() {
        return isAnyVisible(skipAction, skipLabel, doneAction, stickerTool, stickerToolLabel);
    }

    public boolean openStickerTray() {
        return tapFirstVisible(stickerTool, stickerToolLabel);
    }

    /** Tray items plus stickers/text already placed on the canvas. */
    public int visibleStickerCount() {
        return visibleElements(stickerTrayItem).size() + visibleElements(placedOverlay).size();
    }

    public boolean tapFirstStickerInTray() {
        return tapFirstVisible(stickerTrayItem);
    }

    /** iOS has no "Remove sticker" control: a long-press on a placed overlay removes it.
     * Returns true once the canvas holds one overlay fewer. */
    public boolean tapRemoveSticker() {
        List<WebElement> placed = visibleElements(placedOverlay);
        if (placed.isEmpty()) {
            return false;
        }
        try {
            longPress(placed.get(0));
        } catch (RuntimeException exception) {
            return false;
        }
        return visibleElements(placedOverlay).size() < placed.size();
    }

    // iOS: the media editor has no "Remove sticker" button; removal is a long-press (see tapRemoveSticker).
    public boolean isRemoveStickerActionVisible() {
        return false;
    }

    public boolean openTextTool() {
        return tapFirstVisible(textTool, textToolLabel);
    }

    public boolean isTextOverlayInputVisible() {
        return isAnyVisible(textOverlayField, textOverlayPlaceholder);
    }

    public boolean typeOverlayText(String value) {
        return typeIntoFirstVisible(value, textOverlayField, textOverlayFieldByType);
    }

    public boolean confirmTextOverlay() {
        return tapFirstVisible(textEditorDone, doneLabel);
    }

    public boolean isOverlayTextVisible(String value) {
        return isAnyVisible(textContains(value));
    }

    /** Skip is this screen's way out: it closes without applying pending edits. */
    @Override
    public boolean navigateBack() {
        return tapFirstVisible(skipAction, skipLabel);
    }

    // ── Crop ─────────────────────────────────────────────────────────────────────────
    /** Opens CreatePostCropSheet (Cancel / "Crop" / Done, aspect chips, Reset). */
    public boolean openCropTool() {
        return tapFirstVisible(cropTool, cropToolLabel);
    }

    public boolean isCropPanelOpen() {
        return isAnyVisible(cropReset, resetLabel);
    }

    // ── Filters ──────────────────────────────────────────────────────────────────────
    public boolean openFilterTray() {
        return tapFirstVisible(filterTool, filterToolLabel);
    }

    public boolean isFilterTrayOpen() {
        return isAnyVisible(originalFilter, warmFilter, monoFilter);
    }

    /** CreatePostImageFilter labels: Original, Warm, Cool, Mono, Vivid. */
    public boolean selectFilter(String label) {
        return tapFirstVisible(text(label));
    }

    public boolean isFilterVisible(String label) {
        return isAnyVisible(text(label));
    }

    // ── Adjust ───────────────────────────────────────────────────────────────────────
    public boolean openAdjustPanel() {
        return tapFirstVisible(adjustTool, adjustToolLabel);
    }

    /** All three sliders, not any-of: the panel is only really open once the whole set shows. */
    public boolean isAdjustPanelOpen() {
        return isAnyVisible(brightnessSlider) && isAnyVisible(contrastSlider)
                && isAnyVisible(saturationSlider);
    }

    public boolean tapResetAdjustments() {
        return tapFirstVisible(resetAdjustments, resetLabel);
    }

    // ── Text overlay ─────────────────────────────────────────────────────────────────
    public boolean cancelTextOverlay() {
        return tapFirstVisible(textOverlayCancel, textOverlayCancelLabel);
    }

    // ── Video ────────────────────────────────────────────────────────────────────────
    /** Only on the video editor's Edit tab, so absence means "this item is not a video". */
    public boolean isRemoveAudioVisible() {
        return isAnyVisible(removeAudioAction, removeAudioLabel);
    }

    // ── Commit ───────────────────────────────────────────────────────────────────────
    /** iOS always reads "Done" (no per-item "Next" in a batch). */
    public boolean commitAndLeave() {
        return tapFirstVisible(doneAction, doneLabel);
    }

    public boolean isStillInEditor() {
        return isAnyVisible(editorTitle, editorTitleText);
    }

    /** Polls: Done returns before the edit is applied and the cover is dismissed (a video
     * export can take a few seconds). */
    public boolean hasLeftEditor() {
        return waitUntilGone(editorTitle, editorTitleText);
    }

    private void longPress(WebElement element) {
        Point location = element.getLocation();
        Dimension size = element.getSize();
        int x = location.getX() + size.getWidth() / 2;
        int y = location.getY() + size.getHeight() / 2;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence press = new Sequence(finger, 1);
        press.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        press.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        press.addAction(finger.createPointerMove(Duration.ofMillis(900), PointerInput.Origin.viewport(), x, y));
        press.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(press));
    }
}
