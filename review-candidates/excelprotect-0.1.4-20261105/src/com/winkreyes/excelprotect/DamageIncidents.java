package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelProtect 0.1.2 — DAMAGE INCIDENTS (SPEC: protect-damage-build/SPEC.md).
 * The pure coalescer: consecutive damage ticks to the same player from the
 * same source (resolved attacker identity + cause) within the gap window
 * merge into ONE incident record on the `damage` stream. No Bukkit types —
 * the Bukkit glue (DamageWatch) resolves attackers, health and elite status
 * and feeds ticks in; closed incidents render through Json/Ledger exactly
 * like every other stream. Elite level encoding: &gt;= 0 = that elite level,
 * NOT_ELITE (-1) = probed, not an elite (also the answer when EliteMobs is
 * absent — no elites can exist), UNKNOWN (-2) = EliteMobs present but its
 * API could not be resolved, so the record carries no elite claim at all.
 * Excel — Wink Reyes Valley
 */
public final class DamageIncidents {
    public static final int NOT_ELITE = -1;
    public static final int UNKNOWN = -2;

    /** One resolved damage tick, fed by the glue. healthBefore = red hearts just before this tick lands. */
    public static final class Tick {
        public final UUID playerUuid;
        public final String player, world, cause, sourceKey;
        public final double x, y, z;
        public final String attackerKind;    // mob | player | entity | environment | projectile
        public final String attackerName;    // kind=player
        public final String attackerEntity;  // entity-type key (mob / entity / projectile owner)
        public final String attackerUuid;
        public final String projectile;      // projectile type key when the hit arrived by projectile
        public final int eliteLevel;         // >= 0 elite level, NOT_ELITE, UNKNOWN (mob attackers)
        public final double damage, healthBefore, maxHealth;
        public final long ms;

        public Tick(UUID playerUuid, String player, String world, double x, double y, double z,
                    String cause, String sourceKey, String attackerKind, String attackerName,
                    String attackerEntity, String attackerUuid, String projectile, int eliteLevel,
                    double damage, double healthBefore, double maxHealth, long ms) {
            this.playerUuid = playerUuid; this.player = player; this.world = world;
            this.x = x; this.y = y; this.z = z;
            this.cause = cause; this.sourceKey = sourceKey; this.attackerKind = attackerKind;
            this.attackerName = attackerName; this.attackerEntity = attackerEntity;
            this.attackerUuid = attackerUuid; this.projectile = projectile;
            this.eliteLevel = eliteLevel;
            this.damage = damage; this.healthBefore = healthBefore; this.maxHealth = maxHealth;
            this.ms = ms;
        }
    }

    /** One open (still merging) or closed incident. */
    public static final class Incident {
        public final UUID playerUuid;
        public final String player, world, cause, sourceKey;
        public final double x, y, z;
        public final String attackerKind, attackerName, attackerEntity, attackerUuid, projectile;
        public final int eliteLevel;
        public final long startMs;
        public long lastMs;
        public int tickCount;
        public double totalDamage, maxSingleHit;
        public final double healthStart, maxHealth;
        public double healthEnd, healthMin;
        public boolean died;
        public String closed; // window | death | quit | shutdown

        Incident(Tick t) {
            this.playerUuid = t.playerUuid; this.player = t.player; this.world = t.world;
            this.x = t.x; this.y = t.y; this.z = t.z;
            this.cause = t.cause; this.sourceKey = t.sourceKey;
            this.attackerKind = t.attackerKind; this.attackerName = t.attackerName;
            this.attackerEntity = t.attackerEntity; this.attackerUuid = t.attackerUuid;
            this.projectile = t.projectile; this.eliteLevel = t.eliteLevel;
            this.startMs = t.ms; this.lastMs = t.ms;
            this.healthStart = t.healthBefore; this.maxHealth = t.maxHealth;
            merge(t);
        }

        void merge(Tick t) {
            tickCount++;
            totalDamage += t.damage;
            maxSingleHit = Math.max(maxSingleHit, t.damage);
            double after = Math.max(0, t.healthBefore - t.damage);
            healthEnd = after;
            healthMin = tickCount == 1 ? after : Math.min(healthMin, after);
            lastMs = t.ms;
        }

        /** The record body (the Ledger envelope adds ts/ts_ms + the start location). */
        public Json.Obj render(double nearDeathFraction) {
            Json.Obj o = new Json.Obj().put("type", "damage_incident")
                    .put("player", player).put("player_uuid", playerUuid.toString())
                    .put("cause", cause).put("attacker_kind", attackerKind);
            if (attackerName != null) o.put("attacker", attackerName);
            if (attackerEntity != null) o.put("attacker_entity", attackerEntity);
            if (attackerUuid != null) o.put("attacker_uuid", attackerUuid);
            if (projectile != null) o.put("projectile", projectile);
            if ("mob".equals(attackerKind)) {
                if (eliteLevel >= 0) o.put("elite", true).put("elite_level", eliteLevel);
                else if (eliteLevel == NOT_ELITE) o.put("elite", false);
                else o.put("elite_probe", "unavailable");
            }
            boolean nearDeath = !died && healthEnd <= nearDeathFraction * maxHealth;
            o.put("start_ts", Ledger.isoOf(startMs)).put("start_ts_ms", startMs)
             .put("end_ts_ms", lastMs).put("duration_ms", lastMs - startMs)
             .put("tick_count", tickCount)
             .put("total_damage", CombatCore.round2(totalDamage))
             .put("max_single_hit", CombatCore.round2(maxSingleHit))
             .put("health_start", CombatCore.round2(healthStart))
             .put("health_end", CombatCore.round2(healthEnd))
             .put("health_min", CombatCore.round2(healthMin))
             .put("max_health", CombatCore.round2(maxHealth))
             .put("near_death", nearDeath)
             .put("died", died)
             .put("closed", closed);
            return o;
        }
    }

    private final long gapMs;
    private final double nearDeathFraction;
    private final Map<UUID, Map<String, Incident>> open = new HashMap<>();

    public DamageIncidents(long gapMs, double nearDeathFraction) {
        this.gapMs = Math.max(0, gapMs);
        this.nearDeathFraction = nearDeathFraction;
    }

    public long gapMs() { return gapMs; }
    public double nearDeathFraction() { return nearDeathFraction; }

    /**
     * Feed one tick. An open incident for the same source whose gap expired
     * closes first (reason "window") and a fresh incident starts; returns the
     * incidents this tick closed (0 or 1).
     */
    public List<Incident> record(Tick t) {
        Map<String, Incident> bySource = open.computeIfAbsent(t.playerUuid, k -> new HashMap<>());
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

    /**
     * Close all of one player's open incidents (death / quit). On death the
     * most recently active incident carries died=true and health_end 0 — the
     * link to the deaths stream's snapshot at the same moment.
     */
    public List<Incident> closePlayer(UUID uuid, String reason, boolean died) {
        Map<String, Incident> bySource = open.remove(uuid);
        if (bySource == null || bySource.isEmpty()) return List.of();
        List<Incident> out = new ArrayList<>(bySource.values());
        out.sort((a, b) -> Long.compare(a.lastMs, b.lastMs));
        for (Incident inc : out) inc.closed = reason;
        if (died) {
            Incident last = out.get(out.size() - 1);
            last.died = true;
            last.healthEnd = 0;
            last.healthMin = 0;
        }
        return out;
    }

    /** Close everything (plugin shutdown, so the tail of an exposure is never lost). */
    public List<Incident> closeAll(String reason) {
        List<Incident> out = new ArrayList<>();
        for (UUID u : new ArrayList<>(open.keySet())) out.addAll(closePlayer(u, reason, false));
        return out;
    }

    public int openCount() {
        int n = 0;
        for (Map<String, Incident> m : open.values()) n += m.size();
        return n;
    }
}
