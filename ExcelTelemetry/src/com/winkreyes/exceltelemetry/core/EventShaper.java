package com.winkreyes.exceltelemetry.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure shaping of event payloads (everything except ts/ts_ms, which
 * the Bukkit layer stamps). Keeping the shapes here - away from
 * Bukkit types - is what lets the unit tests pin the exact JSONL
 * contract the Python report tooling reads.
 */
public final class EventShaper {
    private EventShaper() {}

    /** Item snapshot shape shared by weapons and armor pieces. */
    public static Map<String, Object> item(String id, Map<String, Integer> enchants, String customName) {
        if (id == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        if (enchants != null && !enchants.isEmpty()) {
            Map<String, Object> e = new LinkedHashMap<>();
            enchants.forEach((k, v) -> e.put(k, v));
            m.put("enchants", e);
        }
        if (customName != null && !customName.isBlank()) m.put("name", customName);
        return m;
    }

    public static Map<String, Object> kill(String victimType, String victimUuid, String victimName,
                                           Boolean elite, Integer eliteLevel,
                                           String killerKind, String killerUuid, String killerName,
                                           Map<String, Object> weapon,
                                           double fightSeconds, Map<String, Object> damage,
                                           String originClass, String originRaw, boolean rgDenied,
                                           double fallShare, List<Map<String, Object>> flags) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "kill");
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("type", victimType);
        if (victimUuid != null) v.put("uuid", victimUuid);
        if (victimName != null && !victimName.isBlank()) v.put("name", victimName);
        if (elite != null) v.put("elite", elite);
        if (eliteLevel != null) v.put("elite_level", eliteLevel);
        m.put("victim", v);
        Map<String, Object> k = new LinkedHashMap<>();
        k.put("kind", killerKind);
        if (killerUuid != null) k.put("uuid", killerUuid);
        if (killerName != null) k.put("name", killerName);
        m.put("killer", k);
        if (weapon != null) m.put("weapon", weapon);
        m.put("fight_s", Json.round1(fightSeconds));
        if (damage != null) m.put("damage", damage);
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("class", originClass);
        if (originRaw != null) o.put("raw", originRaw);
        if (rgDenied) o.put("rg_denied", true);
        m.put("origin", o);
        m.put("fall_softened", fallShare >= 0.5);
        if (flags != null && !flags.isEmpty()) m.put("flags", flags);
        return m;
    }

    public static Map<String, Object> playerDeath(String uuid, String name,
                                                  String causeKind, String damageCause,
                                                  String damagerType, String damagerUuid,
                                                  String deathMessage,
                                                  String world, String dimension,
                                                  int x, int y, int z,
                                                  Map<String, Object> gear,
                                                  Map<String, Object> mcmmoCombat) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "player_death");
        m.put("player", uuid);
        m.put("name", name);
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("kind", causeKind);
        if (damageCause != null) c.put("damage_cause", damageCause);
        if (damagerType != null) c.put("damager_type", damagerType);
        if (damagerUuid != null) c.put("damager_uuid", damagerUuid);
        if (deathMessage != null && !deathMessage.isBlank()) c.put("message", deathMessage);
        m.put("cause", c);
        Map<String, Object> loc = new LinkedHashMap<>();
        loc.put("world", world);
        loc.put("dimension", dimension);
        loc.put("x", x);
        loc.put("y", y);
        loc.put("z", z);
        m.put("location", loc);
        if (gear != null) m.put("gear", gear);
        if (mcmmoCombat != null) m.put("mcmmo_combat", mcmmoCombat);
        return m;
    }

    /** mcMMO level-up. Returns null for degenerate input (never emit a broken line). */
    public static Map<String, Object> mcmmoLevelUp(String uuid, String name, String skill,
                                                    int newLevel, int levelsGained) {
        if (uuid == null || skill == null || skill.isBlank() || newLevel < 0) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "mcmmo_level_up");
        m.put("player", uuid);
        if (name != null) m.put("name", name);
        m.put("skill", skill);
        m.put("level", newLevel);
        if (levelsGained > 0) m.put("levels_gained", levelsGained);
        return m;
    }

    /** mcMMO levels snapshot. skills == null (mcMMO unreadable) yields null - no fake data. */
    public static Map<String, Object> mcmmoSnapshot(String uuid, String name, String reason,
                                                    Map<String, Integer> skills, int power) {
        if (uuid == null || skills == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "mcmmo_snapshot");
        m.put("player", uuid);
        if (name != null) m.put("name", name);
        m.put("reason", reason);
        Map<String, Object> s = new LinkedHashMap<>();
        skills.forEach((k, v) -> s.put(k, v));
        m.put("skills", s);
        m.put("power", power);
        return m;
    }

    public static Map<String, Object> advancement(String uuid, String name, String key) {
        if (uuid == null || key == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "advancement");
        m.put("player", uuid);
        if (name != null) m.put("name", name);
        m.put("key", key);
        return m;
    }
}
