package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.categories.P0;
import com.shriaikyam.aikyam.pages.DeepLinkPage;
import com.shriaikyam.aikyam.pages.SevaOrdersPage;
import io.appium.java_client.appmanagement.ApplicationState;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * After payment: Order History, Order Details, and the push links that open an order.
 * A seva push ("Your seva is confirmed" / "complete" / "Photos ready") carries
 * aikyam://orders/{sevaInstanceId}; firing that link is what tapping the push does, so these
 * tests fire it directly. Receiving a real push stays a manual case.
 *
 * TC963/TC964/TC971/TC972 need app PR aikyam-devotee-ios#882 (order link routing and the
 * "Order details not available" state); on an older build the link is ignored.
 * TC969 (Android POST_NOTIFICATIONS) has no iOS equivalent that automation can toggle.
 */
public class SevaOrdersNotificationTest extends BaseTest {

    /** One phone session for the whole class (see BaseTest); starting WDA per case cost ~30 s each. */
    @Override
    protected boolean reuseDriverAcrossTests() {
        return true;
    }

    @org.junit.AfterClass
    public static void endSharedSession() {
        releaseSharedDriver();
    }

    @Category(P0.class)
    @Test
    public void TC961_Verify_Order_History_Lists_An_Order_With_A_Status() {
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue("aikyam://orders should open Order History", orders.openOrderHistory());
        Assert.assertTrue("An order with its ORD number and status should be listed", orders.isAnyOrderListed());
    }

    @Category(P0.class)
    @Test
    public void TC962_Verify_Tapping_An_Order_Card_Opens_Its_Details() {
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue(orders.openOrderHistory());
        String bookingNo = System.getProperty("aikyam.ownBookingNo", "").replaceFirst("^ORD", "");
        Assert.assertTrue("An order card should be tappable",
                bookingNo.isEmpty() ? orders.tapFirstOrder() : orders.tapOrder(bookingNo));
        Assert.assertTrue("Order Details should load with its data", orders.isOrderLoaded());
        if (!bookingNo.isEmpty()) {
            Assert.assertTrue("Details should be for ORD" + bookingNo, orders.isOrderNumberVisible(bookingNo));
        }
    }

    @Category(P0.class)
    @Test
    public void TC963_Verify_Order_Link_Opens_The_Order_While_App_Is_Running() {
        String orderId = SevaTestConfig.requireOwnOrderId();
        Assert.assertTrue(new DeepLinkPage(driver).fire("aikyam://orders/" + orderId));
        Assert.assertTrue("The push link should open that order with its data", new SevaOrdersPage(driver).isOrderLoaded());
    }

    @Category(P0.class)
    @Test
    public void TC964_Verify_Order_Link_Cold_Starts_The_App_And_Opens_The_Order() {
        String orderId = SevaTestConfig.requireOwnOrderId();
        DeepLinkPage links = new DeepLinkPage(driver);
        Assert.assertTrue(links.forceStopApp());
        Assert.assertTrue(links.fire("aikyam://orders/" + orderId));
        Assert.assertTrue("Cold start from a push link should land on the loaded order",
                new SevaOrdersPage(driver).isOrderLoaded());
    }

    @Test
    public void TC965_Verify_Completed_Order_Shows_Completed_And_Paid() {
        String orderId = SevaTestConfig.requireOwnOrderId();
        Assume.assumeTrue("Skipping: set aikyam.ownOrderCompleted=true for a completed order",
                Boolean.getBoolean("aikyam.ownOrderCompleted"));
        Assert.assertTrue(new DeepLinkPage(driver).fire("aikyam://orders/" + orderId));
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue(orders.isOrderLoaded());
        Assert.assertTrue("Completed status should show", orders.isCompletedStatusVisible());
        Assert.assertTrue("Payment status should show paid", orders.isPaymentPaidVisible());
    }

    @Test
    public void TC967_Verify_Malformed_Order_Links_Fail_Safely() {
        DeepLinkPage links = new DeepLinkPage(driver);
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        for (String id : new String[] {"abc%27%20OR%201=1", "a/b", "%00", "..%2F..%2Fetc"}) {
            Assert.assertTrue("Link for '" + id + "' should be delivered", links.fire("aikyam://orders/" + id));
            Assert.assertFalse("'" + id + "' must not open an order", orders.isOrderLoaded());
            Assert.assertEquals("App should stay alive after '" + id + "'",
                    ApplicationState.RUNNING_IN_FOREGROUND, driver.queryAppState(DeepLinkPage.DEV_BUNDLE_ID));
        }
    }

    @Test
    public void TC970_Verify_Upcoming_Sevas_Does_Not_List_A_Completed_Order() {
        String bookingNo = SevaTestConfig.requireOwnBookingNumber();
        Assume.assumeTrue("Skipping: set aikyam.ownOrderCompleted=true for a completed order",
                Boolean.getBoolean("aikyam.ownOrderCompleted"));
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue("Upcoming Sevas should open", orders.openUpcomingSevas());
        Assert.assertTrue("Upcoming Sevas should load (list or 'No upcoming sevas')", orders.isUpcomingSectionVisible());
        Assert.assertFalse("Completed ORD" + bookingNo + " must not be under Upcoming", orders.isBookingNumberListed(bookingNo));
    }

    // ---- iOS edge cases (app PR #882) ----

    @Category(P0.class)
    @Test
    public void TC971_Verify_Unknown_Order_Id_Says_Not_Available_Not_A_Placeholder_Order() {
        String unknown = "00000000-0000-4000-8000-000000000000";
        Assert.assertTrue(new DeepLinkPage(driver).fire("aikyam://orders/" + unknown));
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue("Order Details should open", orders.isOrderPageVisible());
        Assert.assertTrue("Unknown id should say 'Order details not available'", orders.isNotAvailableMessageVisible());
        Assert.assertFalse("No placeholder status chip", orders.isAnyOrderStatusVisible());
    }

    @Test
    public void TC972_Verify_Unknown_Order_Id_On_Cold_Start_Says_Not_Available() {
        DeepLinkPage links = new DeepLinkPage(driver);
        Assert.assertTrue(links.forceStopApp());
        Assert.assertTrue(links.fire("aikyam://orders/00000000-0000-4000-8000-000000000000"));
        Assert.assertTrue(new SevaOrdersPage(driver).isNotAvailableMessageVisible());
    }

    @Test
    public void TC973_Verify_Bare_Orders_Link_Still_Opens_Order_History() {
        Assert.assertTrue(new DeepLinkPage(driver).fire("aikyam://orders"));
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue("aikyam://orders should open Order History", orders.isOrderHistoryVisible());
        Assert.assertTrue("The list should load (orders or the empty state)", orders.isEmptyOrListLoaded());
    }

    @Test
    public void TC974_Verify_Back_From_An_Order_Opened_By_Link_Leaves_The_App_Usable() {
        String orderId = SevaTestConfig.requireOwnOrderId();
        Assert.assertTrue(new DeepLinkPage(driver).fire("aikyam://orders/" + orderId));
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue(orders.isOrderLoaded());
        com.shriaikyam.aikyam.pages.BasePage.navigateBack(driver);
        Assert.assertFalse("Back should leave Order Details", orders.isOrderPageVisible());
        Assert.assertEquals(ApplicationState.RUNNING_IN_FOREGROUND, driver.queryAppState(DeepLinkPage.DEV_BUNDLE_ID));
    }

    @Test
    public void TC975_Verify_Same_Order_Link_Twice_Does_Not_Break_Navigation() {
        String orderId = SevaTestConfig.requireOwnOrderId();
        DeepLinkPage links = new DeepLinkPage(driver);
        Assert.assertTrue(links.fire("aikyam://orders/" + orderId));
        Assert.assertTrue(links.fire("aikyam://orders/" + orderId));
        Assert.assertTrue("A repeated push tap should still show the order", new SevaOrdersPage(driver).isOrderLoaded());
    }
}
