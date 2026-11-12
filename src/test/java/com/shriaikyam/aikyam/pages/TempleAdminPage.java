package com.shriaikyam.aikyam.pages;

import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Temple account editing on TempleAccountHomeView (app PR #711), ported from the Android
 * suite's TempleAdminPage. Ids come first; the English label is the fallback.
 */
public class TempleAdminPage extends BasePage {

    private final By changeBanner = id("temple.banner.edit");
    private final By editAvatar = id("temple.avatar.edit");
    private final By editHistory = id("temple.history.edit");
    private final By historyField = id("temple.history.field");
    private final By historyCounter = id("temple.history.counter");
    private final By saveAction = id("temple.history.save");
    private final By historyTitle = text("Temple History");

    public TempleAdminPage(IOSDriver driver) {
        super(driver);
    }

    public boolean hasChangeBanner() {
        return isAnyVisible(changeBanner, accessibilityId("Change banner"));
    }

    public boolean hasEditAvatar() {
        return isAnyVisible(editAvatar, accessibilityId("Change profile photo"));
    }

    public boolean hasEditHistory() {
        return scrollUntilVisible(4, editHistory, accessibilityId("Edit Temple History"));
    }

    public boolean openEditHistory() {
        return hasEditHistory() && tapFirstVisible(editHistory, accessibilityId("Edit Temple History"));
    }

    public boolean isEditHistoryScreenOpen() {
        return isAnyVisible(historyField) && isAnyVisible(saveAction);
    }

    private WebElement field() {
        List<WebElement> fields = driver.findElements(historyField);
        return fields.isEmpty() ? null : fields.get(0);
    }

    public String historyText() {
        WebElement field = field();
        if (field == null) {
            return null;
        }
        String value = field.getAttribute("value");
        return value == null ? "" : value;
    }

    /** iOS formats the limit for the locale, e.g. "18 / 5,000". */
    public boolean isCounterShowing() {
        if (!isAnyVisible(historyCounter)) {
            return false;
        }
        String label = driver.findElement(historyCounter).getAttribute("label");
        return label != null && label.matches("\\d+ / 5[,.]?000");
    }

    /** Replaces the whole history with {@code text}. */
    public boolean replaceHistory(String text) {
        WebElement field = field();
        if (field == null) {
            return false;
        }
        try {
            field.click();
            waitUntilFocused(field);
            field.clear();
            // clear() selects all and deletes; whitespace-only text offers nothing to select,
            // so delete whatever is left key by key.
            String left = historyText();
            if (left != null && !left.isEmpty()) {
                field.sendKeys("\b".repeat(left.length()));
            }
            if (!text.isEmpty()) {
                field.sendKeys(text);
            }
            hideKeyboardIfShown();
            return text.equals(historyText());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean appendToHistory(String text) {
        WebElement field = field();
        if (field == null) {
            return false;
        }
        try {
            String before = historyText();
            field.click();
            waitUntilFocused(field);
            field.sendKeys(text);
            hideKeyboardIfShown();
            String after = historyText();
            return after != null && after.length() > (before == null ? 0 : before.length()) && after.contains(text);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isSaveEnabled() {
        List<WebElement> save = driver.findElements(saveAction);
        return !save.isEmpty() && save.get(0).isEnabled();
    }

    public boolean tapSave() {
        return isSaveEnabled() && tapFirstVisible(saveAction);
    }

    /** Leaves the editor without saving; true once the temple profile shows again. */
    public boolean leaveEditorWithoutSaving() {
        if (!isVisibleNow(historyField)) {
            return true;
        }
        navigateBack();
        return waitUntilGone(historyField);
    }

    public void closeEditorIfOpen() {
        if (isVisibleNow(historyField)) {
            leaveEditorWithoutSaving();
        }
    }

    public boolean isBackOnProfileWithHistory() {
        return isAnyVisible(historyTitle) && isAnyVisible(editHistory, accessibilityId("Edit Temple History"));
    }

    public boolean profileHistoryContains(String fragment) {
        try {
            return driver.getPageSource().contains(fragment);
        } catch (RuntimeException unavailable) {
            return false;
        }
    }
}
