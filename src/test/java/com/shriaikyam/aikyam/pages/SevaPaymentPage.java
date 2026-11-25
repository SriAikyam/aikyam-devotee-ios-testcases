package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * Review & Pay's email field and the screens around PayU CheckoutPro on iOS. Nothing here opens
 * PayU; that goes through BookSevaPage.tapProceedToPaymentInPayUTestMode(). PayU's card and OTP
 * pages render inside the SDK, so tests only cancel out of PayU and never type card data.
 */
public class SevaPaymentPage extends BasePage {

    private final By emailField = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeTextField' AND (placeholderValue CONTAINS[c] 'email' OR value CONTAINS '@')");
    private final By paymentSuccess = text("Payment Successful");
    private final By paymentCancelled = textContains("Payment cancelled");
    private final By paymentFailed = textContains("Payment failed");
    private final By emailRequired = text("Email is required");
    // PayU CheckoutPro's option list; these labels come from the PayU SDK.
    private final By payUCards = textContains("Cards");
    private final By payUUpi = text("UPI");
    private final By payUNetBanking = textContains("Net Banking");

    public SevaPaymentPage(IOSDriver driver) {
        super(driver);
    }

    public boolean enterEmail(String email) {
        if (!scrollUntilVisible(8, emailField)) {
            return false;
        }
        List<WebElement> fields = visibleElements(emailField);
        WebElement field = fields.get(fields.size() - 1);
        field.click();
        waitUntilKeyboardShown();
        field.clear();
        field.sendKeys(email);
        hideKeyboardIfShown();
        return valueLandedInField(field, email);
    }

    public boolean clearEmail() {
        if (!scrollUntilVisible(8, emailField)) {
            return false;
        }
        WebElement field = visibleElements(emailField).get(0);
        field.click();
        field.clear();
        hideKeyboardIfShown();
        String v = field.getAttribute("value");
        return v == null || !v.contains("@");
    }

    public boolean isEmailRequiredVisible() {
        return isAnyVisible(emailRequired);
    }

    /**
     * PayU CheckoutPro hides its whole screen from iOS accessibility: with PayU in front, Appium's
     * page source still shows Review & Pay underneath and none of PayU's labels (confirmed on
     * iPhone 12, 25 Nov 2026, with snapshotMaxDepth raised too). So PayU is recognised from a
     * screenshot instead: its dark header and dark footer bar, which Review & Pay (light) lacks.
     */
    public enum PayUState { NOT_SHOWN, CHECKOUT, CANCEL_SHEET }

    public PayUState payUState() {
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(
                    new java.io.ByteArrayInputStream(driver.getScreenshotAs(org.openqa.selenium.OutputType.BYTES)));
            boolean header = isDarkGrey(img, 0.85, 0.12);
            boolean footer = isDarkGrey(img, 0.30, 0.975);
            if (header && footer) {
                return PayUState.CHECKOUT;
            }
            if (header && isLight(img, 0.50, 0.93)) {
                return PayUState.CANCEL_SHEET;
            }
            return PayUState.NOT_SHOWN;
        } catch (java.io.IOException | RuntimeException e) {
            return PayUState.NOT_SHOWN;
        }
    }

    private static int[] rgb(java.awt.image.BufferedImage img, double fx, double fy) {
        int c = img.getRGB((int) (img.getWidth() * fx), Math.min(img.getHeight() - 1, (int) (img.getHeight() * fy)));
        return new int[] {(c >> 16) & 0xff, (c >> 8) & 0xff, c & 0xff};
    }

    /** PayU's bar is a flat dark grey (~#26282C); a photo or tinted header is rarely that flat. */
    private static boolean isDarkGrey(java.awt.image.BufferedImage img, double fx, double fy) {
        int[] p = rgb(img, fx, fy);
        int max = Math.max(p[0], Math.max(p[1], p[2]));
        int min = Math.min(p[0], Math.min(p[1], p[2]));
        return max < 70 && max - min < 20;
    }

    private static boolean isLight(java.awt.image.BufferedImage img, double fx, double fy) {
        int[] p = rgb(img, fx, fy);
        return p[0] > 200 && p[1] > 200 && p[2] > 200;
    }

    public boolean isPayUOnScreen() {
        return payUState() != PayUState.NOT_SHOWN;
    }

    public boolean waitForPayUCheckout(Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (isVisibleNow(payUCards, payUUpi, payUNetBanking) || payUState() == PayUState.CHECKOUT) {
                return true;
            }
        }
        return false;
    }

    /** Leaves PayU with its own back arrow (top left), confirming "Cancel Payment" with YES. Taps go
     * by position because PayU's controls aren't in the accessibility tree. True once PayU is gone. */
    public boolean cancelPayU() {
        org.openqa.selenium.Dimension size = driver.manage().window().getSize();
        for (int i = 0; i < 6; i++) {
            PayUState state = payUState();
            if (state == PayUState.NOT_SHOWN) {
                return true;
            }
            if (state == PayUState.CANCEL_SHEET) {
                tapPoint((int) (size.getWidth() * 0.27), (int) (size.getHeight() * 0.935)); // YES
            } else {
                tapPoint((int) (size.getWidth() * 0.04), (int) (size.getHeight() * 0.097)); // PayU back
            }
            settle(2000);
        }
        return payUState() == PayUState.NOT_SHOWN;
    }

    private static void settle(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean arePayULabelsReadable() {
        return isVisibleNow(payUCards, payUUpi, payUNetBanking);
    }

    public boolean isTextVisibleAnywhere(String value) {
        return isAnyVisible(textContains(value));
    }

    public boolean isNetBankingOptionVisible() {
        return isAnyVisible(payUNetBanking);
    }


    public boolean isPaymentSuccessVisible() {
        return isAnyVisible(paymentSuccess);
    }

    public boolean isPaymentCancelledOrFailedVisible() {
        return isAnyVisible(paymentCancelled, paymentFailed);
    }

    /** Raw technical errors that must never reach a devotee. */
    public boolean isRawTechnicalErrorVisible() {
        return isVisibleNow(textContains("status:"), textContains("Exception"), textContains("HTTP 5"),
                textContains("NSURLError"), textContains("endpoint not found"), textContains("decoding"));
    }
}
