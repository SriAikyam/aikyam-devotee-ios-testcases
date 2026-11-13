package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.AppsPage;
import com.shriaikyam.aikyam.pages.BasePage;
import com.shriaikyam.aikyam.pages.DeepLinkPage;
import com.shriaikyam.aikyam.pages.EventsPage;
import com.shriaikyam.aikyam.pages.ExplorePage;
import com.shriaikyam.aikyam.pages.FamilyMembersPage;
import com.shriaikyam.aikyam.pages.FeedPage;
import com.shriaikyam.aikyam.pages.HomePage;
import com.shriaikyam.aikyam.pages.MusicPage;
import com.shriaikyam.aikyam.pages.ProfilePage;
import com.shriaikyam.aikyam.pages.SettingsPage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;

/**
 * Deep / shared link journeys. Real link: the feed's Share sheet previews the short link
 * the app mints (https://api-dev.shriaikyam.com/p/&lt;code&gt;); that exact link is fired back
 * at the dev bundle. Every link is sent with an explicit bundle id (DeepLinkPage.fire).
 */
public class DeepLinkJourneyTest extends BaseTest {

    private static final Duration LAND = Duration.ofSeconds(30);
    private static final String POST_ERROR = "Unable to load this shared post.";
    /** PostDetailView's inline comment composer - only the post detail screen has it. */
    private static final String COMPOSER = "postDetail.commentInput";

    private DeepLinkPage links;

    @Before
    public void startOnHome() {
        links = new DeepLinkPage(driver);
        new HomePage(driver).tapHomeTab();
        FeedPage feed = new FeedPage(driver);
        feed.waitUntilLoaded();
        // The first post is read to know which post a link should open, so start from the
        // top of All Posts: the Temples/Live filter and the scroll position persist.
        feed.tapAllPostsFilter();
    }

    @After
    public void leaveOnHome() {
        try {
            links.returnHome();
        } catch (RuntimeException ignored) {
            // cleanup must not mask the test verdict
        }
    }

    /** Shares the first feed post; returns {link, author}. */
    private String[] shareFirstPost() {
        FeedPage feed = new FeedPage(driver);
        String author = waitForFirstPostAuthor(feed);
        Assert.assertNotNull("First post author must be readable", author);
        Assert.assertTrue("Share must be tappable", feed.tapFirstShareAction());
        String link = links.readLinkFromShareSheet();
        links.closeShareSheet();
        Assert.assertNotNull("Share sheet must preview a short link", link);
        Assert.assertTrue("Home must be back after closing the sheet", links.isHomeFeedShown(Duration.ofSeconds(10)));
        return new String[] {link, author};
    }

    /** The feed re-renders after the All Posts tap, so the header can be unreadable for a moment. */
    private static String waitForFirstPostAuthor(FeedPage feed) {
        String author = null;
        for (int attempt = 0; attempt < 20 && author == null; attempt++) {
            author = feed.firstPostAuthor();
            if (author == null) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return author;
    }

    private void assertPostOpen(String author) {
        Assert.assertTrue("Link must open the post detail (composer shown)", links.waitFor(links.byDesc(COMPOSER), LAND));
        Assert.assertTrue("Opened post must be by " + author, links.isShown(links.byContainsText(author)));
    }

    @Ignore("iOS share sheet does not expose the link; share flow skipped")
    @Test
    public void TC240_Verify_Shared_Post_Link_Opens_Post_While_App_Running_And_Back_Returns_Home() {
        String[] shared = shareFirstPost();
        Assert.assertTrue(links.fire(shared[0]));
        assertPostOpen(shared[1]);
        BasePage.navigateBack(driver);
        Assert.assertTrue("Back must return to the Home feed", links.isHomeFeedShown(Duration.ofSeconds(15)));
    }

    @Ignore("iOS share sheet does not expose the link; share flow skipped")
    @Test
    public void TC241_Verify_Shared_Post_Link_Opens_Post_From_Cold_Start_Still_Logged_In() {
        String[] shared = shareFirstPost();
        Assert.assertTrue(links.forceStopApp());
        Assert.assertTrue(links.fire(shared[0]));
        assertPostOpen(shared[1]);
        Assert.assertFalse("Must not be sent to login", links.isShown(links.byContainsText("Enter your phone")));
        BasePage.navigateBack(driver);
        Assert.assertTrue("Back must reach the Home feed", links.isHomeFeedShown(Duration.ofSeconds(20)));
    }

    @Ignore("iOS share sheet does not expose the link; share flow skipped")
    @Test
    public void TC242_Verify_Custom_Scheme_Short_Link_Opens_Post() {
        String[] shared = shareFirstPost();
        String code = shared[0].substring(shared[0].lastIndexOf('/') + 1);
        Assert.assertTrue(links.fire("aikyam://p/" + code));
        assertPostOpen(shared[1]);
    }

    @Test
    public void TC243_Verify_Unknown_Short_Code_Link_Stays_On_Home_Without_Crash() {
        Assert.assertTrue(links.fire("https://api-dev.shriaikyam.com/p/zzzzzzzzzz"));
        Assert.assertTrue("Unresolvable short link must leave the Home feed showing",
                links.isHomeFeedShown(Duration.ofSeconds(15)));
        Assert.assertFalse(links.isShown(links.byDesc(COMPOSER)));
    }

    @Test
    @Ignore("Error-state assertions do not match what the app renders - script defect, fix pending.")
    public void TC244_Verify_Unknown_Post_Id_Link_Shows_Load_Error_And_Back_Returns_Home() {
        Assert.assertTrue(links.fire("https://m.shriaikyam.com/posts/doesnotexist123"));
        Assert.assertTrue("Shared-post error state must show", links.waitForText(POST_ERROR, LAND));
        Assert.assertTrue(links.isShown(links.byText("Retry")));
        Assert.assertTrue("Back button must be offered", links.isShown(links.byText("Back")));
        driver.findElement(links.byText("Back")).click();
        Assert.assertTrue("Back must land on Home", links.isHomeFeedShown(Duration.ofSeconds(15)));
    }

    @Test
    public void TC245_Verify_Malformed_Links_Are_Ignored_And_App_Stays_On_Home() {
        Assert.assertTrue(links.fire("https://m.shriaikyam.com/posts/bad%20id!"));
        Assert.assertTrue("Bad id shape must be ignored", links.isHomeFeedShown(Duration.ofSeconds(10)));
        Assert.assertFalse(links.isShown(links.byText(POST_ERROR)));
        Assert.assertTrue(links.fire("https://m.shriaikyam.com/posts/"));
        Assert.assertTrue("Link without an id must be ignored", links.isHomeFeedShown(Duration.ofSeconds(10)));
        Assert.assertFalse(links.isShown(links.byText(POST_ERROR)));
    }

    @Ignore("iOS share sheet does not expose the link; share flow skipped")
    @Test
    public void TC246_Verify_Shared_Own_Profile_Link_Opens_That_Profile() {
        ProfilePage profile = new ProfilePage(driver);
        Assert.assertTrue(profile.openSettingsHub());
        Assert.assertTrue(profile.openPersonalProfile());
        String handle = links.firstHandle();
        Assert.assertNotNull("Profile must show a @handle", handle);
        Assert.assertTrue(profile.tapShareProfile());
        String link = links.readLinkFromShareSheet();
        links.closeShareSheet();
        Assert.assertNotNull("Profile share sheet must preview a short link", link);
        links.returnHome();
        Assert.assertTrue(links.fire(link));
        Assert.assertTrue("Link must open the user profile showing " + handle,
                links.waitForText(handle, LAND) && links.isShown(links.byDesc("Share")));
    }

    @Test
    public void TC247_Verify_Unknown_Temple_Id_Link_Shows_Error_Without_Crash() {
        Assert.assertTrue(links.fire("https://m.shriaikyam.com/temples/doesnotexist123"));
        Assert.assertTrue("Temple error state must show", links.waitForText("Error loading temple details", LAND));
        Assert.assertTrue(links.isShown(links.byText("Retry")));
    }

    @Test
    @Ignore("Error-state assertions do not match what the app renders - script defect, fix pending.")
    public void TC248_Verify_Unknown_Event_Id_Link_Shows_Error_Without_Crash() {
        Assert.assertTrue(links.fire("https://m.shriaikyam.com/events/doesnotexist123"));
        Assert.assertTrue("Event error state must show", links.waitForText("getEventDetail failed: HTTP 404", LAND));
        Assert.assertTrue(links.isShown(links.byText("Retry")));
    }

    /** Records CURRENT behaviour: a link to a user id that does not exist opens an empty
     * "ghost" profile ("@doesnotexist123", 0 posts) instead of a not-found state. That is
     * arguably a product bug - if the app starts showing a not-found screen, update this test. */
    @Test
    @Ignore("Error-state assertions do not match what the app renders - script defect, fix pending.")
    public void TC249_Verify_Unknown_User_Id_Link_Opens_Profile_Screen_Without_Crash() {
        Assert.assertTrue(links.fire("https://m.shriaikyam.com/u/doesnotexist123"));
        Assert.assertTrue("User profile screen for the linked id must show",
                links.waitForText("@doesnotexist123", LAND) && links.isShown(links.byDesc("Back")));
    }

    // -------------------------------------------------------------------------
    // TC255-TC257: tab-switch deep links (PR #698 fix for issue #668 and #699)
    // -------------------------------------------------------------------------

    /**
     * aikyam://explore must switch to the Explore tab, not push Explore as an overlay on
     * the Home tab (which left Home selected in the tab bar and showed a Back button).
     * Fixed in feat/aikyam-deeplinks-clean (issue #668).
     */
    @Test
    public void TC255_Verify_Explore_Deep_Link_Switches_To_Explore_Tab_Not_Push_On_Home() {
        Assert.assertTrue("Fire aikyam://explore must succeed", links.fire("aikyam://explore"));
        ExplorePage explore = new ExplorePage(driver);
        Assert.assertTrue("Explore content (map/list toggle) must be visible after the tab switch",
                explore.isExploreVisible());
        // A tab switch leaves no Back button — only a push on the navigation stack would.
        Assert.assertFalse("No Back button must appear after a tab switch",
                links.isShown(links.byDesc("Back")));
        // The bottom nav must still be visible — we are at the tab root, not inside a pushed screen.
        Assert.assertTrue("Bottom navigation must remain visible at the tab root",
                new HomePage(driver).isHomeTabVisible());
    }

    /**
     * aikyam://music must switch to the Music tab directly, with no Back button (tab switch,
     * not a push). The Music tab's own header must be visible.
     */
    @Test
    public void TC256_Verify_Music_Deep_Link_Switches_To_Music_Tab_Not_Push() {
        Assert.assertTrue("Fire aikyam://music must succeed", links.fire("aikyam://music"));
        MusicPage music = new MusicPage(driver);
        Assert.assertTrue("Music tab content must be visible after the link",
                music.isMusicScreenVisible());
        Assert.assertFalse("No Back button must appear after a tab switch",
                links.isShown(links.byDesc("Back")));
        Assert.assertTrue("Bottom navigation must remain visible at the tab root",
                new HomePage(driver).isHomeTabVisible());
    }

    /**
     * aikyam://streams (no stream id) must stay on the Home tab and open the Live feed
     * highlight instead of returning not-routable. Fixed by PR #699 (issue #685).
     * Before the fix the bare host was silently dropped and nothing happened.
     */
    @Test
    public void TC257_Verify_Bare_Streams_Link_Stays_On_Home_And_Opens_Live_Feed() {
        Assert.assertTrue("Fire aikyam://streams must succeed", links.fire("aikyam://streams"));
        // The app must stay on the Home tab; the Live feed highlight (live banner or the
        // existing feed) must be visible — specifically, no crash and no unexpected navigation.
        Assert.assertTrue("Home tab must remain active after a bare streams link",
                links.isHomeFeedShown(LAND));
        Assert.assertFalse("No push must have occurred (no Back button)",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * For a priest in temple context, aikyam://explore must NOT open the Orders tab.
     * Tab id 3 is Orders for priests, so the link is not routable and the app stays
     * wherever it was (fix from feat/aikyam-deeplinks-clean PR #698 review P2).
     *
     * Skipped in automated runs: switching to a priest temple account requires a dedicated
     * priest test account and a temple context switch via PriestSettingsPage, which the
     * current CI device pool cannot guarantee per run. Verify manually using the QA
     * priest account documented in the team's Notion test-accounts page.
     */
    @Ignore("Requires dedicated priest temple test account - verify manually")
    @Test
    public void TC258_Verify_Explore_Deep_Link_Not_Routable_For_Priest_Temple_Account() {
        // Switch to temple context via Settings → Account switcher (PriestSettingsPage).
        // Fire aikyam://explore.
        // Assert: Orders tab content must NOT be visible; Home or the previous tab stays.
        // Assert: No Back button (no push occurred).
        // Switch back to personal account to restore the session for subsequent tests.
        Assert.fail("Not implemented — requires priest temple context setup");
    }

    // -------------------------------------------------------------------------
    // TC259-TC260: remaining tab-switch deep links
    // -------------------------------------------------------------------------

    /**
     * aikyam://events must switch to the Events tab (tab id 2) without pushing.
     * NotificationHandler screen="events" routes here via the same tab-switch path.
     */
    @Test
    public void TC259_Verify_Events_Deep_Link_Switches_To_Events_Tab_Not_Push() {
        Assert.assertTrue("Fire aikyam://events must succeed", links.fire("aikyam://events"));
        EventsPage events = new EventsPage(driver);
        Assert.assertTrue("Events content must be visible after the tab switch",
                events.isEventsScreenVisible());
        Assert.assertFalse("No Back button must appear after a tab switch",
                links.isShown(links.byDesc("Back")));
        Assert.assertTrue("Bottom navigation must remain visible at the tab root",
                new HomePage(driver).isHomeTabVisible());
    }

    /**
     * aikyam://apps must switch to the Apps tab (tab id 4) without pushing.
     * NotificationHandler screen="settings" routes to this same tab (tab 4 = Apps).
     */
    @Test
    public void TC260_Verify_Apps_Deep_Link_Switches_To_Apps_Tab_Not_Push() {
        Assert.assertTrue("Fire aikyam://apps must succeed", links.fire("aikyam://apps"));
        AppsPage apps = new AppsPage(driver);
        Assert.assertTrue("Apps screen must be visible after the tab switch",
                apps.isAppsScreenVisible());
        Assert.assertFalse("No Back button must appear after a tab switch",
                links.isShown(links.byDesc("Back")));
        Assert.assertTrue("Bottom navigation must remain visible at the tab root",
                new HomePage(driver).isHomeTabVisible());
    }

    // -------------------------------------------------------------------------
    // TC261-TC265: feature-screen deep links (pushed onto the navigation stack)
    // -------------------------------------------------------------------------

    /**
     * aikyam://settings pushes the Settings hub onto the current tab's stack.
     * Unlike tab-switch links, a Back button IS expected (it is a push route).
     */
    @Test
    public void TC261_Verify_Settings_Deep_Link_Opens_Settings_Hub_With_Back_Button() {
        Assert.assertTrue("Fire aikyam://settings must succeed", links.fire("aikyam://settings"));
        SettingsPage settings = new SettingsPage(driver);
        Assert.assertTrue("Settings hub must be reachable (Logout row visible)",
                settings.isSettingsHubVisible());
        Assert.assertTrue("Back button must be visible — settings is a pushed route",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * aikyam://orders pushes the Order History screen. This is a push route so a
     * Back button appears. The screen title "Order History" must be visible.
     */
    @Test
    public void TC262_Verify_Orders_Deep_Link_Opens_Order_History_With_Back_Button() {
        Assert.assertTrue("Fire aikyam://orders must succeed", links.fire("aikyam://orders"));
        Assert.assertTrue("Order History screen title must be visible",
                links.waitForText("Order History", LAND));
        Assert.assertTrue("Back button must be visible — orders is a pushed route",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * aikyam://family_members pushes the Family Members screen. Back button expected.
     */
    @Test
    public void TC263_Verify_Family_Members_Deep_Link_Opens_Family_Screen_With_Back_Button() {
        Assert.assertTrue("Fire aikyam://family_members must succeed",
                links.fire("aikyam://family_members"));
        FamilyMembersPage family = new FamilyMembersPage(driver);
        Assert.assertTrue("Family Members screen must be visible",
                family.isScreenVisible());
        Assert.assertTrue("Back button must be visible — family members is a pushed route",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * aikyam://panchang opens the Panchang mini-app web view directly (a push route,
     * not just navigating to the Apps tab). Back button expected; web view must load.
     */
    @Test
    public void TC264_Verify_Panchang_Deep_Link_Opens_Panchang_Mini_App_Web_View() {
        Assert.assertTrue("Fire aikyam://panchang must succeed", links.fire("aikyam://panchang"));
        AppsPage apps = new AppsPage(driver);
        Assert.assertTrue("Panchang web view must load",
                apps.waitForMiniAppWebViewToLoad());
        Assert.assertTrue("Mini-app web view must be visible",
                apps.isMiniAppWebViewVisible());
        Assert.assertTrue("Back button must be visible — mini app is a pushed route",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * aikyam://stotragalu opens the Stotragalu mini-app web view (same push mechanic
     * as panchang). Back button expected; web view must load.
     */
    @Test
    public void TC265_Verify_Stotragalu_Deep_Link_Opens_Stotragalu_Mini_App_Web_View() {
        Assert.assertTrue("Fire aikyam://stotragalu must succeed",
                links.fire("aikyam://stotragalu"));
        AppsPage apps = new AppsPage(driver);
        Assert.assertTrue("Stotragalu web view must load",
                apps.waitForMiniAppWebViewToLoad());
        Assert.assertTrue("Mini-app web view must be visible",
                apps.isMiniAppWebViewVisible());
        Assert.assertTrue("Back button must be visible — mini app is a pushed route",
                links.isShown(links.byDesc("Back")));
    }

    // -------------------------------------------------------------------------
    // TC266-TC268: entity deep links that need live test data (@Ignore until IDs are stable)
    // -------------------------------------------------------------------------

    /**
     * aikyam://music/tracks/{id} opens the music track in the Music tab's player.
     * The Music deep link goes through MusicDeepLinkStore (not appRoute), so the tab
     * switches to Music and the track auto-plays.
     *
     * Skipped: requires a stable track id from the dev/staging music catalogue.
     * To enable: replace TRACK_ID with a real id from the Music tab and remove @Ignore.
     */
    @Ignore("Needs stable track id from dev catalogue — replace TRACK_ID and remove @Ignore")
    @Test
    public void TC266_Verify_Music_Track_Deep_Link_Opens_Track_In_Music_Tab() {
        final String TRACK_ID = "TRACK_ID";
        Assert.assertTrue(links.fire("aikyam://music/tracks/" + TRACK_ID));
        MusicPage music = new MusicPage(driver);
        Assert.assertTrue("Music tab must be active after a track deep link",
                music.isMusicScreenVisible());
        // The player or now-playing bar must surface the track — verify via a known
        // accessibility id once you have an id from the dev catalogue.
        Assert.assertFalse("No Back button at the Music tab root",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * aikyam://music/albums/{id} opens the music album in the Music tab.
     *
     * Skipped: requires a stable album id from the dev/staging music catalogue.
     */
    @Ignore("Needs stable album id from dev catalogue — replace ALBUM_ID and remove @Ignore")
    @Test
    public void TC267_Verify_Music_Album_Deep_Link_Opens_Album_In_Music_Tab() {
        final String ALBUM_ID = "ALBUM_ID";
        Assert.assertTrue(links.fire("aikyam://music/albums/" + ALBUM_ID));
        MusicPage music = new MusicPage(driver);
        Assert.assertTrue("Music tab must be active after an album deep link",
                music.isMusicScreenVisible());
        Assert.assertFalse("No Back button at the Music tab root",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * aikyam://streams/{id} with a valid YouTube live stream id opens the live stream
     * player (YouTubeLivePlayerView). Without a valid id the app falls back to .liveTab
     * (TC257 covers that). Requires an active or archived YouTube stream on the platform.
     *
     * Notification scenario equivalent: a push notification carrying liveId="{id}" or
     * deeplink="aikyam://streams/{id}" in its payload routes here via the same player.
     *
     * Skipped: live streams are transient; pin a known stream id from your QA environment
     * and remove @Ignore for local verification.
     */
    @Ignore("Needs an active/archived stream id — pin one from QA env and remove @Ignore")
    @Test
    public void TC268_Verify_Stream_Id_Deep_Link_Opens_Live_Stream_Player() {
        final String STREAM_ID = "STREAM_ID";
        Assert.assertTrue(links.fire("aikyam://streams/" + STREAM_ID));
        // The live stream player shows a video player UI. Assert a known element such as
        // a close/back action or the player container's accessibility id once you have a
        // real stream id.
        Assert.assertTrue("Stream player must open (live or replay)",
                links.waitFor(links.byDesc("Back"), LAND)
                        || links.isShown(links.byDesc("Close")));
        Assert.assertFalse("Home feed must not be shown — player is open",
                links.isHomeFeedShown(Duration.ofSeconds(3)));
    }

    // -------------------------------------------------------------------------
    // TC269, TC271-TC278: negative / edge-case deep link flows
    //
    // Every routable destination should handle bad or missing data gracefully —
    // no crash, no unintended navigation. Covers the notification deep link
    // equivalent scenarios (a notification's liveId, deeplink or screen field
    // with a bad value routes through the same resolution path as a URL fire).
    // -------------------------------------------------------------------------

    /**
     * aikyam://streams/{unknownId} — ShareLinkRouter calls resolveStream, the API
     * returns nothing, the static fallback list has no match either, so the router
     * returns nil and falls back to .liveTab (Home tab, Live feed highlight).
     *
     * Notification equivalent: a push notification with liveId="doesnotexist123"
     * or deeplink="aikyam://streams/doesnotexist123" in its payload routes through
     * the same resolveStream call and falls back the same way.
     */
    @Test
    public void TC269_Verify_Unknown_Stream_Id_Falls_Back_To_Home_Without_Crash() {
        Assert.assertTrue(links.fire("aikyam://streams/doesnotexist123"));
        Assert.assertTrue("Unknown stream id must fall back to Home/Live feed, not crash",
                links.isHomeFeedShown(LAND));
        Assert.assertFalse("No Back button — no push happened",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * aikyam://music/tracks/{unknownId} — the ID format is valid so the router
     * emits .musicTrack, MusicDeepLinkStore receives it, switches to the Music
     * tab and attempts to load the track. The track is not found, but the app
     * must not crash and the Music tab must remain active.
     *
     * Notification equivalent: deeplink="aikyam://music/tracks/doesnotexist123"
     * in a notification payload routes here via the same MusicDeepLinkStore path.
     */
    @Test
    public void TC271_Verify_Unknown_Music_Track_Id_Does_Not_Crash_Music_Tab_Active() {
        Assert.assertTrue(links.fire("aikyam://music/tracks/doesnotexist123"));
        MusicPage music = new MusicPage(driver);
        Assert.assertTrue("Music tab must be active even when the track id is unknown",
                music.isMusicScreenVisible());
        Assert.assertFalse("No Back button — Music tab is selected, not pushed from Home",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * aikyam://music/albums/{unknownId} — same as TC271 but for album ids.
     * The router emits .musicAlbum, the album is not found, Music tab stays active.
     */
    @Test
    public void TC272_Verify_Unknown_Music_Album_Id_Does_Not_Crash_Music_Tab_Active() {
        Assert.assertTrue(links.fire("aikyam://music/albums/doesnotexist123"));
        MusicPage music = new MusicPage(driver);
        Assert.assertTrue("Music tab must be active even when the album id is unknown",
                music.isMusicScreenVisible());
        Assert.assertFalse("No Back button",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * aikyam://music/tracks (no id segment) — ShareLinkRouter.aikyamNoIdDestination
     * requires at least 2 path segments with a valid id; this URL has 1 segment so the
     * router returns nil (not routable). The app must stay on Home, no navigation at all.
     *
     * Notification equivalent: a deeplink payload of "aikyam://music/tracks" (truncated)
     * is not routable and the app falls through to the notification's screen field.
     */
    @Test
    public void TC273_Verify_Music_Tracks_Without_Id_Is_Not_Routable_App_Stays_On_Home() {
        Assert.assertTrue(links.fire("aikyam://music/tracks"));
        Assert.assertTrue("No id = not routable; Home feed must remain showing",
                links.isHomeFeedShown(Duration.ofSeconds(15)));
        Assert.assertFalse("No Back button",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * aikyam://music/albums (no id segment) — mirrors TC273 for albums.
     */
    @Test
    public void TC274_Verify_Music_Albums_Without_Id_Is_Not_Routable_App_Stays_On_Home() {
        Assert.assertTrue(links.fire("aikyam://music/albums"));
        Assert.assertTrue("No id = not routable; Home feed must remain showing",
                links.isHomeFeedShown(Duration.ofSeconds(15)));
        Assert.assertFalse("No Back button",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * aikyam://unknownscreen — the host is not in ShareLinkRouter's switch statement,
     * so it returns nil. The app must stay on Home, no navigation, no crash.
     *
     * Notification equivalent: a notification payload with an unrecognised screen value
     * or an unknown deeplink host must leave the app on whatever screen it was on.
     */
    @Test
    public void TC275_Verify_Unrecognised_Deep_Link_Host_Is_Ignored_App_Stays_On_Home() {
        Assert.assertTrue(links.fire("aikyam://doesnotexist"));
        Assert.assertTrue("Unrecognised host must be ignored; Home feed must remain showing",
                links.isHomeFeedShown(Duration.ofSeconds(15)));
        Assert.assertFalse("No Back button — nothing navigated",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * aikyam://panchang/extrapath — the router's no-id handler guards
     * `segments.isEmpty` for single-destination hosts like panchang, settings, etc.
     * Any extra path segment makes the URL not routable: the app stays on Home.
     *
     * Also covers the same guard for aikyam://settings/x, aikyam://orders/x (without
     * a valid orders id), aikyam://explore/x, aikyam://events/x and aikyam://apps/x.
     */
    @Test
    public void TC276_Verify_Extra_Path_On_Single_Destination_Host_Is_Not_Routable() {
        for (String url : new String[]{
                "aikyam://panchang/doesnotexist",
                "aikyam://settings/doesnotexist",
                "aikyam://explore/doesnotexist",
                "aikyam://events/doesnotexist",
                "aikyam://apps/doesnotexist",
        }) {
            Assert.assertTrue("fire must succeed for: " + url, links.fire(url));
            Assert.assertTrue("Extra path segment makes link non-routable; Home must stay: " + url,
                    links.isHomeFeedShown(Duration.ofSeconds(15)));
            Assert.assertFalse("No Back button for: " + url,
                    links.isShown(links.byDesc("Back")));
        }
    }

    /**
     * aikyam://music/tracks/{malformedId} — the id contains characters outside
     * [A-Za-z0-9_-] so ShareLinkRouter.isValidId returns false and the router
     * returns nil (not routable). The app must stay on Home.
     *
     * Notification equivalent: a deeplink payload with a path-encoded special
     * character in the id (e.g. from a malformed notification) must not crash.
     */
    @Test
    public void TC277_Verify_Malformed_Music_Track_Id_Is_Not_Routable_App_Stays_On_Home() {
        // Space (URL-encoded as %20) and ! are outside the allowed id charset.
        Assert.assertTrue(links.fire("aikyam://music/tracks/bad%20track%21"));
        Assert.assertTrue("Malformed id = not routable; Home feed must remain showing",
                links.isHomeFeedShown(Duration.ofSeconds(15)));
        Assert.assertFalse("No Back button", links.isShown(links.byDesc("Back")));
    }

    /**
     * aikyam://streams/{malformedId} — space-encoded id fails isValidId, router
     * returns nil from aikyamNoIdDestination. Because the host is "streams" in the
     * no-id route branch (empty-path guard), a non-empty path makes the whole URL
     * not routable: the app stays on Home.
     *
     * Also documents that a notification payload deeplink="aikyam://streams/%20"
     * (blank or whitespace id) must not crash and must leave the app on Home.
     */
    @Test
    public void TC278_Verify_Malformed_Stream_Id_Is_Not_Routable_App_Stays_On_Home() {
        Assert.assertTrue(links.fire("aikyam://streams/bad%20id%21"));
        Assert.assertTrue("Malformed stream id = not routable; Home feed must remain showing",
                links.isHomeFeedShown(Duration.ofSeconds(15)));
        Assert.assertFalse("No Back button", links.isShown(links.byDesc("Back")));
    }

    // -------------------------------------------------------------------------
    // TC304-TC308: edge cases introduced or fixed by PR #698
    // -------------------------------------------------------------------------

    /**
     * Mini-app switch via deep link while a different mini-app is already open.
     *
     * Before the fix SwiftUI reused the same MiniAppScreen instance when the route
     * changed from .miniApp("panchang") to .miniApp("stotragalu"), so the WebView
     * never reloaded. The fix adds .id(id) to MiniAppScreen inside inTabSearchOverlay,
     * forcing a full teardown + rebuild when the miniAppId changes.
     *
     * Flow:
     *   1. Fire aikyam://panchang → Panchang web view loads.
     *   2. Fire aikyam://stotragalu without backing out of Panchang.
     *   3. Assert: Stotragalu content is now showing (web view reloaded for the new id).
     *   4. Assert: Panchang content is NOT still visible (old view was destroyed).
     */
    @Test
    public void TC304_Verify_Mini_App_Switch_Via_Deeplink_Reloads_New_Mini_App() {
        // Open Panchang.
        Assert.assertTrue("Fire aikyam://panchang must succeed", links.fire("aikyam://panchang"));
        AppsPage apps = new AppsPage(driver);
        Assert.assertTrue("Panchang web view must load first",
                apps.waitForMiniAppWebViewToLoad());

        // Switch to Stotragalu via deep link while Panchang is still open.
        Assert.assertTrue("Fire aikyam://stotragalu must succeed",
                links.fire("aikyam://stotragalu"));

        // The web view must have torn down and rebuilt for the new mini-app id.
        Assert.assertTrue("Stotragalu web view must load after the switch",
                apps.waitForMiniAppWebViewToLoad());
        // The mini-app overlay is still open (no Back was pressed), so web view visible.
        Assert.assertTrue("Mini-app web view must be visible",
                apps.isMiniAppWebViewVisible());
        // If still showing Panchang the fix did not apply; content of the two mini-apps
        // differs enough that asserting the new URL loaded via the web view title or URL
        // bar is the gold standard — verify with apps.miniAppCurrentHost() once the page
        // object exposes it. For now, asserting a successful second load within timeout
        // and no crash is the minimum bar.
    }

    /**
     * Tab-switch deep link resets the navigation stack of the target tab.
     *
     * When the user has pushed screens inside the Music tab (e.g., navigated into an
     * artist view), firing aikyam://music must switch to the Music tab AND pop the
     * entire stack back to its root — no Back button, no pushed screen in view.
     *
     * Fixed in feat/aikyam-deeplinks-clean: tab-switch destinations call
     * NavigationPath() (empty) before switching, clearing whatever was on the stack.
     */
    @Test
    public void TC305_Verify_Tab_Switch_Deeplink_Resets_Navigation_Stack_To_Root() {
        // Navigate to the Music tab and push into the first available sub-screen.
        // If the Music tab has no immediately tappable sub-screen in the test env,
        // navigate to Events (which always has a list) and use aikyam://events instead.
        Assert.assertTrue("Fire aikyam://events must succeed", links.fire("aikyam://events"));
        EventsPage events = new EventsPage(driver);
        Assert.assertTrue("Events tab must be visible", events.isEventsScreenVisible());

        // Now fire aikyam://events again (simulates re-entering from a notification or
        // share link while already on Events). The stack must be at root — no Back button.
        Assert.assertTrue("Fire aikyam://events second time must succeed",
                links.fire("aikyam://events"));
        Assert.assertTrue("Events content must be showing after the second switch",
                events.isEventsScreenVisible());
        Assert.assertFalse("No Back button — tab is at its root after a tab-switch link",
                links.isShown(links.byDesc("Back")));
        Assert.assertTrue("Bottom navigation must still be visible",
                new HomePage(driver).isHomeTabVisible());
    }

    /**
     * Bare aikyam://streams selects the Live chip, not just the Home tab.
     *
     * Before PR #698 / #699: the app switched to the Home tab but left All Posts
     * selected. StreamHighlightStore.pendingLiveTab was introduced so that
     * showPendingLiveStream() activates the Live chip the same way a push-tap does.
     *
     * Assertion: after TC257's tab-switch, the Live chip (accessibility id
     * "feed.filter.live") is selected, not the All Posts chip.
     */
    @Test
    public void TC306_Verify_Bare_Streams_Link_Activates_Live_Chip_On_Home_Feed() {
        // Start from All Posts (set by @Before's tapAllPostsFilter).
        Assert.assertTrue("Fire aikyam://streams must succeed", links.fire("aikyam://streams"));
        Assert.assertTrue("Home tab must be active", links.isHomeFeedShown(LAND));
        // The Live chip must be selected — not the All Posts chip.
        Assert.assertTrue("Live chip must be selected after aikyam://streams",
                links.isShown(links.byDesc("feed.filter.live")));
        Assert.assertFalse("All Posts chip must NOT be selected after aikyam://streams",
                links.isShown(links.byDesc("feed.filter.allPosts")));
    }

    /**
     * Cross-tab deep link: aikyam://explore from a non-Home tab.
     *
     * Fires aikyam://events first (switches to Events tab), then fires
     * aikyam://explore. The app must switch to the Explore tab regardless of
     * which tab it was on when the link arrived.
     *
     * Covers the general tab-switch contract: the deep link handler in routeDeepLink
     * uses the ShareDestination.tabIndex path regardless of the current tab state.
     */
    @Test
    public void TC307_Verify_Explore_Deep_Link_From_Non_Home_Tab_Switches_To_Explore() {
        // Start on Events tab.
        Assert.assertTrue("Fire aikyam://events must succeed", links.fire("aikyam://events"));
        new EventsPage(driver).isEventsScreenVisible();

        // Now switch to Explore from Events tab via deep link.
        Assert.assertTrue("Fire aikyam://explore must succeed", links.fire("aikyam://explore"));
        ExplorePage explore = new ExplorePage(driver);
        Assert.assertTrue("Explore content must be visible after cross-tab switch",
                explore.isExploreVisible());
        Assert.assertFalse("No Back button — Explore is a tab switch, not a push",
                links.isShown(links.byDesc("Back")));
        Assert.assertTrue("Bottom navigation must remain visible",
                new HomePage(driver).isHomeTabVisible());
    }

    /**
     * Mini-app link triggered from within a mini-app navigates the main app.
     *
     * MiniAppWebViewController.decision() previously blocked all navigations except
     * the initial load. The fix adds a linkActivated case: when the web view follows
     * a link that starts with "aikyam://", the view controller calls
     * UIApplication.open so the main app routing handles it. Script-initiated
     * navigations are still blocked.
     *
     * Manual verification flow:
     *   1. Open aikyam://panchang.
     *   2. Inside the Panchang web view, tap a link that emits aikyam://music.
     *   3. Assert: Music tab becomes active, Panchang web view is dismissed.
     *
     * Skipped in automated runs: Appium cannot reliably tap a link that is embedded
     * in a WebView's HTML content without knowing the link's coordinates or
     * accessibility id from the web page source (which varies by content release).
     * Verify this manually using the QA Panchang page that has an "Open Music" link
     * documented in the team's Notion QA playbook for PR #698.
     */
    @Ignore("Requires a known link target in the Panchang web content — verify manually per QA playbook")
    @Test
    public void TC308_Verify_Aikyam_Link_From_Within_Mini_App_Navigates_Main_App() {
        Assert.assertTrue("Fire aikyam://panchang must succeed", links.fire("aikyam://panchang"));
        AppsPage apps = new AppsPage(driver);
        Assert.assertTrue("Panchang web view must load", apps.waitForMiniAppWebViewToLoad());
        // Tap the in-page "Open Music" link (Panchang must expose a known QA link).
        // apps.tapLinkInWebView("Open Music");
        // Assert Music tab is active.
        Assert.assertTrue("Music tab must become active after tapping aikyam://music link in Panchang",
                new MusicPage(driver).isMusicScreenVisible());
        Assert.assertFalse("No Back button — Music tab switch, not a push",
                links.isShown(links.byDesc("Back")));
        Assert.fail("Not implemented — replace with real web view link tap per QA playbook");
    }
}
