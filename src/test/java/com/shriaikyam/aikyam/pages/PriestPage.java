package com.shriaikyam.aikyam.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

/**
 * The priest/temple-role bottom nav and its landing screens (Temple, Orders, Events,
 * Services, Apps), reachable from the same devotee login via
 * {@link SettingsPage#switchToAccountByName(String)} - discovered during exploratory QA.
 * None of this was automated before this pass; the priest role had been assumed to need
 * separate credentials.
 *
 * TC-below in PriestTest encodes a confirmed, currently-reproducing defect: Orders and
 * Order History both render the raw string "No active temple context found" instead of
 * order data or a designed empty state, on both Android 10 and 11. That assertion is
 * expected to FAIL until the underlying session/context bug is fixed.
 */
public class PriestPage extends BasePage {

    private final By temple = text("Temple");
    private final By orders = text("Orders");
    private final By events = text("Events");
    private final By services = text("Services");
    private final By apps = text("Apps");

    // Temple tab's own sub-tabs.
    private final By aboutSubTab = text("About");
    private final By postsSubTab = text("Posts");
    private final By liveSubTab = text("Live");

    // Confirmed defect signal - see class doc.
    private final By templeContextError = text("No active temple context found");
    private final By pendingAcceptanceTab = text("Pending Acceptance");

    private final By composerAction = accessibilityId("Create Post");
    private final By postComposerTab = text("Post");
    private final By eventComposerTab = text("Events");

    public PriestPage(AndroidDriver driver) {
        super(driver);
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

    /** True if Posts renders its correct empty state - used to confirm the temple
     * account's own Posts tab is healthy, in contrast with Orders below. */
    public boolean isPostsEmptyStateVisible() {
        return isAnyVisible(text("No posts yet"));
    }

    public boolean isLiveTabContentVisible() {
        return isAnyVisible(textContains("Live Stream"), textContains("live events"));
    }

    public boolean isOrdersScreenVisible() {
        return isAnyVisible(pendingAcceptanceTab);
    }

    /** The confirmed defect signal - see class doc. Exposed as its own check (rather
     * than folded into a single pass/fail) so a test can assert its absence directly
     * and name the real expected behavior instead of merely "orders screen visible". */
    public boolean isTempleContextErrorVisible() {
        return isAnyVisible(templeContextError);
    }

    public boolean isServicesScreenVisible() {
        return isAnyVisible(textContains("Services"), textContains("temple services"));
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
        return isAnyVisible(text("Event Title"), text("Event Type"));
    }

    public boolean closeComposer() {
        return tapFirstVisible(accessibilityId("Close"));
    }
}
