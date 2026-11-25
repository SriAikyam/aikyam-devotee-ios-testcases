package com.shriaikyam.aikyam.tests;

import org.junit.Assert;
import org.junit.Assume;

/**
 * Extra -D inputs for the seva corner cases (SevaEdge*Test, QA report groups 5-9). Same rules as
 * SevaTestConfig: nothing is committed, a missing input skips with how to set it, and
 * -Daikyam.seva.strict=true turns that skip into a failure.
 *
 * Seeded data (group 6, 9):
 *   aikyam.emptySevaTempleLink      temple with zero sevas (SEV-K001)
 *   aikyam.multiSevaTempleLink      temple with 2+ sevas priced e.g. ₹1, ₹100, ₹1,000+ (K002, K006)
 *   aikyam.noImageSevaTempleLink    temple whose first seva has no media (K004)
 *   aikyam.longTextSevaTempleLink   temple whose first seva has a very long name/description (K005)
 *   aikyam.emptyAccount             true when the logged-in devotee has no bookings at all (F03, G02)
 *   aikyam.completedMediaOrderId    order id (UUID) of a Completed order with priest photo/video/audio (G04, K407, K515)
 *   aikyam.priestMessage            the priest message text on that order (K408, K515)
 *   aikyam.hasFrequentSevas         true when Home shows "Your frequent sevas" (G05)
 *   aikyam.manyOrders               true when the account has 20+ orders (K404)
 *   aikyam.pastBookingNo            booking number (no ORD) whose seva date has passed (K403)
 *   aikyam.todayBookingNo           temple-side booking number dated today, not yet completed (K513)
 *   aikyam.completedTempleBookingNo temple-side booking number already Completed (K514, H09)
 *   aikyam.sevaHttpsLink            https share link of a seva or temple (I03)
 *   aikyam.matrixTempleIds          comma-separated temple ids for K613 (default T1000699,T051108,T1000710)
 *
 * People at the phone:
 *   aikyam.manualNetwork            (SevaTestConfig) someone toggles airplane mode / Network Link Conditioner
 *   aikyam.manualAssist             someone does what the log says (expire a token, tap a push, open a link)
 *
 * Known-blocked cases skip unless their switch is set:
 *   aikyam.searchReachable          true once search is reachable on iOS (K309)
 *   aikyam.remindersImplemented     true once seva reminders exist (K603)
 */
final class SevaEdgeConfig {

    static final int PERSON_SECONDS = 25;

    private SevaEdgeConfig() {
    }

    static String require(String property, String what) {
        String v = System.getProperty(property, "").trim();
        requireInput("set -D" + property + " (" + what + ")", !v.isEmpty());
        return v;
    }

    static void requireFlag(String property, String what) {
        requireInput("run with -D" + property + "=true (" + what + ")", Boolean.getBoolean(property));
    }

    static void requireManualAssist() {
        Assume.assumeTrue("Skipping: needs a person at the phone. Run with -Daikyam.manualAssist=true and follow "
                + "the '>>> ACTION ON THE PHONE' lines", Boolean.getBoolean("aikyam.manualAssist"));
    }

    /** A case the QA report recorded as blocked by a missing feature: skip with the reason until it ships. */
    static void requireUnblocked(String property, String reason) {
        Assume.assumeTrue("Skipping (blocked): " + reason + ". Run with -D" + property + "=true once it is fixed",
                Boolean.getBoolean(property));
    }

    static String[] matrixTempleLinks() {
        String ids = System.getProperty("aikyam.matrixTempleIds", "T1000699,T051108,T1000710");
        String[] parts = ids.split(",");
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i].trim();
            parts[i] = p.contains("://") ? p : "aikyam://temples/" + p;
        }
        return parts;
    }

    private static void requireInput(String howToSet, boolean present) {
        if (present) {
            return;
        }
        if (Boolean.getBoolean("aikyam.seva.strict")) {
            Assert.fail("Missing input (aikyam.seva.strict): " + howToSet);
        }
        Assume.assumeTrue("Skipping: " + howToSet, false);
    }
}
