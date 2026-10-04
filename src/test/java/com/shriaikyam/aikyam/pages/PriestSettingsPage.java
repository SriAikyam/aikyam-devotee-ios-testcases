package com.shriaikyam.aikyam.pages;

import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;

/**
 * The priest/temple account's own Settings screen (PriestSettingsView, reached via the
 * temple avatar, same position as {@link SettingsPage#openSettingsHub()} on the devotee
 * side) - distinct content from the devotee Settings hub, so kept as its own page object
 * rather than folded into {@link SettingsPage}.
 */
public class PriestSettingsPage extends BasePage {

    // PriestSettingsView rows are Buttons whose label combines title and subtitle, so the
    // ids are the reliable handle; the title text is kept as a fallback.
    private final By orderHistoryRow = id("priest.settings.orderhistory");
    private final By yourPostsRow = id("priest.settings.yourposts");
    private final By assignRolesRow = id("priest.settings.assignroles");
    private final By switchAccountHeader = id("priest.settings.switchaccount");
    private final By comingSoonBadge = textContains("Coming Soon");

    // Same defect signal as PriestPage.isTempleContextErrorVisible(), rendered here by
    // TempleSevaOrdersListView (Order History). iOS copy is "No active temple context".
    private final By templeContextError = textContains("No active temple context");

    public PriestSettingsPage(IOSDriver driver) {
        super(driver);
    }

    public boolean isSettingsScreenVisible() {
        return isAnyVisible(switchAccountHeader, text("Switch account"));
    }

    public boolean openOrderHistory() {
        return scrollUntilVisible(orderHistoryRow) && tapFirstVisible(orderHistoryRow);
    }

    public boolean isTempleContextErrorVisible() {
        return isAnyVisible(templeContextError);
    }

    public boolean openYourPosts() {
        return scrollUntilVisible(yourPostsRow) && tapFirstVisible(yourPostsRow);
    }

    /** MyPostsScreen's empty state reads "No posts yet" on iOS. */
    public boolean isYourPostsEmptyStateVisible() {
        return isAnyVisible(text("No posts yet"), text("No Posts"));
    }

    /** The Assign Roles row's combined label carries its own "Coming Soon" badge text. */
    public boolean isAssignRolesComingSoon() {
        return scrollUntilVisible(assignRolesRow) && isComingSoonBadgeVisible();
    }

    public boolean isComingSoonBadgeVisible() {
        return isAnyVisible(comingSoonBadge);
    }
}
