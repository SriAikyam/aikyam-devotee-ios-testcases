package com.shriaikyam.aikyam.pages;

import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Locale;

/**
 * Events home (bottom nav → Events, EventsHomeView) and its per-card actions (EventCard).
 * EventDetailsView (opened via a card's "Details" action) is covered by
 * {@link #openFirstEventDetails()} and the isXxxVisible() checks below rather than a
 * separate page object, since its content is a straightforward superset of what a card
 * already shows.
 */
public class EventsPage extends BasePage {

    // EventsFilterChipRow: one identifier per chip, "events.filter.<EventsFilterChip rawValue>".
    private final By attendingFilterChip = id("events.filter.attending");
    private final By savedFilterChip = id("events.filter.saved");
    private final By nearMeFilterChip = id("events.filter.near_me");
    private final By eventsTitle = id("events.title");

    private final By detailsAction = id("events.card.details");
    private final By emptyState = id("events.empty");
    private final By attendingAction = id("events.card.attend");
    private final By detailsAttendAction = id("events.details.attend");
    // SaveShareIconRow is shared by the card footer and the details bottom bar.
    private final By saveAction = id("events.save");
    private final By shareAction = id("events.share");

    // EventDetailsView content - opened via detailsAction above.
    private final By backAction = id("events.details.back");
    private final By detailsTitle = id("events.details.title");
    private final By attendeeCountText = textContains("attendees");
    private final By scheduleHeading = id("events.details.schedule");

    public EventsPage(IOSDriver driver) {
        super(driver);
    }

    public boolean isEventsScreenVisible() {
        return isAnyVisible(nearMeFilterChip, attendingFilterChip, savedFilterChip, eventsTitle);
    }

    public boolean isAnyEventCardVisible() {
        return isAnyVisible(detailsAction, emptyState);
    }

    /**
     * Taps a filter chip by its visible English label ("Near Me", "Pujas", ...) and reports
     * whether the chip's own selected state actually changed - not just whether the tap
     * landed.
     */
    public boolean tapFilterChipAndConfirmSelected(String label) {
        if (!tapFirstVisible(filterChip(label), text(label))) {
            return false;
        }
        return isFilterChipSelected(label);
    }

    /** True only if this label's chip reports selected=true. XCUITest derives "selected"
     * from the .isSelected accessibility trait, which EventsFilterChipView does not set
     * yet (selection is only drawn as a ring + bold label) - until it does, this reads
     * false for every chip. */
    public boolean isFilterChipSelected(String label) {
        List<WebElement> matches = driver.findElements(filterChip(label));
        if (matches.isEmpty()) {
            return false;
        }
        return "true".equals(matches.get(0).getAttribute("selected"));
    }

    private By filterChip(String label) {
        return id("events.filter." + label.trim().toLowerCase(Locale.ROOT).replace(' ', '_'));
    }

    public boolean openFirstEventDetails() {
        return tapFirstVisible(detailsAction);
    }

    public boolean isEventDetailsVisible() {
        return isAnyVisible(detailsTitle, attendeeCountText, scheduleHeading);
    }

    public boolean isAttendingActionVisible() {
        return isAnyVisible(attendingAction, detailsAttendAction);
    }

    public boolean isSaveActionVisible() {
        return isAnyVisible(saveAction);
    }

    public boolean isShareActionVisible() {
        return isAnyVisible(shareAction);
    }

    public boolean tapBack() {
        return tapFirstVisible(backAction) || navigateBack();
    }
}
