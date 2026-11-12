package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.PriestPage;
import com.shriaikyam.aikyam.pages.ProfilePage;
import com.shriaikyam.aikyam.pages.SettingsPage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

/**
 * Temple accounts for a user linked to temples.
 *
 * TC368 covers iOS PR #710 (#658): the switcher lists every linked temple from users/profiles,
 * each once. TC369/TC370 cover iOS PR #711: the temple account can edit its avatar, banner and
 * history, and the history Save stays disabled until the text really changes.
 *
 * Needs a test account linked to at least one temple. Nothing is saved: the history edit is
 * abandoned through the sheet's back button, and @After switches back to the personal account.
 */
public class TempleAccountJourneyTest extends BaseTest {

    private ProfilePage profile;
    private SettingsPage settings;
    private PriestPage priest;

    @Before
    public void openSwitcher() {
        profile = new ProfilePage(driver);
        settings = new SettingsPage(driver);
        priest = new PriestPage(driver);
        Assert.assertTrue("Settings (account switcher) should open from the avatar", profile.openSettingsHub());
    }

    @After
    public void backToPersonalAccount() {
        if (priest != null && priest.isTempleBottomNavVisible()
                && profile.openSettingsHub()) {
            settings.switchToPersonalAccount();
        }
    }

    private void switchToFirstTemple() {
        List<String> temples = settings.listedTempleAccountIds();
        Assert.assertFalse("The test account should have at least one temple account", temples.isEmpty());
        Assert.assertTrue("Temple account should be selectable", settings.switchToTempleAccountById(temples.get(0)));
        Assert.assertTrue("Temple bottom navigation should appear", priest.isTempleBottomNavVisible());
    }

    @Test
    public void TC368_Switcher_Lists_Each_Linked_Temple_Once() {
        List<String> temples = settings.listedTempleAccountIds();

        Assert.assertFalse("Every linked temple should be listed as a temple account", temples.isEmpty());
        Assert.assertEquals("Each temple should be listed once: " + temples,
                temples.size(), new HashSet<>(temples).size());
    }

    @Test
    public void TC369_Temple_Profile_Shows_Edit_Controls() {
        switchToFirstTemple();

        Assert.assertTrue("Avatar, banner and history edit buttons should be shown on the temple home",
                priest.areProfileEditControlsVisible());
    }

    @Test
    public void TC370_Temple_History_Save_Needs_A_Change() {
        switchToFirstTemple();
        Assert.assertTrue("History editor should open", priest.openHistoryEditor());

        Assert.assertTrue("A character counter against the 5,000 limit should be shown",
                priest.historyCounterText().endsWith("/ 5,000"));
        Assert.assertFalse("Save should be disabled while the history is unchanged",
                priest.isHistorySaveEnabled());

        Assert.assertTrue("Typing into the history should work", priest.typeIntoHistory("x"));
        Assert.assertTrue("Save should enable once the history changes", priest.isHistorySaveEnabled());

        Assert.assertTrue("Editor should close without saving", priest.closeHistoryEditor());
    }
}
