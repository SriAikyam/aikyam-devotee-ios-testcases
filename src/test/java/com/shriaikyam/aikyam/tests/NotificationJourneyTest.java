package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.data.TestData;
import com.shriaikyam.aikyam.pages.DeepLinkPage;
import com.shriaikyam.aikyam.pages.ExplorePage;
import com.shriaikyam.aikyam.pages.HomePage;
import com.shriaikyam.aikyam.pages.MusicPage;
import com.shriaikyam.aikyam.pages.ProfilePage;
import com.shriaikyam.aikyam.pages.SettingsPage;
import com.shriaikyam.aikyam.utils.DeviceControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

import static com.shriaikyam.aikyam.pages.DeepLinkPage.payload;

/**
 * Push notification journeys: APNS payload -> banner tap -> in-app destination. The Appium
 * counterpart of NotificationHandlerRoutingTests.swift, which checks the payload mapping alone.
 *
 * Each case backgrounds the app, delivers a real payload with `xcrun simctl push`, taps the
 * banner and asserts where the app lands. Routing order (NotificationHandler, then
 * RootView.consumePendingPushNavigationIfReady):
 *   postId > liveId > aikyam://streams/{id} > deeplink with an app route > screen
 *   > any other deeplink, routed like a fired URL.
 *
 * Simulator only. Notification permission is granted once per run (DeepLinkPage.allowNotifications).
 */
public class NotificationJourneyTest extends BaseTest {

    private static final Duration LAND = Duration.ofSeconds(30);
    private static final Duration SETTLE = Duration.ofSeconds(5);
    private static final String UNKNOWN = TestData.UNKNOWN_ID;
    private static final String ISKCON = TestData.STREAM_VIDEO_ID_ISKCON;
    private static final String SHIRDI = TestData.STREAM_VIDEO_ID_SHIRDI;
    private static final String BANNER_MISSED = "The banner never appeared - is notification permission "
            + "granted for the dev bundle? (applesimutils grants it; see README)";

    private static boolean permissionGranted;

    private DeepLinkPage links;

    @Before
    public void startOnHome() {
        Assume.assumeTrue(DeviceControl.SIMULATOR_ONLY_PUSH, DeviceControl.isSimulator());
        links = new DeepLinkPage(driver);
        if (!permissionGranted) {
            links.allowNotifications();
            permissionGranted = true;
        }
        new HomePage(driver).tapHomeTab();
        // Start from All Posts so the Live-feed cases can see the filter change.
        links.selectAllPosts();
        Assert.assertTrue("Precondition: Home feed on All Posts", links.isHomeFeedShown(LAND));
    }

    @After
    public void leaveOnHome() {
        try {
            // A missed banner leaves the app backgrounded; returnHome() would then act on SpringBoard.
            driver.activateApp(DeepLinkPage.DEV_BUNDLE_ID);
            links.returnHome();
        } catch (RuntimeException ignored) {
            // cleanup must not mask the test verdict
        }
    }

    private void notifyAndTap(String body, Object... payloadPairs) {
        Assert.assertTrue(BANNER_MISSED, links.notifyAndTap(body, payload(payloadPairs)));
    }

    /** For cases whose destination is Home: start somewhere else, or they pass without routing. */
    private void startOnEventsTab() {
        links.fire("aikyam://events");
        Assert.assertTrue("Precondition: Events tab selected", links.waitForTabSelected("tab.events", LAND));
    }

    private void assertTab(String tabId) {
        Assert.assertTrue(tabId + " must be selected", links.waitForTabSelected(tabId, LAND));
        Assert.assertFalse("A tab switch pushes nothing", links.isBackShownNow());
    }

    /** The player only opens when the stream id resolves; an id that does not falls back to the
     * Live feed instead. So "player open" also proves which of two competing ids won when one of
     * them is unknown - which is how the priority cases below are built. */
    private void assertLivePlayer() {
        Assert.assertTrue("The live stream player must open", links.isLivePlayerShown(LAND));
    }

    // =========================================================================
    // screen field
    // =========================================================================

    @Test
    public void TC280_Notify_Screen_Explore_Switches_To_Explore_Tab() {
        notifyAndTap("Open Explore", "screen", "explore");
        assertTab("tab.explore");
        Assert.assertTrue("Explore content must show", new ExplorePage(driver).isExploreVisible());
    }

    @Test
    public void TC281_Notify_Screen_Home_Switches_To_Home_Tab() {
        startOnEventsTab();
        notifyAndTap("Open Home", "screen", "home");
        assertTab("tab.home");
        Assert.assertTrue("Home feed must show", links.isHomeFeedShown(LAND));
    }

    /** Android's "community" is the feed, which iOS hosts on Home. */
    @Test
    public void TC282_Notify_Screen_Community_Switches_To_Home_Tab() {
        startOnEventsTab();
        notifyAndTap("Community update", "screen", "community");
        assertTab("tab.home");
        Assert.assertTrue("Home feed must show", links.isHomeFeedShown(LAND));
    }

    /** screen="settings" maps to tab 4 (Apps), not the Settings screen - contrast TC287. */
    @Test
    public void TC283_Notify_Screen_Settings_Switches_To_Apps_Tab() {
        notifyAndTap("Settings reminder", "screen", "settings");
        assertTab("tab.apps");
    }

    /** Legacy alias still sent by older campaigns; before PR #698 it opened Events. */
    @Test
    public void TC309_Notify_Screen_Store_Legacy_Alias_Switches_To_Explore_Tab() {
        notifyAndTap("Find temples near you", "screen", "store");
        assertTab("tab.explore");
        Assert.assertFalse("Not Events (the pre-fix mapping)", links.isTabSelected("tab.events"));
    }

    // =========================================================================
    // deeplink field (and its deep_link / deepLink spellings)
    // =========================================================================

    @Test
    public void TC284_Notify_Deeplink_Explore_Switches_To_Explore_Tab() {
        notifyAndTap("Discover temples near you", "deeplink", "aikyam://explore");
        assertTab("tab.explore");
    }

    @Test
    public void TC285_Notify_Deeplink_Events_Switches_To_Events_Tab() {
        notifyAndTap("Upcoming events", "deeplink", "aikyam://events");
        assertTab("tab.events");
    }

    @Test
    public void TC286_Notify_Deeplink_Panchang_Opens_Panchang_Mini_App() {
        notifyAndTap("Today's Panchang", "deeplink", "aikyam://panchang");
        Assert.assertTrue("The Panchang mini-app must open", links.isMiniAppShown("Panchang", LAND));
    }

    /** deeplink=aikyam://settings has an app route, so it pushes the Settings hub. */
    @Test
    public void TC287_Notify_Deeplink_Settings_Opens_Settings_Hub() {
        notifyAndTap("Update your profile", "deeplink", "aikyam://settings");
        Assert.assertTrue("Settings hub must open", new SettingsPage(driver).isSettingsHubVisible());
        Assert.assertTrue("Settings is pushed - Back must be offered", links.isBackShownNow());
    }

    @Test
    public void TC288_Notify_DeepLink_Underscore_Key_Variant_Routes_To_Explore() {
        notifyAndTap("Explore temples", "deep_link", "aikyam://explore");
        assertTab("tab.explore");
    }

    @Test
    public void TC289_Notify_DeepLink_CamelCase_Key_Variant_Routes_To_Explore() {
        notifyAndTap("Find your temple", "deepLink", "aikyam://explore");
        assertTab("tab.explore");
    }

    /** A tab-only deeplink has no app route, so it yields to the screen field (PR #698). */
    @Test
    public void TC310_Notify_Tab_Switch_Deeplink_Yields_To_Screen_Field() {
        notifyAndTap("Open Explore", "deeplink", "aikyam://music", "screen", "explore");
        assertTab("tab.explore");
        Assert.assertFalse("The tab-only deeplink must not win", links.isTabSelected("tab.music"));
    }

    /** Bare aikyam://streams resolves to nothing in NotificationHandler, so RootView routes the
     * deeplink as a URL: .liveTab, i.e. Home showing the Live feed. */
    @Test
    public void TC316_Notify_Bare_Streams_Deeplink_Opens_Live_Feed() {
        notifyAndTap("Live now", "deeplink", "aikyam://streams");
        Assert.assertTrue("The Live feed must open", links.isLiveFeedShown(LAND));
        Assert.assertTrue("Home must be the selected tab", links.isTabSelected("tab.home"));
    }

    @Test
    public void TC317_Notify_Orders_Deeplink_Opens_Orders_History_Screen() {
        notifyAndTap("Your booking is confirmed", "deeplink", "aikyam://orders");
        Assert.assertTrue("Order History must open",
                links.waitFor(links.byText(DeepLinkPage.ORDER_HISTORY_TITLE), LAND));
        Assert.assertTrue("Order History is pushed - Back must be offered", links.isBackShownNow());
    }

    // =========================================================================
    // liveId / stream deeplink
    // =========================================================================

    @Test
    public void TC290_Notify_LiveId_Known_Stream_Opens_Live_Player() {
        notifyAndTap("ISKCON Bangalore is live", "liveId", ISKCON);
        assertLivePlayer();
    }

    /** The second fallback entry - resolution is not just "take the first stream". */
    @Test
    public void TC291_Notify_LiveId_Shirdi_Stream_Opens_Live_Player() {
        notifyAndTap("Shirdi Saibaba Sansthan live", "liveId", SHIRDI);
        assertLivePlayer();
    }

    /** resolveStream finds nothing, the router falls back to .liveTab. */
    @Test
    public void TC292_Notify_LiveId_Unknown_Falls_Back_To_Live_Feed() {
        notifyAndTap("Live now", "liveId", UNKNOWN);
        Assert.assertTrue("Unknown stream must fall back to the Live feed",
                links.isLiveFeedShown(LAND));
        Assert.assertTrue("Home must be the selected tab", links.isTabSelected("tab.home"));
    }

    /** A whitespace liveId is dropped when the payload is read, so screen decides. */
    @Test
    public void TC293_Notify_Blank_LiveId_Falls_Through_To_Screen_Field() {
        notifyAndTap("Open Explore", "liveId", "   ", "screen", "explore");
        assertTab("tab.explore");
    }

    @Test
    public void TC294_Notify_Deeplink_StreamId_Opens_Live_Player() {
        notifyAndTap("ISKCON live stream", "deeplink", "aikyam://streams/" + ISKCON);
        assertLivePlayer();
    }

    // =========================================================================
    // Priority: postId > liveId > stream deeplink > screen
    // =========================================================================

    @Test
    public void TC295_Notify_LiveId_Takes_Priority_Over_Screen_Field() {
        notifyAndTap("Live now", "liveId", ISKCON, "screen", "explore");
        assertLivePlayer();
        Assert.assertFalse("screen=explore must lose", links.isTabSelected("tab.explore"));
    }

    @Test
    public void TC296_Notify_PostId_Takes_Priority_Over_LiveId() {
        notifyAndTap("Shared post", "postId", UNKNOWN, "liveId", ISKCON);
        Assert.assertTrue("postId must win - the post screen (error state) must open", links.isPostErrorShown(LAND));
        Assert.assertFalse("liveId must lose", links.isLivePlayerShown(Duration.ZERO));
    }

    /** liveId resolves, the deeplink's stream does not: the player opening means liveId won -
     * had the deeplink won, the unknown stream would have fallen back to the Live feed. */
    @Test
    public void TC297_Notify_LiveId_Takes_Priority_Over_Deeplink_Stream() {
        notifyAndTap("Two streams", "liveId", ISKCON, "deeplink", "aikyam://streams/" + UNKNOWN);
        assertLivePlayer();
    }

    @Test
    public void TC298_Notify_Stream_Deeplink_Takes_Priority_Over_Community_Screen() {
        notifyAndTap("Live stream", "deeplink", "aikyam://streams/" + ISKCON, "screen", "community");
        assertLivePlayer();
    }

    /** Before PR #698 a stream deeplink lost to any non-community screen value. */
    @Test
    public void TC313_Notify_Stream_Deeplink_Wins_Over_Non_Community_Screen_Field() {
        notifyAndTap("Live stream now", "deeplink", "aikyam://streams/" + ISKCON, "screen", "explore");
        assertLivePlayer();
        Assert.assertFalse("screen=explore must lose", links.isTabSelected("tab.explore"));
    }

    /** Before PR #698 postId only routed for feed-like screen values. */
    @Test
    public void TC311_Notify_PostId_Always_Wins_Regardless_Of_Screen_Field() {
        notifyAndTap("Someone liked your post", "postId", UNKNOWN, "screen", "settings");
        Assert.assertTrue("postId must win - the post screen (error state) must open", links.isPostErrorShown(LAND));
        Assert.assertFalse("screen=settings (Apps tab) must lose", links.isTabSelected("tab.apps"));
    }

    @Test
    public void TC312_Notify_PostId_Wins_Over_Screen_Events() {
        notifyAndTap("Post update", "postId", UNKNOWN, "screen", "events");
        Assert.assertTrue("postId must win - the post screen (error state) must open", links.isPostErrorShown(LAND));
        Assert.assertFalse("screen=events must lose", links.isTabSelected("tab.events"));
    }

    // =========================================================================
    // No route / errors
    // =========================================================================

    /** Only an alert, no routing key at all: the tap just foregrounds the app. */
    @Test
    public void TC299_Notify_No_Routing_Payload_Does_Not_Navigate() {
        notifyAndTap("Generic notification");
        Assert.assertTrue("No route - Home must stay, nothing pushed", links.staysOnHome(SETTLE));
    }

    /** A screen value NotificationHandler does not know resolves to nothing. */
    @Test
    public void TC318_Notify_Unknown_Screen_Value_Does_Not_Navigate() {
        notifyAndTap("Unknown screen", "screen", "unknown_screen_value");
        Assert.assertTrue("Unknown screen - Home must stay, nothing pushed", links.staysOnHome(SETTLE));
    }

    @Test
    public void TC300_Notify_PostId_Unknown_Shows_Load_Error_And_Back_Returns_Home() {
        notifyAndTap("Shared post", "postId", UNKNOWN);
        Assert.assertTrue("The post screen's error state must show", links.isPostErrorShown(LAND));
        Assert.assertTrue("Back must be offered", links.isBackShownNow());
        driver.findElement(links.byDesc("Back")).click();
        Assert.assertTrue("Back must land on the Home feed", links.isHomeFeedShown(Duration.ofSeconds(15)));
    }

    // =========================================================================
    // Real entities - ids from the dev catalogue (TestData), skipped until supplied
    // =========================================================================

    @Test
    public void TC301_Notify_PostId_Real_Opens_Post_Detail() {
        Assume.assumeTrue(TestData.missing("postId"), TestData.isSet(TestData.POST_ID));
        notifyAndTap("Someone shared a post", "postId", TestData.POST_ID);
        Assert.assertTrue("Post detail must open (comment composer)",
                links.waitFor(links.byDesc("postDetail.commentInput"), LAND));
    }

    @Test
    public void TC302_Notify_Screen_UserProfile_Opens_Profile() {
        Assume.assumeTrue(TestData.missing("userId"), TestData.isSet(TestData.USER_ID));
        notifyAndTap("Profile update", "screen", "user_profile", "userId", TestData.USER_ID);
        Assert.assertTrue("The user's profile must open", new ProfilePage(driver).isPostsTabVisible());
        Assert.assertTrue("The profile is pushed - Back must be offered", links.isBackShownNow());
    }

    @Test
    public void TC303_Notify_Deeplink_MusicTrack_Opens_Music_Player() {
        Assume.assumeTrue(TestData.missing("musicTrackId"), TestData.isSet(TestData.MUSIC_TRACK_ID));
        notifyAndTap("New track available", "deeplink", "aikyam://music/tracks/" + TestData.MUSIC_TRACK_ID);
        Assert.assertTrue("Track links are handled by the Music tab", links.waitForTabSelected("tab.music", LAND));
        Assert.assertTrue("The track's player must open", new MusicPage(driver).isFullPlayerVisible());
    }
}
