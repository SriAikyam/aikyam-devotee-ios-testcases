package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

public class ProfilePage extends BasePage {

    // Reads whichever appPackage BaseTest configured the session with (dev or prod,
    // controlled by -Dappium.appPackage) so back-press recovery guards below always
    // compare against the app actually under test, not a hardcoded flavor.
    private String appPackage() {
        Object appPackage = driver.getCapabilities().getCapability("appium:appPackage");
        return appPackage != null ? appPackage.toString() : "com.shriaikyam.aikyam.dev";
    }

    // The bottom-nav "Apps" tab is an unrelated stub screen ("Apps Coming Soon");
    // Profile is only reachable via the top-bar avatar icon, which does have a real
    // contentDescription ("Profile") unlike the bottom-nav text labels.
    private final By profileAvatarAction = accessibilityId("Profile");
    // Confirmed live: on a dual-role (devotee + temple) account, the Settings screen
    // shows a "Switch account" section ABOVE this row, and its own section header text
    // is also exactly "Personal Profile" (unlike single-role accounts, where the
    // header reads "Personal Details" instead - no collision there). Matching the
    // LAST such text node reaches the real clickable row in both cases: for
    // single-role accounts it's the only match; for dual-role accounts the header
    // comes first in document order, so the row is always last.
    private final By personalProfileAction = By.xpath("(//*[@text='Personal Profile'])[last()]");
    private final By editProfileAction = accessibilityId("Edit Profile");
    private final By editTextField = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\")"
    );
    private final By nameFieldLabel = text("Name *");
    private final By usernameFieldLabel = text("Username *");
    // Field order confirmed live via ProfileEditScreen.kt: Name(0), Username(1), Bio(2),
    // Website(3), then Gender (radio buttons, not an EditText) and DOB (date picker, not
    // an EditText) sit between Website and City - City/Gotram/Nakshatra/Primary Language
    // are further down and need a scroll before they're in the visible EditText list.
    private final By maleOption = text("Male");
    private final By femaleOption = text("Female");
    private final By otherOption = text("Other");
    // Both buttons have no contentDescription at all (confirmed live: content-desc is
    // null), so these must match on visible label text. The banner button reads "Add
    // banner" when no banner is set yet (the only state this test account has) and
    // "Change banner" once one exists - textContains("banner") covers both.
    private final By bannerAction = textContains("banner");
    private final By changePhotoAction = text("Change photo");
    private final By updateAction = text("Update");
    private final By postsTab = accessibilityId("Posts");
    private final By aboutTab = accessibilityId("About");
    private final By textViews = AppiumBy.className("android.widget.TextView");
    private final By profileNameLabel = text("Name");
    private final By usernameLabel = text("Username");
    private final By noPostsLabel = textContains("No posts");
    private final By profileBannerLabel = text("Profile Banner");
    // The post-detail "kebab" menu's contentDescription is "More" (not "More options",
    // which is the feed card's own action-row icon reused elsewhere).
    private final By postDetailMoreAction = accessibilityId("More");
    private final By deleteOptionAction = text("Delete");
    // Logout lives on the Settings hub screen (reached directly from the avatar tap,
    // one level shallower than the "Personal Profile" detail screen openProfile() below
    // navigates into). The button has no contentDescription - match on its visible text.
    // Tapping it only opens a confirmation dialog; the real logout needs "Yes" tapped too.
    private final By logoutAction = text("Logout");
    private final By logoutConfirmYesAction = text("Yes");
    // Also on the Settings hub screen, under "Personal Details" - reached directly from
    // the avatar tap, no need to go through "Personal Profile" first.
    private final By familyProfileAction = text("Family Profile");
    // Under "Seva Orders" section on the Settings hub, may need a scroll to reach.
    private final By orderHistoryAction = text("Order History");
    // Delete Account lives in the "DANGER ZONE" section at the bottom of the Edit Profile
    // screen (avatar -> Settings hub -> Personal Profile). The confirmation dialog reuses
    // the exact same "Delete Account" text for its own confirm button, but the dialog
    // renders in its own separate window on top, so re-using this locator after the
    // dialog opens reliably hits the dialog's button rather than the link behind it.
    private final By deleteAccountLink = text("Delete Account");
    // The system Photo Picker (com.google.android.photopicker, not our app's own
    // package) labels every grid tile with this exact accessibility description
    // pattern regardless of which app opened it - a stable, standard locator.
    private final By pickerFirstPhoto = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Photo taken\").instance(0)"
    );
    private final By pickerDoneAction = text("Done");
    // Shown with a spinner on the Change Photo/Add Banner tile while the picked image
    // uploads - Update stays disabled until this clears.
    private final By uploadingLabel = textContains("Uploading");
    // The app's own save-confirmation snackbar - a uniform, reliable success signal
    // for name, username, photo, and banner updates alike, unlike trying to detect a
    // changed image pixel-for-pixel.
    private final By updateSuccessToast = textContains("updated successfully");

    // Personal Profile (openPersonalProfile()) actually lands on the shared
    // UserProfileScreen (Settings routes onPersonalProfileClick to
    // Screen.UserProfile.createRoute(own userId) - confirmed live via
    // DevoteeSettingsNavGraph.kt), so it carries the same "Share" action other users'
    // profiles do (UserProfileHeader.kt, hardcoded contentDescription "Share" - not a
    // string resource). feature_devotee_profile's own separate ProfileScreen/"Share
    // via" action is dead code (confirmed: zero nav graph references), not this one.
    private final By shareAction = accessibilityId("Share");

    public ProfilePage(IOSDriver driver) {
        super(driver);
    }

    /**
     * The avatar is only present on the Home feed's top bar. Earlier tests can leave
     * the app on Edit Profile, an About/Posts tab, or an external photo/banner picker
     * (a different package entirely) - none of which expose it - so this backs out
     * screen by screen until the avatar becomes reachable again. It can also be simply
     * scrolled out of view on the Home feed itself (e.g. an autoplaying video), which a
     * back press alone can't fix - so also try scrolling back to the top each attempt.
     */
    public boolean openProfile() {
        return reachAvatar() && tapFirstVisible(profileAvatarAction) && openPersonalProfile();
    }

    /** Gets the top-bar avatar on screen: cheap retries first, the costly reset once.
     *
     * The previous loop ran the expensive step on every one of six iterations. Tapping the
     * Home tab is not one action - HomePage.tap does up to six back-presses, then waits up
     * to 30s for the tab, taps, then waits up to 30s again for the bottom bar. Over a
     * minute per call, up to six times, so a single openProfile() could burn ~7 minutes.
     * Measured: TC014/TC016/TC119 all sat at a ~138s floor across two runs, and the Appium
     * log showed waits up to 4985ms with 37 of 64 lookups finding nothing.
     *
     * Repeating it was pointless anyway: if resetting to the feed did not reveal the
     * avatar the first time, doing it five more times cannot either. So back-presses -
     * which are cheap and fix the common case of being stuck on another screen - do the
     * retrying, and the Home-tab reset runs at most once, for the rarer case of the feed
     * being scrolled away from its top. */
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

    /** Returns to the top of the Home feed by re-selecting the Home tab - no swiping.
     *
     * The avatar lives in the top bar, so it is unreachable when the feed is scrolled
     * away from the top. This used to swipe down to fix that: first unconditionally on
     * all six recovery iterations, then via scrollBackUntilVisible. Both kept dragging the
     * screen, because the "did the content move?" guard compares page-source length and
     * the Home feed's length changes constantly on its own - autoplaying video, items
     * loading - so the guard read "moved" every time and never stopped early.
     *
     * Re-selecting an already-active Home tab is a documented no-op/scroll-to-top in this
     * app, and HomePage.tapHomeTab uses the real-touch nav path that is known to work. One
     * deterministic gesture replaces up to twelve blind drags per call. */
    private boolean returnToTopOfFeed() {
        try {
            new HomePage(driver).tapHomeTab();
            return isVisibleNow(profileAvatarAction);
        } catch (RuntimeException notOnFeed) {
            return false;
        }
    }

    /** Confirmed live: repeated recovery back-presses (openProfile()/openSettingsHub()'s
     * loops) can walk all the way past the app's own root screen and out to the
     * Android home screen entirely, stranding every later test on the wrong app until
     * something explicitly relaunches it. If we're no longer in the app's own
     * package, relaunch it instead of pressing back again. */
    private void navigateBackQuietly() {
        try {
            String currentPackage = driver.getCurrentPackage();
            if (currentPackage != null && !currentPackage.equals(appPackage())) {
                driver.activateApp(appPackage());
                return;
            }
            driver.navigate().back();
        } catch (RuntimeException ignored) {
            // Some states (e.g. certain external pickers) may not support back; the
            // next loop iteration's visibility check still governs whether to retry.
        }
    }

    /** Reaches the Settings hub (avatar tap only) - this is where Logout actually lives,
     * one level shallower than openProfile()'s "Personal Profile" detail screen.
     * Confirmed live: missing the swipeDown() openProfile() has meant a feed scrolled
     * away from the top left the avatar permanently unreachable via back-presses alone
     * (a scroll position, not a screen, so no amount of back-navigation fixes it) -
     * every subsequent call in the same run failed identically until the app was
     * relaunched. */
    public boolean openSettingsHub() {
        // Same recovery as openProfile - see reachAvatar for why the costly Home-tab reset
        // runs at most once rather than on every retry.
        return reachAvatar() && tapFirstVisible(profileAvatarAction);
    }

    public boolean openPersonalProfile() {
        if (!tapFirstVisible(personalProfileAction)) {
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
                nameFieldLabel,
                usernameFieldLabel,
                editTextField,
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
        for (WebElement element : visibleElements(textViews)) {
            String text = element.getText();
            if (text != null && !text.isBlank() && isLikelyProfileName(text)) {
                return true;
            }
        }
        return false;
    }

    public boolean isUsernameVisible() {
        for (WebElement element : visibleElements(textViews)) {
            String text = element.getText();
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
        return ensureEditFieldsVisible() && (isAnyVisible(nameFieldLabel) || !visibleElements(editTextField).isEmpty());
    }

    public boolean isUsernameFieldVisible() {
        return ensureEditFieldsVisible() && (isAnyVisible(usernameFieldLabel) || visibleElements(editTextField).size() > 1);
    }

    public boolean updateName(String value) {
        return typeIntoFieldAtIndex(0, value);
    }

    public boolean updateUsername(String value) {
        return typeIntoFieldAtIndex(1, value);
    }

    public boolean updateBio(String value) {
        return typeIntoFieldAtIndex(2, value);
    }

    /** Taps the Male/Female/Other option - the whole label+radio Row is one clickable
     * unit (confirmed live via ProfileEditScreen.kt), so tapping the label text lands
     * within the same clickable bounds even though the Text node itself carries no
     * click handler of its own. */
    public boolean selectGender(String gender) {
        By option;
        if ("Male".equalsIgnoreCase(gender)) {
            option = maleOption;
        } else if ("Female".equalsIgnoreCase(gender)) {
            option = femaleOption;
        } else {
            option = otherOption;
        }
        // Confirmed live: Gender sits below Bio/Website, out of the initially-rendered
        // viewport - tapFirstVisible() alone never finds it without scrolling first.
        if (!scrollUntilVisible(option)) {
            return false;
        }
        // Tap the clickable Row, not the label. The label Text node is not clickable (the
        // app puts .clickable on the whole label+radio Row - the same fact
        // isGenderSelected relies on to READ the state), so click() on it is dispatched as
        // an accessibility action that is not reliably routed to the handler. Confirmed
        // live: TC120 reported "gender does not persist" while the diagnostics screenshot
        // showed the radio unchanged AND the Update button still disabled - the app had
        // seen no change at all, because the tap never landed. Nothing was failing to
        // persist; nothing was ever selected.
        String label = "Male".equalsIgnoreCase(gender) ? "Male"
                : "Female".equalsIgnoreCase(gender) ? "Female" : "Other";
        return tapRadioBesideLabel(label);
    }

    /** Taps the RadioButton next to a label, not the label or its row.
     *
     * Read from the actual hierarchy rather than guessed at: each option is three nodes -
     * the label TextView (clickable=false), a row container (clickable=true), and a
     * sibling RadioButton (checkable=true, clickable=true while unselected and false once
     * selected). The radio sits to the LEFT of its label - e.g. Male's radio spans
     * x 52-148 while its label starts at x 144 - so tapping the row container landed on
     * the label area instead of the control, which is why three XPath attempts all failed
     * with the gender never changing.
     *
     * Matched by position: the radio on the same horizontal band as the label, nearest to
     * its left. That survives a reordering of Male/Female/Other, which an index would
     * not. */
    private boolean tapRadioBesideLabel(String labelText) {
        By label = text(labelText);
        if (!scrollUntilVisible(label)) {
            return false;
        }
        List<WebElement> labels = visibleElements(label);
        if (labels.isEmpty()) {
            return false;
        }
        Point labelAt = labels.get(0).getLocation();
        Dimension labelSize = labels.get(0).getSize();
        int labelMidY = labelAt.getY() + labelSize.getHeight() / 2;

        WebElement radio = null;
        int nearestGap = Integer.MAX_VALUE;
        for (WebElement candidate : visibleElements(AppiumBy.className("android.widget.RadioButton"))) {
            try {
                Point at = candidate.getLocation();
                Dimension size = candidate.getSize();
                boolean sameRow = at.getY() <= labelMidY && labelMidY <= at.getY() + size.getHeight();
                int gap = labelAt.getX() - (at.getX() + size.getWidth());
                if (sameRow && gap >= -size.getWidth() && gap < nearestGap) {
                    nearestGap = gap;
                    radio = candidate;
                }
            } catch (RuntimeException recomposed) {
                // Skip nodes that vanish mid-scan.
            }
        }
        if (radio == null) {
            return false;
        }
        tapElementCenter(radio);
        return true;
    }

    /** True if the given gender's RadioButton reports itself checked - a real
     * persistence signal for a saved gender selection, since the main profile view
     * doesn't display gender as plain text anywhere. */
    public boolean isGenderSelected(String gender) {
        By label = "Male".equalsIgnoreCase(gender) ? maleOption
                : "Female".equalsIgnoreCase(gender) ? femaleOption : otherOption;
        // The label's own immediate parent isn't necessarily the Row containing its
        // sibling RadioButton (confirmed live: that XPath never matched) - the nearest
        // CLICKABLE ancestor is the whole label+radio Row per source
        // (.clickable { selectedGender = value }), same pattern already used for
        // isDeleteAccountConfirmEnabled()'s equivalent "find the real container" need.
        By radioNode = By.xpath(
                "(//*[@text='" + gender + "']/ancestor::*[@clickable='true'][1]//*[@checkable='true'])[1]"
        );
        if (!scrollUntilVisible(label)) {
            return false;
        }
        try {
            WebElement radio = waitForAnyVisible(radioNode);
            return radio.isSelected();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isUpdateButtonVisible() {
        return isAnyVisible(updateAction);
    }

    /** Taps Update to actually save the edited field(s), not just check the button appears. */
    /** Fails loudly when Update is disabled instead of reporting a successful tap.
     *
     * tapFirstVisible returns true whenever it finds and clicks a node, whether or not
     * that node is enabled - so a disabled Update button was reported as "saved". The app
     * only enables Update once something has actually changed, so a disabled button means
     * the edit before it never landed. That is exactly what hid the real TC120 cause:
     * the gender tap missed, Update stayed greyed out, tapUpdate() still said true, and
     * the failure surfaced two steps later as "gender does not persist". */
    public boolean tapUpdate() {
        By updateContainer = By.xpath("(//*[@text='Update']/ancestor::*[@clickable='true'])[last()]");
        try {
            WebElement container = waitForAnyVisible(updateContainer);
            if (!container.isEnabled()) {
                return false;
            }
        } catch (RuntimeException notResolved) {
            // Fall through to the plain tap - better to try than to fail on the probe.
        }
        return tapFirstVisible(updateAction);
    }

    /** Confirms an edited value was saved by finding it displayed as plain text on screen.
     * tapUpdate() returns as soon as the tap is dispatched, not once the save request has
     * completed and the profile screen has re-rendered with the new value - poll briefly
     * rather than checking only once immediately after. */
    public boolean isValueDisplayed(String expectedValue) {
        try {
            // Saving the profile is a real network round-trip - SHORT_TIMEOUT (8s) was
            // observed to be too tight under real network conditions (confirmed live: the
            // new value was showing correctly on screen, just after the poll had already
            // given up).
            // Staleness has to be tolerated per poll, not allowed to end the wait.
            // WebDriverWait only ignores NotFoundException by default, so a
            // StaleElementReferenceException - which this screen throws constantly while
            // it transitions from the edit form back to the profile - aborted the wait on
            // its first tick and returned false. Confirmed live: TC119 reported it could
            // not find "QA bio 1787763801017" while the captured page source held exactly
            // that string, in a displayed TextView.
            return new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ignored -> {
                try {
                    return visibleElements(textViews).stream()
                            .map(element -> {
                                try {
                                    return element.getText();
                                } catch (StaleElementReferenceException recomposed) {
                                    return null;
                                }
                            })
                            .anyMatch(text -> text != null && text.trim().equals(expectedValue.trim()));
                } catch (StaleElementReferenceException recomposed) {
                    return false;
                }
            });
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** "Change photo" is a caption under the avatar, not the control - the camera badge
     * beside it is what opens the picker, and the caption's own node is not clickable. So
     * click() on the text was dispatched as an unrouted accessibility action and no picker
     * appeared, which surfaced as "External picker should be visible" in TC047 while the
     * screenshot showed Edit Profile sitting there untouched. Same fix as the bottom nav,
     * volume toggle and gender radios: real touch on the clickable ancestor. */
    /** Taps the camera badge ABOVE the "Change photo" caption.
     *
     * The caption itself does nothing - it is a label, not a control (confirmed on device
     * by the account owner). Earlier attempts tapped the text, then its clickable
     * container, and neither opened the picker. The real control is the small camera icon
     * sitting on the avatar just above that caption, and it carries no
     * contentDescription, so it is found by position: the nearest clickable node whose
     * bounds sit above the caption and overlap it horizontally. */
    public boolean openProfilePhotoPicker() {
        return tapCameraBadgeAbove("Change photo");
    }

    private boolean tapCameraBadgeAbove(String captionText) {
        By caption = text(captionText);
        if (!scrollUntilVisible(caption)) {
            return false;
        }
        List<WebElement> captions = visibleElements(caption);
        if (captions.isEmpty()) {
            return false;
        }
        Point captionAt = captions.get(0).getLocation();
        Dimension captionSize = captions.get(0).getSize();
        int captionCentreX = captionAt.getX() + captionSize.getWidth() / 2;

        WebElement badge = null;
        int smallestGap = Integer.MAX_VALUE;
        for (WebElement candidate : visibleElements(AppiumBy.xpath("//*[@clickable='true']"))) {
            try {
                Point at = candidate.getLocation();
                Dimension size = candidate.getSize();
                int bottom = at.getY() + size.getHeight();
                boolean above = bottom <= captionAt.getY();
                boolean overlapsHorizontally = at.getX() <= captionCentreX
                        && captionCentreX <= at.getX() + size.getWidth();
                // Ignore full-width containers - the badge is a small icon, and a
                // page-wide clickable row would also satisfy the checks above.
                boolean smallEnough = size.getWidth() < captionSize.getWidth() * 4;
                if (above && overlapsHorizontally && smallEnough) {
                    int gap = captionAt.getY() - bottom;
                    if (gap < smallestGap) {
                        smallestGap = gap;
                        badge = candidate;
                    }
                }
            } catch (RuntimeException recomposed) {
                // Skip nodes that vanish mid-scan.
            }
        }
        if (badge == null) {
            return false;
        }
        tapElementCenter(badge);
        return true;
    }

    /** Resolves the clickable container by ancestor:: XPath rather than by walking up with
     * relative "..". BasePage.clickableAncestorOf uses the latter and did not resolve here
     * - TC047/TC120 failed identically after switching to it - whereas the ancestor:: form
     * is already proven on this driver by isGenderSelected(), which reads the radio state
     * through exactly this expression. */
    private boolean tapClickableContainerOf(String labelText) {
        By label = text(labelText);
        if (!scrollUntilVisible(label)) {
            return false;
        }
        By container = By.xpath("(//*[@text='" + labelText + "']/ancestor::*[@clickable='true'])[1]");
        try {
            tapElementCenter(waitForAnyVisible(container));
            return true;
        } catch (RuntimeException notResolved) {
            return false;
        }
    }

    /** The banner button's label varies ("Add banner" before one is set, "Change banner"
     * after), so this resolves the container from whichever label is on screen rather than
     * from a fixed string. */
    public boolean openBannerPhotoPicker() {
        List<WebElement> matches = visibleElements(bannerAction);
        if (matches.isEmpty()) {
            return false;
        }
        try {
            return tapClickableContainerOf(matches.get(0).getText());
        } catch (RuntimeException unreadable) {
            return false;
        }
    }

    public boolean isExternalPickerVisible() {
        String currentPackage = driver.getCurrentPackage();
        return currentPackage != null && !currentPackage.equals(appPackage());
    }

    /** Selects the first (most recent) photo in the system Photo Picker and confirms
     * the selection - drives Change Photo/Add Banner through to a real image applied,
     * rather than only checking that the picker opens. */
    public boolean selectFirstAvailablePhoto() {
        return tapFirstVisible(pickerFirstPhoto) && tapFirstVisible(pickerDoneAction);
    }

    /** Waits for the post-selection upload to finish - Update stays disabled and the
     * tile shows "Uploading..." until this clears. */
    public boolean waitUntilUploadComplete() {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ignored -> !isAnyVisible(uploadingLabel));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Confirms an edit was actually saved via the app's own save-confirmation
     * snackbar - works uniformly for name, username, photo, and banner updates. */
    /** Confirms a media update stuck, without relying on the success popup.
     *
     * The popup is a system Toast - it shows for a couple of seconds and this device
     * captures it unreliably, so asserting on it made TC047 fail even when the upload had
     * genuinely worked (run 1 reached this final step with everything before it passing).
     *
     * Instead: leave the screen entirely, come back, and check the editor reopens with the
     * photo tile settled - present, and no longer showing "Uploading". A value that
     * survives navigating away and returning has actually been saved, which is the real
     * guarantee the popup was only hinting at. */
    public boolean didMediaUpdatePersist() {
        if (!openProfile()) {
            return false;
        }
        waitUntilLoaded();
        if (!openEditProfile()) {
            return false;
        }
        // Still mid-upload means it did not finish, whatever the popup said.
        if (isVisibleNow(uploadingLabel)) {
            return false;
        }
        return isAnyVisible(changePhotoAction, bannerAction);
    }

    public boolean isUpdateSuccessToastVisible() {
        return isAnyVisible(updateSuccessToast);
    }

    public boolean openEditProfile() {
        if (isEditProfileScreenVisible()) {
            return true;
        }
        if (!tapFirstVisible(editProfileAction, text("Edit Profile"))) {
            return false;
        }
        waitUntilLoaded();
        return isEditProfileScreenVisible();
    }

    public boolean isEditProfileVisible() {
        return isEditProfileScreenVisible();
    }

    public boolean openPostsTab() {
        if (tapFirstVisible(postsTab, text("Posts"))) {
            return true;
        }
        if (isEditProfileScreenVisible() && navigateBackToProfile()) {
            return tapFirstVisible(postsTab, text("Posts"));
        }
        return false;
    }

    public boolean openAboutTab() {
        if (tapFirstVisible(aboutTab, text("About"))) {
            return true;
        }
        if (isEditProfileScreenVisible() && navigateBackToProfile()) {
            return tapFirstVisible(aboutTab, text("About"));
        }
        return false;
    }

    public boolean areProfilePostsDisplayed() {
        return isAnyVisible(postsTab, noPostsLabel, accessibilityId("Video"), text("Posts"));
    }

    /**
     * Opens a post from the profile grid by its caption - the grid thumbnail's
     * contentDescription is set to the post's caption text.
     */
    public boolean openPostByCaption(String caption) {
        return scrollUntilVisible(accessibilityId(caption)) && tapFirstVisible(accessibilityId(caption));
    }

    /** Deletes the currently open post via its detail-view "More" menu. */
    public boolean deleteOpenPost() {
        if (!tapFirstVisible(postDetailMoreAction)) {
            return false;
        }
        return tapFirstVisible(deleteOptionAction);
    }

    public boolean isPostAbsent(String caption) {
        return !isAnyVisible(accessibilityId(caption));
    }

    /** Taps "Family Profile" on the Settings hub, reached from openSettingsHub(). */
    public boolean openFamilyProfile() {
        return scrollUntilVisible(familyProfileAction) && tapFirstVisible(familyProfileAction);
    }

    public boolean openOrderHistory() {
        return scrollUntilVisible(orderHistoryAction) && tapFirstVisible(orderHistoryAction);
    }

    public boolean isLogoutVisible() {
        return scrollUntilVisible(logoutAction);
    }

    /** Taps Logout, then confirms the "Are you sure you want to logout?" dialog. */
    public boolean tapLogout() {
        if (!scrollUntilVisible(logoutAction) || !tapFirstVisible(logoutAction)) {
            return false;
        }
        try {
            waitForAnyVisible(logoutConfirmYesAction);
        } catch (RuntimeException exception) {
            return false;
        }
        return tapFirstVisible(logoutConfirmYesAction);
    }

    /** Navigates avatar -> Settings hub -> Personal Profile, landing on the Edit Profile
     * screen where the Danger Zone / Delete Account section lives. */
    /** Reaches the Edit Profile screen and REPORTS WHETHER IT GOT THERE.
     *
     * This used to end in an unconditional `return true`, so it claimed success wherever
     * it landed - unlike openEditProfile(), which returns isEditProfileScreenVisible().
     * That made TC111's "Edit Profile screen should open" a false positive: it passed
     * while the app sat on the Profile screen, and the failure surfaced one line later as
     * "Delete Account dialog should open", pointing at the wrong thing entirely
     * (confirmed live from the diagnostics screenshot).
     *
     * Prefers the profile's own Edit Profile button, the route openEditProfile() already
     * proves reliable, and keeps Settings -> Personal Profile as a fallback. */
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
        if (openSettingsHub() && tapFirstVisible(personalProfileAction)) {
            waitUntilLoaded();
        }
        return isEditProfileScreenVisible();
    }

    public boolean tapShareProfile() {
        return tapFirstVisible(shareAction);
    }

    /** The native Android share sheet is a system UI surface outside this app's own
     * package - same detection technique isExternalPickerVisible() already uses for
     * the system photo picker. The chooser Intent takes a moment to render after the
     * tap dispatches, so this polls briefly rather than checking once immediately. */
    public boolean isNativeShareSheetVisible() {
        try {
            return new WebDriverWait(driver, SHORT_TIMEOUT).until(ignored -> isExternalPickerVisible());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean dismissShareSheet() {
        try {
            driver.navigate().back();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean openDeleteAccountDialog() {
        if (!scrollUntilVisible(deleteAccountLink)) {
            // scrollUntilVisible()'s own attempts weren't always enough to reach the
            // Danger Zone on this screen - a few extra swipes as a fallback.
            for (int attempt = 0; attempt < 4 && !isAnyVisible(deleteAccountLink); attempt++) {
                swipeUp();
            }
            if (!isAnyVisible(deleteAccountLink)) {
                return false;
            }
        }
        return tapFirstVisible(deleteAccountLink);
    }

    /** Types the confirmation word into the delete-account dialog's text field - the
     * dialog is the only place with an editable field once it's open. */
    public boolean enterDeleteConfirmationText(String value) {
        return typeIntoFirstVisible(value, editTextField);
    }

    /** Taps the dialog's "Delete Account" confirm button (only enabled once the typed
     * confirmation text exactly matches). */
    public boolean confirmAccountDeletion() {
        return tapFirstVisible(deleteAccountLink);
    }

    /** True only if the dialog's confirm button is actually enabled - the button's own
     * Text node always reports enabled=true regardless of its real disabled state
     * (same quirk documented on PostPage's Post button), so this checks the nearest
     * clickable ancestor instead. Non-destructive: reads state only, never taps. */
    /** Reads the dialog's confirm button, not the Danger Zone link behind it.
     *
     * "Delete Account" appears TWICE in the tree while the dialog is open - the dialog's
     * own button and the Edit Profile link underneath it - and (...)[1] on an ancestor::
     * axis selects the FIRST IN DOCUMENT ORDER, which is the outer/earlier match, not the
     * nearest one. So this was reading the link's enabled state (true) instead of the
     * button's, and reported the confirm button as enabled while it was correctly
     * disabled. Confirmed live: the button node is enabled=false, clickable=true while
     * the wrong node it was reading is enabled=true, and the screenshot shows the button
     * visibly dimmed. [last()] takes the dialog's button, which renders later.
     *
     * The app's validation was never broken - this check was. */
    public boolean isDeleteAccountConfirmEnabled() {
        By confirmContainer = By.xpath("(//*[@text='Delete Account']/ancestor::*[@clickable='true'])[last()]");
        try {
            WebElement container = waitForAnyVisible(confirmContainer);
            return container.isEnabled();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Dismisses the Delete Account dialog without confirming. */
    public boolean cancelDeleteAccountDialog() {
        return tapFirstVisible(text("Cancel"));
    }


    /** Confirmed live: some fields on this screen (Bio, confirmed - Name/Username
     * happen to work) silently no-op on sendKeys() rather than throwing, the same
     * Compose ACTION_SET_TEXT gap BasePage.typeIntoFirstVisible() already works around
     * for other screens. Falls back to a real coordinate tap + adb-injected keystrokes
     * when the direct sendKeys() value doesn't actually land. */
    private boolean typeIntoFieldAtIndex(int index, String value) {
        try {
            List<WebElement> fields = visibleElements(editTextField);
            if (fields.size() <= index) {
                return false;
            }

            WebElement field = fields.get(index);
            field.click();
            waitUntilFocused(field);
            try {
                field.clear();
            } catch (RuntimeException ignored) {
            }
            field.sendKeys(value);
            if (valueLandedInField(field, value)) {
                return true;
            }

            tapElementCenter(field);
            waitUntilFocused(field);
            // The prior sendKeys() attempt may have partially inserted text before
            // being judged to have failed - clear again before typing the full value.
            com.shriaikyam.aikyam.utils.AdbBridge.clearField();
            com.shriaikyam.aikyam.utils.AdbBridge.typeText(value);
            com.shriaikyam.aikyam.utils.AdbBridge.pressImeDone();
            for (int attempt = 0; attempt < 10; attempt++) {
                if (valueLandedInField(field, value)) {
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

    private boolean isEditProfileScreenVisible() {
        return isAnyVisible(
                nameFieldLabel,
                usernameFieldLabel,
                changePhotoAction,
                bannerAction,
                updateAction
        );
    }

    private boolean ensureEditFieldsVisible() {
        if (isAnyVisible(nameFieldLabel, usernameFieldLabel) || visibleElements(editTextField).size() > 1) {
            return true;
        }

        for (int attempt = 0; attempt < 4; attempt++) {
            swipeUp();
            if (isAnyVisible(nameFieldLabel, usernameFieldLabel) || visibleElements(editTextField).size() > 1) {
                return true;
            }
        }
        return isAnyVisible(nameFieldLabel, usernameFieldLabel) || visibleElements(editTextField).size() > 1;
    }

    private boolean navigateBackToProfile() {
        try {
            driver.navigate().back();
            waitUntilLoaded();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean isLikelyProfileName(String text) {
        String normalized = text.trim();
        if (normalized.startsWith("@")) {
            return false;
        }
        return !normalized.equals("Posts")
                && !normalized.equals("About")
                && !normalized.equals("Edit Profile")
                && !normalized.equals("Personal Profile")
                && !normalized.equals("Name")
                && !normalized.equals("Username")
                && !normalized.equals("Profile Banner")
                && !normalized.equals("Change photo")
                && !normalized.equals("Add banner")
                && !normalized.equals("Change banner")
                && !normalized.equals("Update")
                && !normalized.equals("No posts yet")
                && normalized.length() > 1;
    }
}
