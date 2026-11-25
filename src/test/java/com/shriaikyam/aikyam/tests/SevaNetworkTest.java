package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.BookSevaPage;
import com.shriaikyam.aikyam.pages.DeepLinkPage;
import com.shriaikyam.aikyam.pages.SevaOrdersPage;
import com.shriaikyam.aikyam.pages.SevaPaymentPage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

/**
 * Network loss during booking and payment (Android TC942-TC947). XCUITest cannot switch the
 * network off, so these run only with -Daikyam.manualNetwork=true and a person at the phone who
 * toggles airplane mode when the log prints ">>> ACTION ON THE PHONE". Otherwise they skip.
 *
 * UNVERIFIED: none of TC942-TC947 has been run on a device yet; treat a failure here as a possible
 * test bug (labels, timings) until a manual run confirms them.
 */
public class SevaNetworkTest extends BaseTest {

    /** One phone session for the whole class (see BaseTest); starting WDA per case cost ~30 s each. */
    @Override
    protected boolean reuseDriverAcrossTests() {
        return true;
    }

    @AfterClass
    public static void endSharedSession() {
        releaseSharedDriver();
    }

    private static final int TOGGLE_SECONDS = 20;

    @After
    public void backOnline() {
        if (driver != null && Boolean.getBoolean("aikyam.manualNetwork")) {
            SevaTestConfig.askPerson("make sure airplane mode is OFF", 5);
            new BookSevaPage(driver).leavePaymentFlowIfOpen();
        }
    }

    private BookSevaPage openFilledForm() {
        String link = SevaTestConfig.requireTempleLink();
        Assert.assertTrue(new DeepLinkPage(driver).fire(link));
        BookSevaPage seva = new BookSevaPage(driver);
        Assert.assertTrue(seva.openSevasTab());
        Assert.assertTrue(seva.tapFirstSevaBook());
        Assert.assertTrue(seva.confirmDefaultDate());
        Assert.assertTrue(seva.toggleDevotee(SevaTestConfig.requireSelfName()));
        return seva;
    }

    @Test
    public void TC942_Verify_Place_Seva_Order_Offline_Shows_A_Readable_Error_And_Keeps_The_Form() {
        SevaTestConfig.requireManualNetwork();
        BookSevaPage seva = openFilledForm();
        int devotees = seva.readTotalDevoteesCount();
        SevaTestConfig.askPerson("turn airplane mode ON", TOGGLE_SECONDS);
        Assert.assertTrue(seva.tapPlaceSevaOrder());
        Assert.assertFalse("Offline submit must not reach Review & Pay", seva.waitForPaymentReviewScreen());
        String message = seva.readAlertMessage();
        Assert.assertNotNull("Offline submit should explain itself in an alert", message);
        Assert.assertFalse("Alert should be readable, not raw: " + message,
                message.contains("NSURLError") || message.contains("Exception"));
        Assert.assertTrue(seva.dismissAlert());
        Assert.assertTrue("The form should still be there", seva.isBookingDetailsScreenVisible());
        Assert.assertEquals("The selected devotee should be kept", devotees, seva.readTotalDevoteesCount());
    }

    @Test
    public void TC943_Verify_Submit_After_Reconnect_Reaches_Review_Once() {
        SevaTestConfig.requireManualNetwork();
        BookSevaPage seva = openFilledForm();
        SevaTestConfig.askPerson("turn airplane mode ON", TOGGLE_SECONDS);
        seva.tapPlaceSevaOrder();
        Assert.assertTrue("Offline submit should show an alert", seva.isAlertVisible());
        seva.dismissAlert();
        SevaTestConfig.askPerson("turn airplane mode OFF and wait for Wi-Fi", TOGGLE_SECONDS);
        Assert.assertTrue(seva.tapPlaceSevaOrder());
        Assert.assertTrue("Should reach Review & Pay after reconnect", seva.waitForPaymentReviewScreen());
        Assert.assertTrue(seva.cancelPaymentReview());
        Assert.assertFalse("Only one Review & Pay should be stacked", seva.isPaymentReviewVisibleNow());
    }

    @Test
    public void TC944_Verify_Proceed_To_Payment_Offline_Does_Not_Open_PayU() {
        SevaTestConfig.requireManualNetwork();
        SevaTestConfig.requirePayUTestMode();
        BookSevaPage seva = openFilledForm();
        Assert.assertTrue(seva.tapPlaceSevaOrder());
        Assert.assertTrue(seva.waitForPaymentReviewScreen());
        SevaPaymentPage pay = new SevaPaymentPage(driver);
        Assert.assertTrue(pay.enterEmail(SevaTestConfig.TEST_EMAIL));
        SevaTestConfig.askPerson("turn airplane mode ON", TOGGLE_SECONDS);
        seva.tapProceedToPaymentInPayUTestMode();
        Assert.assertFalse("PayU must not open offline", pay.waitForPayUCheckout(Duration.ofSeconds(15)));
        Assert.assertFalse("No raw technical error offline", pay.isRawTechnicalErrorVisible());
    }

    @Test
    public void TC947_Verify_Order_Link_Offline_Then_Online_Recovers() {
        SevaTestConfig.requireManualNetwork();
        String orderId = SevaTestConfig.requireOwnOrderId();
        DeepLinkPage links = new DeepLinkPage(driver);
        Assert.assertTrue(links.forceStopApp());
        SevaTestConfig.askPerson("turn airplane mode ON", TOGGLE_SECONDS);
        Assert.assertTrue(links.fire("aikyam://orders/" + orderId));
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue("Offline with no cached order: 'Couldn't load order details' + Retry",
                orders.isLoadErrorVisible());
        Assert.assertFalse(new SevaPaymentPage(driver).isRawTechnicalErrorVisible());
        SevaTestConfig.askPerson("turn airplane mode OFF and wait for Wi-Fi", TOGGLE_SECONDS);
        Assert.assertTrue(links.fire("aikyam://orders/" + orderId));
        Assert.assertTrue("Order should load once online", orders.isOrderLoaded());
    }
}
