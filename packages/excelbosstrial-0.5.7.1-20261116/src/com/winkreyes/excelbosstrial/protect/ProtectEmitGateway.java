/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.protect;

import com.winkreyes.excelbosstrial.core.ProtectEmitSink;
import com.winkreyes.excelprotect.api.ExcelProtectAPI;
import com.winkreyes.excelprotect.api.FightTableEvent;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The ExcelProtect adapter (ExcelBossTrial 0.3.4) - the ONLY
 * class in BossTrial that touches ExcelProtect's API, kept in
 * its own package and instantiated by the host ONLY when the
 * ExcelProtect plugin is present and enabled (the SealCreditHook
 * isolation pattern): BossTrial compiles against Protect's API
 * jar but loads and runs exactly as before when Protect is
 * absent, because nothing outside this class links the API.
 *
 * The seam this implements is replaceable by design (house build
 * style): the host speaks {@link ProtectEmitSink} in plain JDK
 * types, the shapes come from core/ProtectTableShape, and this
 * class only carries them across -
 *   fightTable -> the contract's FightTableEvent (Protect
 *     records its combat fight_summary + audit seal_verdict);
 *   record     -> ExcelProtectAPI.publish, Protect's general
 *     door (boss_state + fight_progress records).
 *
 * Fail-soft, always: every call is wrapped end to end. If
 * Protect was removed mid-boot, its API moved, or the event bus
 * refuses the event, the publish is dropped with at most three
 * warning lines per boot and the fight runs untouched - the
 * encounter's own fights/ record remains the proof of the fight.
 */
public final class ProtectEmitGateway implements ProtectEmitSink {

    private final Logger log;
    private int failures;

    public ProtectEmitGateway(Logger log) {
        this.log = log;
    }

    @Override
    public void fightTable(String encounter, String verdict, String credit, double pool,
                           double durationSeconds, List<Map<String, Object>> rows) {
        try {
            Bukkit.getPluginManager().callEvent(new FightTableEvent(
                    encounter, verdict, credit, pool, durationSeconds, rows));
        } catch (Throwable t) {
            failure("fight table for " + encounter + " (" + verdict + ")", t);
        }
    }

    @Override
    public void record(String stream, String type, Map<String, Object> fields) {
        try {
            ExcelProtectAPI.publish(stream, type, fields);
        } catch (Throwable t) {
            failure("record " + type + " -> " + stream, t);
        }
    }

    private void failure(String what, Throwable t) {
        failures++;
        if (failures <= 3) {
            log.warning("ExcelBossTrial protect emit: " + what
                    + " was not published (" + t + ") - the fight is unaffected; "
                    + "its record stands in fights/.");
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
