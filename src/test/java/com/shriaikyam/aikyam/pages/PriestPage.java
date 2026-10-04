package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * The priest/temple-role bottom nav and its landing screens (Temple, Orders, Events,
 * Services, Apps), reachable from the same devotee login via
 * {@link SettingsPage#switchToAccountByName(String)}.
 *
 * PriestTest asserts that Orders and Order History do not render the raw "No active temple
 * context" error (TempleOrdersViewModel / TempleSevaOrdersListViewModel set it when the
 * active context carries no temple id) instead of order data or a designed empty state.
 */
public class PriestPage extends BasePage {

    // MainTabView swaps the tab bar's identifiers in temple context. The priest "Events"
    // tab is L10n.Tabs.schedules (English label "Events"), hence tab.schedules.
    private final By temple = id("tab.temple");
    private final By orders = id("tab.orders");
    private final By events = id("tab.schedules");
    private final By services = id("tab.services");
    private final By apps = id("tab.apps");

    // TempleAccountHomeView's About / Posts / Live segment buttons. Each button's label
    // combines its SF Symbol and title, so match on the trailing title; "Posts" also
    // matches the header's Posts stat pill, which selects the same segment.
    private final By aboutSubTab = segmentButton("About");
    private final By postsSubTab = segmentButton("Posts");
    private final By liveSubTab = segmentButton("Live");

    // iOS copy is "No active temple context"; Android's was "... context found".
    private final By templeContextError = textContains("No active temple context");
    private final By pendingAcceptanceTab = id("priest.orders.pending");

    // FeedTopBar's "+" (TempleAccountHomeView, id feed.createPost) and PriestEventsView's
    // "+" both push TempleGoLiveView.
    private final By composerAction = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND (name == 'feed.createPost' OR name == 'priest.events.create')");
    // TempleGoLiveView's Post / Events mode segment.
    private final By postComposerTab = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND label == 'Post'");
    private final By eventComposerTab = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND label == 'Events' AND NOT (name BEGINSWITH 'tab.')");
    private final By createEventTitleField = id("events.create.title");
    private final By createEventTypePicker = id("events.create.type");
    // TempleGoLiveView's close button is an unlabeled "xmark" image button.
    private final By composerCloseAction = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND (name == 'xmark' OR label == 'Close')");

    public PriestPage(IOSDriver driver) {
        super(driver);
    }

    private static By segmentButton(String title) {
        String v = predicateLiteral(title);
        String suffix = predicateLiteral(", " + title);
        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND NOT (name BEGINSWITH 'tab.') AND (label == " + v
                        + " OR label ENDSWITH " + suffix + ")");
    }

    public boolean isPriestBottomNavVisible() {
        return isAnyVisible(temple) && isAnyVisible(orders) && isAnyVisible(events)
                && isAnyVisible(services) && isAnyVisible(apps);
    }

    public void tapTempleTab() {
        tapFirstVisible(temple);
    }

    public void tapOrdersTab() {
        tapFirstVisible(orders);
    }

    public void tapEventsTab() {
        tapFirstVisible(events);
    }

    public void tapServicesTab() {
        tapFirstVisible(services);
    }

    public void tapAppsTab() {
        tapFirstVisible(apps);
    }

    public boolean isTempleProfileVisible() {
        return isAnyVisible(aboutSubTab, postsSubTab, liveSubTab);
    }

    public boolean tapPostsSubTab() {
        return tapFirstVisible(postsSubTab);
    }

    public boolean tapLiveSubTab() {
        return tapFirstVisible(liveSubTab);
    }

    /** True if Posts renders its correct empty state (TempleAccountPostsView) - used to
     * confirm the temple account's own Posts tab is healthy, in contrast with Orders. */
    public boolean isPostsEmptyStateVisible() {
        return isAnyVisible(text("No posts yet"));
    }

    public boolean isLiveTabContentVisible() {
        return isAnyVisible(id("priest.live.happening"), id("priest.live.upcoming"));
    }

    public boolean isOrdersScreenVisible() {
        return isAnyVisible(pendingAcceptanceTab);
    }

    /** Exposed as its own check (rather than folded into a single pass/fail) so a test can
     * assert its absence directly and name the real expected behavior instead of merely
     * "orders screen visible". */
    public boolean isTempleContextErrorVisible() {
        return isAnyVisible(templeContextError);
    }

    /** Services is a PriestTempleTabPlaceholder in temple context: no title, only a
     * "Coming Soon" message - so require the Services tab to be the selected one too. */
    public boolean isServicesScreenVisible() {
        return isTabSelected(services) && isAnyVisible(textContains("Coming Soon"));
    }

    public boolean isAppsComingSoonVisible() {
        return isAnyVisible(textContains("Coming Soon"));
    }

    public boolean openComposer() {
        return tapFirstVisible(composerAction);
    }

    public boolean isComposerVisible() {
        return isAnyVisible(postComposerTab, eventComposerTab);
    }

    public boolean tapEventComposerTab() {
        return tapFirstVisible(eventComposerTab);
    }

    public boolean isCreateEventFormVisible() {
        return isAnyVisible(createEventTitleField, createEventTypePicker);
    }

    public boolean closeComposer() {
        return tapFirstVisible(composerCloseAction) || navigateBack();
    }

    private boolean isTabSelected(By tab) {
        List<WebElement> matches = driver.findElements(tab);
        return !matches.isEmpty() && "true".equals(matches.get(0).getAttribute("selected"));
    }
}
