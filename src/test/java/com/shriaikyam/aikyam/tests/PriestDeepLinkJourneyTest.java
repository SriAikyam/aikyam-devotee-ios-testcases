package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.data.TestData;
import com.shriaikyam.aikyam.pages.DeepLinkPage;
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
 * Deep links in a priest's temple context. Temple accounts reuse the devotee tab ids for
 * different content (id 1 = Schedules, id 3 = Orders), so RootView makes tab-switch links
 * not routable there (iOS PR #698) - while stream links, which push a route, still work.
 *
 * Each case switches the session to the seeded temple account and back on its own, so it
 * does not depend on PriestTest or on run order.
 */
public class PriestDeepLinkJourneyTest extends BaseTest {

    private static final Duration LAND = Duration.ofSeconds(30);
    private static final Duration SETTLE = Duration.ofSeconds(5);

    private DeepLinkPage links;

    @Before
    public void switchToTempleAccount() {
        Assume.assumeTrue(DeviceControl.SIMULATOR_ONLY_SEEDED_ACCOUNT, DeviceControl.isSimulator());
        links = new DeepLinkPage(driver);
        Assert.assertTrue("Precondition: Settings hub opens", new ProfilePage(driver).openSettingsHub());
        Assert.assertTrue("Precondition: temple account selectable", new SettingsPage(driver).switchToTempleAccount());
        Assert.assertTrue("Precondition: Temple tab selected", links.waitForTabSelected("tab.temple", LAND));
    }

    @After
    public void switchBackToPersonalAccount() {
        try {
            driver.activateApp(DeepLinkPage.DEV_BUNDLE_ID);
            if (new ProfilePage(driver).openSettingsHub()) {
                new SettingsPage(driver).switchToPersonalAccount();
            }
        } catch (RuntimeException ignored) {
            // BaseTest.resetToHome restores the personal account before the next test anyway.
        }
    }

    /** Tab id 3 is Orders for a temple account: aikyam://explore must not open it. */
    @Test
    public void TC258_Verify_Explore_Deep_Link_Not_Routable_For_Priest_Temple_Account() {
        links.fire("aikyam://explore");
        Assert.assertTrue("Not routable for a temple account - Temple tab must stay, nothing pushed",
                links.staysOnTab("tab.temple", SETTLE));
        Assert.assertFalse("Orders (tab id 3) must not open", links.isTabSelected("tab.orders"));
    }

    @Test
    public void TC315_Verify_All_Tab_Switch_Deeplinks_Blocked_For_Priest_Temple_Account() {
        for (String link : new String[] {"aikyam://music", "aikyam://events", "aikyam://apps"}) {
            links.fire(link);
            Assert.assertTrue(link + " is not routable for a temple account - Temple tab must stay",
                    links.staysOnTab("tab.temple", SETTLE));
        }
    }

    /** liveId resolves through routeDeepLink to a pushed route, not a tab switch, so the
     * temple-account guard does not apply and the player opens. */
    @Test
    public void TC314_Notify_LiveId_Opens_Stream_Player_For_Priest_Temple_Account() {
        Assume.assumeTrue(DeviceControl.SIMULATOR_ONLY_PUSH, DeviceControl.isSimulator());
        links.allowNotifications();
        Assert.assertTrue("The banner never appeared - is notification permission granted?",
                links.notifyAndTap("ISKCON live", payload("liveId", TestData.STREAM_VIDEO_ID_ISKCON)));
        Assert.assertTrue("The player for that stream must open",
                links.isLivePlayerShown(LAND));
    }
}
