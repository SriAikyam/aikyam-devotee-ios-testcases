package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Home feed's Live filter: the YouTube live stream cards (FeedYoutubeStreamCard). */
public class LiveFeedPage extends BasePage {

    // The chip reads "Live, Live" (its circle and caption); older builds had the identifier. Not a bare
    // "Live": the temple home's Live tab is kept in the tree while hidden and would be tapped instead.
    private final By liveFilter = AppiumBy.iOSNsPredicateString(
            "name == 'feed.filter.live' OR (type == 'XCUIElementTypeButton' AND label == 'Live, Live')");
    private final By liveNow = byExactText("LIVE NOW");
    private final By noLiveEvents = byExactText("No live events");
    // #687: a card's thumbnail is one button, "<temple>, Live, <n> viewer(s)[, <activity>]".
    private final By cardThumbnail = AppiumBy.iOSNsPredicateString(
            "type == 'XCUIElementTypeButton' AND label CONTAINS ', Live, ' AND label CONTAINS ' viewer'");

    public LiveFeedPage(IOSDriver driver) {
        super(driver);
    }

    /** Taps the Live filter and waits for its list; false if the filter isn't there. */
    public boolean openLiveFeed() {
        WebElement filter = driver.findElements(liveFilter).stream().filter(WebElement::isDisplayed).findFirst().orElse(null);
        if (filter == null) {
            return false;
        }
        filter.click();
        return waitForAnyVisible(cardThumbnail, liveNow, noLiveEvents) != null;
    }

    public boolean hasNoLiveEvents() {
        return !driver.findElements(noLiveEvents).isEmpty() && driver.findElements(cardThumbnail).isEmpty();
    }

    /** The first live card's thumbnail as VoiceOver reads it, or null if none appears. */
    public WebElement firstCardThumbnail() {
        try {
            return new WebDriverWait(driver, SHORT_TIMEOUT)
                    .until(d -> d.findElements(cardThumbnail).stream().findFirst().orElse(null));
        } catch (org.openqa.selenium.TimeoutException none) {
            return null;
        }
    }

    /**
     * Elements VoiceOver can still land on with the old badge labels: the eye icon (read "Show") and
     * the bare "LIVE" text. Read from the page source because only it carries `accessible`; a
     * locator search also returns the badges hidden inside the thumbnail button.
     */
    public List<String> accessibleOldBadgeLabels() {
        Matcher element = Pattern.compile("<XCUIElementType(?:Image|StaticText) [^>]*>").matcher(driver.getPageSource());
        java.util.ArrayList<String> found = new java.util.ArrayList<>();
        while (element.find()) {
            String tag = element.group();
            boolean oldBadge = tag.contains("name=\"eye.fill\"") || tag.contains("label=\"LIVE\"");
            if (oldBadge && tag.contains("accessible=\"true\"")) {
                found.add(tag);
            }
        }
        return found;
    }
}
