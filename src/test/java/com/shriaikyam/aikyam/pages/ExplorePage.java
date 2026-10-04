package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Collections;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import java.util.List;

public class ExplorePage extends BasePage {

    // Compose Text nodes carry no contentDescription (confirmed live, same as
    // HomePage's bottom nav) -- match on visible label text throughout, same
    // convention as the rest of this suite. Both "Map" and "List" are always present
    // together in the toggle row regardless of which one is active (only the
    // background colour changes), so neither locator alone tells you which view is
    // showing -- see isMapViewActive() below for how that's actually determined.
    private final By mapToggle = text("Map");
    private final By listToggle = text("List");

    // The only element on this screen with a real contentDescription -- its value
    // flips to "Add" instead of "Change" when location access is denied and no label
    // is set yet (see ExploreLocationInfo in ExploreScreen.kt). This suite always runs
    // with permissions auto-granted (BaseTest's setAutoGrantPermissions(true)), so
    // "Change" is the only state actually reachable here.
    private final By changeLocationIcon = accessibilityId("Change");
    private final By changeLocationDialogTitle = text("Change location");
    private final By noLocationsFoundText = text("No locations found");

    // Same generic-EditText convention SearchPage/ProfilePage already use for Compose
    // text fields (they carry no distinguishing contentDescription of their own). Only
    // one text field is ever visible/interactive at a time on this screen -- the top
    // temple search bar sits behind the dimmed scrim whenever the "Change location"
    // dialog is open, so this resolves unambiguously in both states.
    private final By textField = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\")"
    );

    // List-view-only content. If none of these are visible while the toggle row is,
    // the screen must be showing the native Google Map surface instead, which is a
    // native SDK view exposing no matching text/semantics of its own to assert on.
    private final By loadingTemplesText = text("Loading temples…");
    private final By noTemplesFoundText = text("No temples found");
    // TempleListRow carries no contentDescription and no fixed text: its second line is a
    // distance ("1.1 km") with a location fix and the temple's city ("Gudur") without one,
    // so every text-based row locator this page used before broke the moment GPS was
    // unavailable (confirmed live: same list, two different second lines). Match on shape
    // instead - a row is the only clickable child of a scrollable that holds two TextViews
    // (name + subtitle). The category tab strip is the screen's other scrollable and its
    // chips hold exactly one, so this stays 0 in Map view and N in List view (verified
    // against captured page sources from both).
    private final By templeRow = AppiumBy.xpath(
            "//android.view.View[@scrollable='true']/android.view.View[@clickable='true']"
                    + "[count(.//android.widget.TextView)>=2]"
    );

    // Reached after tapping a temple row -- confirms navigation actually landed on a
    // temple's profile rather than merely leaving the Explore screen. "Donate" is
    // deliberately not used as a locator here.
    private final By aboutTab = accessibilityId("About");
    private final By followAction = text("Follow");
    private final By followingAction = text("Following");

    public ExplorePage(IOSDriver driver) {
        super(driver);
    }

    public void waitUntilLoaded() {
        waitForAnyVisible(mapToggle, listToggle);
    }

    public boolean isExploreVisible() {
        return isAnyVisible(mapToggle) && isAnyVisible(listToggle);
    }

    public boolean isListContentVisible() {
        return isAnyVisible(loadingTemplesText, noTemplesFoundText, templeRow);
    }

    /** Map is the only view with no distinguishing text of its own on this screen --
     * inferred here by the absence of any List-only content while the toggle row
     * itself is present. */
    public boolean isMapViewActive() {
        return isExploreVisible() && !isListContentVisible();
    }

    public void switchToListView() {
        tapFirstVisible(listToggle);
    }

    public void switchToMapView() {
        tapFirstVisible(mapToggle);
    }

    public boolean waitForListContent() {
        try {
            waitForAnyVisible(loadingTemplesText, noTemplesFoundText, templeRow);
        } catch (RuntimeException ignored) {
            // Fall through to the plain visibility check below -- the wait is
            // best-effort, the real answer is whatever isListContentVisible() reports.
        }
        return isListContentVisible();
    }

    public boolean openChangeLocationDialog() {
        return tapFirstVisible(changeLocationIcon);
    }

    public boolean isChangeLocationDialogVisible() {
        return isAnyVisible(changeLocationDialogTitle);
    }

    public boolean searchLocation(String query) {
        return typeIntoFirstVisible(query, textField);
    }

    /** The location-search results list only populates after the keyboard's
     * search/submit action fires -- typing alone does not trigger live suggestions on
     * this screen (confirmed live: no dropdown appears until Enter is pressed). */
    public void submitSearch() {
        driver.pressKey(new KeyEvent(AndroidKey.ENTER));
    }

    public boolean isNoLocationsFoundVisible() {
        return isAnyVisible(noLocationsFoundText);
    }

    public boolean selectLocationSuggestion(String cityText) {
        return tapFirstVisible(textContains(cityText));
    }

    public boolean isLocationLabelUpdated(String cityText) {
        return isAnyVisible(textContains(cityText));
    }

    public boolean searchTemple(String query) {
        return typeIntoFirstVisible(query, textField);
    }

    /** ExploreSearchBar.kt's clear icon (ui_clear_search) empties the query and restores
     * the Map/List toggle row that a live query replaces - leaving a query typed strands
     * every later test that expects the default view (confirmed live: TC071 left
     * "Ganesha" typed, timing out every subsequent waitUntilLoaded() call). */
    public boolean clearTempleSearch() {
        if (!tapFirstVisible(accessibilityId("Clear search"))) {
            return false;
        }
        hideKeyboardIfShown();
        // Emptying the query is not enough. ExploreScreen keeps an opaque full-body
        // suggestions sheet (Trending/Popular) over the screen for as long as the field
        // holds focus, whatever it contains, and that sheet hides the Map/List toggle.
        // Its only dismiss is a tap on its own empty area, which clears focus - the sheet
        // carries no text or contentDescription to locate, hence the coordinate tap.
        // Guarded on the toggle still being hidden so the tap can't land on a temple row
        // or the map when the sheet isn't up (confirmed live: hideKeyboard alone leaves
        // the sheet showing, this tap restores the toggle).
        if (!isExploreVisible()) {
            Dimension screen = driver.manage().window().getSize();
            tapPoint(screen.getWidth() / 2, (int) (screen.getHeight() * 0.8));
        }
        return isExploreVisible();
    }

    /** The location chip is absent from some builds of this screen - assert nothing on it
     * without checking here first (see the Assume calls in ExploreTest). */
    public boolean isLocationChipVisible() {
        return isAnyVisible(changeLocationIcon);
    }

    /** Taps the first temple row in the currently-loaded List view. templeRow resolves the
     * row container itself (the node carrying the click handler), so a plain click is
     * enough - no coordinate tap on a child text node needed. */
    public boolean openFirstTempleFromList() {
        List<WebElement> rows = visibleElements(templeRow);
        if (rows.isEmpty()) {
            return false;
        }
        rows.get(0).click();
        return true;
    }

    public boolean isTempleProfileVisible() {
        return isAnyVisible(aboutTab, followAction, followingAction);
    }

    public boolean navigateBack() {
        try {
            driver.navigate().back();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    // ---------------------------------------------------------------------
    // Entity tab strip: Temples / Devotees / Posts / Deities / Events /
    // Books / Vedic Texts / Festivals / Pilgrimages.
    //
    // Verified live on the 1.6.1 dev build (Android 11): the strip scrolls
    // horizontally and only the first three tabs render real content - the
    // rest render a single "Coming Soon" label. Unlike the Events screen's
    // filter chips, whose clickable target is the icon ABOVE the label rather
    // than the label itself, these tabs are tappable on their label text -
    // confirmed by the search placeholder changing after each tap.
    //
    // Selection state is NOT exposed on these nodes: every View reports
    // selected="false", and the only selected="true" node on screen belongs to
    // the bottom navigation bar. Selection is therefore asserted through the
    // content a tab owns - its search placeholder, or the Coming Soon label -
    // which is observable and is the behaviour a user actually depends on.
    // ---------------------------------------------------------------------

    private final By templesEntityTab = text("Temples");
    private final By devoteesEntityTab = text("Devotees");
    private final By postsEntityTab = text("Posts");

    private final By templeSearchPlaceholder = textContains("Search temples");
    private final By devoteeSearchPlaceholder = textContains("Search devotees");
    private final By postSearchPlaceholder = textContains("Search posts");
    private final By comingSoonLabel = text("Coming Soon");
    /** Every tab label in the strip, in order. Used as swipe anchors: when the strip is
     * scrolled to its far end, Temples/Devotees/Posts are ALL off-screen, so anchoring the
     * swipe on those alone left the strip unrecoverable (confirmed live - six tests timed
     * out on an Explore screen showing only Books/Vedic Texts/Festivals/Pilgrimages). */
    private final By[] allEntityTabs = new By[] {
            text("Temples"), text("Devotees"), text("Posts"), text("Deities"),
            text("Events"), text("Books"), text("Vedic Texts"), text("Festivals"),
            text("Pilgrimages")
    };

    /** The strip is present whenever at least the three content-bearing tabs are. */
    public boolean isEntityTabStripVisible() {
        return isAnyVisible(templesEntityTab)
                && isAnyVisible(devoteesEntityTab)
                && isAnyVisible(postsEntityTab);
    }

    public boolean isEntityTabVisible(String label) {
        return isAnyVisible(text(label));
    }

    /** Waits for the tab strip itself rather than the Map/List toggle. waitUntilLoaded()
     * can't be used by the strip tests: the toggle only exists under Temples, and the
     * selected tab survives across sessions (noReset=true), so entering Explore while a
     * Coming Soon tab is selected leaves that wait timing out on a screen that has in
     * fact loaded correctly. */
    public void waitUntilTabStripLoaded() {
        // Wait for ANY tab: the strip's scroll offset persists, so the three
        // content-bearing tabs cannot be assumed on-screen on entry.
        waitForAnyVisible(allEntityTabs);
        scrollStripToStart();
    }

    /** The strip's horizontal scroll offset survives across tests and app sessions
     * (noReset=true), so a test that scrolled out to Books leaves the next one opening on
     * a strip where Temples is off-screen. Sweeping back to the start makes every strip
     * test begin from the same known position instead of inheriting one. Unconditional
     * swipes rather than poll-per-swipe: checking visibility between each one cost ~4
     * minutes per test in the previous run for no added reliability. */
    public void scrollStripToStart() {
        for (int attempt = 0; attempt < 4; attempt++) {
            swipeStrip(true);
        }
    }

    /** Taps a tab by its label, scrolling the strip horizontally first when an earlier
     * selection has carried that tab off-screen.
     *
     * The strip scrolls sideways, so BasePage's scrollUntilVisible (which swipes
     * vertically) can never reveal a tab - confirmed live: selecting Deities carries
     * Temples off the left edge, and every later attempt to return to it failed until
     * this swiped horizontally instead. Both directions are tried because the wanted tab
     * may sit on either side of the current viewport. */
    public boolean selectEntityTab(String label) {
        By tab = text(label);
        if (!isVisibleNow(tab) && !scrollStripUntilVisible(tab)) {
            return false;
        }
        return tapFirstVisible(tab);
    }

    private boolean scrollStripUntilVisible(By tab) {
        // Back to a known start, then sweep forward. Two cheap unconditional sweeps beat
        // polling in both directions: the strip is short and the start is deterministic.
        scrollStripToStart();
        if (isVisibleNow(tab)) {
            return true;
        }
        for (int attempt = 0; attempt < 6; attempt++) {
            swipeStrip(false);
            if (isVisibleNow(tab)) {
                return true;
            }
        }
        return false;
    }

    /** Horizontal swipe pinned to the strip's own row. The y is taken from whichever tab
     * is currently on screen, so this keeps working if the strip ever moves vertically,
     * and it can never be mistaken for a swipe on the content below it. */
    private void swipeStrip(boolean towardEarlierTabs) {
        WebElement anchor = null;
        for (By candidate : allEntityTabs) {
            List<WebElement> found = visibleElements(candidate);
            if (!found.isEmpty()) {
                anchor = found.get(0);
                break;
            }
        }
        if (anchor == null) {
            return;
        }
        int y = anchor.getLocation().getY() + (anchor.getSize().getHeight() / 2);

        Dimension size = driver.manage().window().getSize();
        int startX = towardEarlierTabs ? (int) (size.getWidth() * 0.2) : (int) (size.getWidth() * 0.8);
        int endX = towardEarlierTabs ? (int) (size.getWidth() * 0.8) : (int) (size.getWidth() * 0.2);

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, y));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), endX, y));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(swipe));
    }

    public boolean isTempleSearchVisible() {
        return isAnyVisible(templeSearchPlaceholder);
    }

    public boolean isDevoteeSearchVisible() {
        return isAnyVisible(devoteeSearchPlaceholder);
    }

    public boolean isPostSearchVisible() {
        return isAnyVisible(postSearchPlaceholder);
    }

    public boolean isComingSoonVisible() {
        return isAnyVisible(comingSoonLabel);
    }

    // ---------------------------------------------------------------------
    // Search-journey helpers (SearchJourneyTest). Additive only.
    // ---------------------------------------------------------------------

    private WebElement searchField() {
        return waitForAnyVisible(textField);
    }

    /** The field's text as the user sees it (the placeholder while empty). */
    public String searchFieldText() {
        try {
            return searchField().getText();
        } catch (RuntimeException e) {
            return "";
        }
    }

    /** Types a query and VERIFIES the field holds exactly that text. Live finding:
     * typeIntoFirstVisible's adb fallback dropped characters on the Devotees tab ("Santhosh"
     * landed as "Sanh" and searched for that). Retries per-character with a short pause,
     * which the debounced field keeps up with. */
    public boolean typeQuery(String query) {
        try {
            WebElement field = searchField();
            field.click();
            waitUntilFocused(field);
            try {
                field.sendKeys(query);
            } catch (RuntimeException ignored) {
                // fall through to the verified retry loop
            }
            for (int attempt = 0; attempt < 3; attempt++) {
                if (query.equals(searchFieldText())) {
                    return true;
                }
                // wipe whatever landed, then type one character at a time
                driver.pressKey(new KeyEvent(AndroidKey.MOVE_END));
                for (int i = 0; i < 40; i++) {
                    driver.pressKey(new KeyEvent(AndroidKey.DEL));
                }
                for (char c : query.toCharArray()) {
                    com.shriaikyam.aikyam.utils.AdbBridge.typeText(String.valueOf(c));
                    pause(150);
                }
                pause(500);
            }
            return query.equals(searchFieldText());
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Fires the keyboard action (ENTER) with the keyboard still up - hiding the keyboard first
     * costs a BACK press that, on Explore, can leave the tab for Home (confirmed live). */
    public void submitQuery() {
        driver.pressKey(new KeyEvent(AndroidKey.ENTER));
    }

    /** Case-insensitive match of a TextView (never the search field itself) whose text
     * contains the fragment. */
    private By resultTextContaining(String fragment) {
        return By.xpath("//android.widget.TextView[contains(translate(@text,"
                + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'"
                + fragment.toLowerCase() + "')]");
    }

    /** Texts of all on-screen result labels containing the fragment (polls up to timeoutSec). */
    public List<String> waitForResultsContaining(String fragment, int timeoutSec) {
        long end = System.currentTimeMillis() + timeoutSec * 1000L;
        List<String> out = new java.util.ArrayList<>();
        do {
            out.clear();
            try {
                for (WebElement e : visibleElements(resultTextContaining(fragment))) {
                    out.add(e.getText());
                }
            } catch (RuntimeException ignored) {
                // stale during recomposition; poll again
            }
            if (!out.isEmpty()) {
                return out;
            }
            pause(500);
        } while (System.currentTimeMillis() < end);
        return out;
    }

    public boolean tapResultContaining(String fragment) {
        try {
            List<WebElement> found = visibleElements(resultTextContaining(fragment));
            if (found.isEmpty()) {
                return false;
            }
            tapElementCenter(found.get(0));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Polls for an exact on-screen text. */
    public boolean waitForExactText(String exact, int timeoutSec) {
        long end = System.currentTimeMillis() + timeoutSec * 1000L;
        do {
            if (!visibleElements(text(exact)).isEmpty()) {
                return true;
            }
            pause(500);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    public boolean isExactTextVisibleNow(String exact) {
        return !visibleElements(text(exact)).isEmpty();
    }

    /** Posts results render as a grid of clickable, text-less image tiles. Counts the large
     * (>= 250px square-ish) clickable tiles currently on screen. */
    public int postGridTileCount() {
        int n = 0;
        for (WebElement e : visibleElements(By.xpath(
                "//android.view.View[@clickable='true'][@text='' and (not(@content-desc) or @content-desc='')]"))) {
            try {
                org.openqa.selenium.Rectangle r = e.getRect();
                if (r.getWidth() >= 250 && r.getHeight() >= 250) {
                    n++;
                }
            } catch (RuntimeException ignored) {
                // stale
            }
        }
        return n;
    }

    /** Taps the search field while empty so the recents/trending sheet opens. */
    public boolean focusSearchField() {
        try {
            tapElementCenter(searchField());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public boolean tapClearAllRecents() {
        return tapFirstVisible(text("Clear all"));
    }

    public boolean waitUntilExactTextGone(String exact, int timeoutSec) {
        long end = System.currentTimeMillis() + timeoutSec * 1000L;
        do {
            if (visibleElements(text(exact)).isEmpty()) {
                return true;
            }
            pause(500);
        } while (System.currentTimeMillis() < end);
        return false;
    }

    /** Taps only the field's clear icon - no coordinate tap. clearTempleSearch() follows it
     * with a blind tap at 80% screen height whenever the Map/List toggle is hidden, which on
     * the Posts tab lands on a post tile (confirmed live: it opened a post). */
    public boolean tapClearSearchIcon() {
        return !visibleElements(accessibilityId("Clear search")).isEmpty()
                && tapFirstVisible(accessibilityId("Clear search"));
    }

    /** All non-empty TextView texts currently on screen, in order (the search field is an
     * EditText and is not included). */
    public List<String> visibleTexts() {
        List<String> out = new java.util.ArrayList<>();
        try {
            for (WebElement e : visibleElements(AppiumBy.className("android.widget.TextView"))) {
                String t = e.getText();
                if (t != null && !t.isEmpty()) {
                    out.add(t);
                }
            }
        } catch (RuntimeException ignored) {
            // stale during recomposition
        }
        return out;
    }
}
