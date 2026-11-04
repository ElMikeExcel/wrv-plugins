/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;

/**
 * The 0.2.3 announcement window: the active-window truth table
 * (incl. the boundary and the unset-until case), the hourly due
 * check, the per-seal rally throttle (grants, denials that never
 * stamp, per-seal independence), and the combined cooldown list
 * text (empty/single/multiple, ordering, remaining-time format).
 */
public final class AnnouncementsTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    private static void checkEq(Object actual, Object expected, String label) {
        check(expected.equals(actual),
                label + " (expected <" + expected + "> but was <" + actual + ">)");
    }

    public static void main(String[] args) {
        long hour = 3_600_000L;
        long t0 = 1_700_000_000_000L;
        long until = t0 + 48 * hour;

        // --- windowActive truth table ---
        check(!Announcements.windowActive(false, until, t0),
                "window closed when the master switch is off, even inside the dates");
        check(!Announcements.windowActive(true, 0L, t0),
                "window closed when until is unset (0)");
        check(!Announcements.windowActive(true, -5L, t0),
                "window closed when until is negative");
        check(Announcements.windowActive(true, until, t0),
                "window open at the start instant");
        check(Announcements.windowActive(true, until, until - 1),
                "window open one ms before until");
        check(!Announcements.windowActive(true, until, until),
                "window closed exactly AT until (strictly-before rule)");
        check(!Announcements.windowActive(true, until, until + hour),
                "window closed after until");

        // --- hourlyDue ---
        check(Announcements.hourlyDue(0L, hour, t0),
                "hourly due when it never fired (last = 0)");
        check(!Announcements.hourlyDue(t0, hour, t0 + hour - 1),
                "hourly not due one ms before the interval");
        check(Announcements.hourlyDue(t0, hour, t0 + hour),
                "hourly due exactly at the interval");
        check(Announcements.hourlyDue(t0, hour, t0 + 5 * hour),
                "hourly still due long after (no catch-up burst logic here)");
        check(Announcements.hourlyDue(t0, 0L, t0),
                "a zero interval is always due (config clamps to >= 1 min upstream)");

        // --- RallyThrottle ---
        Announcements.RallyThrottle throttle = new Announcements.RallyThrottle();
        long fiveMin = 300_000L;
        check(throttle.tryAnnounce("warden", t0, fiveMin),
                "first rally announce for a seal is granted");
        check(!throttle.tryAnnounce("warden", t0 + 1, fiveMin),
                "second announce inside the cooldown is denied");
        check(!throttle.tryAnnounce("warden", t0 + fiveMin - 1, fiveMin),
                "announce one ms before the cooldown ends is denied");
        check(throttle.lastAnnouncedMs("warden") == t0,
                "denied tries never move the stamp");
        check(throttle.tryAnnounce("warden", t0 + fiveMin, fiveMin),
                "announce exactly at the cooldown end is granted");
        check(throttle.lastAnnouncedMs("warden") == t0 + fiveMin,
                "a grant re-stamps");
        check(throttle.tryAnnounce("wither", t0 + 1, fiveMin),
                "throttles are per-seal: another seal announces freely");
        check(throttle.lastAnnouncedMs("elder_guardian") == 0L,
                "an untouched seal reads 0");

        // --- cooldownListText ---
        checkEq(Announcements.cooldownListText(List.of()), "",
                "no parts -> empty text (callers stay silent)");
        checkEq(Announcements.cooldownListText(List.of(
                        new Announcements.CooldownPart("The Warden", 0L))), "",
                "a zero-remaining part is skipped (expired between check and render)");
        checkEq(Announcements.cooldownListText(List.of(
                        new Announcements.CooldownPart("The Warden", 5 * hour + 42 * 60_000L))),
                "The Warden seal (5h 42m left)",
                "single part renders name + seal + formatted remaining");
        checkEq(Announcements.cooldownListText(List.of(
                        new Announcements.CooldownPart("The Wither", 12 * 60_000L),
                        new Announcements.CooldownPart("The Warden", 5 * hour + 42 * 60_000L))),
                "The Wither seal (12m left), The Warden seal (5h 42m left)",
                "multiple parts join with ', ' in the order given");
        checkEq(Announcements.cooldownListText(List.of(
                        new Announcements.CooldownPart("The Wither", -1L),
                        new Announcements.CooldownPart("The Warden", 38_000L))),
                "The Warden seal (38s left)",
                "negative remaining is skipped; sub-minute renders as seconds");

        System.out.println("AnnouncementsTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

