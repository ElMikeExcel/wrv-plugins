package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ExcelProtect 0.1.3 — HEALING incidents (SPEC: protect-dealt-build/SPEC.md).
 * Consecutive EntityRegainHealthEvents for the same PLAYER target from the
 * same healer + source within the gap window merge into ONE incident on the
 * `healing` stream. Self-healing (healer = target: regen, food, own potions,
 * own heal spells) and player-to-player healing (a Magic cast claim pairing,
 * the CombatCore heal-claim model) are the two shapes Mike asked for; a
 * CUSTOM regain nobody can attribute keeps healer = null instead of guessing.
 *
 * Amount semantics, decided once: the record carries BOTH sums.
 *   total_healed    — the sum of the events' own amounts (what the sources
 *                     TRIED to heal; this is what per-fight healing totals in
 *                     CombatCore also sum, so the layers agree).
 *   total_effective — the cap-adjusted sum: each event contributes at most
 *                     the target's missing health at that moment, tracked by
 *                     an internal cursor that starts at the first event's
 *                     health_before and advances by each effective amount
 *                     (clamped at max health). When nothing else heals or
 *                     hurts the target inside the incident this EQUALS the
 *                     real health gained; health_end is that cursor, honest
 *                     to event-time data — no delayed sampling is invented.
 * Pure logic, no Bukkit types — HealingWatch resolves attribution and feeds
 * ticks in. Excel — Wink Reyes Valley
 */
public final class HealingIncidents {

    /** One resolved heal event, fed by the glue. healthBefore = target red hearts just before this event applies. */
    public static final class Tick {
        public final UUID targetUuid;
        public final String target, world, sourceKey;
        public final double x, y, z;
        public final UUID healerUuid;   // null = unattributed (CUSTOM regain, no claim)
        public final String healer;     // healer name when attributed
        public final boolean self;      // healer == target
        public final String source;     // SPELL | the RegainReason name
        public final String reason;     // raw RegainReason name (CUSTOM for spell-paired heals)
        public final String spell;      // Magic spell key when source == SPELL
        public final double amount, healthBefore, maxHealth;
        public final long ms;

        public Tick(UUID targetUuid, String target, String world, double x, double y, double z,
                    String sourceKey, UUID healerUuid, String healer, boolean self,
                    String source, String reason, String spell,
                    double amount, double healthBefore, double maxHealth, long ms) {
            this.targetUuid = targetUuid; this.target = target; this.world = world;
            this.x = x; this.y = y; this.z = z; this.sourceKey = sourceKey;
            this.healerUuid = healerUuid; this.healer = healer; this.self = self;
            this.source = source; this.reason = reason; this.spell = spell;
            this.amount = amount; this.healthBefore = healthBefore; this.maxHealth = maxHealth;
            this.ms = ms;
        }
    }

    /** One open (still merging) or closed healing incident. */
    public static final class Incident {
        public final UUID targetUuid;
        public final String target, world, sourceKey;
        public final double x, y, z;
        public final UUID healerUuid;
        public final String healer, source, spell;
        public final boolean self;
        public final Set<String> reasons = new LinkedHashSet<>();
        public final long startMs;
        public long lastMs;
        public int eventCount;
        public double totalHealed, totalEffective, maxSingle;
        public final double healthStart, maxHealth;
        public double healthCursor;
        public String closed; // window | quit | shutdown

        Incident(Tick t) {
            this.targetUuid = t.targetUuid; this.target = t.target; this.world = t.world;
            this.x = t.x; this.y = t.y; this.z = t.z; this.sourceKey = t.sourceKey;
            this.healerUuid = t.healerUuid; this.healer = t.healer;
            this.source = t.source; this.spell = t.spell; this.self = t.self;
            this.startMs = t.ms; this.lastMs = t.ms;
            this.healthStart = t.healthBefore; this.maxHealth = t.maxHealth;
            this.healthCursor = t.healthBefore;
            merge(t);
        }

        void merge(Tick t) {
            eventCount++;
            totalHealed += t.amount;
            maxSingle = Math.max(maxSingle, t.amount);
            double room = Math.max(0, maxHealth - healthCursor);
            double eff = Math.min(t.amount, room);
            totalEffective += eff;
            healthCursor = Math.min(maxHealth, healthCursor + eff);
            reasons.add(t.reason);
            lastMs = t.ms;
        }

        /** The record body (the Ledger envelope adds ts/ts_ms + the start location). */
        public Json.Obj render() {
            Json.Obj o = new Json.Obj().put("type", "healing_incident")
                    .put("target", target).put("target_uuid", targetUuid.toString())
                    .put("source", source).put("self", self);
            if (healer != null) o.put("healer", healer);
            if (healerUuid != null) o.put("healer_uuid", healerUuid.toString());
            if (spell != null) o.put("spell", spell);
            Json.Arr rs = new Json.Arr();
            for (String r : reasons) rs.add(r);
            o.put("reasons", rs);
            o.put("start_ts", Ledger.isoOf(startMs)).put("start_ts_ms", startMs)
             .put("end_ts_ms", lastMs).put("duration_ms", lastMs - startMs)
             .put("event_count", eventCount)
             .put("total_healed", CombatCore.round2(totalHealed))
             .put("total_effective", CombatCore.round2(totalEffective))
             .put("max_single_heal", CombatCore.round2(maxSingle))
             .put("health_start", CombatCore.round2(healthStart))
             .put("health_end", CombatCore.round2(healthCursor))
             .put("max_health", CombatCore.round2(maxHealth))
             .put("closed", closed);
            return o;
        }
    }

    private final long gapMs;
    private final Map<UUID, Map<String, Incident>> open = new HashMap<>();

    public HealingIncidents(long gapMs) { this.gapMs = Math.max(0, gapMs); }

    public long gapMs() { return gapMs; }

    /** Feed one event; returns incidents this event closed by window expiry (0 or 1). */
    public List<Incident> record(Tick t) {
        Map<String, Incident> bySource = open.computeIfAbsent(t.targetUuid, k -> new HashMap<>());
        Incident cur = bySource.get(t.sourceKey);
        List<Incident> closedOut = new ArrayList<>();
        if (cur != null && t.ms - cur.lastMs > gapMs) {
            cur.closed = "window";
            bySource.remove(t.sourceKey);
            closedOut.add(cur);
            cur = null;
        }
        if (cur == null) bySource.put(t.sourceKey, new Incident(t));
        else cur.merge(t);
        return closedOut;
    }

    /** Close every incident whose gap has expired by nowMs (the sweep tick). */
    public List<Incident> sweep(long nowMs) {
        List<Incident> out = new ArrayList<>();
        open.values().forEach(bySource -> bySource.values().removeIf(inc -> {
            if (nowMs - inc.lastMs > gapMs) { inc.closed = "window"; out.add(inc); return true; }
            return false;
        }));
        open.values().removeIf(Map::isEmpty);
        return out;
    }

    /** Close all incidents FOR one target (their quit ends their merge state). */
    public List<Incident> closeTarget(UUID uuid, String reason) {
        Map<String, Incident> bySource = open.remove(uuid);
        if (bySource == null || bySource.isEmpty()) return List.of();
        List<Incident> out = new ArrayList<>(bySource.values());
        for (Incident inc : out) inc.closed = reason;
        return out;
    }

    /** Close everything (plugin shutdown). */
    public List<Incident> closeAll(String reason) {
        List<Incident> out = new ArrayList<>();
        for (UUID u : new ArrayList<>(open.keySet())) out.addAll(closeTarget(u, reason));
        return out;
    }

    public int openCount() {
        int n = 0;
        for (Map<String, Incident> m : open.values()) n += m.size();
        return n;
    }
}
