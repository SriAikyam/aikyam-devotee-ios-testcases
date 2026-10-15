package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.Map;

/** The Settings hub (SettingsView), reached via ProfilePage.openSettingsHub() - reused
 * rather than duplicated here. Every row carries a `settings.row.*` identifier; callers
 * still pass the English row label, which is mapped to that identifier here, with the
 * label itself as fallback (PriestSettingsView, shown to priest roles, has no ids). */
public class SettingsPage extends BasePage {

    private static final Map<String, String> ROW_IDS = Map.ofEntries(
            Map.entry("Personal Profile", "settings.row.personalProfile"),
            Map.entry("Family Profile", "settings.row.familyProfile"),
            Map.entry("Upcoming Sevas", "settings.row.upcomingSevas"),
            Map.entry("Order History", "settings.row.orderHistory"),
            Map.entry("Payment Methods", "settings.row.paymentMethods"),
            Map.entry("Linked Accounts", "settings.row.linkedAccounts"),
            Map.entry("Your Posts", "settings.row.yourPosts"),
            Map.entry("Following", "settings.row.following"),
            Map.entry("Followers", "settings.row.followers"),
            Map.entry("Saved Posts", "settings.row.savedPosts"),
            Map.entry("Liked Posts", "settings.row.likedPosts"),
            Map.entry("Commented Posts", "settings.row.commentedPosts"),
            Map.entry("Search History", "settings.row.searchHistory"),
            Map.entry("Help Center", "settings.row.helpCenter"),
            Map.entry("About Aikyam", "settings.row.aboutAikyam"),
            Map.entry("App Language", "settings.row.appLanguage"),
            Map.entry("Content Preferences", "settings.row.contentPreferences"),
            Map.entry("Open Source Licenses", "about.row.openSourceLicenses"),
            Map.entry("Open source licences", "about.row.openSourceLicenses")
    );

    /** Android copy that iOS spells differently. */
    private static final Map<String, String> IOS_TITLES = Map.of(
            "Open source licences", "Open Source Licenses"
    );

    private final By logoutAction = id("settings.logout");
    private final By logoutConfirmYes = alertButton("Yes");
    private final By logoutConfirmNo = alertButton("No");
    private final By comingSoonBadge = textContains("Coming Soon");
    // Your/Saved/Liked/Commented Posts all render FeedPostRow, so its action row is the
    // "real post content rendered" signal shared across all four screens.
    private final By postCardContentSignal = id("feed.post.like");
    private final By postCardCommentSignal = id("feed.post.comment");
    private final By userListContentSignal = AppiumBy.iOSNsPredicateString(
            "name == 'following.row' OR name == 'followers.row'");
    private final By accountSwitcherCapsule = id("settings.accountSwitcher");

    public SettingsPage(IOSDriver driver) {
        super(driver);
    }

    /** Logout sits at the very bottom of the Settings hub, below the fold, so this must
     * scroll rather than check visibility once. */
    public boolean isSettingsHubVisible() {
        return scrollUntilVisible(logoutAction, text("Logout"));
    }

    public boolean isRowVisible(String label) {
        return scrollUntilVisible(rowLocators(label));
    }

    public boolean isComingSoonBadgeVisible() {
        return scrollUntilVisible(comingSoonBadge);
    }

    /** A "Coming Soon" row is a combined static element rather than a Button on iOS, so
     * a row counts as disabled when it is not a Button or reports enabled=false. */
    public boolean isRowDisabled(String label) {
        By[] row = rowLocators(label);
        if (!scrollUntilVisible(row)) {
            return false;
        }
        try {
            WebElement element = waitForAnyVisible(row);
            return !"XCUIElementTypeButton".equals(element.getAttribute("type")) || !element.isEnabled();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean openRow(String label) {
        By[] row = rowLocators(label);
        return scrollUntilVisible(row) && tapFirstVisible(row);
    }

    public boolean isDestinationTitleVisible(String title) {
        try {
            waitForAnyVisible(text(IOS_TITLES.getOrDefault(title, title)));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** True if the destination shows its own empty-state message. */
    public boolean isEmptyStateShown(String emptyStateText) {
        return isAnyVisible(text(emptyStateText), textContains(emptyStateText));
    }

    /** True if the destination shows either real post content (a FeedPostRow action) or
     * the screen's own empty-state message - never just because the title rendered. iOS
     * empty states read "No ... yet", so textContains covers Android's shorter copy. */
    public boolean isPostListContentPresent(String emptyStateText) {
        return isAnyVisible(postCardContentSignal, postCardCommentSignal,
                text(emptyStateText), textContains(emptyStateText));
    }

    /** Same idea as isPostListContentPresent(), for the Following/Followers user-row lists. */
    public boolean isUserListContentPresent(String emptyStateText) {
        return isAnyVisible(userListContentSignal, id("following.empty"), id("followers.empty"),
                textContains(emptyStateText));
    }

    private final By savedPostSaveAction = AppiumBy.accessibilityId("feed.post.save");
    /** The destructive confirm on SavedPostsScreen's "Unsave post?" alert. */
    private final By unsaveConfirmAction = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND label == 'Unsave'");

    /** Unsaves the first post listed on Saved Posts and confirms the alert the screen
     * raises. Returns false once there is nothing left to unsave.
     *
     * SavedPostsScreen passes requiresUnsaveConfirmation: true, so the bookmark does not
     * flip until the "Unsave post?" alert is confirmed. FeedPage's own handler
     * deliberately taps Cancel on that alert - correct on the feed, where the alert is an
     * interruption, and exactly wrong here, where confirming it is the point. Hence a
     * separate helper rather than reusing FeedPage.tapFirstSaveAction(), which also waits
     * on isAnyFeedItemVisible() and so would time out on the last row. */
    public boolean unsaveFirstListedPost(String emptyStateText) {
        int before = visibleElements(savedPostSaveAction).size();
        if (before == 0) {
            return false;
        }
        if (!tapFirstVisible(savedPostSaveAction)) {
            return false;
        }
        tapFirstVisible(unsaveConfirmAction);
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ignored ->
                    visibleElements(savedPostSaveAction).size() < before
                            || isEmptyStateShown(emptyStateText));
            return true;
        } catch (RuntimeException timedOut) {
            return false;
        }
    }

    public boolean tapLogoutConfirm() {
        return tapFirstVisible(logoutConfirmYes);
    }

    @Override
    public boolean navigateBack() {
        try {
            return super.navigateBack();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean tapLogout() {
        return scrollUntilVisible(logoutAction, text("Logout")) && tapFirstVisible(logoutAction, text("Logout"));
    }

    /** Logout confirmation is a native alert; Yes and No both present is the signal. */
    public boolean isLogoutConfirmDialogVisible() {
        return isAnyVisible(logoutConfirmYes) && isAnyVisible(logoutConfirmNo);
    }

    /** Cancels out of the logout confirmation without logging out. */
    public boolean tapLogoutCancel() {
        return tapFirstVisible(logoutConfirmNo);
    }

    /** Settings -> About Aikyam -> "Open Source Licenses". */
    public boolean openOpenSourceLicensesFromAboutAikyam() {
        return openRow("About Aikyam")
                && isDestinationTitleVisible("About Aikyam")
                && openRow("Open Source Licenses");
    }

    /** iOS: there is no inline "Switch account" section - the header's account capsule
     * opens AccountSwitcherSheet, whose rows are matched by display name. */
    public boolean switchToAccountByName(String accountName) {
        openAccountSwitcherIfClosed();
        return tapFirstVisible(text(accountName), textContains(accountName));
    }

    /** Counterpart of switchToPersonalAccount. The switcher lists the temple account by its
     * own identifier; the display name is the fallback for builds that predate it. */
    public boolean switchToTempleAccount() {
        openAccountSwitcherIfClosed();
        return tapFirstVisible(id("accountSwitcher.temple"), textContains("Temple"));
    }

    public boolean switchToPersonalAccount() {
        openAccountSwitcherIfClosed();
        return tapFirstVisible(id("accountSwitcher.personal"), text("Personal Account"));
    }

    public boolean isSwitchAccountSectionVisible() {
        return isAnyVisible(accountSwitcherCapsule);
    }

    private void openAccountSwitcherIfClosed() {
        if (isVisibleNow(id("accountSwitcher.personal"), id("accountSwitcher.temple"))) {
            return;
        }
        tapFirstVisible(accountSwitcherCapsule);
    }

    private By[] rowLocators(String label) {
        String rowId = ROW_IDS.get(label);
        String title = IOS_TITLES.getOrDefault(label, label);
        return rowId == null
                ? new By[] {text(title)}
                : new By[] {id(rowId), text(title)};
    }

    private static By alertButton(String label) {
        return AppiumBy.iOSClassChain("**/XCUIElementTypeAlert/**/XCUIElementTypeButton[`label == "
                + predicateLiteral(label).replace('\'', '"') + "`]");
    }
}
