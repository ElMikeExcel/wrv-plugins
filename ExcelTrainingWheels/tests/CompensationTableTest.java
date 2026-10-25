import com.winkreyes.exceltrainingwheels.core.CompensationTable;

import java.util.HashMap;
import java.util.Map;

/**
 * Seal-compensation math (0.1.1, Mike's absolute line): for ELITE
 * damagers the effective factor is base / dealt(seal), so a
 * protected player always takes base x the mob's UNSCALED damage;
 * for non-elites the factor is the flat base at every seal.
 */
public class CompensationTableTest {
    /** The old v4 ladder, kept as a fixture for the division math (0.1.6 changed the shipped defaults). */
    static final double[] V4 = {1.50, 1.65, 1.80, 1.95, 2.10, 2.25};

    static void run() {
        System.out.println("CompensationTable:");
        // 0.1.6: shipped defaults = the Oct 25, 2026 ladder (Seal 5 is spawn-only, so it repeats 2.20).
        CompensationTable d = CompensationTable.of(CompensationTable.DEFAULT_DEALT);
        double[] want = {1.60, 1.75, 1.90, 2.05, 2.20, 2.20};
        for (int i = 0; i < want.length; i++) Check.close(want[i], d.dealtMultiplier(i), "0.1.6 default dealt at seal " + i + " = " + want[i]);
        Check.close(0.25 / 2.20, d.eliteFactor(0.25, 5), "0.1.6 default: base 0.25 at seal 5 = 0.25 / 2.20");
        CompensationTable t = CompensationTable.of(V4);

        // The v4 fixture, read back.
        Check.close(1.50, t.dealtMultiplier(0), "dealt at seal 0 = 1.50");
        Check.close(1.65, t.dealtMultiplier(1), "dealt at seal 1 = 1.65");
        Check.close(1.80, t.dealtMultiplier(2), "dealt at seal 2 = 1.80");
        Check.close(1.95, t.dealtMultiplier(3), "dealt at seal 3 = 1.95");
        Check.close(2.10, t.dealtMultiplier(4), "dealt at seal 4 = 2.10");
        Check.close(2.25, t.dealtMultiplier(5), "dealt at seal 5 = 2.25");
        Check.eq(6, t.size(), "table has 6 rows (seals 0..5)");

        // Mike's numbers: base 0.5 at every seal (brief's table).
        Check.close(0.5 / 1.50, t.eliteFactor(0.5, 0), "elite factor seal 0 = 0.3333");
        Check.close(0.5 / 1.65, t.eliteFactor(0.5, 1), "elite factor seal 1 = 0.3030");
        Check.close(0.5 / 1.80, t.eliteFactor(0.5, 2), "elite factor seal 2 = 0.2778");
        Check.close(0.5 / 1.95, t.eliteFactor(0.5, 3), "elite factor seal 3 = 0.2564");
        Check.close(0.5 / 2.10, t.eliteFactor(0.5, 4), "elite factor seal 4 = 0.2381");
        Check.close(0.5 / 2.25, t.eliteFactor(0.5, 5), "elite factor seal 5 = 0.2222");

        // The absolute line itself: scaled hit x elite factor == base x unscaled,
        // for an unscaled hit of 4.5 (Hard zombie) at every seal.
        for (int seal = 0; seal <= 5; seal++) {
            double scaledHit = 4.5 * t.dealtMultiplier(seal);
            Check.close(4.5 * 0.5, scaledHit * t.eliteFactor(0.5, seal),
                    "absolute line holds at seal " + seal);
        }

        // Non-elites: flat base factor at every seal.
        for (int seal = 0; seal <= 5; seal++) {
            Check.close(0.5, t.effectiveFactor(0.5, false, seal),
                    "non-elite factor flat at seal " + seal);
            Check.close(t.eliteFactor(0.5, seal), t.effectiveFactor(0.5, true, seal),
                    "effectiveFactor(elite) matches eliteFactor at seal " + seal);
        }

        // Per-player bases ride the same division.
        Check.close(0.75 / 2.25, t.eliteFactor(0.75, 5), "base 0.75 at seal 5 = 0.3333");
        Check.close(1.0 / 1.50, t.eliteFactor(1.0, 0), "base 1.0 at seal 0 = 0.6667 (listed, graduated: never above normal)");
        Check.close(0.0, t.eliteFactor(0.0, 3), "base 0.0 stays 0.0");
        Check.close(0.25 / 1.95, t.eliteFactor(0.25, 3), "base 0.25 at seal 3");

        // Seal counts clamp to the table instead of blowing up.
        Check.close(2.25, t.dealtMultiplier(6), "seal 6 clamps to the last row");
        Check.close(2.25, t.dealtMultiplier(99), "seal 99 clamps to the last row");
        Check.close(1.50, t.dealtMultiplier(-1), "seal -1 clamps to row 0");

        // A dealt multiplier of exactly 1.0 leaves elites at the base factor.
        CompensationTable flat = CompensationTable.of(1.0, 1.0);
        Check.close(0.5, flat.eliteFactor(0.5, 1), "dealt 1.0 -> elite factor = base");

        // Invalid bases are rejected like ShieldRule rejects them.
        Check.ok(throwsIAE(() -> t.eliteFactor(1.5, 0)), "eliteFactor rejects base > 1");
        Check.ok(throwsIAE(() -> t.eliteFactor(-0.1, 0)), "eliteFactor rejects base < 0");
        Check.ok(throwsIAE(() -> t.effectiveFactor(Double.NaN, true, 0)), "effectiveFactor rejects NaN base");
        Check.ok(throwsIAE(() -> CompensationTable.of(0.5)), "of() rejects a multiplier below 1.0");
        Check.ok(throwsIAE(() -> CompensationTable.of(15.0)), "of() rejects a multiplier above 5.0");
        Check.ok(throwsIAE(() -> CompensationTable.of()), "of() rejects an empty table");

        // Raw parsing: good rows land, bad rows fall back + report.
        Map<String, String> raw = new HashMap<>();
        raw.put("0", "1.50");
        raw.put("3", "2.00");            // a deliberate config edit
        raw.put("2", "not-a-number");    // junk -> default + error
        raw.put("4", "99");              // out of range -> default + error
        raw.put("5", "0.25");            // below 1.0 -> default + error
        raw.put("9", "3.00");            // outside the table -> ignored + error
        raw.put("seal", "1.5");          // non-numeric key -> ignored + error
        CompensationTable.ParseResult parsed = CompensationTable.fromRaw(raw, V4);
        CompensationTable pt = parsed.table();
        Check.close(1.50, pt.dealtMultiplier(0), "parse: row 0 kept");
        Check.close(1.65, pt.dealtMultiplier(1), "parse: missing row 1 = default");
        Check.close(1.80, pt.dealtMultiplier(2), "parse: junk row 2 = default");
        Check.close(2.00, pt.dealtMultiplier(3), "parse: edited row 3 lands");
        Check.close(2.10, pt.dealtMultiplier(4), "parse: out-of-range row 4 = default");
        Check.close(2.25, pt.dealtMultiplier(5), "parse: below-range row 5 = default");
        Check.eq(5, parsed.errors().size(), "parse: 5 bad rows reported (2, 4, 5, 9, 'seal')");

        // Null/empty raw = pure defaults, no errors.
        CompensationTable.ParseResult clean = CompensationTable.fromRaw(null, V4);
        Check.eq(0, clean.errors().size(), "parse: null raw -> no errors");
        Check.close(1.95, clean.table().dealtMultiplier(3), "parse: null raw -> defaults intact");
        CompensationTable.ParseResult empty = CompensationTable.fromRaw(Map.of(), V4);
        Check.eq(0, empty.errors().size(), "parse: empty raw -> no errors");

        // Multiplier validation bounds.
        Check.ok(CompensationTable.validMultiplier(1.0), "multiplier 1.0 valid (lower bound)");
        Check.ok(CompensationTable.validMultiplier(5.0), "multiplier 5.0 valid (upper bound)");
        Check.ok(!CompensationTable.validMultiplier(0.99), "multiplier 0.99 invalid");
        Check.ok(!CompensationTable.validMultiplier(5.01), "multiplier 5.01 invalid");
        Check.ok(!CompensationTable.validMultiplier(Double.NaN), "multiplier NaN invalid");
    }

    static boolean throwsIAE(Runnable r) {
        try { r.run(); return false; }
        catch (IllegalArgumentException e) { return true; }
    }
}

// ExcelTrainingWheels — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
