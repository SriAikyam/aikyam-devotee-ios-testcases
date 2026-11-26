package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Review & Pay's email field and the screens around PayU CheckoutPro on iOS. Nothing here opens
 * PayU; that goes through BookSevaPage.tapProceedToPaymentInPayUTestMode(). PayU's card and OTP
 * pages render inside the SDK, so tests only cancel out of PayU and never type card data.
 */
public class SevaPaymentPage extends BasePage {

    private final By emailField = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeTextField' AND (placeholderValue CONTAINS[c] 'email' OR value CONTAINS '@')");
    private final By paymentSuccess = text("Payment Successful");
    private final By emailRequired = text("Email is required");
    private final By invalidEmail = text("Please enter a valid email address");
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
        WebElement field = emailFieldElement();
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
        WebElement field = emailFieldElement();
        field.click();
        field.clear();
        hideKeyboardIfShown();
        String v = field.getAttribute("value");
        return v == null || !v.contains("@");
    }

    /** The same field for typing and clearing: the last match (Review & Pay's own field sits lowest). */
    private WebElement emailFieldElement() {
        List<WebElement> fields = visibleElements(emailField);
        return fields.get(fields.size() - 1);
    }

    /** The phone's appearance before the first forceLightMode(), so restoreAppearance() can put it back. */
    private static String appearanceBeforeRun;

    /** Switches the phone to light mode (XCUITest mobile: setAppearance) and confirms it. */
    public boolean forceLightMode() {
        try {
            if (appearanceBeforeRun == null) {
                appearanceBeforeRun = appearance(driver);
            }
            driver.executeScript("mobile: setAppearance", Map.of("style", "light"));
            return "light".equals(appearance(driver));
        } catch (RuntimeException unsupported) {
            return false;
        }
    }

    /** Puts back the appearance forceLightMode() replaced, if any. Called when the shared session ends. */
    public static void restoreAppearance(IOSDriver driver) {
        if (appearanceBeforeRun == null) {
            return;
        }
        try {
            driver.executeScript("mobile: setAppearance", Map.of("style", appearanceBeforeRun));
            appearanceBeforeRun = null;
        } catch (RuntimeException ignored) {
            // Best effort: the session may already be gone.
        }
    }

    @SuppressWarnings("unchecked")
    private static String appearance(IOSDriver driver) {
        Object info = driver.executeScript("mobile: getAppearance");
        return String.valueOf(((Map<String, Object>) info).get("style"));
    }

    public boolean isEmailRequiredVisible() {
        return isAnyVisible(emailRequired);
    }

    public boolean isInvalidEmailVisible() {
        return isAnyVisible(invalidEmail);
    }

    /**
     * PayU CheckoutPro hides its whole screen from iOS accessibility: with PayU in front, Appium's
     * page source still shows Review & Pay underneath and none of PayU's labels (confirmed on
     * iPhone 12, 25 Nov 2026, with snapshotMaxDepth raised too). So PayU is recognised from a
     * screenshot instead: its dark header and dark footer bar, which Review & Pay (light) lacks.
     * Only valid in light mode; BookSevaPage.tapProceedToPaymentInPayUTestMode() forces it.
     *
     * Sample and tap positions are screen fractions calibrated on an iPhone 12 (iOS 27.0.1, PayU
     * CheckoutPro as of 25 Nov 2026). Another screen shape (Dynamic Island) or PayU SDK update may
     * move them: override with -Daikyam.payu.<name>=<fraction> (names in the pos() calls below).
     */
    public enum PayUState { NOT_SHOWN, CHECKOUT, CANCEL_SHEET }

    public PayUState payUState() {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(driver.getScreenshotAs(OutputType.BYTES)));
            boolean header = isDarkGrey(img, pos("headerX", 0.85), pos("headerY", 0.12));
            boolean footer = isDarkGrey(img, pos("footerX", 0.30), pos("footerY", 0.975));
            if (header && footer) {
                return PayUState.CHECKOUT;
            }
            if (header && isLight(img, pos("sheetX", 0.50), pos("sheetY", 0.93))) {
                return PayUState.CANCEL_SHEET;
            }
            return PayUState.NOT_SHOWN;
        } catch (IOException | RuntimeException e) {
            return PayUState.NOT_SHOWN;
        }
    }

    /** A calibrated screen fraction, overridable with -Daikyam.payu.<name>. */
    private static double pos(String name, double iPhone12Default) {
        String v = System.getProperty("aikyam.payu." + name);
        return v == null ? iPhone12Default : Double.parseDouble(v);
    }

    private static int[] rgb(BufferedImage img, double fx, double fy) {
        int c = img.getRGB((int) (img.getWidth() * fx), Math.min(img.getHeight() - 1, (int) (img.getHeight() * fy)));
        return new int[] {(c >> 16) & 0xff, (c >> 8) & 0xff, c & 0xff};
    }

    /** PayU's bar is a flat dark grey (~#26282C); a photo or tinted header is rarely that flat. */
    private static boolean isDarkGrey(BufferedImage img, double fx, double fy) {
        int[] p = rgb(img, fx, fy);
        int max = Math.max(p[0], Math.max(p[1], p[2]));
        int min = Math.min(p[0], Math.min(p[1], p[2]));
        return max < 70 && max - min < 20;
    }

    private static boolean isLight(BufferedImage img, double fx, double fy) {
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
            settle(500);
        }
        return false;
    }

    /** Leaves PayU with its own back arrow (top left), confirming "Cancel Payment" with YES. Taps go
     * by position because PayU's controls aren't in the accessibility tree. True once PayU is gone. */
    public boolean cancelPayU() {
        Dimension size = driver.manage().window().getSize();
        for (int i = 0; i < 6; i++) {
            PayUState state = payUState();
            if (state == PayUState.NOT_SHOWN) {
                return true;
            }
            if (state == PayUState.CANCEL_SHEET) {
                tapPoint((int) (size.getWidth() * pos("yesX", 0.27)), (int) (size.getHeight() * pos("yesY", 0.935)));
            } else {
                tapPoint((int) (size.getWidth() * pos("backX", 0.04)), (int) (size.getHeight() * pos("backY", 0.097)));
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

    public boolean isNetBankingOptionVisible() {
        return isAnyVisible(payUNetBanking);
    }


    public boolean isPaymentSuccessVisible() {
        return isAnyVisible(paymentSuccess);
    }

    /** Raw technical errors that must never reach a devotee. */
    public boolean isRawTechnicalErrorVisible() {
        return isVisibleNow(textContains("status:"), textContains("Exception"), textContains("HTTP 5"),
                textContains("NSURLError"), textContains("endpoint not found"), textContains("decoding"));
    }
}
