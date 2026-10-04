package com.shriaikyam.aikyam.pages;

import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class OnboardingPage extends BasePage {

    private final By backAction = accessibilityId("Back");
    private final By screenAnchor = text("Tell us about yourself");
    private final By usernameFieldLabel = textContains("Username");
    private final By dobFieldAnchor = text("Date of Birth *");
    private final By dobPlaceholder = text("DD/MM/YYYY");
    private final By maleOption = text("Male");
    private final By femaleOption = text("Female");
    private final By otherOption = text("Other");
    private final By continueAction = text("Continue");
    private final By loadingOverlay = text("Loading...");
    // Full Name and Username are the only genuinely typable fields on this screen (DOB
    // is read-only and opens a picker dialog instead); order on screen is Full Name
    // then Username, same index-based pattern used elsewhere in this framework.
    private final By editTextField = io.appium.java_client.AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\")"
    );
    // The DOB wheel-picker dialog defaults to 15 June 2000 (well over the 13-year-old
    // minimum) - accepting that default via "OK" is a reliable happy path. Actually
    // scrolling the wheel to a specific date is fragile via Appium swipes and is not
    // attempted here.
    private final By dobDialogOkAction = text("OK");
    private final By dobDialogCancelAction = text("Cancel");

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
        return typeIntoFieldAtIndex(0, name);
    }

    public boolean isUsernamePrefilled() {
        List<WebElement> fields = visibleElements(editTextField);
        if (fields.size() < 2) {
            return false;
        }
        String value = fields.get(1).getText();
        return value != null && !value.isBlank();
    }

    public boolean enterUsername(String username) {
        return typeIntoFieldAtIndex(1, username);
    }

    public boolean isUsernameFieldVisible() {
        return isAnyVisible(usernameFieldLabel);
    }

    public boolean openDobPicker() {
        return tapFirstVisible(dobFieldAnchor, dobPlaceholder);
    }

    /** Opens the DOB picker and accepts its default date (15 June 2000) via OK. */
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
        return tapFirstVisible(option);
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
        return tapFirstVisible(continueAction);
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
                if (isAnyVisible(loadingOverlay)) {
                    return false;
                }
                return feedPage.isAnyFeedItemVisible() || feedPage.hasNonBlankPageSource();
            });
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean typeIntoFieldAtIndex(int index, String value) {
        try {
            List<WebElement> fields = visibleElements(editTextField);
            if (fields.size() <= index) {
                return false;
            }

            WebElement field = fields.get(index);
            field.click();
            waitUntilFocused(field);
            try {
                field.clear();
            } catch (RuntimeException ignored) {
            }
            field.sendKeys(value);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
