package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.ProfilePage;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.shriaikyam.aikyam.categories.P0;
import org.junit.runners.MethodSorters;

// Four cases here mutate the same profile (name, username, bio, gender) and one opens the
// Delete Account dialog, but ordering was left to JUnit's hash-based default - the same
// hazard already pinned in ExploreTest/PostTest/SettingsTest/FamilyMembersTest.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProfileTest extends BaseTest {

    /* ---- Phase 3 pilot: THIS CLASS ONLY. ProfileTest is ordered NAME_ASCENDING for
       determinism and holds no shared mutable fields. It IS state-heavy: TC047 saves a new
       name, username, profile photo and banner without restoring them, and drives the
       external DocumentsUI picker twice - so this pilot exercises the Phase 1b
       foreign-package recovery against a shared session, which no earlier pilot did.
       BaseTest.resetToHome() still runs before EVERY test; only the session is reused. */
    @Override
    protected boolean reuseDriverAcrossTests() {
        return true;
    }

    /** Scopes the shared session to this class; without it the session would leak into
     * whichever class runs next. */
    @AfterClass
    public static void quitSharedDriver() {
        releaseSharedDriver();
    }

    /** Bio field, part of the same Edit Profile screen as TC047 but not covered
     * there - real round-trip save+display check, same pattern as name/username. */
    @Test
    public void TC119_Verify_Profile_Edit_Bio_Field() {
        ProfilePage profilePage = new ProfilePage(driver);
        String bio = "QA bio " + System.currentTimeMillis();

        Assert.assertTrue("Profile should open", profilePage.openProfile());
        profilePage.waitUntilLoaded();
        Assert.assertTrue("Edit profile should open", profilePage.openEditProfile());

        Assert.assertTrue("Bio field should accept input", profilePage.updateBio(bio));
        Assert.assertTrue("Update should save the new bio", profilePage.tapUpdate());
        Assert.assertTrue("New bio should be reflected on the profile, looked for: " + bio,
                profilePage.isValueDisplayed(bio));
    }

    /** Upgraded from "About tab is visible" (TC038-style) to confirming switching to
     * it actually shows the profile's real info, not just that the tab label exists. */
    @Test
    public void TC121_Verify_About_Tab_Shows_Profile_Info() {
        ProfilePage profilePage = new ProfilePage(driver);

        Assert.assertTrue("Profile should open", profilePage.openProfile());
        profilePage.waitUntilLoaded();

        Assert.assertTrue("About tab should be tappable", profilePage.openAboutTab());
        Assert.assertTrue("About tab should show the profile name", profilePage.isProfileNameVisible());
        Assert.assertTrue("About tab should show the username", profilePage.isUsernameVisible());
    }

    // TC111_Verify_Delete_Account_Confirmation_Validation removed on request.
    // It was the non-destructive complement to AuthenticationTest.TC004, checking the
    // confirm button's enabled state without ever confirming the deletion. Worth
    // recording that the app's validation is correct - verified from the tree, the button
    // is enabled=false while the typed text is wrong - so this was never a product
    // defect; the test's own enabled-check was reading the Danger Zone link behind the
    // dialog instead of the dialog's button.

    /** Settings' "Personal Profile" row routes to the same UserProfileScreen used for
     * viewing other users (confirmed live via DevoteeSettingsNavGraph.kt - it navigates
     * to Screen.UserProfile with your own userId), which carries a real Share action. */
    @Test
    public void TC118_Verify_Share_Profile_Opens_Native_Share_Sheet() {
        ProfilePage profilePage = new ProfilePage(driver);

        Assert.assertTrue("Settings hub should open", profilePage.openSettingsHub());
        Assert.assertTrue("Personal Profile should open", profilePage.openPersonalProfile());

        Assert.assertTrue("Share action should be tappable", profilePage.tapShareProfile());
        Assert.assertTrue("Native share sheet should open", profilePage.isNativeShareSheetVisible());

        Assert.assertTrue("Share sheet should be dismissible", profilePage.dismissShareSheet());
    }
}
