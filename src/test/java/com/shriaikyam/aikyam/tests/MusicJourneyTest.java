package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.HomePage;
import com.shriaikyam.aikyam.pages.MusicPage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import com.shriaikyam.aikyam.categories.P0;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** End-to-end Music journeys. Black-box: tracks are discovered from the live catalogue,
 * never hard-coded. The full player's controls are unlabelled, so they are located by
 * position (see MusicPage journey helpers). */
public class MusicJourneyTest extends BaseTest {

    private HomePage home;
    private MusicPage music;

    @Before
    public void enterMusic() {
        home = new HomePage(driver);
        music = new MusicPage(driver);
        home.tapHomeTab();
        home.tapMusicTab();
        music.waitUntilLoaded();
        music.backToMusicHome();
    }

    /** Leave Music home with playback paused and no overlay open. */
    @After
    public void leaveMusicHomePaused() {
        try {
            music.backToMusicHome();
            if (music.isPlaybackPlaying()) {
                music.tapMiniPlayerPlayPause();
                music.waitForPlaybackPaused();
            }
            music.backToMusicHome();
            music.scrollHomeToTop();
        } catch (RuntimeException ignored) {
            // cleanup is best effort; the assertion in the test body is what reports
        }
    }

    /** Taps browsable cards in order until one opens the full player (a track), skipping
     * titles in {@code exclude}. Returns the tapped title, or null. */
    private String openFirstTrackExcluding(Set<String> exclude) {
        Set<String> tried = new HashSet<>();
        music.scrollHomeToTop();
        for (int screen = 0; screen < 4 && tried.size() < 8; screen++) {
            for (String title : music.largeCardTitles()) {
                if (exclude.contains(title) || !tried.add(title)) {
                    continue;
                }
                if (!music.tapBrowsableTitle(title) || !music.waitForPlayerOrAlbum()) {
                    music.backToMusicHome();
                    continue;
                }
                if (music.isTrackPlayerNow()) {
                    return title;
                }
                music.backToMusicHome(); // an album or placeholder - not what we want here
            }
            music.scrollHomeDown();
        }
        return null;
    }

    private String startTrackAndReturnToMini() {
        String title = openFirstTrackExcluding(new HashSet<>());
        Assert.assertNotNull("Some catalogue card should open the full player", title);
        Assert.assertTrue("Back should leave the full player", music.navigateBack());
        Assert.assertTrue("Mini player should show after leaving the player", music.isMiniPlayerVisible());
        return title;
    }

    @Test
    public void TC200_Favourite_Round_Trip_Lists_And_Removes_Track() {
        Assert.assertTrue("Favourites should open from the Music top bar", music.openFavourites());
        List<String> before = music.favouriteTitles();
        if (before.isEmpty()) {
            Assert.assertTrue("Empty favourites should show its empty-state message",
                    music.isFavouritesEmptyStateVisible());
        }
        Assert.assertTrue(music.backToMusicHome());

        String title = openFirstTrackExcluding(new HashSet<>(before));
        Assert.assertNotNull("A track not already in favourites should open the player", title);
        boolean toggled = false;
        try {
            Assert.assertTrue("Favourite heart should be tappable", music.tapFullPlayerFavourite());
            toggled = true;
            Assert.assertTrue(music.backToMusicHome());
            Assert.assertTrue(music.openFavourites());
            Assert.assertTrue("Favourited track '" + title + "' should be listed; got " + music.favouriteTitles(),
                    music.favouriteTitles().contains(title));

            // Unfavourite via the player opened from the Favourites row.
            Assert.assertTrue("Favourite row should open the player", music.tapFavouriteRow(title));
            Assert.assertTrue(music.waitForPlayerOrAlbum());
            Assert.assertTrue("Heart should be tappable again", music.tapFullPlayerFavourite());
            toggled = false;
            Assert.assertTrue(music.backToMusicHome());
            Assert.assertTrue(music.openFavourites());
            Assert.assertFalse("Unfavourited track should be gone; got " + music.favouriteTitles(),
                    music.favouriteTitles().contains(title));
            Assert.assertEquals("Favourites should be back to the starting set", new HashSet<>(before),
                    new HashSet<>(music.favouriteTitles()));
        } finally {
            if (toggled) {
                undoFavourite(title);
            }
        }
    }

    /** Best-effort restore if the test died between favouriting and unfavouriting. */
    private void undoFavourite(String title) {
        try {
            music.backToMusicHome();
            if (music.openFavourites() && music.favouriteTitles().contains(title)
                    && music.tapFavouriteRow(title) && music.waitForPlayerOrAlbum()) {
                music.tapFullPlayerFavourite();
            }
            music.backToMusicHome();
        } catch (RuntimeException ignored) {
            // surfaced by the leftover favourite in the report
        }
    }

    @Test
    public void TC201_Next_And_Previous_Change_The_Track() {
        startTrackAndReturnToMini();
        String first = music.miniPlayerTitle();
        Assert.assertNotNull("Mini player should show a title", first);

        Assert.assertTrue("Next track should be tappable", music.tapNextTrack());
        String second = music.waitForMiniPlayerTitleChange(first);
        Assert.assertNotNull("Next should change the visible title away from '" + first + "'", second);

        Assert.assertTrue("Previous track should be tappable", music.tapPreviousTrack());
        Assert.assertTrue("Previous should return to '" + first + "' but title is '"
                + music.miniPlayerTitle() + "'", music.waitForMiniPlayerTitle(first));
    }

    @Category(P0.class)
    @Test
    public void TC202_Mini_Player_Persists_Across_Tabs_With_Same_Track() {
        startTrackAndReturnToMini();
        String title = music.miniPlayerTitle();
        Assert.assertNotNull(title);

        home.tapHomeTab();
        home.tapAppsTab();
        home.tapMusicTab();
        music.backToMusicHome();

        Assert.assertTrue("Mini player should still be present after Home/Apps round trip",
                music.isMiniPlayerVisible());
        Assert.assertEquals("Mini player should still show the same track", title, music.miniPlayerTitle());
    }

    @Test
    public void TC203_Album_Details_Lists_Tracks_And_Play_Starts_An_Album_Track() {
        String album = null;
        Set<String> tried = new HashSet<>();
        music.scrollHomeToTop();
        // Cards are tracks (open the player) or albums (open Album Details, whose hero reads
        // "N tracks"); scroll the catalogue until one opens as an album.
        for (int screen = 0; screen < 6 && album == null && tried.size() < 30; screen++) {
            for (String title : music.largeCardTitles()) {
                if (!tried.add(title)) {
                    continue;
                }
                if (music.tapBrowsableTitle(title) && music.waitForPlayerOrAlbum()
                        && music.isAlbumScreenNow()
                        && music.waitForTrackCountText()) {
                    album = title;
                    break;
                }
                music.backToMusicHome();
            }
            if (album == null) {
                music.scrollHomeDown();
            }
        }
        Assert.assertNotNull("At least one catalogue card should open an album; tried " + tried, album);

        Set<String> texts = music.visibleTexts();
        long durations = texts.stream().filter(t -> t.matches("\\d{1,2}:\\d{2}")).count();
        Assert.assertTrue("Album '" + album + "' should list tracks with durations; saw " + texts, durations >= 1);

        Assert.assertTrue("Play should be tappable", music.tapAlbumPlay());
        Assert.assertTrue("Mini player should appear after Play", music.isMiniPlayerVisible());
        String playing = music.miniPlayerTitle();
        Assert.assertNotNull(playing);
        Assert.assertTrue("Mini title '" + playing + "' should be a track listed on album '" + album
                + "'; album texts were " + texts, texts.contains(playing));
    }
}
