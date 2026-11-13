package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.BasePage;
import com.shriaikyam.aikyam.pages.LoginPage;
import com.shriaikyam.aikyam.pages.ProfilePage;
import com.shriaikyam.aikyam.pages.SettingsPage;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** aikyam-devotee-ios#719 (issue #657): logout deletes the tokens before the login screen
 * shows, so killing the app right after can't sign the user back in on the next launch.
 *
 * Every test here logs out and signs back in, and dev has no fixed OTP: without
 * AIKYAM_TEST_PHONE and AIKYAM_TEST_OTP the class skips instead of leaving the rest of the
 * run logged out. Each test signs back in at the end even when it fails. */
public class LogoutSurvivesKillTest extends BaseTest {

    private static final By HOME_TAB = BasePage.idOrLabel("tab.home", "Home");
    /** Relaunch shows the launch icon first; a cold simulator can hold it for a while. */
    private static final Duration RELAUNCH_TIMEOUT = Duration.ofSeconds(60);

    /** The fix's guarantee: once the login flow is on screen the session is gone, so a kill
     * at that moment (with server cleanup still running in the background) relaunches
     * logged out. */
    @Test
    public void TC390_Kill_Right_After_Logout_Relaunches_Logged_Out() {
        assumeTestLoginAvailable();
        try {
            logOutAndKillAsSoonAsLoginShows();

            relaunch();

            LoginPage loginPage = new LoginPage(driver);
            Assert.assertTrue("Relaunch after a killed logout should show the logged-out entry screen",
                    loginPage.waitForLoggedOutEntry(RELAUNCH_TIMEOUT));
            Assert.assertFalse("Relaunch after a killed logout must not land on Home",
                    isHomeTabVisibleNow());
        } finally {
            signBackInBestEffort();
        }
    }

    /** The wipe happens only on Yes: cancelling the dialog and then killing the app keeps
     * the user signed in. */
    @Test
    public void TC391_Cancel_Logout_Then_Kill_Keeps_Session() {
        assumeTestLoginAvailable();
        try {
            SettingsPage settingsPage = openSettingsHub();
            Assert.assertTrue("Logout row should be tappable", settingsPage.tapLogout());
            Assert.assertTrue("Logout confirmation should show", settingsPage.isLogoutConfirmDialogVisible());
            Assert.assertTrue("No should dismiss the confirmation", settingsPage.tapLogoutCancel());

            relaunch();

            Assert.assertTrue("After cancelling logout and relaunching, Home should show",
                    waitForHomeTab(RELAUNCH_TIMEOUT));
        } finally {
            signBackInBestEffort();
        }
    }

    /** A kill mid-cleanup leaves a "logout pending" marker that the next launch finishes.
     * Signing in on that launch must clear it: a second relaunch has to keep the NEW session
     * instead of logging it out again. */
    @Test
    public void TC392_Sign_In_After_Killed_Logout_Survives_Next_Relaunch() {
        assumeTestLoginAvailable();
        try {
            logOutAndKillAsSoonAsLoginShows();
            relaunch();

            Assert.assertTrue("Sign-in after a killed logout should reach Home", signIn());

            relaunch();

            Assert.assertTrue("The new session should survive the next relaunch",
                    waitForHomeTab(RELAUNCH_TIMEOUT));
        } finally {
            signBackInBestEffort();
        }
    }

    private void logOutAndKillAsSoonAsLoginShows() {
        SettingsPage settingsPage = openSettingsHub();
        Assert.assertTrue("Logout row should be tappable", settingsPage.tapLogout());
        Assert.assertTrue("Logout confirmation should show", settingsPage.isLogoutConfirmDialogVisible());
        Assert.assertTrue("Yes should confirm logout", settingsPage.tapLogoutConfirm());
        Assert.assertTrue("The logged-out entry screen should show after confirming logout",
                new LoginPage(driver).waitForLoggedOutEntry(Duration.ofSeconds(15)));
        driver.terminateApp(bundleId());
    }

    private void relaunch() {
        driver.terminateApp(bundleId());
        driver.activateApp(bundleId());
    }

    private boolean signIn() {
        LoginPage loginPage = new LoginPage(driver);
        if (!loginPage.waitForLoggedOutEntry(RELAUNCH_TIMEOUT) || !loginPage.proceedToPhoneNumberScreen()) {
            return false;
        }
        if (!loginPage.enterPhoneNumber(System.getenv("AIKYAM_TEST_PHONE"))
                || !loginPage.tapSendVerificationCode()
                || !loginPage.waitForOtpScreen()
                || !loginPage.enterOtp(System.getenv("AIKYAM_TEST_OTP"))) {
            return false;
        }
        return waitForHomeTab(RELAUNCH_TIMEOUT);
    }

    /** Leaves the app signed in for the next test whatever happened above. Catches
     * Throwable: a failure here must not hide the test's own result. */
    private void signBackInBestEffort() {
        try {
            if (isHomeTabVisibleNow()) {
                return;
            }
            relaunch();
            if (!waitForHomeTab(Duration.ofSeconds(15))) {
                signIn();
            }
        } catch (Throwable ignored) {
            // Best-effort only.
        }
    }

    private SettingsPage openSettingsHub() {
        ProfilePage profilePage = new ProfilePage(driver);
        Assert.assertTrue("Settings hub should open", profilePage.openSettingsHub());
        return new SettingsPage(driver);
    }

    private boolean waitForHomeTab(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(ignored -> isHomeTabVisibleNow());
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean isHomeTabVisibleNow() {
        return driver.findElements(HOME_TAB).stream().anyMatch(element -> element.isDisplayed());
    }

    private static void assumeTestLoginAvailable() {
        Assume.assumeTrue("Skipping: AIKYAM_TEST_PHONE and AIKYAM_TEST_OTP must be set - these tests "
                        + "log out and need to sign back in, and dev has no fixed OTP",
                isSet("AIKYAM_TEST_PHONE") && isSet("AIKYAM_TEST_OTP"));
    }

    private static boolean isSet(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }
}
