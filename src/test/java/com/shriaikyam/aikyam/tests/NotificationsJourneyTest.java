package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.BasePage;
import com.shriaikyam.aikyam.pages.HomePage;
import com.shriaikyam.aikyam.pages.NotificationsPage;
import com.shriaikyam.aikyam.pages.ProfilePage;
import org.junit.Assert;
import org.junit.Test;

/**
 * Notifications journeys. No iOS top bar carries a notifications bell and there is no
 * Notifications screen, so the only live notification control is the Settings hub's
 * Notifications switch.
 */
public class NotificationsJourneyTest extends BaseTest {

    @Test
    public void TC231_Settings_Notifications_Switch_Is_Present_With_Definite_State() {
        NotificationsPage notifications = openSettingsNotifications();
        try {
            String state = notifications.switchChecked();
            Assert.assertTrue("Switch checked attribute must be true or false, was " + state,
                    "true".equals(state) || "false".equals(state));
        } finally {
            backToHome();
        }
    }

    @Test
    public void TC232_Settings_Notifications_Switch_Toggles_And_Is_Restored() {
        NotificationsPage notifications = openSettingsNotifications();
        String initial = notifications.switchChecked();
        boolean initialOn = "true".equals(initial);
        boolean toggled = false;
        try {
            notifications.tapSwitch();
            toggled = true;
            Assert.assertEquals("Switch should flip after tap", String.valueOf(!initialOn),
                    notifications.awaitSwitch(!initialOn));
            notifications.settle();
            Assert.assertEquals("Flipped state should persist after the save round-trip",
                    String.valueOf(!initialOn), notifications.switchChecked());
        } finally {
            try {
                if (toggled) {
                    notifications.settle();
                    if (!String.valueOf(initialOn).equals(notifications.switchChecked())) {
                        notifications.tapSwitch();
                    }
                    Assert.assertEquals("Switch must be restored to its initial state", String.valueOf(initialOn),
                            notifications.awaitSwitch(initialOn));
                    notifications.settle();
                    Assert.assertEquals("Restored state should persist", String.valueOf(initialOn),
                            notifications.switchChecked());
                }
            } finally {
                backToHome();
            }
        }
    }

    private NotificationsPage openSettingsNotifications() {
        Assert.assertTrue("Settings hub should open", new ProfilePage(driver).openSettingsHub());
        NotificationsPage notifications = new NotificationsPage(driver);
        Assert.assertTrue("Notifications row should be on the Settings hub", notifications.scrollToSettingsRow());
        return notifications;
    }

    private void backToHome() {
        HomePage home = new HomePage(driver);
        for (int i = 0; i < 3 && !home.isHomeTabVisible(); i++) {
            BasePage.navigateBack(driver);
        }
        home.tapHomeTab();
    }
}
