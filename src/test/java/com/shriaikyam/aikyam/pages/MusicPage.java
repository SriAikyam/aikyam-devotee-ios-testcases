package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

public class MusicPage extends BasePage {

    // MusicTopBar's centred title. Not the tab-bar label, which carries tab.music.
    private final By musicHeaderTitle = id("music.title");

    // Title Text of every MusicTrackCard on Music home (track and album cards alike - the
    // section's own tap handler decides player vs Album Details). Its label is the card's
    // title. Deity/genre/radio/voice tiles are not track cards and are not matched.
    private static final String TRACK_CARD = "music.track.card";
    private final By trackCardTitle = id(TRACK_CARD);

    // MusicMiniPlayer transport. The play/pause button's identifier flips with the state
    // (music.mini.pause while playing, music.mini.play while paused).
    private final By miniPlayAction = id("music.mini.play");
    private final By miniPauseAction = id("music.mini.pause");
    private final By miniPreviousAction = id("music.mini.previous");
    private final By miniNextAction = id("music.mini.next");
    private final By miniTitle = id("music.mini.title");

    // Full player (MusicPlayerScreen / PlayerComponents), same state-flipping convention.
    private final By playerPlayAction = id("music.player.play");
    private final By playerPauseAction = id("music.player.pause");
    private final By playerPreviousAction = id("music.player.previous");
    private final By playerNextAction = id("music.player.next");
    private final By playerClose = id("music.player.close");
    private final By playerTitle = id("music.player.title");
    private final By playerPosition = id("music.player.position");
    private final By playerDuration = id("music.player.duration");
    private final By playerFavourite = id("music.player.favorite");

    // "See All" on a MusicSectionContainer header; pushes MusicSectionScreen.
    private final By seeAllAction = id("music.section.seeall");

    // Album Details (MusicAlbumScreen): the hero's Play button.
    private final By playSectionAction = id("music.album.play");

    // Back button of every pushed Music screen (MusicSubScreenTopBar, AlbumDetailsTopBar).
    private final By subScreenBack = id("common.back");

    public MusicPage(IOSDriver driver) {
        super(driver);
    }

    /** Backs out of the full player / pushed screens to Music home, then waits for the
     * catalogue - the header renders before the catalogue request completes, so "loaded"
     * means at least one track card exists. Best-effort: if the catalogue never arrives the
     * caller's own assertion reports it. */
    public void waitUntilLoaded() {
        // The full player is an overlay; Music home (header included) stays in the tree
        // beneath it, so the header alone cannot say whether the player is covering it.
        for (int attempt = 0; attempt < 3 && (isFullPlayerVisible() || !isAnyVisible(musicHeaderTitle)); attempt++) {
            navigateBack();
        }
        waitForAnyVisible(musicHeaderTitle);
        try {
            new WebDriverWait(driver, CATALOGUE_TIMEOUT)
                    .until(ignored -> firstBrowsableItemTitle() != null);
        } catch (RuntimeException stillLoading) {
            // Fall through - isMusicScreenVisible()/the caller's assertion reports it.
        }
    }

    /** Longer than DEFAULT_TIMEOUT: the catalogue is several backend-driven carousels. */
    private static final Duration CATALOGUE_TIMEOUT = Duration.ofSeconds(45);

    public boolean isMusicScreenVisible() {
        return isAnyVisible(musicHeaderTitle);
    }

    /** Title of the first track card on Music home, or null if none has loaded yet. */
    public String firstBrowsableItemTitle() {
        return browsableItemDescriptions().stream().findFirst().orElse(null);
    }

    /** Taps the first track card and returns its title (a track opens the full player, an
     * album opens Album Details), or null if nothing browsable was visible. */
    public String tapFirstBrowsableItem() {
        List<WebElement> candidates = describedImageCandidates();
        if (candidates.isEmpty()) {
            return null;
        }
        String title = candidates.get(0).getAttribute("label");
        return tapDescribedNode(title) ? title : null;
    }

    private boolean tapDescribedNode(String title) {
        if (title == null) {
            return false;
        }
        try {
            List<WebElement> cards = visibleElements(AppiumBy.iOSNsPredicateString(
                    "name == " + predicateLiteral(TRACK_CARD) + " AND label == " + predicateLiteral(title)));
            if (cards.isEmpty()) {
                return false;
            }
            cards.get(0).click();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Confirms the Player or Album Details opened for the given title: both render it as
     * text (player track row, album hero). The home card with the same title stays in the
     * tree under the player overlay, so it is excluded. */
    public boolean isDestinationVisibleFor(String title) {
        return title != null && isAnyVisible(AppiumBy.iOSNsPredicateString(
                "label == " + predicateLiteral(title) + " AND name != " + predicateLiteral(TRACK_CARD)));
    }

    private static final java.util.regex.Pattern CLOCK =
            java.util.regex.Pattern.compile("^(\\d+:)?\\d{1,2}:\\d{2}$");

    /** The full player's elapsed-time label only exists while the player is up. */
    public boolean isFullPlayerVisible() {
        return currentPositionText() != null;
    }

    /** Elapsed position shown under the player's seek bar, or null when no player. */
    public String currentPositionText() {
        return clockLabel(playerPosition);
    }

    private String clockLabel(By locator) {
        // The label ticks every second, so a reference can go stale between lookup and read.
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                for (WebElement element : visibleElements(locator)) {
                    String value = element.getAttribute("label");
                    if (value != null && CLOCK.matcher(value.trim()).matches()) {
                        return value.trim();
                    }
                }
                return null;
            } catch (StaleElementReferenceException updated) {
                // Take a fresh snapshot.
            }
        }
        return null;
    }

    /** Album Details, reached when the tapped card is an album rather than a single track. */
    public boolean isAlbumDetailsVisible() {
        return isAnyVisible(playSectionAction);
    }

    /** Gets from "a browsable card was tapped" to "something is playing", whichever
     * destination the card led to (track: full player; album: Album Details + Play). */
    public boolean startPlaybackFromCurrentDestination() {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ignored -> isAlbumDetailsVisible() || isFullPlayerVisible());
        } catch (RuntimeException neitherArrived) {
            return false;
        }
        if (isAlbumDetailsVisible()) {
            return tapFirstVisible(playSectionAction);
        }
        return isFullPlayerVisible();
    }

    /** True when the player shows the track's duration alongside the elapsed position -
     * proof the track really loaded rather than an empty player. */
    public boolean hasTrackDuration() {
        return currentPositionText() != null && clockLabel(playerDuration) != null;
    }

    /** Playback is "running" only if the position actually moves. */
    public boolean hasPositionAdvanced(String before) {
        try {
            return new WebDriverWait(driver, SHORT_TIMEOUT).until(ignored -> {
                String now = currentPositionText();
                return now != null && !now.equals(before);
            });
        } catch (RuntimeException timedOut) {
            return false;
        }
    }

    /** Whichever transport is on screen (mini player or full player) - its play/pause
     * identifier reflects the real playback state. */
    public boolean isPlaybackPlaying() {
        return isAnyVisible(miniPauseAction, playerPauseAction);
    }

    public boolean isPlaybackPaused() {
        return isAnyVisible(miniPlayAction, playerPlayAction);
    }

    /** Waits for the state to flip rather than checking once: the toggle goes through the
     * playback manager and the button re-renders a moment later. */
    public boolean waitForPlaybackPlaying() {
        return waitUntilGone(miniPlayAction, playerPlayAction) && isAnyVisible(miniPauseAction, playerPauseAction);
    }

    public boolean waitForPlaybackPaused() {
        return waitUntilGone(miniPauseAction, playerPauseAction) && isAnyVisible(miniPlayAction, playerPlayAction);
    }

    public boolean tapMiniPlayerPlayPause() {
        return tapFirstVisible(miniPauseAction, miniPlayAction);
    }

    public boolean isMiniPlayerVisible() {
        return isAnyVisible(miniPauseAction, miniPlayAction, miniNextAction, miniPreviousAction);
    }

    public boolean tapNextTrack() {
        return tapFirstVisible(miniNextAction, playerNextAction);
    }

    public boolean isMiniPlayerOrPlayerControlsVisible() {
        return isAnyVisible(miniPlayAction, miniPauseAction, playerPlayAction, playerPauseAction);
    }

    public boolean isFullPlayerControlSetVisible() {
        return isAnyVisible(miniPlayAction, miniPauseAction, playerPlayAction, playerPauseAction)
                && isAnyVisible(miniPreviousAction, playerPreviousAction)
                && isAnyVisible(miniNextAction, playerNextAction);
    }

    /** The full player dismisses through its own chevron (no navigation bar), and pushed
     * Music screens through their own back button; anything else falls back to BasePage. */
    @Override
    public boolean navigateBack() {
        for (By control : new By[] {playerClose, subScreenBack}) {
            try {
                List<WebElement> found = visibleElements(control);
                if (!found.isEmpty()) {
                    found.get(0).click();
                    return true;
                }
            } catch (RuntimeException gone) {
                // Try the next control.
            }
        }
        try {
            return super.navigateBack();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isSeeAllVisible() {
        return scrollUntilVisible(seeAllAction);
    }

    public boolean tapFirstSeeAll() {
        if (!scrollUntilVisible(seeAllAction)) {
            return false;
        }
        try {
            tapElementCenter(waitForAnyVisible(seeAllAction));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** MusicSectionScreen: a pushed screen (own back button, its rows are MusicTrackListRows)
     * with Music home - and so every See All - hidden beneath it. */
    public boolean isSectionDetailsScreenVisible() {
        // The section re-fetches its full list on push; give it the full timeout.
        try {
            waitForAnyVisible(id("music.track.row"), subScreenBack);
        } catch (RuntimeException ignored) {
            return false;
        }
        return isAnyVisible(subScreenBack) && !isVisibleNow(seeAllAction);
    }

    private List<String> browsableItemDescriptions() {
        return describedImageCandidates().stream()
                .map(element -> {
                    try {
                        return element.getAttribute("label");
                    } catch (StaleElementReferenceException exception) {
                        return null;
                    }
                })
                .filter(label -> label != null && !label.isBlank())
                .collect(Collectors.toList());
    }

    private List<WebElement> describedImageCandidates() {
        // Cards load after the header renders - wait for one before reading the list, or a
        // call made right after navigation reports nothing browsable.
        try {
            waitForAnyVisible(trackCardTitle);
        } catch (RuntimeException ignored) {
            // Genuinely nothing browsable loaded - the empty list below reports that.
        }
        return visibleElements(trackCardTitle);
    }

    // ------------------------------------------------------------------------------
    // Journey helpers (MusicJourneyTest).
    // ------------------------------------------------------------------------------

    private final By favouritesIcon = id("music.favorites");
    // MusicFavoritesScreen's top bar title (US spelling on iOS).
    private final By favouritesScreenTitle = AppiumBy.iOSNsPredicateString(
            "name == 'music.subscreen.title' AND label == 'Favorites'");
    private final By favouritesEmpty = id("music.favorites.empty");
    // Title Text of each MusicTrackListRow (favourites, section, library rows).
    private static final String TRACK_ROW = "music.track.row";
    private final By trackRowTitle = id(TRACK_ROW);

    /** Title of the track in the mini player (null if no mini player). */
    public String miniPlayerTitle() {
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                List<WebElement> titles = visibleElements(miniTitle);
                if (titles.isEmpty()) {
                    return null;
                }
                String value = titles.get(0).getAttribute("label");
                return value == null || value.isBlank() ? null : value.trim();
            } catch (StaleElementReferenceException updated) {
                // retry with a fresh snapshot
            }
        }
        return null;
    }

    /** Waits for a mini player title different from {@code previous}; returns it or null. */
    public String waitForMiniPlayerTitleChange(String previous) {
        try {
            return new WebDriverWait(driver, SHORT_TIMEOUT).until(ignored -> {
                String now = miniPlayerTitle();
                return now != null && !now.equals(previous) ? now : null;
            });
        } catch (RuntimeException timedOut) {
            return null;
        }
    }

    public boolean waitForMiniPlayerTitle(String expected) {
        try {
            return new WebDriverWait(driver, SHORT_TIMEOUT)
                    .until(ignored -> expected.equals(miniPlayerTitle()));
        } catch (RuntimeException timedOut) {
            return false;
        }
    }

    public boolean tapPreviousTrack() {
        return tapFirstVisible(miniPreviousAction, playerPreviousAction);
    }

    /** Titles of every track card currently on Music home, in on-screen order. */
    public List<String> browsableTitles() {
        return browsableItemDescriptions().stream().distinct().collect(Collectors.toList());
    }

    /** Taps the track card with exactly this title. */
    public boolean tapBrowsableTitle(String title) {
        return tapDescribedNode(title);
    }

    /** Waits for the full player or Album Details to arrive after a card tap. */
    public boolean waitForPlayerOrAlbum() {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ignored -> isFullPlayerVisible() || isAlbumDetailsVisible());
            return true;
        } catch (RuntimeException neither) {
            return false;
        }
    }

    /** Title shown in the full player's track row. */
    public String fullPlayerTitle() {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                List<WebElement> titles = visibleElements(playerTitle);
                if (titles.isEmpty()) {
                    return null;
                }
                String value = titles.get(0).getAttribute("label");
                return value == null || value.isBlank() ? null : value.trim();
            } catch (StaleElementReferenceException updated) {
                // retry
            }
        }
        return null;
    }

    /** The heart in the full player's track row. */
    public boolean tapFullPlayerFavourite() {
        List<WebElement> hearts = visibleElements(playerFavourite);
        if (hearts.isEmpty()) {
            return false;
        }
        tapElementCenter(hearts.get(0));
        return true;
    }

    /** Music home is showing: header present, no player, no pushed screen (Music home is
     * accessibility-hidden while any screen is pushed, so its header leaves the tree). */
    public boolean isOnMusicHomeNow() {
        return !isFullPlayerVisible()
                && driver.findElements(playSectionAction).isEmpty()
                && driver.findElements(favouritesScreenTitle).isEmpty()
                && visibleElements(subScreenBack).isEmpty()
                && !driver.findElements(musicHeaderTitle).isEmpty();
    }

    /** Pops overlays (player, album, favourites) until Music home is showing. Never backs
     * out once already home. */
    public boolean backToMusicHome() {
        for (int i = 0; i < 4 && !isOnMusicHomeNow(); i++) {
            navigateBack();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        return isOnMusicHomeNow();
    }

    /** Opens Favourites from the Music top bar heart and waits for its title. */
    public boolean openFavourites() {
        try {
            tapElementCenter(waitForAnyVisible(favouritesIcon));
            waitForAnyVisible(favouritesScreenTitle);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isFavouritesEmptyStateVisible() {
        return isAnyVisible(favouritesEmpty);
    }

    /** Settles the Favourites list (rows or empty state), then returns the row titles. */
    public List<String> favouriteTitles() {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ignored ->
                    !favouriteRowTitles().isEmpty() || !driver.findElements(favouritesEmpty).isEmpty());
        } catch (RuntimeException settled) {
            // fall through with whatever is there
        }
        return favouriteRowTitles();
    }

    private List<String> favouriteRowTitles() {
        java.util.ArrayList<String> titles = new java.util.ArrayList<>();
        for (int attempt = 0; attempt < 3; attempt++) {
            titles.clear();
            try {
                for (WebElement row : driver.findElements(trackRowTitle)) {
                    String value = row.getAttribute("label");
                    if (value != null && !value.isBlank()) {
                        titles.add(value.trim());
                    }
                }
                return titles;
            } catch (StaleElementReferenceException updated) {
                // retry
            }
        }
        return titles;
    }

    /** Taps the Favourites row for this title (opens the player). */
    public boolean tapFavouriteRow(String title) {
        try {
            tapElementCenter(driver.findElement(AppiumBy.iOSNsPredicateString(
                    "name == " + predicateLiteral(TRACK_ROW) + " AND label == " + predicateLiteral(title))));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Visible label of every static text on screen (album track lists are plain text rows). */
    public java.util.Set<String> visibleTexts() {
        java.util.Set<String> texts = new java.util.LinkedHashSet<>();
        for (int attempt = 0; attempt < 3; attempt++) {
            texts.clear();
            try {
                for (WebElement element : driver.findElements(By.className("XCUIElementTypeStaticText"))) {
                    String value = element.getAttribute("label");
                    if (value != null && !value.isBlank()) {
                        texts.add(value.trim());
                    }
                }
                return texts;
            } catch (StaleElementReferenceException updated) {
                // retry
            }
        }
        return texts;
    }

    public boolean tapAlbumPlay() {
        return tapFirstVisible(playSectionAction);
    }

    public void scrollHomeDown() {
        swipeUp();
    }

    /** Track/album cards only - every MusicTrackCard is the same large square artwork, so
     * this is the same set as browsableTitles(). */
    public List<String> largeCardTitles() {
        return browsableTitles();
    }

    public void scrollHomeToTop() {
        for (int i = 0; i < 5; i++) {
            swipeDown();
        }
    }

    /** Waits for the album hero's "N tracks • M mins" summary. */
    public boolean waitForTrackCountText() {
        return isAnyVisible(id("music.album.trackcount"));
    }

    /** Non-waiting probe: Album Details shows its Play button; the full player never does. */
    public boolean isAlbumScreenNow() {
        return !driver.findElements(playSectionAction).isEmpty();
    }

    /** True only for the real full player (position label and no album Play button). */
    public boolean isTrackPlayerNow() {
        return !isAlbumScreenNow() && isFullPlayerVisible();
    }
}
