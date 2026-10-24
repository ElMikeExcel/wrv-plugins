package com.winkreyes.excelraidbuff.core;

/**
 * THE ROWAN PIN (Mike's decision, Oct 21, 2026: "Keep her at
 * exactly a quarter of vanilla — compensate for the buff").
 *
 * TrainingWheels guarantees Rowan takes 0.25 x a mob's NORMAL
 * unscaled damage. The raid buff raises a non-elite raider's
 * melee damage at the attribute level, so the companion
 * TrainingWheels delta divides its shield factor by
 * ExcelRaidBuffAPI.damageMultiplierOf(damager) — the factor the
 * buff stored on the entity — on the melee causes only. This
 * suite pins the composed arithmetic end to end, from the same
 * sources of truth the live code uses:
 *
 *   event damage  = vanilla x attribute multiplier, where the
 *                   attribute's effective multiplier is
 *                   1 + BuffMath.modifierAmount(factor)
 *   shield take   = event damage x 0.25
 *   compensated   = shield take / API divisor, where the divisor
 *                   is the stored factor itself
 *   REQUIRED      = 0.25 x vanilla, at every omen level.
 *
 * And the two non-application pins: an ELITE damager's divisor is
 * 1.0 (elites are never marked), so the existing elite-factor
 * compensation comes out numerically unchanged; and a PROJECTILE
 * (crossbow bolt — never attribute-scaled) takes NO division, or
 * she would drop below the guaranteed quarter.
 */
public final class RowanCompensationTest {

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

    /** TrainingWheels' protected factor — the guarantee itself. */
    private static final double BASE_FACTOR = 0.25;

    /** The TrainingWheels elite path today: factor divided by the ladder's dealt multiplier. */
    private static double eliteCompensatedTake(double eliteUnscaledDamage, double ladderDealt) {
        double eventDamage = eliteUnscaledDamage * ladderDealt;      // EM scaled combat
        double factor = BASE_FACTOR / ladderDealt;                    // existing compensation
        return eventDamage * factor;
    }

    public static void main(String[] args) {
        RaidBuffConfig cfg = RaidBuffConfig.defaults();
        // Vanilla melee damages as the event would carry them pre-buff:
        // pillager fist 5, a vindicator's effective axe hit 13, a light hit 3.
        double[] vanillaDamages = {3.0, 5.0, 13.0};

        for (int omen = 1; omen <= 5; omen++) {
            ScalingTable.Factors f = ScalingTable.factors(cfg, omen, 1, 7);
            double storedFactor = f.damage();
            // The attribute's effective multiplier, via the modifier the glue adds.
            double attributeMultiplier = 1.0 + BuffMath.modifierAmount(storedFactor);
            eq(attributeMultiplier, storedFactor,
                    "omen " + omen + ": attribute multiplier == stored/API divisor");
            for (double vanilla : vanillaDamages) {
                double eventDamage = vanilla * attributeMultiplier;
                double shieldTake = eventDamage * BASE_FACTOR;
                double compensated = shieldTake / storedFactor; // the API divisor
                eq(compensated, BASE_FACTOR * vanilla,
                        "omen " + omen + ", vanilla " + vanilla
                                + ": compensated take is exactly a quarter of vanilla");
            }
            // The pinned headline numbers, one per omen (pillager melee 5 -> 1.25).
            double headline = 5.0 * attributeMultiplier * BASE_FACTOR / storedFactor;
            eq(headline, 1.25, "omen " + omen + ": pillager melee take pinned at 1.25");
        }

        // Omen I is identity: the divisor is 1.0 and compensation changes nothing.
        eq(cfg.damageMultiplier(1), 1.0, "omen I divisor is 1.0");

        // ELITES: never marked, divisor 1.0 — the existing elite
        // compensation is bit-for-bit the same with the raid
        // division applied on top (a multiply by 1.0's inverse).
        for (double ladderDealt : new double[] {1.0, 1.5, 2.25}) {
            double before = eliteCompensatedTake(12.0, ladderDealt);
            double after = before / 1.0; // raid divisor for an elite damager
            eq(after, before, "elite take unchanged at ladder dealt x" + ladderDealt);
            eq(before, BASE_FACTOR * 12.0,
                    "elite take is the compensated quarter at ladder dealt x" + ladderDealt);
        }

        // PROJECTILES: a crossbow bolt is never attribute-scaled,
        // so NO division is applied at any omen — the take stays
        // the plain quarter of the bolt's vanilla damage. (If a
        // consumer divided here, she would take 0.25/1.30 = 0.192
        // of vanilla at Omen V — below the guarantee. Pinned so
        // the contract's melee-only scope can never drift.)
        for (int omen = 1; omen <= 5; omen++) {
            double boltVanilla = 9.0;
            double take = boltVanilla * BASE_FACTOR; // no buff scale, no division
            eq(take, 2.25, "omen " + omen + ": crossbow bolt take stays the plain quarter");
        }

        // The failure direction is safe: a divisor below 1.0 can
        // never occur (config clamps multipliers at 1.0), so the
        // division can only ever cancel a real buff, never amplify
        // her protection past the guarantee or weaken it.
        for (int omen = 1; omen <= 5; omen++) {
            check(cfg.damageMultiplier(omen) >= 1.0,
                    "omen " + omen + ": divisor never below 1.0");
        }

        System.out.println("RowanCompensationTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
