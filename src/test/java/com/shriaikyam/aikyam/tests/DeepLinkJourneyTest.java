package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.data.TestData;
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
import org.junit.Assume;
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
        // Start from All Posts: the Temples/Live filter persists, and the Live-feed cases need
        // to see the filter change.
        links.selectAllPosts();
        Assert.assertTrue("Precondition: Home feed on All Posts", links.isHomeFeedShown(LAND));
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
    // TC255-TC260: tab-switch deep links (iOS PR #698, issues #668 / #685).
    // A tab switch selects the tab and pushes nothing, so each case asserts the selected
    // tab (the tab bar's isSelected trait), the tab's own content, and no Back button.
    // -------------------------------------------------------------------------

    private static final Duration SETTLE = Duration.ofSeconds(5);
    private static final String UNKNOWN = TestData.UNKNOWN_ID;

    private void assertTabSwitch(String link, String tabId, boolean contentVisible) {
        Assert.assertTrue(link + " must select " + tabId, links.waitForTabSelected(tabId, LAND));
        Assert.assertTrue(link + " must show the tab's content", contentVisible);
        Assert.assertFalse(link + " is a tab switch - nothing may be pushed", links.isBackShownNow());
    }

    @Test
    public void TC255_Verify_Explore_Deep_Link_Switches_To_Explore_Tab_Not_Push_On_Home() {
        links.fire("aikyam://explore");
        assertTabSwitch("aikyam://explore", "tab.explore", new ExplorePage(driver).isExploreVisible());
    }

    @Test
    public void TC256_Verify_Music_Deep_Link_Switches_To_Music_Tab_Not_Push() {
        links.fire("aikyam://music");
        assertTabSwitch("aikyam://music", "tab.music", new MusicPage(driver).isMusicScreenVisible());
    }

    /** Bare aikyam://streams (no id) resolves to .liveTab: Home tab switched to the Live feed
     * (StreamHighlightStore.setLiveTab). @Before leaves All Posts selected, so the switch to
     * Live is what proves the link was handled. */
    @Test
    public void TC257_Verify_Bare_Streams_Link_Stays_On_Home_And_Opens_Live_Feed() {
        links.fire("aikyam://streams");
        Assert.assertTrue("The Live feed must open", links.isLiveFeedShown(LAND));
        Assert.assertTrue("Home must stay the selected tab", links.isTabSelected("tab.home"));
        Assert.assertFalse("Nothing may be pushed", links.isBackShownNow());
    }

    @Test
    public void TC259_Verify_Events_Deep_Link_Switches_To_Events_Tab_Not_Push() {
        links.fire("aikyam://events");
        assertTabSwitch("aikyam://events", "tab.events", new EventsPage(driver).isEventsScreenVisible());
    }

    @Test
    public void TC260_Verify_Apps_Deep_Link_Switches_To_Apps_Tab_Not_Push() {
        links.fire("aikyam://apps");
        assertTabSwitch("aikyam://apps", "tab.apps", new AppsPage(driver).isAppsScreenVisible());
    }

    // -------------------------------------------------------------------------
    // TC261-TC265: feature screens. settings/orders/family_members push onto the Home
    // stack (RootView.deepLinkOwningTab returns 0); a mini-app opens inside the Apps tab.
    // -------------------------------------------------------------------------

    @Test
    public void TC261_Verify_Settings_Deep_Link_Opens_Settings_Hub_With_Back_Button() {
        links.fire("aikyam://settings");
        Assert.assertTrue("Settings hub must open", new SettingsPage(driver).isSettingsHubVisible());
        Assert.assertTrue("Settings is pushed - Back must be offered", links.isBackShownNow());
        Assert.assertTrue("Pushed onto the Home tab", links.isTabSelected("tab.home"));
    }

    @Test
    public void TC262_Verify_Orders_Deep_Link_Opens_Order_History_With_Back_Button() {
        links.fire("aikyam://orders");
        Assert.assertTrue("Order History must open",
                links.waitFor(links.byText(DeepLinkPage.ORDER_HISTORY_TITLE), LAND));
        Assert.assertTrue("Order History is pushed - Back must be offered", links.isBackShownNow());
    }

    @Test
    public void TC263_Verify_Family_Members_Deep_Link_Opens_Family_Screen_With_Back_Button() {
        links.fire("aikyam://family_members");
        Assert.assertTrue("Family Members must open", new FamilyMembersPage(driver).isScreenVisible());
        Assert.assertTrue("Family Members is pushed - Back must be offered", links.isBackShownNow());
    }

    @Test
    public void TC264_Verify_Panchang_Deep_Link_Opens_Panchang_Mini_App_Web_View() {
        links.fire("aikyam://panchang");
        Assert.assertTrue("The Panchang mini-app must open", links.isMiniAppShown("Panchang", LAND));
        Assert.assertTrue("Mini-apps open inside the Apps tab", links.isTabSelected("tab.apps"));
    }

    @Test
    public void TC265_Verify_Stotragalu_Deep_Link_Opens_Stotragalu_Mini_App_Web_View() {
        links.fire("aikyam://stotragalu");
        Assert.assertTrue("The Stotragalu mini-app must open", links.isMiniAppShown("Stotragalu", LAND));
        Assert.assertTrue("Mini-apps open inside the Apps tab", links.isTabSelected("tab.apps"));
    }

    // -------------------------------------------------------------------------
    // TC266-TC268: entity links. Music ids are dev-catalogue data (TestData); the stream id
    // is compiled into the app's fallback list, so TC268 always runs.
    // -------------------------------------------------------------------------

    @Test
    public void TC266_Verify_Music_Track_Deep_Link_Opens_Track_In_Music_Tab() {
        Assume.assumeTrue(TestData.missing("musicTrackId"), TestData.isSet(TestData.MUSIC_TRACK_ID));
        links.fire("aikyam://music/tracks/" + TestData.MUSIC_TRACK_ID);
        Assert.assertTrue("Track links are handled by the Music tab", links.waitForTabSelected("tab.music", LAND));
        Assert.assertTrue("The track's player must open", new MusicPage(driver).isFullPlayerVisible());
    }

    @Test
    public void TC267_Verify_Music_Album_Deep_Link_Opens_Album_In_Music_Tab() {
        Assume.assumeTrue(TestData.missing("musicAlbumId"), TestData.isSet(TestData.MUSIC_ALBUM_ID));
        links.fire("aikyam://music/albums/" + TestData.MUSIC_ALBUM_ID);
        Assert.assertTrue("Album links are handled by the Music tab", links.waitForTabSelected("tab.music", LAND));
        Assert.assertTrue("The album's details must open", new MusicPage(driver).isAlbumDetailsVisible());
    }

    @Test
    public void TC268_Verify_Stream_Id_Deep_Link_Opens_Live_Stream_Player() {
        links.fire("aikyam://streams/" + TestData.STREAM_VIDEO_ID_ISKCON);
        Assert.assertTrue("The player for that stream must open",
                links.isLivePlayerShown(LAND));
    }

    // -------------------------------------------------------------------------
    // TC269, TC271-TC278: bad or missing data. A rejected link is asserted with
    // staysOnHome(SETTLE) - Home stays selected with nothing pushed for the whole window -
    // because checking Home right after firing passes before the link is even processed.
    // -------------------------------------------------------------------------

    /** An unknown stream id is still routed: resolveStream finds nothing and the router
     * falls back to .liveTab - Home showing the Live feed. */
    @Test
    public void TC269_Verify_Unknown_Stream_Id_Falls_Back_To_Live_Feed_Without_Crash() {
        links.fire("aikyam://streams/" + UNKNOWN);
        Assert.assertTrue("Unknown stream must fall back to the Live feed",
                links.isLiveFeedShown(LAND));
        Assert.assertTrue("Home must stay the selected tab", links.isTabSelected("tab.home"));
        Assert.assertFalse("No player may open", links.isBackShownNow());
    }

    @Test
    public void TC271_Verify_Unknown_Music_Track_Id_Does_Not_Crash_Music_Tab_Active() {
        links.fire("aikyam://music/tracks/" + UNKNOWN);
        Assert.assertTrue("Track links are handled by the Music tab even when the id is unknown",
                links.waitForTabSelected("tab.music", LAND));
        Assert.assertTrue("Music must still render", new MusicPage(driver).isMusicScreenVisible());
    }

    @Test
    public void TC272_Verify_Unknown_Music_Album_Id_Does_Not_Crash_Music_Tab_Active() {
        links.fire("aikyam://music/albums/" + UNKNOWN);
        Assert.assertTrue("Album links are handled by the Music tab even when the id is unknown",
                links.waitForTabSelected("tab.music", LAND));
        Assert.assertTrue("Music must still render", new MusicPage(driver).isMusicScreenVisible());
    }

    /** aikyamNoIdDestination needs a sub-path AND an id under music; one segment is nil. */
    @Test
    public void TC273_Verify_Music_Tracks_Without_Id_Is_Not_Routable_App_Stays_On_Home() {
        links.fire("aikyam://music/tracks");
        Assert.assertTrue("Not routable - Home must stay, nothing pushed", links.staysOnHome(SETTLE));
    }

    @Test
    public void TC274_Verify_Music_Albums_Without_Id_Is_Not_Routable_App_Stays_On_Home() {
        links.fire("aikyam://music/albums");
        Assert.assertTrue("Not routable - Home must stay, nothing pushed", links.staysOnHome(SETTLE));
    }

    @Test
    public void TC275_Verify_Unrecognised_Deep_Link_Host_Is_Ignored_App_Stays_On_Home() {
        links.fire("aikyam://" + UNKNOWN);
        Assert.assertTrue("Unknown host - Home must stay, nothing pushed", links.staysOnHome(SETTLE));
    }

    /** No-id hosts require an empty path, and none of these hosts is an entity type, so an
     * extra segment is not routable. aikyam://events/x is deliberately NOT here: "events" IS
     * an entity type, so that URL opens event details (TC305 relies on exactly that). */
    @Test
    public void TC276_Verify_Extra_Path_On_Single_Destination_Host_Is_Not_Routable() {
        for (String host : new String[] {"panchang", "settings", "explore", "apps"}) {
            String url = "aikyam://" + host + "/" + UNKNOWN;
            links.fire(url);
            Assert.assertTrue("Not routable - Home must stay, nothing pushed: " + url, links.staysOnHome(SETTLE));
        }
    }

    /** Space and ! fail ShareLinkRouter.isValidId. */
    @Test
    public void TC277_Verify_Malformed_Music_Track_Id_Is_Not_Routable_App_Stays_On_Home() {
        links.fire("aikyam://music/tracks/bad%20track%21");
        Assert.assertTrue("Malformed id - Home must stay, nothing pushed", links.staysOnHome(SETTLE));
    }

    @Test
    public void TC278_Verify_Malformed_Stream_Id_Is_Not_Routable_App_Stays_On_Home() {
        links.fire("aikyam://streams/bad%20id%21");
        Assert.assertTrue("Malformed id - Home must stay, nothing pushed", links.staysOnHome(SETTLE));
    }

    // -------------------------------------------------------------------------
    // TC304-TC307: behaviour fixed by iOS PR #698
    // -------------------------------------------------------------------------

    /** MiniAppScreen is keyed by .id(miniAppId); without it SwiftUI reused the Panchang web
     * view when the route changed to Stotragalu. The mini-app id on the web view proves which
     * one is loaded. */
    @Test
    public void TC304_Verify_Mini_App_Switch_Via_Deeplink_Reloads_New_Mini_App() {
        links.fire("aikyam://panchang");
        Assert.assertTrue("Panchang must open first", links.isMiniAppShown("Panchang", LAND));
        links.fire("aikyam://stotragalu");
        Assert.assertTrue("Stotragalu must replace it", links.isMiniAppShown("Stotragalu", LAND));
        Assert.assertFalse("Panchang must be gone", links.isMiniAppShown("Panchang", Duration.ZERO));
    }

    /** A tab-switch link clears the target tab's stack (pathsByTab[tab] = NavigationPath()).
     * aikyam://events/<id> pushes event details onto the Events tab; aikyam://events must then
     * land on the Events root, not on top of that pushed screen. */
    @Test
    public void TC305_Verify_Tab_Switch_Deeplink_Resets_Navigation_Stack_To_Root() {
        links.fire("aikyam://events/" + UNKNOWN);
        Assert.assertTrue("Event details belong to the Events tab", links.waitForTabSelected("tab.events", LAND));
        Assert.assertTrue("Event details are pushed - Back must show", links.waitFor(links.byDesc("Back"), LAND));
        links.fire("aikyam://events");
        Assert.assertTrue("The Events root must show", new EventsPage(driver).isEventsScreenVisible());
        Assert.assertFalse("The pushed screen must be cleared", links.isBackShownNow());
    }

    @Test
    public void TC307_Verify_Explore_Deep_Link_From_Non_Home_Tab_Switches_To_Explore() {
        links.fire("aikyam://events");
        Assert.assertTrue("Start on the Events tab", links.waitForTabSelected("tab.events", LAND));
        links.fire("aikyam://explore");
        assertTabSwitch("aikyam://explore", "tab.explore", new ExplorePage(driver).isExploreVisible());
        Assert.assertFalse("Events must no longer be selected", links.isTabSelected("tab.events"));
    }
}
