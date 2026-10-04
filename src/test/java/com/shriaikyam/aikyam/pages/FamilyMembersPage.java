package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** FamilyProfileScreen: member list, then the "New Family Member Details" add form
 * (family.add.*), with FamilyMemberFormSheet (family.edit.*) for editing a row. Both
 * forms render FamilyMemberFormFieldsView, so their fields differ only by id prefix. */
public class FamilyMembersPage extends BasePage {

    /** The member list grows on every suite run and is never pruned, so the add form and
     * its Save button below it keep moving further down. The swipes stop as soon as the
     * target is on screen, so the budget is only spent when the list really is that long. */
    private static final int LIST_SWIPE_BUDGET = 12;
    private static final Pattern COUNT_CHIP = Pattern.compile("^\\s*(\\d+) Members\\s*$");

    private final By memberCountChip = id("family.count");
    private final By emptyStateLabel = id("family.empty");
    private final By newMemberFormHeader = id("family.add.title");
    private final By saveMembersAction = id("family.save");
    private final By addFullNameField = id("family.add.fullName");
    private final By addRelationshipField = id("family.add.relationship");
    private final By addGothramField = id("family.add.gothram");
    private final By addNakshatramField = id("family.add.nakshatram");
    // Every row's Edit button shares one id; a specific member's is found by pairing it
    // with the name on its own row.
    private final By memberNames = id("family.member.name");
    private final By editIconAction = id("family.member.edit");
    private final By editSheetTitle = id("family.edit.title");
    private final By editFullNameField = id("family.edit.fullName");
    private final By editRelationshipField = id("family.edit.relationship");
    private final By editGothramField = id("family.edit.gothram");
    private final By editNakshatramField = id("family.edit.nakshatram");
    private final By deleteAction = id("family.edit.delete");
    private final By updateAction = id("family.edit.update");
    private final By editCancelAction = id("family.edit.cancel");

    public FamilyMembersPage(IOSDriver driver) {
        super(driver);
    }

    public boolean isScreenVisible() {
        return isAnyVisible(memberCountChip, newMemberFormHeader, saveMembersAction);
    }

    /** Unambiguously "still on Family Members", for the after-navigating-away check. */
    public boolean isAddMemberFormVisible() {
        return isAnyVisible(newMemberFormHeader);
    }

    public void waitUntilLoaded() {
        waitForAnyVisible(memberCountChip, newMemberFormHeader, saveMembersAction);
    }

    public boolean tapBack() {
        if (!navigateBack()) {
            return false;
        }
        // Let the pop transition settle before the caller checks where it landed.
        waitUntilGone(memberCountChip, newMemberFormHeader, saveMembersAction);
        return true;
    }

    public boolean isEmptyStateVisible() {
        return isAnyVisible(emptyStateLabel);
    }

    /** Newly added members are appended to the bottom of the list, which can land below
     * the fold - scroll to find it rather than only checking the current viewport. */
    public boolean isMemberVisible(String name) {
        hideKeyboardIfShown();
        return scrollUntilVisible(LIST_SWIPE_BUDGET, memberNamed(name));
    }

    /** Reads the "N Members" count chip and returns N, or -1 if it can't be parsed. */
    public int readMemberCount() {
        scrollBackUntilVisible(LIST_SWIPE_BUDGET, memberCountChip);
        Integer count = parseVisibleCount();
        return count == null ? -1 : count;
    }

    public boolean enterFullName(String name) {
        hideKeyboardIfShown();
        boolean typed = typeIntoFormField(addFullNameField, name);
        hideKeyboardIfShown();
        return typed;
    }

    public boolean openRelationshipDropdown() {
        hideKeyboardIfShown();
        return scrollUntilVisible(LIST_SWIPE_BUDGET, addRelationshipField) && tapFirstVisible(addRelationshipField);
    }

    /** The relationship picker is a SwiftUI Menu; its options are buttons. */
    public boolean selectRelationshipOption(String option) {
        return tapFirstVisible(menuOption(option), text(option));
    }

    public boolean isRelationshipFieldVisible() {
        return scrollUntilVisible(LIST_SWIPE_BUDGET, addRelationshipField);
    }

    /** Keyboard first: it covers Save, and swiping across it would glide-type into the
     * focused field. */
    public boolean tapSaveMembers() {
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

    /** Opens the edit sheet for the member with this exact name, pairing the name with
     * the Edit button on its own row by vertical position. */
    public boolean openEditSheetForMember(String name) {
        By named = memberNamed(name);
        if (!scrollUntilVisible(LIST_SWIPE_BUDGET, named)) {
            return false;
        }
        List<WebElement> matches = visibleElements(named);
        if (matches.isEmpty()) {
            return false;
        }
        WebElement nameLabel = matches.get(0);
        int rowCentreY = nameLabel.getLocation().getY() + nameLabel.getSize().getHeight() / 2;
        WebElement rowEditIcon = null;
        int smallestGap = Integer.MAX_VALUE;
        for (WebElement icon : visibleElements(editIconAction)) {
            int iconCentreY = icon.getLocation().getY() + icon.getSize().getHeight() / 2;
            int gap = Math.abs(iconCentreY - rowCentreY);
            if (gap < smallestGap) {
                smallestGap = gap;
                rowEditIcon = icon;
            }
        }
        if (rowEditIcon == null) {
            return false;
        }
        rowEditIcon.click();
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

    /** Replaces the pre-filled Full Name in the open edit sheet. */
    public boolean updateFullNameInEditSheet(String newName) {
        boolean typed = typeIntoFormField(editFullNameField, newName);
        hideKeyboardIfShown();
        return typed;
    }

    public boolean tapUpdate() {
        hideKeyboardIfShown();
        return tapFirstVisible(updateAction);
    }

    public boolean tapDeleteInEditSheet() {
        hideKeyboardIfShown();
        return tapFirstVisible(deleteAction);
    }

    public boolean isMemberAbsent(String name) {
        return !isAnyVisible(memberNamed(name));
    }

    // ---- Helpers for FamilyMembersJourneyTest ----

    public boolean enterFullNameSafe(String name) {
        return enterFullName(name);
    }

    public boolean enterGothram(String gothram) {
        boolean typed = typeIntoFormField(addGothramField, gothram);
        hideKeyboardIfShown();
        return typed;
    }

    /** Opens a picker Menu and taps the option; retried once in case the first tap was
     * spent dismissing the keyboard. */
    private boolean pickFromDropdown(By picker, String option) {
        hideKeyboardIfShown();
        for (int attempt = 0; attempt < 2; attempt++) {
            if (!scrollUntilVisible(LIST_SWIPE_BUDGET, picker) || !tapFirstVisible(picker)) {
                return false;
            }
            if (isVisibleNow(menuOption(option))) {
                return tapFirstVisible(menuOption(option));
            }
        }
        return false;
    }

    public boolean selectRelationshipSafe(String option) {
        return pickFromDropdown(addRelationshipField, option);
    }

    public boolean selectNakshatram(String option) {
        return pickFromDropdown(addNakshatramField, option);
    }

    /** Scrolls to Save Members and taps it. */
    public boolean tapSaveMembersSafe() {
        hideKeyboardIfShown();
        return scrollUntilVisible(LIST_SWIPE_BUDGET, saveMembersAction)
                && tapFirstVisible(saveMembersAction);
    }

    /** Current value of the add form's Full Name field ("" if not found or empty). */
    public String readAddFormFullName() {
        WebElement field = scrollToField(addFullNameField);
        return field == null ? "" : fieldValue(field);
    }

    /** Empties the add form's Full Name field. */
    public boolean clearAddFormFullName() {
        WebElement field = scrollToField(addFullNameField);
        if (field == null) {
            return false;
        }
        try {
            field.click();
            waitUntilFocused(field);
            field.clear();
            hideKeyboardIfShown();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** XCUITest's sendKeys types through the keyboard one character at a time, so the
     * app's per-keystroke 20-char cap applies. Returns what the field really holds. */
    public String typeIntoAddFormFullNameAndRead(String text) {
        WebElement field = scrollToField(addFullNameField);
        if (field == null) {
            return null;
        }
        try {
            field.click();
            waitUntilFocused(field);
            field.sendKeys(text);
        } catch (RuntimeException exception) {
            return null;
        }
        WebElement fresh = scrollToField(addFullNameField);
        return fresh == null ? null : fieldValue(fresh);
    }

    /** Visible labels and values on screen, for failure messages. */
    public String screenTexts() {
        try {
            Matcher m = Pattern.compile("(?:label|value)=\"([^\"]+)\"").matcher(driver.getPageSource());
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
     * no row Edit button is on screen and 10s have passed. */
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

    /** Scrolls back up to the "N Members" chip and reads it. -1 if never found. */
    public int readMemberCountStable() {
        for (int attempt = 0; attempt < LIST_SWIPE_BUDGET; attempt++) {
            Integer count = parseVisibleCount();
            if (count != null) {
                return count;
            }
            swipeDown();
        }
        return -1;
    }

    /** The edit sheet's four fields in reading order: Full Name, Relationship, Gothram,
     * Nakshatram. Only the family.edit.* ids are read, so the add form behind the sheet
     * cannot leak in. Returns their current values, or null if any is missing. */
    public List<String> readEditSheetFields() {
        if (visibleElements(editSheetTitle).isEmpty()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (By locator : new By[] {editFullNameField, editRelationshipField, editGothramField, editNakshatramField}) {
            List<WebElement> found = visibleElements(locator);
            if (found.isEmpty()) {
                return null;
            }
            values.add(fieldValue(found.get(0)));
        }
        return values;
    }

    /** Waits for the edit sheet to close (after Update or Delete). */
    public boolean waitEditSheetClosed() {
        return waitUntilGone(editSheetTitle);
    }

    /** Closes the edit sheet without saving via its Cancel toolbar button. */
    public void dismissEditSheet() {
        if (isVisibleNow(editSheetTitle)) {
            hideKeyboardIfShown();
            if (!tapFirstVisible(editCancelAction)) {
                navigateBack();
            }
            waitUntilGone(editSheetTitle);
        }
    }

    /** True if text containing this fragment (the toast included) shows up within ~6s.
     * Polls the raw page source: the toast lives ~2.5s. */
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

    public boolean isMemberListed(String name) {
        return scrollUntilVisible(LIST_SWIPE_BUDGET, memberNamed(name));
    }

    /** Edit-sheet rename. */
    public boolean renameInEditSheet(String newName) {
        return updateFullNameInEditSheet(newName);
    }

    /** True only after scrolling the whole list: no row carries exactly this name. Matched
     * on the row name id, so a value still sitting in the add form never counts. */
    public boolean isMemberGoneFromList(String name) {
        scrollBackUntilVisible(LIST_SWIPE_BUDGET, memberCountChip);
        return !scrollUntilVisible(LIST_SWIPE_BUDGET, memberNamed(name));
    }

    private By memberNamed(String name) {
        return AppiumBy.iOSNsPredicateString(
                "name == 'family.member.name' AND label == " + predicateLiteral(name));
    }

    private By menuOption(String option) {
        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == " + predicateLiteral(option));
    }

    private Integer parseVisibleCount() {
        try {
            for (WebElement element : visibleElements(memberCountChip)) {
                Matcher match = COUNT_CHIP.matcher(String.valueOf(element.getAttribute("label")));
                if (match.matches()) {
                    return Integer.parseInt(match.group(1));
                }
            }
        } catch (StaleElementReferenceException rerendered) {
            return null;
        }
        return null;
    }

    private WebElement scrollToField(By field) {
        if (!scrollUntilVisible(LIST_SWIPE_BUDGET, field)) {
            return null;
        }
        List<WebElement> fields = visibleElements(field);
        return fields.isEmpty() ? null : fields.get(0);
    }

    private boolean typeIntoFormField(By field, String value) {
        if (scrollToField(field) == null) {
            return false;
        }
        return typeIntoFirstVisible(value, field);
    }

    /** An empty TextField reports its placeholder as `value`; a Menu picker reports its
     * selection as `label`. */
    private String fieldValue(WebElement field) {
        String type = field.getAttribute("type");
        if (!"XCUIElementTypeTextField".equals(type)) {
            String label = field.getAttribute("label");
            return label == null ? "" : label;
        }
        String value = field.getAttribute("value");
        String placeholder = field.getAttribute("placeholderValue");
        if (value == null || value.equals(placeholder)) {
            return "";
        }
        return value;
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
