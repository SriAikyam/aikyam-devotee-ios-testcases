package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.categories.P0;
import com.shriaikyam.aikyam.pages.TempleAccountsPage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashSet;
import java.util.List;

/**
 * The account switcher lists every temple the user can act as (app PR #710, issue #658):
 * temples from priest/me and users/profiles, each once. Every test starts and ends on the
 * personal account, so later classes find the session as they expect.
 *
 * The temple is {@code -Dappium.templeAccount} (default: the one PriestTest uses).
 */
public class TempleAccountsTest extends BaseTest {

    static final String TEMPLE = System.getProperty("appium.templeAccount", "Chennakesava Temple");

    private TempleAccountsPage accounts;

    @Before
    public void openSwitcher() {
        accounts = new TempleAccountsPage(driver);
        Assert.assertTrue("Settings should open from the avatar", accounts.openSettings());
        Assert.assertTrue("The account switcher should show", accounts.isSwitcherVisible());
    }

    @After
    public void backToPersonalAccount() {
        try {
            if (accounts.isTempleOnlyNavShown() && accounts.openSettings()) {
                accounts.switchToPersonal();
            }
        } catch (RuntimeException ignored) {
            // Best effort; a failed switch back is reported by the next test's @Before.
        }
    }

    @Category(P0.class)
    @Test
    public void TC1805_Verify_The_Switcher_Lists_Every_Linked_Temple_Once() {
        List<String> templeIds = accounts.templeRowIds();
        Assert.assertFalse("At least one temple account should be listed", templeIds.isEmpty());
        Assert.assertEquals("Each temple should be listed once", new HashSet<>(templeIds).size(), templeIds.size());
    }

    @Category(P0.class)
    @Test
    public void TC1806_Verify_Switching_Into_A_Temple_Opens_Its_Temple_Home() {
        Assert.assertTrue("The temple account should be selectable", accounts.switchToTemple(TEMPLE));
        Assert.assertTrue("The temple navigation should show", accounts.isTempleHomeShown());

        Assert.assertTrue("The temple home should show that temple's name", accounts.isTempleNameShown(TEMPLE));
    }

    /** Ported from the Android suite's TempleAccountGapTest. */
    @Category(P0.class)
    @Test
    public void TC1801_Verify_Switching_Back_To_The_Personal_Account_Restores_The_Devotee_Home_And_Tabs() {
        Assert.assertTrue("The temple account should be selectable", accounts.switchToTemple(TEMPLE));
        Assert.assertTrue("The temple navigation should show", accounts.isTempleHomeShown());

        Assert.assertTrue("Settings should open from the temple account", accounts.openSettings());
        Assert.assertTrue("The personal account should be selectable", accounts.switchToPersonal());

        Assert.assertTrue("The devotee home and its Home/Explore/Music tabs should show", accounts.isDevoteeHomeShown());
        Assert.assertFalse("The temple-only tabs must be gone", accounts.isTempleOnlyNavShown());
    }
}
