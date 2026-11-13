package com.shriaikyam.aikyam.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/** The update prompt (AppUpdatePromptView, aikyam-devotee-ios#716). Forced: "Update Required"
 * with Update Now only, can't be swiped away. Optional: "Update Available" with Update Now,
 * Remind Me Later and Skip This Version.
 *
 * Identifiers were added in #716; each locator falls back to the English text. */
public class AppUpdatePage extends BasePage {

    private static final By FORCED_TITLE = idOrText("appUpdate.title.forced", "Update Required");
    private static final By OPTIONAL_TITLE = idOrText("appUpdate.title.optional", "Update Available");
    private static final By UPDATE_NOW = idOrLabel("appUpdate.updateNow", "Update Now");
    private static final By REMIND_LATER = idOrLabel("appUpdate.remindLater", "Remind Me Later");
    private static final By SKIP = idOrLabel("appUpdate.skip", "Skip This Version");

    /** The prompt waits for the launch screen, then a network round-trip. */
    private static final Duration PROMPT_TIMEOUT = Duration.ofSeconds(45);

    public AppUpdatePage(IOSDriver driver) {
        super(driver);
    }

    public boolean waitForForcedPrompt() {
        return waitFor(PROMPT_TIMEOUT, FORCED_TITLE);
    }

    public boolean waitForOptionalPrompt() {
        return waitFor(PROMPT_TIMEOUT, OPTIONAL_TITLE);
    }

    /** True if either prompt shows up within {@code timeout}. Used to assert it does NOT. */
    public boolean anyPromptAppearsWithin(Duration timeout) {
        return waitFor(timeout, FORCED_TITLE, OPTIONAL_TITLE);
    }

    public boolean waitUntilPromptGone() {
        return waitUntilGone(FORCED_TITLE, OPTIONAL_TITLE);
    }

    public boolean isForcedPromptVisible() {
        return isAnyVisible(FORCED_TITLE);
    }

    public boolean isUpdateNowVisible() {
        return isAnyVisible(UPDATE_NOW);
    }

    public boolean isRemindLaterVisible() {
        return isAnyVisible(REMIND_LATER);
    }

    public boolean isSkipVisible() {
        return isAnyVisible(SKIP);
    }

    /** No wait: for asserting that the forced prompt has no way out. */
    public boolean isRemindLaterOrSkipVisibleNow() {
        return isVisibleNow(REMIND_LATER, SKIP);
    }

    public boolean tapUpdateNow() {
        return tapFirstVisible(UPDATE_NOW);
    }

    /** Drags from the prompt's title to near the bottom of the screen, the gesture that
     * dismisses a sheet when it is allowed to. */
    public boolean swipePromptDown() {
        List<WebElement> titles = driver.findElements(FORCED_TITLE);
        if (titles.isEmpty()) {
            titles = driver.findElements(OPTIONAL_TITLE);
        }
        if (titles.isEmpty()) {
            return false;
        }
        Point start = titles.get(0).getLocation();
        Dimension screen = driver.manage().window().getSize();
        int x = screen.getWidth() / 2;
        int endY = (int) (screen.getHeight() * 0.95);

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence drag = new Sequence(finger, 1);
        drag.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, start.getY()));
        drag.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        drag.addAction(finger.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), x, endY));
        drag.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(drag));
        return true;
    }

    private boolean waitFor(Duration timeout, By... locators) {
        try {
            new WebDriverWait(driver, timeout)
                    .ignoring(StaleElementReferenceException.class)
                    .until(ignored -> {
                        for (By locator : locators) {
                            for (WebElement element : driver.findElements(locator)) {
                                if (element.isDisplayed()) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    });
            return true;
        } catch (RuntimeException notShown) {
            return false;
        }
    }

    /** Identifier, or the exact English text. Not {@link #idOrLabel}, which only matches
     * buttons: the titles are plain Text. */
    private static By idOrText(String identifier, String text) {
        String t = predicateLiteral(text);
        return AppiumBy.iOSNsPredicateString(
                "name == " + predicateLiteral(identifier) + " OR label == " + t);
    }
}
