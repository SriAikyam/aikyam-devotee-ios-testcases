package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.PriestPage;
import com.shriaikyam.aikyam.pages.PriestSettingsPage;
import com.shriaikyam.aikyam.pages.ProfilePage;
import com.shriaikyam.aikyam.pages.SettingsPage;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * The priest/temple role, reached from this same devotee login via account-switching
 * (Settings -> Switch account) - discovered during exploratory QA. None of this module
 * had any automation before this pass; it had been assumed to need separate credentials.
 *
 * Pinned to TC-number order for the same reason as AuthenticationTest: TC161 switches
 * the shared session onto the temple account and TC270 switches it back, so every test
 * in between - and every other class that runs after this one - finds the session in
 * the state it expects.
 *
 * TC168 asserts Order History does not render the raw "No active temple context" error
 * instead of order data or a designed empty state.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PriestTest extends BaseTest {

    private static final String TEMPLE_ACCOUNT_NAME = "Chennakesava Temple";

    /** Puts the shared session on the temple account.
     *
     * This and TC270 were missing from the iOS port, while the three tests that DEPEND on
     * them (TC164/166/168) were ported - so the priest tests ran against the devotee
     * account and asserted priest-only UI, and nothing restored the devotee context
     * afterwards. In the 14 Oct p1Rest run that cost 22 of 37 failures: PriestTest's own 2,
     * plus every class sorting after it (ProfileTest 4, SearchJourneyTest 5, SettingsTest 6,
     * StoriesJourneyTest 5) failing wholesale on timeouts. The class doc above already
     * described this contract; only the tests enforcing it were absent. */
    @Test
    public void TC161_Verify_Switch_To_Temple_Account() {
        ProfilePage profilePage = new ProfilePage(driver);
        SettingsPage settingsPage = new SettingsPage(driver);
        PriestPage priestPage = new PriestPage(driver);

        Assert.assertTrue("Settings hub should open", profilePage.openSettingsHub());
        Assert.assertTrue("Switch account section should be visible",
                settingsPage.isSwitchAccountSectionVisible());
        Assert.assertTrue("Temple account should be selectable",
                settingsPage.switchToTempleAccount());
        Assert.assertTrue("Priest bottom navigation should appear",
                priestPage.isPriestBottomNavVisible());
    }

    @Test
    public void TC164_Verify_Priest_Events_Tab_Opens() {
        PriestPage priestPage = new PriestPage(driver);
        priestPage.tapEventsTab();

        Assert.assertFalse(
                "Priest Events tab should not show the session error either",
                priestPage.isTempleContextErrorVisible()
        );
    }

    @Test
    public void TC166_Verify_Priest_Apps_Tab_Shows_Coming_Soon() {
        PriestPage priestPage = new PriestPage(driver);
        priestPage.tapAppsTab();

        Assert.assertTrue("Apps should be marked Coming Soon", priestPage.isAppsComingSoonVisible());
    }

    /** Regression test - see class doc. Reached via the priest Settings screen (not the
     * Orders tab). */
    @Test
    public void TC168_Verify_Priest_Settings_Order_History_Does_Not_Show_Session_Error() {
        ProfilePage profilePage = new ProfilePage(driver);
        PriestSettingsPage priestSettingsPage = new PriestSettingsPage(driver);

        Assert.assertTrue("Priest Settings should open", profilePage.openSettingsHub());
        Assert.assertTrue("Order History should be reachable", priestSettingsPage.openOrderHistory());
        Assert.assertFalse(
                "Order History should show order data or a designed empty state, not a raw session error",
                priestSettingsPage.isTempleContextErrorVisible()
        );
    }


    /** Restores the shared session to the personal/devotee account. Every other class in
     * the suite assumes that starting state - without this, PriestTest leaves the session
     * on the temple account and poisons everything that follows it. */
    @Test
    public void TC270_Verify_Switch_Back_To_Personal_Account() {
        ProfilePage profilePage = new ProfilePage(driver);
        SettingsPage settingsPage = new SettingsPage(driver);

        Assert.assertTrue("Settings hub should open", profilePage.openSettingsHub());
        Assert.assertTrue("Personal account should be selectable",
                settingsPage.switchToPersonalAccount());
    }
}
