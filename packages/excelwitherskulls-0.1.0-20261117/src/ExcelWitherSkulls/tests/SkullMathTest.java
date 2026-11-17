import com.winkreyes.excelwitherskulls.core.SkullMath;

/**
 * The probability math, pinned deterministically: every number
 * here is the audit's extracted vanilla table or arithmetic on it.
 * No randomness anywhere in this suite.
 */
public final class SkullMathTest {

    public static void run() {
        System.out.println("SkullMathTest");

        // Vanilla's own chances (26.2 loot table: 2.5% + 1%/Looting).
        Check.close(2.5, SkullMath.vanillaChancePercent(0), "vanilla chance, no Looting = 2.5");
        Check.close(3.5, SkullMath.vanillaChancePercent(1), "vanilla chance, Looting I = 3.5");
        Check.close(4.5, SkullMath.vanillaChancePercent(2), "vanilla chance, Looting II = 4.5");
        Check.close(5.5, SkullMath.vanillaChancePercent(3), "vanilla chance, Looting III = 5.5");
        Check.close(2.5, SkullMath.vanillaChancePercent(-1), "negative Looting clamps to base");

        // The top-up is exactly the difference toward the target.
        Check.close(22.5, SkullMath.topUpChancePercent(25.0, 0), "top-up at 25 target, no Looting = 22.5");
        Check.close(21.5, SkullMath.topUpChancePercent(25.0, 1), "top-up at 25 target, Looting I = 21.5");
        Check.close(20.5, SkullMath.topUpChancePercent(25.0, 2), "top-up at 25 target, Looting II = 20.5");
        Check.close(19.5, SkullMath.topUpChancePercent(25.0, 3), "top-up at 25 target, Looting III = 19.5");

        // Only ever adds: target at/below vanilla's chance = no roll.
        Check.close(0.0, SkullMath.topUpChancePercent(3.0, 3), "target 3 < Looting III 5.5 -> 0");
        Check.close(0.0, SkullMath.topUpChancePercent(2.5, 0), "target == vanilla base -> 0");
        Check.close(0.0, SkullMath.topUpChancePercent(0.0, 0), "target 0 -> 0");
        Check.close(0.0, SkullMath.topUpChancePercent(-10.0, 0), "negative target clamps -> 0");

        // Extremes.
        Check.close(97.5, SkullMath.topUpChancePercent(100.0, 0), "target 100, no Looting = 97.5");
        Check.close(97.5, SkullMath.topUpChancePercent(250.0, 0), "target 250 clamps to 100 first");
        Check.close(0.0, SkullMath.topUpChancePercent(Double.NaN, 0), "NaN target -> 0 (no roll)");

        // The realized total: v + (1 - v) x top-up, stated honestly.
        Check.close(24.4375, SkullMath.realizedTotalPercent(25.0, 0), "realized total, 25 target, no Looting = 24.4375");
        Check.close(23.9275, SkullMath.realizedTotalPercent(25.0, 3), "realized total, 25 target, Looting III = 23.9275");
        Check.close(97.5625, SkullMath.realizedTotalPercent(100.0, 0),
                "realized total at target 100 = 97.5625 (the difference roll can never quite reach 100)");
        Check.close(2.5, SkullMath.realizedTotalPercent(2.5, 0), "realized total at target == vanilla = vanilla");

        // Roll boundaries: roll < chance succeeds; equality misses.
        Check.ok(SkullMath.rollSucceeds(22.5, 0.0), "roll 0 always succeeds on a real chance");
        Check.ok(SkullMath.rollSucceeds(22.5, 22.4999), "roll just under chance succeeds");
        Check.ok(!SkullMath.rollSucceeds(22.5, 22.5), "roll == chance misses");
        Check.ok(!SkullMath.rollSucceeds(22.5, 99.999), "roll 99.999 misses a 22.5 chance");
        Check.ok(!SkullMath.rollSucceeds(0.0, 0.0), "zero chance never succeeds");
    }
}
