package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.appmanagement.ApplicationState;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** LoginView: PhoneNumberContentView, then VerifyOtpContentView in place once an OTP is
 * requested. "Verify your number" is shown on BOTH steps, so the steps are told apart by
 * their own controls (phone field / send button vs. the hidden OTP field). */
public class LoginPage extends BasePage {

    // The login screens set no identifiers yet, so each control also matches its English text
    // (or, for the single text field on each step, its type).
    private final By phoneField = idOrTextField("login.phone.field");
    private final By sendVerificationCodeAction = idOrLabel("login.sendCode", "Send Verification Code");
    private final By otpField = id("login.otp.field");
    /** Only used once the OTP step is confirmed, where the hidden OTP field is the one text field. */
    private final By otpEntryField = idOrTextField("login.otp.field");
    private final By verificationSentToAnchor = textContains("sent the verification code");
    private final By verifyingIndicator = id("login.verifying");
    private final By backAction = id("login.otp.back");
    private final By wrongNumberAction = idOrLabel("login.otp.wrongNumber", "Wrong Number?");

    // What a logged-out launch shows before the phone step: WelcomeConsentView every time
    // (the app clears terms acceptance at sign-in), then LanguageSelectionView on some paths.
    private final By welcomeAgreeAction = idOrLabel("welcome.agree", "Agree and continue");
    private final By languageTitle = textContains("Pick your language");
    private final By languageContinueAction = idOrLabel("language.continue", "Continue");

    public LoginPage(IOSDriver driver) {
        super(driver);
    }

    public boolean isPhoneNumberScreenVisible() {
        return isAnyVisible(phoneField, sendVerificationCodeAction);
    }

    public boolean enterPhoneNumber(String phoneNumber) {
        return typeIntoFirstVisible(phoneNumber, phoneField);
    }

    public boolean tapSendVerificationCode() {
        return tapFirstVisible(sendVerificationCodeAction);
    }

    public boolean waitForOtpScreen() {
        try {
            waitForAnyVisible(verificationSentToAnchor, otpField, wrongNumberAction);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isOtpScreenVisible() {
        return isAnyVisible(verificationSentToAnchor, otpField, wrongNumberAction);
    }

    /** The six boxes are drawn over one near-transparent TextField; six digits auto-verify,
     * there is no submit button. The OTP itself comes from AIKYAM_TEST_OTP only. */
    public boolean enterOtp(String otp) {
        return typeIntoFirstVisible(otp, otpEntryField);
    }

    /** Any logged-out entry screen: Welcome, language picker or the phone step. Waits up to
     * {@code timeout} because a relaunch first shows the launch icon. */
    public boolean waitForLoggedOutEntry(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(ignored ->
                    isVisibleNow(welcomeAgreeAction, languageTitle, sendVerificationCodeAction));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** From a logged-out entry screen to the phone step: agrees to the terms and keeps the
     * already-selected language when those screens show up. */
    public boolean proceedToPhoneNumberScreen() {
        if (isVisibleNow(welcomeAgreeAction)) {
            tapFirstVisible(welcomeAgreeAction);
        }
        try {
            new WebDriverWait(driver, SHORT_TIMEOUT).until(ignored ->
                    isVisibleNow(languageTitle, sendVerificationCodeAction));
        } catch (RuntimeException ignored) {
            return false;
        }
        if (isVisibleNow(languageTitle)) {
            tapFirstVisible(languageContinueAction);
        }
        return isPhoneNumberScreenVisible();
    }

    private static By idOrTextField(String identifier) {
        return AppiumBy.iOSNsPredicateString("name == " + predicateLiteral(identifier)
                + " OR type == 'XCUIElementTypeTextField'");
    }

    /** iOS: there is no SMS consent sheet - the code is offered as a keyboard suggestion
     * via textContentType(.oneTimeCode), which needs no dismissal. */
    public boolean dismissSmsConsentIfPresent() {
        return true;
    }

    public boolean tapWrongNumber() {
        return tapFirstVisible(wrongNumberAction);
    }

    public boolean waitForDashboard(FeedPage feedPage) {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ignored -> {
                if (isVisibleNow(verifyingIndicator)) {
                    return false;
                }
                return feedPage.isAnyFeedItemVisible() || feedPage.hasNonBlankPageSource();
            });
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Taps the OTP screen's top-left back arrow back-to-back with no wait between taps -
     * used to probe for a rapid-tap crash. */
    public void rapidTapBack(int times) {
        for (int i = 0; i < times; i++) {
            tapFirstVisible(backAction);
        }
    }

    /** True if the app is still in the foreground with a non-empty tree (not crashed)
     * after a disruptive action like rapid back-button tapping. */
    public boolean isAppResponsive() {
        try {
            Object bundle = driver.getCapabilities().getCapability("appium:bundleId");
            String bundleId = bundle != null ? bundle.toString() : DeepLinkPage.DEV_BUNDLE_ID;
            String source = driver.getPageSource();
            return driver.queryAppState(bundleId) == ApplicationState.RUNNING_IN_FOREGROUND
                    && source != null
                    && !source.isBlank();
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
