package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.categories.P0;
import com.shriaikyam.aikyam.pages.BasePage;
import com.shriaikyam.aikyam.pages.BookSevaPage;
import com.shriaikyam.aikyam.pages.DeepLinkPage;
import com.shriaikyam.aikyam.pages.HomePage;
import com.shriaikyam.aikyam.pages.SevaPaymentPage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * iOS port of the Android seva booking cases (TC901-TC911, same ids), plus iOS edge cases
 * (TC912-TC915). Devotee books a seva from a temple page up to Review & Pay; never opens PayU.
 *
 * Held bookings: Place Seva Order creates a Held booking on dev. Tests leave Review & Pay through
 * the app's own back path; the hold expires after its TTL, so the dev seva needs free slots for a
 * full run. Read-only cases (TC901-TC905, TC912-TC914) are P0; cases that place an order are not.
 */
public class SevaBookingJourneyTest extends BaseTest {

    /** One phone session for the whole class (see BaseTest); starting WDA per case cost ~30 s each. */
    @Override
    protected boolean reuseDriverAcrossTests() {
        return true;
    }

    @AfterClass
    public static void endSharedSession() {
        releaseSharedDriver();
    }

    @After
    public void leavePaymentFlow() {
        if (driver != null) {
            new BookSevaPage(driver).leavePaymentFlowIfOpen();
        }
    }

    private BookSevaPage openTempleSevas() {
        String link = SevaTestConfig.requireTempleLink();
        Assert.assertTrue("Temple link should open the app", new DeepLinkPage(driver).fire(link));
        BookSevaPage seva = new BookSevaPage(driver);
        Assert.assertTrue("Temple page should show a Sevas tab", seva.openSevasTab());
        Assert.assertTrue("Sevas tab should list a bookable seva (Book)", seva.isSevaCardVisible());
        return seva;
    }

    private BookSevaPage openBookingForm() {
        BookSevaPage seva = openTempleSevas();
        Assert.assertTrue("Book should open the booking form", seva.tapFirstSevaBook());
        Assert.assertTrue("Book a Seva form should show", seva.isBookingDetailsScreenVisible());
        return seva;
    }

    private BookSevaPage fillValidForm() {
        BookSevaPage seva = openBookingForm();
        Assert.assertTrue("A date should be selectable", seva.confirmDefaultDate());
        Assert.assertTrue("Devotee should be selectable", seva.toggleDevotee(SevaTestConfig.requireSelfName()));
        Assert.assertTrue(seva.tapPlaceSevaOrder());
        return seva;
    }

    @Category(P0.class)
    @Test
    public void TC901_Verify_Temple_Sevas_Tab_Lists_A_Bookable_Seva_With_Book_And_Details() {
        openTempleSevas();
    }

    @Category(P0.class)
    @Test
    public void TC902_Verify_Seva_Details_Opens_And_Book_This_Seva_Reaches_The_Form() {
        BookSevaPage seva = openTempleSevas();
        Assert.assertTrue(seva.tapFirstSevaDetails());
        Assert.assertTrue("Seva details should offer Book this seva", seva.isCatalogDetailsScreenVisible());
        Assert.assertTrue(seva.tapBookThisSeva());
        Assert.assertTrue("Book this seva should open the booking form", seva.isBookingDetailsScreenVisible());
    }

    @Category(P0.class)
    @Test
    public void TC903_Verify_Empty_Form_Shows_Date_And_Devotee_Errors_And_Does_Not_Submit() {
        BookSevaPage seva = openBookingForm();
        Assert.assertTrue(seva.tapPlaceSevaOrder());
        Assert.assertTrue("Missing date should be flagged", seva.isDateValidationErrorVisible());
        Assert.assertTrue("Missing devotee should be flagged", seva.isDevoteeValidationErrorVisible());
        Assert.assertFalse("An invalid form must not reach Review & Pay", seva.isPaymentReviewVisibleNow());
    }

    @Category(P0.class)
    @Test
    public void TC904_Verify_Date_Only_Still_Requires_A_Devotee() {
        BookSevaPage seva = openBookingForm();
        Assert.assertTrue(seva.confirmDefaultDate());
        Assert.assertTrue(seva.tapPlaceSevaOrder());
        Assert.assertTrue("Devotee error should show", seva.isDevoteeValidationErrorVisible());
        Assert.assertFalse("Date error should not show once a date is chosen", seva.isDateValidationErrorVisible());
    }

    @Category(P0.class)
    @Test
    public void TC905_Verify_Devotee_Count_Follows_Selection() {
        BookSevaPage seva = openBookingForm();
        String self = SevaTestConfig.requireSelfName();
        Assert.assertTrue(seva.isDevoteeDetailsVisible());
        int before = seva.readTotalDevoteesCount();
        Assert.assertTrue("Total number of devotees should be readable", before >= 0);
        Assert.assertTrue(seva.toggleDevotee(self));
        Assert.assertEquals("Selecting one devotee adds one", before + 1, seva.readTotalDevoteesCount());
        Assert.assertTrue(seva.toggleDevotee(self));
        Assert.assertEquals("Unselecting returns the count", before, seva.readTotalDevoteesCount());
    }

    @Test
    public void TC906_Verify_Valid_Form_Reaches_Review_And_Pay_With_Breakdown_And_Total() {
        BookSevaPage seva = fillValidForm();
        Assert.assertTrue("Review & Pay should open", seva.waitForPaymentReviewScreen());
        Assert.assertTrue("Amount Breakdown should show", seva.isAmountBreakdownVisible());
        Assert.assertTrue("Total Amount should show", seva.isFinalTotalVisible());
        Assert.assertTrue(new SevaPaymentPage(driver).enterEmail(SevaTestConfig.TEST_EMAIL));
        Assert.assertTrue("Proceed to Payment should be enabled with an email", seva.isProceedToPaymentEnabled());
        Assert.assertTrue(seva.cancelPaymentReview());
    }

    @Test
    public void TC907_Verify_Kanike_And_Hundi_Add_Exactly_To_The_Total() {
        BookSevaPage seva = fillValidForm();
        Assert.assertTrue(seva.waitForPaymentReviewScreen());
        BigDecimal base = seva.readFinalTotal();
        Assert.assertNotNull("Total Amount should show an amount", base);
        Assert.assertTrue(seva.cancelPaymentReview());

        BookSevaPage again = openBookingForm();
        Assert.assertTrue(again.confirmDefaultDate());
        Assert.assertTrue(again.toggleDevotee(SevaTestConfig.requireSelfName()));
        // Chip values match Android (iOS PR for QA DEF-09): Kanike 5/10/20/50, Hundi 11/21/51/116.
        int kanike = Integer.getInteger("aikyam.kanikeChip", 20);
        int hundi = Integer.getInteger("aikyam.hundiChip", 21);
        Assert.assertTrue("Kanike ₹" + kanike + " chip", again.tapAmountChip(kanike));
        Assert.assertTrue("Hundi ₹" + hundi + " chip", again.tapAmountChip(hundi));
        Assert.assertTrue(again.tapPlaceSevaOrder());
        Assert.assertTrue(again.waitForPaymentReviewScreen());
        BigDecimal withOfferings = again.readFinalTotal();
        Assert.assertNotNull(withOfferings);
        Assert.assertEquals("Total = seva + kanike + hundi", 0,
                base.add(BigDecimal.valueOf(kanike + hundi)).compareTo(withOfferings));
        Assert.assertTrue(again.cancelPaymentReview());
    }

    /** QA SEV-D09: two devotees. A per-person seva doubles the seva amount; a per-seva one keeps it. Needs
     * -Daikyam.otherDevoteeName (a second family member on the account). */
    @Test
    public void TC916_Verify_Two_Devotees_Scale_The_Total_By_Price_Type() {
        String other = System.getProperty("aikyam.otherDevoteeName", "");
        org.junit.Assume.assumeFalse("Skipping: set aikyam.otherDevoteeName", other.isEmpty());
        BookSevaPage seva = fillValidForm();
        Assert.assertTrue(seva.waitForPaymentReviewScreen());
        BigDecimal one = seva.readFinalTotal();
        Assert.assertNotNull(one);
        Assert.assertTrue(seva.cancelPaymentReview());

        BookSevaPage again = openBookingForm();
        Assert.assertTrue(again.confirmDefaultDate());
        Assert.assertTrue(again.toggleDevotee(SevaTestConfig.requireSelfName()));
        Assert.assertTrue(again.toggleDevotee(other));
        Assert.assertEquals("Total number of devotees", 2, again.readTotalDevoteesCount());
        boolean perPerson = again.isFooterTextVisible("(2 x ");
        Assert.assertTrue("Footer should say 2 devotees", again.isFooterTextVisible("2 devotees"));
        Assert.assertTrue(again.tapPlaceSevaOrder());
        Assert.assertTrue(again.waitForPaymentReviewScreen());
        BigDecimal two = again.readFinalTotal();
        Assert.assertNotNull(two);
        BigDecimal expected = perPerson ? one.multiply(BigDecimal.valueOf(2)) : one;
        Assert.assertEquals((perPerson ? "Per-person" : "Per-seva") + " total for 2 devotees", 0, expected.compareTo(two));
        Assert.assertTrue(again.cancelPaymentReview());
    }

    @Test
    public void TC908_Verify_Back_From_Review_And_Pay_Returns_Without_Charging() {
        BookSevaPage seva = fillValidForm();
        Assert.assertTrue(seva.waitForPaymentReviewScreen());
        Assert.assertTrue("Back should leave Review & Pay", seva.cancelPaymentReview());
        Assert.assertTrue("Back should land on the booking form", seva.isBookingDetailsScreenVisible());
        Assert.assertFalse("No payment result should show", new SevaPaymentPage(driver).isPaymentSuccessVisible());
    }

    @Test
    public void TC909_Verify_Double_Tap_On_Place_Seva_Order_Leaves_No_Stacked_Review() {
        BookSevaPage seva = openBookingForm();
        Assert.assertTrue(seva.confirmDefaultDate());
        Assert.assertTrue(seva.toggleDevotee(SevaTestConfig.requireSelfName()));
        Assert.assertTrue("Place Seva Order should be on screen", seva.doubleTapPlaceSevaOrder());
        Assert.assertTrue(seva.waitForPaymentReviewScreen());
        Assert.assertTrue(seva.cancelPaymentReview());
        Assert.assertFalse("One back must leave Review & Pay; a second copy means a double submit",
                seva.isPaymentReviewVisibleNow());
    }

    @Test
    public void TC911_Verify_Backgrounding_On_Review_And_Pay_Keeps_The_Screen_And_Total() {
        BookSevaPage seva = fillValidForm();
        Assert.assertTrue(seva.waitForPaymentReviewScreen());
        BigDecimal total = seva.readFinalTotal();
        Assert.assertNotNull("Total Amount should show an amount", total);
        driver.runAppInBackground(Duration.ofSeconds(15));
        Assert.assertTrue("Review & Pay should still be there", seva.waitForPaymentReviewScreen());
        BigDecimal after = seva.readFinalTotal();
        Assert.assertNotNull("Total Amount should still show an amount", after);
        Assert.assertEquals("Total should be unchanged after resume", 0, total.compareTo(after));
        Assert.assertTrue(seva.cancelPaymentReview());
    }

    // ---- iOS edge cases ----

    @Category(P0.class)
    @Test
    public void TC912_Verify_Back_From_Booking_Form_Returns_To_Sevas_Without_An_Order() {
        BookSevaPage seva = openBookingForm();
        Assert.assertTrue(seva.confirmDefaultDate());
        BasePage.navigateBack(driver);
        Assert.assertTrue("Back should return to the temple's Sevas", seva.isSevaCardVisible());
        Assert.assertFalse("Leaving the form must not open Review & Pay", seva.isPaymentReviewVisibleNow());
    }

    @Category(P0.class)
    @Test
    public void TC913_Verify_Date_Sheet_Cancel_Keeps_The_Form_Without_A_Date() {
        BookSevaPage seva = openBookingForm();
        Assert.assertTrue(seva.cancelDateSheet());
        Assert.assertTrue(seva.tapPlaceSevaOrder());
        Assert.assertTrue("No date was picked, so the date error should show", seva.isDateValidationErrorVisible());
    }

    @Category(P0.class)
    @Test
    public void TC914_Verify_Backgrounding_On_Booking_Form_Keeps_Date_And_Devotee() {
        BookSevaPage seva = openBookingForm();
        Assert.assertTrue(seva.confirmDefaultDate());
        Assert.assertTrue(seva.toggleDevotee(SevaTestConfig.requireSelfName()));
        int count = seva.readTotalDevoteesCount();
        driver.runAppInBackground(Duration.ofSeconds(10));
        Assert.assertTrue("The form should still be there", seva.isBookingDetailsScreenVisible());
        Assert.assertEquals("Selected devotees should be kept", count, seva.readTotalDevoteesCount());
        Assert.assertFalse("The chosen date should be kept", seva.isDateValidationErrorVisible());
    }

    @Test
    public void TC915_Verify_Killing_The_App_On_Review_And_Pay_Shows_No_Payment_Result() {
        BookSevaPage seva = fillValidForm();
        Assert.assertTrue(seva.waitForPaymentReviewScreen());
        DeepLinkPage links = new DeepLinkPage(driver);
        Assert.assertTrue(links.forceStopApp());
        driver.activateApp(DeepLinkPage.DEV_BUNDLE_ID);
        Assert.assertTrue("Relaunch should reach a usable screen (Home or Review & Pay)",
                new HomePage(driver).isHomeBottomNavigationVisible() || seva.isPaymentReviewVisibleNow());
        SevaPaymentPage pay = new SevaPaymentPage(driver);
        Assert.assertFalse("A payment never started must not show as successful", pay.isPaymentSuccessVisible());
    }

    /** Not yet run on a device. Booking the same seva, date and devotee twice must raise the
     * "already booked" prompt; every other test accepts it silently, so this is the one that checks it. */
    @Test
    public void TC917_Verify_Booking_The_Same_Seva_And_Date_Again_Asks_First() {
        BookSevaPage seva = fillValidForm();
        Assert.assertTrue(seva.waitForPaymentReviewScreen());
        Assert.assertTrue(seva.cancelPaymentReview());

        BookSevaPage again = openBookingForm();
        Assert.assertTrue(again.confirmDefaultDate());
        Assert.assertTrue(again.toggleDevotee(SevaTestConfig.requireSelfName()));
        Assert.assertTrue(again.tapPlaceSevaOrder());
        Assert.assertTrue("A second booking for the same date should ask before booking again",
                again.wasDuplicatePromptShown());
        Assert.assertTrue(again.waitForPaymentReviewScreen());
        Assert.assertTrue(again.cancelPaymentReview());
    }
}
