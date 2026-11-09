package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelProtect 0.1.3 — DAMAGE DEALT incidents (SPEC: protect-dealt-build/SPEC.md).
 * The mirror of DamageIncidents on the attacker side: consecutive hits BY one
 * player ON the same non-player victim with the same cause (and the same
 * delivery — hand, projectile type, or claimed spell) within the gap window
 * merge into ONE incident record on the `dealt` stream. This is the damage-
 * CONTRIBUTION evidence base: who did what to whom, how hard, and whether the
 * victim died under them. Player-vs-player damage is deliberately NOT here —
 * 0.1.2's victim-side `damage` stream already records it from the victim's
 * angle; this stream is non-player victims only, so nothing double-counts.
 *
 * Pure logic, no Bukkit types — DealtWatch resolves shooter/spell/health and
 * probes EliteMobs for the VICTIM-side elite attribution, feeding ticks in.
 * Elite level encoding matches DamageIncidents exactly: &gt;= 0 = elite level,
 * NOT_ELITE (-1) = probed, not an elite (also the answer when EliteMobs is
 * absent), UNKNOWN (-2) = EliteMobs present but its API could not answer, so
 * the record carries no elite claim at all.
 * Excel — Wink Reyes Valley
 */
public final class DealtIncidents {
    public static final int NOT_ELITE = -1;
    public static final int UNKNOWN = -2;

    /** One resolved dealt-damage hit, fed by the glue. victimHealthBefore = victim red hearts just before this hit lands. */
    public static final class Tick {
        public final UUID attackerUuid;
        public final String attacker, world, cause, sourceKey;
        public final double x, y, z;
        public final String victimKey;     // stable per-victim key: entity-type key + ":" + uuid
        public final String victimEntity;  // entity-type key, e.g. minecraft:zombie
        public final String victimUuid;
        public final String victimName;    // custom name when the victim carries one
        public final String projectile;    // projectile type key when the hit arrived by projectile
        public final String spell;         // Magic spell key when a fresh cast claim explains the hit
        public final String delivery;      // 0.1.8 §S1: hand | projectile | spell | potion (explicit)
        public final int eliteLevel;       // victim-side: >= 0 elite level, NOT_ELITE, UNKNOWN
        public final double damage, victimHealthBefore, victimMaxHealth;
        public final long ms;

        public Tick(UUID attackerUuid, String attacker, String world, double x, double y, double z,
                    String cause, String sourceKey, String victimKey, String victimEntity,
                    String victimUuid, String victimName, String projectile, String spell,
                    int eliteLevel, double damage, double victimHealthBefore, double victimMaxHealth,
                    long ms) {
            this(attackerUuid, attacker, world, x, y, z, cause, sourceKey, victimKey, victimEntity,
                    victimUuid, victimName, projectile, spell,
                    spell != null ? "spell"
                            : HealAttribution.isPotionProjectile(projectile) ? "potion"
                            : projectile != null ? "projectile" : "hand",
                    eliteLevel, damage, victimHealthBefore, victimMaxHealth, ms);
        }

        public Tick(UUID attackerUuid, String attacker, String world, double x, double y, double z,
                    String cause, String sourceKey, String victimKey, String victimEntity,
                    String victimUuid, String victimName, String projectile, String spell,
                    String delivery,
                    int eliteLevel, double damage, double victimHealthBefore, double victimMaxHealth,
                    long ms) {
            this.attackerUuid = attackerUuid; this.attacker = attacker; this.world = world;
            this.x = x; this.y = y; this.z = z;
            this.cause = cause; this.sourceKey = sourceKey;
            this.victimKey = victimKey; this.victimEntity = victimEntity;
            this.victimUuid = victimUuid; this.victimName = victimName;
            this.projectile = projectile; this.spell = spell; this.delivery = delivery;
            this.eliteLevel = eliteLevel;
            this.damage = damage; this.victimHealthBefore = victimHealthBefore;
            this.victimMaxHealth = victimMaxHealth; this.ms = ms;
        }
    }

    /** One open (still merging) or closed dealt incident. */
    public static final class Incident {
        public final UUID attackerUuid;
        public final String attacker, world, cause, sourceKey;
        public final double x, y, z;
        public final String victimKey, victimEntity, victimUuid, victimName, projectile;
        public String spell;    // mutable: a late claim can relabel the incident (§S1)
        public String delivery; // mutable with spell (relabel flips it to "spell")
        public final int eliteLevel;
        public final long startMs;
        public long lastMs;
        public int hitCount;
        public double totalDamage, maxSingleHit;
        public final double victimHealthStart, victimMaxHealth;
        public double victimHealthEnd, victimHealthMin;
        public boolean died;
        public String closed; // window | victim_death | quit | shutdown

        Incident(Tick t) {
            this.attackerUuid = t.attackerUuid; this.attacker = t.attacker; this.world = t.world;
            this.x = t.x; this.y = t.y; this.z = t.z;
            this.cause = t.cause; this.sourceKey = t.sourceKey;
            this.victimKey = t.victimKey; this.victimEntity = t.victimEntity;
            this.victimUuid = t.victimUuid; this.victimName = t.victimName;
            this.projectile = t.projectile; this.spell = t.spell; this.delivery = t.delivery;
            this.eliteLevel = t.eliteLevel;
            this.startMs = t.ms; this.lastMs = t.ms;
            this.victimHealthStart = t.victimHealthBefore; this.victimMaxHealth = t.victimMaxHealth;
            merge(t);
        }

        /**
         * §S1 late relabel: the incident's ticks arrived in the thrown-potion
         * costume (CastEvent is post-cast), but a claim naming this victim
         * landed before the incident closed — the spell takes the label and
         * the costume comes off. No-op once a spell is set.
         */
        public boolean relabel(String spellKey) {
            if (spell != null || spellKey == null) return false;
            this.spell = spellKey;
            this.delivery = "spell";
            return true;
        }

        void merge(Tick t) {
            hitCount++;
            totalDamage += t.damage;
            maxSingleHit = Math.max(maxSingleHit, t.damage);
            double after = Math.max(0, t.victimHealthBefore - t.damage);
            victimHealthEnd = after;
            victimHealthMin = hitCount == 1 ? after : Math.min(victimHealthMin, after);
            lastMs = t.ms;
        }

        /** The record body (the Ledger envelope adds ts/ts_ms + the start location). */
        public Json.Obj render() {
            Json.Obj o = new Json.Obj().put("type", "damage_dealt_incident")
                    .put("player", attacker).put("player_uuid", attackerUuid.toString())
                    .put("cause", cause)
                    .put("victim", victimEntity).put("victim_uuid", victimUuid)
                    .put("victim_key", victimKey);
            if (victimName != null) o.put("victim_name", victimName);
            // §S1: delivery is explicit on every record; a spell-labelled hit
            // does NOT carry the thrown-potion costume it arrived in.
            o.put("delivery", delivery);
            if (projectile != null && !"spell".equals(delivery)) o.put("projectile", projectile);
            if (spell != null) o.put("spell", spell);
            if (eliteLevel >= 0) o.put("elite", true).put("elite_level", eliteLevel);
            else if (eliteLevel == NOT_ELITE) o.put("elite", false);
            else o.put("elite_probe", "unavailable");
            o.put("start_ts", Ledger.isoOf(startMs)).put("start_ts_ms", startMs)
             .put("end_ts_ms", lastMs).put("duration_ms", lastMs - startMs)
             .put("hit_count", hitCount)
             .put("total_damage", CombatCore.round2(totalDamage))
             .put("max_single_hit", CombatCore.round2(maxSingleHit))
             .put("victim_health_start", CombatCore.round2(victimHealthStart))
             .put("victim_health_end", CombatCore.round2(victimHealthEnd))
             .put("victim_health_min", CombatCore.round2(victimHealthMin))
             .put("victim_max_health", CombatCore.round2(victimMaxHealth))
             .put("died", died)
             .put("closed", closed);
            return o;
        }
    }

    private final long gapMs;
    private final Map<UUID, Map<String, Incident>> open = new HashMap<>();

    public DealtIncidents(long gapMs) { this.gapMs = Math.max(0, gapMs); }

    public long gapMs() { return gapMs; }

    /**
     * Feed one hit. An open incident for the same source whose gap expired
     * closes first (reason "window") and a fresh incident starts; returns the
     * incidents this hit closed (0 or 1).
     */
    public List<Incident> record(Tick t) {
        Map<String, Incident> bySource = open.computeIfAbsent(t.attackerUuid, k -> new HashMap<>());
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
     * A victim died: close EVERY open incident on that victim (all attackers),
     * reason "victim_death". The killing-blow attribution lands on the incident
     * belonging to killerUuid when one is open; otherwise on the most recently
     * active incident (the last damage the victim took before dying — e.g. a
     * finisher the killer field does not name, like a spell DoT). That one
     * incident carries died=true and victim_health_end 0.
     */
    public List<Incident> closeVictim(String victimKey, UUID killerUuid) {
        List<Incident> out = new ArrayList<>();
        for (Map<String, Incident> bySource : open.values()) {
            bySource.values().removeIf(inc -> {
                if (inc.victimKey.equals(victimKey)) { out.add(inc); return true; }
                return false;
            });
        }
        open.values().removeIf(Map::isEmpty);
        if (out.isEmpty()) return out;
        Incident killer = null;
        for (Incident inc : out) {
            inc.closed = "victim_death";
            if (killerUuid != null && inc.attackerUuid.equals(killerUuid)) killer = inc;
        }
        if (killer == null) {
            killer = out.get(0);
            for (Incident inc : out) if (inc.lastMs > killer.lastMs) killer = inc;
        }
        killer.died = true;
        killer.victimHealthEnd = 0;
        killer.victimHealthMin = 0;
        return out;
    }

    /** Close all of one attacker's open incidents (quit). */
    public List<Incident> closePlayer(UUID uuid, String reason) {
        Map<String, Incident> bySource = open.remove(uuid);
        if (bySource == null || bySource.isEmpty()) return List.of();
        List<Incident> out = new ArrayList<>(bySource.values());
        for (Incident inc : out) inc.closed = reason;
        return out;
    }

    /** Close everything (plugin shutdown, so no engagement tail is lost). */
    public List<Incident> closeAll(String reason) {
        List<Incident> out = new ArrayList<>();
        for (UUID u : new ArrayList<>(open.keySet())) out.addAll(closePlayer(u, reason));
        return out;
    }

    public int openCount() {
        int n = 0;
        for (Map<String, Incident> m : open.values()) n += m.size();
        return n;
    }
}
