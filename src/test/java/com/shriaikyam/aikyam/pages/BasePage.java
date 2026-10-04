package com.shriaikyam.aikyam.pages;

import com.shriaikyam.aikyam.utils.AdbBridge;
import com.shriaikyam.aikyam.utils.WaitUtils;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
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
import java.util.stream.Collectors;

public abstract class BasePage {

    protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    protected static final Duration SHORT_TIMEOUT = Duration.ofSeconds(8);

    protected final AndroidDriver driver;

    protected BasePage(AndroidDriver driver) {
        this.driver = driver;
    }

    protected By accessibilityId(String value) {
        return AppiumBy.accessibilityId(value);
    }

    protected By textContains(String value) {
        return AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + value + "\")");
    }

    protected By text(String value) {
        return AppiumBy.androidUIAutomator("new UiSelector().text(\"" + value + "\")");
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
        try {
            return new WebDriverWait(driver, SHORT_TIMEOUT)
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
            WebElement target = waitForAnyVisible(locators);
            warnIfNoActionableTarget(target, locators);
            target.click();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** ADVISORY DIAGNOSTIC ONLY - never changes what gets tapped.
     *
     * Three separate defects in this suite shared one shape: a locator matched a node that
     * was visible but carried no click handler, so the tap silently did nothing and the
     * test burned its whole wait budget before failing somewhere unrelated. This surfaces
     * that shape immediately in the run log instead of costing hours of page-source
     * archaeology.
     *
     * Read the output as a HINT, NOT PROOF. Verified live during the DocumentsUI
     * investigation: the filename label there reports clickable=false with no clickable
     * ancestor anywhere above it, yet tapping it genuinely selects the file - Android
     * routes the touch to the underlying cell even though the accessibility tree never
     * describes that relationship. So a warning here does not mean the tap is broken, and
     * the absence of one does not mean it works. Confirm against a real run either way. */
    private void warnIfNoActionableTarget(WebElement target, By... locators) {
        try {
            if (Boolean.parseBoolean(target.getAttribute("clickable"))) {
                return;
            }
            List<WebElement> actionable = target.findElements(
                    By.xpath("./ancestor::*[@clickable='true']"));
            if (!actionable.isEmpty()) {
                return;
            }
            System.out.println("[tap-advisory] matched a node with clickable=false and no"
                    + " clickable ancestor. This is a HINT, not proof the tap failed"
                    + " (Android can still route the touch to an underlying cell)."
                    + " locators=" + Arrays.toString(locators)
                    + " class=" + attributeOrNull(target, "class")
                    + " text=" + attributeOrNull(target, "text")
                    + " content-desc=" + attributeOrNull(target, "content-desc")
                    + " resource-id=" + attributeOrNull(target, "resource-id")
                    + " bounds=" + attributeOrNull(target, "bounds"));
        } catch (RuntimeException ignored) {
            // Diagnostics must never influence the outcome of a tap.
        }
    }

    private String attributeOrNull(WebElement element, String name) {
        try {
            String value = element.getAttribute(name);
            return (value == null || value.isEmpty()) ? "-" : value;
        } catch (RuntimeException unavailable) {
            return "?";
        }
    }

    protected boolean typeIntoFirstVisible(String value, By... locators) {
        try {
            WebElement element = waitForAnyVisible(locators);
            element.click();
            // click() returns as soon as the tap is dispatched, not once the field has actually
            // gained focus/the IME has attached - immediately calling clear()/sendKeys() can race
            // that transition and silently land on a not-yet-focused field. Wait for it explicitly.
            waitUntilFocused(element);
            try {
                element.clear();
            } catch (RuntimeException ignored) {
                // Some Compose text fields do not expose clear; sending keys still works on empty fields.
            }
            element.sendKeys(value);
            if (valueLandedInField(element, value)) {
                return true;
            }
            // Some Compose text fields don't support the accessibility ACTION_SET_TEXT
            // that sendKeys() relies on internally - it silently no-ops instead of
            // throwing. A plain click() may also focus the field via an accessibility
            // action rather than a genuine touch, leaving no real IME connection for
            // adb-injected keystrokes to land on - use a real coordinate-based tap.
            tapElementCenter(element);
            waitUntilFocused(element);
            // The prior sendKeys() attempt may have partially inserted text before
            // being judged to have failed (confirmed live: this produced concatenated
            // old+new text) - clear again before typing the full value here.
            AdbBridge.clearField();
            AdbBridge.typeText(value);
            // Pressing the keyboard's IME "Done" action commits the typed value to
            // the field's real state - without it the text can sit typed-but-uncommitted.
            AdbBridge.pressImeDone();
            // The shell command returning doesn't mean the IME has finished delivering
            // keystrokes and the UI/accessibility tree has caught up yet - poll for it.
            for (int attempt = 0; attempt < 10; attempt++) {
                if (valueLandedInField(element, value)) {
                    return true;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return false;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    protected boolean valueLandedInField(WebElement element, String value) {
        try {
            String actual = element.getText();
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
    public static void tapElementCenter(AndroidDriver driver, WebElement element) {
        Point location = element.getLocation();
        Dimension size = element.getSize();
        tapPoint(driver, location.getX() + size.getWidth() / 2, location.getY() + size.getHeight() / 2);
    }

    /** Walks up to the nearest clickable ancestor, or returns the element unchanged if it
     * is already clickable or none is found. Two reasons, both confirmed live on the
     * bottom nav, whose labels are non-clickable Compose Text nodes: click() on a
     * non-clickable node is dispatched as an accessibility ACTION_CLICK that is not
     * reliably routed to the handler on its parent, and those labels sit in the last
     * ~30px of the screen (centre y=1582 where content ends at 1604), inside the system
     * gesture-navigation inset, where a synthesised touch gets swallowed before the app
     * sees it. The clickable parent spans icon+label, so its centre clears both problems. */
    public static WebElement clickableAncestorOf(WebElement element) {
        WebElement current = element;
        for (int depth = 0; depth < 4; depth++) {
            if ("true".equals(current.getAttribute("clickable"))) {
                return current;
            }
            try {
                current = current.findElement(By.xpath(".."));
            } catch (RuntimeException stopAtRoot) {
                break;
            }
        }
        return element;
    }

    /** Same real-touch gesture as {@link #tapElementCenter}, for the cases where the thing
     * to tap is a region rather than a node - a Compose scrim or dismiss-on-tap sheet
     * carries no text, contentDescription or class of its own to locate it by. */
    protected void tapPoint(int x, int y) {
        tapPoint(driver, x, y);
    }

    public static void tapPoint(AndroidDriver driver, int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
    }

    /** Only calls hideKeyboard() when a keyboard is actually showing - hideKeyboard()
     * dismisses via an internal BACK key press, which navigates the app back a screen
     * if there is no keyboard open to absorb it. */
    protected void hideKeyboardIfShown() {
        try {
            if (driver.isKeyboardShown()) {
                driver.hideKeyboard();
            }
        } catch (RuntimeException ignored) {
            // Best-effort: some devices report keyboard state unreliably.
        }
    }

    protected void waitUntilFocused(WebElement element) {
        try {
            new WebDriverWait(driver, SHORT_TIMEOUT)
                    .until(ignored -> "true".equals(element.getAttribute("focused")));
        } catch (RuntimeException ignored) {
            // Some custom Compose widgets never report a "focused" attribute at all; proceed
            // best-effort - sendKeys below will still fail cleanly if the field truly isn't ready.
        }
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
        String existing = field.getText();
        if (existing == null || existing.isEmpty()) {
            return;
        }
        driver.pressKey(new KeyEvent(AndroidKey.MOVE_END));
        for (int i = 0; i < existing.length(); i++) {
            driver.pressKey(new KeyEvent(AndroidKey.DEL));
        }
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
        swipe(0.30, 0.80);
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
        int centerX = size.getWidth() / 2;
        int startY = (int) (size.getHeight() * startPercentY);
        int endY = (int) (size.getHeight() * endPercentY);

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
                + element.getAttribute("content-desc") + ":"
                + element.getAttribute("selected") + ":"
                + location.getX() + ","
                + location.getY() + ","
                + size.getWidth() + "x"
                + size.getHeight();
    }
}
