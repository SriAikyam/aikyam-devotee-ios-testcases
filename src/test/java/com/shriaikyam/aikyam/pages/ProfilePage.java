package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.appmanagement.ApplicationState;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/** Header avatar -> SettingsView (the Settings hub) -> "Personal Profile" ->
 * UserProfileScreen for the signed-in user -> "Edit Profile" -> EditProfileView. */
public class ProfilePage extends BasePage {

    private String bundleId() {
        Object bundle = driver.getCapabilities().getCapability("appium:bundleId");
        return bundle != null ? bundle.toString() : DeepLinkPage.DEV_BUNDLE_ID;
    }

    // AccountSettingsButton: the avatar at the trailing end of every tab's header.
    private final By profileAvatarAction = id("header.account");
    private final By personalProfileAction = id("settings.row.personalProfile");
    private final By editProfileAction = id("profile.edit");
    private final By editProfileTitle = id("profile.edit.title");
    private final By nameField = id("profile.edit.name");
    private final By usernameField = id("profile.edit.username");
    private final By bioField = id("profile.edit.bio");
    private final By maleOption = id("profile.edit.gender.male");
    private final By femaleOption = id("profile.edit.gender.female");
    private final By otherOption = id("profile.edit.gender.other");
    // The banner and avatar tiles are PhotosPicker buttons; their captions ("Add banner",
    // "Change photo") are plain text beside them.
    private final By bannerAction = id("profile.edit.banner");
    private final By changePhotoAction = id("profile.edit.photo");
    private final By updateAction = id("profile.edit.update");
    private final By postsTab = id("profile.tab.posts");
    private final By aboutTab = id("profile.tab.about");
    private final By staticTexts = AppiumBy.className("XCUIElementTypeStaticText");
    private final By profileNameLabel = id("profile.name");
    private final By usernameLabel = id("profile.username");
    private final By noPostsLabel = id("profile.posts.empty");
    private final By postGridCell = id("profile.post.cell");
    private final By profileBannerLabel = text("Profile Banner");
    // FeedPostImmersiveView's ellipsis menu, then PostOptionsMenu's Delete.
    private final By postDetailMoreAction = accessibilityId("More options");
    private final By deleteOptionAction = id("postOptions.delete");
    private final By logoutAction = id("settings.logout");
    private final By logoutConfirmYesAction = AppiumBy.iOSClassChain(
            "**/XCUIElementTypeAlert/**/XCUIElementTypeButton[`label == \"Yes\"`]");
    private final By familyProfileAction = id("settings.row.familyProfile");
    private final By orderHistoryAction = id("settings.row.orderHistory");
    // Edit Profile's Danger Zone link; it opens DeleteAccountSheet, whose confirm button
    // has its own identifier, so the two are never confused.
    private final By deleteAccountLink = id("profile.edit.deleteAccount");
    private final By deleteAccountConfirmField = id("deleteAccount.confirmField");
    private final By deleteAccountConfirm = id("deleteAccount.confirm");
    private final By deleteAccountCancel = id("deleteAccount.cancel");
    // PHPicker renders out of process; its grid cells surface as images labelled
    // "Photo, <date>".
    private final By pickerFirstPhoto = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeImage' AND label BEGINSWITH 'Photo'");
    private final By pickerChrome = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeNavigationBar' AND (name == 'Photos' OR name == 'Collections')");
    private final By pickerDoneAction = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND (label == 'Done' OR label == 'Add')");
    private final By uploadingLabel = id("profile.edit.uploading");
    private final By shareAction = id("profile.share");
    // UIActivityViewController: its list container is identified "ActivityListView".
    private final By shareSheet = AppiumBy.iOSNsPredicateString(
            "name == 'ActivityListView' OR (type == 'XCUIElementTypeCell' AND label == 'Copy')");

    public ProfilePage(IOSDriver driver) {
        super(driver);
    }

    /** The avatar is on every tab's header but not on pushed screens or sheets, so this
     * backs out screen by screen until it is reachable again. */
    public boolean openProfile() {
        return reachAvatar() && tapFirstVisible(profileAvatarAction) && openPersonalProfile();
    }

    /** Cheap back-outs do the retrying; the Home-tab reset (scroll to top) runs at most once. */
    private boolean reachAvatar() {
        if (isVisibleNow(profileAvatarAction)) {
            return true;
        }
        for (int attempt = 0; attempt < 4; attempt++) {
            navigateBackQuietly();
            if (isVisibleNow(profileAvatarAction)) {
                return true;
            }
        }
        returnToTopOfFeed();
        return isVisibleNow(profileAvatarAction);
    }

    /** Re-selecting the active Home tab scrolls the feed back to its top, where the
     * header avatar lives. */
    private boolean returnToTopOfFeed() {
        try {
            new HomePage(driver).tapHomeTab();
            return isVisibleNow(profileAvatarAction);
        } catch (RuntimeException notOnFeed) {
            return false;
        }
    }

    /** The photo picker and share sheet can leave another process frontmost; bring the
     * app back instead of backing out of a screen that is not ours. */
    private void navigateBackQuietly() {
        try {
            if (driver.queryAppState(bundleId()) != ApplicationState.RUNNING_IN_FOREGROUND) {
                driver.activateApp(bundleId());
                return;
            }
            navigateBack();
        } catch (RuntimeException ignored) {
            // The next loop iteration's visibility check governs whether to retry.
        }
    }

    /** Reaches the Settings hub (avatar tap only) - where Logout lives, one level
     * shallower than openProfile(). */
    public boolean openSettingsHub() {
        return reachAvatar() && tapFirstVisible(profileAvatarAction);
    }

    public boolean openPersonalProfile() {
        if (!(scrollUntilVisible(personalProfileAction, text("Personal Profile"))
                && tapFirstVisible(personalProfileAction, text("Personal Profile")))) {
            return false;
        }
        waitUntilLoaded();
        return true;
    }

    public void waitUntilLoaded() {
        waitForAnyVisible(
                personalProfileAction,
                editProfileAction,
                profileNameLabel,
                usernameLabel,
                aboutTab,
                postsTab,
                editProfileTitle,
                nameField,
                usernameField,
                changePhotoAction,
                bannerAction,
                updateAction,
                profileBannerLabel
        );
    }

    public boolean areProfileDetailsVisible() {
        return isProfileNameVisible()
                || isUsernameVisible()
                || isEditProfileScreenVisible()
                || isAnyVisible(profileBannerLabel, editProfileAction);
    }

    public boolean isProfileNameVisible() {
        if (isAnyVisible(profileNameLabel)) {
            return true;
        }
        for (WebElement element : visibleElements(staticTexts)) {
            String text = element.getAttribute("label");
            if (text != null && !text.isBlank() && isLikelyProfileName(text)) {
                return true;
            }
        }
        return false;
    }

    public boolean isUsernameVisible() {
        if (isAnyVisible(usernameLabel)) {
            return true;
        }
        for (WebElement element : visibleElements(staticTexts)) {
            String text = element.getAttribute("label");
            if (text != null && text.startsWith("@") && text.length() > 1) {
                return true;
            }
        }
        return false;
    }

    public boolean isAboutTabVisible() {
        return isAnyVisible(aboutTab);
    }

    public boolean isPostsTabVisible() {
        return isAnyVisible(postsTab);
    }

    public boolean isEditProfileActionVisible() {
        return isAnyVisible(editProfileAction);
    }

    public boolean isNameFieldVisible() {
        return ensureEditFieldsVisible() && isAnyVisible(nameField);
    }

    public boolean isUsernameFieldVisible() {
        return ensureEditFieldsVisible() && isAnyVisible(usernameField);
    }

    public boolean updateName(String value) {
        return typeInto(nameField, value);
    }

    public boolean updateUsername(String value) {
        return typeInto(usernameField, value);
    }

    /** Bio is a TextEditor (XCUIElementTypeTextView), below Name and Username. */
    public boolean updateBio(String value) {
        return typeInto(bioField, value);
    }

    /** Each option is one SwiftUI Button (radio glyph + label), so tapping it selects. */
    public boolean selectGender(String gender) {
        By option = genderOption(gender);
        hideKeyboardIfShown();
        return scrollUntilVisible(option) && tapFirstVisible(option);
    }

    /** The radio state is drawn with an SF Symbol and not exposed as a trait, so this
     * reads `selected` and falls back to the symbol name surfacing in the label. */
    public boolean isGenderSelected(String gender) {
        By option = genderOption(gender);
        if (!scrollUntilVisible(option)) {
            return false;
        }
        try {
            WebElement radio = waitForAnyVisible(option);
            if (radio.isSelected()) {
                return true;
            }
            String label = radio.getAttribute("label");
            return label != null && label.toLowerCase().contains("fill");
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isUpdateButtonVisible() {
        return isAnyVisible(updateAction);
    }

    /** Fails when Update is disabled instead of reporting a successful tap: the app only
     * enables Update once something changed, so a disabled button means the edit before
     * it never landed. */
    public boolean tapUpdate() {
        hideKeyboardIfShown();
        try {
            WebElement update = waitForAnyVisible(updateAction);
            if (!update.isEnabled()) {
                return false;
            }
            update.click();
            return true;
        } catch (RuntimeException notResolved) {
            return false;
        }
    }

    /** Confirms an edited value was saved by finding it displayed as plain text on screen.
     * Saving is a network round-trip followed by EditProfileView dismissing, so poll. */
    public boolean isValueDisplayed(String expectedValue) {
        String expected = expectedValue.trim();
        By exact = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label == " + predicateLiteral(expected));
        try {
            return new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .ignoring(StaleElementReferenceException.class)
                    .until(ignored -> driver.findElements(exact).stream().anyMatch(WebElement::isDisplayed));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean openProfilePhotoPicker() {
        return scrollBackUntilVisible(changePhotoAction) && tapFirstVisible(changePhotoAction);
    }

    public boolean openBannerPhotoPicker() {
        return scrollBackUntilVisible(bannerAction) && tapFirstVisible(bannerAction);
    }

    /** True while the system photo picker is presented over the app. */
    public boolean isExternalPickerVisible() {
        return isAnyVisible(pickerChrome, pickerFirstPhoto);
    }

    /** Picks the first photo. The pickers allow one selection, which closes the picker
     * on its own; Done/Add is tapped only if the picker is still up. */
    public boolean selectFirstAvailablePhoto() {
        if (!tapFirstVisible(pickerFirstPhoto)) {
            return false;
        }
        if (isVisibleNow(pickerDoneAction)) {
            tapFirstVisible(pickerDoneAction);
        }
        return true;
    }

    /** Waits for the post-selection upload to finish - Update stays disabled and the
     * tile shows "Uploading" until this clears. */
    public boolean waitUntilUploadComplete() {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ignored -> !isVisibleNow(uploadingLabel));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Confirms a media update stuck: leave the screen, come back, and check the editor
     * reopens with the photo tiles present and no upload still in flight. */
    public boolean didMediaUpdatePersist() {
        if (!openProfile()) {
            return false;
        }
        waitUntilLoaded();
        if (!openEditProfile()) {
            return false;
        }
        if (isVisibleNow(uploadingLabel)) {
            return false;
        }
        return isAnyVisible(changePhotoAction, bannerAction);
    }

    /** iOS: EditProfileView shows no success toast - it dismisses itself once the save
     * succeeds, so the editor having closed is the success signal here. */
    public boolean isUpdateSuccessToastVisible() {
        return waitUntilGone(updateAction, editProfileTitle);
    }

    public boolean openEditProfile() {
        if (isEditProfileScreenVisible()) {
            return true;
        }
        if (!tapFirstVisible(editProfileAction)) {
            return false;
        }
        waitUntilLoaded();
        return isEditProfileScreenVisible();
    }

    public boolean isEditProfileVisible() {
        return isEditProfileScreenVisible();
    }

    public boolean openPostsTab() {
        if (tapFirstVisible(postsTab)) {
            return true;
        }
        if (isEditProfileScreenVisible() && navigateBackToProfile()) {
            return tapFirstVisible(postsTab);
        }
        return false;
    }

    public boolean openAboutTab() {
        if (tapFirstVisible(aboutTab)) {
            return true;
        }
        if (isEditProfileScreenVisible() && navigateBackToProfile()) {
            return tapFirstVisible(aboutTab);
        }
        return false;
    }

    public boolean areProfilePostsDisplayed() {
        return isAnyVisible(postGridCell, noPostsLabel, postsTab);
    }

    /** iOS: profile grid cells carry no caption (ProfilePostGridCell is a bare thumbnail
     * button), so a post cannot be opened by caption (see parity report). */
    public boolean openPostByCaption(String caption) {
        return false;
    }

    /** Deletes the currently open post via its immersive-view "More options" menu. */
    public boolean deleteOpenPost() {
        if (!tapFirstVisible(postDetailMoreAction, id("feed.post.more"))) {
            return false;
        }
        return tapFirstVisible(deleteOptionAction, text("Delete"));
    }

    /** iOS: captions are not rendered on the profile grid, so this can only check that
     * the caption is not on screen anywhere - it cannot positively locate the post. */
    public boolean isPostAbsent(String caption) {
        return !isAnyVisible(textContains(caption));
    }

    /** Taps "Family Profile" on the Settings hub, reached from openSettingsHub(). */
    public boolean openFamilyProfile() {
        return scrollUntilVisible(familyProfileAction, text("Family Profile"))
                && tapFirstVisible(familyProfileAction, text("Family Profile"));
    }

    public boolean openOrderHistory() {
        return scrollUntilVisible(orderHistoryAction, text("Order History"))
                && tapFirstVisible(orderHistoryAction, text("Order History"));
    }

    public boolean isLogoutVisible() {
        return scrollUntilVisible(logoutAction, text("Logout"));
    }

    /** Taps Logout, then confirms the "Are you sure you want to logout?" alert. */
    public boolean tapLogout() {
        if (!scrollUntilVisible(logoutAction, text("Logout")) || !tapFirstVisible(logoutAction, text("Logout"))) {
            return false;
        }
        try {
            waitForAnyVisible(logoutConfirmYesAction);
        } catch (RuntimeException exception) {
            return false;
        }
        return tapFirstVisible(logoutConfirmYesAction);
    }

    /** Reaches the Edit Profile screen and reports whether it got there. Prefers the
     * profile's own Edit Profile button, with Settings -> Personal Profile as fallback. */
    public boolean openEditProfileScreen() {
        if (isEditProfileScreenVisible()) {
            return true;
        }
        if (openProfile()) {
            waitUntilLoaded();
            if (openEditProfile()) {
                return true;
            }
        }
        if (openSettingsHub() && openPersonalProfile()) {
            openEditProfile();
        }
        return isEditProfileScreenVisible();
    }

    public boolean tapShareProfile() {
        return tapFirstVisible(shareAction);
    }

    /** The share action first mints a short link, then presents UIActivityViewController
     * in a sheet - so this polls briefly rather than checking once. */
    public boolean isNativeShareSheetVisible() {
        try {
            return new WebDriverWait(driver, SHORT_TIMEOUT)
                    .ignoring(StaleElementReferenceException.class)
                    .until(ignored -> driver.findElements(shareSheet).stream().anyMatch(WebElement::isDisplayed));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean dismissShareSheet() {
        try {
            navigateBack();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean openDeleteAccountDialog() {
        hideKeyboardIfShown();
        if (!scrollUntilVisible(8, deleteAccountLink)) {
            return false;
        }
        return tapFirstVisible(deleteAccountLink);
    }

    public boolean enterDeleteConfirmationText(String value) {
        return typeIntoFirstVisible(value, deleteAccountConfirmField);
    }

    /** Taps the sheet's "Delete Account" confirm button (enabled only once the typed
     * confirmation text exactly matches). */
    public boolean confirmAccountDeletion() {
        return tapFirstVisible(deleteAccountConfirm);
    }

    /** Non-destructive: reads the confirm button's state only, never taps. A disabled
     * SwiftUI Button reports enabled=false on iOS. */
    public boolean isDeleteAccountConfirmEnabled() {
        hideKeyboardIfShown();
        try {
            return waitForAnyVisible(deleteAccountConfirm).isEnabled();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Dismisses the Delete Account sheet without confirming. */
    public boolean cancelDeleteAccountDialog() {
        hideKeyboardIfShown();
        return tapFirstVisible(deleteAccountCancel);
    }

    private By genderOption(String gender) {
        if ("Male".equalsIgnoreCase(gender)) {
            return maleOption;
        }
        if ("Female".equalsIgnoreCase(gender)) {
            return femaleOption;
        }
        return otherOption;
    }

    /** Edit Profile fields can sit below the fold (Bio, Website, City...), so scroll to
     * the field before typing. XCUITest's clear() really empties the field. */
    private boolean typeInto(By field, String value) {
        if (!scrollUntilVisible(field)) {
            return false;
        }
        return typeIntoFirstVisible(value, field);
    }

    private boolean isEditProfileScreenVisible() {
        return isAnyVisible(
                editProfileTitle,
                nameField,
                usernameField,
                changePhotoAction,
                bannerAction,
                updateAction
        );
    }

    private boolean ensureEditFieldsVisible() {
        return scrollUntilVisible(nameField, usernameField);
    }

    private boolean navigateBackToProfile() {
        try {
            if (!tapFirstVisible(id("profile.edit.back"))) {
                navigateBack();
            }
            waitUntilLoaded();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static final List<String> NON_NAME_LABELS = List.of(
            "Posts", "About", "Edit Profile", "Personal Profile", "Name", "Username",
            "Profile Banner", "Change photo", "Add banner", "Update", "No posts yet",
            "Followers", "Following", "Views", "Bio", "Location", "Settings");

    private boolean isLikelyProfileName(String text) {
        String normalized = text.trim();
        if (normalized.startsWith("@")) {
            return false;
        }
        return !NON_NAME_LABELS.contains(normalized) && normalized.length() > 1;
    }
}
