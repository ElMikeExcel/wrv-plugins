package excelemfixes.partydrops;

/**
 * The party multiplier and the fractional-drop rounding
 * (PARTY-DROPS-SPEC-2026-10-20 §Shape and §4).
 *
 * Multiplier: {@code 1 + stepPerExtra x (participants - 1)}, capped
 * at {@code multiplierCap}; 0 or 1 participants is exactly 1x — a
 * lone hunter's world does not change by one item.
 *
 * Rounding, per drop stack, independently:
 *   total      = amount x multiplier
 *   guaranteed = floor(total)
 *   extra      = 1 more item with probability (total - guaranteed)
 * Expected value per stack is exactly amount x multiplier — no
 * systematic over- or under-pay. (Deterministic round-half-up was
 * rejected: it turns every 1-item drop at 1.5x into 2 items, a 2x
 * feature wearing a 1.5x label. Always-floor silently pays 1x.)
 * When the fractional part is zero the random source is never drawn
 * from, so exact-integer cases (multiplier 1.0, 2.0, 3.0 on any
 * amount) are byte-exact and consume zero draws — the unit tests
 * assert this.
 *
 * Pure math, no Bukkit types, so it is unit tested on the plain JVM.
 */
public final class MultiplierMath {

    /** Injectable randomness so tests can seed it and count draws. */
    public interface RandomSource {
        double nextDouble();
    }

    private static final double EPS = 1e-9;

    private MultiplierMath() {
    }

    /** The multiplier for a participant count. Never below 1.0. */
    public static double multiplier(int participants, double stepPerExtra, double multiplierCap) {
        if (participants <= 1) return 1.0;
        double step = (Double.isFinite(stepPerExtra) && stepPerExtra >= 0.0) ? stepPerExtra : 0.5;
        double cap = (Double.isFinite(multiplierCap) && multiplierCap >= 1.0) ? multiplierCap : 3.0;
        double m = 1.0 + step * (participants - 1);
        return Math.min(m, cap);
    }

    /**
     * Scales one stack amount by the multiplier with probabilistic
     * rounding. Amounts at or below zero pass through untouched and
     * consume no draws.
     */
    public static int scaledAmount(int amount, double multiplier, RandomSource rng) {
        if (amount <= 0) return amount;
        double total = amount * multiplier;
        double floorD = Math.floor(total);
        long guaranteed = (long) floorD;
        double frac = total - floorD;
        long result;
        if (frac <= EPS) {
            result = guaranteed;
        } else if (frac >= 1.0 - EPS) {
            result = guaranteed + 1;
        } else {
            result = guaranteed + (rng.nextDouble() < frac ? 1 : 0);
        }
        if (result > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) result;
    }
}

// ExcelEMFixes party-drops module — ported from ExcelPartyDrops 0.1.0 (Wave 3, D245).
