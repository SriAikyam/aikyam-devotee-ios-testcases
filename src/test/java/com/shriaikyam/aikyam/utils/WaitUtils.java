package com.shriaikyam.aikyam.utils;

import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public final class WaitUtils {

    private WaitUtils() {
    }

    /** First DISPLAYED match. The main tab view keeps every tab alive, so an id such as
     * header.account exists once per tab and only one copy is visible; Selenium's
     * visibilityOfElementLocated only inspects the first match, which is often a hidden one. */
    public static WebElement waitForVisibility(IOSDriver driver, By locator, Duration timeout) {
        return new WebDriverWait(driver, timeout)
                .ignoring(StaleElementReferenceException.class)
                .until(ignored -> driver.findElements(locator).stream()
                        .filter(WebElement::isDisplayed)
                        .findFirst()
                        .orElse(null));
    }

    public static boolean isVisible(IOSDriver driver, By locator, Duration timeout) {
        try {
            return waitForVisibility(driver, locator, timeout).isDisplayed();
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
