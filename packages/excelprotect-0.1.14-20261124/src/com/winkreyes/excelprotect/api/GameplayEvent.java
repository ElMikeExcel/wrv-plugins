package com.winkreyes.excelprotect.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Map;

/**
 * ExcelProtect integration contract (0.1.10, P-PROTECT-WATCH-1 — the AUDIT
 * WATCH emitter door): a gameplay plugin (ExcelSummons above all) fires this
 * event to record a gameplay fact in the owner's per-player audit log
 * (plugins/ExcelProtect/audit/<player>-<day>.jsonl, mirrored onto the
 * canonical audit stream). Firing it requires nothing of the emitter beyond
 * this class on the classpath, and if ExcelProtect is absent the event
 * simply has no listeners.
 *
 * kind: spell_cast | summon_spawn | summon_despawn | mana_reserve |
 * summon_refusal. fields MUST carry player + player_uuid (a record with no
 * player has no per-player home and is dropped, counted). Expected fields
 * per kind (numbers as Number, text as String):
 *   spell_cast:     spell, mana_cost, mana_after, mana_max, effective_mana_max
 *   summon_spawn:   summon (spell family base key), entity_uuid, entity_type,
 *                   branch, origin (cast|adopted)
 *   summon_despawn: summon, entity_uuid, reason (expired|dismissed|died|unloaded|…)
 *   mana_reserve:   summon, hold_percent, held_total_percent, held_total,
 *                   usable_mana, mana_max, effective_mana_max
 *   summon_refusal: summon (or spell), reason (cap|reserve_ceiling|
 *                   reserve_floor|mana), plus the same numbers shown to the
 *                   player (cap / counted names / hold percents as fired)
 * Emitters should also put source=<plugin name> in fields; Protect stamps
 * source=emitter when it is absent. Excel — Wink Reyes Valley
 */
public class GameplayEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String kind;
    private final Map<String, Object> fields;

    public GameplayEvent(String kind, Map<String, Object> fields) {
        super(false); // sync event: emitters fire on the main thread (verdicts, casts, trades)
        this.kind = kind; this.fields = fields;
    }

    public String kind() { return kind; }
    public Map<String, Object> fields() { return fields; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
