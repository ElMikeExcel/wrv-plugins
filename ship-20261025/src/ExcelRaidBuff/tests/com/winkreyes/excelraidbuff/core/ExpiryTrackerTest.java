package com.winkreyes.excelraidbuff.core;

import java.util.Map;
import java.util.Set;

/**
 * The raid lifetime caps (0.1.2): a raid's clock starts at its
 * first sighting, fires exactly at the cap, and is moved by
 * NOTHING in between — there is no progress input to this
 * tracker on purpose. The clock survives a simulated restart
 * (snapshot out, restore into a fresh tracker), raids end
 * cleanly before the cap, and raids keep independent clocks.
 *
 * Times are epoch milliseconds, injected: T0 is arbitrary.
 */
public final class ExpiryTrackerTest {

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

    private static final long T0 = 1_700_000_000_000L;
    private static final long CAP = 6L * 3_600_000L; // 6 hours
    private static final int RAID = 7;
    private static final int OTHER = 9;

    public static void main(String[] args) {
        // --- First sighting starts the clock, exactly once.
        ExpiryTracker t = new ExpiryTracker();
        check(t.startOf(RAID) == -1, "an unseen raid has no clock");
        check(t.onRaidSeen(RAID, T0), "the first sighting starts a clock");
        check(!t.onRaidSeen(RAID, T0 + 60_000), "a later sighting starts nothing");
        check(t.startOf(RAID) == T0, "the clock reads its true start");
        check(t.ageMs(RAID, T0 + 90_000) == 90_000, "age is now minus start");

        // --- The cap fires exactly at the cap, never before.
        check(!t.isExpired(RAID, T0, CAP), "a newborn raid is not expired");
        check(!t.isExpired(RAID, T0 + CAP - 1, CAP), "one ms before the cap is not expired");
        check(t.isExpired(RAID, T0 + CAP, CAP), "the cap fires exactly at the cap");
        check(t.isExpired(RAID, T0 + CAP * 3, CAP), "well past the cap is expired");

        // --- Nothing extends the cap: sightings (the adapter's
        // only mid-life input) never move the start, however
        // often the raid is seen across its whole life.
        ExpiryTracker busy = new ExpiryTracker();
        busy.onRaidSeen(RAID, T0);
        for (int i = 1; i <= 100; i++) {
            busy.onRaidSeen(RAID, T0 + i * 60_000L); // a wave a minute, say
        }
        check(busy.startOf(RAID) == T0,
                "a hundred busy sightings never move the start (no progress input exists)");
        check(busy.isExpired(RAID, T0 + CAP, CAP),
                "a raid that fought non-stop still dies down at the cap");

        // --- A raid finished before the cap is untouched.
        ExpiryTracker won = new ExpiryTracker();
        won.onRaidSeen(RAID, T0);
        check(won.onRaidEnd(RAID), "ending a tracked raid reports the drop");
        check(won.trackedRaids() == 0, "a finished raid is no longer tracked");
        check(won.startOf(RAID) == -1 && !won.isExpired(RAID, T0 + CAP * 2, CAP),
                "a finished raid can never expire after the fact");
        check(!won.onRaidEnd(RAID + 100), "ending an untracked raid drops nothing");

        // --- The clock survives a restart: snapshot, fresh
        // tracker, restore — the start (and the deadline) hold.
        ExpiryTracker before = new ExpiryTracker();
        before.onRaidSeen(RAID, T0);
        before.onRaidSeen(OTHER, T0 + 3_600_000L);
        Map<Integer, Long> saved = before.snapshot();
        ExpiryTracker after = new ExpiryTracker();
        after.restore(saved);
        check(after.startOf(RAID) == T0, "a restored clock keeps its start");
        check(!after.isExpired(RAID, T0 + CAP - 1, CAP)
                        && after.isExpired(RAID, T0 + CAP, CAP),
                "a restored raid still dies down at its original deadline");
        check(!after.onRaidSeen(RAID, T0 + CAP),
                "the first sighting after a restart does not restart the clock");

        // --- Restore never moves a live clock.
        ExpiryTracker live = new ExpiryTracker();
        live.onRaidSeen(RAID, T0 + 1_000);
        live.restore(Map.of(RAID, T0));
        check(live.startOf(RAID) == T0 + 1_000, "restore loses to a clock already running");

        // --- The snapshot is a copy: mutating it moves nothing.
        saved.put(RAID, T0 + 999_999L);
        check(before.startOf(RAID) == T0, "the snapshot is defensive");

        // --- Two raids expire independently.
        ExpiryTracker two = new ExpiryTracker();
        two.onRaidSeen(RAID, T0);
        two.onRaidSeen(OTHER, T0 + CAP / 2);
        check(two.isExpired(RAID, T0 + CAP, CAP) && !two.isExpired(OTHER, T0 + CAP, CAP),
                "each raid dies down on its own clock");
        two.onRaidEnd(RAID);
        check(two.trackedRaids() == 1 && two.startOf(OTHER) == T0 + CAP / 2,
                "ending one leaves the other's clock untouched");

        // --- retainOnly: prune the vanished, keep the live.
        ExpiryTracker p = new ExpiryTracker();
        p.onRaidSeen(RAID, T0);
        p.onRaidSeen(OTHER, T0);
        check(p.retainOnly(Set.of(OTHER)).equals(Set.of(RAID)),
                "pruning reports exactly the vanished raid");
        check(p.trackedRaids() == 1 && p.startOf(OTHER) == T0, "the live raid survives pruning");

        // --- reset: everything goes.
        ExpiryTracker z = new ExpiryTracker();
        z.onRaidSeen(RAID, T0);
        z.onRaidSeen(OTHER, T0);
        check(z.reset().equals(Set.of(RAID, OTHER)), "reset reports every tracked raid");
        check(z.trackedRaids() == 0 && z.oldestStartMs() == -1, "reset empties the tracker");

        // --- The status facts: oldest start.
        ExpiryTracker o = new ExpiryTracker();
        o.onRaidSeen(OTHER, T0 + 5_000);
        o.onRaidSeen(RAID, T0);
        check(o.oldestStartMs() == T0, "the oldest start is the earliest clock");

        System.out.println("ExpiryTrackerTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
