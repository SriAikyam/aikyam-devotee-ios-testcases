package com.shriaikyam.aikyam.base;

import com.shriaikyam.aikyam.pages.FeedPage;
import com.shriaikyam.aikyam.pages.LoginPage;
import com.shriaikyam.aikyam.pages.OnboardingPage;
import com.shriaikyam.aikyam.pages.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.appmanagement.ApplicationState;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseTest {

    protected IOSDriver driver;

    /** Phase 2 experiment. Was 5s. An implicit wait sits underneath every explicit
     * wait, so a multi-locator WebDriverWait pays it once PER LOCATOR PER POLL -
     * the classic Selenium implicit+explicit anti-pattern. Single constant so the
     * A/B is one toggle and trivially reversible. */
    private static final Duration IMPLICIT_WAIT = Duration.ZERO;

    /* ---- Driver lifecycle instrumentation (counting only - no behaviour change) ----
     *
     * The first Phase 3 pilot produced no lifecycle numbers at all: its counters were
     * printed from an @AfterClass whose output never reached the captured log, so the run
     * could not be evaluated even aside from the adb outage. These counters are static,
     * incremented at the single points where each event happens, and flushed from a JVM
     * shutdown hook - which fires deterministically when the forked Gradle test JVM exits,
     * after every test class has finished, without any test class needing to call it.
     *
     * DRIVER_REUSES and DRIVER_REBUILDS are wired up but stay at 0 today: no reuse
     * mechanism exists in this build. They are here so a future pilot reports real
     * numbers instead of silence. */
    protected static final java.util.concurrent.atomic.AtomicInteger DRIVER_CREATIONS =
            new java.util.concurrent.atomic.AtomicInteger();
    protected static final java.util.concurrent.atomic.AtomicInteger DRIVER_REUSES =
            new java.util.concurrent.atomic.AtomicInteger();
    protected static final java.util.concurrent.atomic.AtomicInteger DRIVER_REBUILDS =
            new java.util.concurrent.atomic.AtomicInteger();
    protected static final java.util.concurrent.atomic.AtomicInteger DRIVER_QUITS =
            new java.util.concurrent.atomic.AtomicInteger();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                System.out.println("[driver-lifecycle]"
                        + (System.getProperty("appium.worker") == null ? "" : " worker=" + System.getProperty("appium.worker"))
                        + " creations=" + DRIVER_CREATIONS.get()
                        + " reuses=" + DRIVER_REUSES.get()
                        + " rebuilds=" + DRIVER_REBUILDS.get()
                        + " quits=" + DRIVER_QUITS.get())));
    }

    private static final String DEFAULT_APPIUM_SERVER_URL = "http://127.0.0.1:4723";
    private static final String DEFAULT_DEVICE_NAME = "iPhone 17 Pro";
    /** Dev-Debug configuration of the AikyamDevotee-Dev scheme. Never point this suite at
     * the prod bundle (com.shriaikyam.ikyam): it writes to the account it runs as. */
    private static final String DEFAULT_BUNDLE_ID = "com.shriaikyam.ikyam.dev";
    /** Phase 4: under a parallel shard (-Dappium.worker=N) diagnostics go to a worker-scoped
     * directory. Two shards share the same class names, so the fixed path would have them
     * overwriting each other's screenshots and page sources - the evidence we rely on to
     * triage a failure. Absent the property this resolves to the original path, so
     * sequential runs and regressionReport are unaffected. */
    private static final Path DIAGNOSTICS_DIR = diagnosticsDir();

    private static Path diagnosticsDir() {
        String worker = System.getProperty("appium.worker");
        if (worker == null || worker.isBlank()) {
            return Paths.get("build", "test-results", "diagnostics");
        }
        return Paths.get("build", "parallel-results", "worker-" + worker, "diagnostics");
    }

    /** JUnit's @Rule wraps @Before+@Test+@After as a single Statement, so a TestWatcher's
     * failed()/finished() callbacks only fire once that whole block has already run -
     * quitting the driver from an @After tearDown() would leave nothing to screenshot by
     * the time failed() sees the exception. Session cleanup lives in finished() instead,
     * so failed() still has a live driver to capture diagnostics from. */
    @Rule
    public TestWatcher diagnosticsWatcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            captureFailureDiagnostics(description, e);
        }

        @Override
        protected void finished(Description description) {
            // An opted-in class keeps its session between tests; @AfterClass ends it.
            if (reuseDriverAcrossTests()) {
                return;
            }
            if (driver != null) {
                try {
                    driver.quit();
                    DRIVER_QUITS.incrementAndGet();
                } catch (RuntimeException ignored) {
                    // Session may already be dead (e.g. an instrumentation crash) - nothing more to clean up.
                }
                driver = null;
            }
        }
    };

    @Before
    public void setUp() throws MalformedURLException {
        XCUITestOptions options = new XCUITestOptions()
                .setDeviceName(getProperty("appium.deviceName", DEFAULT_DEVICE_NAME))
                .setNoReset(true)
                // Counterpart of autoGrantPermissions: accepts the system photo/camera/
                // notification permission alerts as they appear.
                .setAutoAcceptAlerts(true)
                .setNewCommandTimeout(Duration.ofSeconds(120));
        String platformVersion = getProperty("appium.platformVersion", null);
        if (platformVersion != null) {
            options.setPlatformVersion(platformVersion);
        }

        // A BrowserStack run supplies an uploaded app id (bs://...) instead of an
        // already-installed package/activity - the two are mutually exclusive, so this
        // branches instead of always setting bundleId. Local/simulator
        // behavior below is unchanged when appium.bstackApp is absent.
        String bstackApp = getProperty("appium.bstackApp", null);
        if (bstackApp != null && !bstackApp.isBlank()) {
            applyBrowserStackOptions(options, bstackApp);
        } else {
            options.setBundleId(bundleId());

            // With more than one simulator booted, an unpinned session is ambiguous.
            String udid = System.getProperty("appium.udid");
            if (udid != null && !udid.isBlank()) {
                options.setUdid(udid);
            }
        }

        // Default path unchanged: driver is null here for every class that has not opted
        // in, so a fresh session is built per test exactly as before.
        if (reuseDriverAcrossTests() && sharedDriver != null) {
            if (isSessionAlive(sharedDriver)) {
                driver = sharedDriver;
                DRIVER_REUSES.incrementAndGet();
            } else {
                System.out.println("[driver-lifecycle] shared session dead - rebuilding once");
                quitQuietly(sharedDriver);
                sharedDriver = null;
                DRIVER_REBUILDS.incrementAndGet();
            }
        }

        if (driver == null) {
            driver = new IOSDriver(
                    new URL(getProperty("appium.serverUrl", DEFAULT_APPIUM_SERVER_URL)),
                    options
            );
            DRIVER_CREATIONS.incrementAndGet();
            if (reuseDriverAcrossTests()) {
                sharedDriver = driver;
            }
        }

        driver.manage()
                .timeouts()
                .implicitlyWait(IMPLICIT_WAIT);

        // setNoReset(true) preserves app/session state across tests, but doesn't
        // guarantee the app is actually in the foreground at session start - e.g. if a
        // prior run's recovery logic backed all the way out to the home screen, or the
        // app was manually closed. Explicitly bring it to the foreground rather than
        // relying on it already being open.
        driver.activateApp(bundleId());

        // A BrowserStack real-device session starts with no persisted app state - unlike
        // a local emulator that's already past onboarding and logged in. Opt-in only
        // (default false) so this never runs against the shared local/emulator session;
        // existing test classes are untouched either way.
        if (Boolean.parseBoolean(getProperty("appium.bstackLogin", "false"))) {
            establishAuthenticatedSession();
        }

        if (shouldResetToHome()) {
            resetToHome();
        }
    }

    /* ---------------- Phase 3 pilot: opt-in per-class driver reuse ---------------- */

    /** Opt-in, default OFF, so all 18 other classes keep the existing per-test lifecycle.
     * Deliberately not a global switch. The holder below is static only so it survives
     * between JUnit's per-test instances of ONE class; releaseSharedDriver() clears it at
     * the end of that class. The inherited `driver` field stays non-static. */
    protected boolean reuseDriverAcrossTests() {
        return false;
    }

    private static IOSDriver sharedDriver;

    /** Cheap liveness probe so a dead session is rebuilt explicitly rather than carried
     * into the next test, where it would surface as a product-looking failure. */
    private static boolean isSessionAlive(IOSDriver candidate) {
        try {
            candidate.getSessionId();
            candidate.getWindowHandle();
            return true;
        } catch (RuntimeException dead) {
            return false;
        }
    }

    private static void quitQuietly(IOSDriver candidate) {
        try {
            candidate.quit();
            DRIVER_QUITS.incrementAndGet();
        } catch (RuntimeException ignored) {
            // Already gone.
        }
    }

    /** Ends the shared session. An opted-in class calls this from @AfterClass, which is
     * what keeps the session scoped to that class instead of leaking into the next. */
    protected static void releaseSharedDriver() {
        if (sharedDriver != null) {
            quitQuietly(sharedDriver);
            sharedDriver = null;
        }
    }

    /** Overridable escape hatch: a class whose tests intentionally continue from the
     * previous test's screen (rather than merely from its data) can opt out without this
     * class needing to know about it. Every current test class self-navigates -
     * FamilyMembersTest calls openFamilyMembersScreen(), PostTest calls
     * openCreatePostPage() - so the default is safe for all of them, including the eight
     * using @FixMethodOrder, whose dependencies are on data, not on screen position. */
    protected boolean shouldResetToHome() {
        return true;
    }

    private static final int MAX_BACK_ATTEMPTS = 5;

    /** The Home tab of AikyamCustomTabBar (MainTabView), by its stable identifier. */
    private static final By HOME_BOTTOM_NAV = AppiumBy.accessibilityId("tab.home");

    /** Button labels of the iOS system permission alerts. autoAcceptAlerts normally
     * handles these; this is the fallback for one raised mid-reset. "Don't Allow" is
     * deliberately absent - denying would permanently change app behaviour for every
     * later test. */
    private static final String[] PERMISSION_ALLOW_LABELS = {
            "Allow While Using App", "Allow Full Access", "Allow Once", "Allow"
    };

    /** Isolation guard. With setNoReset(true) the app resumes on whatever screen the
     * previous test abandoned, so a later test can start somewhere it never expected -
     * confirmed live: all nine SearchTest failures began on the Profile screen, and
     * MusicTest's began on Events. activateApp() above only foregrounds the app; it does
     * not navigate, so it cannot fix this by itself.
     *
     * Never touches authentication: no resetApp(), no clearApp(), no logout, no re-login.
     * Bounded and idempotent - re-tapping an already-active Home tab is a harmless
     * no-op/scroll-to-top, the same convention HomePage.waitUntilLoaded already relies on.
     *
     * Best-effort by design: if Home genuinely cannot be reached the test still runs and
     * fails on its own assertion with full diagnostics, which is far easier to triage than
     * a setUp() error that obscures which test was affected. */
    private void resetToHome() {
        // Probe cheaply. The configured 5s implicit wait would otherwise be paid on every
        // negative lookup below (up to five back attempts). Restored in the finally block
        // so the test body still runs under the framework's normal wait strategy.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            dismissSoftKeyboardIfShown();
            dismissKnownPermissionDialog();
            dismissDiscardPostDialogIfPresent();
            if (tapHomeIfPresent()) {
                return;
            }
            // Home tab isn't on screen: a modal or full-screen viewer is covering the
            // bottom bar. Back out a bounded number of times rather than force-stopping
            // the app, which would cost the warm, logged-in session this suite depends on.
            for (int attempt = 0; attempt < MAX_BACK_ATTEMPTS; attempt++) {
                navigateBackQuietly();
                dismissKnownPermissionDialog();
                // Backing out of Create Post with unsaved changes RAISES this dialog, so it
                // has to be handled inside the loop, not only before it.
                dismissDiscardPostDialogIfPresent();
                if (tapHomeIfPresent()) {
                    return;
                }
            }
        } catch (RuntimeException ignored) {
            // The isolation guard must never itself fail a test.
        } finally {
            driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT);
        }
    }

    /** A keyboard left open by the previous test covers the floating tab bar. */
    private void dismissSoftKeyboardIfShown() {
        BasePage.hideKeyboardIfShown(driver);
    }

    private boolean tapHomeIfPresent() {
        List<WebElement> home = driver.findElements(HOME_BOTTOM_NAV);
        if (home.isEmpty() || !home.get(0).isDisplayed()) {
            return false;
        }
        home.get(0).click();
        return true;
    }

    /** Dismisses only an iOS system permission alert, matched as an alert button so app
     * content with the same wording can never be tapped. */
    private void dismissKnownPermissionDialog() {
        for (String label : PERMISSION_ALLOW_LABELS) {
            List<WebElement> buttons = driver.findElements(AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeAlert/**/XCUIElementTypeButton[`label == "
                            + BasePage.predicateLiteral(label).replace('\'', '"') + "`]"));
            if (!buttons.isEmpty()) {
                try {
                    buttons.get(0).click();
                } catch (RuntimeException ignored) {
                    // Dialog may have dismissed itself between the lookup and the tap.
                }
                return;
            }
        }
    }

    /** Confirms the "Discard post?" dialog when - and ONLY when - that exact dialog is on
     * screen, so the reset can escape a Create Post screen left holding unsaved changes.
     *
     * Confirmed live: backing out of Create Post with an attached image raises this modal,
     * which absorbs the BACK press. The reset's back-loop therefore made no progress and
     * every following test failed at "Create post should open" - 11 PostTest cases in one
     * run, each burning its full wait budget first.
     *
     * Discarding is safe here, and deliberately narrow:
     *  - this runs from @Before, BEFORE the test body, so any draft on screen belongs to a
     *    PREVIOUS test that has already finished and had its verdict recorded;
     *  - no test asserts on a draft left behind by another test - TC154 documents that
     *    Save Draft has no observable effect to assert against, and there is no Drafts
     *    list screen in the app;
     *  - PostTest already discards leftover drafts itself in discardLeftoverDraft(), so
     *    this matches the suite's own established convention rather than inventing one.
     *
     * Gated on the dialog TITLE, not the button, so a stray "Discard" label on any other
     * screen can never be tapped by this. */
    private void dismissDiscardPostDialogIfPresent() {
        By discardTitle = BasePage.byExactText("Discard post?");
        if (driver.findElements(discardTitle).isEmpty()) {
            return;
        }
        List<WebElement> confirm = driver.findElements(BasePage.byExactText("Discard"));
        if (confirm.isEmpty()) {
            return;
        }
        try {
            confirm.get(0).click();
        } catch (RuntimeException ignored) {
            // Dialog may have dismissed itself between the lookup and the tap.
        }
    }

    /** Back, but tolerant of the app not being frontmost - the photo picker and share
     * sheet run out of process on iOS, where our back controls do not exist. */
    private void navigateBackQuietly() {
        try {
            if (driver.queryAppState(bundleId()) != ApplicationState.RUNNING_IN_FOREGROUND) {
                driver.activateApp(bundleId());
                return;
            }
            BasePage.navigateBack(driver);
        } catch (RuntimeException ignored) {
            // The next visibility check governs the retry.
        }
    }

    protected String bundleId() {
        return getProperty("appium.bundleId", DEFAULT_BUNDLE_ID);
    }

    /** Drives the app from a genuinely fresh install (first-run language picker, then
     * the phone-number login screen) to the dashboard, using a dedicated test phone
     * number and a static/QA-bypass OTP - both read from environment variables only,
     * never a -D property or a hardcoded literal. Reuses LoginPage/OnboardingPage as-is;
     * no existing page object or test class is modified by this. */
    private void establishAuthenticatedSession() {
        String phone = System.getenv("AIKYAM_TEST_PHONE");
        String otp = System.getenv("AIKYAM_TEST_OTP");
        if (phone == null || phone.isBlank() || otp == null || otp.isBlank()) {
            throw new IllegalStateException(
                    "AIKYAM_TEST_PHONE and AIKYAM_TEST_OTP must both be set as environment "
                            + "variables when appium.bstackLogin=true.");
        }

        dismissFirstRunLanguagePickerIfPresent();

        LoginPage loginPage = new LoginPage(driver);
        if (!loginPage.isPhoneNumberScreenVisible()) {
            return;
        }

        if (!loginPage.enterPhoneNumber(phone)) {
            throw new IllegalStateException(
                    "Phone number did not land in the field - aborting rather than continuing blind.");
        }
        if (!loginPage.tapSendVerificationCode()) {
            throw new IllegalStateException("Send Verification Code was not tappable.");
        }
        if (!loginPage.waitForOtpScreen() || !loginPage.isOtpScreenVisible()) {
            throw new IllegalStateException(
                    "OTP screen was not reached - aborting rather than typing the OTP into "
                            + "whatever screen is currently showing.");
        }

        loginPage.dismissSmsConsentIfPresent();

        // Bounded retry, re-checking the OTP screen is still showing each time - sendKeys()
        // into this Compose field is not 100% reliable on the first try, and the local-adb
        // fallback inside typeIntoFirstVisible cannot reach a remote BrowserStack device, so
        // this is the only retry path available in this environment.
        boolean otpEntered = false;
        for (int attempt = 0; attempt < 3 && loginPage.isOtpScreenVisible(); attempt++) {
            if (loginPage.enterOtp(otp)) {
                otpEntered = true;
                break;
            }
        }
        if (!otpEntered) {
            throw new IllegalStateException("OTP could not be entered after retrying.");
        }

        FeedPage feedPage = new FeedPage(driver);
        if (!loginPage.waitForDashboard(feedPage)) {
            throw new IllegalStateException("Dashboard was not reached after OTP entry.");
        }

        // A brand-new account lands on the "Tell us about yourself" profile-setup screen
        // after OTP verification; an already-onboarded test account skips straight to the
        // dashboard. Handle both without asserting either way.
        OnboardingPage onboardingPage = new OnboardingPage(driver);
        if (onboardingPage.isScreenVisible()) {
            onboardingPage.completeWithValidData(
                    "QA Automation", "qa_automation_" + System.currentTimeMillis(), "Other");
            onboardingPage.waitForDashboard(feedPage);
        }
    }

    /** The very first launch of a freshly-installed app shows a language-selection
     * screen with no locator anywhere else in this suite (every existing test assumes
     * this step is already behind it) - a one-shot, best-effort dismissal rather than a
     * new page object, since it exists only to get out of the way of the real
     * precondition-establishment below.
     *
     * Requires this screen's own distinctive title ("Namaste! Pick your language." -
     * confirmed live from the first BrowserStack POC screenshot) before tapping Continue,
     * rather than matching on the "Continue" label alone: that label is not unique to
     * this screen - OnboardingPage's own "Tell us about yourself" form uses it too - and
     * a blind tap there would submit that form with blank required fields before
     * establishAuthenticatedSession() gets a chance to fill it in properly. */
    private void dismissFirstRunLanguagePickerIfPresent() {
        By languageScreenAnchor = AppiumBy.iOSNsPredicateString(
                "label CONTAINS 'Pick your language'");
        By continueAction = BasePage.byExactText("Continue");
        try {
            List<WebElement> anchorMatches = driver.findElements(languageScreenAnchor);
            if (anchorMatches.isEmpty() || !anchorMatches.get(0).isDisplayed()) {
                return;
            }
            List<WebElement> continueMatches = driver.findElements(continueAction);
            if (!continueMatches.isEmpty() && continueMatches.get(0).isDisplayed()) {
                continueMatches.get(0).click();
            }
        } catch (RuntimeException ignored) {
            // Best-effort: if this screen isn't showing, there's nothing to dismiss.
        }
    }

    /** Best-effort: a diagnostics-capture failure must never mask the real test failure,
     * so every step here swallows its own exceptions independently. */
    private void captureFailureDiagnostics(Description description, Throwable failure) {
        String testId = description.getClassName() + "." + description.getMethodName();
        Path dir = DIAGNOSTICS_DIR.resolve(description.getTestClass().getSimpleName());
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
            return;
        }

        // setUp() can fail BEFORE a driver exists - a session that never started (adb
        // unreachable, device offline, Appium refusing) leaves driver == null. This used
        // to return silently, so those failures produced no diagnostic record at all and
        // looked indistinguishable from a test that was never attempted. Write the record
        // anyway, stating plainly that there was no session to photograph. The original
        // exception is preserved verbatim and is still what JUnit reports - nothing here
        // replaces or hides it.
        if (driver == null) {
            try {
                String summary = "Test: " + testId + System.lineSeparator()
                        + "Diagnostics: driver unavailable; session was not created."
                        + " No screenshot or page source could be captured."
                        + System.lineSeparator()
                        + "Failure: " + failure.getClass().getName() + ": " + failure.getMessage()
                        + System.lineSeparator();
                Files.write(dir.resolve(description.getMethodName() + "_failure.txt"),
                        summary.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                // Diagnostics must never mask the real failure.
            }
            return;
        }
        try {
            File screenshot = driver.getScreenshotAs(OutputType.FILE);
            Files.copy(screenshot.toPath(), dir.resolve(description.getMethodName() + ".png"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (RuntimeException | IOException ignored) {
        }
        try {
            String pageSource = driver.getPageSource();
            Files.write(dir.resolve(description.getMethodName() + "_pagesource.xml"),
                    (pageSource == null ? "" : pageSource).getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException | IOException ignored) {
        }
        try {
            String summary = "Test: " + testId + System.lineSeparator()
                    + "Failure: " + failure.getClass().getName() + ": " + failure.getMessage()
                    + System.lineSeparator();
            Files.write(dir.resolve(description.getMethodName() + "_failure.txt"),
                    summary.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }
    }

    private String getProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /** Credentials come from BROWSERSTACK_USERNAME/BROWSERSTACK_ACCESS_KEY environment
     * variables only - never a -D system property (which shows up in process listings
     * and shell history) and never hardcoded. Fails fast with a clear message rather
     * than letting IOSDriver's constructor produce an opaque auth error. */
    private void applyBrowserStackOptions(XCUITestOptions options, String bstackApp) {
        String username = System.getenv("BROWSERSTACK_USERNAME");
        String accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY");
        if (username == null || username.isBlank() || accessKey == null || accessKey.isBlank()) {
            throw new IllegalStateException(
                    "BROWSERSTACK_USERNAME and BROWSERSTACK_ACCESS_KEY must both be set as "
                            + "environment variables to run against appium.bstackApp.");
        }

        Map<String, Object> bstackOptions = new HashMap<>();
        bstackOptions.put("userName", username);
        bstackOptions.put("accessKey", accessKey);
        bstackOptions.put("deviceName", getProperty("appium.deviceName", DEFAULT_DEVICE_NAME));
        bstackOptions.put("osVersion", getProperty("appium.osVersion", ""));
        bstackOptions.put("projectName", getProperty("appium.bstackProject", "Aikyam Devotee iOS"));
        bstackOptions.put("buildName", getProperty("appium.bstackBuild", "ios-appium"));
        bstackOptions.put("sessionName", getProperty("appium.bstackSession", "BrowserStack POC"));
        bstackOptions.put("realMobile", "true");

        options.setApp(bstackApp);
        options.setCapability("bstack:options", bstackOptions);
    }
}
