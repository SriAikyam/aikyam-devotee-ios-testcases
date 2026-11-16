package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.HomePage;
import com.shriaikyam.aikyam.pages.LiveFeedPage;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;

/**
 * iOS #687: on the Home feed's Live filter, a YouTube live card's thumbnail is one VoiceOver button
 * naming the stream ("Siddhivinayak Temple, Live, 71 viewers"), and the eye icon is no longer read
 * on its own as "Show". Read-only: nothing is tapped on the cards. Skipped when nothing is live.
 */
public class LiveCardAccessibilityTest extends BaseTest {

    private LiveFeedPage live;

    @Before
    public void openLiveFeed() {
        new HomePage(driver).tapHomeTab();
        live = new LiveFeedPage(driver);
        Assert.assertTrue("The Live filter should open its list", live.openLiveFeed());
        Assume.assumeFalse("Skipping: no live streams right now", live.hasNoLiveEvents());
    }

    @Test
    public void TC393_Live_Card_Thumbnail_Is_A_Button_Naming_Temple_Live_And_Viewers() {
        WebElement thumbnail = live.firstCardThumbnail();
        Assert.assertNotNull("A live card's thumbnail should be one button reading '<temple>, Live, <n> viewers'",
                thumbnail);
        String label = thumbnail.getAttribute("label");
        Assert.assertTrue("Label should start with the temple name, then Live: " + label,
                label.matches("[^,].*, Live, .+"));
        Assert.assertTrue("Label should give the viewer count in words ('<n> viewers' or '1 viewer'): " + label,
                label.matches(".*, Live, (1 viewer|\\S+ viewers)(, .*)?"));
    }

    @Test
    public void TC394_Live_Card_Badges_Are_Not_Read_On_Their_Own() {
        Assert.assertNotNull("A live card should be listed", live.firstCardThumbnail());
        Assert.assertTrue("VoiceOver should not reach the eye icon ('Show') or a bare 'LIVE' on its own: "
                + live.accessibleOldBadgeLabels(), live.accessibleOldBadgeLabels().isEmpty());
    }
}
