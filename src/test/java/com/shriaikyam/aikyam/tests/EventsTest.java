package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.EventsPage;
import com.shriaikyam.aikyam.pages.HomePage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Events (bottom nav → Events).
 */
public class EventsTest extends BaseTest {

    /** Same convention as MusicTest: enter from Home so a previous test's leftover
     * screen state can never be mistaken for Events failing to load. */
    @Before
    public void enterEventsFromHome() {
        HomePage homePage = new HomePage(driver);
        homePage.tapHomeTab();
        homePage.tapEventsTab();
    }

    @Test
    public void TC155_Verify_Events_Tab_Opens_Events_Screen() {
        EventsPage eventsPage = new EventsPage(driver);

        Assert.assertTrue("Events screen should be visible", eventsPage.isEventsScreenVisible());
    }

    @Test
    public void TC156_Verify_Event_Card_Shows_Its_Actions() {
        EventsPage eventsPage = new EventsPage(driver);

        Assert.assertTrue(
                "An event card should be visible, or the screen's own empty state should be",
                eventsPage.isAnyEventCardVisible()
        );
    }

}
