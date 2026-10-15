package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * TempleProfileView - reached from Explore by tapping a temple row.
 *
 * Ported from the Android TempleDetailsPage, but the tab set is genuinely different and the
 * port does not pretend otherwise. Android exposes Posts / About / Sevas / Live; iOS exposes
 * About / Gallery / Posts / Sevas (TempleProfileView.TempleTab). There is no Live tab on iOS,
 * and Gallery has no Android counterpart.
 */
public class TempleDetailsPage extends BasePage {

    private final By backAction = id("common.back");
    private final By followAction = id("temple.follow");
    private final By unfollowConfirm = id("unfollow.confirm");
    private final By unfollowCancel = id("unfollow.cancel");
    private final By readToggle = id("temple.about.readToggle");
    private final By aboutSection = id("temple.about.section");
    private final By loadError = id("temple_profile_load_error");

    public TempleDetailsPage(IOSDriver driver) {
        super(driver);
    }

    /** iOS tab identifiers are temple.tab.<lowercased raw value>. */
    private By tab(String name) {
        return id("temple.tab." + name.toLowerCase());
    }

    public boolean isLoaded() {
        return isAnyVisible(tab("about"), tab("posts"), followAction);
    }

    public boolean isTabVisible(String name) {
        return isAnyVisible(tab(name));
    }

    public boolean openTab(String name) {
        return tapFirstVisible(tab(name));
    }

    /** The About tab's own body - used to prove a tab switch actually changed the content
     *  rather than only moving the selection highlight.
     *
     *  Keyed on temple.about.section, not on the read-more toggle: that toggle renders only
     *  for a temple whose history is long enough (shouldShowHistoryToggle), so TC106/TC142
     *  read a perfectly good About tab as missing whenever the first Explore result happened
     *  to be a temple with a short history. */
    public boolean isAboutContentVisible() {
        return isAnyVisible(aboutSection);
    }

    public boolean isFollowActionVisible() {
        return isAnyVisible(followAction);
    }

    /** Label carries the state ("Follow" / "Following"), so read it rather than guess. */
    public String followLabel() {
        WebElement element = firstVisible(followAction);
        return element == null ? null : element.getAttribute("label");
    }

    public boolean tapFollow() {
        return tapFirstVisible(followAction);
    }

    public boolean isUnfollowSheetVisible() {
        return isAnyVisible(unfollowConfirm, unfollowCancel);
    }

    public boolean confirmUnfollow() {
        return tapFirstVisible(unfollowConfirm);
    }

    public boolean cancelUnfollow() {
        return tapFirstVisible(unfollowCancel);
    }

    public boolean isReadToggleVisible() {
        return isAnyVisible(readToggle) || scrollUntilVisible(8, readToggle);
    }

    public String readToggleLabel() {
        WebElement element = firstVisible(readToggle);
        return element == null ? null : element.getAttribute("label");
    }

    public boolean tapReadToggle() {
        return isReadToggleVisible() && tapFirstVisible(readToggle);
    }

    public boolean isLoadErrorVisible() {
        return isAnyVisible(loadError);
    }

    public boolean tapBack() {
        return tapFirstVisible(backAction) || navigateBack();
    }

    private WebElement firstVisible(By locator) {
        for (WebElement element : driver.findElements(locator)) {
            try {
                if (element.isDisplayed()) {
                    return element;
                }
            } catch (RuntimeException ignored) {
                // Gone between lookup and read.
            }
        }
        return null;
    }

    private By byTextContains(String value) {
        return AppiumBy.iOSNsPredicateString("label CONTAINS " + predicateLiteral(value));
    }
}
