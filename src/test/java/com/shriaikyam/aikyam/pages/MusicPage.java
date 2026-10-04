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

    // "Music" (topbar title, R.string.nav_music) is the only static text on this
    // screen -- every carousel below it (tracks, albums, deities, genres, radios,
    // voices) is backend-driven content with no fixed strings to match against.
    private final By musicHeaderTitle = text("Music");

    // Every browsable card on this screen (MusicTrackCard, MusicDeityCarousel,
    // MusicGenreGrid, MusicRadioCarousel, MusicVoiceCarousel) renders its artwork as
    // a Compose AsyncImage whose contentDescription is set to that item's own title --
    // this is the only content-agnostic way to find "any browsable item" here.
    // Confirmed live: in this build Compose semantics nodes expose as plain
    // "android.view.View", NOT "android.widget.ImageView" (that earlier assumption was
    // wrong and caused every candidate to be missed). The described node itself is
    // also never clickable - the tap target is its nearest clickable ancestor, found
    // via tapDescribedNode() below. The top bar's own chrome (bottom-nav labels,
    // profile avatar, whose contentDescription is the account's display name rather
    // than a fixed string) must be excluded explicitly/by position.
    private final By anyDescribedNode = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionMatches(\".+\")"
    );
    private static final java.util.Set<String> CHROME_DESCRIPTIONS = java.util.Set.of(
            "Home", "Explore", "Music", "Sevas", "Events", "Apps", "Profile"
    );
    // Top bar/header content sits within roughly this many pixels of the top of the
    // screen on every device this suite runs on - anything above it is chrome
    // (title, avatar), never a browsable card.
    private static final int HEADER_ZONE_BOTTOM_Y = 300;

    // MusicMiniPlayer's action buttons are the only interactive elements with real,
    // stable contentDescriptions on the player screens.
    private final By playAction = accessibilityId("Play");
    private final By pauseAction = accessibilityId("Pause");
    private final By previousTrackAction = accessibilityId("Previous track");
    private final By nextTrackAction = accessibilityId("Next track");

    // "See All" (music_see_all string res) opens the shared Section/Album Details
    // overlay for that section's full list - reuses the exact same screen Album
    // Details uses (isFromSection=true hides the about/related sections there).
    private final By seeAllAction = AppiumBy.androidUIAutomator("new UiSelector().text(\"See All\")");

    // Section/Album Details' track rows are plain text (number, title, duration) with
    // no image contentDescription, so firstBrowsableItemTitle() can't see them -- its
    // "Play" pill button (confirmed live, unique to this screen) is a reliable signal
    // that we actually landed here instead.
    private final By playSectionAction = AppiumBy.androidUIAutomator("new UiSelector().text(\"Play\")");

    public MusicPage(IOSDriver driver) {
        super(driver);
    }

    /** Confirmed live: re-tapping the Music bottom-nav tab does not reset the Music
     * module's own internal nav stack (Section Details/Album Details/Player are pushed
     * onto it as overlays) - a prior test's navigation can strand a later one deep
     * inside it. Neither the "Music" title nor the bottom nav bar work as a "did we
     * escape" signal - confirmed live that Compose keeps the covered Scaffold
     * (including both) reporting displayed=true in the accessibility tree even while
     * fully hidden underneath an overlay. "See All" is the one element confirmed
     * present only on the real home screen (never on the Section/Album Details
     * overlay) - use that as the recovery loop's stopping condition instead. */
    /** Waits for the catalogue, not just the chrome.
     *
     * This used to return as soon as musicHeaderTitle appeared - but text("Music") is the
     * top-bar title AND the bottom-nav label, both of which render instantly, before the
     * catalogue request has even completed. So it returned while the screen was still
     * showing its loading spinner, and the caller then looked for tracks that had not
     * arrived yet. That is what every "Music home" timeout in this suite actually was:
     * a load-time problem misread as flakiness (confirmed live - the diagnostics
     * screenshot for TC143 shows the Music screen with nothing but a spinner).
     *
     * Content is backend-driven with no fixed strings, so "loaded" means at least one
     * browsable card exists outside the header zone. Best-effort rather than throwing:
     * if the catalogue genuinely never arrives, the caller's own assertion should report
     * that, not an opaque wait failure here. */
    public void waitUntilLoaded() {
        // Back out to Music home, keyed on the header rather than on "See All".
        //
        // The old condition was !isAnyVisible(seeAllAction), which pressed back up to five
        // times whenever no See All section was on screen - including when the catalogue
        // simply had not loaded yet, and including when already on Music home. Five back
        // presses from Music home EXIT THE APP, which is exactly what happened: TC143's
        // diagnostics screenshot shows the device sitting on the launcher with the app not
        // running, and everything after it failed for want of a foreground app. See All is
        // also optional content, so its absence never meant "not home".
        // The full player is an overlay: Music home (header included) stays composed beneath
        // it, so the header alone cannot say whether the player is covering it.
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

    /** Longer than DEFAULT_TIMEOUT: the catalogue is several backend-driven carousels and
     * was observed still spinning well past the default. */
    private static final Duration CATALOGUE_TIMEOUT = Duration.ofSeconds(45);

    public boolean isMusicScreenVisible() {
        return isAnyVisible(musicHeaderTitle);
    }

    /** Finds the first browsable card on the Music home screen and returns its title,
     * read from the card's own image contentDescription -- none of this content is
     * static/known ahead of time. Returns null if nothing browsable has loaded yet. */
    public String firstBrowsableItemTitle() {
        return browsableItemDescriptions().stream().findFirst().orElse(null);
    }

    /** Taps the first browsable card found and returns the title it should navigate
     * to (a track opens the Player screen, an album opens Album Details -- both
     * destinations re-expose the same title as their own artwork's contentDescription,
     * see isDestinationVisibleFor()), or null if nothing browsable was visible. */
    public String tapFirstBrowsableItem() {
        List<WebElement> candidates = describedImageCandidates();
        if (candidates.isEmpty()) {
            return null;
        }
        String title = candidates.get(0).getAttribute("content-desc");
        return tapDescribedNode(title) ? title : null;
    }

    /** The description-bearing node itself is not clickable (confirmed live) - the
     * actual tap target is its nearest clickable ancestor. */
    private boolean tapDescribedNode(String description) {
        By clickableAncestor = By.xpath(
                "(//*[@content-desc=\"" + description + "\"]/ancestor-or-self::*[@clickable=\"true\"])[1]"
        );
        try {
            driver.findElement(clickableAncestor).click();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Confirms the Player or Album Details screen actually opened for the given
     * title -- both destinations expose an artwork Image whose contentDescription
     * equals the tapped item's own title (PlayerArtworkTrackHeader, AlbumDetailsHero). */
    public boolean isDestinationVisibleFor(String title) {
        return title != null && isAnyVisible(accessibilityId(title));
    }

    private final By anyTextView = AppiumBy.className("android.widget.TextView");

    private static final java.util.regex.Pattern CLOCK =
            java.util.regex.Pattern.compile("^\\d{1,2}:\\d{2}$");

    /** Tapping a track opens the full player directly - not a mini player (confirmed live:
     * the page source shows position/duration and the lyrics section, and no Play or Pause
     * node at all). The full player's transport controls are unlabelled, so the elapsed
     * time is the only playback signal it exposes. Reading it costs nothing and needs no
     * contentDescription, which is what makes playback assertable despite that gap. */
    public boolean isFullPlayerVisible() {
        return currentPositionText() != null;
    }

    /** First clock-formatted label on screen: the elapsed position. Duration follows it,
     * so document order distinguishes them without needing ids. */
    public String currentPositionText() {
        // Retried as a whole, not just per element: the clock ticks once a second and
        // recomposes the subtree, so resolving the collection itself throws
        // StaleElementReferenceException - guarding only getText() left that escaping and
        // failed three cases (confirmed live).
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                for (WebElement element : visibleElements(anyTextView)) {
                    String value = element.getText();
                    if (value != null && CLOCK.matcher(value.trim()).matches()) {
                        return value.trim();
                    }
                }
                return null;
            } catch (StaleElementReferenceException recomposed) {
                // Tree changed under us mid-read; take a fresh snapshot.
            }
        }
        return null;
    }

    /** Album Details, reached when the tapped browsable item is an album rather than a
     * single track. Identified by its own Play button, which - unlike the full player's
     * transport - is real labelled text. */
    public boolean isAlbumDetailsVisible() {
        return isAnyVisible(playSectionAction);
    }

    /** Gets from "a browsable item was tapped" to "something is playing", whichever
     * destination the item led to. The first browsable item is an ALBUM on the prod build
     * (Album Details with a Play button and a track list) and was a single track on dev
     * (straight to the full player) - confirmed live on both, and the reason the playback
     * cases failed on prod while passing on dev. */
    public boolean startPlaybackFromCurrentDestination() {
        // Wait for whichever destination is arriving before deciding. Checking immediately
        // after the tap reported neither - Album Details had not composed its Play button
        // yet and the player was not up either, so this returned false while the app was
        // mid-navigation (confirmed live: TC143 failed here while TC144/TC146, which reach
        // the same code a moment later in a longer flow, passed).
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

    /** True when a second clock label is present, i.e. the track's duration alongside the
     * elapsed position - proof the track really loaded rather than the screen merely
     * rendering an empty player. */
    public boolean hasTrackDuration() {
        int clocks = 0;
        try {
            for (WebElement element : visibleElements(anyTextView)) {
                String value = element.getText();
                if (value != null && CLOCK.matcher(value.trim()).matches()) {
                    clocks++;
                }
            }
        } catch (StaleElementReferenceException recomposed) {
            return clocks >= 2;
        }
        return clocks >= 2;
    }

    /** Playback is "running" only if the position actually moves. Polls rather than
     * sleeping a fixed amount, and treats a position that never changes as paused. */
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

    /** The mini player's play/pause icon is the only control in the whole music feature
     * that carries a contentDescription, and it swaps between "Pause" (playing) and
     * "Play" (paused) with the state - see MusicMiniPlayer.kt. That makes it the one
     * genuine playback-state signal available to assert on. Everything on the full player
     * - play/pause, seek, favourite, repeat, lyrics - passes contentDescription = null,
     * so it cannot be located at all (15 of 17 icons in feature_music are unlabelled). */
    public boolean isPlaybackPlaying() {
        return isAnyVisible(pauseAction);
    }

    public boolean isPlaybackPaused() {
        return isAnyVisible(playAction);
    }

    /** Waits for the state to flip rather than checking once: the toggle is dispatched to
     * the player service and the icon recomposes a moment later. */
    public boolean waitForPlaybackPlaying() {
        return waitUntilGone(playAction) && isAnyVisible(pauseAction);
    }

    public boolean waitForPlaybackPaused() {
        return waitUntilGone(pauseAction) && isAnyVisible(playAction);
    }

    public boolean tapMiniPlayerPlayPause() {
        return tapFirstVisible(pauseAction, playAction);
    }

    public boolean isMiniPlayerVisible() {
        return isAnyVisible(pauseAction, playAction, nextTrackAction, previousTrackAction);
    }

    public boolean tapNextTrack() {
        return tapFirstVisible(nextTrackAction);
    }

    public boolean isMiniPlayerOrPlayerControlsVisible() {
        return isAnyVisible(playAction, pauseAction);
    }

    public boolean isFullPlayerControlSetVisible() {
        return isAnyVisible(playAction, pauseAction)
                && isAnyVisible(previousTrackAction)
                && isAnyVisible(nextTrackAction);
    }

    public boolean navigateBack() {
        try {
            driver.navigate().back();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isSeeAllVisible() {
        return scrollUntilVisible(seeAllAction);
    }

    /** Confirmed live: WebElement.click() on this TextView dispatches without error
     * (clickable="true" per the UI dump) but does not actually trigger Compose's click
     * handler - the tap needs to be a genuine coordinate-based touch gesture, same
     * class of issue tapElementCenter() already exists to work around. */
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

    /** Section Details reuses the same overlay Album Details uses - confirmed by
     * "See All" no longer being visible (navigated away from Music home) while its
     * own "Play" pill button renders (track rows have no image contentDescription for
     * firstBrowsableItemTitle() to see, so that signal doesn't work on this screen). */
    public boolean isSectionDetailsScreenVisible() {
        // Confirmed live this can race: navigating here re-fetches that section's full
        // track list, which occasionally takes longer than SHORT_TIMEOUT (8s) -- give
        // the "Play" pill the full DEFAULT_TIMEOUT to actually render before deciding.
        try {
            waitForAnyVisible(playSectionAction);
        } catch (RuntimeException ignored) {
            return false;
        }
        return !isAnyVisible(seeAllAction);
    }

    /** The carousel can recompose between describedImageCandidates() collecting these
     * elements and this method reading their attribute - confirmed live
     * (StaleElementReferenceException), since content here loads/reflows
     * asynchronously. A staled element just means that card is gone; skip it rather
     * than fail the whole read. */
    private List<String> browsableItemDescriptions() {
        return describedImageCandidates().stream()
                .map(element -> {
                    try {
                        return element.getAttribute("content-desc");
                    } catch (org.openqa.selenium.StaleElementReferenceException exception) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<WebElement> describedImageCandidates() {
        // Carousel cards load asynchronously after the "Music" header itself renders --
        // wait for at least one described node to actually appear before reading the
        // list, otherwise a call made right after navigation races the empty/loading
        // state and wrongly reports nothing browsable.
        try {
            waitForAnyVisible(anyDescribedNode);
        } catch (RuntimeException ignored) {
            // Genuinely nothing browsable loaded -- let the empty list below report that.
        }
        return visibleElements(anyDescribedNode).stream()
                .filter(element -> {
                    String desc = element.getAttribute("content-desc");
                    return desc != null && !desc.isBlank() && !CHROME_DESCRIPTIONS.contains(desc)
                            && element.getLocation().getY() > HEADER_ZONE_BOTTOM_Y;
                })
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------------------
    // Journey helpers (MusicJourneyTest). Additive only - none of the methods above
    // were changed. Live-verified layout: the full player's controls are clickable
    // android.widget.Button nodes with no description, so they are found by position;
    // the mini player's title is a TextView on the same row as "Next track".
    // ------------------------------------------------------------------------------

    private final By favouritesIcon = accessibilityId("Favourites");
    private final By favouritesScreenTitle = text("Favourites");
    private final By clickableButtons = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.Button\").clickable(true)");

    /** Title of the track in the mini player (null if no mini player). Read from the
     * TextView sharing a row with the Next control, left of Previous. */
    public String miniPlayerTitle() {
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                List<WebElement> nexts = driver.findElements(nextTrackAction);
                if (nexts.isEmpty()) {
                    return null;
                }
                org.openqa.selenium.Rectangle next = nexts.get(0).getRect();
                int centreY = next.y + next.height / 2;
                for (WebElement tv : driver.findElements(anyTextView)) {
                    org.openqa.selenium.Rectangle r = tv.getRect();
                    String value = tv.getText();
                    if (value != null && !value.isBlank() && r.x < next.x
                            && Math.abs(r.y + r.height / 2 - centreY) < 70) {
                        return value.trim();
                    }
                }
                return null;
            } catch (StaleElementReferenceException recomposed) {
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
        return tapFirstVisible(previousTrackAction);
    }

    /** Titles of every browsable card currently on Music home, in on-screen order. */
    public List<String> browsableTitles() {
        return browsableItemDescriptions().stream().distinct().collect(Collectors.toList());
    }

    /** Taps the browsable card with exactly this title. */
    public boolean tapBrowsableTitle(String title) {
        return tapDescribedNode(title);
    }

    /** Waits for the full player or Album Details to arrive after a card tap. */
    public boolean waitForPlayerOrAlbum() {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT)
                    .until(ignored -> isFullPlayerVisible() || isAlbumDetailsVisible()
                            || !driver.findElements(text("Coming soon")).isEmpty());
            return true;
        } catch (RuntimeException neither) {
            return false;
        }
    }

    /** Title shown by the full player (TextView in the track header, right of the artwork). */
    public String fullPlayerTitle() {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                for (WebElement tv : driver.findElements(anyTextView)) {
                    org.openqa.selenium.Rectangle r = tv.getRect();
                    String value = tv.getText();
                    if (value != null && !value.isBlank() && r.x >= 250 && r.y >= 290 && r.y < 450) {
                        return value.trim();
                    }
                }
                return null;
            } catch (StaleElementReferenceException recomposed) {
                // retry
            }
        }
        return null;
    }

    /** The favourite heart: the only description-less clickable button in the right-hand
     * track-header row (below the top chrome, above the lyrics). */
    public boolean tapFullPlayerFavourite() {
        for (WebElement button : driver.findElements(clickableButtons)) {
            org.openqa.selenium.Rectangle r = button.getRect();
            if (r.x > 800 && r.y > 300 && r.y < 600) {
                tapElementCenter(button);
                return true;
            }
        }
        return false;
    }

    public boolean isOnMusicHomeNow() {
        return !isFullPlayerVisible() && driver.findElements(playSectionAction).isEmpty()
                && driver.findElements(favouritesScreenTitle).isEmpty()
                && driver.findElements(text("Coming soon")).isEmpty()
                && driver.findElements(accessibilityId("Back")).isEmpty()
                && !driver.findElements(musicHeaderTitle).isEmpty();
    }

    /** Pops overlays (player, album, favourites) until Music home is showing. Never presses
     * back once already home, so it cannot exit the app. */
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

    /** Opens Favourites from the Music top bar and waits for its title. */
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
        return isAnyVisible(textContains("No favourites yet"));
    }

    /** Settles the Favourites list (rows or empty state), then returns the row titles.
     * A row is a clickable node holding a title TextView plus a clock-format duration. */
    public List<String> favouriteTitles() {
        try {
            new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ignored ->
                    !favouriteRowTitles().isEmpty() || !driver.findElements(textContains("No favourites yet")).isEmpty());
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
                for (WebElement row : driver.findElements(
                        By.xpath("//android.view.View[@clickable='true'][.//android.widget.TextView]"))) {
                    List<WebElement> tvs = row.findElements(anyTextView);
                    boolean hasClock = tvs.stream().anyMatch(t -> CLOCK.matcher(t.getText().trim()).matches());
                    if (hasClock && !tvs.isEmpty()) {
                        titles.add(tvs.get(0).getText().trim());
                    }
                }
                return titles;
            } catch (StaleElementReferenceException recomposed) {
                // retry
            }
        }
        return titles;
    }

    /** Taps the Favourites row for this title (opens the player). */
    public boolean tapFavouriteRow(String title) {
        try {
            tapElementCenter(driver.findElement(By.xpath(
                    "//android.view.View[@clickable='true'][.//android.widget.TextView[@text=\"" + title
                            + "\"]][.//android.widget.TextView[contains(@text,':')]]")));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Visible text of every TextView on screen (album track lists are plain text rows). */
    public java.util.Set<String> visibleTexts() {
        java.util.Set<String> texts = new java.util.LinkedHashSet<>();
        for (int attempt = 0; attempt < 3; attempt++) {
            texts.clear();
            try {
                for (WebElement tv : driver.findElements(anyTextView)) {
                    String value = tv.getText();
                    if (value != null && !value.isBlank()) {
                        texts.add(value.trim());
                    }
                }
                return texts;
            } catch (StaleElementReferenceException recomposed) {
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

    /** Like browsableTitles() but only large artwork cards (track/album cards), skipping the
     * small deity/voice circles and the mini-player thumbnail. */
    public List<String> largeCardTitles() {
        java.util.ArrayList<String> titles = new java.util.ArrayList<>();
        for (WebElement e : describedImageCandidates()) {
            try {
                if (e.getRect().width > 250 && !titles.contains(e.getAttribute("content-desc"))) {
                    titles.add(e.getAttribute("content-desc"));
                }
            } catch (StaleElementReferenceException gone) {
                // card recomposed away; skip it
            }
        }
        return titles;
    }

    public void scrollHomeToTop() {
        for (int i = 0; i < 5; i++) {
            swipeDown();
        }
    }

    /** Waits for an album hero's "N tracks" summary (poll, the hero loads after the screen). */
    public boolean waitForTrackCountText() {
        try {
            return new WebDriverWait(driver, SHORT_TIMEOUT).until(ignored ->
                    visibleTexts().stream().anyMatch(t -> t.toLowerCase().contains("track")));
        } catch (RuntimeException timedOut) {
            return false;
        }
    }

    /** Non-waiting probe: Album/Section Details shows a "Play" pill; the full player never does.
     * (Album track rows carry clock durations, so isFullPlayerVisible() is also true there.) */
    public boolean isAlbumScreenNow() {
        return !driver.findElements(playSectionAction).isEmpty();
    }

    /** True only for the real full player (clock labels and no album Play pill). */
    public boolean isTrackPlayerNow() {
        return !isAlbumScreenNow() && isFullPlayerVisible();
    }
}
