# Aikyam Devotee iOS - Appium Test Cases

Automated UI tests for the Aikyam devotee iOS app (Java 17, JUnit 4, Gradle, Appium 3 with the
XCUITest driver).

This suite is a port of the Android suite in
[aikyam-devotee-android-testcases](https://github.com/SriAikyam/aikyam-devotee-android-testcases)
(`main`, only verified-passing tests). The test classes and page-object APIs are the same;
the page objects locate iOS elements, mostly through `accessibilityIdentifier`s set in the
SwiftUI views (`<screen>.<element>`, e.g. `tab.home`, `feed.post.like`).

## Requirements

- macOS with Xcode and an iOS simulator (default: `iPhone 17 Pro`)
- JDK 17+
- Appium 3.x with the XCUITest driver (`appium driver install xcuitest`), default
  `http://127.0.0.1:4723`
- The **Dev-Debug** build (`AikyamDevotee-Dev` scheme, bundle `com.shriaikyam.ikyam.dev`)
  installed on the simulator and already logged in. Never run against the prod bundle.

## Build and install the app (from the repo root, on latest `main`)

```bash
xcodebuild -project AikyamDevotee.xcodeproj -scheme AikyamDevotee-Dev \
  -configuration Dev-Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -derivedDataPath build/DerivedData build
xcrun simctl boot "iPhone 17 Pro"; open -a Simulator
xcrun simctl install booted "build/DerivedData/Build/Products/Dev-Debug-iphonesimulator/Aikyam Devotee.app"
```

Log in once on the simulator with the test account; tests never log in themselves.

## Run

```bash
cd AppiumTests
./gradlew p0Smoke -Dappium.udid=<simulator-udid>   # smoke suite
./gradlew test    -Dappium.udid=<simulator-udid>   # every test
```

Optional properties: `appium.serverUrl`, `appium.bundleId`, `appium.deviceName`,
`appium.platformVersion`, `appium.worker`.

Read the verdict from the JUnit XML in `build/test-results/<task>/`. Failure screenshots and
page sources are saved to `build/test-results/diagnostics/<TestClass>/`.

## Conventions

- Add an `.accessibilityIdentifier` in the SwiftUI view rather than matching on visible text;
  labels are localized (English, Kannada, Hindi, Tamil, Telugu).
- Every test asserts a real outcome and undoes any account change it makes.
- No credentials in git: `AIKYAM_TEST_PHONE`, `AIKYAM_TEST_OTP`, `BROWSERSTACK_USERNAME`,
  `BROWSERSTACK_ACCESS_KEY` come from the environment.
- Do not add `@Ignore` or loosen an assertion to get a green run.
