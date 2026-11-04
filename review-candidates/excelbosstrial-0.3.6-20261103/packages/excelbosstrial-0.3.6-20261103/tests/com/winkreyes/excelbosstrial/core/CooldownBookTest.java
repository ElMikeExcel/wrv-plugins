/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.Map;

/**
 * The 6-hour credit cooldown: start/expiry boundaries, per-seal
 * independence, snapshot/restore across a simulated restart, and
 * the human-readable remaining-time format.
 */
public final class CooldownBookTest {

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

    public static void main(String[] args) {
        long hour = 3_600_000L;
        long t0 = 1_000_000_000L;
        CooldownBook book = new CooldownBook(); // default 6h
        check(book.durationMs() == 6 * hour, "default duration is the settled 6 hours");
        check(!book.isActive("wither", t0), "no cooldown before any start");
        check(book.remainingMs("wither", t0) == 0, "remaining is 0 when inactive");

        book.start("wither", t0);
        check(book.isActive("wither", t0), "active the moment it starts");
        check(book.isActive("wither", t0 + 6 * hour - 1), "active one ms before expiry");
        check(!book.isActive("wither", t0 + 6 * hour), "expires exactly at 6h");
        check(book.remainingMs("wither", t0 + hour) == 5 * hour, "5h left after 1h");
        check(book.remainingMs("wither", t0 + 7 * hour) == 0, "remaining clamps at 0");
        check(!book.isActive("warden", t0), "cooldowns are per-seal");

        // A second unearned kill refreshes (adapter only calls start
        // for DENY_SHARE / DENY_PRESENCE verdicts).
        book.start("wither", t0 + 2 * hour);
        check(book.untilMs("wither") == t0 + 8 * hour, "re-start moves the end to start+6h");

        // Persistence across a simulated restart.
        Map<String, Object> snap = book.snapshot();
        CooldownBook restored = new CooldownBook();
        restored.restore(snap);
        check(restored.isActive("wither", t0 + 3 * hour), "cooldown survives the restart");
        check(restored.untilMs("wither") == t0 + 8 * hour, "the end instant survives exactly");

        // Expired entries restore as inactive.
        CooldownBook late = new CooldownBook();
        late.restore(snap);
        check(!late.isActive("wither", t0 + 9 * hour), "restored cooldown still expires on time");

        // Clear paths.
        check(restored.clear("wither"), "clear reports a removal");
        check(!restored.isActive("wither", t0 + 3 * hour), "cleared cooldown is gone");
        check(!restored.clear("wither"), "clearing twice reports nothing removed");
        book.clearAll();
        check(book.snapshot().isEmpty(), "clearAll empties the book");

        // Custom duration (config cooldown-hours path).
        CooldownBook custom = new CooldownBook(30_000L);
        custom.start("warden", t0);
        check(custom.isActive("warden", t0 + 29_999L) && !custom.isActive("warden", t0 + 30_000L),
                "custom duration honored at both edges");

        // Format: whole hours+minutes, minute resolution below that.
        check(CooldownBook.formatRemaining(6 * hour).equals("6h 0m"), "format 6h 0m");
        check(CooldownBook.formatRemaining(5 * hour + 23 * 60_000L).equals("5h 23m"), "format 5h 23m");
        check(CooldownBook.formatRemaining(42 * 60_000L).equals("42m"), "format 42m");
        check(CooldownBook.formatRemaining(30_000L).equals("30s"), "sub-minute shows seconds");
        check(CooldownBook.formatRemaining(0L).equals("0s"), "format zero");
        check(CooldownBook.formatRemaining(-5_000L).equals("0s"), "negative clamps to 0s");

        System.out.println("CooldownBookTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

