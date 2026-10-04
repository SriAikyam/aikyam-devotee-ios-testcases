package com.shriaikyam.aikyam.pages;

import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/** OnboardingProfileScreen ("Complete your profile"): Full Name, Username, a Date of
 * Birth button that opens a graphical DatePicker sheet, Male/Female/Other, an optional
 * location section, and Continue. */
public class OnboardingPage extends BasePage {

    private final By backAction = id("onboarding.back");
    private final By screenAnchor = id("onboarding.title");
    private final By fullNameField = id("onboarding.fullName");
    private final By usernameField = id("onboarding.username");
    private final By dobField = id("onboarding.dob");
    private final By maleOption = id("onboarding.gender.male");
    private final By femaleOption = id("onboarding.gender.female");
    private final By otherOption = id("onboarding.gender.other");
    private final By continueAction = id("onboarding.continue");
    private final By loadingOverlay = id("onboarding.loading");
    // The sheet's DatePicker starts on viewModel.selectedDate; Done commits it as-is.
    // Driving the graphical calendar to a specific date is not attempted here.
    private final By dobDialogOkAction = id("onboarding.dob.done");
    private final By dobDialogCancelAction = id("onboarding.dob.cancel");

    public OnboardingPage(IOSDriver driver) {
        super(driver);
    }

    public boolean isScreenVisible() {
        return isAnyVisible(screenAnchor, continueAction);
    }

    public void waitUntilLoaded() {
        waitForAnyVisible(screenAnchor, continueAction);
    }

    public boolean tapBack() {
        return tapFirstVisible(backAction);
    }

    public boolean enterFullName(String name) {
        return typeInto(fullNameField, name);
    }

    /** An empty SwiftUI TextField reports its placeholder as `value`, so a value equal to
     * the placeholder means the field is empty. */
    public boolean isUsernamePrefilled() {
        List<WebElement> fields = visibleElements(usernameField);
        if (fields.isEmpty()) {
            return false;
        }
        WebElement field = fields.get(0);
        String value = field.getAttribute("value");
        String placeholder = field.getAttribute("placeholderValue");
        return value != null && !value.isBlank() && !value.equals(placeholder);
    }

    public boolean enterUsername(String username) {
        return typeInto(usernameField, username);
    }

    public boolean isUsernameFieldVisible() {
        return isAnyVisible(usernameField);
    }

    public boolean openDobPicker() {
        return tapFirstVisible(dobField);
    }

    /** Opens the DOB sheet and commits the date it opens on via Done. */
    public boolean acceptDefaultDob() {
        if (!openDobPicker()) {
            return false;
        }
        try {
            waitForAnyVisible(dobDialogOkAction);
        } catch (RuntimeException exception) {
            return false;
        }
        return tapFirstVisible(dobDialogOkAction);
    }

    public boolean cancelDobPicker() {
        if (!openDobPicker()) {
            return false;
        }
        try {
            waitForAnyVisible(dobDialogCancelAction);
        } catch (RuntimeException exception) {
            return false;
        }
        return tapFirstVisible(dobDialogCancelAction);
    }

    public boolean selectGender(String gender) {
        By option;
        if ("Male".equals(gender)) {
            option = maleOption;
        } else if ("Female".equals(gender)) {
            option = femaleOption;
        } else {
            option = otherOption;
        }
        hideKeyboardIfShown();
        return scrollUntilVisible(option) && tapFirstVisible(option);
    }

    public boolean isContinueEnabled() {
        try {
            WebElement continueButton = waitForAnyVisible(continueAction);
            return continueButton.isEnabled();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean tapContinue() {
        hideKeyboardIfShown();
        return scrollUntilVisible(continueAction) && tapFirstVisible(continueAction);
    }

    /** Fills every required field with valid data and taps Continue - the shortest
     * reliable path to completing onboarding. */
    public boolean completeWithValidData(String fullName, String username, String gender) {
        if (!enterFullName(fullName)) {
            return false;
        }
        if (!enterUsername(username)) {
            return false;
        }
        hideKeyboardIfShown();
        if (!acceptDefaultDob()) {
            return false;
        }
        if (!selectGender(gender)) {
            return false;
        }
        return tapContinue();
    }

    public boolean waitForDashboard(FeedPage feedPage) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(45)).until(ignored -> {
                if (isVisibleNow(loadingOverlay)) {
                    return false;
                }
                return feedPage.isAnyFeedItemVisible() || feedPage.hasNonBlankPageSource();
            });
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean typeInto(By field, String value) {
        return typeIntoFieldAtIndex(field, 0, value);
    }
}
