/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.Random;

/**
 * The elite-sprinkle roll (ExcelBossTrial 0.3.3, study section
 * 3). Pure logic, no Bukkit - the adapter (SprinkleService)
 * supplies the live counts and acts on the decision.
 *
 * The rules, in order:
 * <ol>
 *   <li><b>Caps beat chance.</b> When the fight's spawned total
 *       has reached max-per-fight, later stamps DO NOT ROLL AT
 *       ALL (the decision says so, and the adapter logs "cap
 *       reached"): the sprinkle budget is spent and the later
 *       stamps' better odds are moot.</li>
 *   <li>Otherwise roll once: pass when the roll lands under the
 *       effective chance = base chance x stamp multiplier,
 *       clamped to the ceiling.</li>
 *   <li>A passed roll spawns up to the room available - the
 *       smaller of (max-alive - alive now) and (max-per-fight -
 *       spawned so far). A partial wave is normal. If there is
 *       no room, the passed roll is SPENT, never banked: banking
 *       would quietly turn "chance per stamp" into "guaranteed,
 *       eventually", which the design forbids.</li>
 * </ol>
 */
public final class SprinkleRoller {

    private SprinkleRoller() {
    }

    /**
     * The outcome of one stamp.
     *
     * @param rolled          false when the per-fight cap kept
     *                        the stamp from rolling at all
     * @param passed          the roll landed under the chance
     * @param roll            the value rolled (NaN when not rolled)
     * @param effectiveChance base x stamp multiplier, ceiling-clamped
     * @param spawnCount      elites to spawn (0 on a fail, and 0
     *                        on a pass with no room - spent, not banked)
     */
    public record Decision(boolean rolled, boolean passed, double roll,
                           double effectiveChance, int spawnCount) {
    }

    /** base x stamp multiplier, clamped to [0, ceiling]. */
    public static double effectiveChance(double baseChance, double stampMultiplier,
                                         double ceiling) {
        double raw = baseChance * stampMultiplier;
        if (Double.isNaN(raw) || raw < 0.0) {
            return 0.0;
        }
        return Math.min(raw, Math.max(0.0, ceiling));
    }

    /** The multiplier for a stamp bit; a short or malformed list
     *  falls back to the settled [0.75, 1.0, 1.25] at that index. */
    public static double stampMultiplier(List<Double> multipliers, int stampIndex) {
        int i = Math.max(0, Math.min(2, stampIndex));
        if (multipliers != null && multipliers.size() > i) {
            Double m = multipliers.get(i);
            if (m != null && !m.isNaN() && !m.isInfinite() && m >= 0.0) {
                return m;
            }
        }
        return SprinkleSettings.DEFAULT_STAMP_MULTIPLIERS.get(i);
    }

    /**
     * Decide one stamp.
     *
     * @param rng             roll source (nextDouble in [0, 1))
     * @param baseChance      the chapter's sprinkle.chance
     * @param stampMultiplier this stamp's multiplier
     * @param ceiling         the effective-chance ceiling
     * @param waveCount       the chapter's count per passed roll
     * @param aliveNow        this encounter's living sprinkles
     * @param maxAlive        the chapter's max-alive cap
     * @param spawnedThisFight elites this encounter has spawned so far
     * @param maxPerFight     the chapter's max-per-fight cap
     */
    public static Decision decide(Random rng, double baseChance, double stampMultiplier,
                                  double ceiling, int waveCount, int aliveNow,
                                  int maxAlive, int spawnedThisFight, int maxPerFight) {
        double eff = effectiveChance(baseChance, stampMultiplier, ceiling);
        if (waveCount <= 0 || maxPerFight <= 0 || spawnedThisFight >= maxPerFight) {
            return new Decision(false, false, Double.NaN, eff, 0);
        }
        double roll = rng.nextDouble();
        boolean passed = roll < eff;
        if (!passed) {
            return new Decision(true, false, roll, eff, 0);
        }
        int room = Math.min(maxAlive - aliveNow, maxPerFight - spawnedThisFight);
        int spawn = Math.max(0, Math.min(waveCount, room));
        return new Decision(true, true, roll, eff, spawn);
    }
}
