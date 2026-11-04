/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The feel patch's view-model (ExcelBossTrial 0.3.6): one
 * encounter's pool numbers, derived ONLY from ledger getters -
 * {@code maxHealth()}, {@code totalDamage()}, {@code dealtBy()} -
 * or, for the dragon, from the chapter's phase pool and phase
 * damage (the dragon's ledger spans both phases; its title is
 * priced per phase).
 *
 * The record holds the TRUE doubles. Remaining clamps at 0 (an
 * overkill hit never prints a negative pool) and the bar
 * fraction clamps to [0, 1]; every rounding decision lives in
 * FeedbackFormat, at render time. Nothing here mutates anything:
 * the display reads the fight, it never writes it - the
 * on/off ledger-equality regression (FeedbackNoDamageChangeTest)
 * pins that the fight is byte-identical with this view computed
 * after every hit or never computed at all.
 */
public record FeedbackTotals(double pool, double dealt, double remaining,
                             double fraction) {

    /** Derive the view from raw pool/dealt numbers (sanitized:
     *  NaN reads as 0, negative dealt reads as 0, a non-positive
     *  pool reads as an empty view). */
    public static FeedbackTotals of(double pool, double dealt) {
        double p = clean(pool);
        double d = Math.max(0.0, clean(dealt));
        if (p <= 0) {
            return new FeedbackTotals(0.0, 0.0, 0.0, 0.0);
        }
        double remaining = Math.max(0.0, p - d);
        double fraction = remaining / p;
        if (fraction < 0.0) {
            fraction = 0.0;
        } else if (fraction > 1.0) {
            fraction = 1.0;
        }
        return new FeedbackTotals(p, d, remaining, fraction);
    }

    /** The standard chapters' view: straight off the ledger. */
    public static FeedbackTotals fromLedger(EncounterLedger ledger) {
        if (ledger == null) {
            return of(0.0, 0.0);
        }
        return of(ledger.maxHealth(), ledger.totalDamage());
    }

    /** The dragon's view: the CURRENT phase's pool and the
     *  damage dealt inside this phase (the title's total flips
     *  4,000 -> 16,000 at the phase boundary by construction). */
    public static FeedbackTotals forPhase(double phasePool, double phaseDealt) {
        return of(phasePool, phaseDealt);
    }

    private static double clean(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0 : value;
    }

    /** The boss bar title for this view under a chapter format. */
    public String title(String format, BossKind kind) {
        return FeedbackFormat.title(format, kind, remaining, pool);
    }

    /** One player's action-bar line: their last hit and their
     *  running total, both read from the same ledger the fight
     *  table reads - the display total IS the table total. */
    public static String actionBarLine(String format, double lastHit,
                                       EncounterLedger ledger,
                                       java.util.UUID player) {
        double total = ledger == null || player == null ? 0.0 : ledger.dealtBy(player);
        return FeedbackFormat.actionBar(format, lastHit, total);
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
