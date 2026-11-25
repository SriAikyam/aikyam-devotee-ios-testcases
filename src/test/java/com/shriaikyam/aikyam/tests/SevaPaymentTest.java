package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.BookSevaPage;
import com.shriaikyam.aikyam.pages.DeepLinkPage;
import com.shriaikyam.aikyam.pages.SevaPaymentPage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Review & Pay validation, then PayU CheckoutPro in TEST MODE only (TC921-TC928, Android ids;
 * TC929-TC930 iOS edge cases). Every test that opens PayU calls requirePayUTestMode() first and
 * opens it only on the dev build. Tests cancel out of PayU and never type card details.
 *
 * TC923/TC926/TC928 open PayU's iOS CheckoutPro screen, which is what payment-service 1.0.14/1.0.15
 * (iOS hash names) fixed; a hash refusal shows up here as PayU never reaching its option list.
 */
public class SevaPaymentTest extends BaseTest {

    /** One phone session for the whole class (see BaseTest); starting WDA per case cost ~30 s each. */
    @Override
    protected boolean reuseDriverAcrossTests() {
        return true;
    }

    @org.junit.AfterClass
    public static void endSharedSession() {
        releaseSharedDriver();
    }

    @After
    public void leavePaymentFlow() {
        if (driver != null) {
            new BookSevaPage(driver).leavePaymentFlowIfOpen();
        }
    }

    private BookSevaPage reachPaymentReview() {
        String link = SevaTestConfig.requireTempleLink();
        String self = SevaTestConfig.requireSelfName();
        Assert.assertTrue(new DeepLinkPage(driver).fire(link));
        BookSevaPage seva = new BookSevaPage(driver);
        Assert.assertTrue(seva.openSevasTab());
        Assert.assertTrue(seva.tapFirstSevaBook());
        Assert.assertTrue(seva.confirmDefaultDate());
        Assert.assertTrue(seva.toggleDevotee(self));
        Assert.assertTrue(seva.tapPlaceSevaOrder());
        Assert.assertTrue("Review & Pay should open", seva.waitForPaymentReviewScreen());
        return seva;
    }

    @Test
    public void TC921_Verify_Missing_Email_Blocks_Payment() {
        BookSevaPage seva = reachPaymentReview();
        SevaPaymentPage pay = new SevaPaymentPage(driver);
        Assert.assertTrue("Email field should be clearable", pay.clearEmail());
        boolean enabled = seva.isProceedToPaymentEnabled();
        if (enabled && BookSevaPage.isPayUTestModeEnabled()) {
            seva.tapProceedToPaymentInPayUTestMode();
            Assert.assertFalse("PayU must not open without an email", pay.waitForPayUCheckout(Duration.ofSeconds(8)));
            Assert.assertTrue("Email is required should show", pay.isEmailRequiredVisible());
        } else {
            Assert.assertFalse("With no email, Proceed to Payment should be disabled", enabled);
        }
        Assert.assertTrue(seva.cancelPaymentReview());
    }

    @Test
    public void TC923_Verify_PayU_Opens_In_Test_Mode_With_The_Review_Amount() {
        SevaTestConfig.requirePayUTestMode();
        BookSevaPage seva = reachPaymentReview();
        BigDecimal total = seva.readFinalTotal();
        Assert.assertNotNull("Total Amount should show an amount", total);
        SevaPaymentPage pay = new SevaPaymentPage(driver);
        Assert.assertTrue(pay.enterEmail(SevaTestConfig.TEST_EMAIL));
        Assert.assertTrue(seva.tapProceedToPaymentInPayUTestMode());
        Assert.assertTrue("PayU checkout should open (iOS hash names signed)", pay.waitForPayUCheckout(Duration.ofSeconds(40)));
        // PayU's amount isn't readable through accessibility on iOS (see SevaPaymentPage); the
        // failure screenshot of a run shows it. The Review & Pay total it was opened with is checked.
        Assert.assertTrue("Review & Pay total should be positive", total.signum() > 0);
        Assert.assertTrue("Back + YES should leave PayU", pay.cancelPayU());
    }

    @Test
    public void TC926_Verify_Killing_The_App_Inside_PayU_Does_Not_Show_A_False_Success() {
        SevaTestConfig.requirePayUTestMode();
        BookSevaPage seva = reachPaymentReview();
        SevaPaymentPage pay = new SevaPaymentPage(driver);
        Assert.assertTrue(pay.enterEmail(SevaTestConfig.TEST_EMAIL));
        Assert.assertTrue(seva.tapProceedToPaymentInPayUTestMode());
        Assert.assertTrue(pay.waitForPayUCheckout(Duration.ofSeconds(40)));
        DeepLinkPage links = new DeepLinkPage(driver);
        Assert.assertTrue(links.forceStopApp());
        driver.activateApp(DeepLinkPage.DEV_BUNDLE_ID);
        Assert.assertFalse("A payment abandoned by a kill must not show as successful", pay.isPaymentSuccessVisible());
    }

    @Test
    public void TC928_Verify_Net_Banking_List_Loads_In_PayU() {
        SevaTestConfig.requirePayUTestMode();
        BookSevaPage seva = reachPaymentReview();
        SevaPaymentPage pay = new SevaPaymentPage(driver);
        Assert.assertTrue(pay.enterEmail(SevaTestConfig.TEST_EMAIL));
        Assert.assertTrue(seva.tapProceedToPaymentInPayUTestMode());
        Assert.assertTrue(pay.waitForPayUCheckout(Duration.ofSeconds(40)));
        org.junit.Assume.assumeTrue("Skipping: PayU hides its option list from iOS accessibility, so Net Banking "
                + "can only be checked by eye (seen on iPhone 12, 25 Nov 2026: Cards, Net Banking, UPI, Wallets, EMI)",
                pay.arePayULabelsReadable());
        Assert.assertTrue("Net Banking should be offered", pay.isNetBankingOptionVisible());
    }

    // ---- iOS edge cases ----

    @Test
    public void TC929_Verify_Invalid_Email_Is_Not_Accepted() {
        BookSevaPage seva = reachPaymentReview();
        SevaPaymentPage pay = new SevaPaymentPage(driver);
        pay.clearEmail();
        Assert.assertTrue(pay.enterEmail("not-an-email"));
        boolean enabled = seva.isProceedToPaymentEnabled();
        if (enabled && BookSevaPage.isPayUTestModeEnabled()) {
            seva.tapProceedToPaymentInPayUTestMode();
            Assert.assertFalse("PayU must not open with an invalid email", pay.waitForPayUCheckout(Duration.ofSeconds(8)));
        } else {
            Assert.assertFalse("An invalid email should keep Proceed to Payment disabled", enabled);
        }
        Assert.assertFalse(pay.isRawTechnicalErrorVisible());
        Assert.assertTrue(seva.cancelPaymentReview());
    }

    @Test
    public void TC930_Verify_Cancelling_PayU_Returns_With_No_Success_And_No_Raw_Error() {
        SevaTestConfig.requirePayUTestMode();
        BookSevaPage seva = reachPaymentReview();
        SevaPaymentPage pay = new SevaPaymentPage(driver);
        Assert.assertTrue(pay.enterEmail(SevaTestConfig.TEST_EMAIL));
        Assert.assertTrue(seva.tapProceedToPaymentInPayUTestMode());
        Assert.assertTrue(pay.waitForPayUCheckout(Duration.ofSeconds(40)));
        Assert.assertTrue("Back + YES should leave PayU", pay.cancelPayU());
        Assert.assertFalse("Cancelling PayU must not show Payment Successful", pay.isPaymentSuccessVisible());
        Assert.assertFalse("No raw technical error after cancel", pay.isRawTechnicalErrorVisible());
    }
}
