package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The account switcher at the top of PriestSettingsView (app PR #710 / issue #658): the
 * personal account, then every temple the user can act as, from priest/me and
 * users/profiles. Rows carry `priest.settings.account.personal` and
 * `priest.settings.account.temple.<templeId>`.
 *
 * An account that can use temple accounts opens this screen from the avatar instead of the
 * devotee Settings sheet that {@link SettingsPage#switchToAccountByName} drives.
 */
public class TempleAccountsPage extends BasePage {

    private static final String TEMPLE_ROW_PREFIX = "priest.settings.account.temple.";

    private final By personalRow = id("priest.settings.account.personal");
    private final By templeRows = AppiumBy.iOSNsPredicateString(
            "name BEGINSWITH " + predicateLiteral(TEMPLE_ROW_PREFIX));

    private final By accountButton = id("header.account");

    // iOS 26+ draws the native TabView bar, whose buttons carry the tab title, not the
    // `tab.*` ids the custom bar sets; scoped to the tab bar so no other text matches.
    private final By[] templeTab = tab("tab.temple", "Temple");
    private final By[] ordersTab = tab("tab.orders", "Orders");
    private final By[] homeTab = tab("tab.home", "Home");
    private final By[] exploreTab = tab("tab.explore", "Explore");
    private final By[] musicTab = tab("tab.music", "Music");

    public TempleAccountsPage(IOSDriver driver) {
        super(driver);
    }

    private By[] tab(String identifier, String title) {
        return new By[] {
                id(identifier),
                AppiumBy.iOSClassChain("**/XCUIElementTypeTabBar/**/XCUIElementTypeButton[`label == "
                        + predicateLiteral(title) + "`]")
        };
    }

    /**
     * Opens Settings from the top bar's account button. Hidden tabs keep their own top bars
     * mounted, so several `header.account` buttons report visible (and none reports
     * hittable); a hidden one sits under the status bar, which is the one
     * ProfilePage.openSettingsHub taps from a temple account. The lowest one is in the real
     * top bar, and a touch there lands on whichever view is on top: the visible one.
     */
    public boolean openSettings() {
        WebElement lowest = null;
        for (WebElement button : driver.findElements(accountButton)) {
            if (lowest == null || button.getLocation().getY() > lowest.getLocation().getY()) {
                lowest = button;
            }
        }
        if (lowest == null) {
            return false;
        }
        tapElementCenter(lowest);
        return isSwitcherVisible();
    }

    public boolean isSwitcherVisible() {
        return isAnyVisible(personalRow);
    }

    /** Temple ids of every temple row, in screen order. */
    public List<String> templeRowIds() {
        return driver.findElements(templeRows).stream()
                .map(row -> row.getAttribute("name"))
                .map(name -> name.substring(TEMPLE_ROW_PREFIX.length()))
                .collect(Collectors.toList());
    }

    /**
     * Switches to the temple and lands on its home. Tapping the row of the account that is
     * already active does nothing, so when the switcher is still showing afterwards, Back
     * returns to that temple's home.
     */
    public boolean switchToTemple(String templeName) {
        By row = templeRow(templeName);
        if (!(scrollUntilVisible(3, row) && tapFirstVisible(row))) {
            return false;
        }
        if (isAnyVisible(templeTab)) {
            return true;
        }
        if (isVisibleNow(personalRow)) {
            navigateBack();
        }
        return isAnyVisible(templeTab);
    }

    private By templeRow(String templeName) {
        return AppiumBy.iOSNsPredicateString("name BEGINSWITH " + predicateLiteral(TEMPLE_ROW_PREFIX)
                + " AND label CONTAINS " + predicateLiteral(templeName));
    }

    public boolean switchToPersonal() {
        return scrollBackUntilVisible(3, personalRow) && tapFirstVisible(personalRow);
    }

    public boolean isTempleHomeShown() {
        return isAnyVisible(templeTab);
    }

    public void openTempleTab() {
        tapFirstVisible(templeTab);
    }

    public boolean isDevoteeHomeShown() {
        return isAnyVisible(homeTab) && isAnyVisible(exploreTab) && isAnyVisible(musicTab);
    }

    public boolean isTempleOnlyNavShown() {
        return isVisibleNow(templeTab) || isVisibleNow(ordersTab);
    }

    /** The temple home names the active temple in its header. */
    public boolean isTempleNameShown(String templeName) {
        return isAnyVisible(text(templeName), textContains(templeName));
    }
}
