package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class FamilyMembersPage extends BasePage {

    /** The member list grows on every suite run and is never pruned, so anything below it
     * - the add form and its Save button - keeps moving further down. Confirmed live: at
     * nine members the default four-swipe budget no longer reached Save, and it will only
     * get worse. Generous rather than tuned; the swipes stop as soon as the target is on
     * screen, so the cost is only paid when the list really is that long. */
    private static final int LIST_SWIPE_BUDGET = 12;

    private final By backAction = accessibilityId("Back");
    // Deliberately left as a loose contains: textMatches("\\d+ Members") was tried, to
    // stop this also matching the Settings screen's "Members, Gothram, Nakshatram"
    // subtitle, and it matched nothing at all - the \d escape is ambiguous once a Java
    // literal is handed to UiAutomator's own parser, and waitUntilLoaded then timed out
    // in 6 of 9 cases. Callers that need to know they are really on this screen should
    // use the form header instead, which no other screen carries.
    private final By memberCountChip = textContains("Members");
    private final By emptyStateLabel = textContains("No family members added yet");
    private final By newMemberFormHeader = text("New Family Member Details");
    private final By saveMembersAction = text("Save Members");
    private final By relationshipFieldLabel = text("Relationship");
    private final By fullNameFieldLabel = text("Full Name");
    // Full Name is a genuinely free-text EditText; Relationship is a read-only
    // ExposedDropdownMenuBox field that still renders as the same EditText class - both
    // share this one locator. Full Name is still taken at index 0 (it is the first field
    // on the screen), but Relationship is anchored on its label instead: see
    // openRelationshipDropdown for why a fixed index stopped holding.
    private final By editTextField = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\")"
    );
    // Every row's edit icon shares this exact contentDescription - there is no
    // per-row-unique locator, so editing a specific member is scoped by pairing the icon
    // with the member name sharing its row.
    private final By editIconAction = accessibilityId("Edit");
    private final By editSheetTitle = text("Edit family member");
    private final By deleteAction = text("Delete");
    private final By updateAction = text("Update");

    public FamilyMembersPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean isScreenVisible() {
        return isAnyVisible(memberCountChip, newMemberFormHeader, saveMembersAction);
    }

    /** Unambiguously "still on Family Members", for the after-navigating-away check.
     * isScreenVisible() is not safe for that: its memberCountChip is a loose
     * textContains("Members") that also matches the Settings screen's Family Profile
     * subtitle, "Members, Gothram, Nakshatram", so it reports true on the very screen
     * back navigation is supposed to land on (confirmed live: TC009 failed while the
     * screenshot showed Settings). This header exists only here. */
    public boolean isAddMemberFormVisible() {
        return isAnyVisible(newMemberFormHeader);
    }

    public void waitUntilLoaded() {
        waitForAnyVisible(memberCountChip, newMemberFormHeader, saveMembersAction);
    }

    public boolean tapBack() {
        if (!tapFirstVisible(backAction)) {
            return false;
        }
        // Give the navigation transition a moment to actually settle before the
        // caller checks what screen it landed on, rather than racing the animation.
        waitUntilGone(memberCountChip, newMemberFormHeader, saveMembersAction);
        return true;
    }

    public boolean isEmptyStateVisible() {
        return isAnyVisible(emptyStateLabel);
    }

    /** Newly added members are appended to the bottom of the list, which can land
     * below the fold once there are enough members on the account - scroll to find it
     * rather than only checking the current viewport. */
    public boolean isMemberVisible(String name) {
        hideKeyboardIfShown();
        return scrollUntilVisible(LIST_SWIPE_BUDGET, text(name));
    }

    /** Reads the "N Members" count chip and returns N, or -1 if it can't be parsed.
     * Tolerates staleness: the chip's own text changes as the list loads, so a reference
     * collected a moment ago can be gone by the time it is read (confirmed live: TC001
     * died on StaleElementReferenceException, which then left TC005 comparing against a
     * -1 baseline). Same per-element guard already used in MusicPage. */
    public int readMemberCount() {
        // The chip sits above the member list, so once the account has enough members
        // for an earlier step to have scrolled down, it is off the top of the viewport
        // and this returned -1 (confirmed live: TC005 compared "was 8, now -1").
        scrollBackUntilVisible(LIST_SWIPE_BUDGET, memberCountChip);
        for (WebElement element : visibleElements(memberCountChip)) {
            String value;
            try {
                value = element.getText();
            } catch (org.openqa.selenium.StaleElementReferenceException recomposed) {
                continue;
            }
            if (value == null) {
                continue;
            }
            String digits = value.replaceAll("[^0-9]", "");
            if (!digits.isBlank()) {
                return Integer.parseInt(digits);
            }
        }
        return -1;
    }

    public boolean enterFullName(String name) {
        WebElement field = formFieldUnder(fullNameFieldLabel);
        if (field == null) {
            return false;
        }
        try {
            tapElementCenter(field);
            waitUntilFocused(field);
            field.sendKeys(name);
            confirmEntryAndCloseKeyboard();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Presses the keyboard's own bottom-right action key (the tick / IME Done) once the
     * name is typed, which is what a real user does and what actually reveals Save
     * Members - the keyboard covers it otherwise. Preferred over driver.hideKeyboard(),
     * which dismisses via an internal BACK press and can navigate the app back a screen
     * when no keyboard is up. hideKeyboardIfShown() stays as the fallback for the case
     * where the IME action does not close it. */
    private void confirmEntryAndCloseKeyboard() {
        try {
            driver.pressKey(new KeyEvent(AndroidKey.ENTER));
        } catch (RuntimeException ignored) {
            // Fall through to the generic dismiss below.
        }
        hideKeyboardIfShown();
    }

    /** Scrolls the form into view and returns the field sitting directly under the given
     * label. Both form fields used to be taken at a fixed index into the visible
     * EditTexts, which stops working as the account gains members: the form sits below
     * the member list, so it slides down and eventually off the bottom entirely.
     * Confirmed live as the list grew - the Relationship lookup broke at eight members
     * and Full Name at around ten, when a fresh open showed no EditText at all. */
    private WebElement formFieldUnder(By label) {
        // Same reason as tapSaveMembers: swiping with the keyboard up glide-types into the
        // focused field and hides the lower part of the form.
        hideKeyboardIfShown();
        if (!scrollUntilVisible(LIST_SWIPE_BUDGET, label)) {
            return null;
        }
        List<WebElement> labels = visibleElements(label);
        if (labels.isEmpty()) {
            return null;
        }
        int labelBottom = labels.get(0).getLocation().getY() + labels.get(0).getSize().getHeight();
        WebElement field = null;
        int smallestGap = Integer.MAX_VALUE;
        for (WebElement candidate : visibleElements(editTextField)) {
            int gap = candidate.getLocation().getY() - labelBottom;
            if (gap >= 0 && gap < smallestGap) {
                smallestGap = gap;
                field = candidate;
            }
        }
        return field;
    }

    public boolean openRelationshipDropdown() {
        WebElement field = formFieldUnder(relationshipFieldLabel);
        if (field == null) {
            return false;
        }
        tapElementCenter(field);
        return true;
    }

    public boolean selectRelationshipOption(String option) {
        return tapFirstVisible(text(option));
    }

    /** Scrolls to it: the form sits below the member list, so it is off the bottom of the
     * viewport once the account has enough members (confirmed live: TC004 failed here at
     * eight members, on a screen where the field was present but below the fold). */
    public boolean isRelationshipFieldVisible() {
        return scrollUntilVisible(LIST_SWIPE_BUDGET, relationshipFieldLabel);
    }

    /** Scrolls first: the button sits below the New Family Member Details form, so every
     * member added pushes it further down, and past four members it is off-screen and
     * absent from the accessibility tree entirely. Confirmed live: TC003 filled the form
     * correctly (Full Name and Relationship both set) and failed only because Save was
     * not reachable, which read like a broken dropdown. This is why the failure appears
     * as the suite accumulates members rather than on a clean account. */
    public boolean tapSaveMembers() {
        // Dismiss the keyboard FIRST. With it open two things go wrong at once, both
        // confirmed live from TC002's diagnostics screenshot:
        //
        //  - It covers the bottom of the screen, and Save sits under it, so no amount of
        //    scrolling can reach it. TC003 only passed because opening the Relationship
        //    dropdown happens to dismiss the keyboard before it saves.
        //  - Worse, the scroll swipes then run ACROSS the keyboard, which glide-types into
        //    the focused field: the name went in as "QA 381219" and ended up
        //    "QA 381219 Hu Hu Hu", picking up the predictive-text suggestions on screen.
        //    So the scroll was corrupting the very value it was about to save.
        hideKeyboardIfShown();
        return scrollUntilVisible(LIST_SWIPE_BUDGET, saveMembersAction)
                && tapFirstVisible(saveMembersAction);
    }

    /** Adds a member using only the required Full Name field, leaving Relationship blank. */
    public boolean addMemberNameOnly(String name) {
        return enterFullName(name) && tapSaveMembers();
    }

    /** Enter Full Name, then Relationship, then Save - in that order. */
    public boolean addMemberWithRelationship(String name, String relationship) {
        if (!enterFullName(name)) {
            return false;
        }
        if (!openRelationshipDropdown() || !selectRelationshipOption(relationship)) {
            return false;
        }
        return tapSaveMembers();
    }

    /** Opens the edit bottom sheet for the member with this exact name - scoped by
     * pairing the name with the Edit icon on its own row, since every row's Edit icon
     * shares the same contentDescription. */
    public boolean openEditSheetForMember(String name) {
        // The member's row must be scrolled into view first - an off-screen row in a
        // lazily-composed list isn't in the accessibility tree at all yet.
        if (!scrollUntilVisible(LIST_SWIPE_BUDGET, text(name))) {
            return false;
        }
        List<WebElement> named = visibleElements(text(name));
        if (named.isEmpty()) {
            return false;
        }
        // Pair the row's name with its own edit icon by vertical position. This used to
        // be "//*[@text='...']/following::*[@content-desc='Edit'][1]", which reads better
        // and matches the tree exactly (confirmed live: name then Edit, in document
        // order, four of each) - but UiAutomator2's XPath engine matches nothing for it,
        // so the tap silently no-opped and every edit-sheet case failed. Same engine
        // limitation that made a nested-predicate XPath unusable in HomePage. Rows are
        // ~154px apart and an icon shares its name's y exactly, so nearest-by-y is
        // unambiguous.
        int rowY = named.get(0).getLocation().getY();
        WebElement rowEditIcon = null;
        int smallestGap = Integer.MAX_VALUE;
        for (WebElement icon : visibleElements(editIconAction)) {
            int gap = Math.abs(icon.getLocation().getY() - rowY);
            if (gap < smallestGap) {
                smallestGap = gap;
                rowEditIcon = icon;
            }
        }
        if (rowEditIcon == null) {
            return false;
        }
        tapElementCenter(rowEditIcon);
        try {
            waitForAnyVisible(editSheetTitle);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isEditSheetVisible() {
        return isAnyVisible(editSheetTitle);
    }

    /** Replaces the Full Name field's value inside the currently open edit sheet. */
    /** Unlike the add form's Full Name, this field arrives pre-filled with the member's
     * current name, so the existing value has to be deleted before typing - see
     * BasePage.deleteExistingValue for why clear() is not enough. */
    public boolean updateFullNameInEditSheet(String newName) {
        List<WebElement> fields = visibleElements(editTextField);
        if (fields.isEmpty()) {
            return false;
        }
        WebElement field = fields.get(0);
        try {
            tapElementCenter(field);
            waitUntilFocused(field);
            deleteExistingValue(field);
            field.sendKeys(newName);
            confirmEntryAndCloseKeyboard();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean tapUpdate() {
        return tapFirstVisible(updateAction);
    }

    public boolean tapDeleteInEditSheet() {
        return tapFirstVisible(deleteAction);
    }

    public boolean isMemberAbsent(String name) {
        return !isAnyVisible(text(name));
    }

    // ---- Helpers for FamilyMembersJourneyTest (additive) ----

    private final By gothramFieldLabel = text("Gothram");
    private final By nakshatramFieldLabel = text("Nakshatram");

    /** Same lookup as formFieldUnder but WITHOUT the up-front hideKeyboardIfShown.
     * driver.hideKeyboard() dismisses with BACK, and when the IME has already closed
     * (e.g. after the Enter key) a stale isKeyboardShown() makes that BACK pop the whole
     * Family Members screen - observed live: the run ended on Settings/Profile and every
     * later lookup failed. The journey helpers dismiss the keyboard through
     * closeKeyboardOnce() instead, which re-checks before acting and never repeats. */
    private WebElement fieldUnder(By label) {
        if (!scrollUntilVisible(LIST_SWIPE_BUDGET, label)) {
            return null;
        }
        List<WebElement> labels = visibleElements(label);
        if (labels.isEmpty()) {
            return null;
        }
        int labelBottom = labels.get(0).getLocation().getY() + labels.get(0).getSize().getHeight();
        int labelX = labels.get(0).getLocation().getX();
        // Gothram and Nakshatram sit side by side with labels on the same row, so the
        // field is the nearest one below the label AND, among equals, the one whose left
        // edge is closest to the label's (picking by y alone returned Gothram's field for
        // the Nakshatram label and the dropdown never opened).
        WebElement field = null;
        long best = Long.MAX_VALUE;
        for (WebElement candidate : visibleElements(editTextField)) {
            int gap = candidate.getLocation().getY() - labelBottom;
            if (gap < 0) {
                continue;
            }
            long score = (long) (gap / 20) * 100000L + Math.abs(candidate.getLocation().getX() - labelX);
            if (score < best) {
                best = score;
                field = candidate;
            }
        }
        return field;
    }

    /** Presses Enter (IME action) and gives the IME a moment to close. */
    private void commitAndCloseKeyboardOnce() {
        try {
            driver.pressKey(new KeyEvent(AndroidKey.ENTER));
        } catch (RuntimeException ignored) {
            // fall through
        }
        pause(700);
        // Deliberately no hideKeyboard()/BACK here: when Enter has closed the IME a stale
        // isKeyboardShown() makes that BACK pop the Family Members screen (live: the run
        // ended on Profile). Opening the next dropdown closes any IME that is left up.
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean typeInto(By label, String value) {
        WebElement field = fieldUnder(label);
        if (field == null) {
            return false;
        }
        try {
            tapElementCenter(field);
            waitUntilFocused(field);
            field.sendKeys(value);
            commitAndCloseKeyboardOnce();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean enterFullNameSafe(String name) {
        return typeInto(fullNameFieldLabel, name);
    }

    public boolean enterGothram(String gothram) {
        return typeInto(gothramFieldLabel, gothram);
    }

    /** A tap that lands while the IME is still up can be spent closing it, so the menu
     * does not open; retry once if the option did not appear. */
    private boolean pickFromDropdown(By label, String option) {
        for (int attempt = 0; attempt < 2; attempt++) {
            WebElement field = fieldUnder(label);
            if (field == null) {
                return false;
            }
            tapElementCenter(field);
            pause(700);
            if (isVisibleNow(text(option))) {
                return tapFirstVisible(text(option));
            }
        }
        return false;
    }

    public boolean selectRelationshipSafe(String option) {
        return pickFromDropdown(relationshipFieldLabel, option);
    }

    public boolean selectNakshatram(String option) {
        return pickFromDropdown(nakshatramFieldLabel, option);
    }

    /** Scrolls to Save Members and taps it (no keyboard handling; callers closed it). */
    public boolean tapSaveMembersSafe() {
        return scrollUntilVisible(LIST_SWIPE_BUDGET, saveMembersAction)
                && tapFirstVisible(saveMembersAction);
    }

    /** Current value of the add form's Full Name field ("" if not found). */
    public String readAddFormFullName() {
        WebElement field = fieldUnder(fullNameFieldLabel);
        return field == null ? "" : field.getText();
    }

    /** Empties the add form's Full Name field. */
    public boolean clearAddFormFullName() {
        WebElement field = fieldUnder(fullNameFieldLabel);
        if (field == null) {
            return false;
        }
        try {
            tapElementCenter(field);
            waitUntilFocused(field);
            deleteExistingValue(field);
            commitAndCloseKeyboardOnce();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Types like a user (real key events via adb, one char at a time) so the app's
     * per-keystroke 20-char cap applies - WebElement.sendKeys replaces the whole value at
     * once and the app then rejects it outright. Returns what the field really holds. */
    public String typeIntoAddFormFullNameAndRead(String text) {
        WebElement field = fieldUnder(fullNameFieldLabel);
        if (field == null) {
            return null;
        }
        tapElementCenter(field);
        waitUntilFocused(field);
        com.shriaikyam.aikyam.utils.AdbBridge.typeText(text);
        pause(800);
        WebElement fresh = fieldUnder(fullNameFieldLabel);
        return fresh == null ? null : fresh.getText();
    }

    /** Visible texts on screen, for failure messages. */
    public String screenTexts() {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("text=\"([^\"]+)\"")
                    .matcher(driver.getPageSource());
            StringBuilder out = new StringBuilder();
            while (m.find()) {
                out.append('[').append(m.group(1)).append(']');
            }
            return out.toString();
        } catch (RuntimeException unavailable) {
            return "<page source unavailable>";
        }
    }

    /** Waits for the list to finish loading: the chip reads "0 Members" until the fetch
     * lands. A value counts once two reads ~2s apart agree, and a 0 is only believed when
     * no row Edit icon is on screen and 10s have passed (TC220 once took a premature 0
     * as its baseline on an account that really held 4 members). */
    public int readSettledMemberCount() {
        long startedAt = System.currentTimeMillis();
        int previous = readMemberCountStable();
        for (int i = 0; i < 12; i++) {
            pause(2000);
            int now = readMemberCountStable();
            boolean zeroBelievable = System.currentTimeMillis() - startedAt > 10000
                    && visibleElements(editIconAction).isEmpty();
            if (now == previous && (now > 0 || zeroBelievable)) {
                return now;
            }
            previous = now;
        }
        return previous;
    }

    /** Count read that really scrolls to the "N Members" chip: readMemberCount's own
     * scroll-back stops as soon as ANY text containing "Members" is visible, and the top
     * bar title "Family Members" always is, so it never scrolled and returned -1 once the
     * list had been scrolled. Matches the chip by Java-side regex. -1 if never found. */
    public int readMemberCountStable() {
        java.util.regex.Pattern chip = java.util.regex.Pattern.compile("^\\s*(\\d+) Members\\s*$");
        for (int attempt = 0; attempt < LIST_SWIPE_BUDGET; attempt++) {
            try {
                for (WebElement element : visibleElements(memberCountChip)) {
                    java.util.regex.Matcher match = chip.matcher(String.valueOf(element.getText()));
                    if (match.matches()) {
                        return Integer.parseInt(match.group(1));
                    }
                }
            } catch (org.openqa.selenium.StaleElementReferenceException recomposing) {
                continue;
            }
            swipeDown();
        }
        return -1;
    }

    /** The edit sheet's four fields in reading order: Full Name, Relationship, Gothram,
     * Nakshatram. Only fields below the sheet title are taken, so the add form behind
     * the sheet cannot leak in. Returns their current texts, or null if not four. */
    public List<String> readEditSheetFields() {
        List<WebElement> titles = visibleElements(editSheetTitle);
        if (titles.isEmpty()) {
            return null;
        }
        int titleY = titles.get(0).getLocation().getY();
        List<WebElement> fields = new java.util.ArrayList<>();
        for (WebElement field : visibleElements(editTextField)) {
            if (field.getLocation().getY() > titleY) {
                fields.add(field);
            }
        }
        fields.sort(java.util.Comparator
                .comparingInt((WebElement f) -> f.getLocation().getY() / 20)
                .thenComparingInt(f -> f.getLocation().getX()));
        if (fields.size() != 4) {
            return null;
        }
        List<String> values = new java.util.ArrayList<>();
        for (WebElement field : fields) {
            values.add(field.getText());
        }
        return values;
    }

    /** Waits for the edit sheet to close (after Update or Delete). */
    public boolean waitEditSheetClosed() {
        return waitUntilGone(editSheetTitle);
    }

    /** Closes the edit sheet without saving (system Back dismisses the modal sheet). */
    public void dismissEditSheet() {
        if (isVisibleNow(editSheetTitle)) {
            driver.pressKey(new KeyEvent(AndroidKey.BACK));
            waitUntilGone(editSheetTitle);
        }
    }

    /** True if text containing this fragment (a Toast included) shows up within ~6s.
     * Polls the raw page source: a Toast lives ~2s and need not report isDisplayed. */
    public boolean isTextShown(String fragment) {
        long end = System.currentTimeMillis() + 6000;
        while (System.currentTimeMillis() < end) {
            try {
                String source = driver.getPageSource();
                if (source != null && source.contains(fragment)) {
                    return true;
                }
            } catch (RuntimeException transientFailure) {
                // retry
            }
        }
        return false;
    }

    /** Like isMemberVisible but without its up-front hideKeyboardIfShown (see fieldUnder). */
    public boolean isMemberListed(String name) {
        return scrollUntilVisible(LIST_SWIPE_BUDGET, text(name));
    }

    /** Edit-sheet rename without the hideKeyboard call (see fieldUnder). */
    public boolean renameInEditSheet(String newName) {
        List<WebElement> fields = visibleElements(editTextField);
        if (fields.isEmpty()) {
            return false;
        }
        WebElement field = fields.get(0);
        try {
            tapElementCenter(field);
            waitUntilFocused(field);
            deleteExistingValue(field);
            field.sendKeys(newName);
            commitAndCloseKeyboardOnce();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** True only after scrolling the whole list: no row carries exactly this name. */
    public boolean isMemberGoneFromList(String name) {
        scrollBackUntilVisible(LIST_SWIPE_BUDGET, memberCountChip);
        return !scrollUntilVisible(LIST_SWIPE_BUDGET, text(name));
    }

    private boolean tapFieldAtIndex(int index) {
        try {
            List<WebElement> fields = visibleElements(editTextField);
            if (fields.size() <= index) {
                return false;
            }
            fields.get(index).click();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Tap the field, type via adb (this screen's fields don't support the
     * accessibility action Appium's sendKeys() relies on), then tap the keyboard's
     * IME "Done" button to commit the value. */
    private boolean typeIntoFieldAtIndex(int index, String value) {
        try {
            List<WebElement> fields = visibleElements(editTextField);
            if (fields.size() <= index) {
                return false;
            }
            tapElementCenter(fields.get(index));
            com.shriaikyam.aikyam.utils.AdbBridge.typeText(value);
            com.shriaikyam.aikyam.utils.AdbBridge.pressImeDone();
            // pressImeDone() commits the value but does not close the keyboard on this
            // field - left open, it physically covers the Relationship field/Update
            // button beneath it, so a subsequent tap lands on the keyboard instead and
            // can cascade into navigating out of the screen entirely.
            hideKeyboardIfShown();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
