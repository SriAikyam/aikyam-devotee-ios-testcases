package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Notification surfaces of the devotee app. On iOS that is only the Settings hub's
 * Notifications switch (SettingsView.settingsRowWithToggle). */
public class NotificationsPage extends BasePage {

    private final By settingsRowLabel = text("Notifications");
    private final By settingsSwitch = id("settings.notifications.toggle");
    private final By permissionDialogDeny = AppiumBy.iOSClassChain(
            "**/XCUIElementTypeAlert/**/XCUIElementTypeButton[`label == \"Don’t Allow\" OR label == \"Don't Allow\"`]");

    public NotificationsPage(IOSDriver driver) {
        super(driver);
    }

    /** iOS: no top bar has a notifications bell - FeedTopBar is create-post + avatar only
     * (see parity report). */
    public boolean isBellVisible() {
        return false;
    }

    /** iOS: no notifications bell to tap (see parity report). */
    public boolean tapBell() {
        return false;
    }

    /** iOS: there is no Notifications placeholder screen (see parity report). */
    public boolean isComingSoonShown() {
        return false;
    }

    public boolean scrollToSettingsRow() {
        return scrollUntilVisible(settingsSwitch, settingsRowLabel);
    }

    /** "true"/"false" - an XCUIElementTypeSwitch reports its state as value "1"/"0". */
    public String switchChecked() {
        String value = waitSwitch().getAttribute("value");
        if ("1".equals(value)) {
            return "true";
        }
        if ("0".equals(value)) {
            return "false";
        }
        return value;
    }

    private WebElement waitSwitch() {
        return new WebDriverWait(driver, Duration.ofSeconds(15))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> d.findElements(settingsSwitch).stream().findFirst().orElse(null));
    }

    /** Taps the switch itself, not its row: a SwiftUI Toggle row is one element whose
     * centre is the label area, which does not flip it. */
    public void tapSwitch() {
        WebElement toggle = waitSwitch();
        try {
            WebElement knob = toggle.findElement(AppiumBy.className("XCUIElementTypeSwitch"));
            knob.click();
        } catch (RuntimeException noInnerSwitch) {
            toggle.click();
        }
    }

    /** Waits until the switch reports the wanted state; denies (never grants) a system
     * permission dialog if one appears. Returns the final state.
     *
     * Note: with autoAcceptAlerts the notification prompt is accepted before this sees it;
     * if permission is denied instead, SettingsView flips the switch back off and opens
     * the system Settings app. */
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
                // Transient re-render; retry.
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
