package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.DeepLinkPage;
import com.shriaikyam.aikyam.pages.FeedPage;
import com.shriaikyam.aikyam.pages.HomePage;
import com.shriaikyam.aikyam.pages.ProfilePage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;

/**
 * Deep / shared link journeys. Real link: the feed's Share sheet previews the short link
 * the app mints (https://api-dev.shriaikyam.com/p/&lt;code&gt;); that exact link is fired back
 * at the dev package. Every link is sent with an explicit package.
 * Link formats come from AndroidManifest.xml intent-filters and core/share/ShareLinkRouter.kt.
 */
public class DeepLinkJourneyTest extends BaseTest {

    private static final Duration LAND = Duration.ofSeconds(30);
    private static final String POST_ERROR = "Unable to load this shared post.";
    private static final String COMPOSER = "Add a comment...";

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
        author = author.split(" \u00b7")[0].trim(); // "@handle . 3d ago" -> "@handle"
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
        Assert.assertTrue("Link must open the post detail (composer shown)", links.waitForText(COMPOSER, LAND));
        Assert.assertTrue("Opened post must be by " + author, links.isShown(links.byContainsText(author)));
    }

    @Test
    public void TC240_Verify_Shared_Post_Link_Opens_Post_While_App_Running_And_Back_Returns_Home() {
        String[] shared = shareFirstPost();
        Assert.assertTrue(links.fire(shared[0]));
        assertPostOpen(shared[1]);
        driver.navigate().back();
        Assert.assertTrue("Back must return to the Home feed", links.isHomeFeedShown(Duration.ofSeconds(15)));
    }

    @Test
    public void TC241_Verify_Shared_Post_Link_Opens_Post_From_Cold_Start_Still_Logged_In() {
        String[] shared = shareFirstPost();
        Assert.assertTrue(links.forceStopApp());
        Assert.assertTrue(links.fire(shared[0]));
        assertPostOpen(shared[1]);
        Assert.assertFalse("Must not be sent to login", links.isShown(links.byContainsText("Enter your phone")));
        driver.navigate().back();
        Assert.assertTrue("Back must reach the Home feed", links.isHomeFeedShown(Duration.ofSeconds(20)));
    }

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
        Assert.assertFalse(links.isShown(links.byText(COMPOSER)));
    }

    @Test
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
    public void TC248_Verify_Unknown_Event_Id_Link_Shows_Error_Without_Crash() {
        Assert.assertTrue(links.fire("https://m.shriaikyam.com/events/doesnotexist123"));
        Assert.assertTrue("Event error state must show", links.waitForText("getEventDetail failed: HTTP 404", LAND));
        Assert.assertTrue(links.isShown(links.byText("Retry")));
    }

    /** Records CURRENT behaviour: a link to a user id that does not exist opens an empty
     * "ghost" profile ("@doesnotexist123", 0 posts) instead of a not-found state. That is
     * arguably a product bug - if the app starts showing a not-found screen, update this test. */
    @Test
    public void TC249_Verify_Unknown_User_Id_Link_Opens_Profile_Screen_Without_Crash() {
        Assert.assertTrue(links.fire("https://m.shriaikyam.com/u/doesnotexist123"));
        Assert.assertTrue("User profile screen for the linked id must show",
                links.waitForText("@doesnotexist123", LAND) && links.isShown(links.byDesc("Back")));
    }
}
