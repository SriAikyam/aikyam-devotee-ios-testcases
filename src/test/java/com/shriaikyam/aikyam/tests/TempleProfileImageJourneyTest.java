package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.TempleHomePage;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Changing the temple's avatar and banner from the temple account's home (app issue #672).
 *
 * Needs the session already on a temple account (PriestTest.TC161, or switched by hand)
 * and at least one photo in the simulator's library.
 *
 * TC378/TC379 replace the temple's real avatar and banner with a library photo. There is
 * no API to restore the previous image, so they cannot undo the change; run them only on
 * the dev seed temple.
 */
public class TempleProfileImageJourneyTest extends BaseTest {

    private TempleHomePage templeHome;

    /** The temple account has no Home tab to reset to. */
    @Override
    protected boolean shouldResetToHome() {
        return false;
    }

    @Before
    public void openTempleHome() {
        templeHome = new TempleHomePage(driver);
        Assume.assumeTrue("Needs the session on a temple account (run PriestTest.TC161 first)",
                templeHome.isOnTempleAccount());
        Assert.assertTrue("Temple home should open with its edit buttons", templeHome.open());
    }

    @Test
    public void TC377_Verify_Temple_Avatar_And_Banner_Edit_Buttons_Shown() {
        Assert.assertTrue("Avatar camera badge should be shown", templeHome.isAvatarEditVisible());
        Assert.assertTrue("Banner camera button should be shown", templeHome.isBannerEditVisible());
    }

    @Test
    public void TC378_Verify_Picking_Temple_Avatar_Uploads_It() {
        Assert.assertTrue("A photo should be picked for the avatar", templeHome.changeAvatar());
        Assert.assertTrue("Avatar upload should finish without an error",
                templeHome.waitForUploadToFinish(true));
        Assert.assertFalse("No upload error should be shown", templeHome.isErrorShown());
    }

    @Test
    public void TC379_Verify_Picking_Temple_Banner_Uploads_It() {
        Assert.assertTrue("A photo should be picked for the banner", templeHome.changeBanner());
        Assert.assertTrue("Banner upload should finish without an error",
                templeHome.waitForUploadToFinish(false));
        Assert.assertFalse("No upload error should be shown", templeHome.isErrorShown());
    }
}
