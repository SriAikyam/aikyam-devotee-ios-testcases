package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.BookSevaPage;
import com.shriaikyam.aikyam.pages.DeepLinkPage;
import com.shriaikyam.aikyam.pages.SevaEdgePage;
import com.shriaikyam.aikyam.pages.SevaOrdersPage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.openqa.selenium.ScreenOrientation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Sevas QA report group 8: display, language, accessibility (8 cases). Each case walks the seva
 * flow (Sevas tab, booking form, Review & Pay, Order History) and checks nothing is clipped off
 * screen and the key buttons stay reachable.
 *
 * Language and text size are set with launch arguments (-AppleLanguages, the app's
 * Bundle+AppLanguage honours it; -UIPreferredContentSizeCategoryName), so they run unattended;
 * @After relaunches with no arguments.
 */
public class SevaEdgeDisplayTest extends BaseTest {

    @Override
    protected boolean reuseDriverAcrossTests() {
        return true;
    }

    @AfterClass
    public static void endSharedSession() {
        releaseSharedDriver();
    }

    private boolean relaunched;

    @After
    public void restore() {
        if (driver == null) {
            return;
        }
        try {
            driver.rotate(ScreenOrientation.PORTRAIT);
        } catch (RuntimeException ignored) {
            // Already portrait or rotation unsupported.
        }
        new BookSevaPage(driver).leavePaymentFlowIfOpen();
        if (relaunched) {
            new SevaEdgePage(driver).relaunchWithArguments();
            relaunched = false;
        }
    }

    /** Runs the seva flow and returns every problem seen (clipped elements, unreachable buttons). */
    private List<String> walkFlow(String templeLink) {
        List<String> problems = new ArrayList<>();
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue(new DeepLinkPage(driver).fire(templeLink));
        BookSevaPage seva = new BookSevaPage(driver);
        Assert.assertTrue("Sevas tab should open", seva.openSevasTab());
        Assert.assertTrue(seva.isSevaCardVisible());
        clipped("Sevas tab", edge, problems);
        Assert.assertTrue(seva.tapFirstSevaBook());
        Assert.assertTrue(seva.isBookingDetailsScreenVisible());
        clipped("Booking form", edge, problems);
        Assert.assertTrue(seva.confirmDefaultDate());
        Assert.assertTrue(seva.toggleDevotee(SevaTestConfig.requireSelfName()));
        if (!seva.tapPlaceSevaOrder()) {
            problems.add("Place Seva Order not reachable");
            return problems;
        }
        Assert.assertTrue(seva.waitForPaymentReviewScreen());
        clipped("Review & Pay", edge, problems);
        if (!seva.isProceedToPaymentEnabled()) {
            problems.add("Proceed to Payment not reachable");
        }
        Assert.assertTrue(seva.cancelPaymentReview());
        SevaOrdersPage orders = new SevaOrdersPage(driver);
        Assert.assertTrue(orders.openOrderHistory());
        Assert.assertTrue(orders.isEmptyOrListLoaded());
        clipped("Order History", edge, problems);
        return problems;
    }

    private static void clipped(String screen, SevaEdgePage edge, List<String> problems) {
        for (String e : edge.elementsOffScreenHorizontally()) {
            problems.add(screen + ": clipped " + e);
        }
    }

    /** SEV-J01: known failure in the report; asserts the correct behaviour. */
    @Test
    public void TC1005_Verify_Kannada_Translates_Seva_Screens_Without_Truncation() {
        String link = SevaTestConfig.requireTempleLink();
        SevaEdgePage edge = new SevaEdgePage(driver);
        edge.relaunchWithArguments("-AppleLanguages", "(kn)", "-AppleLocale", "kn_IN");
        relaunched = true;
        Assert.assertTrue(new DeepLinkPage(driver).fire(link));
        BookSevaPage seva = new BookSevaPage(driver);
        seva.openSevasTab();
        Assert.assertTrue("Kannada text should show on the Sevas tab", edge.isAnyKannadaTextShown());
        List<String> english = edge.untranslatedEnglish("Sevas", "Book", "Details", "Place Seva Order",
                "Proceed to Payment", "Order History");
        Assert.assertTrue("Untranslated seva strings: " + english, english.isEmpty());
        Assert.assertTrue("Clipped in Kannada: " + edge.elementsOffScreenHorizontally(),
                edge.elementsOffScreenHorizontally().isEmpty());
    }

    /** SEV-K612: known failure in the report. iOS restarts the app on a language change, so the
     * form is filled, the language switched in the app's own setting by a person, and the form checked. */
    @Test
    public void TC1006_Verify_Language_Switch_Mid_Booking_Updates_Strings_And_Keeps_Form() {
        SevaEdgeConfig.requireManualAssist();
        Assert.assertTrue(new DeepLinkPage(driver).fire(SevaTestConfig.requireTempleLink()));
        BookSevaPage seva = new BookSevaPage(driver);
        Assert.assertTrue(seva.openSevasTab());
        Assert.assertTrue(seva.tapFirstSevaBook());
        Assert.assertTrue(seva.confirmDefaultDate());
        Assert.assertTrue(seva.toggleDevotee(SevaTestConfig.requireSelfName()));
        int devotees = seva.readTotalDevoteesCount();
        SevaTestConfig.askPerson("Profile > Settings > App Language > Kannada, then go back to the booking form", 60);
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assert.assertTrue("Strings should switch to Kannada", edge.isAnyKannadaTextShown());
        Assert.assertTrue("Clipped: " + edge.elementsOffScreenHorizontally(), edge.elementsOffScreenHorizontally().isEmpty());
        Assert.assertEquals("Form data should be kept", devotees, seva.readTotalDevoteesCount());
        SevaTestConfig.askPerson("switch App Language back to English", 45);
    }

    /** SEV-J04 */
    @Test
    public void TC1007_Verify_Largest_Dynamic_Type_Does_Not_Clip_Seva_Screens() {
        String link = SevaTestConfig.requireTempleLink();
        new SevaEdgePage(driver).relaunchWithArguments(
                "-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL");
        relaunched = true;
        List<String> problems = walkFlow(link);
        Assert.assertTrue("Largest text size: " + problems, problems.isEmpty());
    }

    /** SEV-J05: run against an iPhone SE-size simulator (-Dappium.deviceName="iPhone SE (3rd generation)"). */
    @Test
    public void TC1008_Verify_Small_Screen_Keeps_Layout_And_Buttons_Reachable() {
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assume.assumeTrue("Skipping: needs a 375-pt-wide or smaller device (iPhone SE/mini); this one is "
                + edge.screenWidthPoints(), edge.screenWidthPoints() <= 375);
        List<String> problems = walkFlow(SevaTestConfig.requireTempleLink());
        Assert.assertTrue("Small screen: " + problems, problems.isEmpty());
    }

    /** SEV-K007 */
    @Test
    public void TC1009_Verify_Background_And_Return_Keeps_Tab_And_Scroll_Position() {
        Assert.assertTrue(new DeepLinkPage(driver).fire(SevaTestConfig.requireTempleLink()));
        BookSevaPage seva = new BookSevaPage(driver);
        Assert.assertTrue(seva.openSevasTab());
        SevaEdgePage edge = new SevaEdgePage(driver);
        edge.scrollDown();
        List<String> before = edge.visibleTexts();
        driver.runAppInBackground(Duration.ofSeconds(10));
        Assert.assertTrue("Should come back on the Sevas tab", seva.isSevaCardVisible());
        Assert.assertEquals("Scroll position should be kept", before, edge.visibleTexts());
    }

    /** SEV-K606: automated proxy for VoiceOver; a manual VoiceOver walk still confirms how labels read. */
    @Test
    public void TC1010_Verify_Every_Seva_Control_Has_An_Accessibility_Label() {
        Assert.assertTrue(new DeepLinkPage(driver).fire(SevaTestConfig.requireTempleLink()));
        BookSevaPage seva = new BookSevaPage(driver);
        Assert.assertTrue(seva.openSevasTab());
        SevaEdgePage edge = new SevaEdgePage(driver);
        List<String> missing = new ArrayList<>(edge.unlabelledControls());
        Assert.assertTrue(seva.tapFirstSevaBook());
        Assert.assertTrue(seva.isDevoteeDetailsVisible());
        missing.addAll(edge.unlabelledControls());
        Assert.assertTrue(seva.confirmDefaultDate());
        Assert.assertTrue(seva.toggleDevotee(SevaTestConfig.requireSelfName()));
        Assert.assertTrue(seva.tapPlaceSevaOrder());
        Assert.assertTrue(seva.waitForPaymentReviewScreen());
        missing.addAll(edge.unlabelledControls());
        Assert.assertTrue(seva.cancelPaymentReview());
        Assert.assertTrue("Controls VoiceOver would read as nothing: " + missing, missing.isEmpty());
    }

    /** SEV-K607 */
    @Test
    public void TC1011_Verify_Landscape_Keeps_Seva_Layout_Intact() {
        try {
            driver.rotate(ScreenOrientation.LANDSCAPE);
        } catch (RuntimeException e) {
            Assume.assumeNoException("Skipping: the app or device does not rotate to landscape", e);
        }
        SevaEdgePage edge = new SevaEdgePage(driver);
        Assume.assumeTrue("Skipping: app stays portrait-only", edge.isScreenWiderThanTall());
        List<String> problems = walkFlow(SevaTestConfig.requireTempleLink());
        Assert.assertTrue("Landscape: " + problems, problems.isEmpty());
    }

    /** SEV-K613 */
    @Test
    public void TC1012_Verify_Seva_Flow_Behaves_The_Same_For_Three_Temples() {
        SevaTestConfig.requireSelfName();
        List<String> problems = new ArrayList<>();
        for (String link : SevaEdgeConfig.matrixTempleLinks()) {
            try {
                for (String p : walkFlow(link)) {
                    problems.add(link + " " + p);
                }
            } catch (AssertionError e) {
                problems.add(link + " " + e.getMessage());
                new BookSevaPage(driver).leavePaymentFlowIfOpen();
            }
        }
        Assert.assertTrue("Temples that differ: " + problems, problems.isEmpty());
    }
}
