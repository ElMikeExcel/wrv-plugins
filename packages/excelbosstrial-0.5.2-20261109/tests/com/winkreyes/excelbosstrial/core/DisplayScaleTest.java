/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;

/**
 * The display scale (Packet B, Item 35 - seal-fight DESIGN
 * section 0.4 as amended Nov 8, 2026): ONE number space (pool
 * space) rendered at a uniform x1000, so the vanilla gear
 * ladder survives every chapter's intake factor AFTER the
 * display's own rounding. The checks run the design's V-1
 * method at desk scale for every governed intake factor in
 * the settled slate - wither ~0.0468 (the worst on record),
 * elder quad ~0.0599 (the tightest in this pass), dragon
 * phase 1 0.256 and phase 2 0.064 - plus the amendment's own
 * worked examples, word for word.
 */
public final class DisplayScaleTest {

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

    private static void eq(double actual, double expected, double tol, String label) {
        check(Math.abs(actual - expected) <= tol,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    /** The action-bar rendering of one hit at a factor+scale:
     *  the exact string path FeedbackDisplay walks. */
    private static double renderedHit(double raw, double factor, double scale) {
        String line = FeedbackFormat.actionBar(
                FeedbackSettings.DEFAULT_DAMAGE_TOTAL_FORMAT,
                DisplayScale.displayedHit(scale, raw, factor),
                DisplayScale.displayedHit(scale, raw, factor));
        // "{last} hit · {total} pool damage total" - parse {last}.
        return Double.parseDouble(line.substring(0, line.indexOf(' ')));
    }

    private static void ladder(double factor, String chapter) {
        double scale = DisplayScale.DEFAULT_SCALE;
        double wood = renderedHit(4.0, factor, scale);
        double diamond = renderedHit(7.0, factor, scale);
        double netherite = renderedHit(8.0, factor, scale);
        check(wood < diamond && diamond < netherite,
                chapter + ": ladder ordered after display rounding ("
                        + wood + " < " + diamond + " < " + netherite + ")");
        check(DisplayScale.ladderVisible(wood, diamond, netherite, 0.06),
                chapter + ": ladder ratios recognizable after rounding ("
                        + wood + "/" + diamond + "/" + netherite + ")");
        // And at the SMALL factors (wither / elder / dragon
        // phase 2 - the chapters where the complaint was lived)
        // the UNSCALED display huddles under 1: the failure this
        // packet exists to retire, pinned so the contrast can
        // never quietly invert. (Dragon phase 1's 0.256 was
        // never the failing case - its unscaled tiers already
        // clear 1 - and the scaled check above still binds it.)
        if (factor < 0.1) {
            double w0 = renderedHit(4.0, factor, 1.0);
            double d0 = renderedHit(7.0, factor, 1.0);
            double n0 = renderedHit(8.0, factor, 1.0);
            check(w0 < 1.0 && n0 < 1.0,
                    chapter + ": unscaled tiers huddle under 1 (" + w0 + "/" + d0 + "/" + n0 + ")");
        }
    }

    public static void main(String[] args) {
        // --- Sanitization: junk reads as the settled default.
        eq(DisplayScale.sanitized(1000.0), 1000.0, 0, "scale: 1000 passes through");
        eq(DisplayScale.sanitized(Double.NaN), 1000.0, 0, "scale: NaN reads as default");
        eq(DisplayScale.sanitized(-5), 1000.0, 0, "scale: negative reads as default");
        eq(DisplayScale.sanitized(0), 1000.0, 0, "scale: zero reads as default");
        eq(DisplayScale.sanitized(Double.POSITIVE_INFINITY), 1000.0, 0,
                "scale: infinity reads as default");
        eq(DisplayScale.sanitized(0.5), 1.0, 0, "scale: sub-1 clamps to the floor 1");
        eq(DisplayScale.sanitized(1e9), 100_000.0, 0, "scale: huge clamps to the ceiling");
        eq(DisplayScale.apply(1000, Double.NaN), 0, 0, "apply: NaN value degrades to 0");
        eq(DisplayScale.apply(1000, 21900), 21_900_000.0, 0,
                "apply: the Wither pool renders 21,900,000 (the amendment's figure)");

        // --- The amendment's worked examples (Wither, f=0.0468),
        // rendered as integers the way the boss bar groups them.
        double f = 0.0468;
        check(Math.round(DisplayScale.displayedHit(1000, 4.0, f)) == 187,
                "amendment: wooden 187");
        check(Math.round(DisplayScale.displayedHit(1000, 7.0, f)) == 328,
                "amendment: diamond 328");
        check(Math.round(DisplayScale.displayedHit(1000, 8.0, f)) == 374,
                "amendment: netherite 374");
        check(Math.round(DisplayScale.displayedHit(1000, 11.0, f)) == 515,
                "amendment: netherite + Sharp V 515");
        check(Math.round(DisplayScale.displayedHit(1000, 16.5, f)) == 772,
                "amendment: crit 772");
        check(Math.round(DisplayScale.displayedHit(1000, 10.0, f)) == 468,
                "amendment: Missile-scale hit 468");

        // --- The ladder in EVERY governed chapter of the slate.
        ladder(0.0468, "wither (f 0.0468)");
        ladder(0.0599, "elder quad (f 0.0599)");
        ladder(1024.0 / 4000.0, "dragon phase 1 (f 0.256)");
        ladder(1024.0 / 16000.0, "dragon phase 2 (f 0.064)");
        ladder(1024.0 / 17100.0, "elder quad exact (1024/17100)");

        // --- FeedbackSettings carries the scale.
        eq(FeedbackSettings.defaults().displayScale(), 1000.0, 0,
                "settings: default scale is the settled 1000");
        eq(FeedbackSettings.sanitized(true, "actionbar", "x", "x", 64, 0, 1000,
                "x", 250.0).displayScale(), 250.0, 0,
                "settings: a configured scale survives sanitization");
        eq(FeedbackSettings.sanitized(true, "actionbar", "x", "x", 64, 0, 1000,
                "x", Double.NaN).displayScale(), 1000.0, 0,
                "settings: a NaN scale reads as the default");
        FeedbackSettings custom = FeedbackSettings.sanitized(true, "actionbar",
                "x", "x", 64, 0, 1000, "x", 250.0);
        eq(custom.withOverrides(null, null).displayScale(), 250.0, 0,
                "settings: chapter overrides preserve the scale");
        // The 0.4.0 sanitized form (no scale argument) defaults.
        eq(FeedbackSettings.sanitized(true, "actionbar", "x", "x", 64, 0, 1000)
                .displayScale(), 1000.0, 0,
                "settings: the pre-Packet-B sanitized form defaults the scale");

        // --- The title path: pool figures scale together.
        FeedbackTotals view = FeedbackTotals.of(21900, 2469);
        String title = com.winkreyes.excelbosstrial.core.FeedbackFormat.title(
                FeedbackSettings.DEFAULT_TITLE_FORMAT, BossKind.WITHER,
                DisplayScale.apply(1000, view.remaining()),
                DisplayScale.apply(1000, view.pool()));
        check(title.equals("Wither — 19,431,000 / 21,900,000"),
                "title: scaled pool figures group correctly (got \"" + title + "\")");
        check(Math.abs(view.fraction() - (21900 - 2469) / 21900.0) < 1e-9,
                "title: the bar fraction itself is NEVER scaled");

        // --- ladderVisible rejects the collapsed shapes.
        check(!DisplayScale.ladderVisible(0.2, 0.4, 0.5, 0.06),
                "ladderVisible: the sub-1 huddle fails on ratios (0.2/0.4/0.5)");
        check(!DisplayScale.ladderVisible(187, 187, 374, 0.06),
                "ladderVisible: a collapsed middle tier fails");
        check(!DisplayScale.ladderVisible(374, 328, 187, 0.06),
                "ladderVisible: an inverted ladder fails");
        check(DisplayScale.ladderVisible(187.2, 327.6, 374.4, 0.01),
                "ladderVisible: the true wither renders pass at 1% tolerance");

        System.out.println("DisplayScaleTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
        // Silence the unused-import style checkers: List is used
        // by sibling harnesses' conventions only.
        List.of();
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
