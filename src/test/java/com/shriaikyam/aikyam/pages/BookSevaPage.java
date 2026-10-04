package com.shriaikyam.aikyam.pages;

import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Covers the Seva booking wizard: Seva List -> Catalog Details -> Booking Details ->
 * Payment Review. Deliberately stops there - there is no sandbox/test payment mode
 * anywhere in this app (PaymentScreen.kt hardcodes .setIsProduction(true) for every
 * build flavor), so this page object has NO method that taps "Proceed to Payment" -
 * that button is only ever checked for visibility/enabled state, never invoked, to
 * make it structurally impossible for a test to trigger a real charge.
 */
public class BookSevaPage extends BasePage {

    // Bottom-nav labels are plain Text with no contentDescription, same as FeedPage.
    private final By sevasTabAction = text("Sevas");
    private final By sevaCardBookAction = text("Book");
    private final By sevaCardDetailsAction = text("Details");
    private final By bookThisSevaAction = text("Book this seva");

    private final By templeSectionLabel = text("Temple");
    private final By selectedSevaLabel = text("Selected Seva");
    private final By dateOfSevaLabel = text("Date of Seva");
    private final By selectDateIconAction = accessibilityId("Select date of seva");
    private final By timeSlotLabel = text("Time slot");
    private final By devoteeDetailsLabel = text("Devotee Details");
    private final By selectFamilyMembersLabel = text("Select Family Members");
    private final By addFamilyMemberRow = accessibilityId("Add family member or other devotee");
    private final By totalDevoteesLabel = textContains("Total number of devotees");
    private final By devoteeValidationError = text("Please select at least one devotee");
    private final By dateValidationError = text("Please select date of seva");
    private final By placeSevaOrderAction = text("Place Seva Order");
    private final By finalTotalLabel = text("Final Total");

    // "Proceed to Payment" is checked for presence/enabled state only - see class
    // comment. Never tap this from a test.
    private final By proceedToPaymentAction = text("Proceed to Payment");
    private final By amountBreakdownLabel = text("Amount Breakdown");

    // Hundi Donation screen (HundiDonationScreen) - reached from a temple row's
    // "Hundi" button on this same Sevas list, conditional on temple.showHundiButton
    // (not guaranteed present for every temple). Fields are stacked OutlinedTextFields
    // with no distinguishing contentDescription - accessed by index via
    // typeIntoFieldAtIndex(): 0=amount, 1=name of person, 2=occasion, 3=PAN number,
    // 4=name on PAN card.
    private final By hundiEntryAction = text("Hundi");
    private final By hundiAmountLabel = text("Donation to Hundi");
    private final By hundiDonateAction = text("Donate");
    private final By hundiInvalidAmountError = textContains("Please enter a valid amount");
    private final By hundiAmountRequiredError = textContains("Please enter donation amount");
    private final By hundiEditTextField = io.appium.java_client.AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\")"
    );

    // Frequent/reorder flow - only rendered when the account has past frequent sevas
    // (temples.frequentSevas.isNotEmpty()); callers must treat absence as a skip, not a
    // failure, same as LivePlayerPage's content-dependent checks.
    private final By frequentSevasSectionHeader = text("Your frequent sevas");
    private final By frequentDetailsAction = text("Details");
    private final By frequentReorderAction = text("Reorder");
    private final By bookingDetailsTitle = text("Booking details");

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
        return tapFirstVisible(selectDateIconAction, dateOfSevaLabel);
    }

    /** Confirms whichever date the Material3 DatePicker defaults to (today) via its
     * "OK" confirm action - avoids manipulating the calendar grid via Appium. */
    public boolean confirmDefaultDate() {
        if (!openDatePicker()) {
            return false;
        }
        try {
            waitForAnyVisible(text("OK"));
        } catch (RuntimeException exception) {
            return false;
        }
        return tapFirstVisible(text("OK"));
    }

    public boolean isTimeSlotFieldVisible() {
        return isAnyVisible(timeSlotLabel);
    }

    public boolean isDevoteeDetailsVisible() {
        return isAnyVisible(devoteeDetailsLabel, selectFamilyMembersLabel);
    }

    public boolean isMemberCheckboxVisible(String name) {
        return isAnyVisible(text(name));
    }

    /** Toggles a family member's checkbox by finding the nearest Checkbox that appears
     * before their name in document order (the row lays out Checkbox then name). */
    public boolean toggleDevotee(String name) {
        By checkboxForMember = By.xpath(
                "(//*[@text='" + name + "']/preceding::android.widget.CheckBox)[last()]"
        );
        return tapFirstVisible(checkboxForMember);
    }

    public boolean tapAddFamilyMemberRow() {
        return tapFirstVisible(addFamilyMemberRow);
    }

    /** Reads "Total number of devotees: N" and returns N, or -1 if unreadable. */
    public int readTotalDevoteesCount() {
        for (WebElement element : visibleElements(totalDevoteesLabel)) {
            String value = element.getText();
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

    public boolean isDevoteeValidationErrorVisible() {
        return isAnyVisible(devoteeValidationError);
    }

    public boolean isDateValidationErrorVisible() {
        return isAnyVisible(dateValidationError);
    }

    /** Taps a Kanike/Hundi amount chip - callers must pick amounts unique to the
     * section being tested (Kanike: 20/30/40/50, Hundi: 25/50/75/100 - the value 50
     * exists in both, so avoid it to prevent tapping the wrong section's chip). */
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

    /** Backs out of Payment Review without ever tapping Proceed to Payment - the
     * screen's BackHandler/toolbar back both route through onPaymentCancelled back to
     * the Sevas list. */
    public boolean cancelPaymentReview() {
        try {
            driver.navigate().back();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    // ---- Hundi Donation ----

    /** Best-effort: the "Hundi" button only renders for temples with
     * showHundiButton=true, not guaranteed present. Returns false if none found after
     * scrolling, letting callers skip rather than fail. */
    public boolean openHundiFromSevaList() {
        return scrollUntilVisible(hundiEntryAction) && tapFirstVisible(hundiEntryAction);
    }

    public boolean isHundiScreenVisible() {
        return isAnyVisible(hundiAmountLabel);
    }

    public boolean enterHundiAmount(String amount) {
        return typeIntoFieldAtIndex(hundiEditTextField, 0, amount);
    }

    public boolean enterHundiPersonName(String name) {
        return typeIntoFieldAtIndex(hundiEditTextField, 1, name);
    }

    public boolean isHundiDonateActionVisible() {
        return isAnyVisible(hundiDonateAction);
    }

    /** Only ever called with invalid data (e.g. amount 0) - real client-side
     * validation blocks the screen from advancing, so this never risks reaching
     * Payment. Never call this after entering valid data. */
    public boolean tapHundiDonate() {
        return tapFirstVisible(hundiDonateAction);
    }

    public boolean isHundiInvalidAmountErrorVisible() {
        return isAnyVisible(hundiInvalidAmountError);
    }

    public boolean isHundiAmountRequiredErrorVisible() {
        return isAnyVisible(hundiAmountRequiredError);
    }

    public boolean navigateBackFromHundi() {
        try {
            driver.navigate().back();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
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
        return isAnyVisible(frequentReorderAction);
    }

    /** Reorder starts a brand-new booking pre-filled with the previous order's
     * temple/seva/priest, landing on the same Booking Details screen already covered
     * by isBookingDetailsScreenVisible()/tapBookThisSeva() flows above. */
    public boolean tapReorderFromFrequentOrderDetails() {
        return tapFirstVisible(frequentReorderAction);
    }
}
