/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.UUID;

/**
 * The feel patch's view-model against ledger truth (0.3.6): the
 * display total must BE the fight table's total - the same
 * dealtBy the verdict record reads - and the display must
 * follow what the ledger actually recorded, including what a
 * share band refused.
 */
public final class FeedbackTotalsTest {

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

    private static void eq(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < 1e-9,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    private static void eqs(String actual, String expected, String label) {
        check(expected.equals(actual),
                label + " (expected \"" + expected + "\", got \"" + actual + "\")");
    }

    public static void main(String[] args) {
        UUID a = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        UUID b = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
        long t0 = 1_000_000L;

        // --- Scripted elder sequence on a real ledger (duo pool).
        EncounterLedger ledger = new EncounterLedger(
                BossKind.ELDER_GUARDIAN, UUID.randomUUID(), 10_900, t0);
        ledger.recordDealt(a, 4.0, t0 + 1);
        ledger.recordDealt(a, 11.0, t0 + 2);
        ledger.recordDealt(a, 16.5, t0 + 3);
        ledger.recordDealt(b, 23.8, t0 + 4);

        FeedbackTotals view = FeedbackTotals.fromLedger(ledger);
        eq(view.pool(), 10_900, "view pool is the ledger pool");
        eq(view.dealt(), 55.3, "view dealt is the ledger total (4 + 11 + 16.5 + 23.8)");
        eq(view.remaining(), 10_844.7, "view remaining is pool - dealt, unrounded");
        eqs(view.title(FeedbackSettings.DEFAULT_TITLE_FORMAT, BossKind.ELDER_GUARDIAN),
                "Elder Guardian — 10,845 / 10,900",
                "title renders the true remaining (10,844.7 rounds to 10,845)");

        // --- The display total IS the fight-table total: the
        // same double dealtBy returns, the same value the
        // snapshot's per-player map carries.
        check(ledger.dealtBy(a) == 31.5, "ledger dealtBy(A) is exactly 31.5");
        eqs(FeedbackTotals.actionBarLine(
                        FeedbackSettings.DEFAULT_DAMAGE_TOTAL_FORMAT, 16.5, ledger, a),
                "16.5 hit · 31.5 pool damage total",
                "A's action-bar total is A's ledger dealt");
        check(ledger.dealtSnapshot().get(a) == ledger.dealtBy(a),
                "dealtSnapshot and dealtBy agree (the fight table reads the same truth)");
        check(ledger.dealtSnapshot().get(b) == ledger.dealtBy(b),
                "dealtSnapshot and dealtBy agree for B");
        eqs(FeedbackTotals.actionBarLine(
                        FeedbackSettings.DEFAULT_DAMAGE_TOTAL_FORMAT, 23.8, ledger, b),
                "23.8 hit · 23.8 pool damage total",
                "B's action-bar total is B's ledger dealt");

        // --- Banded hits: the display follows what recordDealt-
        // Banded actually recorded - a refused blow raises the
        // pool total but never the player's total.
        EncounterLedger warden = new EncounterLedger(
                BossKind.WARDEN, UUID.randomUUID(), 7_200, t0);
        warden.recordDealtBanded(b, 20.0, t0 + 1, false, true);
        eq(warden.totalDamage(), 20.0, "banded refused blow still lands on the pool");
        eq(warden.dealtBy(b), 0.0, "banded refused blow credits the player nothing");
        FeedbackTotals wv = FeedbackTotals.fromLedger(warden);
        eq(wv.remaining(), 7_180, "the title follows the pool, refused blow included");
        eqs(FeedbackTotals.actionBarLine(
                        FeedbackSettings.DEFAULT_DAMAGE_TOTAL_FORMAT, 0.0, warden, b),
                "0.0 hit · 0.0 pool damage total",
                "the player's total never invents the refused damage");
        warden.recordDealtBanded(b, 20.0, t0 + 2, true, true);
        eq(warden.dealtBy(b), 20.0, "an in-band blow credits normally");
        eq(FeedbackTotals.fromLedger(warden).remaining(), 7_160,
                "the title follows both blows");

        // --- The dragon's per-phase view: totals flip at the
        // phase boundary by construction.
        FeedbackTotals p1 = FeedbackTotals.forPhase(4_000, 1_000);
        eq(p1.remaining(), 3_000, "phase 1: 4,000 pool, 1,000 dealt");
        eq(p1.fraction(), 0.75, "phase 1 fraction");
        FeedbackTotals p2 = FeedbackTotals.forPhase(16_000, 1_000);
        eqs(p2.title(FeedbackSettings.DEFAULT_TITLE_FORMAT, BossKind.ENDER_DRAGON),
                "Ender Dragon — 15,000 / 16,000",
                "phase 2: the title total is the phase-2 pool");
        check(FeedbackTotals.forPhase(0, 500).pool() == 0,
                "an untracked dragon (pool 0) reads as an empty view");

        // --- Degenerate ledgers stay safe.
        check(FeedbackTotals.fromLedger(null).remaining() == 0,
                "a null ledger reads as an empty view");

        // --- 0.4.0 (PM-5): the bar runs on NET damage - a heal
        // visibly moves it back up. (Before 0.4.0 the boss healed
        // while its bar only ever fell: the bar lied.)
        {
            EncounterLedger healed = new EncounterLedger(
                    BossKind.WITHER, UUID.randomUUID(), 1000, t0);
            healed.recordDealt(a, 400, t0 + 1);
            FeedbackTotals before = FeedbackTotals.fromLedger(healed);
            eq(before.dealt(), 400, "bar: dealt before the heal");
            eq(before.remaining(), 600, "bar: remaining before the heal");
            healed.recordHealed(150);
            FeedbackTotals after = FeedbackTotals.fromLedger(healed);
            eq(after.dealt(), 250, "bar: a 150 pool heal moves dealt back to 250");
            eq(after.remaining(), 750, "bar: remaining rises by the heal");
            check(after.fraction() > before.fraction(), "bar: the fraction rises as the boss heals");
            healed.recordHealed(10_000);
            eq(FeedbackTotals.fromLedger(healed).dealt(), 0, "bar: net never drops below zero");
        }

        // --- 0.4.0 (PM-5): the verdict-pending title is a named
        // state, not a number.
        eqs(FeedbackFormat.title(FeedbackSettings.defaults().exhaustedFormat(),
                        BossKind.WITHER, 0, 0),
                "Wither — FALLING…",
                "the exhausted title renders the boss name + FALLING");
        eqs(FeedbackSettings.defaults().damageTotalFormat(),
                "{last} hit · {total} pool damage total",
                "the default action-bar format names the pool space (PM-4)");

        System.out.println("FeedbackTotalsTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
