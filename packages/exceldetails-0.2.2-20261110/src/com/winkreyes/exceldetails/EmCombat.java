package com.winkreyes.exceldetails;

/**
 * 0.2.2 EliteMobs prep — the pure half (unit-tested, no Bukkit).
 *
 * Mike's rule (Nov 9, 2026): the meter must be prepared for the
 * EliteMobs world — EM's simulated-damage numbers must not
 * render as meaningless millions (EM audit T4). DESIGN.md §2
 * carries the full evidence from the EliteMobs 10.9.12 jar;
 * the decision is SEPARATELY BUCKET: damage done to an
 * EM-tracked elite, and any event whose amount is beyond every
 * real HP pool on the server, accumulate in the segment's EM
 * bucket — never ranked, never rendered, never merged into the
 * overall (true-HP) totals.
 *
 * This class owns only the routing arithmetic; the elite
 * detection seam (EntityTracker.isEliteMob, reflection) lives
 * in EliteLookup and fails soft to "not elite" — this class's
 * amount floor is the seam-independent second layer.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class EmCombat {

    /**
     * The simulated-amount floor. The largest real HP pool on
     * this server is 2048 (EliteMobs raises the max-health
     * attribute cap to 2048 for its normalized bosses; the
     * platform cap is 1024) and real single-event overkill is
     * bounded by real weapon damage — no real event reaches
     * 100,000. EM's simulated numbers, by its own config's
     * warning, reach the millions. The floor sits ~50x above
     * the largest possible real pool and far below the
     * simulated range.
     */
    public static final double SIMULATED_FLOOR = 100_000.0;

    /** Where one damage event belongs. */
    public enum Route { NORMAL, EM_BUCKET }

    private EmCombat() {}

    /** True when an amount cannot be real HP on this server. */
    public static boolean isSimulatedAmount(double amount) {
        return !Double.isNaN(amount) && amount > SIMULATED_FLOOR;
    }

    /**
     * Damage-DONE routing: an elite victim (EM's simulated
     * number space) or a simulated-sized amount goes to the
     * bucket; everything else is a normal meter event.
     */
    public static Route routeDamageDone(double amount, boolean victimIsElite) {
        if (victimIsElite || isSimulatedAmount(amount)) return Route.EM_BUCKET;
        return Route.NORMAL;
    }

    /**
     * Damage-TAKEN routing: elite→player damage is real HP (a
     * player cannot lose simulated HP), so only a
     * simulated-sized amount — impossible as real HP loss —
     * buckets. The damager being an elite does NOT bucket
     * taken; Damage Taken stays a live metric in EM content.
     */
    public static Route routeDamageTaken(double amount, boolean damagerIsElite) {
        if (isSimulatedAmount(amount)) return Route.EM_BUCKET;
        return Route.NORMAL;
    }
}
