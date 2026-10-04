package com.shriaikyam.aikyam.pages;

import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;

/**
 * The priest/temple account's own Settings screen (reached via the temple avatar, same
 * position as {@link SettingsPage#openSettingsHub()} on the devotee side) - distinct
 * content from the devotee Settings hub, so kept as its own page object rather than
 * folded into {@link SettingsPage}. Discovered during exploratory QA; not previously
 * automated.
 */
public class PriestSettingsPage extends BasePage {

    private final By orderHistoryRow = text("Order History");
    private final By yourPostsRow = text("Your Posts");
    private final By comingSoonBadge = textContains("Coming Soon");

    // Confirmed defect signal, same as PriestPage.isTempleContextErrorVisible() - this
    // screen's Order History reproduces the identical error independently of the
    // Orders tab, which is what confirms the bug is data-path-scoped rather than
    // specific to one screen.
    private final By templeContextError = text("No active temple context found");

    public PriestSettingsPage(IOSDriver driver) {
        super(driver);
    }

    public boolean isSettingsScreenVisible() {
        return isAnyVisible(text("Switch account"));
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

    public boolean isYourPostsEmptyStateVisible() {
        return isAnyVisible(text("No Posts"));
    }

    public boolean isAssignRolesComingSoon() {
        return scrollUntilVisible(text("Assign Roles")) && isComingSoonBadgeVisible();
    }

    public boolean isComingSoonBadgeVisible() {
        return isAnyVisible(comingSoonBadge);
    }
}
