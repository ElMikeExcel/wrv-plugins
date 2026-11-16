/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The sprinkle's guaranteed cadence (ExcelBossTrial 0.4.0, the
 * Wither-night fix train, PM-6). The 0.3.3 sprinkle rolled a die
 * at each milestone stamp: on Nov 7, 2026 all three rolls failed
 * and a four-champion Wither fight got ZERO adds - Mike's ruling
 * the same night: adds become CONSISTENT (guaranteed cadence, no
 * RNG-gated zero-add fights), because the waves were the fun.
 *
 * So occurrence is no longer rolled at all. A wave is OWED when
 * any of these is true:
 *   - a milestone beat (75/50/25% of the pool remaining) fires;
 *   - a previously owed wave is still pending (deferred);
 *   - the timed cadence has elapsed since the last wave.
 * An owed wave FIRES when there is room (alive under max-alive
 * and the per-fight budget unspent); with no room it is
 * DEFERRED to the next cadence tick, never spent. Only the
 * per-fight budget ending retires an owed wave for good (the
 * budget is spent; there is nothing left to defer). Wave SIZE
 * may still jitter (+/- 1 within config bounds) in the adapter -
 * occurrence never does.
 *
 * Pure logic, unit-tested directly; the adapter
 * (SprinkleService) owns entities, timers and the bookkeeping
 * state (SprinkleBook carries the pending flag and last-wave
 * stamp this class judges from).
 */
public final class SprinkleCadence {

    private SprinkleCadence() {
    }

    public enum Decision {
        /** Spawn the wave now (spawnCount elites). */
        FIRE,
        /** Owed but no room: keep the pending flag, retry next tick. */
        DEFER,
        /** Owed but the per-fight budget is spent: retire the wave. */
        SPENT,
        /** Nothing owed. */
        IDLE
    }

    public record Outcome(Decision decision, int spawnCount) {
    }

    /** True when the timed cadence has elapsed (a non-positive
     *  cadence retires the timer - beats and deferrals only).
     *  A last-wave stamp of 0 means the cadence clock has not
     *  started for this encounter (the service stamps it at the
     *  first tick): not due - the first timed wave belongs a
     *  full cadence into the fight, never at the opening bell
     *  (the milestone stamps carry the early fight). */
    public static boolean cadenceDue(long lastWaveMs, long nowMs, int cadenceSeconds) {
        if (cadenceSeconds <= 0) {
            return false;
        }
        if (lastWaveMs <= 0) {
            return false; // the clock starts at the encounter's first tick
        }
        return nowMs - lastWaveMs >= cadenceSeconds * 1000L;
    }

    /**
     * Judge one wave opportunity.
     *
     * @param owed            a beat fired, a deferral pends, or
     *                        the cadence elapsed
     * @param waveCount       the wave template's size
     * @param aliveNow        this encounter's living adds
     * @param maxAlive        the chapter's max-alive cap
     * @param spawnedThisFight adds this encounter has spawned
     * @param maxPerFight     the chapter's per-fight cap
     */
    public static Outcome judge(boolean owed, int waveCount, int aliveNow,
                                int maxAlive, int spawnedThisFight, int maxPerFight) {
        if (!owed) {
            return new Outcome(Decision.IDLE, 0);
        }
        if (waveCount <= 0 || maxPerFight <= 0 || spawnedThisFight >= maxPerFight) {
            return new Outcome(Decision.SPENT, 0);
        }
        int room = Math.min(maxAlive - aliveNow, maxPerFight - spawnedThisFight);
        if (room <= 0) {
            return new Outcome(Decision.DEFER, 0);
        }
        return new Outcome(Decision.FIRE, Math.min(waveCount, room));
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
