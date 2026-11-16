package com.winkreyes.excelsummons.protect;

import com.winkreyes.excelsummons.GameplaySink;
import com.winkreyes.excelprotect.api.ExcelProtectAPI;

import java.util.Map;
import java.util.logging.Logger;

/**
 * The ExcelProtect adapter (ExcelSummons 0.1.12) — the ONLY class
 * in ExcelSummons that touches ExcelProtect's API, kept in its
 * own package and instantiated by GameplayDoor ONLY when the
 * ExcelProtect plugin is present and enabled (the house isolation
 * pattern): Summons compiles against Protect 0.1.10's API jar but
 * loads and runs exactly as before when Protect is absent,
 * because nothing outside this class links the API.
 *
 * The seam it carries is ExcelProtectAPI.gameplay — the 0.1.10
 * Audit Watch emitter door (api/GameplayEvent): a gameplay fact
 * (summon_refusal / summon_spawn / summon_despawn / mana_reserve)
 * lands in the owner's per-player audit log under
 * plugins/ExcelProtect/audit/, mirrored onto Protect's canonical
 * audit stream. Fire-and-forget, fail-soft end to end: if Protect
 * was removed mid-boot or the event bus refuses the event, the
 * record is dropped with at most three warning lines per boot —
 * the summon, the refusal, and the player's message are never
 * affected.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class ProtectGameplayGateway implements GameplaySink {

    private final Logger log;
    private int failures;

    public ProtectGameplayGateway(Logger log) {
        this.log = log;
    }

    @Override
    public void emit(String kind, Map<String, Object> fields) {
        try {
            ExcelProtectAPI.gameplay(kind, fields);
        } catch (Throwable t) {
            failures++;
            if (failures <= 3) {
                log.warning("ExcelSummons gameplay emit: a " + kind
                        + " record was not published to ExcelProtect (" + t
                        + ") — the summon itself is unaffected.");
            }
        }
    }
}
