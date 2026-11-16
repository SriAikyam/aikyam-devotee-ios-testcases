package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.LiveChantPage;
import com.shriaikyam.aikyam.pages.MusicPage;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;

/**
 * Live Chant on iOS, with the app's waiting step:
 * Start Live Chant -> Start -> Waiting -> host taps Start Chant -> LIVE.
 *
 * Opt-in with -Dlivechant.enabled=true: every case starts a real chant on the shared dev backend.
 * Run it on ONE shard only (never split across -Dappium.worker shards): shards on the same login
 * hit the "one active chant per host" limit, and one shard's cleanup would end the other's chant.
 *
 * Needs a logged-in dev build with Live Chant, a japa track (-Dlivechant.japaTrack) and a non-japa
 * track (-Dlivechant.otherTrack) on Music home. "Live Chant as a participant" needs a second
 * logged-in device and is covered by the app's unit tests.
 */
public class LiveChantTest extends BaseTest {

    private static final String JAPA_TRACK = System.getProperty("livechant.japaTrack", "Gayatri Mantra");
    private static final String OTHER_TRACK = System.getProperty("livechant.otherTrack", "Shiva Tandav Stotram");
    /** Same value as BasePage.DEFAULT_TIMEOUT, kept local so that stays protected. */
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(30);
    /** Longer than the position label could stay put while really playing (it ticks every second). */
    private static final Duration PAUSE_HOLD = Duration.ofSeconds(8);

    private LiveChantPage chant;

    /**
     * Never leave a chant running for other devotees if a step failed mid-way. A rule rather than
     * an @After so a failure is captured BEFORE the chant is ended: BaseTest's diagnosticsWatcher
     * only fires after @After, by which time the screen would show the cleaned-up state. Order 1
     * puts this inside diagnosticsWatcher, so the driver is still alive here.
     */
    @Rule(order = 1)
    public TestRule endChantAfterTest = (base, description) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            try {
                base.evaluate();
            } catch (AssumptionViolatedException skipped) {
                throw skipped; // a skip is not a failure: no diagnostics
            } catch (Throwable failure) {
                captureFailureDiagnostics(Description.createTestDescription(
                        description.getTestClass(), description.getMethodName() + "_before_cleanup"), failure);
                throw failure;
            } finally {
                endChantIfStillLive();
            }
        }
    };

    @BeforeClass
    public static void optIn() {
        Assume.assumeTrue("Skipping: Live Chant starts real chants on the shared dev backend - run it with"
                + " -Dlivechant.enabled=true, on one shard only", Boolean.getBoolean("livechant.enabled"));
    }

    @Before
    public void enterMusic() {
        chant = new LiveChantPage(driver);
        endChantIfStillLive(); // a chant left by an earlier failed run would block Start (ACTIVE_SESSION_EXISTS)
        // A failure, not a skip: a logged-in build is a prerequisite, and a skip would keep a broken run green.
        Assert.assertTrue("The Music tab is not reachable (is the dev build logged in?)", chant.openMusicTab());
    }

    private void endChantIfStillLive() {
        if (chant == null) {
            return; // setUp failed before the page existed
        }
        try {
            endOwnChantIfAny();
        } catch (RuntimeException cleanupFailure) {
            // Not rethrown: the test body's assertions are what report. But say so - a chant left
            // live here blocks the next Start and shows to every devotee on the dev backend.
            warn("could not end this device's Live Chant: " + cleanupFailure);
        }
    }

    @Test
    public void TC355_Verify_Host_Starts_A_Live_Chant_From_The_Player_And_Ends_It() {
        startChantFromPlayer(JAPA_TRACK);
        endChantFromScreen();
    }

    @Test
    public void TC358_Verify_Cancelling_The_Start_Sheet_Starts_Nothing() {
        openPlayer(JAPA_TRACK);
        Assert.assertTrue("The player options menu should open", chant.openPlayerOptions());
        Assert.assertTrue("Start Live Chant should be tappable", chant.tapOption(LiveChantPage.START_OPTION));
        Assert.assertTrue("The start sheet should show", waitUntil(() -> LiveChantPage.START_TITLE.equals(chant.sheetTitle())));
        Assert.assertTrue("Cancel should be tappable", chant.cancelSheet());
        Assert.assertTrue("The sheet should close", chant.isSheetGone());
        Assert.assertTrue("Still on the normal player", chant.isPlayerVisible());
        Assert.assertFalse("No Live Chant screen", chant.isEndVisible());

        // Nothing started anywhere: Music home has no Live now card and no LIVE mini player.
        chant.closePlayerIfOpen();
        Assert.assertTrue("The Music tab should open", chant.openMusicTab());
        Assert.assertNull("No Live now card for " + JAPA_TRACK, chant.cardAction(JAPA_TRACK));
        Assert.assertFalse("The mini player is not LIVE", chant.isMiniPlayerLive());
    }

    /** The keep-screen-on toggle is gone on iOS: the screen always stays on. */
    @Test
    public void TC359_Verify_Host_Controls_Pause_Resume_Mute_And_Who_Is_Chanting() {
        startChantFromPlayer(JAPA_TRACK);
        Assert.assertEquals("The chant shows the shared track", JAPA_TRACK, chant.titleText());
        String startedBy = chant.startedByText();
        Assert.assertTrue("The chant shows who started it (was " + startedBy + ")",
                startedBy != null && startedBy.matches("Started by \\S.*"));
        Assert.assertTrue(JAPA_TRACK + " is a japa track, so the counter should show", chant.isJapaVisible());
        Assert.assertTrue("The japa counter starts at the first repetition", chant.japaRepetition() >= 1);

        // Pause must stop the chant, not just relabel the badge: the position moves while LIVE...
        int livePosition = chant.positionSeconds();
        Assert.assertTrue("The chant shows its position (was " + livePosition + ")", livePosition >= 0);
        Assert.assertTrue("The position should advance while LIVE",
                waitUntil(() -> chant.positionSeconds() > livePosition));

        Assert.assertEquals("Pause is offered while live", "Pause", chant.primaryText());
        Assert.assertTrue("Pause should be tappable", chant.tapPrimary());
        Assert.assertTrue("The badge should change to Paused", chant.waitForBadge("Paused"));
        Assert.assertEquals("Pause becomes Resume", "Resume", chant.primaryText());
        // ...and holds while Paused (one tick of slack for the label catching up with the pause).
        int pausedPosition = chant.positionSeconds();
        int pausedRepetition = chant.japaRepetition();
        Assert.assertFalse("The position should hold while Paused",
                waitUntil(PAUSE_HOLD, () -> chant.positionSeconds() > pausedPosition + 1));
        Assert.assertEquals("The japa counter should hold while Paused", pausedRepetition, chant.japaRepetition());

        Assert.assertTrue("Resume should be tappable", chant.tapPrimary());
        Assert.assertTrue("The badge should be LIVE again", chant.waitForBadge("LIVE"));
        Assert.assertEquals("Resume becomes Pause", "Pause", chant.primaryText());
        int resumedPosition = chant.positionSeconds();
        Assert.assertTrue("The position should advance again after Resume",
                waitUntil(() -> chant.positionSeconds() > resumedPosition));

        Assert.assertEquals("Mute is offered", "Mute audio", chant.muteLabel());
        Assert.assertTrue("Mute should be tappable", chant.tapMute());
        Assert.assertTrue("The muted note should show", chant.isMutedNoteVisible());
        Assert.assertEquals("The button now offers Unmute", "Unmute audio", chant.muteLabel());
        Assert.assertTrue("Unmute should be tappable", chant.tapMute());
        Assert.assertTrue("The muted note should go", chant.isMutedNoteGone());

        String who = chant.participantsLabel();
        Assert.assertTrue("The count says how many are chanting (was " + who + ")",
                who != null && who.matches("\\d+ devotees? chanting"));
        Assert.assertTrue("Who is chanting should open", chant.openParticipants());
        Assert.assertTrue("'Chanting now' sheet should open", chant.isWhoSheetVisible());
        Assert.assertTrue("The host is tagged in the list", chant.isHostTagVisible());
        Assert.assertTrue("The 'Chanting now' sheet should close", chant.closeWhoSheet());
        Assert.assertTrue("Still on the Live Chant screen", chant.isScreenVisible());

        endChantFromScreen();
    }

    /** The chant outlives the screen and the app process. */
    @Test
    public void TC360_Verify_Live_Now_Card_And_LIVE_Mini_Player_Bring_The_Host_Back() {
        startChantFromPlayer(JAPA_TRACK);
        Assert.assertTrue("Back should collapse the Live Chant screen", chant.tapBack());
        Assert.assertTrue("The Live Chant screen should collapse", chant.isGone());

        Assert.assertEquals("Own chant's card says Open (not Join Chant)", "Open", waitForCard(JAPA_TRACK));
        Assert.assertTrue("Mini player shows the LIVE badge", chant.isMiniPlayerLive());

        Assert.assertTrue("Open on the card should be tappable", chant.tapCard(JAPA_TRACK));
        Assert.assertTrue("The card reopens the chant", waitUntil(chant::isEndVisible));
        Assert.assertEquals("It is this device's " + JAPA_TRACK + " chant", JAPA_TRACK, chant.titleText());

        Assert.assertTrue("Back should collapse it again", chant.tapBack() && chant.isGone());
        Assert.assertTrue("The LIVE mini player should be tappable", chant.tapMiniPlayerLive());
        Assert.assertTrue("The mini player reopens the chant", waitUntil(chant::isEndVisible));

        // The chant outlives the app process: a cold start still shows it, and the host can end it.
        driver.terminateApp(bundleId());
        driver.activateApp(bundleId());
        Assert.assertTrue("The Music tab should open after a cold start", chant.openMusicTab());
        Assert.assertEquals("The " + JAPA_TRACK + " chant is still live after a restart", "Open", waitForCard(JAPA_TRACK));
        Assert.assertTrue("Reopen the chant after the restart", chant.tapCard(JAPA_TRACK));
        Assert.assertTrue("Host controls are back after the restart", waitUntil(chant::isEndVisible));
        endChantFromScreen();
    }

    @Test
    public void TC361_Verify_Playing_Another_Track_Asks_To_End_The_Chant() {
        startChantFromPlayer(JAPA_TRACK);
        Assert.assertTrue("Back should collapse the Live Chant screen", chant.tapBack() && chant.isGone());

        Assert.assertTrue(OTHER_TRACK + " should be on Music home (change it with -Dlivechant.otherTrack)",
                chant.openTrack(OTHER_TRACK));
        Assert.assertTrue("Asked to end the chant first",
                waitUntil(() -> LiveChantPage.SWITCH_TITLE.equals(chant.sheetTitle())));
        assertSheetExplains("The sheet warns it ends for everyone");
        Assert.assertTrue("Cancel should be tappable", chant.cancelSheet());
        Assert.assertTrue("The sheet should close", chant.isSheetGone());
        chant.closePlayerIfOpen();
        Assert.assertEquals("The chant is still running (" + JAPA_TRACK + " card)", "Open", waitForCard(JAPA_TRACK));
        Assert.assertTrue("Mini player still LIVE", chant.isMiniPlayerLive());

        Assert.assertTrue("Tap the other track again", chant.openTrack(OTHER_TRACK));
        Assert.assertTrue("Asked again", waitUntil(() -> LiveChantPage.SWITCH_TITLE.equals(chant.sheetTitle())));
        Assert.assertTrue("End Chant should be tappable", chant.confirmSheet());
        Assert.assertTrue("No second confirmation", chant.isSheetGone());

        // Ending the chant must also hand over to the track that was tapped, and play it.
        MusicPage music = new MusicPage(driver);
        Assert.assertTrue("The full player should open for " + OTHER_TRACK,
                waitUntil(() -> OTHER_TRACK.equals(music.fullPlayerTitle())));
        Assert.assertTrue(OTHER_TRACK + " should be playing", music.waitForPlaybackPlaying());
        Assert.assertTrue(OTHER_TRACK + "'s position should advance",
                music.hasPositionAdvanced(music.currentPositionText()));

        chant.closePlayerIfOpen();
        Assert.assertTrue("The " + JAPA_TRACK + " card is gone", chant.isCardGone(JAPA_TRACK));
        Assert.assertTrue("Mini player is no longer LIVE", chant.isMiniPlayerLiveGone());
    }

    /** A chant on a non-japa track: no count picker on the start sheet and no japa counter on screen. */
    @Test
    public void TC368_Verify_A_Non_Japa_Live_Chant_Has_No_Japa_Counter() {
        openPlayer(OTHER_TRACK);
        Assert.assertTrue("The player options menu should open", chant.openPlayerOptions());
        Assert.assertTrue("Start Live Chant should be tappable", chant.tapOption(LiveChantPage.START_OPTION));
        Assert.assertTrue("The start sheet should show", waitUntil(() -> LiveChantPage.START_TITLE.equals(chant.sheetTitle())));
        Assert.assertFalse(OTHER_TRACK + " is not a japa track (change it with -Dlivechant.otherTrack),"
                + " so the start sheet has no count picker", chant.isCountPickerVisible());
        Assert.assertTrue("Start should be tappable", chant.confirmSheet());
        startFromWaitingScreen();

        Assert.assertEquals("The chant shows the shared track", OTHER_TRACK, chant.titleText());
        Assert.assertFalse("A non-japa chant has no japa counter", chant.isJapaVisible());
        endChantFromScreen(OTHER_TRACK);
    }

    /** A real Wi-Fi to cellular handover. XCUITest can't toggle Wi-Fi, and a simulator shares the
     * Mac's network. An Assume, not @Ignore: Gradle's JUnit XML keeps an assumption's message but
     * drops an @Ignore reason, and the regression report lists skip reasons. */
    @Test
    public void TC356_Verify_Live_Chant_Survives_A_WiFi_To_Cellular_Switch() {
        Assume.assumeTrue("Skipping: not automatable on iOS - check by hand on a real iPhone: start a chant on"
                + " Wi-Fi, turn Wi-Fi off, the chant reconnects on cellular and Pause/Resume still work", false);
    }

    /**
     * The host backgrounds the app for -Dlivechant.backgroundMinutes (default 30). The chant must
     * still be live when the app comes back, and host controls must still work. Long-running: only
     * with -Dlivechant.long=true (a quick check of the path: -Dlivechant.backgroundMinutes=2).
     */
    @Test
    public void TC357_Verify_Live_Chant_Keeps_Going_With_The_App_In_The_Background() {
        Assume.assumeTrue("Skipping: long test, run with -Dlivechant.long=true", Boolean.getBoolean("livechant.long"));
        int minutes = Integer.getInteger("livechant.backgroundMinutes", 30);
        startChantFromPlayer(JAPA_TRACK);
        // -1 keeps the app in the background until activateApp. A loop of runAppInBackground(60s) would
        // bring it back every minute, so iOS would never suspend it for the full period.
        driver.runAppInBackground(Duration.ofSeconds(-1));
        Instant backAt = Instant.now().plus(Duration.ofMinutes(minutes));
        new WebDriverWait(driver, Duration.ofMinutes(minutes + 2), Duration.ofSeconds(60)).until(ignored -> {
            driver.queryAppState(bundleId()); // one command a minute keeps the Appium session alive
            return !Instant.now().isBefore(backAt);
        });
        driver.activateApp(bundleId());
        Assert.assertTrue("The chant should still be LIVE after " + minutes + " min in the background",
                waitUntil(chant::isScreenVisible) && "LIVE".equals(chant.badgeText()));
        Assert.assertTrue("Pause should still reach the server", chant.tapPrimary() && chant.waitForBadge("Paused"));
        Assert.assertTrue("Resume should still reach the server", chant.tapPrimary() && chant.waitForBadge("LIVE"));
        endChantFromScreen();
    }

    // --- helpers ---------------------------------------------------------------------------

    private void openPlayer(String track) {
        Assert.assertTrue(track + " should be on Music home (change it with -Dlivechant.japaTrack / .otherTrack)",
                chant.openTrack(track));
        Assert.assertTrue("The full player should open", waitUntil(chant::isPlayerVisible));
    }

    /** Opens the track, starts a Live Chant from the player menu, then starts it from the waiting screen. */
    private void startChantFromPlayer(String track) {
        openPlayer(track);
        Assert.assertTrue("The player options menu should open", chant.openPlayerOptions());
        Assert.assertTrue("Options should offer Start Live Chant", chant.isOptionVisible(LiveChantPage.START_OPTION));
        Assert.assertTrue("Start Live Chant should be tappable", chant.tapOption(LiveChantPage.START_OPTION));

        Assert.assertTrue("The start sheet should show", waitUntil(() -> LiveChantPage.START_TITLE.equals(chant.sheetTitle())));
        assertSheetExplains("The sheet should explain the chant");
        Assert.assertTrue("Start should be tappable", chant.confirmSheet());
        startFromWaitingScreen();
    }

    /** The chant waits at 0:00 so the host can share it first; Start Chant makes it LIVE. */
    private void startFromWaitingScreen() {
        Assert.assertTrue("The Live Chant screen should open waiting", chant.waitForBadge("Waiting"));
        Assert.assertTrue("The host is told it starts when they tap Start Chant", chant.isWaitingNoteVisible());
        Assert.assertEquals("The host is offered Start Chant", "Start Chant", chant.primaryText());
        Assert.assertTrue("Start Chant should be tappable", chant.tapPrimary());
        Assert.assertTrue("The Live Chant screen should show the LIVE badge", chant.waitForBadge("LIVE"));
        Assert.assertTrue("The host should see End Chant", chant.isEndVisible());
    }

    private void endChantFromScreen() {
        endChantFromScreen(JAPA_TRACK);
    }

    /** Ends the chant from its screen and checks it really ended: the screen closing is not proof,
     * Music home must drop the Live now card and the LIVE mini player too. */
    private void endChantFromScreen(String track) {
        Assert.assertTrue("End Chant should be tappable", chant.tapEnd());
        Assert.assertTrue("The end confirmation should show", waitUntil(() -> LiveChantPage.END_TITLE.equals(chant.sheetTitle())));
        assertSheetExplains("It warns it ends for everyone");
        Assert.assertTrue("Confirm End Chant should be tappable", chant.confirmSheet());
        Assert.assertTrue("The chant should end (screen closes)", chant.isGone());

        chant.closePlayerIfOpen();
        Assert.assertTrue("The Music tab should open", chant.openMusicTab());
        Assert.assertTrue("The Live now card should be gone", chant.isCardGone(track));
        Assert.assertTrue("Mini player is no longer LIVE", chant.isMiniPlayerLiveGone());
    }

    private void assertSheetExplains(String message) {
        String body = chant.sheetBody();
        Assert.assertTrue(message + " (was " + body + ")", body != null && !body.isBlank());
    }

    /** Ends only a chant THIS device is hosting: its open screen, its LIVE mini player, or its own (Open) card. */
    private void endOwnChantIfAny() {
        // A test that failed with a sheet up: dismiss it first, or the taps below land behind it.
        if (chant.sheetTitle() != null) {
            chant.cancelSheet();
            chant.isSheetGone();
        }
        if (chant.isWhoSheetVisible()) {
            chant.closeWhoSheet();
        }
        if (!chant.isEndVisible()) {
            chant.openMusicTab();
            // Wait for the chant screen only after tapping into it; with nothing to end, return at once.
            if (chant.isMiniPlayerLive()) {
                if (chant.tapMiniPlayerLive()) {
                    waitUntil(chant::isEndVisible);
                }
            } else if ("Open".equals(chant.cardAction(JAPA_TRACK))) {
                if (chant.tapCard(JAPA_TRACK)) {
                    waitUntil(chant::isEndVisible);
                }
            } else if ("Open".equals(chant.cardAction(OTHER_TRACK))) {
                if (chant.tapCard(OTHER_TRACK)) {
                    waitUntil(chant::isEndVisible);
                }
            } else {
                return; // nothing of ours is live
            }
        }
        boolean ended = chant.isEndVisible()
                && chant.tapEnd()
                && waitUntil(() -> LiveChantPage.END_TITLE.equals(chant.sheetTitle()))
                && chant.confirmSheet()
                && chant.isGone();
        if (!ended) {
            warn("this device's Live Chant may still be live on the dev backend - End it by hand");
        }
    }

    private static void warn(String message) {
        System.out.println("[LiveChantTest] cleanup WARNING: " + message);
    }

    private String waitForCard(String trackTitle) {
        try {
            return new WebDriverWait(driver, WAIT_TIMEOUT).until(ignored -> chant.cardAction(trackTitle));
        } catch (TimeoutException timeout) {
            return null;
        }
    }

    private boolean waitUntil(BooleanSupplier condition) {
        return waitUntil(WAIT_TIMEOUT, condition);
    }

    private boolean waitUntil(Duration timeout, BooleanSupplier condition) {
        try {
            return new WebDriverWait(driver, timeout).until(ignored -> condition.getAsBoolean());
        } catch (TimeoutException timedOut) {
            return false;
        }
    }
}
