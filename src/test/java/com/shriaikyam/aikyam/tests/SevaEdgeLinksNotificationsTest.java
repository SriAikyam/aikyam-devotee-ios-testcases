package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.BookSevaPage;
import com.shriaikyam.aikyam.pages.DeepLinkPage;
import com.shriaikyam.aikyam.pages.SevaOrdersPage;
import com.shriaikyam.aikyam.pages.SevaPaymentPage;
import com.shriaikyam.aikyam.utils.DeviceControl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.time.Duration;

/**
 * Sevas QA report group 7: links and notifications (5 cases). Order links are already covered
 * by SevaOrdersNotificationTest (TC963-964, TC971-975); these are the rest. Real pushes can't be
 * sent or tapped by XCUITest reliably, so push cases need -Daikyam.manualAssist=true.
 */
public class SevaEdgeLinksNotificationsTest extends BaseTest {

    @Override
    protected boolean reuseDriverAcrossTests() {
        return true;
    }

    @AfterClass
    public static void endSharedSession() {
        releaseSharedDriver();
    }

    @After
    public void leaveFlows() {
        if (driver != null) {
            new BookSevaPage(driver).leavePaymentFlowIfOpen();
        }
    }

    /** SEV-I03 (aikyam:// part runs unattended; the https part needs -Daikyam.sevaHttpsLink). */
    @Test
    public void TC1000_Verify_Seva_Share_Link_Opens_The_Temple_Sevas() {
        DeepLinkPage links = new DeepLinkPage(driver);
        Assert.assertTrue(links.fire(SevaTestConfig.requireTempleLink()));
        BookSevaPage seva = new BookSevaPage(driver);
        Assert.assertTrue("aikyam:// link should open the temple and its sevas", seva.openSevasTab());
        Assert.assertTrue(seva.isSevaCardVisible());

        String https = System.getProperty("aikyam.sevaHttpsLink", "").trim();
        Assume.assumeFalse("Skipping https part: set -Daikyam.sevaHttpsLink", https.isEmpty());
        links.returnHome();
        Assert.assertTrue(links.fire(https));
        // Report: universal links are a known gap on device, so this is expected to fail until fixed.
        Assert.assertTrue("https share link should open the temple in the app (known universal-links gap)",
                seva.openSevasTab() && seva.isSevaCardVisible());
    }

    /** SEV-K605: the share sheet is only visible to WDA on a simulator. */
    @Test
    public void TC1001_Verify_Shared_Temple_Link_Opens_That_Temple() {
        Assume.assumeTrue(DeviceControl.SIMULATOR_ONLY_SHARE_SHEET, DeviceControl.isSimulator());
        DeepLinkPage links = new DeepLinkPage(driver);
        Assert.assertTrue(links.fire(SevaTestConfig.requireTempleLink()));
        BookSevaPage seva = new BookSevaPage(driver);
        Assert.assertTrue(seva.openSevasTab());
        Assert.assertTrue("Temple share should open the share sheet",
                links.waitFor(io.appium.java_client.AppiumBy.accessibilityId("templeHome.banner.share"), Duration.ofSeconds(5))
                        && tap("templeHome.banner.share"));
        String shared = links.readLinkFromShareSheet();
        Assert.assertNotNull("Share sheet should carry a link", shared);
        links.returnHome();
        Assert.assertTrue(links.fire(shared));
        Assert.assertTrue("Shared link should open that temple", seva.openSevasTab() && seva.isSevaCardVisible());
    }

    private boolean tap(String accessibilityId) {
        driver.findElement(io.appium.java_client.AppiumBy.accessibilityId(accessibilityId)).click();
        return true;
    }

    /** SEV-K601 */
    @Test
    public void TC1002_Verify_Booking_Confirmation_Push_Opens_The_Order() {
        SevaEdgeConfig.requireManualAssist();
        SevaTestConfig.requirePayUTestMode();
        Assert.assertTrue(new DeepLinkPage(driver).fire(SevaTestConfig.requireTempleLink()));
        BookSevaPage seva = new BookSevaPage(driver);
        Assert.assertTrue(seva.openSevasTab());
        Assert.assertTrue(seva.tapFirstSevaBook());
        Assert.assertTrue(seva.confirmDefaultDate());
        Assert.assertTrue(seva.toggleDevotee(SevaTestConfig.requireSelfName()));
        Assert.assertTrue(seva.tapPlaceSevaOrder());
        Assert.assertTrue(seva.waitForPaymentReviewScreen());
        SevaPaymentPage pay = new SevaPaymentPage(driver);
        Assert.assertTrue(pay.enterEmail(SevaTestConfig.TEST_EMAIL));
        Assert.assertTrue(seva.tapProceedToPaymentInPayUTestMode());
        Assert.assertTrue(pay.waitForPayUCheckout(Duration.ofSeconds(40)));
        SevaTestConfig.askPerson("pay with a PayU TEST card, lock the phone, wait for the booking push, unlock and TAP it", 120);
        Assert.assertTrue("Tapping the booking push should open the order", new SevaOrdersPage(driver).isOrderLoaded());
    }

    /** SEV-K602 */
    @Test
    public void TC1003_Verify_Seva_Completed_Push_Opens_The_Order() {
        SevaEdgeConfig.requireManualAssist();
        new DeepLinkPage(driver).returnHome();
        SevaTestConfig.askPerson("on the temple account (another phone), complete one of this devotee's sevas; "
                + "when the push arrives here, TAP it", 180);
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue("Completed push should open the order", orders.isOrderLoaded());
        Assert.assertTrue(orders.isCompletedStatusVisible());
    }

    /** SEV-K603: confirm with the dev team that reminders exist before enabling. */
    @Test
    public void TC1004_Verify_Reminder_Fires_Before_Seva() {
        SevaEdgeConfig.requireUnblocked("aikyam.remindersImplemented", "seva reminders are not confirmed to exist");
        SevaEdgeConfig.requireManualAssist();
        new DeepLinkPage(driver).returnHome();
        SevaTestConfig.askPerson("make sure a seva is booked for tomorrow; when the reminder arrives, TAP it", 180);
        Assert.assertTrue("Reminder should open the order", new SevaOrdersPage(driver).isOrderLoaded());
    }
}
