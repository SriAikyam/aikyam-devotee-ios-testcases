package com.shriaikyam.aikyam.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Notification surfaces of the devotee app: the Home top-bar bell (Screen.Notifications entry),
 * the Notifications placeholder screen, and the Notifications switch on the Settings hub. */
public class NotificationsPage extends BasePage {

    private final By bell = accessibilityId("Notifications");
    private final By settingsRowLabel = text("Notifications");
    // The Compose Switch is a checkable+clickable sibling of the row's title Text.
    private final By settingsSwitch = By.xpath(
            "//*[@text='Notifications']/following-sibling::*[@checkable='true']");
    private final By permissionDialogDeny = By.xpath(
            "//*[@resource-id='com.android.permissioncontroller:id/permission_deny_button']");

    public NotificationsPage(AndroidDriver driver) {
        super(driver);
    }

    /** Bell on whatever top bar is showing right now. */
    public boolean isBellVisible() {
        return isAnyVisible(bell);
    }

    public boolean tapBell() {
        return tapFirstVisible(bell);
    }

    public boolean isComingSoonShown() {
        return isAnyVisible(text("Coming Soon"));
    }

    public boolean scrollToSettingsRow() {
        return scrollUntilVisible(settingsRowLabel);
    }

    /** "true"/"false" of the switch's checked attribute. */
    public String switchChecked() {
        return waitSwitch().getAttribute("checked");
    }

    private WebElement waitSwitch() {
        return new WebDriverWait(driver, Duration.ofSeconds(15))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> d.findElements(settingsSwitch).stream().findFirst().orElse(null));
    }

    public void tapSwitch() {
        waitSwitch().click();
    }

    /** Waits until the switch reports the wanted state; denies (never grants) a system
     * permission dialog if one appears. Returns the final state. */
    public String awaitSwitch(boolean wanted) {
        long end = System.currentTimeMillis() + 15000;
        String state = null;
        while (System.currentTimeMillis() < end) {
            try {
                if (!driver.findElements(permissionDialogDeny).isEmpty()) {
                    driver.findElements(permissionDialogDeny).get(0).click();
                }
                state = switchChecked();
                if (state.equals(String.valueOf(wanted))) {
                    return state;
                }
            } catch (RuntimeException ignored) {
                // transient recomposition; retry
            }
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        return state;
    }

    /** The state must still hold after the save round-trip. */
    public void settle() {
        try { Thread.sleep(2500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
