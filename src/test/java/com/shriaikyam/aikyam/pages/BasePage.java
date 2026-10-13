package com.shriaikyam.aikyam.pages;

import com.shriaikyam.aikyam.utils.WaitUtils;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class BasePage {

    protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    protected static final Duration SHORT_TIMEOUT = Duration.ofSeconds(8);

    protected final IOSDriver driver;

    protected BasePage(IOSDriver driver) {
        this.driver = driver;
    }

    /** XCUITest resolves an accessibility id against the element's `name`, which is its
     * `.accessibilityIdentifier` when the view sets one and its label otherwise - so this
     * matches both the stable ids added for this suite and a plain label. */
    protected By accessibilityId(String value) {
        return AppiumBy.accessibilityId(value);
    }

    /** A stable `.accessibilityIdentifier` set in the SwiftUI view. Prefer this over any
     * text match: identifiers survive copy changes and the Kannada/Hindi locales. */
    protected By id(String identifier) {
        return AppiumBy.accessibilityId(identifier);
    }

    protected By textContains(String value) {
        String v = predicateLiteral(value);
        return AppiumBy.iOSNsPredicateString(
                "label CONTAINS " + v + " OR value CONTAINS " + v + " OR name CONTAINS " + v);
    }

    protected By text(String value) {
        return byExactText(value);
    }

    /** The iOS counterpart of UiSelector().text(): SwiftUI surfaces a Text or Button's
     * string as `label`, a TextField's content as `value`, and either as `name` when no
     * identifier is set - so an exact match has to look at all three. */
    public static By byExactText(String value) {
        String v = predicateLiteral(value);
        return AppiumBy.iOSNsPredicateString(
                "label == " + v + " OR value == " + v + " OR name == " + v);
    }

    public static String predicateLiteral(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    /** Polls for any of the given locators to be visible. A Compose screen actively
     * recomposing (e.g. right after a navigation tap) can invalidate an element
     * reference between findElements() and isDisplayed() - ignoring
     * StaleElementReferenceException here lets the wait simply retry on the next poll
     * instead of failing the whole call outright. */
    protected WebElement waitForAnyVisible(By... locators) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                .ignoring(StaleElementReferenceException.class)
                .until(ignored -> Arrays.stream(locators)
                        .flatMap(locator -> driver.findElements(locator).stream())
                        .filter(WebElement::isDisplayed)
                        .findFirst()
                        .orElse(null));
    }

    /** Waits for locators to no longer be displayed - used to let a navigation
     * transition actually settle before the caller checks what screen it landed on,
     * rather than racing a Compose animation that's still mid-flight. */
    protected boolean waitUntilGone(By... locators) {
        return waitUntilGone(SHORT_TIMEOUT, locators);
    }

    protected boolean waitUntilGone(Duration timeout, By... locators) {
        try {
            return new WebDriverWait(driver, timeout)
                    .until(ignored -> Arrays.stream(locators)
                            .flatMap(locator -> driver.findElements(locator).stream())
                            .noneMatch(WebElement::isDisplayed));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** True when ANY of the locators is displayed within SHORT_TIMEOUT.
     *
     * Was: Arrays.stream(locators).anyMatch(l -> WaitUtils.isVisible(driver, l, SHORT_TIMEOUT)) -
     * a SEQUENTIAL chain that spent the full SHORT_TIMEOUT on each locator in turn before
     * moving to the next. isCreatePostVisible() passes five locators, so a screen that
     * held none of them cost 5 x 8s = 40s to answer "no" - and it answers "no" on exactly
     * the unhappy paths where the suite is already slowest.
     *
     * Now: one bounded wait that polls every locator each iteration, the same shape
     * waitForAnyVisible already used. Worst case is 8s total rather than 8s per locator.
     * Return semantics and locator meaning are unchanged; ordering is irrelevant to an
     * "any" predicate.
     *
     * Behaviour note: an element that only appears AFTER SHORT_TIMEOUT but before the old
     * cumulative budget (e.g. at 20s with five locators) was previously caught and now is
     * not. That is the old behaviour being accidental rather than intended - a per-locator
     * timeout was never meant to grant later locators a longer deadline - but it is a real
     * difference, so this went in as its own measured change. */
    protected boolean isAnyVisible(By... locators) {
        if (locators.length == 0) {
            return false;
        }
        try {
            return new WebDriverWait(driver, SHORT_TIMEOUT)
                    .ignoring(StaleElementReferenceException.class)
                    .until(ignored -> Arrays.stream(locators)
                            .flatMap(locator -> driver.findElements(locator).stream())
                            .anyMatch(WebElement::isDisplayed));
        } catch (RuntimeException notVisible) {
            return false;
        }
    }

    /** "Is this on screen right now?" - for probes inside retry loops, where the caller
     * will look again anyway and a long wait is pure cost.
     *
     * isAnyVisible waits SHORT_TIMEOUT (8s), which is right for an assertion and wrong
     * here: the scroll helpers probe after every swipe, so a 12-swipe budget spent up to
     * 96 seconds waiting for something that was never going to appear, and
     * openProfile()'s six recovery iterations spent ~48s. That, not the swiping, is what
     * made ProfileTest take 45 minutes for 10 tests (confirmed from the Appium log:
     * repeated "no such element" for accessibility id Profile with the wait climbing
     * 3124ms, 3725ms, 4343ms...). */
    protected boolean isVisibleNow(By... locators) {
        return Arrays.stream(locators)
                .anyMatch(locator -> WaitUtils.isVisible(driver, locator, PROBE_TIMEOUT));
    }

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(2);

    protected boolean tapFirstVisible(By... locators) {
        try {
            waitForAnyVisible(locators).click();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    protected boolean typeIntoFirstVisible(String value, By... locators) {
        try {
            WebElement element = waitForAnyVisible(locators);
            element.click();
            waitUntilKeyboardShown();
            try {
                element.clear();
            } catch (RuntimeException ignored) {
                // Nothing to clear on an empty field.
            }
            element.sendKeys(value);
            if (valueLandedInField(element, value)) {
                return true;
            }
            // XCUITest types through the real keyboard, so a miss here is a focus race
            // (the field was still animating in), not an unsupported field - one clean
            // retry from an empty field covers it without concatenating old and new text.
            tapElementCenter(element);
            waitUntilKeyboardShown();
            try {
                element.clear();
            } catch (RuntimeException ignored) {
                // Nothing to clear on an empty field.
            }
            element.sendKeys(value);
            return valueLandedInField(element, value);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    protected boolean valueLandedInField(WebElement element, String value) {
        try {
            String actual = element.getAttribute("value");
            if (actual == null) {
                actual = element.getText();
            }
            return actual != null && normalizeWhitespace(actual).contains(normalizeWhitespace(value));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Some IME/adb-injected text ends up with non-breaking spaces (U+00A0) instead of
     * regular spaces, which look identical printed but fail a literal .contains() check
     * (a bug already seen once elsewhere in this project) - normalize before comparing. */
    private String normalizeWhitespace(String value) {
        return value.replace(' ', ' ');
    }

    /** Taps an element via a real coordinate-based touch gesture rather than
     * WebElement.click() - some Appium drivers dispatch click() as an accessibility
     * ACTION_CLICK rather than a genuine touch, which can leave a Compose text field
     * "focused" per its semantics without ever attaching a real IME input connection,
     * so keystrokes injected afterward (via adb or otherwise) have nowhere to land. */
    protected void tapElementCenter(WebElement element) {
        tapElementCenter(driver, element);
    }

    /** Static so the page objects that predate BasePage and cannot extend it - they carry
     * their own private copies of visibleElements/tapFirstVisible, which would collide -
     * still get the one implementation of this gesture rather than a second copy. */
    public static void tapElementCenter(IOSDriver driver, WebElement element) {
        Point location = element.getLocation();
        Dimension size = element.getSize();
        tapPoint(driver, location.getX() + size.getWidth() / 2, location.getY() + size.getHeight() / 2);
    }

    /** Android needed to walk up from a non-clickable Compose label to its clickable
     * parent. XCUITest already reports a SwiftUI Button (label included) as one element,
     * so the element is its own tap target. Kept so page objects ported from Android
     * call the same API on both platforms. */
    public static WebElement clickableAncestorOf(WebElement element) {
        return element;
    }

    /** Same real-touch gesture as {@link #tapElementCenter}, for the cases where the thing
     * to tap is a region rather than a node - a Compose scrim or dismiss-on-tap sheet
     * carries no text, contentDescription or class of its own to locate it by. */
    protected void tapPoint(int x, int y) {
        tapPoint(driver, x, y);
    }

    public static void tapPoint(IOSDriver driver, int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
    }

    /** iOS has no BACK key to fall back on, and hideKeyboard() fails on SwiftUI fields
     * that show no Return/Done key - so try the driver first, then the keyboard's own
     * dismiss keys. */
    protected void hideKeyboardIfShown() {
        hideKeyboardIfShown(driver);
    }

    public static void hideKeyboardIfShown(IOSDriver driver) {
        try {
            if (!driver.isKeyboardShown()) {
                return;
            }
            driver.hideKeyboard();
        } catch (RuntimeException driverCouldNotHide) {
            for (String key : new String[] {"Done", "Return", "return", "done"}) {
                List<WebElement> keys = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND label == " + predicateLiteral(key)));
                if (!keys.isEmpty()) {
                    try {
                        keys.get(0).click();
                        return;
                    } catch (RuntimeException ignored) {
                        // Try the next key.
                    }
                }
            }
        }
    }

    /** iOS exposes no reliable per-field "focused" attribute; the keyboard appearing is
     * the observable signal that the field took focus. */
    protected void waitUntilKeyboardShown() {
        try {
            new WebDriverWait(driver, SHORT_TIMEOUT).until(ignored -> driver.isKeyboardShown());
        } catch (RuntimeException ignored) {
            // Hardware keyboard connected in the simulator hides the soft keyboard; sendKeys
            // still works, and the value check after it decides.
        }
    }

    protected void waitUntilFocused(WebElement element) {
        waitUntilKeyboardShown();
    }

    /** iOS equivalent of the system BACK press used throughout the Android suite. In
     * order: the navigation bar's back button, an explicit close/cancel control, and
     * finally a downward swipe that dismisses a presented sheet. Returns false only when
     * none of them applied. */
    protected boolean navigateBack() {
        return navigateBack(driver);
    }

    public static boolean navigateBack(IOSDriver driver) {
        hideKeyboardIfShown(driver);
        List<WebElement> navBack = driver.findElements(AppiumBy.iOSClassChain(
                "**/XCUIElementTypeNavigationBar/XCUIElementTypeButton[1]"));
        if (!navBack.isEmpty() && tapIfDisplayed(navBack.get(0))) {
            return true;
        }
        for (String id : new String[] {"common.back", "common.close", "Back", "Close", "Cancel", "xmark", "chevron.left", "chevron.backward"}) {
            List<WebElement> controls = driver.findElements(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (name == " + predicateLiteral(id)
                            + " OR label == " + predicateLiteral(id) + ")"));
            for (WebElement control : controls) {
                if (tapIfDisplayed(control)) {
                    return true;
                }
            }
        }
        if (!isDisplayedNow(driver, "tab.home")) {
            Dimension size = driver.manage().window().getSize();
            swipe(driver, size.getWidth() / 2, (int) (size.getHeight() * 0.25),
                    (int) (size.getHeight() * 0.90));
        }
        return false;
    }

    private static boolean tapIfDisplayed(WebElement element) {
        try {
            if (element.isDisplayed()) {
                element.click();
                return true;
            }
        } catch (RuntimeException ignored) {
            // Gone between lookup and tap.
        }
        return false;
    }

    /** Types into the Nth currently-visible element matching the given locator (0 =
     * first in document order) - for screens with several same-class fields (e.g.
     * stacked OutlinedTextFields) that carry no distinguishing contentDescription of
     * their own. Same pattern already used ad hoc by ProfilePage/FamilyMembersPage/
     * OnboardingPage, generalized here so new pages can reuse it directly. */
    /** Deletes a pre-filled value one key at a time. Only for fields known to hold a real
     * value: Compose reports a placeholder through getText() the same way it reports a
     * value, so calling this on an empty field fires a DEL per placeholder character.
     * Needed because clear() does not work on Compose text fields - it reports success
     * and leaves the value in place, so the sendKeys that follows appends (confirmed
     * live: updating a family member to "QA 854573 Updated" stored
     * "QA 854573QA 854573 U", both values run together and cut at that field's 20-char
     * cap). Not folded into typeIntoFieldAtIndex: that helper is used across the suite
     * for empty fields, where this is pure overhead and cost 4 of 9 FamilyMembersTest
     * cases when it was. */
    protected void deleteExistingValue(WebElement field) {
        String existing = field.getAttribute("value");
        if (existing == null || existing.isEmpty()) {
            return;
        }
        // Unlike Compose, XCUITest's clear() really empties a UITextField/SwiftUI TextField.
        field.clear();
    }

    protected boolean typeIntoFieldAtIndex(By fieldLocator, int index, String value) {
        List<WebElement> fields = visibleElements(fieldLocator);
        if (fields.size() <= index) {
            return false;
        }
        WebElement field = fields.get(index);
        try {
            field.click();
            waitUntilFocused(field);
            try {
                field.clear();
            } catch (RuntimeException ignored) {
                // Some Compose text fields do not expose clear.
            }
            field.sendKeys(value);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    protected List<WebElement> visibleElements(By locator) {
        return driver.findElements(locator).stream()
                .filter(WebElement::isDisplayed)
                .collect(Collectors.toList());
    }

    protected void swipeUp() {
        swipe(0.75, 0.30);
    }

    /** Scrolls content back toward the top of the screen - the opposite direction of
     * swipeUp(). Needed to recover the Home feed's top bar (avatar, search, etc.) after
     * it's been scrolled out of view, e.g. by an autoplaying video or a prior test
     * leaving the feed scrolled down; a back-button press alone can't fix this since
     * it's a scroll position, not a different screen. */
    protected void swipeDown() {
        if (isAtTopOfScroll(driver)) {
            return;
        }
        swipe(0.30, 0.80);
    }

    /** A downward drag on the feed's first screen is a pull-to-refresh, not a scroll. */
    public static boolean isAtTopOfFeed(IOSDriver driver) {
        return isDisplayedNow(driver, "feed.filter.allPosts");
    }

    /** Generalisation of {@link #isAtTopOfFeed}: true when whatever scroll view is on
     * screen is already at its top, feed or not.
     *
     * The feed-only check was not enough. Several screens this suite scrolls BACK
     * through are plain SwiftUI ScrollViews that declare .refreshable -
     * FamilyProfileScreen and SavedPostsScreen both do - and on those,
     * "feed.filter.allPosts" is never present, so every backward swipe went through as a
     * 0.30 -> 0.80 drag on a list already at its top. That is a pull-to-refresh, and the
     * refresh re-renders the list, which changes the page source - so
     * scrollWhileContentMoves' "nothing moved" check read it as real movement and let the
     * loop run its full budget. On the family list that budget is 12, which is why the
     * screen visibly dragged down over and over during a run.
     *
     * Checking the offset first keeps the drag for the only case it was meant for:
     * content that is genuinely scrolled down. */
    static boolean isAtTopOfScroll(IOSDriver driver) {
        if (isAtTopOfFeed(driver)) {
            return true;
        }
        try {
            for (WebElement scrollView : driver.findElements(
                    AppiumBy.className("XCUIElementTypeScrollView"))) {
                if (!scrollView.isDisplayed()) {
                    continue;
                }
                // Direct first child, scoped to this one element - a bounded lookup, not
                // a document-wide XPath scan.
                List<WebElement> content = scrollView.findElements(By.xpath("./*[1]"));
                if (content.isEmpty()) {
                    continue;
                }
                // The first child sitting at or below the scroll view's own top edge
                // means there is nothing above it left to reveal.
                return content.get(0).getRect().getY() >= scrollView.getRect().getY();
            }
        } catch (RuntimeException ignored) {
            // Tree changed under us - fall through and allow the swipe.
        }
        return false;
    }

    static boolean isDisplayedNow(IOSDriver driver, String identifier) {
        try {
            return driver.findElements(AppiumBy.accessibilityId(identifier)).stream()
                    .anyMatch(WebElement::isDisplayed);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /** Checks visibility, swiping up to reveal further content if not immediately
     * found - needed for items appended to the bottom of a growing scrollable list
     * (e.g. a newly added row landing below the fold). */
    /** Mirror of {@link #scrollUntilVisible}, which only ever swipes toward the end of a
     * list. For content that sits above the current viewport - a header or count chip on
     * a list an earlier step has already scrolled down through. */
    protected boolean scrollBackUntilVisible(By... locators) {
        return scrollBackUntilVisible(4, locators);
    }

    protected boolean scrollBackUntilVisible(int maxSwipes, By... locators) {
        return scrollWhileContentMoves(maxSwipes, false, locators);
    }

    protected boolean scrollUntilVisible(By... locators) {
        return scrollUntilVisible(4, locators);
    }

    /** Swipe budget is explicit for screens whose content grows without bound: the family
     * member list gains rows on every suite run, and four swipes stopped reaching the
     * Save button below it once the account held nine members. Kept at 4 by default so
     * genuinely-absent content still fails fast. */
    protected boolean scrollUntilVisible(int maxSwipes, By... locators) {
        return scrollWhileContentMoves(maxSwipes, true, locators);
    }

    /** Swipes until the target appears OR the screen stops changing, whichever comes
     * first. The budget alone was not enough: it kept swiping against the end of a list
     * long after there was nothing left to reveal - visibly hammering the screen and
     * costing a swipe round-trip each time, worst on the family member list where the
     * budget is 12 and several lookups scroll per test. Comparing the page before and
     * after each swipe ends the loop as soon as the content has bottomed out. */
    private boolean scrollWhileContentMoves(int maxSwipes, boolean forward, By... locators) {
        if (isVisibleNow(locators)) {
            return true;
        }
        for (int attempt = 0; attempt < maxSwipes; attempt++) {
            String before = pageSignature();
            if (forward) {
                swipeUp();
            } else {
                swipeDown();
            }
            // Probe, not assert: the loop tries again, so waiting 8s per swipe here cost
            // up to 96 seconds on a 12-swipe budget.
            if (isVisibleNow(locators)) {
                return true;
            }
            if (before != null && before.equals(pageSignature())) {
                // Nothing moved - already at the end. Another swipe cannot help.
                break;
            }
        }
        return isAnyVisible(locators);
    }

    /** Cheap "has the screen changed" fingerprint. Length rather than full content: it is
     * enough to tell a scrolled list from a stuck one, without holding the whole tree. */
    private String pageSignature() {
        try {
            String source = driver.getPageSource();
            return source == null ? null : Integer.toString(source.length());
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    protected void pullToRefresh() {
        swipe(0.28, 0.78);
    }

    private void swipe(double startPercentY, double endPercentY) {
        Dimension size = driver.manage().window().getSize();
        swipe(driver, size.getWidth() / 2, (int) (size.getHeight() * startPercentY),
                (int) (size.getHeight() * endPercentY));
    }

    private static void swipe(IOSDriver driver, int centerX, int startY, int endY) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(700), PointerInput.Origin.viewport(), centerX, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(swipe));
    }

    protected String elementState(WebElement element) {
        Point location = element.getLocation();
        Dimension size = element.getSize();
        return element.getText() + ":"
                + element.getAttribute("label") + ":"
                + element.getAttribute("selected") + ":"
                + location.getX() + ","
                + location.getY() + ","
                + size.getWidth() + "x"
                + size.getHeight();
    }
}
