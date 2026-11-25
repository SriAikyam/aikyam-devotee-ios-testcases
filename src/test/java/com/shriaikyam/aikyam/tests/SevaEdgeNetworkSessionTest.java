package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.BookSevaPage;
import com.shriaikyam.aikyam.pages.DeepLinkPage;
import com.shriaikyam.aikyam.pages.HomePage;
import com.shriaikyam.aikyam.pages.SevaEdgePage;
import com.shriaikyam.aikyam.pages.SevaOrdersPage;
import com.shriaikyam.aikyam.pages.SevaPaymentPage;
import io.appium.java_client.appmanagement.ApplicationState;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

/**
 * Sevas QA report group 5: network and session (9 cases). XCUITest can't switch the network off
 * or expire a token, so every case needs a person at the phone (-Daikyam.manualNetwork=true or
 * -Daikyam.manualAssist=true) who acts when the log prints ">>> ACTION ON THE PHONE". Throttling
 * means Settings > Developer > Network Link Conditioner ("Very Bad Network").
 */
public class SevaEdgeNetworkSessionTest extends BaseTest {

    @Override
    protected boolean reuseDriverAcrossTests() {
        return true;
    }

    @AfterClass
    public static void endSharedSession() {
        releaseSharedDriver();
    }

    private static final int TOGGLE = 20;

    @After
    public void restoreNetwork() {
        if (driver != null && (Boolean.getBoolean("aikyam.manualNetwork") || Boolean.getBoolean("aikyam.manualAssist"))) {
            SevaTestConfig.askPerson("airplane mode OFF and Network Link Conditioner OFF", 5);
            new BookSevaPage(driver).leavePaymentFlowIfOpen();
        }
    }

    private BookSevaPage openSevas() {
        Assert.assertTrue(new DeepLinkPage(driver).fire(SevaTestConfig.requireTempleLink()));
        BookSevaPage seva = new BookSevaPage(driver);
        Assert.assertTrue("Sevas tab should open", seva.openSevasTab());
        return seva;
    }

    private BookSevaPage openFilledForm() {
        BookSevaPage seva = openSevas();
        Assert.assertTrue(seva.tapFirstSevaBook());
        Assert.assertTrue(seva.confirmDefaultDate());
        Assert.assertTrue(seva.toggleDevotee(SevaTestConfig.requireSelfName()));
        return seva;
    }

    private void assertAlive() {
        Assert.assertEquals("App should still be running", ApplicationState.RUNNING_IN_FOREGROUND,
                driver.queryAppState(DeepLinkPage.DEV_BUNDLE_ID));
        Assert.assertFalse("No raw technical error", new SevaPaymentPage(driver).isRawTechnicalErrorVisible());
    }

    /** SEV-E05 */
    @Test
    public void TC976_Verify_Network_Drop_At_Confirm_Shows_Check_Upcoming_Message() {
        SevaTestConfig.requireManualNetwork();
        SevaTestConfig.requirePayUTestMode();
        BookSevaPage seva = openFilledForm();
        Assert.assertTrue(seva.tapPlaceSevaOrder());
        Assert.assertTrue(seva.waitForPaymentReviewScreen());
        SevaPaymentPage pay = new SevaPaymentPage(driver);
        Assert.assertTrue(pay.enterEmail(SevaTestConfig.TEST_EMAIL));
        Assert.assertTrue(seva.tapProceedToPaymentInPayUTestMode());
        Assert.assertTrue(pay.waitForPayUCheckout(Duration.ofSeconds(40)));
        SevaTestConfig.askPerson("pay in PayU with a TEST card, then turn airplane mode ON as soon as it returns", 60);
        SevaTestConfig.askPerson("turn airplane mode OFF", TOGGLE);
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue("Expected 'The connection dropped ... Check Upcoming Sevas before booking again.'",
                edge.isTextShown(SevaEdgePage.CONNECTION_DROPPED));
        assertAlive();
    }

    /** SEV-K608 */
    @Test
    public void TC977_Verify_Low_Connectivity_Place_Order_Retries_Safely_With_One_Hold() {
        SevaTestConfig.requireManualNetwork();
        int before = countListedOrders();
        BookSevaPage seva = openFilledForm();
        SevaTestConfig.askPerson("turn Network Link Conditioner ON (Very Bad Network)", TOGGLE);
        seva.tapPlaceSevaOrder();
        boolean reached = seva.waitForPaymentReviewScreen();
        if (!reached && seva.isAlertVisible()) {
            seva.dismissAlert();
        }
        SevaTestConfig.askPerson("turn Network Link Conditioner OFF", TOGGLE);
        if (!reached) {
            Assert.assertTrue("Form should still be there for a retry", seva.isBookingDetailsScreenVisible());
            Assert.assertTrue(seva.tapPlaceSevaOrder());
            Assert.assertTrue("Retry should reach Review & Pay", seva.waitForPaymentReviewScreen());
        }
        Assert.assertTrue(seva.cancelPaymentReview());
        assertAlive();
        int after = countListedOrders();
        Assert.assertTrue("At most one new held order (before " + before + ", after " + after + ")",
                after - before <= 1);
    }

    private int countListedOrders() {
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue(orders.openOrderHistory());
        Assert.assertTrue(orders.isEmptyOrListLoaded());
        int n = 0;
        for (String t : new SevaEdgePage(driver).visibleTexts()) {
            if (t.startsWith("ORD")) {
                n++;
            }
        }
        return n;
    }

    /** SEV-J06 */
    @Test
    public void TC978_Verify_Slow_Network_Shows_Loaders_And_Settles() {
        SevaTestConfig.requireManualNetwork();
        SevaTestConfig.askPerson("turn Network Link Conditioner ON (3G or Very Bad Network)", TOGGLE);
        SevaEdgePage edge = new SevaEdgePage(driver);
        BookSevaPage seva = openSevas();
        Assert.assertTrue("Sevas list should settle", edge.waitUntilSettled(Duration.ofSeconds(60)));
        Assert.assertTrue("Sevas should show once data arrives", seva.isSevaCardVisible());
        Assert.assertTrue(seva.tapFirstSevaDetails());
        Assert.assertTrue("Seva details should settle", edge.waitUntilSettled(Duration.ofSeconds(60)));
        Assert.assertTrue(seva.isCatalogDetailsScreenVisible());
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue(orders.openOrderHistory());
        Assert.assertTrue("Order History should settle", edge.waitUntilSettled(Duration.ofSeconds(60)));
        Assert.assertTrue(orders.isEmptyOrListLoaded());
        assertAlive();
    }

    /** SEV-J07 */
    @Test
    public void TC979_Verify_Expired_Session_On_Book_Leads_To_Re_Login() {
        SevaEdgeConfig.requireManualAssist();
        BookSevaPage seva = openSevas();
        SevaTestConfig.askPerson("expire this devotee's access AND refresh token on the backend (dev)", 60);
        seva.tapFirstSevaBook();
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue("Expected the re-login flow ('Session expired. Please sign in again.' or the login screen)",
                edge.isTextShown(SevaEdgePage.SESSION_EXPIRED)
                        || new com.shriaikyam.aikyam.pages.LoginPage(driver).isPhoneNumberScreenVisible());
        Assert.assertFalse("Must not be a blank screen", edge.visibleTexts().isEmpty());
        assertAlive();
    }

    /** SEV-K610 */
    @Test
    public void TC980_Verify_Token_Expiry_On_Form_Keeps_Form_Data_After_Re_Login() {
        SevaEdgeConfig.requireManualAssist();
        BookSevaPage seva = openFilledForm();
        int devotees = seva.readTotalDevoteesCount();
        SevaTestConfig.askPerson("expire this devotee's access token on the backend (keep the refresh token)", 60);
        seva.tapPlaceSevaOrder();
        if (!seva.waitForPaymentReviewScreen()) {
            SevaTestConfig.askPerson("if the app asked you to sign in, sign in again as the same devotee", 90);
            Assert.assertTrue("Should be back on the booking form", seva.isBookingDetailsScreenVisible());
            Assert.assertEquals("Selected devotees should be kept", devotees, seva.readTotalDevoteesCount());
        } else {
            Assert.assertTrue("Token refreshed silently: Review & Pay reached", seva.cancelPaymentReview());
        }
        assertAlive();
    }

    /** SEV-K409 */
    @Test
    public void TC981_Verify_Order_History_Shows_Cached_Orders_Offline_Then_Refreshes() {
        SevaTestConfig.requireManualNetwork();
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue(orders.openOrderHistory());
        Assert.assertTrue("Precondition: at least one order online", orders.isAnyOrderListed());
        new DeepLinkPage(driver).returnHome();
        SevaTestConfig.askPerson("turn airplane mode ON", TOGGLE);
        Assert.assertTrue(orders.openOrderHistory());
        Assert.assertTrue("Cached orders should show offline", orders.isAnyOrderListed());
        SevaTestConfig.askPerson("turn airplane mode OFF and wait for Wi-Fi", TOGGLE);
        new SevaEdgePage(driver).refresh();
        Assert.assertTrue("Orders should refresh online", orders.isAnyOrderListed());
        assertAlive();
    }

    /** SEV-A07 */
    @Test
    public void TC982_Verify_Sevas_List_Offline_Shows_Error_With_Retry_That_Reloads() {
        SevaTestConfig.requireManualNetwork();
        SevaTestConfig.askPerson("turn airplane mode ON", TOGGLE);
        Assert.assertTrue(new DeepLinkPage(driver).fire(SevaTestConfig.requireTempleLink()));
        BookSevaPage seva = new BookSevaPage(driver);
        seva.openSevasTab();
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue(edge.isTextShown(SevaEdgePage.SEVAS_NETWORK_ERROR));
        Assert.assertTrue("Retry should be offered", edge.isRetryVisible());
        SevaTestConfig.askPerson("turn airplane mode OFF and wait for Wi-Fi", TOGGLE);
        Assert.assertTrue(edge.tapRetry());
        Assert.assertTrue("Retry should load the sevas", seva.isSevaCardVisible());
    }

    /** SEV-C05 */
    @Test
    public void TC983_Verify_Seva_Details_Offline_Shows_Error_With_Retry_Or_Back() {
        SevaTestConfig.requireManualNetwork();
        BookSevaPage seva = openSevas();
        Assert.assertTrue(seva.isSevaCardVisible());
        SevaTestConfig.askPerson("turn airplane mode ON", TOGGLE);
        Assert.assertTrue(seva.tapFirstSevaDetails());
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue(edge.isTextShown(SevaEdgePage.SEVA_DETAILS_ERROR));
        Assert.assertTrue("A way out: Retry or Back", edge.isRetryVisible() || edge.back());
        assertAlive();
    }

    /** SEV-K309: search isn't reachable on iOS yet (report: Blocked). */
    @Test
    public void TC984_Verify_Search_Offline_Shows_Error_With_Retry() {
        SevaEdgeConfig.requireUnblocked("aikyam.searchReachable", "search is not reachable on iOS");
        SevaTestConfig.requireManualNetwork();
        SevaTestConfig.askPerson("turn airplane mode ON", TOGGLE);
        new HomePage(driver).tapExploreTab();
        SevaTestConfig.askPerson("type a temple name into search and submit it", TOGGLE);
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue("Search offline should offer Retry", edge.isRetryVisible());
        assertAlive();
    }
}
