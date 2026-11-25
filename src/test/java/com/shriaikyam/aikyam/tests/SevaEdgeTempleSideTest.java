package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.DeepLinkPage;
import com.shriaikyam.aikyam.pages.PriestPage;
import com.shriaikyam.aikyam.pages.ProfilePage;
import com.shriaikyam.aikyam.pages.SettingsPage;
import com.shriaikyam.aikyam.pages.SevaEdgePage;
import com.shriaikyam.aikyam.pages.SevaOrdersPage;
import com.shriaikyam.aikyam.utils.DeviceControl;
import io.appium.java_client.appmanagement.ApplicationState;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.util.Map;

/**
 * Sevas QA report group 9: temple and priest side (7 cases). Runs on the temple account reached
 * through the account switcher (like PriestTest), so it needs the seeded simulator account with
 * priest membership, plus booking numbers passed as -D inputs (see SevaEdgeConfig).
 */
public class SevaEdgeTempleSideTest extends BaseTest {

    @Override
    protected boolean reuseDriverAcrossTests() {
        return true;
    }

    @AfterClass
    public static void endSharedSession() {
        releaseSharedDriver();
    }

    private boolean onTemple;

    @After
    public void backToPersonal() {
        if (driver != null && onTemple) {
            ProfilePage profile = new ProfilePage(driver);
            if (profile.openSettingsHub()) {
                new SettingsPage(driver).switchToPersonalAccount();
            }
            onTemple = false;
        }
    }

    private PriestPage switchToTemple() {
        Assume.assumeTrue(DeviceControl.SIMULATOR_ONLY_SEEDED_ACCOUNT, DeviceControl.isSimulator());
        Assert.assertTrue("Settings hub should open", new ProfilePage(driver).openSettingsHub());
        Assert.assertTrue("Should switch to the temple account", new SettingsPage(driver).switchToTempleAccount());
        onTemple = true;
        PriestPage priest = new PriestPage(driver);
        Assert.assertTrue("Temple bottom nav should show", priest.isPriestBottomNavVisible());
        return priest;
    }

    /** Temple Orders -> booking card with this number -> Seva Order Details. */
    private SevaEdgePage openTempleBooking(String bookingNo) {
        PriestPage priest = switchToTemple();
        priest.tapOrdersTab();
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertFalse(edge.isTextShown(SevaEdgePage.NO_TEMPLE_CONTEXT));
        Assert.assertTrue("Booking " + bookingNo + " should be listed", edge.tapText(bookingNo));
        Assert.assertTrue("Seva Order Details should open", edge.isTextShown("Seva Order Details"));
        return edge;
    }

    private void setPermission(String service, String value) {
        driver.executeScript("mobile: setPermission",
                Map.of("bundleId", DeepLinkPage.DEV_BUNDLE_ID, "access", Map.of(service, value)));
    }

    private void assertAlive() {
        Assert.assertEquals("App should not crash", ApplicationState.RUNNING_IN_FOREGROUND,
                driver.queryAppState(DeepLinkPage.DEV_BUNDLE_ID));
    }

    /** SEV-K507: setPermission is simulator-only. */
    @Test
    public void TC1013_Verify_Denied_Camera_Shows_Permission_Message() {
        String no = SevaEdgeConfig.require("aikyam.todayBookingNo", "temple-side booking dated today, not completed");
        Assume.assumeTrue(DeviceControl.isSimulator());
        setPermission("camera", "no");
        SevaEdgePage edge = openTempleBooking(no);
        Assert.assertTrue(edge.tapPerformSeva() || edge.isTextShown("Upload Content"));
        Assert.assertTrue(edge.tapTakePhoto());
        edge.denySystemPrompt();
        Assert.assertTrue("Expected: " + SevaEdgePage.CAMERA_PERMISSION,
                edge.isTextShown(SevaEdgePage.CAMERA_PERMISSION) || edge.isTextShown("Camera is not available"));
        assertAlive();
    }

    /** SEV-K508 */
    @Test
    public void TC1014_Verify_Denied_Microphone_Shows_Permission_Message() {
        String no = SevaEdgeConfig.require("aikyam.todayBookingNo", "temple-side booking dated today, not completed");
        Assume.assumeTrue(DeviceControl.isSimulator());
        setPermission("microphone", "no");
        SevaEdgePage edge = openTempleBooking(no);
        Assert.assertTrue(edge.tapPerformSeva() || edge.isTextShown("Upload Content"));
        Assert.assertTrue(edge.tapRecordAudio());
        edge.denySystemPrompt();
        Assert.assertTrue("Expected: " + SevaEdgePage.MIC_PERMISSION, edge.isTextShown(SevaEdgePage.MIC_PERMISSION));
        assertAlive();
    }

    /** SEV-K513: changes data (completes the booking), so the seeded booking is used once. */
    @Test
    public void TC1015_Verify_Completing_On_The_Seva_Date_Marks_It_Completed() {
        String no = SevaEdgeConfig.require("aikyam.todayBookingNo", "temple-side booking dated today, not completed");
        SevaEdgePage edge = openTempleBooking(no);
        Assert.assertTrue(edge.tapPerformSeva() || edge.isTextShown("Upload Content"));
        Assert.assertTrue(edge.typeMessage("Seva performed. Blessings."));
        Assert.assertTrue(edge.tapComplete());
        Assert.assertTrue("Expected: " + SevaEdgePage.ORDER_COMPLETED, edge.isTextShown(SevaEdgePage.ORDER_COMPLETED));
        Assert.assertTrue("Status should become Completed", edge.isTextShown("Completed"));
        // Devotee notification: TC1003 (SEV-K602) covers the push.
    }

    /** SEV-K514 */
    @Test
    public void TC1016_Verify_Editing_A_Completed_Seva_Shows_Updated_Message() {
        String no = SevaEdgeConfig.require("aikyam.completedTempleBookingNo", "temple-side Completed booking");
        SevaEdgePage edge = openTempleBooking(no);
        Assert.assertTrue("Update should be offered on a completed seva", edge.isUpdateShown());
        Assert.assertTrue(edge.typeMessage(" Edited " + System.currentTimeMillis()));
        Assert.assertTrue(edge.tapUpdate());
        Assert.assertTrue(edge.isTextShown(SevaEdgePage.DETAILS_UPDATED));
    }

    /** SEV-H09 */
    @Test
    public void TC1017_Verify_Completed_Seva_Update_Persists_After_Reopening() {
        String no = SevaEdgeConfig.require("aikyam.completedTempleBookingNo", "temple-side Completed booking");
        String marker = "QA" + System.currentTimeMillis();
        SevaEdgePage edge = openTempleBooking(no);
        Assert.assertTrue(edge.typeMessage(" " + marker));
        Assert.assertTrue(edge.tapUpdate());
        Assert.assertTrue(edge.isTextShown(SevaEdgePage.DETAILS_UPDATED));
        edge.back();
        Assert.assertTrue(edge.tapText(no));
        Assert.assertTrue("Edit should persist after reopening", edge.scrollToText(marker));
    }

    /** SEV-K515 */
    @Test
    public void TC1018_Verify_Devotee_Sees_Priest_Media_And_Message() {
        String orderId = SevaEdgeConfig.require("aikyam.completedMediaOrderId", "devotee order completed with media + message");
        String message = SevaEdgeConfig.require("aikyam.priestMessage", "the priest message on that order");
        Assert.assertTrue(new DeepLinkPage(driver).fire("aikyam://orders/" + orderId));
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue(orders.isOrderLoaded());
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue("Priest media should be visible", edge.isAnyMediaShown());
        Assert.assertTrue("Priest message should be visible",
                edge.scrollToText(message.substring(0, Math.min(20, message.length()))));
    }

    /** SEV-K519: the header itself is only visible in the proxy log; here the app must not fall back to
     * the personal context, which shows as "No active temple context" or an empty temple order list. */
    @Test
    public void TC1019_Verify_Temple_Actions_Use_The_Temple_Context() {
        PriestPage priest = switchToTemple();
        priest.tapOrdersTab();
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertFalse("Orders by date must use the temple context", priest.isTempleContextErrorVisible());
        Assert.assertTrue("Temple Orders should load", edge.isTextShown("Seva Bookings")
                || edge.isTextShown("Pending Acceptance") || edge.isTextShown("No bookings for this day"));
        String no = System.getProperty("aikyam.todayBookingNo", "").trim();
        if (!no.isEmpty() && edge.tapText(no)) {
            Assert.assertFalse("Perform Seva must use the temple context", edge.isTextShown(SevaEdgePage.NO_TEMPLE_CONTEXT));
            Assert.assertFalse(edge.isTextShown("Invalid booking id"));
        }
    }
}
