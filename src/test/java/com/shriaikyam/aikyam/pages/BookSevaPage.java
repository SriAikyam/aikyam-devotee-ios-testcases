package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Covers the Seva booking wizard: Seva List (SevasView) -> Catalog Details
 * (SevaCatalogDetailsView) -> Booking Details (SevaBookingView) -> Payment Review
 * (BookSevaView's HundiReviewPayView). Deliberately stops there: this page object has NO
 * method that taps "Proceed to Payment" - that button is only ever checked for
 * visibility/enabled state, never invoked, to make it structurally impossible for a test
 * to trigger a real charge.
 *
 * iOS state today (see BookSevaTest's @Ignore): there is no Sevas tab, SevasView's
 * Book/Hundi/Reorder quick actions and the catalog's "Book this seva" bar are hidden behind
 * file-level flags, and "Place Seva Order (Coming soon)" only shows an alert. The locators
 * below still target the real views so the flow works once those gates lift.
 */
public class BookSevaPage extends BasePage {

    // iOS has no Sevas tab (Book Seva is on hold); same id HomePage keeps for it.
    private final By sevasTabAction = id("tab.sevas");
    private final By sevaCardBookAction = id("sevas.card.book");
    private final By sevaCardDetailsAction = id("sevas.card.details");
    private final By bookThisSevaAction = id("sevas.catalog.book");

    // SevaBookingView section titles (BookSevaSectionTitle: icon + plain Text).
    private final By templeSectionLabel = text("Temple");
    private final By selectedSevaLabel = text("Selected Seva");
    private final By dateOfSevaLabel = text("Date of Seva");
    private final By selectDateAction = id("sevas.booking.datefield");
    private final By datePickerOk = id("sevas.datepicker.ok");
    private final By devoteeDetailsLabel = text("Devotee Details");
    private final By selectFamilyMembersLabel = text("Select Family Members");
    private final By addFamilyMemberRow = id("sevas.booking.addmember");
    private final By totalDevoteesLabel = id("sevas.booking.totaldevotees");
    private final By devoteeCheckbox = id("sevas.booking.devotee.checkbox");
    private final By devoteeName = id("sevas.booking.devotee.name");
    private final By devoteeValidationError = id("sevas.booking.devoteeerror");
    private final By dateValidationError = id("sevas.booking.dateerror");
    private final By placeSevaOrderAction = id("sevas.booking.placeorder");
    private final By finalTotalLabel = id("sevas.booking.finaltotal");

    // "Proceed to Payment" is checked for presence/enabled state only - see class
    // comment. Never tap this from a test.
    private final By proceedToPaymentAction = id("hundi.review.proceed");
    private final By amountBreakdownLabel = id("hundi.review.breakdown");
    private final By paymentReviewBack = id("hundi.review.back");

    // Hundi Donation screen (BookSevaView) - reached from a temple row's "Hundi" button on
    // the Sevas list. Fields in order: 0=amount (HundiDonationAmountField), then the
    // HundiLabeledField TextFields 1=name of person, 2=occasion, 3=PAN number, 4=name on
    // PAN card.
    private final By hundiEntryAction = id("sevas.hundi");
    private final By hundiScreenTitle = id("hundi.title");
    private final By hundiAmountField = id("hundi.amount");
    private final By hundiBack = id("hundi.back");
    private final By hundiDonateAction = id("hundi.donate");
    private final By hundiInvalidAmountError = textContains("Please enter a valid amount");
    private final By hundiAmountRequiredError = textContains("Please enter donation amount");
    private final By hundiTextField = AppiumBy.className("XCUIElementTypeTextField");

    // Frequent/reorder flow - only rendered when the account has past frequent sevas;
    // callers must treat absence as a skip, not a failure.
    private final By frequentSevasSectionHeader = id("sevas.frequent.header");
    private final By frequentDetailsAction = id("sevas.frequent.details");
    private final By frequentReorderAction = id("sevas.frequent.reorder");
    private final By frequentDetailsReorderAction = id("sevas.frequentdetails.reorder");
    private final By bookingDetailsTitle = text("Booking Details");

    public BookSevaPage(IOSDriver driver) {
        super(driver);
    }

    public boolean openSevasTab() {
        return tapFirstVisible(sevasTabAction);
    }

    public boolean isSevaCardVisible() {
        return isAnyVisible(sevaCardBookAction, sevaCardDetailsAction);
    }

    public boolean tapCategoryChip(String category) {
        return tapFirstVisible(text(category));
    }

    public boolean tapFirstSevaDetails() {
        return tapFirstVisible(sevaCardDetailsAction);
    }

    public boolean tapFirstSevaBook() {
        return tapFirstVisible(sevaCardBookAction);
    }

    public boolean tapBookThisSeva() {
        return tapFirstVisible(bookThisSevaAction);
    }

    public boolean isCatalogDetailsScreenVisible() {
        return isAnyVisible(bookThisSevaAction);
    }

    public boolean isBookingDetailsScreenVisible() {
        return isAnyVisible(templeSectionLabel, selectedSevaLabel, dateOfSevaLabel, placeSevaOrderAction);
    }

    public boolean openDatePicker() {
        return tapFirstVisible(selectDateAction);
    }

    /** Confirms whichever date SevaDatePickerSheet defaults to (the minimum bookable day)
     * via its "OK" toolbar action - avoids manipulating the calendar grid via Appium. */
    public boolean confirmDefaultDate() {
        if (!openDatePicker()) {
            return false;
        }
        return tapFirstVisible(datePickerOk, text("OK"));
    }

    // iOS: SevaBookingView has no time slot field - the slot comes from the seva itself
    // (see parity report).
    public boolean isTimeSlotFieldVisible() {
        return false;
    }

    public boolean isDevoteeDetailsVisible() {
        return isAnyVisible(devoteeDetailsLabel, selectFamilyMembersLabel);
    }

    public boolean isMemberCheckboxVisible(String name) {
        return isAnyVisible(text(name));
    }

    /** Toggles a family member's checkbox: BookingDevoteeRowView lays out checkbox then
     * name in one row, and every row shares the same ids, so pick the checkbox whose
     * vertical center is closest to the name's. */
    public boolean toggleDevotee(String name) {
        WebElement nameElement = null;
        for (WebElement candidate : visibleElements(devoteeName)) {
            if (name.equals(candidate.getAttribute("label"))) {
                nameElement = candidate;
                break;
            }
        }
        if (nameElement == null) {
            return false;
        }
        int nameY = centerY(nameElement);
        WebElement best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (WebElement checkbox : visibleElements(devoteeCheckbox)) {
            int distance = Math.abs(centerY(checkbox) - nameY);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = checkbox;
            }
        }
        if (best == null) {
            return false;
        }
        try {
            best.click();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static int centerY(WebElement element) {
        return element.getLocation().getY() + element.getSize().getHeight() / 2;
    }

    public boolean tapAddFamilyMemberRow() {
        return tapFirstVisible(addFamilyMemberRow);
    }

    /** Reads "Total number of devotees: N" and returns N, or -1 if unreadable. */
    public int readTotalDevoteesCount() {
        for (WebElement element : visibleElements(totalDevoteesLabel)) {
            String value = element.getAttribute("label");
            if (value == null) {
                value = element.getText();
            }
            if (value == null) {
                continue;
            }
            String digits = value.replaceAll("[^0-9]", "");
            if (!digits.isBlank()) {
                return Integer.parseInt(digits);
            }
        }
        return -1;
    }

    /** Only set by SevaBookingView.placeSevaOrder(), which the "Coming soon" gate on the
     * Place Seva Order button currently makes unreachable on iOS. */
    public boolean isDevoteeValidationErrorVisible() {
        return isAnyVisible(devoteeValidationError);
    }

    /** See isDevoteeValidationErrorVisible(). The date field also shows this slot for the
     * "connect to confirm today" clock-sync error. */
    public boolean isDateValidationErrorVisible() {
        return isAnyVisible(dateValidationError);
    }

    /** Taps a Kanike/Hundi amount chip (BookSevaAmountChipsRow) - callers must pick amounts
     * unique to the section being tested, since a value present in both rows would tap
     * whichever chip comes first. */
    public boolean tapAmountChip(int amount) {
        return tapFirstVisible(text("₹" + amount));
    }

    public boolean isFinalTotalVisible() {
        return isAnyVisible(finalTotalLabel);
    }

    public boolean tapPlaceSevaOrder() {
        return tapFirstVisible(placeSevaOrderAction);
    }

    public boolean waitForPaymentReviewScreen() {
        try {
            waitForAnyVisible(proceedToPaymentAction, amountBreakdownLabel);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isAmountBreakdownVisible() {
        return isAnyVisible(amountBreakdownLabel);
    }

    /** Visibility/enabled check ONLY - see class comment. Never tapped anywhere. */
    public boolean isProceedToPaymentEnabled() {
        try {
            WebElement button = waitForAnyVisible(proceedToPaymentAction);
            return button.isEnabled();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Backs out of Payment Review without ever tapping Proceed to Payment. */
    public boolean cancelPaymentReview() {
        return tapFirstVisible(paymentReviewBack) || navigateBack();
    }

    // ---- Hundi Donation ----

    /** Best-effort: the "Hundi" button is not guaranteed present (and is hidden on iOS
     * while SevasView's quick actions are gated off). Returns false if none found after
     * scrolling, letting callers skip rather than fail. */
    public boolean openHundiFromSevaList() {
        return scrollUntilVisible(hundiEntryAction) && tapFirstVisible(hundiEntryAction);
    }

    public boolean isHundiScreenVisible() {
        return isAnyVisible(hundiScreenTitle, hundiAmountField);
    }

    public boolean enterHundiAmount(String amount) {
        return typeIntoFieldAtIndex(hundiAmountField, 0, amount);
    }

    public boolean enterHundiPersonName(String name) {
        return typeIntoFieldAtIndex(hundiTextField, 1, name);
    }

    public boolean isHundiDonateActionVisible() {
        return isAnyVisible(hundiDonateAction);
    }

    /** Only ever called with invalid data (e.g. amount 0) - real client-side
     * validation blocks the screen from advancing, so this never risks reaching
     * Payment. Never call this after entering valid data. */
    public boolean tapHundiDonate() {
        hideKeyboardIfShown();
        return tapFirstVisible(hundiDonateAction);
    }

    public boolean isHundiInvalidAmountErrorVisible() {
        return isAnyVisible(hundiInvalidAmountError);
    }

    public boolean isHundiAmountRequiredErrorVisible() {
        return isAnyVisible(hundiAmountRequiredError);
    }

    public boolean navigateBackFromHundi() {
        hideKeyboardIfShown();
        return tapFirstVisible(hundiBack) || navigateBack();
    }

    // ---- Frequent seva / Reorder ----

    /** Best-effort: this section only renders when the account has past frequent
     * sevas - not guaranteed present on a fresh account. Returns false (skip, not
     * fail) if not found. */
    public boolean isFrequentSevasSectionVisible() {
        return scrollUntilVisible(frequentSevasSectionHeader);
    }

    public boolean openFirstFrequentOrderDetails() {
        return scrollUntilVisible(frequentDetailsAction) && tapFirstVisible(frequentDetailsAction);
    }

    public boolean isBookingDetailsScreenTitleVisible() {
        return isAnyVisible(bookingDetailsTitle);
    }

    public boolean isReorderActionVisible() {
        return isAnyVisible(frequentDetailsReorderAction, frequentReorderAction);
    }

    /** Reorder starts a brand-new booking pre-filled with the previous order's
     * temple/seva/priest, landing on the same Booking Details screen already covered
     * by isBookingDetailsScreenVisible()/tapBookThisSeva() flows above. */
    public boolean tapReorderFromFrequentOrderDetails() {
        return tapFirstVisible(frequentDetailsReorderAction, frequentReorderAction);
    }
}
