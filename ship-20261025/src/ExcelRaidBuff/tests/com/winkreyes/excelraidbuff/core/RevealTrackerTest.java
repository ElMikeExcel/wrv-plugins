package com.winkreyes.excelraidbuff.core;

import java.util.Set;

/**
 * The stuck-wave reveal's stall clocks (0.1.1): a raid reveals
 * only after a full quiet span with living members; every
 * progress signal (member death / member damaged / member dealt
 * damage / new wave — the adapter routes all four to onProgress)
 * resets the clock and clears a live reveal; a raid end drops
 * the raid; the announce line is owed exactly once per raid.
 *
 * Times are milliseconds, injected: t0 is an arbitrary epoch.
 */
public final class RevealTrackerTest {

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

    private static final long T0 = 1_000_000L;
    private static final long QUIET = 180_000L;
    private static final int RAID = 7;
    private static final int OTHER = 9;

    public static void main(String[] args) {
        // --- The stall clock: nothing fires early.
        RevealTracker t = new RevealTracker();
        t.onRaidSeen(RAID, T0);
        check(t.evaluate(RAID, T0, QUIET, true) == RevealTracker.Activation.NONE,
                "no reveal at first sighting");
        check(t.evaluate(RAID, T0 + QUIET - 1, QUIET, true) == RevealTracker.Activation.NONE,
                "no reveal one ms before the quiet span");
        check(t.evaluate(RAID, T0 + QUIET, QUIET, true) == RevealTracker.Activation.ACTIVATED,
                "reveal fires exactly at the quiet span");
        check(t.isRevealed(RAID), "raid reads revealed after activation");
        check(t.evaluate(RAID, T0 + QUIET + 60_000, QUIET, true)
                        == RevealTracker.Activation.STILL_REVEALED,
                "a stalled raid stays revealed on later sweeps");

        // --- Progress clears a live reveal and restarts the clock.
        check(t.onProgress(RAID, T0 + QUIET + 61_000),
                "progress during a reveal reports it cleared one");
        check(!t.isRevealed(RAID), "progress un-reveals the raid");
        check(t.evaluate(RAID, T0 + QUIET + 61_000 + QUIET - 1, QUIET, true)
                        == RevealTracker.Activation.NONE,
                "the re-stall clock runs from the progress signal");
        check(t.evaluate(RAID, T0 + QUIET + 61_000 + QUIET, QUIET, true)
                        == RevealTracker.Activation.ACTIVATED,
                "a raid can stall and reveal a second time");

        // --- A raid that keeps making progress never reveals.
        RevealTracker busy = new RevealTracker();
        busy.onRaidSeen(RAID, T0);
        boolean everRevealed = false;
        for (int i = 1; i <= 10; i++) {
            long at = T0 + i * 60_000L;
            busy.onProgress(RAID, at); // a signal every minute, say a new wave then a death
            everRevealed |= busy.evaluate(RAID, at + 30_000, QUIET, true)
                    != RevealTracker.Activation.NONE;
        }
        check(!everRevealed, "progress every minute never reveals (signals reset the clock)");

        // --- No living members: no reveal, however long the quiet.
        RevealTracker empty = new RevealTracker();
        empty.onRaidSeen(RAID, T0);
        check(empty.evaluate(RAID, T0 + QUIET * 10, QUIET, false)
                        == RevealTracker.Activation.NONE,
                "an emptied raid never reveals");
        check(!empty.isRevealed(RAID), "an emptied raid is not marked revealed");

        // --- The announce line: owed once per raid, across re-stalls.
        RevealTracker a = new RevealTracker();
        a.onRaidSeen(RAID, T0);
        check(a.announceOwed(RAID), "announce owed before the first activation");
        a.evaluate(RAID, T0 + QUIET, QUIET, true);
        a.markAnnounced(RAID);
        check(!a.announceOwed(RAID), "announce settled once marked");
        a.onProgress(RAID, T0 + QUIET + 1_000);
        a.evaluate(RAID, T0 + QUIET + 1_000 + QUIET, QUIET, true);
        check(!a.announceOwed(RAID), "a re-stall does not owe the line a second time");

        // --- Raid end drops the raid (and reports a live reveal).
        RevealTracker e = new RevealTracker();
        e.onRaidSeen(RAID, T0);
        e.evaluate(RAID, T0 + QUIET, QUIET, true);
        check(e.onRaidEnd(RAID), "ending a revealed raid reports the clear");
        check(e.trackedRaids() == 0, "an ended raid is no longer tracked");
        check(e.evaluate(RAID, T0 + QUIET + 1, QUIET, true) == RevealTracker.Activation.NONE,
                "a raid key reused after its end starts a fresh clock");
        check(!e.onRaidEnd(RAID + 100), "ending an untracked raid clears nothing");

        // --- retainOnly: prune the ended, keep the live, report the lit.
        RevealTracker p = new RevealTracker();
        p.onRaidSeen(RAID, T0);
        p.onRaidSeen(OTHER, T0);
        p.evaluate(RAID, T0 + QUIET, QUIET, true);       // RAID revealed
        p.evaluate(OTHER, T0 + QUIET - 1, QUIET, true);  // OTHER not
        Set<Integer> cleared = p.retainOnly(Set.of(OTHER));
        check(cleared.equals(Set.of(RAID)), "pruning reports exactly the revealed dropouts");
        check(p.trackedRaids() == 1 && !p.isRevealed(OTHER), "the live raid survives pruning");

        // --- reset: everything goes, lit raids reported.
        RevealTracker z = new RevealTracker();
        z.onRaidSeen(RAID, T0);
        z.onRaidSeen(OTHER, T0);
        z.evaluate(RAID, T0 + QUIET, QUIET, true);
        check(z.reset().equals(Set.of(RAID)), "reset reports the revealed raids");
        check(z.trackedRaids() == 0, "reset empties the tracker");

        // --- Raids are independent.
        RevealTracker two = new RevealTracker();
        two.onRaidSeen(RAID, T0);
        two.onRaidSeen(OTHER, T0 + QUIET); // OTHER starts a full span later
        check(two.evaluate(RAID, T0 + QUIET, QUIET, true) == RevealTracker.Activation.ACTIVATED
                        && two.evaluate(OTHER, T0 + QUIET, QUIET, true)
                        == RevealTracker.Activation.NONE,
                "each raid keeps its own stall clock");

        System.out.println("RevealTrackerTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
