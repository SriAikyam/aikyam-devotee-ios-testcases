package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class ExplorePage extends BasePage {

    // Map/List segments of ExploreView.mapListToggle. Both are always present on the Temples
    // tab; the active one carries the isSelected trait (see isMapViewActive()).
    private final By mapToggle = id("explore.toggle.map");
    private final By listToggle = id("explore.toggle.list");

    // Pencil button in ExploreView.locationInfoPill - only rendered once location resolution
    // has landed (viewModel.shouldShowLocationPill).
    private final By changeLocationIcon = id("explore.location.change");
    private final By locationLabel = id("explore.location.label");
    // ManualLocationDialog: a NavigationStack sheet titled "Change location".
    private final By changeLocationDialogTitle = text("Change location");
    private final By locationSearchField = id("explore.location.search");
    // iOS copy is "No matches found" (Android: "No locations found").
    private final By noLocationsFoundText = id("explore.location.empty");

    // The Explore search capsule; one field whose placeholder changes per tab.
    private final By searchField = id("explore.search");

    // List-view-only content (ExploreView.templesListContent).
    private final By listLoading = id("explore.list.loading");
    private final By listEmpty = id("explore.list.empty");
    private final By listError = id("explore.list.error");
    // The name Text of each ExploreTempleRow (temples and pandals); its label is the title,
    // and a tap on it lands on the row's own tap gesture.
    private final By templeRow = id("explore.temple.name");

    // Reached after tapping a temple row. A PANDAL result opens PandalDetailsView instead.
    private final By aboutTab = id("temple.tab.about");
    private final By followAction = id("temple.follow");
    private final By pandalName = id("pandal.name");

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
        return isAnyVisible(listLoading, listEmpty, listError, templeRow);
    }

    /** The selected segment carries the isSelected trait, which XCUITest reports as
     * selected=true. Falls back to "no List-only content on screen" if neither segment
     * reports a selection. */
    public boolean isMapViewActive() {
        if (!isExploreVisible()) {
            return false;
        }
        if (isSelected(mapToggle)) {
            return true;
        }
        if (isSelected(listToggle)) {
            return false;
        }
        return !isListContentVisible();
    }

    private boolean isSelected(By locator) {
        try {
            List<WebElement> found = visibleElements(locator);
            return !found.isEmpty() && "true".equals(found.get(0).getAttribute("selected"));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public void switchToListView() {
        tapFirstVisible(listToggle);
    }

    public void switchToMapView() {
        tapFirstVisible(mapToggle);
    }

    public boolean waitForListContent() {
        try {
            waitForAnyVisible(listLoading, listEmpty, listError, templeRow);
        } catch (RuntimeException ignored) {
            // Best-effort wait; the real answer is whatever isListContentVisible() reports.
        }
        return isListContentVisible();
    }

    public boolean openChangeLocationDialog() {
        return tapFirstVisible(changeLocationIcon);
    }

    public boolean isChangeLocationDialogVisible() {
        return isAnyVisible(changeLocationDialogTitle, locationSearchField);
    }

    public boolean searchLocation(String query) {
        return typeIntoFirstVisible(query, locationSearchField);
    }

    /** ManualLocationDialog suggests as-you-type (MKLocalSearchCompleter), so Return only
     * submits/dismisses the keyboard; kept so callers written for Android still work. */
    public void submitSearch() {
        pressReturn(locationSearchField);
    }

    public boolean isNoLocationsFoundVisible() {
        return isAnyVisible(noLocationsFoundText);
    }

    public boolean selectLocationSuggestion(String cityText) {
        return tapFirstVisible(withIdLabelContaining("explore.location.suggestion", cityText));
    }

    public boolean isLocationLabelUpdated(String cityText) {
        return isAnyVisible(withIdLabelContaining("explore.location.label", cityText));
    }

    public boolean searchTemple(String query) {
        return typeIntoFirstVisible(query, searchField);
    }

    /** iOS has no clear icon in the Explore search capsule, so the field is emptied directly.
     * The Map/List toggle stays on the Temples tab whatever the query, so no suggestions
     * sheet needs dismissing afterwards. Returns false when there was no query to clear. */
    public boolean clearTempleSearch() {
        if (!clearQueryIfAny()) {
            return false;
        }
        return isExploreVisible();
    }

    public boolean isLocationChipVisible() {
        return isAnyVisible(changeLocationIcon, locationLabel);
    }

    /** Taps the first temple row in the currently-loaded List view. */
    public boolean openFirstTempleFromList() {
        List<WebElement> rows = visibleElements(templeRow);
        if (rows.isEmpty()) {
            return false;
        }
        rows.get(0).click();
        return true;
    }

    public boolean isTempleProfileVisible() {
        return isAnyVisible(aboutTab, followAction, pandalName);
    }

    @Override
    public boolean navigateBack() {
        try {
            return super.navigateBack();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    // ---------------------------------------------------------------------
    // Entity tab strip (ExploreCategoryTabs): Temples / Devotees / Posts / Deities /
    // Events / Books / Vedic Texts / Festivals / Pilgrimages - ExploreTab.allCases.
    // Only the first three have a results panel; the rest render a "coming soon" label.
    // Each tab's id is "explore.tab." + its case name lowercased (vedicTexts -> vedictexts).
    // ---------------------------------------------------------------------

    private static final String[] ENTITY_TAB_LABELS = {
            "Temples", "Devotees", "Posts", "Deities", "Events", "Books", "Vedic Texts",
            "Festivals", "Pilgrimages"
    };

    private By entityTab(String label) {
        return id("explore.tab." + label.toLowerCase().replace(" ", ""));
    }

    private final By templesEntityTab = entityTab("Temples");
    private final By devoteesEntityTab = entityTab("Devotees");
    private final By postsEntityTab = entityTab("Posts");
    private final By comingSoonLabel = id("explore.comingsoon");

    /** Every tab in the strip, in order. Used as swipe anchors: when the strip is scrolled to
     * its far end, Temples/Devotees/Posts are all off-screen. */
    private final By[] allEntityTabs = java.util.Arrays.stream(ENTITY_TAB_LABELS)
            .map(this::entityTab)
            .toArray(By[]::new);

    /** The strip is present whenever at least the three content-bearing tabs are. */
    public boolean isEntityTabStripVisible() {
        return isAnyVisible(templesEntityTab)
                && isAnyVisible(devoteesEntityTab)
                && isAnyVisible(postsEntityTab);
    }

    public boolean isEntityTabVisible(String label) {
        return isAnyVisible(entityTab(label));
    }

    /** Waits for the tab strip itself rather than the Map/List toggle, which only exists
     * under Temples - the selected tab can survive across sessions (noReset). */
    public void waitUntilTabStripLoaded() {
        waitForAnyVisible(allEntityTabs);
        scrollStripToStart();
    }

    /** The strip's horizontal offset can carry over from an earlier test; sweeping back to
     * the start makes every strip test begin from the same position. */
    public void scrollStripToStart() {
        for (int attempt = 0; attempt < 4; attempt++) {
            swipeStrip(true);
        }
    }

    /** Taps a tab, scrolling the strip horizontally first when the tab is off-screen -
     * BasePage's scroll helpers only swipe vertically. */
    public boolean selectEntityTab(String label) {
        By tab = entityTab(label);
        if (!isVisibleNow(tab) && !scrollStripUntilVisible(tab)) {
            return false;
        }
        return tapFirstVisible(tab);
    }

    private boolean scrollStripUntilVisible(By tab) {
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

    /** Horizontal swipe pinned to the strip's own row, taking y from whichever tab is on
     * screen so it can never be mistaken for a swipe on the content below it. */
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

    // Placeholders per ExploreView.searchPlaceholder.
    public boolean isTempleSearchVisible() {
        return isSearchPlaceholder("Search temples");
    }

    public boolean isDevoteeSearchVisible() {
        return isSearchPlaceholder("Search devotees");
    }

    public boolean isPostSearchVisible() {
        return isSearchPlaceholder("Search posts");
    }

    /** The field keeps its placeholder in `placeholderValue` whether or not a query is typed;
     * `value` also reports it while the field is empty. */
    private boolean isSearchPlaceholder(String fragment) {
        try {
            return new WebDriverWait(driver, SHORT_TIMEOUT)
                    .ignoring(StaleElementReferenceException.class)
                    .until(ignored -> visibleElements(searchField).stream().anyMatch(field ->
                            contains(field.getAttribute("placeholderValue"), fragment)
                                    || contains(field.getAttribute("value"), fragment)));
        } catch (RuntimeException notShown) {
            return false;
        }
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.contains(needle);
    }

    public boolean isComingSoonVisible() {
        return isAnyVisible(comingSoonLabel);
    }

    // ---------------------------------------------------------------------
    // Search-journey helpers (SearchJourneyTest).
    // ---------------------------------------------------------------------

    private WebElement searchField() {
        return waitForAnyVisible(searchField);
    }

    /** The field's text as the user sees it (the placeholder while empty). */
    public String searchFieldText() {
        try {
            String value = searchField().getAttribute("value");
            return value == null ? "" : value;
        } catch (RuntimeException e) {
            return "";
        }
    }

    /** Types a query and verifies the field holds exactly that text, retrying from an empty
     * field when a keystroke was lost to the field still animating in. */
    public boolean typeQuery(String query) {
        try {
            WebElement field = searchField();
            field.click();
            waitUntilKeyboardShown();
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    field.clear();
                } catch (RuntimeException ignored) {
                    // Nothing to clear.
                }
                field.sendKeys(query);
                pause(500);
                if (query.equals(searchFieldText())) {
                    return true;
                }
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

    /** Presses the keyboard's Search key, firing the field's onSubmit (records the query in
     * the local search history and unfocuses the field). */
    public void submitQuery() {
        pressReturn(searchField);
    }

    private void pressReturn(By field) {
        try {
            List<WebElement> fields = visibleElements(field);
            if (!fields.isEmpty()) {
                fields.get(0).sendKeys("\n");
                return;
            }
        } catch (RuntimeException fallThrough) {
            // Try the keyboard's own key below.
        }
        List<WebElement> keys = driver.findElements(AppiumBy.iOSClassChain(
                "**/XCUIElementTypeKeyboard/**/XCUIElementTypeButton[`name IN {'search', 'Search', 'Return', 'return'}`]"));
        if (!keys.isEmpty()) {
            keys.get(0).click();
        }
    }

    /** Case-insensitive match of a text or button label (never the search field itself)
     * containing the fragment. Devotee rows are Buttons whose label combines name and handle. */
    private By resultTextContaining(String fragment) {
        return AppiumBy.iOSNsPredicateString(
                "type IN {'XCUIElementTypeStaticText', 'XCUIElementTypeButton'} AND label CONTAINS[c] "
                        + predicateLiteral(fragment));
    }

    private By withIdLabelContaining(String identifier, String fragment) {
        return AppiumBy.iOSNsPredicateString("name == " + predicateLiteral(identifier)
                + " AND label CONTAINS[c] " + predicateLiteral(fragment));
    }

    /** Texts of all on-screen result labels containing the fragment (polls up to timeoutSec). */
    public List<String> waitForResultsContaining(String fragment, int timeoutSec) {
        long end = System.currentTimeMillis() + timeoutSec * 1000L;
        List<String> out = new java.util.ArrayList<>();
        do {
            out.clear();
            try {
                for (WebElement e : visibleElements(resultTextContaining(fragment))) {
                    out.add(e.getAttribute("label"));
                }
            } catch (RuntimeException ignored) {
                // Stale mid-update; poll again.
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

    /** Posts results render as a 3-column grid of image tiles (ExplorePostsGrid). */
    public int postGridTileCount() {
        return visibleElements(id("explore.post.tile")).size();
    }

    /** Taps the search field. */
    public boolean focusSearchField() {
        try {
            tapElementCenter(searchField());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // iOS: Explore has no inline recent-searches sheet or "Clear all" - history lives on
    // Settings > Search History (see parity report, "History UI").
    public boolean tapClearAllRecents() {
        return false;
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

    /** iOS has no clear icon on the field; empties it directly instead. Returns false when no
     * query was typed. */
    public boolean tapClearSearchIcon() {
        return clearQueryIfAny();
    }

    private boolean clearQueryIfAny() {
        List<WebElement> fields = visibleElements(searchField);
        if (fields.isEmpty()) {
            return false;
        }
        WebElement field = fields.get(0);
        String value = field.getAttribute("value");
        String placeholder = field.getAttribute("placeholderValue");
        if (value == null || value.isEmpty() || value.equals(placeholder)) {
            return false;
        }
        field.clear();
        hideKeyboardIfShown();
        return true;
    }

    /** All non-empty static-text labels currently on screen, in order (the search field is
     * a TextField and is not included). */
    public List<String> visibleTexts() {
        List<String> out = new java.util.ArrayList<>();
        try {
            for (WebElement e : visibleElements(By.className("XCUIElementTypeStaticText"))) {
                String t = e.getAttribute("label");
                if (t != null && !t.isEmpty()) {
                    out.add(t);
                }
            }
        } catch (RuntimeException ignored) {
            // Stale mid-update.
        }
        return out;
    }
}
