/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The ExcelProtect emitter switches (ExcelBossTrial 0.3.4): read
 * adapter-side from the top-level {@code protect-emit:} config
 * block (the fight-tracking / milestone / sprinkle precedent - no
 * core verdict rule consumes them, so BossTrialConfig's canonical
 * record stays untouched).
 *
 * The emitter is an OBSERVER: these keys gate ONLY what BossTrial
 * publishes to ExcelProtect. No value here can change a fight, a
 * verdict, a pool or a reward; turning every key off leaves
 * BossTrial behaving exactly as 0.3.3 (the fight records still
 * land in fights/ as before).
 *
 * Defaults are all ON (settled with the 0.3.4 commission, Mike,
 * Nov 2, 2026): the shadow-live Protect is the suite's record
 * system, and the seal-verdict slot in its audit family exists
 * precisely for this publish. {@code enabled} is the master kill
 * switch; {@code fightTables} gates the verdict fight-table
 * publish (and the elder-progress publish that rides the same
 * shaping); {@code stateChanges} gates the boss_state records
 * (fight started / fight ended / respawn scheduled / respawned).
 */
public record ProtectEmitSettings(boolean enabled, boolean fightTables,
                                  boolean stateChanges) {

    public static ProtectEmitSettings defaults() {
        return new ProtectEmitSettings(true, true, true);
    }

    /** Sanitized construction: the record is three booleans, so
     *  sanitizing is only the shape guarantee - a settings object
     *  with the master off reports both sub-switches off too, so
     *  no consumer ever has to re-check the master. */
    public static ProtectEmitSettings sanitized(boolean enabled, boolean fightTables,
                                                boolean stateChanges) {
        if (!enabled) {
            return new ProtectEmitSettings(false, false, false);
        }
        return new ProtectEmitSettings(true, fightTables, stateChanges);
    }

    /** True when nothing at all may be published. */
    public boolean dark() {
        return !enabled || (!fightTables && !stateChanges);
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
