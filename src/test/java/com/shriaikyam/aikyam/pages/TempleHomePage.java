package com.shriaikyam.aikyam.pages;

import io.appium.java_client.ios.IOSDriver;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;

/** The temple account's home (TempleAccountHomeView): avatar and banner edit buttons
 * (app issue #672). Reached from the priest bottom bar's Temple tab. */
public class TempleHomePage extends BasePage {

    private final By templeTab = idOrLabel("tab.temple", "Temple");
    private final By avatarEdit = id("templeHome.avatar.edit");
    private final By bannerEdit = id("templeHome.banner.edit");
    private final By imageError = id("templeHome.imageError");

    /** The upload (media upload + profile save) is a few network round trips. */
    private static final Duration UPLOAD_TIMEOUT = Duration.ofSeconds(60);
    /** A full-size photo on a real iPhone (often fetched from iCloud) can take well over 10s
     * to load before the upload starts; simulator library photos load at once. */
    private static final Duration UPLOAD_START_TIMEOUT = Duration.ofSeconds(30);

    public TempleHomePage(IOSDriver driver) {
        super(driver);
    }

    public boolean isOnTempleAccount() {
        return isAnyVisible(templeTab);
    }

    public boolean open() {
        return tapFirstVisible(templeTab) && isAnyVisible(avatarEdit, bannerEdit);
    }

    public boolean isAvatarEditVisible() {
        return isAnyVisible(avatarEdit);
    }

    public boolean isBannerEditVisible() {
        return isAnyVisible(bannerEdit);
    }

    public boolean changeAvatar() {
        return tapFirstVisible(avatarEdit) && pickPhoto(avatarEdit);
    }

    public boolean changeBanner() {
        return tapFirstVisible(bannerEdit) && pickPhoto(bannerEdit);
    }

    /** Picking a photo starts an upload, which removes the edit button until it ends.
     * changeAvatar()/changeBanner() return once that removal is seen, so the button showing
     * again here means the upload finished. */
    public boolean waitForUploadToFinish(boolean avatar) {
        By edit = avatar ? avatarEdit : bannerEdit;
        long deadline = System.currentTimeMillis() + UPLOAD_TIMEOUT.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isErrorShown()) {
                return false;
            }
            if (state(edit) == EditButton.SHOWN) {
                return true;
            }
            sleep(POLL_MILLIS);
        }
        return false;
    }

    public boolean isErrorShown() {
        return isDisplayedNow(driver, imageError);
    }

    /**
     * The system photo picker runs out of process, so its grid is not in the app's
     * accessibility tree and has to be tapped by position. The first launch shows a
     * "Private Access to Photos" banner above the grid; the first tap is on its close
     * button, which on a picker without the banner lands on a photo instead. If the picker
     * is still up after that, the second tap takes the first row's middle photo.
     *
     * Returns true once the upload has started (edit button removed). Polls rather than
     * sleeping: an upload can finish in a couple of seconds, and a fixed pause let it start
     * and end unseen.
     */
    private boolean pickPhoto(By editButton) {
        Dimension size = driver.manage().window().getSize();
        if (!waitForState(editButton, EditButton.COVERED, PICKER_OPEN_MILLIS)) {
            return false;
        }
        sleep(PICKER_SETTLE_MILLIS);
        tapPoint((int) (size.getWidth() * 0.905), (int) (size.getHeight() * 0.183));
        if (waitForUploadStart(editButton, PICKER_SETTLE_MILLIS * 2)) {
            return true;
        }
        if (state(editButton) == EditButton.COVERED) {
            tapPoint(size.getWidth() / 2, (int) (size.getHeight() * 0.235));
        }
        return waitForUploadStart(editButton, UPLOAD_START_TIMEOUT.toMillis());
    }

    private enum EditButton { SHOWN, COVERED, GONE }

    /** Shown on screen, in the tree but covered (picker up), or removed (uploading). */
    private EditButton state(By editButton) {
        List<WebElement> buttons = driver.findElements(editButton);
        if (buttons.isEmpty()) {
            return EditButton.GONE;
        }
        try {
            return buttons.stream().anyMatch(WebElement::isDisplayed) ? EditButton.SHOWN : EditButton.COVERED;
        } catch (RuntimeException staleDuringRender) {
            return EditButton.GONE;
        }
    }

    private boolean waitForState(By editButton, EditButton wanted, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (state(editButton) == wanted) {
                return true;
            }
            sleep(POLL_MILLIS);
        }
        return false;
    }

    private boolean waitForUploadStart(By editButton, long timeoutMillis) {
        return waitForState(editButton, EditButton.GONE, timeoutMillis);
    }

    private static final long POLL_MILLIS = 150;
    private static final long PICKER_OPEN_MILLIS = 8_000;
    private static final long PICKER_SETTLE_MILLIS = 1_000;

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
