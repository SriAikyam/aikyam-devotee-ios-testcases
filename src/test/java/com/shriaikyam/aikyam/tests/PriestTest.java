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
 * the shared session onto the temple account and TC170 switches it back, so every test
 * in between - and every other class that runs after this one - finds the session in
 * the state it expects.
 *
 * TC163/TC168 encode a confirmed, currently-reproducing defect: Orders and Order History
 * both render the literal string "No active temple context found" instead of order data
 * or a designed empty state, on both Android 10 and 11. TC169 is the contrast case (Your
 * Posts on the same account, same session, shows a correct empty state) that is what
 * confirms this is scoped to the orders/booking data path rather than a blanket session
 * failure. TC163/TC168 are expected to FAIL until the underlying bug is fixed.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PriestTest extends BaseTest {

    private static final String TEMPLE_ACCOUNT_NAME = "Chennakesava Temple";

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

    /** Regression test - see class doc. Expected to FAIL until fixed. Reached via the
     * priest Settings screen (not the Orders tab) - the same error appearing here too
     * is what confirms this is a data-path bug, not specific to one screen. */
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

}
