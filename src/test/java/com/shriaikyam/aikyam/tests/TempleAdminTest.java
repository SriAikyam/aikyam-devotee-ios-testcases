package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.categories.P0;
import com.shriaikyam.aikyam.pages.PriestPage;
import com.shriaikyam.aikyam.pages.TempleAccountsPage;
import com.shriaikyam.aikyam.pages.TempleAdminPage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.function.BooleanSupplier;

/**
 * Temple account editing (app PR #711), ported from the Android suite's TempleAdminTest and
 * TempleAccountGapTest: the edit controls, the Temple History editor, and one real history
 * edit that is restored exactly afterwards. Every test switches to the temple account first
 * and back to the personal account at the end.
 *
 * Photo and banner uploads are not changed here: they need an image in the photo picker and
 * would leave the shared test temple with different pictures.
 */
public class TempleAdminTest extends BaseTest {

    private static final String MARKER = " QAEDIT";
    private static final String DISCARD_MARKER = "QADISCARD";

    private TempleAccountsPage accounts;
    private TempleAdminPage admin;

    @Before
    public void switchToTempleAccount() {
        accounts = new TempleAccountsPage(driver);
        admin = new TempleAdminPage(driver);
        Assert.assertTrue("Settings should open from the avatar", accounts.openSettings());
        Assert.assertTrue("The temple account should be selectable", accounts.switchToTemple(TempleAccountsTest.TEMPLE));
        Assert.assertTrue("The temple navigation should show", accounts.isTempleHomeShown());
        accounts.openTempleTab();
        Assert.assertTrue("The temple profile should show", new PriestPage(driver).isTempleProfileVisible());
    }

    @After
    public void switchBackToPersonalAccount() {
        try {
            admin.closeEditorIfOpen();
            if (accounts.openSettings()) {
                accounts.switchToPersonal();
            }
        } catch (RuntimeException ignored) {
            // Best effort; a failed switch back is reported by the next test's @Before.
        }
    }

    @Category(P0.class)
    @Test
    public void TC801_Verify_The_Temple_Profile_Shows_Its_Edit_Controls() {
        Assert.assertTrue("Change banner should be offered", admin.hasChangeBanner());
        Assert.assertTrue("Edit avatar should be offered", admin.hasEditAvatar());
        Assert.assertTrue("Edit Temple History should be offered", admin.hasEditHistory());
    }

    @Category(P0.class)
    @Test
    public void TC802_Verify_The_Temple_History_Editor_Opens_Prefilled_And_Back_Leaves_It_Unchanged() {
        Assert.assertTrue("Edit Temple History should open", admin.openEditHistory());
        Assert.assertTrue("The editor should show with Save", admin.isEditHistoryScreenOpen());
        String shown = admin.historyText();
        Assert.assertNotNull("The history field should be readable", shown);
        Assert.assertFalse("The editor should be prefilled with the current history", shown.isBlank());
        Assert.assertTrue("The character counter should show", admin.isCounterShowing());
        Assert.assertTrue("Back should leave the editor", admin.leaveEditorWithoutSaving());
        Assert.assertTrue("Back should return to the temple profile", admin.isBackOnProfileWithHistory());
    }

    /** Changes the shared test temple's history and restores it exactly in finally. */
    @Test
    public void TC803_Verify_An_Edited_Temple_History_Is_Saved_And_Shown() {
        Assert.assertTrue("Edit Temple History should open", admin.openEditHistory());
        Assert.assertTrue("The editor should show", admin.isEditHistoryScreenOpen());
        String original = admin.historyText();
        Assert.assertNotNull("The current history should be readable", original);
        boolean changed = false;
        try {
            Assert.assertTrue("The history should accept the edited text", admin.replaceHistory(original + MARKER));
            Assert.assertTrue("Save should be tappable", admin.tapSave());
            changed = true;
            Assert.assertTrue("The profile should be showing again", waitUntil(admin::isBackOnProfileWithHistory));
            Assert.assertTrue("The edit should show on the profile", waitUntil(() -> admin.profileHistoryContains(MARKER.trim())));
        } finally {
            if (changed) {
                restoreHistory(original);
            }
        }
    }

    /**
     * Ported from Android TC1803, stricter on iOS: Save also stays disabled while the history
     * is unchanged (re-saving it sent the same text again and toasted "updated").
     */
    @Category(P0.class)
    @Test
    public void TC1803_Verify_Temple_History_Save_Is_Disabled_Until_The_History_Really_Changes() {
        Assert.assertTrue("Edit Temple History should open", admin.openEditHistory());
        Assert.assertTrue("The editor should show", admin.isEditHistoryScreenOpen());
        try {
            Assert.assertFalse("Save must be disabled while the history is unchanged", admin.isSaveEnabled());

            Assert.assertTrue("The history should clear", admin.replaceHistory(""));
            Assert.assertFalse("Save must be disabled when the history is empty", admin.isSaveEnabled());

            Assert.assertTrue("Spaces should be typed", admin.replaceHistory("   "));
            Assert.assertFalse("Save must be disabled for whitespace only", admin.isSaveEnabled());

            Assert.assertTrue("A real edit should be typed", admin.replaceHistory(DISCARD_MARKER));
            Assert.assertTrue("Save should be enabled once the history really changed", admin.isSaveEnabled());
        } finally {
            admin.leaveEditorWithoutSaving();
        }
    }

    @Category(P0.class)
    @Test
    public void TC1804_Verify_An_Unsaved_History_Edit_Is_Discarded_When_Leaving_With_Back() {
        Assert.assertTrue("Edit Temple History should open", admin.openEditHistory());
        try {
            Assert.assertTrue("The editor should show", admin.isEditHistoryScreenOpen());
            String original = admin.historyText();
            Assert.assertNotNull("The current history should be readable", original);
            Assert.assertFalse("The history should not be empty to begin with", original.isBlank());

            Assert.assertTrue("The edit should be typed", admin.appendToHistory(DISCARD_MARKER));
            Assert.assertTrue("Back should leave the editor", admin.leaveEditorWithoutSaving());

            Assert.assertTrue("The profile should be showing again", admin.isBackOnProfileWithHistory());
            Assert.assertFalse("The unsaved edit must not appear on the profile", admin.profileHistoryContains(DISCARD_MARKER));
            Assert.assertTrue("The editor should reopen", admin.openEditHistory());
            Assert.assertEquals("The editor must show the original history", original, admin.historyText());
        } finally {
            admin.closeEditorIfOpen();
        }
    }

    /** Puts the history back exactly as it was by replacing it whole. */
    private void restoreHistory(String original) {
        Assert.assertTrue("Edit Temple History should reopen to restore it", admin.openEditHistory());
        Assert.assertTrue("The editor should show", admin.isEditHistoryScreenOpen());
        Assert.assertTrue("The original history should be retyped exactly", admin.replaceHistory(original));
        Assert.assertTrue("Save should be tappable", admin.tapSave());
        Assert.assertTrue("The profile should be showing again", waitUntil(admin::isBackOnProfileWithHistory));
        Assert.assertFalse("The marker should be gone", admin.profileHistoryContains(MARKER.trim()));
    }

    private static boolean waitUntil(BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
