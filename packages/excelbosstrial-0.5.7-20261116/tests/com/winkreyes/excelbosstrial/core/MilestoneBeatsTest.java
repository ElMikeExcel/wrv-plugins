/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.UUID;

/**
 * Milestone beats (0.3.2): the threshold tracker fires once per
 * threshold, a single big step can cross several thresholds at
 * once, crossings are judged against the CURRENT pool, and pool
 * growth (participant rally) can never re-arm a spent beat -
 * the elder C shape from the Oct 31, 2026 record (75% fell at
 * pool 775 with 2 champions; the pool then grew to 925).
 * Settings sanitization + the ledger's surge queue ride along.
 */
public final class MilestoneBeatsTest {

    private static int checks;
    private static int failures;

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + label);
        }
    }

    private static void eq(int actual, int expected, String label) {
        check(actual == expected, label + " (expected " + expected + ", got " + actual + ")");
    }

    public static void main(String[] args) {
        // --- the mask itself
        eq(MilestoneBeats.crossedMask(1.0), 0, "full pool: nothing crossed");
        eq(MilestoneBeats.crossedMask(0.76), 0, "just above 75%: nothing crossed");
        eq(MilestoneBeats.crossedMask(0.75), 0b001, "at 75%: the 75 beat is crossed");
        eq(MilestoneBeats.crossedMask(0.50), 0b011, "at 50%: 75 and 50 crossed");
        eq(MilestoneBeats.crossedMask(0.10), 0b111, "at 10%: all three crossed");
        eq(MilestoneBeats.thresholdPercent(0), 75, "bit 0 is the 75% beat");
        eq(MilestoneBeats.thresholdPercent(2), 25, "bit 2 is the 25% beat");

        // --- fire once per threshold, stepwise
        MilestoneBeats b = new MilestoneBeats();
        eq(b.pending(0.90), 0, "no beat before the first threshold");
        eq(b.pending(0.74), 0b001, "75% beat pending at 74% remaining");
        b.commit(0b001);
        eq(b.pending(0.74), 0, "75% beat never re-fires at the same fraction");
        eq(b.pending(0.49), 0b010, "50% beat pending at 49%");
        b.commit(0b010);
        eq(b.pending(0.24), 0b100, "25% beat pending at 24%");
        b.commit(0b100);
        eq(b.pending(0.0), 0, "nothing left at zero");
        eq(b.firedMask(), 0b111, "all three beats spent");

        // --- one huge step crosses everything at once
        MilestoneBeats big = new MilestoneBeats();
        eq(big.pending(0.20), 0b111, "a single blow from full to 20% crosses all three");
        big.commit(big.pending(0.20));
        eq(big.pending(0.20), 0, "and none of them repeats");

        // --- POOL GROWTH dedupe (the rule): judged on the current
        // pool; a spent beat stays spent when growth lifts the
        // remaining fraction back above its threshold.
        EncounterLedger ledger = new EncounterLedger(BossKind.ELDER_GUARDIAN, new UUID(9, 9), 775.0, 0L);
        UUID a = new UUID(0, 1), c = new UUID(0, 2);
        ledger.recordDealt(a, 200.0, 1_000L, true); // 575/775 left = 74.2%
        double remaining = 1.0 - ledger.totalDamage() / ledger.maxHealth();
        eq(ledger.milestoneBeats().pending(remaining), 0b001,
                "elder shape: 75% beat fires at pool 775");
        ledger.milestoneBeats().commit(0b001);
        // Champions 3 + 4 register: the pool rallies to 925.
        ledger.growTo(925.0);
        ledger.recordDealt(c, 100.0, 2_000L, true); // 300/925 dealt, 67.6% left
        remaining = 1.0 - ledger.totalDamage() / ledger.maxHealth();
        eq(ledger.milestoneBeats().pending(remaining), 0,
                "growth lifted the fraction, but the 75% beat stays spent");
        ledger.recordDealt(c, 200.0, 3_000L, true); // 500/925 dealt, 45.9% left
        remaining = 1.0 - ledger.totalDamage() / ledger.maxHealth();
        eq(ledger.milestoneBeats().pending(remaining), 0b010,
                "the 50% beat is judged against the GROWN pool and fires once");
        ledger.milestoneBeats().commit(0b010);
        ledger.growTo(1200.0); // an even later rally
        ledger.recordDealt(c, 425.0, 4_000L, true); // 925/1200 dealt, 22.9% left
        remaining = 1.0 - ledger.totalDamage() / ledger.maxHealth();
        eq(ledger.milestoneBeats().pending(remaining), 0b100,
                "only the 25% beat remains, against the twice-grown pool");

        // --- growth BEFORE any crossing: thresholds wait for the
        // grown pool (no beat fires early against the small pool).
        EncounterLedger fresh = new EncounterLedger(BossKind.WITHER, new UUID(9, 8), 700.0, 0L);
        fresh.growTo(925.0);
        fresh.recordDealt(a, 231.0, 1_000L, true); // 694/925 left = 75.03%
        double r2 = 1.0 - fresh.totalDamage() / fresh.maxHealth();
        eq(fresh.milestoneBeats().pending(r2), 0,
                "231 into a grown 925 pool has not crossed 75% yet");
        fresh.recordDealt(a, 1.0, 2_000L, true); // 693/925 = 74.9%
        r2 = 1.0 - fresh.totalDamage() / fresh.maxHealth();
        eq(fresh.milestoneBeats().pending(r2), 0b001,
                "one more point crosses it - judged on the grown pool");

        // --- restore + reset (the dragon chapter's persistence)
        MilestoneBeats restored = new MilestoneBeats();
        restored.restoreMask(0b101);
        eq(restored.pending(0.10), 0b010, "restored mask: only the unspent beat fires");
        restored.reset();
        eq(restored.pending(0.10), 0b111, "reset re-arms every beat (a fresh phase)");

        // --- the surge queue on the ledger
        check(!ledger.hasPendingSurge(), "no surge queued at open");
        ledger.queueSurge();
        ledger.queueSurge();
        check(ledger.hasPendingSurge(), "surges queue");
        ledger.consumeSurge();
        check(ledger.hasPendingSurge(), "one consume leaves the second surge");
        ledger.consumeSurge();
        check(!ledger.hasPendingSurge(), "both consumed");
        ledger.consumeSurge();
        check(!ledger.hasPendingSurge(), "an extra consume is a no-op, never negative");

        // --- settings: sanitization + the switches
        MilestoneSettings def = MilestoneSettings.defaults(BossKind.WARDEN);
        check(def.enabled() && def.surgeMultiplier() == 1.0 && def.anyText(),
                "warden defaults: on, one-pulse surge, lines set");
        check(def.text75().contains("quarter of the fight is spent")
                && def.text25().contains("three quarters spent"),
                "warden lines read as progress (spent fractions, no raw numbers)");
        MilestoneSettings clamped = MilestoneSettings.sanitized(true, -2.0, "a", "b", "c");
        check(clamped.surgeMultiplier() == 0.0, "a negative surge multiplier clamps to 0 (surge off)");
        MilestoneSettings nan = MilestoneSettings.sanitized(true, Double.NaN, "a", "b", "c");
        check(nan.surgeMultiplier() == 1.0, "a NaN surge multiplier reads as the default 1.0");
        MilestoneSettings silenced = MilestoneSettings.sanitized(true, 1.0, "", "b", "");
        check(silenced.textFor(0).isEmpty() && !silenced.textFor(1).isEmpty()
                && silenced.textFor(2).isEmpty(),
                "an empty text silences only that threshold's line");
        check(!MilestoneSettings.sanitized(true, 1.0, "", "", "").anyText(),
                "all texts empty = no lines (the surge may still fire)");
        for (BossKind kind : BossKind.values()) {
            MilestoneSettings d = MilestoneSettings.defaults(kind);
            check(d.enabled() && d.anyText() && d.surgeMultiplier() == 1.0,
                    kind + " defaults are the settled slate (on / lines / 1.0 surge)");
        }

        System.out.println("MilestoneBeatsTest: " + checks + " checks, " + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
