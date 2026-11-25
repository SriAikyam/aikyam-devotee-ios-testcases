package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.regex.Pattern;

/**
 * Devotee side of a booked seva: Order History (DevoteeOrdersHistoryView), Order Details
 * (DevoteeOrderDetailsView) and Upcoming Sevas. Read-only: nothing here changes an order.
 */
public class SevaOrdersPage extends BasePage {

    private final By orderHistoryTitle = text("Order History");
    private final By sevaOrdersSegment = text("Seva Orders");
    private final By orderDetailsTitle = text("Order Details");
    private final By notAvailable = text("Order details not available");
    private final By couldNotLoad = textContains("Couldn't load order details");
    private final By anyOrderNumber = AppiumBy.iOSNsPredicateString("label BEGINSWITH 'ORD'");
    private final By paymentPaid = AppiumBy.iOSNsPredicateString(
            "label ==[c] 'CAPTURED' OR label ==[c] 'Paid' OR label ==[c] 'SUCCESS'");
    private final By upcomingTitle = text("Upcoming Sevas");
    private final By noUpcoming = text("No upcoming sevas");
    private final By noOrders = text("No orders found");

    public SevaOrdersPage(IOSDriver driver) {
        super(driver);
    }

    private static By status(String s) {
        return AppiumBy.iOSNsPredicateString("label ==[c] " + predicateLiteral(s));
    }

    /** Opens Order History the way a push for the order list does: aikyam://orders. */
    public boolean openOrderHistory() {
        return new DeepLinkPage(driver).fire("aikyam://orders") && isAnyVisible(orderHistoryTitle, sevaOrdersSegment);
    }

    public boolean isOrderHistoryVisible() {
        return isAnyVisible(orderHistoryTitle, sevaOrdersSegment);
    }

    public boolean isAnyOrderListed() {
        return isAnyVisible(anyOrderNumber) && isAnyOrderStatusVisible();
    }

    public boolean tapOrder(String bookingNo) {
        By card = textContains("ORD" + bookingNo);
        return scrollUntilVisible(10, card) && tapFirstVisible(card);
    }

    public boolean tapFirstOrder() {
        return tapFirstVisible(anyOrderNumber);
    }

    /** True if a card shows exactly this booking number ("1001" must not match "ORD10010"). */
    public boolean isBookingNumberListed(String bookingNo) {
        scrollUntilVisible(10, textContains(bookingNo));
        Pattern exact = Pattern.compile("(^|\\D)" + Pattern.quote(bookingNo) + "(\\D|$)");
        for (WebElement e : visibleElements(textContains(bookingNo))) {
            String v = e.getAttribute("label");
            if (v != null && exact.matcher(v).find()) {
                return true;
            }
        }
        return false;
    }

    public boolean isOrderPageVisible() {
        return isAnyVisible(orderDetailsTitle);
    }

    public boolean isOrderUnavailableVisible() {
        return isAnyVisible(notAvailable, couldNotLoad);
    }

    public boolean isNotAvailableMessageVisible() {
        return isAnyVisible(notAvailable);
    }

    public boolean isLoadErrorVisible() {
        return isAnyVisible(couldNotLoad);
    }

    /** Order Details opened AND its data loaded: an ORD number and a status. */
    public boolean isOrderLoaded() {
        return isOrderPageVisible() && !isVisibleNow(notAvailable, couldNotLoad)
                && isAnyVisible(anyOrderNumber) && isAnyOrderStatusVisible();
    }

    public boolean isOrderNumberVisible(String bookingNo) {
        return isAnyVisible(textContains("ORD" + bookingNo));
    }

    public boolean isAnyOrderStatusVisible() {
        return isAnyVisible(status("Confirmed"), status("Completed"), status("Cancelled"), status("Expired"),
                status("Awaiting payment"), status("Pending"), status("Held"));
    }

    public boolean isCompletedStatusVisible() {
        return isAnyVisible(status("Completed"));
    }

    public boolean isPaymentPaidVisible() {
        return scrollUntilVisible(6, paymentPaid);
    }

    public boolean isTextVisible(String value) {
        return scrollUntilVisible(6, textContains(value));
    }

    /** Settings hub -> Upcoming Sevas. */
    public boolean openUpcomingSevas() {
        ProfilePage profile = new ProfilePage(driver);
        return profile.openSettingsHub() && scrollUntilVisible(8, upcomingTitle)
                && tapFirstVisible(upcomingTitle) && isAnyVisible(upcomingTitle);
    }

    public boolean isUpcomingSectionVisible() {
        return isAnyVisible(anyOrderNumber, noUpcoming);
    }

    public boolean isEmptyOrListLoaded() {
        return isAnyVisible(anyOrderNumber, noOrders);
    }
}
