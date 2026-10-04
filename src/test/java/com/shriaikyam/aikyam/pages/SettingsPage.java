package com.shriaikyam.aikyam.pages;

import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/** The Settings hub (feature_devotee_settings), reached via
 * ProfilePage.openSettingsHub() - reused rather than duplicated here. Row labels are
 * plain Text with no contentDescription; a disabled ("Coming Soon") row's own Text
 * node still reports enabled=true (same quirk already documented on PostPage's Post
 * button), so isRowDisabled() checks the nearest clickable ancestor instead. */
public class SettingsPage extends BasePage {

    private final By logoutAction = text("Logout");
    private final By logoutConfirmYes = text("Yes");
    private final By logoutConfirmNo = text("No");
    private final By comingSoonBadge = textContains("Coming Soon");
    // Your Posts/Saved/Liked/Commented Posts all reuse the same PostCard component the
    // main Feed uses (confirmed via UserPostsScreen.kt) - Namaskara/Comment/Save are a
    // reliable "real post content rendered" signal shared across all four screens.
    private final By postCardContentSignal = accessibilityId("Namaskara");
    // Following/Followers are user-row lists (avatar + name + Follow/Following button),
    // not PostCards - the Follow/Following button is their equivalent content signal.
    private final By userListContentSignal = By.xpath("//*[@text='Follow' or @text='Following']");

    public SettingsPage(IOSDriver driver) {
        super(driver);
    }

    /** Logout sits at the very bottom of the Settings hub's LazyColumn, well below
     * the fold - only content near the top renders immediately, so this must scroll
     * rather than check visibility once. */
    public boolean isSettingsHubVisible() {
        return scrollUntilVisible(logoutAction);
    }

    public boolean isRowVisible(String label) {
        return scrollUntilVisible(text(label));
    }

    public boolean isComingSoonBadgeVisible() {
        return scrollUntilVisible(comingSoonBadge);
    }

    /** True only if the row's nearest clickable ancestor reports enabled=false - the
     * row's own Text node always reports enabled=true regardless of the row's real
     * disabled state. */
    public boolean isRowDisabled(String label) {
        By rowContainer = By.xpath("(//*[@text='" + label + "']/ancestor::*[@clickable='true'])[1]");
        if (!scrollUntilVisible(text(label))) {
            return false;
        }
        try {
            WebElement container = waitForAnyVisible(rowContainer);
            return !container.isEnabled();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean openRow(String label) {
        return scrollUntilVisible(text(label)) && tapFirstVisible(text(label));
    }

    public boolean isDestinationTitleVisible(String title) {
        try {
            waitForAnyVisible(text(title));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** True if the destination shows either real post content (a PostCard's Namaskara
     * action) or the screen's own empty-state message - never just because the title
     * happened to render, and works regardless of whether the account currently has
     * any posts of that kind. */
    public boolean isEmptyStateShown(String emptyStateText) {
        return isAnyVisible(text(emptyStateText), textContains(emptyStateText));
    }

    public boolean isPostListContentPresent(String emptyStateText) {
        return isAnyVisible(postCardContentSignal, text(emptyStateText), textContains(emptyStateText));
    }

    /** Same idea as isPostListContentPresent(), for the Following/Followers user-row
     * lists instead of PostCard-based screens. */
    public boolean isUserListContentPresent(String emptyStateText) {
        return isAnyVisible(userListContentSignal) || isAnyVisible(text(emptyStateText), textContains(emptyStateText));
    }

    /** Confirms the tapped confirm/Yes button actually leaves the confirmation dialog
     * (Yes/No no longer both present) - a real state-change signal, not just a tap. */
    public boolean tapLogoutConfirm() {
        return tapFirstVisible(logoutConfirmYes);
    }

    public boolean navigateBack() {
        try {
            driver.navigate().back();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean tapLogout() {
        return scrollUntilVisible(logoutAction) && tapFirstVisible(logoutAction);
    }

    /** Confirms the "Are you sure you want to logout?" dialog opened - Yes/No both
     * present is the unambiguous signal (the dialog's own title text is the same
     * string as the row button, so it alone can't distinguish dialog-open from not). */
    public boolean isLogoutConfirmDialogVisible() {
        return isAnyVisible(logoutConfirmYes) && isAnyVisible(logoutConfirmNo);
    }

    /** Cancels out of the logout confirmation without logging out. */
    public boolean tapLogoutCancel() {
        return tapFirstVisible(logoutConfirmNo);
    }

    /** Settings -> About Aikyam -> "Open source licences" (British spelling, matches
     * the app's own string resource). Reuses openRow()/isDestinationTitleVisible(). */
    public boolean openOpenSourceLicensesFromAboutAikyam() {
        return openRow("About Aikyam")
                && isDestinationTitleVisible("About Aikyam")
                && openRow("Open source licences");
    }

    /** Discovered during exploratory QA: this devotee account has a linked temple
     * account, switchable from right here at the top of Settings ("Switch account" ->
     * one row per account, "Personal account" / "Temple account" subtext). Not
     * previously automated - the priest/temple role was assumed to need separate
     * credentials, but it doesn't; it's reachable from this same login. */
    public boolean switchToAccountByName(String accountName) {
        return tapFirstVisible(text(accountName));
    }

    /** Switches back via the "Personal account" subtext rather than the account's own
     * display name, which is per-test-data and not something to hardcode - the "Switch
     * account" section always carries exactly this label under the devotee row. */
    public boolean switchToPersonalAccount() {
        return tapFirstVisible(text("Personal account"));
    }

    public boolean isSwitchAccountSectionVisible() {
        return isAnyVisible(text("Switch account"));
    }
}
