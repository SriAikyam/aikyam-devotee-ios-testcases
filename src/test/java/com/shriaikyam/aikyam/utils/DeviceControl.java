package com.shriaikyam.aikyam.utils;

/**
 * Which host the run targets, and the reasons a case can only run on one of them.
 *
 * Used with {@code Assume.assumeTrue(REASON, DeviceControl.isSimulator())} so a case that
 * cannot work on the other host is SKIPPED with its reason recorded, rather than failing or
 * being deleted - the run log then says why, and the case still runs where it is valid.
 */
public final class DeviceControl {

    public static final String SIMULATOR_ONLY_SHARE_SHEET =
            "WebDriverAgent cannot see the iOS share sheet on a physical device - it is a "
                    + "separate out-of-process presentation the device's WDA build does not "
                    + "surface. Runs on a simulator only.";

    public static final String SIMULATOR_ONLY_SEEDED_ACCOUNT =
            "Depends on the seeded simulator account's data (profile fields, story circles, "
                    + "priest temple membership). The physical test phone is signed into a "
                    + "real account where that data does not exist. Runs on a simulator only.";

    public static final String SIMULATOR_ONLY_PUSH =
            "Delivers notifications with `xcrun simctl push`, which only reaches a simulator. "
                    + "Runs on a simulator only.";

    /** True when this run targets a simulator. A simulator udid is an uppercase hyphenated
     * UUID; a physical device's is not. No udid at all means the default local simulator.
     *
     * NOTE: build.gradle.kts carries the same UUID shape for its simctl guard. Two copies,
     * deliberately - Gradle cannot see the test classpath at configuration time - so if the
     * rule ever changes, change it in both. The matching back-pointer sits next to the
     * regex in build.gradle.kts, so the coupling is visible from either side. */
    public static boolean isSimulator() {
        String udid = System.getProperty("appium.udid");
        return udid == null || udid.isBlank() || "booted".equals(udid)
                || udid.matches("^[0-9A-F]{8}(-[0-9A-F]{4}){3}-[0-9A-F]{12}$");
    }

    private DeviceControl() {
    }
}
