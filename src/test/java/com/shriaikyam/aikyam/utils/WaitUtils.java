package com.shriaikyam.aikyam.utils;

import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public final class WaitUtils {

    private WaitUtils() {
    }

    public static WebElement waitForVisibility(IOSDriver driver, By locator, Duration timeout) {
        return new WebDriverWait(driver, timeout)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static boolean isVisible(IOSDriver driver, By locator, Duration timeout) {
        try {
            return waitForVisibility(driver, locator, timeout).isDisplayed();
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
