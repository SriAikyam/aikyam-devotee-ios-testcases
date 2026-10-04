package com.shriaikyam.aikyam.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Events home (bottom nav → Events) and its per-card actions. Discovered and manually
 * verified during exploratory QA - the existing suite had no coverage of this module at
 * all before this pass. EventDetailsScreen (opened via a card's "Details" action) is
 * covered by {@link #openFirstEventDetails()} and the isXxxVisible() checks below rather
 * than a separate page object, since its content is a straightforward superset of what a
 * card already shows.
 */
public class EventsPage extends BasePage {

    // Filter chip labels, top of the Events screen. "Near Me" is the default-selected
    // chip; the others are what TC-below found unresponsive on Android 10 only.
    private final By attendingFilterChip = text("Attending");
    private final By savedFilterChip = text("Saved");
    private final By nearMeFilterChip = text("Near Me");
    private final By pujasFilterChip = text("Pujas");
    private final By bhajanFilterChip = text("Bhajan");
    private final By festivalsFilterChip = text("Festivals");

    private final By detailsAction = text("Details");
    private final By attendingAction = text("Attending");
    private final By saveAction = accessibilityId("Save");
    private final By shareAction = accessibilityId("Share");

    // EventDetailsScreen content - opened via detailsAction above.
    private final By backAction = accessibilityId("Back");
    private final By attendeeCountText = textContains("attendees");
    private final By scheduleHeading = text("Schedule");

    public EventsPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isEventsScreenVisible() {
        return isAnyVisible(nearMeFilterChip, attendingFilterChip, savedFilterChip);
    }

    public boolean isAnyEventCardVisible() {
        return isAnyVisible(detailsAction) || driver.getPageSource().contains("No events found");
    }

    /**
     * Taps a filter chip by its visible label and reports whether the chip's own
     * selected/checked state actually changed - not just whether the tap landed.
     * "selected" is read off the chip's nearest ancestor carrying that attribute,
     * since the bare Text node itself does not expose selection state.
     */
    public boolean tapFilterChipAndConfirmSelected(String label) {
        By chip = text(label);
        if (!tapFirstVisible(chip)) {
            return false;
        }
        return isFilterChipSelected(label);
    }

    /** True only if this label's chip container reports selected=true - not merely
     * that the label text is present, which stays true regardless of selection. */
    public boolean isFilterChipSelected(String label) {
        By chipContainer = By.xpath("(//*[@text='" + label + "']/ancestor::*[@selected])[1]");
        List<WebElement> matches = driver.findElements(chipContainer);
        if (matches.isEmpty()) {
            return false;
        }
        return "true".equals(matches.get(0).getAttribute("selected"));
    }

    public boolean openFirstEventDetails() {
        return tapFirstVisible(detailsAction);
    }

    public boolean isEventDetailsVisible() {
        return isAnyVisible(attendeeCountText, scheduleHeading);
    }

    public boolean isAttendingActionVisible() {
        return isAnyVisible(attendingAction);
    }

    public boolean isSaveActionVisible() {
        return isAnyVisible(saveAction);
    }

    public boolean isShareActionVisible() {
        return isAnyVisible(shareAction);
    }

    public boolean tapBack() {
        return tapFirstVisible(backAction);
    }
}
