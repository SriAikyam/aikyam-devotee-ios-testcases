package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.AppUpdatePage;
import io.appium.java_client.appmanagement.ApplicationState;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.time.Duration;

/** The update prompt, aikyam-devotee-ios#716 (#656): checked on cold launch and on every return
 * from the background, logged in or not.
 *
 * Whether a prompt shows depends on the backend's iOS update config, which is shared by everyone
 * on dev. These tests never change it. QA saves the config first and says which state it is in:
 *
 *   -Dappupdate.expect=forced     forceUpdateUpToVersionCode >= the installed build
 *   -Dappupdate.expect=optional   latestVersionCode > the installed build > forceUpdateUpToVersionCode
 *   -Dappupdate.expect=none       latestVersionCode <= the installed build
 *
 * Every case is skipped unless its state is passed, so a normal full run skips the whole class.
 *
 * Run this class on its own: a forced prompt covers the app, so every other class would fail.
 * The app can be logged in or out; the prompt doesn't depend on it, so QA can run each state
 * from both.
 *
 * Name-ordered: TC387 must run before TC388, which leaves the optional prompt suppressed. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AppUpdateTest extends BaseTest {

    private static final String EXPECT = System.getProperty("appupdate.expect", "");
    /** Long enough for the launch screen plus a slow dev response. */
    private static final Duration NO_PROMPT_WINDOW = Duration.ofSeconds(20);

    /** Skips the class before BaseTest opens a session for each case, when no state is given. */
    @BeforeClass
    public static void requireConfigState() {
        Assume.assumeTrue("Needs -Dappupdate.expect=forced|optional|none (see the class notes)",
                EXPECT.equals("forced") || EXPECT.equals("optional") || EXPECT.equals("none"));
    }

    /** BaseTest's reset taps the Home tab, which a forced prompt covers (and a logged-out app
     * doesn't have). Each case here relaunches the app itself instead. */
    @Override
    protected boolean shouldResetToHome() {
        return false;
    }

    // ---------------- Forced ----------------

    @Test
    public void TC383_Forced_Prompt_Shows_On_Cold_Launch_With_Only_Update_Now() {
        assumeExpect("forced");
        AppUpdatePage page = relaunch();

        Assert.assertTrue("'Update Required' should show on cold launch", page.waitForForcedPrompt());
        Assert.assertTrue("Update Now should be offered", page.isUpdateNowVisible());
        Assert.assertFalse("A forced prompt must not offer Remind Me Later or Skip",
                page.isRemindLaterOrSkipVisibleNow());
    }

    @Test
    public void TC384_Forced_Prompt_Cannot_Be_Swiped_Away() {
        assumeExpect("forced");
        AppUpdatePage page = relaunch();
        Assert.assertTrue("'Update Required' should show on cold launch", page.waitForForcedPrompt());

        Assert.assertTrue("Should be able to drag the prompt", page.swipePromptDown());
        Assert.assertTrue("A forced prompt must stay after a swipe down", page.isForcedPromptVisible());
    }

    @Test
    public void TC385_Forced_Prompt_Still_Shows_After_Background_And_Return() {
        assumeExpect("forced");
        AppUpdatePage page = relaunch();
        Assert.assertTrue("'Update Required' should show on cold launch", page.waitForForcedPrompt());

        driver.runAppInBackground(Duration.ofSeconds(5));

        Assert.assertTrue("'Update Required' should still show after returning from the background",
                page.waitForForcedPrompt());
    }

    /** Update Now opens the App Store (on a simulator, which has none, nothing happens). Coming
     * back without updating must not get the user past a forced prompt. */
    @Test
    public void TC386_Forced_Prompt_Stays_After_Update_Now() {
        assumeExpect("forced");
        AppUpdatePage page = relaunch();
        Assert.assertTrue("'Update Required' should show on cold launch", page.waitForForcedPrompt());

        Assert.assertTrue("Update Now should be tappable", page.tapUpdateNow());
        returnToAppIfItLeft();

        Assert.assertTrue("'Update Required' should still show after Update Now", page.waitForForcedPrompt());
    }

    // ---------------- Optional ----------------

    @Test
    public void TC387_Optional_Prompt_Shows_On_Cold_Launch_With_All_Choices() {
        assumeExpect("optional");
        AppUpdatePage page = relaunch();

        Assert.assertTrue("'Update Available' should show on cold launch "
                        + "(not shown: the device may still have it suppressed from an earlier TC388, "
                        + "see the class notes)",
                page.waitForOptionalPrompt());
        Assert.assertTrue("Update Now should be offered", page.isUpdateNowVisible());
        Assert.assertTrue("Remind Me Later should be offered", page.isRemindLaterVisible());
        Assert.assertTrue("Skip This Version should be offered", page.isSkipVisible());
    }

    /** #716 review fix: the prompt used to come straight back when the user returned from the
     * App Store, because every return from the background re-checks.
     *
     * Leaves the optional prompt suppressed on this device for 24 hours. To run TC387 again
     * sooner, reinstall the app or raise latestVersionCode. */
    @Test
    public void TC388_Optional_Prompt_Does_Not_Return_After_Update_Now() {
        assumeExpect("optional");
        AppUpdatePage page = relaunch();
        Assert.assertTrue("'Update Available' should show on cold launch", page.waitForOptionalPrompt());

        Assert.assertTrue("Update Now should be tappable", page.tapUpdateNow());
        returnToAppIfItLeft();
        Assert.assertTrue("The optional prompt should close after Update Now", page.waitUntilPromptGone());

        driver.runAppInBackground(Duration.ofSeconds(5));
        Assert.assertFalse("The optional prompt must not come back on the next return to the app",
                page.anyPromptAppearsWithin(NO_PROMPT_WINDOW));
    }

    // ---------------- Up to date ----------------

    @Test
    public void TC389_No_Prompt_When_Up_To_Date() {
        assumeExpect("none");
        AppUpdatePage page = relaunch();

        Assert.assertFalse("No update prompt should show on cold launch when the build is current",
                page.anyPromptAppearsWithin(NO_PROMPT_WINDOW));

        driver.runAppInBackground(Duration.ofSeconds(5));
        Assert.assertFalse("No update prompt should show after returning from the background",
                page.anyPromptAppearsWithin(NO_PROMPT_WINDOW));
    }

    // ---------------- Helpers ----------------

    private static void assumeExpect(String state) {
        Assume.assumeTrue("Needs the dev update config in state '" + state
                        + "', saved by QA and passed as -Dappupdate.expect=" + state,
                state.equals(EXPECT));
    }

    /** A cold launch: the check runs from the launch task, not a resume. */
    private AppUpdatePage relaunch() {
        driver.terminateApp(bundleId());
        driver.activateApp(bundleId());
        return new AppUpdatePage(driver);
    }

    /** On a device Update Now leaves for the App Store; bring the app back as the user would. */
    private void returnToAppIfItLeft() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        if (driver.queryAppState(bundleId()) != ApplicationState.RUNNING_IN_FOREGROUND) {
            driver.activateApp(bundleId());
        }
    }
}
