/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * Milestone beats (ExcelBossTrial 0.3.2, SEAL-CHALLENGE-AUDIT
 * upgrade 2): the threshold tracker behind the slate-wide
 * broadcast + pulse-surge beats at 75/50/25% of the pool
 * REMAINING. Pure logic, no Bukkit - the adapters (the main
 * plugin for the ledger chapters, DragonChapter for the dragon's
 * phases) feed it remaining fractions and act on the crossings.
 *
 * The shape deliberately mirrors the dragon chapter's wave-mask
 * plumbing (TrialStateMachine.crossedMask / pendingWaveMask /
 * commitWaves): bit i belongs to THRESHOLDS[i], a bit is crossed
 * when the remaining fraction drops to or below its threshold,
 * and a fired bit never fires again for this tracker instance.
 *
 * THE JUDGING RULE (the pool-growth question, settled here): a
 * crossing is judged against the CURRENT pool - for a ledger
 * encounter, the ratcheted maxHealth at the moment of the hit
 * (1 - totalDamage / currentPool); for a dragon phase, the
 * phase's live bar fraction. The pool can GROW mid-fight
 * (participant rally), which pushes the remaining fraction back
 * ABOVE a threshold already crossed; the fired mask is the
 * dedupe: a threshold beat fires at most once per encounter
 * (per phase, for the dragon), however often the fraction
 * re-crosses it. The elder record of Oct 31, 2026 is the shape
 * this codifies: elder C's 75% milestone landed with 2 champions
 * at pool 775, and the pool then grew to 925 as champions 3 and
 * 4 registered - under this rule that beat is spent, correctly,
 * and the 50% beat is judged against the grown pool.
 */
public final class MilestoneBeats {

    /** Remaining-fraction thresholds, high to low. Bit i = THRESHOLDS[i]. */
    public static final double[] THRESHOLDS = {0.75, 0.50, 0.25};

    private int firedMask;

    /** Bit i is set when the remaining fraction has crossed THRESHOLDS[i]. */
    public static int crossedMask(double remainingFraction) {
        int mask = 0;
        for (int i = 0; i < THRESHOLDS.length; i++) {
            if (remainingFraction <= THRESHOLDS[i]) {
                mask |= (1 << i);
            }
        }
        return mask;
    }

    /** Thresholds crossed at this fraction that have not yet fired. */
    public int pending(double remainingFraction) {
        return crossedMask(remainingFraction) & ~firedMask;
    }

    /** Record beats as actually fired (the adapter may fire fewer). */
    public void commit(int mask) {
        firedMask |= mask;
    }

    public int firedMask() {
        return firedMask;
    }

    /** Restore a persisted fired mask (the dragon chapter's state). */
    public void restoreMask(int mask) {
        firedMask = mask & 0b111;
    }

    /** A fresh fight (or a fresh dragon phase): no beats fired. */
    public void reset() {
        firedMask = 0;
    }

    /** The threshold a bit belongs to (for logs and wording). */
    public static double threshold(int bitIndex) {
        return THRESHOLDS[bitIndex];
    }

    /** The threshold as a whole percent of the pool remaining (75/50/25). */
    public static int thresholdPercent(int bitIndex) {
        return (int) Math.round(THRESHOLDS[bitIndex] * 100.0);
    }
}
