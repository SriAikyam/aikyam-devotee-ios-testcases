package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.ExplorePage;
import com.shriaikyam.aikyam.pages.HomePage;
import com.shriaikyam.aikyam.pages.TempleDetailsPage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Temple profile (TempleProfileView), ported from the Android TempleDetailsTest on
 * aikyam-devotee-android-testcases@main (TC105-110, TC139-142).
 *
 * Two cases do not carry over, and are recorded here rather than silently dropped:
 *
 *  - TC109 (Donate visible) - iOS hides the button behind
 *    `templeProfileDonateButtonVisible = false` in TempleProfileView. The wiring is intact,
 *    so this is @Ignore'd rather than deleted; flip the flag and remove the annotation.
 *  - TC140 (Live tab) - iOS has no Live tab at all. TempleProfileView.TempleTab is
 *    About / Gallery / Posts / Sevas. Android's own TC140 already Assume-skips when a temple
 *    exposes no Live tab; on iOS it is not a sometimes-missing tab, it does not exist, so the
 *    test is not applicable and is not ported.
 *
 * iOS also has a Gallery tab with no Android counterpart - covered by TC105 below.
 */
public class TempleDetailsTest extends BaseTest {

    private HomePage homePage;
    private ExplorePage explorePage;

    @Before
    public void openExplore() {
        homePage = new HomePage(driver);
        explorePage = new ExplorePage(driver);
        homePage.tapExploreTab();
        explorePage.waitUntilLoaded();
        explorePage.switchToListView();
    }

    /** Opens the first temple in the Explore list, or fails with a readable reason. */
    private TempleDetailsPage openFirstTemple() {
        Assert.assertTrue("A temple row should be tappable in the Explore list",
                explorePage.openFirstTempleFromList());
        TempleDetailsPage templePage = new TempleDetailsPage(driver);
        Assert.assertTrue("Temple profile should open", templePage.isLoaded());
        return templePage;
    }

    @Test
    public void TC105_Verify_Temple_Details_Opens_With_Tabs() {
        TempleDetailsPage templePage = openFirstTemple();
        Assert.assertTrue("About tab should be present", templePage.isTabVisible("about"));
        Assert.assertTrue("Posts tab should be present", templePage.isTabVisible("posts"));
        // iOS-only tab, no Android counterpart.
        Assert.assertTrue("Gallery tab should be present on iOS", templePage.isTabVisible("gallery"));
    }

    @Test
    public void TC106_Verify_Tabs_Switch_Between_Posts_And_About() {
        TempleDetailsPage templePage = openFirstTemple();
        Assert.assertTrue("Posts tab should be tappable", templePage.openTab("posts"));
        Assert.assertFalse("Posts tab should replace the About content",
                templePage.isAboutContentVisible());
        Assert.assertTrue("About tab should be tappable", templePage.openTab("about"));
        Assert.assertTrue("About content should return", templePage.isAboutContentVisible());
    }

    @Test
    public void TC107_Verify_Follow_Then_Unfollow_State_Toggle() {
        TempleDetailsPage templePage = openFirstTemple();
        Assert.assertTrue("Follow control should be visible", templePage.isFollowActionVisible());
        String before = templePage.followLabel();
        Assert.assertNotNull("Follow control should expose its state as a label", before);

        Assert.assertTrue("Follow should be tappable", templePage.tapFollow());
        boolean wasFollowing = before != null && before.toLowerCase().contains("following");
        if (wasFollowing) {
            // Already following: the tap raises the unfollow sheet rather than toggling.
            Assert.assertTrue("Unfollow confirmation should appear", templePage.isUnfollowSheetVisible());
            Assert.assertTrue("Cancel should close the sheet", templePage.cancelUnfollow());
        } else {
            Assert.assertTrue("Label should change once followed",
                    waitForLabelChange(templePage, before));
            // Leave the temple as found.
            templePage.tapFollow();
            if (templePage.isUnfollowSheetVisible()) {
                templePage.confirmUnfollow();
            }
        }
    }

    @Test
    public void TC108_Verify_Unfollow_Sheet_Cancel_Keeps_Following() {
        TempleDetailsPage templePage = openFirstTemple();
        String before = templePage.followLabel();
        Assert.assertNotNull("Follow control should expose its state", before);

        if (!before.toLowerCase().contains("following")) {
            Assert.assertTrue("Follow should be tappable", templePage.tapFollow());
            Assert.assertTrue("Should reach the Following state first",
                    waitForLabelChange(templePage, before));
        }
        try {
            Assert.assertTrue("Tapping Following should raise the unfollow sheet",
                    templePage.tapFollow() && templePage.isUnfollowSheetVisible());
            Assert.assertTrue("Cancel should be tappable", templePage.cancelUnfollow());
            Assert.assertTrue("Cancelling must leave the temple followed",
                    templePage.followLabel() != null
                            && templePage.followLabel().toLowerCase().contains("following"));
        } finally {
            if (!before.toLowerCase().contains("following")) {
                templePage.tapFollow();
                if (templePage.isUnfollowSheetVisible()) {
                    templePage.confirmUnfollow();
                }
            }
        }
    }

    @Ignore("iOS hides Donate behind templeProfileDonateButtonVisible = false in "
            + "TempleProfileView - the wiring is intact, so this is kept rather than deleted. "
            + "Flip that flag and remove this annotation when Hundi returns.")
    @Test
    public void TC109_Verify_Donate_Action_Visible_Only() {
        // Intentionally empty: see @Ignore. Android asserts the Donate button renders.
    }

    @Test
    public void TC110_Verify_Back_Navigation_Returns_To_Explore() {
        TempleDetailsPage templePage = openFirstTemple();
        Assert.assertTrue("Back should be tappable", templePage.tapBack());
        Assert.assertTrue("Back should land on Explore", explorePage.isExploreVisible());
    }

    @Test
    public void TC139_Verify_Sevas_Tab_Opens_And_Renders_Its_Own_Content() {
        TempleDetailsPage templePage = openFirstTemple();
        Assert.assertTrue("Sevas tab should be present", templePage.isTabVisible("sevas"));
        Assert.assertTrue("Sevas tab should be tappable", templePage.openTab("sevas"));
        Assert.assertFalse("Sevas tab should replace the About content",
                templePage.isAboutContentVisible());
    }

    @Test
    public void TC141_Verify_About_Read_More_Expands_And_Read_Less_Collapses() {
        TempleDetailsPage templePage = openFirstTemple();
        Assert.assertTrue("About tab should be tappable", templePage.openTab("about"));
        // Only temples with a long history render the toggle at all.
        if (!templePage.isReadToggleVisible()) {
            return;
        }
        String collapsed = templePage.readToggleLabel();
        Assert.assertTrue("Collapsed state should offer Read more",
                collapsed != null && collapsed.toLowerCase().contains("read more"));
        Assert.assertTrue("Read more should be tappable", templePage.tapReadToggle());
        Assert.assertTrue("Expanding should offer Read less",
                waitForReadToggle(templePage, "read less"));
        Assert.assertTrue("Read less should be tappable", templePage.tapReadToggle());
        Assert.assertTrue("Collapsing should restore Read more",
                waitForReadToggle(templePage, "read more"));
    }

    @Test
    public void TC142_Verify_Tab_Selection_Resets_To_About_After_Leaving_And_Returning() {
        TempleDetailsPage templePage = openFirstTemple();
        Assert.assertTrue("Posts tab should be tappable", templePage.openTab("posts"));
        Assert.assertTrue("Back should be tappable", templePage.tapBack());
        Assert.assertTrue("Back should land on Explore", explorePage.isExploreVisible());

        TempleDetailsPage reopened = openFirstTemple();
        Assert.assertTrue("Reopening should land on About again",
                reopened.isAboutContentVisible());
    }

    /** The follow label is written after the server round-trip, so poll rather than read once. */
    private boolean waitForLabelChange(TempleDetailsPage page, String previous) {
        for (int attempt = 0; attempt < 15; attempt++) {
            String now = page.followLabel();
            if (now != null && !now.equals(previous)) {
                return true;
            }
        }
        return false;
    }

    private boolean waitForReadToggle(TempleDetailsPage page, String expectedFragment) {
        for (int attempt = 0; attempt < 10; attempt++) {
            String now = page.readToggleLabel();
            if (now != null && now.toLowerCase().contains(expectedFragment)) {
                return true;
            }
        }
        return false;
    }
}
