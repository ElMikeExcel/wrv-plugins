import com.winkreyes.exceltrainingwheels.core.CompensationTable;
import com.winkreyes.exceltrainingwheels.core.RaidDivision;
import com.winkreyes.exceltrainingwheels.core.ShieldRule;
import static com.winkreyes.exceltrainingwheels.core.ShieldRule.Cause.*;

/**
 * The 0.1.2 raid-compensation pins — the SAME numbers
 * ExcelRaidBuff's compensation test pins from its side
 * (42 checks there; the composed identity must hold from both
 * directions). ExcelRaidBuff's damage table (config-driven):
 * Bad Omen I-V damage factors 1.00 / 1.08 / 1.15 / 1.22 / 1.30.
 * The protected base factor is 0.25 (Mike's absolute line).
 */
public class RaidDivisionTest {

    private static final double BASE = 0.25;
    private static final double[] OMEN_DIVISORS = {1.00, 1.08, 1.15, 1.22, 1.30};
    private static final String[] OMEN_NAMES = {"I", "II", "III", "IV", "V"};

    /**
     * The production composition: event damage in, shielded take
     * out. The EVENT side mirrors the world honestly: a melee
     * event arrives attribute-raised (vanilla x divisor); a bolt
     * or blast event arrives at vanilla — those were never
     * raised, which is the whole reason they are never divided.
     */
    private static double composedTake(double vanilla, double divisor,
                                       ShieldRule.Cause cause, double baseFactor) {
        double eventDamage = RaidDivision.isMelee(cause) ? vanilla * divisor : vanilla;
        double effective = RaidDivision.dividedFactor(baseFactor, cause, divisor);
        return ShieldRule.scaled(eventDamage, effective);
    }

    private static boolean bits(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    public static void run() {
        System.out.println("RaidDivisionTest");

        // ---- Cause gate: melee only, exactly the attribute-scaled pair. ----
        Check.ok(RaidDivision.isMelee(ENTITY_ATTACK), "ENTITY_ATTACK is melee");
        Check.ok(RaidDivision.isMelee(ENTITY_SWEEP_ATTACK), "ENTITY_SWEEP_ATTACK is melee");
        Check.ok(!RaidDivision.isMelee(PROJECTILE), "PROJECTILE is not melee (bolts never divided)");
        Check.ok(!RaidDivision.isMelee(ENTITY_EXPLOSION), "ENTITY_EXPLOSION is not melee");
        Check.ok(!RaidDivision.isMelee(OTHER), "OTHER is not melee");
        Check.ok(!RaidDivision.isMelee(null), "null cause is not melee");

        // ---- Divisor sanitation: trust finite >= 1.0, collapse the rest. ----
        Check.close(1.0, RaidDivision.sanitize(1.0), "sanitize keeps 1.0");
        Check.close(1.3, RaidDivision.sanitize(1.3), "sanitize keeps the Omen V factor 1.3");
        Check.close(2.2, RaidDivision.sanitize(2.2), "sanitize keeps a larger factor");
        Check.close(1.0, RaidDivision.sanitize(0.5), "sanitize collapses a sub-1.0 read to 1.0");
        Check.close(1.0, RaidDivision.sanitize(0.0), "sanitize collapses 0.0 to 1.0");
        Check.close(1.0, RaidDivision.sanitize(-1.3), "sanitize collapses a negative read to 1.0");
        Check.close(1.0, RaidDivision.sanitize(Double.NaN), "sanitize collapses NaN to 1.0");
        Check.close(1.0, RaidDivision.sanitize(Double.POSITIVE_INFINITY), "sanitize collapses infinity to 1.0");

        // ---- Non-melee causes are NEVER divided, whatever the read says. ----
        Check.ok(bits(0.25, RaidDivision.dividedFactor(0.25, PROJECTILE, 1.30)),
                "projectile factor undivided at divisor 1.30 (bit-exact)");
        Check.ok(bits(0.25, RaidDivision.dividedFactor(0.25, ENTITY_EXPLOSION, 1.30)),
                "explosion factor undivided at divisor 1.30 (bit-exact)");
        Check.ok(bits(0.25, RaidDivision.dividedFactor(0.25, OTHER, 1.30)),
                "OTHER factor undivided at divisor 1.30 (bit-exact)");

        // ---- THE PINS: composed take == 0.25 x vanilla melee, every omen. ----
        double[] vanillas = {3.0, 5.0, 13.0};
        for (int i = 0; i < OMEN_DIVISORS.length; i++) {
            for (double vanilla : vanillas) {
                Check.close(0.25 * vanilla,
                        composedTake(vanilla, OMEN_DIVISORS[i], ENTITY_ATTACK, BASE),
                        "Omen " + OMEN_NAMES[i] + ": vanilla melee " + vanilla
                                + " lands at exactly a quarter (" + (0.25 * vanilla) + ")");
            }
        }
        // Headline, sweep cause: a pillager's 5 lands at exactly 1.25 at every omen.
        for (int i = 0; i < OMEN_DIVISORS.length; i++) {
            Check.close(1.25, composedTake(5.0, OMEN_DIVISORS[i], ENTITY_SWEEP_ATTACK, BASE),
                    "Omen " + OMEN_NAMES[i] + ": sweep-cause 5 lands at exactly 1.25");
        }
        // Bolt pin: a 9-damage crossbow bolt takes exactly 2.25 at every omen — no division.
        for (int i = 0; i < OMEN_DIVISORS.length; i++) {
            Check.close(2.25, composedTake(9.0, OMEN_DIVISORS[i], PROJECTILE, BASE),
                    "Omen " + OMEN_NAMES[i] + ": bolt 9 lands at exactly 2.25 (undivided)");
        }
        // A creeper blast with a (meaningless) divisor present stays undivided too.
        Check.close(3.25, composedTake(13.0, 1.30, ENTITY_EXPLOSION, BASE),
                "explosion 13 lands at 3.25 even with divisor 1.30 read (undivided)");

        // ---- Elite invariance: elites read 1.0, so the elite path is BIT-IDENTICAL. ----
        CompensationTable table = CompensationTable.of(1.0, 1.5, 2.25);
        double[] bases = {0.25, 0.5};
        for (int seal = 0; seal < table.size(); seal++) {
            for (double base : bases) {
                double eff = table.effectiveFactor(base, true, seal);
                double withRaid = RaidDivision.dividedFactor(eff, ENTITY_ATTACK, 1.0);
                Check.ok(bits(eff, withRaid),
                        "elite factor bit-identical at dealt x" + table.dealtMultiplier(seal)
                                + ", base " + base);
                Check.ok(bits(ShieldRule.scaled(13.0, eff), ShieldRule.scaled(13.0, withRaid)),
                        "elite take bit-identical at dealt x" + table.dealtMultiplier(seal)
                                + ", base " + base);
            }
        }

        // ---- Absent-plugin path: divisor 1.0 is a bit-exact no-op on melee too. ----
        Check.ok(bits(0.25, RaidDivision.dividedFactor(0.25, ENTITY_ATTACK, 1.0)),
                "divisor 1.0 (RaidBuff absent) leaves the melee factor bit-exact");

        // ---- Factor validation mirrors the rest of the shield math. ----
        boolean threw = false;
        try { RaidDivision.dividedFactor(1.5, ENTITY_ATTACK, 1.3); } catch (IllegalArgumentException e) { threw = true; }
        Check.ok(threw, "factor > 1 rejected");
        threw = false;
        try { RaidDivision.dividedFactor(Double.NaN, ENTITY_ATTACK, 1.3); } catch (IllegalArgumentException e) { threw = true; }
        Check.ok(threw, "NaN factor rejected");
    }
}
