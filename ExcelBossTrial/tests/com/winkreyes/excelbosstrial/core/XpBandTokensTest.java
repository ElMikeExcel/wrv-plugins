package com.winkreyes.excelbosstrial.core;

import java.util.UUID;

/**
 * The XP band governor token store (0.2.4): verdicts pair with
 * gains FIFO per player, are consumed on use, expire after
 * TTL_TICKS, and every failure mode degrades to "no opinion"
 * (null = pay in full). These are the pairing rules the whole
 * governor stands on, so they are pinned hard.
 */
public final class XpBandTokensTest {

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

    private static void isVerdict(Boolean actual, boolean expected, String label) {
        check(actual != null && actual == expected,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    private static void isNull(Object actual, String label) {
        check(actual == null, label + " (expected null, got " + actual + ")");
    }

    public static void main(String[] args) {
        UUID ana = UUID.randomUUID();
        UUID ben = UUID.randomUUID();

        // Empty store: no opinion.
        XpBandTokens s = new XpBandTokens();
        isNull(s.poll(ana, 100), "poll with nothing planted is null");
        check(s.pending(ana, 100) == 0, "pending is 0 on an empty store");
        check(s.totalPending(100) == 0, "totalPending is 0 on an empty store");

        // Basic pairing: plant counted, poll returns it once.
        s.plant(ana, true, 100);
        isVerdict(s.poll(ana, 101), true, "a counted verdict polls back");
        isNull(s.poll(ana, 101), "consume-on-use: the same verdict never answers twice");

        // FIFO across mixed verdicts (the perch/honest interleave).
        s.plant(ana, false, 200);
        s.plant(ana, true, 200);
        s.plant(ana, false, 201);
        isVerdict(s.poll(ana, 202), false, "FIFO: first planted answers first (excluded)");
        isVerdict(s.poll(ana, 202), true, "FIFO: second planted answers second (counted)");
        isVerdict(s.poll(ana, 203), false, "FIFO: third planted answers third (excluded)");
        isNull(s.poll(ana, 203), "queue drained after three polls");

        // TTL boundary: planted at T is consumable at T + TTL, gone at T + TTL + 1.
        long ttl = XpBandTokens.TTL_TICKS;
        s.plant(ana, false, 300);
        isVerdict(s.poll(ana, 300 + ttl), false, "verdict still fresh at T + TTL");
        s.plant(ana, false, 400);
        isNull(s.poll(ana, 400 + ttl + 1), "verdict expired at T + TTL + 1 polls null (pay full)");

        // A stale head must not block a fresher token behind it.
        s.plant(ana, false, 500);
        s.plant(ana, true, 500 + ttl + 2);
        isVerdict(s.poll(ana, 500 + ttl + 2), true,
                "stale head is purged; the fresh verdict behind it still answers");
        isNull(s.poll(ana, 500 + ttl + 2), "and nothing stale answers after it");

        // Per-player isolation.
        s.plant(ana, false, 600);
        s.plant(ben, true, 600);
        isVerdict(s.poll(ben, 601), true, "ben's verdict is his own");
        isVerdict(s.poll(ana, 601), false, "ana's verdict is untouched by ben's poll");
        isNull(s.poll(ben, 601), "ben's queue is drained independently");

        // Pending counts only fresh tokens.
        s.plant(ana, true, 700);
        s.plant(ana, false, 700);
        check(s.pending(ana, 701) == 2, "pending counts both fresh verdicts");
        check(s.pending(ana, 700 + ttl + 1) == 0, "pending drops expired verdicts");
        check(s.totalPending(700 + ttl + 1) == 0, "totalPending drops expired verdicts");

        // The cap drops the OLDEST first (fail toward full pay on the oldest hits).
        XpBandTokens capped = new XpBandTokens();
        int cap = XpBandTokens.MAX_PENDING_PER_PLAYER;
        for (int i = 0; i < cap + 10; i++) {
            capped.plant(ana, i % 2 == 0, 1_000); // even index = counted
        }
        check(capped.pending(ana, 1_000) == cap, "the queue is capped");
        // The first 10 planted were dropped; the oldest survivor is index 10 (counted).
        isVerdict(capped.poll(ana, 1_000), true, "cap dropped the oldest 10; index 10 answers first");
        isVerdict(capped.poll(ana, 1_000), false, "index 11 answers second");

        // A fight-shaped interleave, gains polling ~1 tick after
        // each hit as mcMMO's task does: perch spam with one honest
        // hit in the middle pairs every gain with its own verdict.
        XpBandTokens fight = new XpBandTokens();
        for (int i = 0; i < 20; i++) {
            fight.plant(ana, false, 2_000 + 2L * i);
            isVerdict(fight.poll(ana, 2_001 + 2L * i), false, "perch hit " + i + " scaled");
        }
        fight.plant(ana, true, 2_040);
        isVerdict(fight.poll(ana, 2_041), true, "the one honest hit in the sequence pays full");
        isNull(fight.poll(ana, 2_041), "fight queue fully drained");

        // The listener-absent shape: tokens planted but never polled
        // in their window simply expire - nothing stale can answer a
        // later gain (fail-safe to full pay).
        XpBandTokens abandoned = new XpBandTokens();
        abandoned.plant(ana, false, 2_500);
        abandoned.plant(ana, false, 2_500);
        isNull(abandoned.poll(ana, 2_500 + ttl + 1),
                "unpolled verdicts expire; a later gain gets no opinion");
        // A partially drained queue: the first gain polls in-window,
        // the rest lapse - the lapsed ones never answer afterwards.
        abandoned.plant(ana, false, 2_600);
        abandoned.plant(ana, true, 2_600);
        isVerdict(abandoned.poll(ana, 2_601), false, "first gain pairs with the oldest verdict");
        isNull(abandoned.poll(ana, 2_600 + ttl + 1),
                "the unpaired remainder expired rather than answering a later gain");

        // clear / clearAll hygiene.
        XpBandTokens hygiene = new XpBandTokens();
        hygiene.plant(ana, false, 3_000);
        hygiene.plant(ben, false, 3_000);
        hygiene.clear(ana);
        isNull(hygiene.poll(ana, 3_000), "clear(player) drops that player's queue");
        isVerdict(hygiene.poll(ben, 3_000), false, "clear(player) leaves other players alone");
        hygiene.plant(ana, true, 3_000);
        hygiene.clearAll();
        check(hygiene.totalPending(3_000) == 0, "clearAll empties the store");

        System.out.println("XpBandTokensTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
