package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.data.TestData;
import com.shriaikyam.aikyam.pages.AppsPage;
import com.shriaikyam.aikyam.pages.DeepLinkPage;
import com.shriaikyam.aikyam.pages.EventsPage;
import com.shriaikyam.aikyam.pages.ExplorePage;
import com.shriaikyam.aikyam.pages.HomePage;
import com.shriaikyam.aikyam.pages.MusicPage;
import com.shriaikyam.aikyam.pages.SettingsPage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;

/**
 * End-to-end notification deep link journeys.
 *
 * Each test fires a real APNS payload at the dev simulator bundle via
 * {@code xcrun simctl push} (DeepLinkPage.fireAndTapNotification), taps the
 * resulting system banner, and asserts the final navigation destination.
 *
 * This is the Appium counterpart to NotificationHandlerRoutingTests.swift. Those
 * Swift unit tests verify the payload → destination mapping in isolation; these
 * tests verify the full path: APNS payload → notification tap → in-app navigation.
 *
 * HOW IT WORKS
 *   1. App is backgrounded (Home button press).
 *   2. xcrun simctl push delivers the APNS JSON.
 *   3. iOS shows a notification banner (AppDelegate: completionHandler([.banner])).
 *   4. Appium taps the banner.
 *   5. AppDelegate.didReceive calls rememberLaunchNotification and posts
 *      aikyamPushNotificationTapped, which causes RootView to navigate.
 *   6. Test asserts the resulting screen.
 *
 * NOTIFICATION FIELD PRIORITY (from NotificationHandler):
 *   postId > liveId > deeplink/deep_link/deepLink > screen
 */
public class NotificationJourneyTest extends BaseTest {

    private static final Duration LAND    = Duration.ofSeconds(30);
    private static final Duration BANNER  = Duration.ofSeconds(10);
    private static final String   NOTIF_TITLE = "Aikyam";

    private DeepLinkPage links;

    @Before
    public void startOnHome() {
        links = new DeepLinkPage(driver);
        new HomePage(driver).tapHomeTab();
    }

    @After
    public void leaveOnHome() {
        try { links.returnHome(); } catch (RuntimeException ignored) {}
    }

    // =========================================================================
    // screen field — tab-switch routing
    // =========================================================================

    /**
     * screen="explore" → NotificationHandler emits .tab(MainTabView.exploreTabId).
     * RootView switches to the Explore tab; no Back button (tab switch).
     */
    @Test
    public void TC280_Notify_Screen_Explore_Switches_To_Explore_Tab() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Open Explore",
                DeepLinkPage.payload("screen", "explore"),
                "Open Explore", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("Explore tab must be selected after notification tap",
                new ExplorePage(driver).isExploreVisible());
        Assert.assertFalse("No Back button — Explore is a tab switch, not a push",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * screen="home" → NotificationHandler emits .tab(0).
     * App lands on (or stays on) the Home tab with the feed showing.
     */
    @Test
    public void TC281_Notify_Screen_Home_Stays_On_Home_Feed() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Open Home",
                DeepLinkPage.payload("screen", "home"),
                "Open Home", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("Home feed must be visible after screen=home notification",
                links.isHomeFeedShown(LAND));
    }

    /**
     * screen="community" → NotificationHandler emits .tab(0) (same as home).
     * This is the standard "community" push category mapping.
     */
    @Test
    public void TC282_Notify_Screen_Community_Maps_To_Home_Feed() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Community update",
                DeepLinkPage.payload("screen", "community"),
                "Community update", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("screen=community must map to Home feed",
                links.isHomeFeedShown(LAND));
    }

    /**
     * screen="settings" → NotificationHandler emits .tab(4) (the Apps tab, NOT the
     * Settings screen). This is intentional: the notification handler's screen-based
     * routing uses tab indices, not feature screens.
     */
    @Test
    public void TC283_Notify_Screen_Settings_Switches_To_Apps_Tab() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Settings reminder",
                DeepLinkPage.payload("screen", "settings"),
                "Settings reminder", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("screen=settings routes to Apps tab (tab 4), not the settings screen",
                new AppsPage(driver).isAppsScreenVisible());
        Assert.assertFalse("No Back button — this is a tab switch",
                links.isShown(links.byDesc("Back")));
    }

    // =========================================================================
    // deeplink field — URL routing
    // =========================================================================

    /**
     * deeplink="aikyam://explore" → NotificationHandler extracts the deeplink,
     * ShareLinkRouter resolves it to .explore, RootView switches to the Explore tab.
     */
    @Test
    public void TC284_Notify_Deeplink_Explore_Switches_To_Explore_Tab() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Discover temples near you",
                DeepLinkPage.payload("deeplink", "aikyam://explore"),
                "Discover temples", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("deeplink=aikyam://explore must switch to Explore tab",
                new ExplorePage(driver).isExploreVisible());
        Assert.assertFalse("No Back button", links.isShown(links.byDesc("Back")));
    }

    /**
     * deeplink="aikyam://events" → Explore tab with Events content.
     */
    @Test
    public void TC285_Notify_Deeplink_Events_Switches_To_Events_Tab() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Upcoming events",
                DeepLinkPage.payload("deeplink", "aikyam://events"),
                "Upcoming events", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("deeplink=aikyam://events must switch to Events tab",
                new EventsPage(driver).isEventsScreenVisible());
        Assert.assertFalse("No Back button", links.isShown(links.byDesc("Back")));
    }

    /**
     * deeplink="aikyam://panchang" → Panchang mini-app web view opens.
     * This is the standard "panchang reminder" push category.
     */
    @Test
    public void TC286_Notify_Deeplink_Panchang_Opens_Panchang_Mini_App() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Today's Panchang",
                DeepLinkPage.payload("deeplink", "aikyam://panchang"),
                "Today's Panchang", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        AppsPage apps = new AppsPage(driver);
        Assert.assertTrue("Panchang web view must load after notification tap",
                apps.waitForMiniAppWebViewToLoad());
        Assert.assertTrue("Mini-app web view must be visible",
                apps.isMiniAppWebViewVisible());
    }

    /**
     * deeplink="aikyam://settings" → Settings hub pushed (a push route,
     * NOT a tab switch — different from screen="settings" which goes to Apps tab).
     */
    @Test
    public void TC287_Notify_Deeplink_Settings_Opens_Settings_Hub() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Update your profile",
                DeepLinkPage.payload("deeplink", "aikyam://settings"),
                "Update your profile", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("deeplink=aikyam://settings must open the Settings hub",
                new SettingsPage(driver).isSettingsHubVisible());
        Assert.assertTrue("Back button must be visible — settings is a push route",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * deeplink key variant: "deep_link" (underscore) must work the same as "deeplink".
     * NotificationHandler.extractRoutingPayload recognises all three: deeplink,
     * deep_link, deepLink.
     */
    @Test
    public void TC288_Notify_DeepLink_Underscore_Key_Variant_Routes_To_Explore() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Explore temples",
                DeepLinkPage.payload("deep_link", "aikyam://explore"),
                "Explore temples", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("deep_link key variant must route to Explore tab",
                new ExplorePage(driver).isExploreVisible());
        Assert.assertFalse("No Back button", links.isShown(links.byDesc("Back")));
    }

    /**
     * deeplink key variant: "deepLink" (camelCase) must work the same.
     */
    @Test
    public void TC289_Notify_DeepLink_CamelCase_Key_Variant_Routes_To_Explore() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Find your temple",
                DeepLinkPage.payload("deepLink", "aikyam://explore"),
                "Find your temple", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("deepLink camelCase key variant must route to Explore tab",
                new ExplorePage(driver).isExploreVisible());
        Assert.assertFalse("No Back button", links.isShown(links.byDesc("Back")));
    }

    // =========================================================================
    // liveId field — live stream routing
    // =========================================================================

    /**
     * liveId with a known stable YouTube videoId (ISKCON Bangalore, from the app's
     * own YoutubeTempleStreams fallback list) → live stream player opens.
     * resolveStream finds the entry in the static list even when the API is offline.
     */
    @Test
    public void TC290_Notify_LiveId_Known_Stream_Opens_Live_Player() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "ISKCON Bangalore is live!",
                DeepLinkPage.payload("liveId", TestData.STREAM_VIDEO_ID_ISKCON),
                "ISKCON Bangalore is live", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        // The live stream player shows either a video player UI or a back/close action.
        Assert.assertTrue("Stream player or close button must appear",
                links.waitFor(links.byDesc("Back"), LAND)
                        || links.waitFor(links.byDesc("Close"), LAND));
        Assert.assertFalse("Home feed must not be showing — stream player is open",
                links.isHomeFeedShown(Duration.ofSeconds(3)));
    }

    /**
     * liveId with the second known videoId (Shirdi Saibaba) — same assertion,
     * verifies the fallback list is iterated, not just the first entry.
     */
    @Test
    public void TC291_Notify_LiveId_Shirdi_Stream_Opens_Live_Player() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Shirdi Saibaba Sansthan live",
                DeepLinkPage.payload("liveId", TestData.STREAM_VIDEO_ID_SHIRDI),
                "Shirdi Saibaba Sansthan live", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("Stream player or close button must appear",
                links.waitFor(links.byDesc("Back"), LAND)
                        || links.waitFor(links.byDesc("Close"), LAND));
        Assert.assertFalse("Home feed must not be showing",
                links.isHomeFeedShown(Duration.ofSeconds(3)));
    }

    /**
     * liveId that does not exist in the API or the static fallback list →
     * resolveStream returns nil → falls back to .liveTab → Home showing, no crash.
     */
    @Test
    public void TC292_Notify_LiveId_Unknown_Falls_Back_To_Home_No_Crash() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Live now",
                DeepLinkPage.payload("liveId", "doesnotexist123"),
                "Live now", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("Unknown liveId must fall back to Home/Live feed",
                links.isHomeFeedShown(LAND));
    }

    /**
     * liveId=" " (whitespace only) → NotificationHandler trims and treats as empty →
     * falls through to the screen field → screen="explore" → Explore tab.
     * This matches testBlankLiveIdFallsThroughToScreen in the Swift unit tests.
     */
    @Test
    public void TC293_Notify_Blank_LiveId_Falls_Through_To_Screen_Field() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Open Explore",
                DeepLinkPage.payload("liveId", "   ", "screen", "explore"),
                "Open Explore", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("Blank liveId must fall through to screen=explore",
                new ExplorePage(driver).isExploreVisible());
    }

    /**
     * deeplink="aikyam://streams/{videoId}" in the notification payload routes to
     * the live stream player (same resolution as liveId).
     * This is the streaming-notification format used for push-to-player flows.
     */
    @Test
    public void TC294_Notify_Deeplink_StreamId_Opens_Live_Player() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "ISKCON live stream",
                DeepLinkPage.payload("deeplink", "aikyam://streams/" + TestData.STREAM_VIDEO_ID_ISKCON),
                "ISKCON live stream", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("Stream player or close must appear after deeplink=aikyam://streams/{id}",
                links.waitFor(links.byDesc("Back"), LAND)
                        || links.waitFor(links.byDesc("Close"), LAND));
        Assert.assertFalse("Home feed must not be showing",
                links.isHomeFeedShown(Duration.ofSeconds(3)));
    }

    // =========================================================================
    // Priority ordering — postId > liveId > deeplink > screen
    // =========================================================================

    /**
     * Payload has both liveId and screen="explore".
     * liveId takes priority → live stream player opens, NOT the Explore tab.
     * Matches testLiveIdTakesPriorityOverScreen in the Swift unit tests.
     */
    @Test
    public void TC295_Notify_LiveId_Takes_Priority_Over_Screen_Field() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Live now",
                DeepLinkPage.payload("liveId", TestData.STREAM_VIDEO_ID_ISKCON, "screen", "explore"),
                "Live now", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        // Stream player must open, NOT the Explore tab.
        Assert.assertTrue("liveId must take priority; stream player must open",
                links.waitFor(links.byDesc("Back"), LAND)
                        || links.waitFor(links.byDesc("Close"), LAND));
        Assert.assertFalse("Explore tab must NOT be shown when liveId is present",
                new ExplorePage(driver).isExploreVisible());
    }

    /**
     * Payload has postId (unknown) and liveId (known stream).
     * postId takes priority → post-load error screen, NOT the stream player.
     * Matches testPostIdTakesPriorityOverLiveId in the Swift unit tests.
     */
    @Test
    public void TC296_Notify_PostId_Takes_Priority_Over_LiveId() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Shared post",
                DeepLinkPage.payload(
                        "postId", "doesnotexist123",
                        "liveId", TestData.STREAM_VIDEO_ID_ISKCON),
                "Shared post", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        // Post load error must show, not the stream player.
        Assert.assertTrue("postId must take priority; post error or detail must show",
                links.waitForText("Unable to load this shared post.", LAND)
                        || links.waitFor(links.byDesc("Back"), LAND));
        Assert.assertFalse("Stream player must NOT open when postId is present",
                links.waitFor(links.byDesc("Close"), Duration.ofSeconds(3)));
    }

    /**
     * Payload has liveId (known stream) and deeplink="aikyam://streams/{otherId}".
     * liveId takes priority over deeplink.
     * Matches testLiveIdTakesPriorityOverStreamDeeplink in the Swift unit tests.
     */
    @Test
    public void TC297_Notify_LiveId_Takes_Priority_Over_Deeplink_Stream() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Two streams",
                DeepLinkPage.payload(
                        "liveId", TestData.STREAM_VIDEO_ID_ISKCON,
                        "deeplink", "aikyam://streams/" + TestData.STREAM_VIDEO_ID_SHIRDI),
                "Two streams", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("liveId must take priority over deeplink; stream player must open",
                links.waitFor(links.byDesc("Back"), LAND)
                        || links.waitFor(links.byDesc("Close"), LAND));
    }

    /**
     * deeplink present and screen field present.
     * deeplink takes priority over screen.
     * Matches testStreamDeeplinkTakesPriorityOverScreenFallback in the Swift unit tests.
     */
    @Test
    public void TC298_Notify_Deeplink_Takes_Priority_Over_Screen_Field() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Live stream",
                DeepLinkPage.payload(
                        "deeplink", "aikyam://streams/" + TestData.STREAM_VIDEO_ID_ISKCON,
                        "screen", "community"),
                "Live stream", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("deeplink must take priority over screen=community",
                links.waitFor(links.byDesc("Back"), LAND)
                        || links.waitFor(links.byDesc("Close"), LAND));
        Assert.assertFalse("Home feed must NOT show — deeplink opened the stream",
                links.isHomeFeedShown(Duration.ofSeconds(3)));
    }

    // =========================================================================
    // Error / edge cases
    // =========================================================================

    /**
     * Notification with no routing payload at all — no postId, liveId, deeplink
     * or recognised screen. The app must not crash; it stays wherever it was.
     */
    @Test
    public void TC299_Notify_No_Routing_Payload_Does_Not_Crash() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Generic notification",
                DeepLinkPage.payload("screen", "unknown_screen_value"),
                "Generic notification", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        // After tapping a non-routing notification the app comes to the foreground
        // but stays on whatever screen it was at. Home feed is the setUp() state.
        Assert.assertTrue("App must come to foreground without crashing",
                links.isHomeFeedShown(LAND) || links.isShown(links.byDesc("tab.home")));
    }

    /**
     * postId with an unknown id → post load error screen shows, app does not crash.
     * Back button returns to Home.
     * Notification equivalent of DeepLinkJourneyTest.TC244 (URL-fire variant).
     */
    @Test
    public void TC300_Notify_PostId_Unknown_Shows_Load_Error_And_Back_Returns_Home() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Shared post",
                DeepLinkPage.payload("postId", "doesnotexist123"),
                "Shared post", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("Post load error must show for unknown postId",
                links.waitForText("Unable to load this shared post.", LAND));
        Assert.assertTrue("Back button must be offered", links.isShown(links.byDesc("Back")));
        driver.findElement(links.byDesc("Back")).click();
        Assert.assertTrue("Back from post error must land on Home",
                links.isHomeFeedShown(Duration.ofSeconds(15)));
    }

    // =========================================================================
    // @Ignore stubs — require real entity IDs from the dev catalogue
    // =========================================================================

    /**
     * postId with a REAL post id → post detail opens.
     * Populate TestData.POST_ID with a real id from the dev feed, then remove @Ignore.
     */
    @Ignore("Needs TestData.POST_ID — populate from dev feed and remove @Ignore")
    @Test
    public void TC301_Notify_PostId_Real_Opens_Post_Detail() {
        Assert.assertTrue(TestData.isSet(TestData.POST_ID));
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Someone shared a post",
                DeepLinkPage.payload("postId", TestData.POST_ID),
                "Someone shared a post", BANNER);
        Assert.assertTrue(tapped);
        Assert.assertTrue("Post detail composer must appear",
                links.waitFor(links.byDesc("postDetail.commentInput"), LAND));
        Assert.assertTrue("Back button must be offered",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * screen="user_profile" with a real userId → user profile opens.
     * Populate TestData.USER_ID and remove @Ignore.
     */
    @Ignore("Needs TestData.USER_ID — populate from dev profile and remove @Ignore")
    @Test
    public void TC302_Notify_Screen_UserProfile_Opens_Profile() {
        Assert.assertTrue(TestData.isSet(TestData.USER_ID));
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Profile update",
                DeepLinkPage.payload("screen", "user_profile", "userId", TestData.USER_ID),
                "Profile update", BANNER);
        Assert.assertTrue(tapped);
        Assert.assertTrue("Back button must be offered (profile is a push route)",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * deeplink="aikyam://music/tracks/{id}" → music track opens in Music tab.
     * Populate TestData.MUSIC_TRACK_ID and remove @Ignore.
     */
    @Ignore("Needs TestData.MUSIC_TRACK_ID — populate from dev music catalogue and remove @Ignore")
    @Test
    public void TC303_Notify_Deeplink_MusicTrack_Opens_Music_Player() {
        Assert.assertTrue(TestData.isSet(TestData.MUSIC_TRACK_ID));
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "New track available",
                DeepLinkPage.payload("deeplink", "aikyam://music/tracks/" + TestData.MUSIC_TRACK_ID),
                "New track available", BANNER);
        Assert.assertTrue(tapped);
        Assert.assertTrue("Music tab must be active",
                new MusicPage(driver).isMusicScreenVisible());
    }

    // =========================================================================
    // TC309-TC314: edge cases introduced or fixed by PR #698
    // =========================================================================

    /**
     * screen="store" (legacy alias) → Explore tab, not Events tab.
     *
     * Before PR #698 NotificationHandler mapped "explore" to .tab(2) (Events) and
     * "store" had the same wrong mapping. Both now use MainTabView.exploreTabId (3)
     * so this legacy push category opens the correct screen.
     *
     * Distinct from TC280 (screen="explore") which verifies the primary value —
     * this test covers the "store" alias which many older push campaigns still send.
     */
    @Test
    public void TC309_Notify_Screen_Store_Legacy_Alias_Switches_To_Explore_Tab() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Find temples near you",
                DeepLinkPage.payload("screen", "store"),
                "Find temples near you", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("screen=store (legacy alias) must switch to Explore tab (tab 3), not Events",
                new ExplorePage(driver).isExploreVisible());
        Assert.assertFalse("No Back button — Explore is a tab switch",
                links.isShown(links.byDesc("Back")));
        // Guard: Events tab must NOT be open (the pre-fix wrong mapping routed here).
        Assert.assertFalse("Events tab must NOT be active for screen=store",
                new EventsPage(driver).isEventsScreenVisible());
    }

    /**
     * Tab-switch deeplink (aikyam://music) in notification payload with screen="explore".
     * deeplink=aikyam://music is a tab-only destination (tabIndex only, no appRoute, no
     * musicDeepLink), so it is not specific enough to override the notification's screen
     * field. The screen field wins → Explore tab.
     *
     * Fixed by PR #698 commit "fix(notifications): tab-switch deeplinks fall through to
     * screen field". Before the fix, the tabIndex early return in
     * resolveNavigationDestination meant aikyam://music switched to the Music tab even
     * when the backend intended screen="explore".
     *
     * This is intentionally different from TC298 where deeplink=aikyam://streams/{id}
     * IS specific enough (it has an appRoute / .liveStream destination) to win over
     * the screen field.
     */
    @Test
    public void TC310_Notify_Tab_Switch_Deeplink_Yields_To_Screen_Field() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Open Explore",
                DeepLinkPage.payload("deeplink", "aikyam://music", "screen", "explore"),
                "Open Explore", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        // screen="explore" must win — Explore tab, not Music tab.
        Assert.assertTrue("screen=explore must win over deeplink=aikyam://music (tab-only link)",
                new ExplorePage(driver).isExploreVisible());
        Assert.assertFalse("Music tab must NOT be active",
                new MusicPage(driver).isMusicScreenVisible());
        Assert.assertFalse("No Back button — Explore is a tab switch",
                links.isShown(links.byDesc("Back")));
    }

    /**
     * postId always wins regardless of the screen field value — even non-feed screens.
     *
     * iOS bug (pre-PR #698): NotificationHandler.resolveNavigationDestination gated
     * postId routing on `screen ∈ {shared_post, feed, post, post_detail}`. When screen
     * was set to any other value (e.g., "settings", "community_post") the postId was
     * silently dropped and the screen field was used instead.
     *
     * Android always routes to post when postId is present, regardless of screen.
     * The fix removes the screen conditional — postId always takes priority.
     *
     * Test: postId=doesnotexist123 with screen="settings" → post load error screen
     * (not the Apps/Settings tab, which is what the old code would have shown).
     */
    @Test
    public void TC311_Notify_PostId_Always_Wins_Regardless_Of_Screen_Field() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Someone liked your post",
                DeepLinkPage.payload("postId", "doesnotexist123", "screen", "settings"),
                "Someone liked your post", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        // Post load error (or post detail) must show, NOT the Apps/Settings tab.
        Assert.assertTrue("postId must win over screen=settings; post error or detail must show",
                links.waitForText("Unable to load this shared post.", LAND)
                        || links.waitFor(links.byDesc("Back"), LAND));
        Assert.assertFalse("Apps tab must NOT be active — postId took priority",
                new AppsPage(driver).isAppsScreenVisible());
    }

    /**
     * postId always wins regardless of screen="events" (another non-feed screen).
     *
     * Guards against regression to the pre-PR #698 iOS-specific gating logic.
     * Pairs with TC311 to cover two distinct non-feed screen values.
     */
    @Test
    public void TC312_Notify_PostId_Wins_Over_Screen_Events() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Post update",
                DeepLinkPage.payload("postId", "doesnotexist123", "screen", "events"),
                "Post update", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("postId must win over screen=events; post error or detail must show",
                links.waitForText("Unable to load this shared post.", LAND)
                        || links.waitFor(links.byDesc("Back"), LAND));
        Assert.assertFalse("Events tab must NOT be active — postId took priority",
                new EventsPage(driver).isEventsScreenVisible());
    }

    /**
     * deeplink="aikyam://streams/{id}" in notification payload takes priority over
     * screen field, even when screen is a valid non-community value.
     *
     * iOS bug (pre-PR #698): when screen was set to something other than nil, the
     * deeplink field was only tried as a last resort (after all other checks returned
     * nil). Android tries aikyam:// deeplinks BEFORE the screen fallback.
     *
     * Fixed in PR #698: extract streamId from aikyam://streams/<id> in the deeplink
     * field and return .liveStream before falling to the screen handler.
     *
     * Test: deeplink=aikyam://streams/{ISKCON_id} with screen="explore" →
     * stream player opens, NOT the Explore tab.
     */
    @Test
    public void TC313_Notify_Stream_Deeplink_Wins_Over_Non_Community_Screen_Field() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Live stream now",
                DeepLinkPage.payload(
                        "deeplink", "aikyam://streams/" + TestData.STREAM_VIDEO_ID_ISKCON,
                        "screen", "explore"),
                "Live stream now", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        // Stream player must open — deeplink takes priority over screen="explore".
        Assert.assertTrue("Stream player must open (deeplink wins over screen=explore)",
                links.waitFor(links.byDesc("Back"), LAND)
                        || links.waitFor(links.byDesc("Close"), LAND));
        Assert.assertFalse("Explore tab must NOT be active",
                new ExplorePage(driver).isExploreVisible());
        Assert.assertFalse("Home feed must not be showing — stream player is open",
                links.isHomeFeedShown(Duration.ofSeconds(3)));
    }

    /**
     * liveId notification for a priest temple account — stream still opens directly
     * in the player (not routed to Orders tab via the shared tab index).
     *
     * PR #698 guards tab-switch links for priests, but liveId routing uses
     * resolveStream / routeDeepLink (not the tab-switch path), so it must work for
     * priests too — the stream player opens directly.
     *
     * Skipped: requires a dedicated priest temple test account and a temple context
     * switch. Verify manually using the QA priest account in the Notion test accounts.
     */
    /**
     * TC316 — G4: Bare {@code deeplink=aikyam://streams} (no stream id) in a notification
     * payload produces a {@code .liveTab} destination which has no {@code appRoute} and no
     * {@code musicDeepLink} — the notification handler returns {@code nil} and the notification
     * is silently not routed. PR #698 did not introduce a handler for this case; this test
     * documents and guards the current behaviour (home feed stays, no crash).
     *
     * Distinct from TC306 (direct aikyam://streams deeplink works) and TC310 (tab-switch
     * deeplink in notification yields to screen field).
     */
    @Test
    public void TC316_Notify_Bare_Streams_Deeplink_Without_Id_Is_Silently_Not_Routed() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Live now",
                DeepLinkPage.payload("deeplink", "aikyam://streams"),
                "Live now", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        // No screen field, no id — routing returns nil; app must stay on home feed.
        Assert.assertTrue("Home feed must still be visible — bare streams deeplink in notification is not routed",
                links.isHomeFeedShown(Duration.ofSeconds(8)));
    }

    /**
     * TC317 — G5: {@code deeplink=aikyam://orders} in a notification payload routes via
     * {@code ShareDestination.ordersHistory.appRoute} to the orders history screen.
     * TC287 covers {@code aikyam://settings}; this covers the sibling {@code orders} destination
     * which has its own {@code appRoute} entry added in PR #698.
     */
    @Test
    public void TC317_Notify_Orders_Deeplink_Opens_Orders_History_Screen() {
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "Your booking is confirmed",
                DeepLinkPage.payload("deeplink", "aikyam://orders"),
                "Your booking is confirmed", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("Orders history screen must be visible",
                links.waitFor(links.byDesc("ordersHistory.screen"), LAND)
                        || links.waitForText("Orders", LAND)
                        || links.waitForText("Bookings", LAND));
        Assert.assertFalse("No Home feed — orders screen is in the foreground",
                links.isHomeFeedShown(Duration.ofSeconds(3)));
    }

    @Ignore("Requires dedicated priest temple test account — verify manually")
    @Test
    public void TC314_Notify_LiveId_Opens_Stream_Player_For_Priest_Temple_Account() {
        // Switch to temple context via Settings → Account switcher.
        // Fire liveId notification.
        boolean tapped = links.fireAndTapNotification(
                NOTIF_TITLE, "ISKCON live",
                DeepLinkPage.payload("liveId", TestData.STREAM_VIDEO_ID_ISKCON),
                "ISKCON live", BANNER);
        Assert.assertTrue("Notification must be delivered and banner tapped", tapped);
        Assert.assertTrue("Stream player must open for priest temple account too",
                links.waitFor(links.byDesc("Back"), LAND)
                        || links.waitFor(links.byDesc("Close"), LAND));
        Assert.assertFalse("Home feed must not be showing — stream player is open",
                links.isHomeFeedShown(Duration.ofSeconds(3)));
        Assert.fail("Not implemented — requires priest temple context setup");
    }
}
