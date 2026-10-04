package com.shriaikyam.aikyam.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class LoginPage extends BasePage {

    private final By phoneNumberScreenAnchor = textContains("Verify your number");
    private final By sendVerificationCodeAction = text("Send Verification Code");
    private final By verificationSentToAnchor = textContains("sent the verification code");
    private final By otpEditText = io.appium.java_client.AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\")"
    );
    private final By verifyingIndicator = textContains("Verifying");
    // Android's native SMS User Consent bottom sheet (rendered by Google Play Services,
    // not the app's own Compose UI) no longer appears on this device/Android version -
    // OTP autofill is now an inline keyboard suggestion instead, which Appium can't act
    // on. The OTP is read from the SMS inbox directly (DeviceSmsReader) and typed in;
    // "Deny" clears the dialog out of the way first on the rare case it still shows.
    private final By smsConsentDenyAction = text("Deny");
    private final By backAction = accessibilityId("Back");
    // R.string.wrong_number ("Wrong Number?") - a plain text link on the OTP screen,
    // navigates back to the phone-number screen without needing to complete OTP entry.
    private final By wrongNumberAction = text("Wrong Number?");

    public LoginPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isPhoneNumberScreenVisible() {
        return isAnyVisible(phoneNumberScreenAnchor, sendVerificationCodeAction);
    }

    /**
     * The phone number field has no contentDescription; it is the only editable
     * text field on this screen, so a bare EditText-class locator is unambiguous here.
     */
    public boolean enterPhoneNumber(String phoneNumber) {
        return typeIntoFirstVisible(phoneNumber, otpEditText);
    }

    public boolean tapSendVerificationCode() {
        return tapFirstVisible(sendVerificationCodeAction);
    }

    public boolean waitForOtpScreen() {
        try {
            waitForAnyVisible(verificationSentToAnchor, otpEditText);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isOtpScreenVisible() {
        return isAnyVisible(verificationSentToAnchor, otpEditText);
    }

    /**
     * Entering all 6 digits auto-triggers verification after a short delay - no submit
     * button to tap.
     */
    public boolean enterOtp(String otp) {
        return typeIntoFirstVisible(otp, otpEditText);
    }

    /**
     * Clears the SMS consent dialog out of the way (if it happens to be showing) so it
     * doesn't block typing into the app's own OTP field. Not a wait - just a one-shot
     * check, used only once the OTP is already known via DeviceSmsReader.
     */
    public boolean dismissSmsConsentIfPresent() {
        if (!isAnyVisible(smsConsentDenyAction)) {
            return true;
        }
        return tapFirstVisible(smsConsentDenyAction);
    }

    public boolean tapWrongNumber() {
        return tapFirstVisible(wrongNumberAction);
    }

    public boolean waitForDashboard(FeedPage feedPage) {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ignored -> {
                if (isAnyVisible(verifyingIndicator)) {
                    return false;
                }
                return feedPage.isAnyFeedItemVisible() || feedPage.hasNonBlankPageSource();
            });
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Taps the top-left Back arrow on the OTP screen as fast as possible, back-to-back,
     * with no wait between taps - used to probe for a rapid-tap crash. */
    public void rapidTapBack(int times) {
        for (int i = 0; i < times; i++) {
            tapFirstVisible(backAction);
        }
    }

    /** True if the app process is still alive and responsive (not crashed) after a
     * disruptive action like rapid back-button tapping. */
    public boolean isAppResponsive() {
        try {
            Object appPackage = driver.getCapabilities().getCapability("appium:appPackage");
            String expectedPackage = appPackage != null ? appPackage.toString() : "com.shriaikyam.aikyam.dev";
            return expectedPackage.equals(driver.getCurrentPackage())
                    && driver.getPageSource() != null
                    && !driver.getPageSource().isBlank();
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
