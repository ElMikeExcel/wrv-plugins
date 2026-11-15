/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The feel patch's text rules (0.3.6): the boss bar title's true
 * pool numbers and the action bar's damage line. The numbers a
 * player reads must be the ledger's numbers - grouped, rounded
 * at display only, never accumulated from rounded parts.
 */
public final class FeedbackFormatTest {

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

    private static void eq(String actual, String expected, String label) {
        check(expected.equals(actual),
                label + " (expected \"" + expected + "\", got \"" + actual + "\")");
    }

    public static void main(String[] args) {
        // --- The headline case (FEEL-AUDIT section 1): duo elder,
        // 2,469 dealt of the 10,900 pool.
        FeedbackTotals duo = FeedbackTotals.of(10_900, 2_469);
        eq(duo.title(FeedbackSettings.DEFAULT_TITLE_FORMAT, BossKind.ELDER_GUARDIAN),
                "Elder Guardian — 8,431 / 10,900",
                "duo elder title after 2,469 dealt (grouping on both numbers)");

        // --- Chapter short names (displayName carries articles
        // that read wrong mid-title).
        eq(FeedbackFormat.shortName(BossKind.WITHER), "Wither", "short name: wither");
        eq(FeedbackFormat.shortName(BossKind.ELDER_GUARDIAN), "Elder Guardian",
                "short name: elder guardian");
        eq(FeedbackFormat.shortName(BossKind.WARDEN), "Warden", "short name: warden");
        eq(FeedbackFormat.shortName(BossKind.ENDER_DRAGON), "Ender Dragon",
                "short name: ender dragon");

        // --- Remaining clamps at 0 on an overkill hit.
        FeedbackTotals over = FeedbackTotals.of(100, 150);
        check(over.remaining() == 0.0, "overkill: remaining clamps to 0");
        check(over.fraction() == 0.0, "overkill: fraction clamps to 0");
        eq(over.title(FeedbackSettings.DEFAULT_TITLE_FORMAT, BossKind.WITHER),
                "Wither — 0 / 100", "overkill title never prints a negative pool");

        // --- Rounding is display-only: two 16.5 crits move the
        // title by exactly 33 (10900 - 33 = 10867), never 32/34
        // of accumulated per-hit rounding.
        FeedbackTotals crits = FeedbackTotals.of(10_900, 16.5 + 16.5);
        check(crits.remaining() == 10_867.0, "two crits: true remaining is 10,867");
        eq(FeedbackFormat.grouped(crits.remaining()), "10,867",
                "two crits: title reads 10,867 (no rounding drift)");
        eq(FeedbackFormat.grouped(8_431.4), "8,431", "grouping rounds .4 down");
        eq(FeedbackFormat.grouped(8_431.5), "8,432", "grouping rounds .5 up");

        // --- Rally growth: the title's total follows the grown pool.
        FeedbackTotals grown = FeedbackTotals.of(14_000, 2_469);
        eq(grown.title(FeedbackSettings.DEFAULT_TITLE_FORMAT, BossKind.ELDER_GUARDIAN),
                "Elder Guardian — 11,531 / 14,000",
                "rally-grown pool: title total follows the growth");

        // --- Custom formats: reordered placeholders render;
        // unknown placeholders pass through literally; a null or
        // blank format falls back to the settled default.
        eq(FeedbackFormat.title("{total} - {remaining} - {name}",
                        BossKind.WARDEN, 500, 7_200),
                "7,200 - 500 - Warden", "custom title format with reordered placeholders");
        eq(FeedbackFormat.title("{name} {bogus}", BossKind.WITHER, 1, 2),
                "Wither {bogus}", "unknown placeholder passes through literally");
        eq(FeedbackFormat.title(null, BossKind.WITHER, 1, 2),
                "Wither — 1 / 2", "null title format falls back to the default");
        eq(FeedbackFormat.title("  ", BossKind.WITHER, 1, 2),
                "Wither — 1 / 2", "blank title format falls back to the default");

        // --- The action-bar line: one decimal, so fractional
        // spell hits visibly accumulate in the running total.
        eq(FeedbackFormat.actionBar(FeedbackSettings.DEFAULT_DAMAGE_TOTAL_FORMAT, 4.0, 4.0),
                "4.0 hit · 4.0 pool damage total", "wood sword: 4.0 / 4.0");
        eq(FeedbackFormat.actionBar(FeedbackSettings.DEFAULT_DAMAGE_TOTAL_FORMAT, 16.5, 31.5),
                "16.5 hit · 31.5 pool damage total", "crit renders 16.5");
        eq(FeedbackFormat.actionBar(FeedbackSettings.DEFAULT_DAMAGE_TOTAL_FORMAT, 0.47, 0.47),
                "0.5 hit · 0.5 pool damage total", "a 0.47 spell hit renders 0.5 at 1dp");
        double spellTotal = 0;
        for (int i = 0; i < 10; i++) {
            spellTotal += 0.47;
        }
        eq(FeedbackFormat.actionBar(FeedbackSettings.DEFAULT_DAMAGE_TOTAL_FORMAT, 0.47, spellTotal),
                "0.5 hit · 4.7 pool damage total",
                "ten 0.47 hits: the total keeps full precision and renders 4.7");
        eq(FeedbackFormat.actionBar(null, 23.75, 23.75),
                "23.8 hit · 23.8 pool damage total", "Power V 23.75 renders 23.8");

        // --- Junk degrades to 0, never "NaN" on a player's screen.
        eq(FeedbackFormat.grouped(Double.NaN), "0", "NaN groups to 0");
        eq(FeedbackFormat.oneDecimal(Double.NaN), "0.0", "NaN renders 0.0");
        FeedbackTotals nan = FeedbackTotals.of(10_900, Double.NaN);
        eq(nan.title(FeedbackSettings.DEFAULT_TITLE_FORMAT, BossKind.ELDER_GUARDIAN),
                "Elder Guardian — 10,900 / 10,900", "NaN dealt reads as no damage");

        System.out.println("FeedbackFormatTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
