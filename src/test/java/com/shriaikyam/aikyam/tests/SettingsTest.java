package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.FeedPage;
import com.shriaikyam.aikyam.pages.LoginPage;
import com.shriaikyam.aikyam.pages.ProfilePage;
import com.shriaikyam.aikyam.pages.SettingsPage;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.shriaikyam.aikyam.categories.P0;

/** Personal Profile and Family Profile rows are deliberately not re-tested here -
 * they're the exact same locators/flow already covered by ProfilePage/FamilyMembersTest.
 * Order History is visibility-only per the standing Orders exclusion. */
public class SettingsTest extends BaseTest {
    /** Upgraded from "title visible" to confirming the screen actually renders real
     * post content (a PostCard) or its own empty state - both real signals that were
     * previously indistinguishable from a blank/broken screen sharing the same title. */
    @Category(P0.class)
    @Test
    public void TC095_Verify_Your_Posts_Row_Opens() {
        assertPostListRowOpensDestination("Your Posts", "Your Posts", "No posts");
    }

    @Category(P0.class)
    @Test
    public void TC096_Verify_Following_Row_Opens() {
        assertUserListRowOpensDestination("Following", "Following", "Not following anyone yet");
    }

    @Test
    public void TC098_Verify_Saved_Posts_Row_Opens() {
        assertPostListRowOpensDestination("Saved Posts", "Saved Posts", "No saved posts");
    }

    @Test
    public void TC099_Verify_Liked_Posts_Row_Opens() {
        assertPostListRowOpensDestination("Liked Posts", "Liked Posts", "No liked posts");
    }

    @Test
    public void TC100_Verify_Commented_Posts_Row_Opens() {
        assertPostListRowOpensDestination("Commented Posts", "Commented Posts", "No commented posts");
    }

    @Test
    public void TC101_Verify_Search_History_Row_Opens() {
        assertRowOpensDestination("Search History", "Search History");
    }

    @Test
    public void TC103_Verify_App_Language_Row_Opens() {
        assertRowOpensDestination("App Language", "App Language");
    }

    @Test
    public void TC117_Verify_Open_Source_Licenses_Screen_Opens() {
        SettingsPage settingsPage = openSettingsHub();

        Assert.assertTrue("About Aikyam -> Open Source Licenses should be reachable",
                settingsPage.openOpenSourceLicensesFromAboutAikyam());
        Assert.assertTrue("Open Source Licenses screen should be visible",
                settingsPage.isDestinationTitleVisible("Open Source Licenses"));
        // Two levels deep (About Aikyam -> Open source licences) - two pops needed.
        settingsPage.navigateBack();
        settingsPage.navigateBack();
    }

    private void assertRowOpensDestination(String rowLabel, String destinationTitle) {
        SettingsPage settingsPage = openSettingsHub();

        Assert.assertTrue(rowLabel + " row should be tappable", settingsPage.openRow(rowLabel));
        Assert.assertTrue(destinationTitle + " screen should open",
                settingsPage.isDestinationTitleVisible(destinationTitle));
        settingsPage.navigateBack();
    }

    /** Every row-opening test here must leave the app back at the Settings hub (or
     * closer to it) before finishing - confirmed live: with no cleanup, each
     * subsequent test's openSettingsHub() inherits a progressively deeper,
     * un-recovered navigation stack (this destination, then the next, then the next),
     * until it's too deep for that method's own retry budget to unwind and every
     * later test in the run fails identically. Best-effort (not asserted) since a
     * failed cleanup shouldn't mask this test's own real result. */
    private void assertPostListRowOpensDestination(String rowLabel, String destinationTitle, String emptyStateText) {
        SettingsPage settingsPage = openSettingsHub();

        Assert.assertTrue(rowLabel + " row should be tappable", settingsPage.openRow(rowLabel));
        Assert.assertTrue(destinationTitle + " screen should open",
                settingsPage.isDestinationTitleVisible(destinationTitle));
        Assert.assertTrue(
                destinationTitle + " should show real post content or its own empty state, not just the title",
                settingsPage.isPostListContentPresent(emptyStateText)
        );
        settingsPage.navigateBack();
    }

    private void assertUserListRowOpensDestination(String rowLabel, String destinationTitle, String emptyStateText) {
        SettingsPage settingsPage = openSettingsHub();

        Assert.assertTrue(rowLabel + " row should be tappable", settingsPage.openRow(rowLabel));
        Assert.assertTrue(destinationTitle + " screen should open",
                settingsPage.isDestinationTitleVisible(destinationTitle));
        Assert.assertTrue(
                destinationTitle + " should show real user rows or its own empty state, not just the title",
                settingsPage.isUserListContentPresent(emptyStateText)
        );
        settingsPage.navigateBack();
    }

    private boolean waitForPhoneScreen(LoginPage loginPage) {
        for (int attempt = 0; attempt < 25; attempt++) {
            if (loginPage.isPhoneNumberScreenVisible()) {
                return true;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return loginPage.isPhoneNumberScreenVisible();
    }

    /** Best-effort: catches Throwable, not just RuntimeException - completeOtpEntry()
     * uses real Assert.assertTrue() calls (AssertionError, not a RuntimeException), so
     * a hiccup here must not escape uncaught and masquerade as this test's own result
     * (same fix already applied in AuthenticationTest). */
    private void restoreSessionBestEffort(LoginPage loginPage, FeedPage feedPage) {
        try {
            if (!loginPage.isPhoneNumberScreenVisible()) {
                return;
            }
            // iOS simulator has no SMS inbox; the OTP comes only from the environment.
            String otp = System.getenv("AIKYAM_TEST_OTP");
            loginPage.enterPhoneNumber(System.getenv("AIKYAM_TEST_PHONE"));
            loginPage.tapSendVerificationCode();
            loginPage.dismissSmsConsentIfPresent();
            loginPage.enterOtp(otp);
            loginPage.waitForDashboard(feedPage);
        } catch (Throwable ignored) {
            // Best-effort only.
        }
    }

    private SettingsPage openSettingsHub() {
        ProfilePage profilePage = new ProfilePage(driver);
        Assert.assertTrue("Settings hub should open", profilePage.openSettingsHub());
        return new SettingsPage(driver);
    }
}
