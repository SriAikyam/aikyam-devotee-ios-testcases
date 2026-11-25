package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Seva booking on iOS, from a temple page to Payment Review:
 * temple page "Sevas" tab (TempleSevasTab) -> seva card Book / Details -> Seva Details
 * ("Book this seva") -> "Book a Seva" form (SevaBookingView) -> "Review & Pay"
 * (PaymentFlowView / PaymentReviewContentView).
 *
 * Locators are the English labels in the SwiftUI views; these screens set almost no
 * accessibilityIdentifiers. Run the suite with the app in English.
 *
 * "Proceed to Payment" is only ever tapped through tapProceedToPaymentInPayUTestMode(),
 * which refuses unless the dev build is in front and the run opted into PayU test mode.
 */
public class BookSevaPage extends BasePage {

    private final By sevasTab = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND label == 'Sevas'");
    private final By sevaCardBook = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND label == 'Book'");
    private final By sevaCardDetails = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND label == 'Details'");
    private final By bookThisSeva = textContains("Book this seva");

    // "Book a Seva" form.
    private final By formTitle = text("Book a Seva");
    private final By dateOfSevaLabel = text("Date of Seva");
    private final By dateField = textContains("dd/mm/yyyy");
    private final By datePickerOk = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND label == 'OK'");
    private final By devoteeDetailsLabel = text("Devotee Details");
    // "Total number of devotees: N" under the list. Not the bottom bar's "N devotees • Seva ₹X",
    // which never shows fewer than 1.
    private final By totalDevotees = textContains("Total number of devotees");
    private final By devoteeCheckbox = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSwitch'");
    private final By dateError = text("Please select date of seva");
    private final By devoteeError = text("Please select at least one devotee");
    private final By placeSevaOrder = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND label == 'Place Seva Order'");
    private final By duplicateBookAgain = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND label == 'Book again'");

    // "Review & Pay".
    private final By reviewTitle = text("Review & Pay");
    private final By amountBreakdown = text("Amount Breakdown");
    private final By totalAmount = text("Total Amount");
    private final By proceedToPayment = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND label == 'Proceed to Payment'");

    public BookSevaPage(IOSDriver driver) {
        super(driver);
    }

    // ---- Temple page ----

    /** A temple short link (aikyam://p/<code>) resolves over the network first, so wait for the
     * temple page itself before scrolling: swiping early scrolls whatever screen is still showing. */
    public boolean openSevasTab() {
        long end = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < end && !isVisibleNow(sevasTab)) {
            // Polling; isVisibleNow waits up to 2 s per call.
        }
        return (isVisibleNow(sevasTab) || scrollUntilVisible(sevasTab)) && tapFirstVisible(sevasTab);
    }

    /** A bookable seva card: its Book button (scrolls the Sevas tab to find it). */
    public boolean isSevaCardVisible() {
        return scrollUntilVisible(6, sevaCardBook);
    }

    public boolean tapFirstSevaBook() {
        return isSevaCardVisible() && tapFirstVisible(sevaCardBook);
    }

    public boolean tapFirstSevaDetails() {
        return scrollUntilVisible(6, sevaCardDetails) && tapFirstVisible(sevaCardDetails);
    }

    public boolean isCatalogDetailsScreenVisible() {
        return scrollUntilVisible(4, bookThisSeva);
    }

    public boolean tapBookThisSeva() {
        return isCatalogDetailsScreenVisible() && tapFirstVisible(bookThisSeva);
    }

    // ---- Booking form ----

    public boolean isBookingDetailsScreenVisible() {
        return isAnyVisible(formTitle, dateOfSevaLabel, placeSevaOrder);
    }

    /** Opens the date sheet and confirms the date it starts on (the first bookable day). */
    public boolean confirmDefaultDate() {
        if (!(scrollBackUntilVisible(6, dateField, dateOfSevaLabel) && tapFirstVisible(dateField, dateOfSevaLabel))) {
            return false;
        }
        WebElement ok;
        try {
            ok = waitForAnyVisible(datePickerOk);
        } catch (RuntimeException noSheet) {
            return false;
        }
        if (!ok.isEnabled()) {
            // The first day is not offered for this seva; OK stays disabled. Close the sheet.
            tapFirstVisible(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND label == 'Cancel'"));
            return false;
        }
        ok.click();
        return waitUntilGone(datePickerOk);
    }

    /** Opens the date sheet and closes it with Cancel, leaving the form without a date. */
    public boolean cancelDateSheet() {
        if (!(scrollBackUntilVisible(6, dateField, dateOfSevaLabel) && tapFirstVisible(dateField, dateOfSevaLabel))) {
            return false;
        }
        By cancel = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND label == 'Cancel'");
        return tapFirstVisible(cancel) && waitUntilGone(datePickerOk);
    }

    public boolean isDevoteeDetailsVisible() {
        return scrollUntilVisible(6, devoteeDetailsLabel);
    }

    /** Ticks a devotee's checkbox: the Switch nearest the name's row (BookingDevoteeRowView lays
     * out checkbox, then name and relationship). Tapping the name text does not toggle. */
    public boolean toggleDevotee(String name) {
        By nameLabel = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeStaticText' AND label == "
                + predicateLiteral(name));
        // Reading the count scrolls past the list, so the name may be above the viewport.
        if (!(isVisibleNow(nameLabel) || scrollBackUntilVisible(6, nameLabel) || scrollUntilVisible(8, nameLabel))) {
            return false;
        }
        List<WebElement> names = visibleElements(nameLabel);
        if (names.isEmpty()) {
            return false;
        }
        Rectangle n = names.get(0).getRect();
        int nameY = n.getY() + n.getHeight() / 2;
        WebElement best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (WebElement box : visibleElements(devoteeCheckbox)) {
            Rectangle b = box.getRect();
            int distance = Math.abs(b.getY() + b.getHeight() / 2 - nameY);
            if (b.getX() < n.getX() && distance < bestDistance) {
                bestDistance = distance;
                best = box;
            }
        }
        if (best == null || bestDistance > 40) {
            return false;
        }
        tapElementCenter(best);
        return true;
    }

    /** "Total number of devotees: N" -> N, or -1. */
    public int readTotalDevoteesCount() {
        if (!scrollUntilVisible(8, totalDevotees)) {
            return -1;
        }
        for (WebElement e : visibleElements(totalDevotees)) {
            String digits = String.valueOf(e.getAttribute("label")).replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                return Integer.parseInt(digits);
            }
        }
        return -1;
    }

    public boolean isDateValidationErrorVisible() {
        return scrollBackUntilVisible(6, dateError) || scrollUntilVisible(6, dateError);
    }

    public boolean isDevoteeValidationErrorVisible() {
        return scrollBackUntilVisible(6, devoteeError) || scrollUntilVisible(6, devoteeError);
    }

    /** Kanike / Hundi chip by amount, e.g. "Poojari Kanike ₹20" or "Hundi ₹21". */
    public boolean tapAmountChip(int amount) {
        By chip = AppiumBy.iOSNsPredicateString("label == " + predicateLiteral("₹" + amount));
        return scrollUntilVisible(8, chip) && tapFirstVisible(chip);
    }

    public boolean isFinalTotalVisible() {
        return scrollUntilVisible(6, totalAmount);
    }

    public boolean tapPlaceSevaOrder() {
        if (!tapFirstVisible(placeSevaOrder)) {
            return false;
        }
        // The account already holds this seva on this date: the app asks before booking again.
        if (isVisibleNow(duplicateBookAgain)) {
            tapFirstVisible(duplicateBookAgain);
        }
        return true;
    }

    /** Two taps 50 ms apart in one W3C action: both land before the first submit disables the form. */
    public boolean doubleTapPlaceSevaOrder() {
        List<WebElement> buttons = visibleElements(placeSevaOrder);
        if (buttons.isEmpty()) {
            return false;
        }
        Rectangle r = buttons.get(0).getRect();
        int x = r.getX() + r.getWidth() / 2;
        int y = r.getY() + r.getHeight() / 2;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence taps = new Sequence(finger, 1);
        for (int i = 0; i < 2; i++) {
            taps.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
            taps.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            taps.addAction(new Pause(finger, Duration.ofMillis(50)));
            taps.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            if (i == 0) {
                taps.addAction(new Pause(finger, Duration.ofMillis(50)));
            }
        }
        driver.perform(Collections.singletonList(taps));
        if (isVisibleNow(duplicateBookAgain)) {
            tapFirstVisible(duplicateBookAgain);
        }
        return true;
    }

    /** The order went out and the app moved on, or showed an alert explaining why not. */
    public String readAlertMessage() {
        List<WebElement> alerts = driver.findElements(AppiumBy.className("XCUIElementTypeAlert"));
        if (alerts.isEmpty()) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (WebElement t : alerts.get(0).findElements(AppiumBy.className("XCUIElementTypeStaticText"))) {
            text.append(t.getAttribute("label")).append(' ');
        }
        return text.toString().trim();
    }

    public boolean dismissAlert() {
        List<WebElement> alerts = driver.findElements(AppiumBy.className("XCUIElementTypeAlert"));
        if (alerts.isEmpty()) {
            return false;
        }
        List<WebElement> buttons = alerts.get(0).findElements(AppiumBy.className("XCUIElementTypeButton"));
        if (buttons.isEmpty()) {
            return false;
        }
        buttons.get(0).click();
        return true;
    }

    public boolean isAlertVisible() {
        return isAnyVisible(AppiumBy.className("XCUIElementTypeAlert"));
    }

    // ---- Review & Pay ----

    public boolean waitForPaymentReviewScreen() {
        return isAnyVisible(reviewTitle, amountBreakdown, proceedToPayment)
                || waitFor(Duration.ofSeconds(20), reviewTitle, amountBreakdown, proceedToPayment);
    }

    private boolean waitFor(Duration timeout, By... locators) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (isVisibleNow(locators)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAmountBreakdownVisible() {
        return scrollUntilVisible(6, amountBreakdown);
    }

    public boolean isProceedToPaymentEnabled() {
        if (!scrollUntilVisible(8, proceedToPayment)) {
            return false;
        }
        List<WebElement> buttons = visibleElements(proceedToPayment);
        return !buttons.isEmpty() && buttons.get(0).isEnabled();
    }

    /** The amount on the "Total Amount" row: the ₹ value whose vertical centre is inside the row. */
    public BigDecimal readFinalTotal() {
        if (!scrollUntilVisible(6, totalAmount)) {
            return null;
        }
        List<WebElement> labels = visibleElements(totalAmount);
        Rectangle row = labels.get(labels.size() - 1).getRect();
        for (WebElement e : visibleElements(textContains("₹"))) {
            String v = e.getAttribute("label");
            Rectangle r = e.getRect();
            int centre = r.getY() + r.getHeight() / 2;
            if (v != null && v.trim().matches("₹\\s?[0-9,]+(\\.[0-9]{1,2})?")
                    && Math.abs(centre - (row.getY() + row.getHeight() / 2)) <= row.getHeight()) {
                return new BigDecimal(v.replaceAll("[^0-9.]", ""));
            }
        }
        return null;
    }

    /** Leaves Review & Pay with the screen's back control. True once it is gone. */
    public boolean cancelPaymentReview() {
        if (!isVisibleNow(reviewTitle, amountBreakdown, proceedToPayment)) {
            return false;
        }
        navigateBack();
        return waitUntilGone(Duration.ofSeconds(10), reviewTitle, proceedToPayment);
    }

    /**
     * The only way a test may tap "Proceed to Payment". Refuses unless the run set
     * -Daikyam.payu.testMode=true AND the dev build is in front: the dev build talks to PayU's
     * test environment (PaymentViewModel: .development -> .test); the prod app charges real cards.
     */
    public boolean tapProceedToPaymentInPayUTestMode() {
        if (!isPayUTestModeEnabled()) {
            throw new IllegalStateException("Refusing to open PayU: run with -Daikyam.payu.testMode=true on the dev build");
        }
        String front = foregroundBundleId();
        if (!DeepLinkPage.DEV_BUNDLE_ID.equals(front)) {
            throw new IllegalStateException("Refusing to open PayU: foreground app is " + front
                    + ", not the dev build " + DeepLinkPage.DEV_BUNDLE_ID);
        }
        return scrollUntilVisible(8, proceedToPayment) && tapFirstVisible(proceedToPayment);
    }

    @SuppressWarnings("unchecked")
    private String foregroundBundleId() {
        try {
            Object info = driver.executeScript("mobile: activeAppInfo");
            return String.valueOf(((Map<String, Object>) info).get("bundleId"));
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

    public static boolean isPayUTestModeEnabled() {
        return Boolean.getBoolean("aikyam.payu.testMode");
    }

    public boolean isNoInternetMessageVisible() {
        return isAnyVisible(textContains("No internet connection"), textContains("Network error"),
                textContains("Check your network connection"), textContains("check your connection"));
    }

    /** Test cleanup: leaves PayU / Review & Pay if a test stopped inside them. Never throws. */
    public void leavePaymentFlowIfOpen() {
        try {
            SevaPaymentPage pay = new SevaPaymentPage(driver);
            if (pay.isPayUOnScreen()) {
                pay.cancelPayU();
            }
            for (int i = 0; i < 3 && isVisibleNow(reviewTitle, proceedToPayment, textContains("Net Banking")); i++) {
                navigateBack();
                if (isVisibleNow(text("Yes"), textContains("YES"))) {
                    tapFirstVisible(text("Yes"), textContains("YES"));
                }
            }
            if (isVisibleNow(AppiumBy.className("XCUIElementTypeAlert"))) {
                dismissAlert();
            }
        } catch (RuntimeException ignored) {
            // Best effort; BaseTest resets to Home before the next test.
        }
    }
}
