package com.winkreyes.excelprotect;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * ExcelProtect 0.1.2 — EliteMobs attribution probe, fail-soft and reflective
 * (the CombatCore.hookMagic pattern: no compile-time dependency, absence is
 * silent, Protect runs identically without EliteMobs).
 *
 * Mechanism VERIFIED Nov 5, 2026 against the real EliteMobs 10.9.12 jar
 * (~/workspace/elitemobs-10.9.12/EliteMobs-10.9.12.jar, javap):
 *   com.magmaguy.elitemobs.entitytracker.EntityTracker
 *     public static boolean isEliteMob(org.bukkit.entity.Entity)
 *     public static EliteEntity getEliteMobEntity(org.bukkit.entity.Entity)
 *   com.magmaguy.elitemobs.mobconstructor.EliteEntity
 *     public int getLevel()
 * EliteMobs' own PlayerDamagedByEliteMobEvent wraps the same EliteEntity, so
 * this is the plugin's canonical identity for an elite — no metadata key or
 * PDC marker is documented in the jar, and none is guessed here.
 * Excel — Wink Reyes Valley
 */
final class EliteProbe {
    static final int UNKNOWN = DamageIncidents.UNKNOWN;
    static final int NOT_ELITE = DamageIncidents.NOT_ELITE;

    private static volatile int state = 0; // 0 untried, 1 hooked, 2 absent, 3 unavailable
    private static Method isEliteMob, getEliteMobEntity, getLevel;

    private EliteProbe() {}

    static synchronized void init(Plugin plugin) {
        if (state != 0) return;
        Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (em == null || !em.isEnabled()) { state = 2; return; }
        try {
            Class<?> tracker = Class.forName("com.magmaguy.elitemobs.entitytracker.EntityTracker");
            isEliteMob = tracker.getMethod("isEliteMob", Entity.class);
            getEliteMobEntity = tracker.getMethod("getEliteMobEntity", Entity.class);
            Class<?> elite = Class.forName("com.magmaguy.elitemobs.mobconstructor.EliteEntity");
            getLevel = elite.getMethod("getLevel");
            state = 1;
        } catch (Throwable t) {
            state = 3;
            plugin.getLogger().warning("[ExcelProtect] damage incidents: EliteMobs present but its EntityTracker API could not be resolved — elite attribution unavailable (records carry elite_probe=unavailable)");
        }
    }

    static String stateName() {
        return switch (state) {
            case 1 -> "hooked";
            case 2 -> "absent";
            case 3 -> "unavailable";
            default -> "untried";
        };
    }

    /**
     * The entity's elite level (&gt;= 0), NOT_ELITE when the probe ran and it
     * is not an elite — including when EliteMobs is absent entirely (no
     * elites can exist) — or UNKNOWN when the API could not answer.
     */
    static int levelOf(Entity e) {
        if (state == 2) return NOT_ELITE;
        if (state != 1) return UNKNOWN;
        try {
            if (!Boolean.TRUE.equals(isEliteMob.invoke(null, e))) return NOT_ELITE;
            Object elite = getEliteMobEntity.invoke(null, e);
            if (elite == null) return NOT_ELITE;
            Object lvl = getLevel.invoke(elite);
            return lvl instanceof Number n ? n.intValue() : UNKNOWN;
        } catch (Throwable t) {
            return UNKNOWN;
        }
    }
}
