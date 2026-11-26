package com.shriaikyam.aikyam.tests;

import org.junit.Assert;
import org.junit.Assume;

/**
 * Run-time inputs for the iOS seva suites, all from -D properties so no ids are committed.
 *
 *   aikyam.sevaTempleLink   temple short link, e.g. aikyam://p/<code>. Needed by every booking test.
 *   aikyam.sevaTempleId     alternative to the link: aikyam://temples/<id> is built from it.
 *   aikyam.selfDevoteeName  the devotee name as listed on the booking form (e.g. "Rakshitha").
 *   aikyam.ownOrderId       SEVA INSTANCE id (UUID) of an order of the logged-in devotee; order links
 *                           aikyam://orders/{id} and booking pushes carry this id, not the ORD number.
 *   aikyam.ownBookingNo     the same order's number as shown in Order History, without "ORD".
 *   aikyam.ownOrderCompleted true when that order is Completed (TC965, TC970).
 *   aikyam.payu.testMode    true to let tests open PayU. Dev build only (PayU test environment).
 *   aikyam.kanikeChip       Poojari Kanike preset amount to tap (default 20, Android preset).
 *   aikyam.hundiChip        Hundi preset amount to tap (default 21, Android preset).
 *   aikyam.manualNetwork    true when someone is at the phone to toggle airplane mode on cue.
 *   aikyam.seva.strict      true to FAIL (not skip) when an input above is missing. Set it on a P0
 *                           gate run: P0 cases skip without their inputs, and an all-skipped run is green.
 */
final class SevaTestConfig {

    /** Typed on Review & Pay; dev build only, so PayU runs in its test environment. */
    static final String TEST_EMAIL = "qa.seva@example.com";

    private SevaTestConfig() {
    }

    static String requireTempleLink() {
        String link = System.getProperty("aikyam.sevaTempleLink", "").trim();
        if (!link.isEmpty()) {
            return link;
        }
        String id = System.getProperty("aikyam.sevaTempleId", "").trim();
        requireInput("set -Daikyam.sevaTempleLink or -Daikyam.sevaTempleId", !id.isEmpty());
        return "aikyam://temples/" + id;
    }

    static String requireSelfName() {
        String self = System.getProperty("aikyam.selfDevoteeName", "").trim();
        requireInput("set -Daikyam.selfDevoteeName to the devotee name on the booking form", !self.isEmpty());
        return self;
    }

    static String requireOwnOrderId() {
        String id = System.getProperty("aikyam.ownOrderId", "").trim();
        requireInput("set -Daikyam.ownOrderId to an order of the logged-in devotee", !id.isEmpty());
        return id;
    }

    static String requireOwnBookingNumber() {
        String n = System.getProperty("aikyam.ownBookingNo", "").trim().replaceFirst("^ORD", "");
        requireInput("set -Daikyam.ownBookingNo (e.g. 10263)", !n.isEmpty());
        return n;
    }

    /** Missing input: skip, or fail under -Daikyam.seva.strict=true. */
    private static void requireInput(String howToSet, boolean present) {
        if (present) {
            return;
        }
        if (Boolean.getBoolean("aikyam.seva.strict")) {
            Assert.fail("Missing input (aikyam.seva.strict): " + howToSet);
        }
        Assume.assumeTrue("Skipping: " + howToSet, false);
    }

    /** Like any other input: skips, or fails under -Daikyam.seva.strict=true so a gate run can't go green on skips. */
    static void requirePayUTestMode() {
        requireInput("run with -Daikyam.payu.testMode=true (dev build only) to open PayU",
                Boolean.getBoolean("aikyam.payu.testMode"));
    }

    /** iOS automation cannot switch the network off, so these cases need a person at the phone. */
    static void requireManualNetwork() {
        Assume.assumeTrue("Skipping: airplane mode can't be automated on iOS. Run with -Daikyam.manualNetwork=true "
                + "and toggle it on the phone when the log says so", Boolean.getBoolean("aikyam.manualNetwork"));
    }

    /** Prints the instruction and gives the person time to act. */
    static void askPerson(String instruction, int seconds) {
        System.out.println("\n>>> ACTION ON THE PHONE: " + instruction + " (" + seconds + "s)\n");
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
