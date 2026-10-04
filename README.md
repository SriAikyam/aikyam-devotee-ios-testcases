# Aikyam Devotee Android - Appium Test Cases

Automated UI tests for the Aikyam devotee Android app (Java 17, JUnit 4, Gradle, Appium 3 with UiAutomator2).

**`main` contains only tests that have been run and pass.** Tests that fail, are blocked by
data or environment, or are not yet verified are developed on other branches and are merged
here once they pass.

## Requirements

- JDK 17+
- Appium 3.x server with the UiAutomator2 driver (default `http://127.0.0.1:4723`)
- Android SDK with `ANDROID_HOME` set
- An emulator or device with the **dev** build installed (`com.shriaikyam.aikyam.dev`) and
  already logged in. The tests never log in and never touch the prod build.

## Run

```bash
./gradlew p0Smoke -Dappium.udid=<device-serial>   # build-gate smoke suite
./gradlew test    -Dappium.udid=<device-serial>   # every test on main
```

Optional properties: `appium.serverUrl`, `appium.appPackage`, `appium.worker`.

Gradle prints only failures, so read the verdict from the JUnit XML in
`build/test-results/<task>/`. Failure screenshots and page sources are saved to
`build/test-results/diagnostics/<TestClass>/`.

## Layout

- `src/test/java/.../base/BaseTest.java` - session handling, reset to Home before every test
- `src/test/java/.../pages/` - page objects
- `src/test/java/.../tests/` - test classes (`*JourneyTest` are end-to-end user journeys;
  tests tagged `@Category(P0.class)` form the smoke suite)
- `src/test/java/.../utils/` - adb helpers
- `docs/` - design notes

## Conventions

- Every test asserts a real outcome and undoes any account change it makes.
- No credentials in git. Read them from the environment: `AIKYAM_TEST_PHONE`,
  `AIKYAM_TEST_OTP`, `BROWSERSTACK_USERNAME`, `BROWSERSTACK_ACCESS_KEY`.
- Do not add `@Ignore` or loosen an assertion to get a green run.
