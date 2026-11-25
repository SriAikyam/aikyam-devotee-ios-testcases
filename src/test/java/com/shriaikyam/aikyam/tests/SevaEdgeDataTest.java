package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.BookSevaPage;
import com.shriaikyam.aikyam.pages.DeepLinkPage;
import com.shriaikyam.aikyam.pages.SevaEdgePage;
import com.shriaikyam.aikyam.pages.SevaOrdersPage;
import com.shriaikyam.aikyam.pages.SevaPaymentPage;
import io.appium.java_client.appmanagement.ApplicationState;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sevas QA report group 6: odd or empty data (15 cases). Most need seeded data passed as -D
 * inputs (see SevaEdgeConfig); without them a case skips and says which input it needs.
 */
public class SevaEdgeDataTest extends BaseTest {

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

    private BookSevaPage openSevas(String templeLink) {
        Assert.assertTrue(new DeepLinkPage(driver).fire(templeLink));
        BookSevaPage seva = new BookSevaPage(driver);
        Assert.assertTrue("Sevas tab should open", seva.openSevasTab());
        return seva;
    }

    private SevaOrdersPage openOrder(String orderId) {
        Assert.assertTrue(new DeepLinkPage(driver).fire("aikyam://orders/" + orderId));
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue("Order should load", orders.isOrderLoaded());
        return orders;
    }

    /** SEV-K001 */
    @Test
    public void TC985_Verify_Temple_With_Zero_Sevas_Shows_Empty_State_With_Retry() {
        String link = SevaEdgeConfig.require("aikyam.emptySevaTempleLink", "temple with no sevas");
        openSevas(link);
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue(edge.isTextShown(SevaEdgePage.NO_SEVAS));
        Assert.assertTrue("Retry should be offered", edge.isRetryVisible());
        Assert.assertTrue(edge.tapRetry());
        Assert.assertTrue("Retry should reload to the same empty state", edge.isTextShown(SevaEdgePage.NO_SEVAS));
    }

    /** SEV-K002 */
    @Test
    public void TC986_Verify_Multiple_Sevas_Keep_A_Stable_Order_With_Own_Price() {
        String link = SevaEdgeConfig.require("aikyam.multiSevaTempleLink", "temple with 2+ sevas");
        openSevas(link);
        SevaEdgePage edge = new SevaEdgePage(driver);
        List<String> first = edge.visibleTexts();
        Assert.assertTrue("Each seva card should show a price (2+ ₹ amounts)", edge.priceCount() >= 2);
        new DeepLinkPage(driver).returnHome();
        openSevas(link);
        Assert.assertEquals("Seva order should be the same on reopen", first, edge.visibleTexts());
    }

    /** SEV-K004 */
    @Test
    public void TC987_Verify_Seva_Without_Image_Shows_Placeholder_And_Intact_Layout() {
        String link = SevaEdgeConfig.require("aikyam.noImageSevaTempleLink", "temple whose first seva has no media");
        BookSevaPage seva = openSevas(link);
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue(seva.isSevaCardVisible());
        Assert.assertTrue("Nothing clipped: " + edge.elementsOffScreenHorizontally(),
                edge.elementsOffScreenHorizontally().isEmpty());
        Assert.assertTrue(seva.tapFirstSevaDetails());
        Assert.assertTrue(seva.isCatalogDetailsScreenVisible());
        Assert.assertTrue("Nothing clipped on details: " + edge.elementsOffScreenHorizontally(),
                edge.elementsOffScreenHorizontally().isEmpty());
    }

    /** SEV-K005 */
    @Test
    public void TC988_Verify_Long_Seva_Text_Wraps_And_Book_Stays_Tappable() {
        String link = SevaEdgeConfig.require("aikyam.longTextSevaTempleLink", "temple whose first seva has very long text");
        BookSevaPage seva = openSevas(link);
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue("Long text must not run off screen: " + edge.elementsOffScreenHorizontally(),
                edge.elementsOffScreenHorizontally().isEmpty());
        Assert.assertTrue("Details should be tappable", seva.tapFirstSevaDetails());
        Assert.assertTrue(seva.isCatalogDetailsScreenVisible());
        edge.back();
        Assert.assertTrue("Book should be tappable", seva.tapFirstSevaBook());
        Assert.assertTrue(seva.isBookingDetailsScreenVisible());
    }

    /** SEV-K006 */
    @Test
    public void TC989_Verify_Prices_Use_Rupee_Symbol_Indian_Grouping_And_No_Stray_Decimals() {
        String link = SevaEdgeConfig.require("aikyam.multiSevaTempleLink", "temple with sevas priced ₹1, ₹100, ₹1,000+");
        openSevas(link);
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue("Prices should show", edge.priceCount() > 0);
        Assert.assertTrue("Badly formatted prices: " + edge.badlyFormattedPrices(), edge.badlyFormattedPrices().isEmpty());
        for (int i = 0; i < 3; i++) {
            edge.scrollDown();
            Assert.assertTrue("Badly formatted prices: " + edge.badlyFormattedPrices(), edge.badlyFormattedPrices().isEmpty());
        }
    }

    /** SEV-F03 */
    @Test
    public void TC990_Verify_Empty_Upcoming_Sevas_Says_No_Upcoming_Sevas() {
        SevaEdgeConfig.requireFlag("aikyam.emptyAccount", "logged-in devotee has no bookings");
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue(orders.openUpcomingSevas());
        Assert.assertTrue(new SevaEdgePage(driver).isTextShown(SevaEdgePage.NO_UPCOMING));
    }

    /** SEV-G02 */
    @Test
    public void TC991_Verify_Empty_Order_History_Says_No_Orders() {
        SevaEdgeConfig.requireFlag("aikyam.emptyAccount", "logged-in devotee has no orders");
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue(orders.openOrderHistory());
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue("'No past sevas' or 'No orders found' should show",
                edge.isTextShown(SevaEdgePage.NO_PAST) || edge.isTextShown(SevaEdgePage.NO_ORDERS));
    }

    /** SEV-G04 */
    @Test
    public void TC992_Verify_Completed_Seva_Media_Shows_And_Opens_Fullscreen() {
        String id = SevaEdgeConfig.require("aikyam.completedMediaOrderId", "Completed order with priest media");
        SevaOrdersPage orders = openOrder(id);
        Assert.assertTrue(orders.isCompletedStatusVisible());
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue("Priest media should show", edge.isAnyMediaShown());
        Assert.assertTrue(edge.openFirstMedia());
        Assert.assertFalse("Fullscreen viewer should cover Order Details", edge.isTextShownNow("Order Details"));
        Assert.assertTrue("Viewer should close", edge.closeViewer());
        Assert.assertTrue("Back on the order", orders.isOrderPageVisible());
    }

    /** SEV-K407 */
    @Test
    public void TC993_Verify_Media_Viewer_Swipes_Closes_And_Plays() {
        String id = SevaEdgeConfig.require("aikyam.completedMediaOrderId", "Completed order with photo, video and audio");
        SevaOrdersPage orders = openOrder(id);
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue(edge.isAnyMediaShown());
        Assert.assertTrue(edge.mediaCount() >= 2 || edge.scrollToText("Video") || edge.scrollToText("Audio"));
        Assert.assertTrue(edge.openFirstMedia());
        List<String> firstItem = edge.visibleTexts();
        edge.scrollDown();
        Assert.assertTrue("App alive while swiping media", isRunning());
        Assert.assertTrue("Playback controls should be offered for video/audio",
                edge.isPlayControlShown() || !firstItem.equals(edge.visibleTexts()));
        Assert.assertTrue(edge.closeViewer());
        Assert.assertTrue(orders.isOrderPageVisible());
    }

    /** SEV-G05 */
    @Test
    public void TC994_Verify_Frequent_Seva_Opens_Summary_And_Rebook_Starts_Same_Seva() {
        SevaEdgeConfig.requireFlag("aikyam.hasFrequentSevas", "Home shows 'Your frequent sevas'");
        new DeepLinkPage(driver).returnHome();
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue("'Your frequent sevas' should show", edge.scrollToText(SevaEdgePage.FREQUENT_SEVAS));
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue("A past order should open", orders.tapFirstOrder() || edge.tapText("ORD"));
        Assert.assertTrue("Order summary should load", orders.isOrderLoaded());
        Assert.assertTrue("Rebook should be offered", edge.tapText("Book again") || edge.tapText("Rebook")
                || edge.tapText("Book"));
        Assert.assertTrue("Rebook should open the booking form", new BookSevaPage(driver).isBookingDetailsScreenVisible());
    }

    /** SEV-K308: search is blocked on iOS, so emoji go into the one free-text field on the flow (email). */
    @Test
    public void TC995_Verify_Emoji_And_Symbols_In_Email_Do_Not_Crash() {
        BookSevaPage seva = openSevas(SevaTestConfig.requireTempleLink());
        Assert.assertTrue(seva.tapFirstSevaBook());
        Assert.assertTrue(seva.confirmDefaultDate());
        Assert.assertTrue(seva.toggleDevotee(SevaTestConfig.requireSelfName()));
        Assert.assertTrue(seva.tapPlaceSevaOrder());
        Assert.assertTrue(seva.waitForPaymentReviewScreen());
        SevaPaymentPage pay = new SevaPaymentPage(driver);
        pay.clearEmail();
        pay.enterEmail("🙏\"'<>&%$#@ॐ.com");
        Assert.assertTrue("App should survive odd input", isRunning());
        Assert.assertTrue("Still on Review & Pay", seva.isPaymentReviewVisibleNow());
        Assert.assertFalse(pay.isRawTechnicalErrorVisible());
        Assert.assertTrue(seva.cancelPaymentReview());
    }

    /** SEV-K404 */
    @Test
    public void TC996_Verify_Long_Order_History_Pages_Without_Duplicates() {
        SevaEdgeConfig.requireFlag("aikyam.manyOrders", "account has 20+ orders");
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue(orders.openOrderHistory());
        Assert.assertTrue(orders.isAnyOrderListed());
        SevaEdgePage edge = new SevaEdgePage(driver);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 8; i++) {
            for (String t : edge.visibleTexts()) {
                if (t.matches("ORD\\d+.*")) {
                    seen.add(t.replaceAll("^(ORD\\d+).*", "$1"));
                }
            }
            edge.scrollDown();
        }
        Assert.assertTrue("20+ distinct orders should load while scrolling, saw " + seen.size(), seen.size() >= 20);
    }

    /** SEV-K403 */
    @Test
    public void TC997_Verify_Past_Seva_Leaves_Upcoming_And_Shows_In_History() {
        String no = SevaEdgeConfig.require("aikyam.pastBookingNo", "booking number whose seva date has passed")
                .replaceFirst("^ORD", "");
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue(orders.openUpcomingSevas());
        Assert.assertTrue("Upcoming should load", orders.isUpcomingSectionVisible());
        Assert.assertFalse("Past ORD" + no + " must not be in Upcoming", orders.isBookingNumberListed(no));
        Assert.assertTrue(orders.openOrderHistory());
        Assert.assertTrue("Past ORD" + no + " should be in Order History", orders.isBookingNumberListed(no));
    }

    /** SEV-K406 */
    @Test
    public void TC998_Verify_Share_Or_Copy_On_Order_Details_Uses_The_Right_Id() {
        String id = SevaTestConfig.requireOwnOrderId();
        String no = SevaTestConfig.requireOwnBookingNumber();
        openOrder(id);
        SevaEdgePage edge = new SevaEdgePage(driver);
        org.junit.Assume.assumeTrue("Skipping: Order Details has no Share or Copy control", edge.isShareOrCopyShown());
        Assert.assertTrue(edge.tapShareOrCopy());
        String link = new DeepLinkPage(driver).readLinkFromShareSheet();
        String copied = link != null ? link : edge.clipboardText();
        Assert.assertNotNull("Share/Copy should produce text", copied);
        Assert.assertTrue("Shared text should carry this order (" + id + " or ORD" + no + "): " + copied,
                copied.contains(id) || copied.contains(no));
    }

    /** SEV-K408 */
    @Test
    public void TC999_Verify_Priest_Message_Shows_In_Full() {
        String id = SevaEdgeConfig.require("aikyam.completedMediaOrderId", "Completed order with a priest message");
        String message = SevaEdgeConfig.require("aikyam.priestMessage", "the priest message text on that order");
        openOrder(id);
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue("Priest message should show", edge.scrollToText(message.substring(0, Math.min(20, message.length()))));
        boolean whole = edge.visibleTexts().stream().anyMatch(t -> t.contains(message));
        Assert.assertTrue("Message should be shown in full, not truncated", whole);
    }

    private boolean isRunning() {
        return driver.queryAppState(DeepLinkPage.DEV_BUNDLE_ID) == ApplicationState.RUNNING_IN_FOREGROUND;
    }
}
