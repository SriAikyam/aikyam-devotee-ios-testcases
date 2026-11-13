package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Live Chant (aikyam-devotee-ios branch feature/live-chant-ios, port of Android app PR #667).
 * Live Chant controls use the `livechant.*` identifiers from LiveChantViews.swift; getting to a
 * track uses visible labels (Music tab, track title, the player's "More options"), so these
 * tests run on a plain feature build without the `music.*` identifiers of qa/appium-ios-tests.
 */
public class LiveChantPage extends BasePage {

    public static final String START_OPTION = "Start Live Chant";
    public static final String START_TITLE = "Start a Live Chant?";
    public static final String START_BODY =
            "Share it so other devotees can join. It starts for everyone when you tap Start Chant.";
    public static final String END_TITLE = "End this Live Chant?";
    public static final String SWITCH_TITLE = "End your Live Chant to play this?";
    public static final String END_BODY = "It ends for everyone chanting with you.";

    private final By badge = id("livechant.badge");
    private final By primary = id("livechant.primary");
    private final By end = id("livechant.end");
    private final By back = id("livechant.back");
    private final By mute = id("livechant.mute");
    private final By mutedNote = id("livechant.mutedNote");
    private final By waitingNote = id("livechant.waitingNote");
    private final By participants = id("livechant.participants");
    private final By whoTitle = id("livechant.who.title");
    private final By japa = id("livechant.japa");
    private final By title = id("livechant.title");
    private final By startedBy = id("livechant.startedBy");
    private final By sheetTitle = id("livechant.sheet.title");
    private final By sheetBody = id("livechant.sheet.body");
    private final By sheetConfirm = id("livechant.sheet.confirm");
    private final By sheetCancel = id("livechant.sheet.cancel");
    private final By miniLive = id("music.miniPlayer.live");
    private final By moreOptions = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND label == 'More options'");
    private final By musicTab = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND (name == 'tab.music' OR label == 'Music')");

    public LiveChantPage(IOSDriver driver) {
        super(driver);
    }

    // --- Getting to a track -------------------------------------------------------------

    public boolean openMusicTab() {
        return tapFirstVisible(musicTab);
    }

    /** Taps a track card on Music home by its title, scrolling down for it; false if absent. */
    public boolean openTrack(String trackTitle) {
        By card = AppiumBy.iOSNsPredicateString("label == " + predicateLiteral(trackTitle)
                + " AND NOT (name BEGINSWITH 'livechant.')");
        // Start from Music home: a previous test can leave the full player or a pushed screen up.
        closePlayerIfOpen();
        scrollBackUntilVisible(8, card);
        if (!scrollUntilVisible(8, card)) {
            return false;
        }
        return tapFirstVisible(card);
    }

    public boolean isPlayerVisible() {
        return isAnyVisible(moreOptions);
    }

    public boolean openPlayerOptions() {
        return tapFirstVisible(moreOptions);
    }

    public boolean isOptionVisible(String label) {
        return isAnyVisible(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == " + predicateLiteral(label)));
    }

    public boolean tapOption(String label) {
        return tapFirstVisible(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND label == " + predicateLiteral(label)));
    }

    // --- Sheets -------------------------------------------------------------------------

    public String sheetTitle() {
        return labelOf(sheetTitle);
    }

    public String sheetBody() {
        return labelOf(sheetBody);
    }

    public boolean confirmSheet() {
        return tapFirstVisible(sheetConfirm);
    }

    public boolean cancelSheet() {
        return tapFirstVisible(sheetCancel);
    }

    public boolean isSheetGone() {
        return waitUntilGone(sheetTitle);
    }

    // --- Live Chant screen --------------------------------------------------------------

    public boolean isScreenVisible() {
        return isAnyVisible(badge);
    }

    public String badgeText() {
        return labelOf(badge);
    }

    public String primaryText() {
        return labelOf(primary);
    }

    public boolean isWaitingNoteVisible() {
        return isAnyVisible(waitingNote);
    }

    public boolean tapPrimary() {
        return tapFirstVisible(primary);
    }

    public boolean isEndVisible() {
        return isVisibleNow(end);
    }

    public boolean tapEnd() {
        return tapFirstVisible(end);
    }

    public boolean tapBack() {
        return tapFirstVisible(back);
    }

    public boolean isGone() {
        return waitUntilGone(Duration.ofSeconds(15), end);
    }

    public String titleText() {
        return labelOf(title);
    }

    public String startedByText() {
        return labelOf(startedBy);
    }

    public String muteLabel() {
        return labelOf(mute);
    }

    public boolean tapMute() {
        return tapFirstVisible(mute);
    }

    public boolean isMutedNoteVisible() {
        return isAnyVisible(mutedNote);
    }

    public boolean isMutedNoteGone() {
        return waitUntilGone(mutedNote);
    }

    public String participantsLabel() {
        return labelOf(participants);
    }

    public boolean openParticipants() {
        return tapFirstVisible(participants);
    }

    public boolean isWhoSheetVisible() {
        return isAnyVisible(whoTitle);
    }

    public boolean isHostTagVisible() {
        // Exact label only: a CONTAINS match would also accept "Hosted by ..." or a name with "Host" in it.
        return isAnyVisible(text("Host"));
    }

    /** Japa "repetition / target", or -1 when this chant isn't a japa. */
    public int japaRepetition() {
        String value = isVisibleNow(japa) ? labelOf(japa) : null;
        if (value == null) {
            return -1;
        }
        Matcher m = Pattern.compile("^(\\d+) / \\d+$").matcher(value.trim());
        return m.matches() ? Integer.parseInt(m.group(1)) : -1;
    }

    public boolean isJapaVisible() {
        return isVisibleNow(japa);
    }

    // --- Music home ---------------------------------------------------------------------

    /** The "Live now" card's button for a chant on `trackTitle`: "Open" for one's own, else "Join Chant". */
    public String cardAction(String trackTitle) {
        By card = id("livechant.card." + trackTitle);
        scrollBackUntilVisible(6, card);
        return isVisibleNow(card) ? labelOf(card) : null;
    }

    public boolean tapCard(String trackTitle) {
        By card = id("livechant.card." + trackTitle);
        scrollBackUntilVisible(6, card);
        return tapFirstVisible(card);
    }

    public boolean isCardGone(String trackTitle) {
        return waitUntilGone(Duration.ofSeconds(10), id("livechant.card." + trackTitle));
    }

    public boolean isMiniPlayerLive() {
        return isAnyVisible(miniLive);
    }

    public boolean isMiniPlayerLiveGone() {
        return waitUntilGone(miniLive);
    }

    public boolean tapMiniPlayerLive() {
        return tapFirstVisible(miniLive);
    }

    /** Swipes a sheet down (no back button on an iOS sheet). */
    public void navigateBackFromSheet() {
        navigateBack();
        waitUntilGone(whoTitle);
    }

    /** The track tap opens the full player behind the switch sheet; close it to get back to Music home. */
    public void closePlayerIfOpen() {
        By close = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name == 'music.player.close' OR label == 'Close')");
        if (isVisibleNow(moreOptions)) {
            tapFirstVisible(close);
            waitUntilGone(moreOptions);
        }
    }

    /** Waits until the badge's label settles on `expected` (or times out). */
    public boolean waitForBadge(String expected) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20))
                    .until(ignored -> expected.equals(labelOf(badge)));
            return true;
        } catch (RuntimeException timeout) {
            return false;
        }
    }

    private String labelOf(By locator) {
        try {
            List<WebElement> found = visibleElements(locator);
            return found.isEmpty() ? null : found.get(0).getAttribute("label");
        } catch (RuntimeException gone) {
            return null;
        }
    }
}
