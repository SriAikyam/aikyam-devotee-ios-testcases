package com.shriaikyam.aikyam.pages;

import com.shriaikyam.aikyam.utils.WaitUtils;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

public class HomePage extends BasePage {

    // Bottom nav labels are plain Compose Text nodes with no contentDescription
    // (content-desc is empty in the accessibility tree), so accessibility-id
    // locators never match. Match on the visible label text instead.
    private final By homeBottomNav = byLabel("Home");
    private final By sevasBottomNav = byLabel("Sevas");
    private final By exploreBottomNav = byLabel("Explore");
    private final By musicBottomNav = byLabel("Music");
    private final By appsBottomNav = byLabel("Apps");
    // Confirmed live: the bottom nav's fifth slot currently reads "Events", not
    // "Sevas" - Book Seva is on hold by a deliberate product decision (see
    // BookSevaTest's class-level @Ignore), and this is what occupies that slot in
    // its place today. Added alongside sevasBottomNav rather than replacing it, same
    // as the rest of this codebase keeps on-hold locators intact instead of deleting
    // them - re-enable is a one-line swap back if Sevas returns to this slot.
    private final By eventsBottomNav = byLabel("Events");

    public HomePage(AndroidDriver driver) {
        super(driver);
    }

    /** Confirmed live: the "Home" bottom-nav label is visible on every tab's own
     * screen (it's part of the persistent bottom bar), so waiting for it alone never
     * proves we're actually ON the Home tab - a prior test leaving the app on Explore/
     * Music/Sevas/Apps would satisfy this wait just as well, silently stranding every
     * later test that assumes a fresh Home starting point (confirmed live: this
     * exact scenario made a tab-switch test land on the wrong destination). Actively
     * tap Home rather than just waiting for the bar to exist - re-tapping an
     * already-active Home tab is a harmless no-op/scroll-to-top, matching the same
     * "reselect" convention used elsewhere in this app. */
    public void waitUntilLoaded() {
        WaitUtils.waitForVisibility(driver, homeBottomNav, DEFAULT_TIMEOUT);
        tapHomeTab();
    }

    public boolean isHomeBottomNavigationVisible() {
        return bottomNavigationLocators().stream()
                .allMatch(locator -> WaitUtils.isVisible(driver, locator, DEFAULT_TIMEOUT));
    }

    public boolean isHomeTabVisible() {
        return isVisible(homeBottomNav);
    }

    public boolean isExploreTabVisible() {
        return isVisible(exploreBottomNav);
    }

    public boolean isMusicTabVisible() {
        return isVisible(musicBottomNav);
    }

    public boolean isSevasTabVisible() {
        return isVisible(sevasBottomNav);
    }

    public boolean isAppsTabVisible() {
        return isVisible(appsBottomNav);
    }

    public boolean isEventsTabVisible() {
        return isVisible(eventsBottomNav);
    }

    public void tapHomeTab() {
        tap(homeBottomNav);
    }

    public void tapExploreTab() {
        tap(exploreBottomNav);
    }

    public void tapMusicTab() {
        tap(musicBottomNav);
    }

    public void tapSevasTab() {
        tap(sevasBottomNav);
    }

    public void tapAppsTab() {
        tap(appsBottomNav);
    }

    public void tapEventsTab() {
        tap(eventsBottomNav);
    }

    public int visibleBottomNavigationItemCount() {
        int visibleCount = 0;
        for (By locator : bottomNavigationLocators()) {
            if (isVisible(locator)) {
                visibleCount++;
            }
        }
        return visibleCount;
    }

    public boolean hasNonBlankPageSource() {
        String pageSource = driver.getPageSource();
        return pageSource != null && !pageSource.isBlank();
    }

    // eventsBottomNav, not sevasBottomNav: this is the set of tabs actually expected
    // on screen today. sevasBottomNav stays defined above (and still used directly by
    // isSevasTabVisible()/tapSevasTab() for whenever Book Seva returns) but is
    // deliberately excluded from "what should currently be visible" - confirmed live,
    // its slot is occupied by Events now, and this list is what TC012's "all 5 items
    // visible" check and isHomeBottomNavigationVisible() actually assert against.
    private List<By> bottomNavigationLocators() {
        return List.of(
                homeBottomNav,
                eventsBottomNav,
                exploreBottomNav,
                musicBottomNav,
                appsBottomNav
        );
    }

    private boolean isVisible(By locator) {
        return WaitUtils.isVisible(driver, locator, DEFAULT_TIMEOUT);
    }

    /** A real touch on the clickable nav item, not click() on its label. The labels are
     * non-clickable Compose Text nodes sitting in the last ~30px of the screen; see
     * BasePage.clickableAncestorOf for why either of those alone breaks the tap.
     * Confirmed live: TC014 stayed on Apps through two Home taps, and once the dispatch
     * was fixed but the tap still landed on the label, TC011/TC013 stayed on Home. */
    private void tap(By locator) {
        ensureBottomNavigationReachable();
        WebElement label = WaitUtils.waitForVisibility(driver, locator, DEFAULT_TIMEOUT);
        tapElementCenter(clickableAncestorOf(label));
        WaitUtils.waitForVisibility(driver, homeBottomNav, DEFAULT_TIMEOUT);
    }

    /** A prior test failing mid-flow can leave the app on a screen with no bottom nav
     * at all (a detail screen, a dialog, a bottom sheet) - without this, the tap()
     * below throws a raw TimeoutException on the target tab locator that looks like a
     * broken locator rather than leftover state from an earlier test. Back out a few
     * times first; each check uses a short timeout so a genuinely broken app doesn't
     * stall the whole retry loop for minutes. */
    private void ensureBottomNavigationReachable() {
        Duration shortCheck = Duration.ofSeconds(2);
        for (int attempt = 0; attempt < 6 && !WaitUtils.isVisible(driver, homeBottomNav, shortCheck); attempt++) {
            try {
                driver.navigate().back();
            } catch (RuntimeException ignored) {
                break;
            }
        }
    }

    /** Matches the label, not the clickable nav item wrapping it - tap() walks up via
     * BasePage.clickableAncestorOf. Locating the item directly with
     * "//*[@clickable='true'][.//*[@text='...']]" reads better and was tried, but
     * UiAutomator2's XPath engine never matches that nested-predicate form: all 15
     * HomeTest cases timed out on it and the run took twice as long. */
    private static By byLabel(String label) {
        return AppiumBy.androidUIAutomator("new UiSelector().text(\"" + label + "\")");
    }
}
