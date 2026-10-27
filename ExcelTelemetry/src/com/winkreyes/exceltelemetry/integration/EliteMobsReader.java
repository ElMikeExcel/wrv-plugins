package com.winkreyes.exceltelemetry.integration;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * EliteMobs elite detection by reflection on its public tracker
 * surface (EntityTracker.isEliteMob / getEliteMobEntity ->
 * EliteEntity.getLevel), verified against the installed 10.9.8 jar.
 * Reflection rather than a compile dependency so a future EliteMobs
 * update that moves the API degrades telemetry to elite=null
 * instead of breaking the plugin. Fails soft, warns once.
 */
public final class EliteMobsReader {
    public record EliteInfo(boolean elite, Integer level) {
        public static final EliteInfo UNKNOWN = new EliteInfo(false, null);
    }

    private Method isEliteMethod;
    private Method getEliteEntityMethod;
    private Method getLevelMethod;
    private boolean resolved;
    private boolean warned;

    public EliteInfo inspect(Entity entity) {
        if (entity == null) return EliteInfo.UNKNOWN;
        try {
            if (!resolve()) return EliteInfo.UNKNOWN;
            Object elite = getEliteEntityMethod.invoke(null, entity);
            if (elite == null) {
                // Cross-check with the boolean surface when it exists.
                if (isEliteMethod != null) {
                    Object b = isEliteMethod.invoke(null, entity);
                    if (Boolean.TRUE.equals(b)) return new EliteInfo(true, null);
                }
                return new EliteInfo(false, null);
            }
            Integer level = null;
            try {
                Object lv = getLevelMethod.invoke(elite);
                if (lv instanceof Number n) level = n.intValue();
            } catch (Throwable ignored) {
                // level is best-effort; the elite flag already landed
            }
            return new EliteInfo(true, level);
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                Bukkit.getLogger().warning("ExcelTelemetry: EliteMobs elite read failed ("
                        + t.getClass().getSimpleName() + ": " + t.getMessage()
                        + ") - elite flags will be absent. This warning fires once.");
            }
            return EliteInfo.UNKNOWN;
        }
    }

    private boolean resolve() {
        if (resolved) return isEliteMethod != null || getEliteEntityMethod != null;
        resolved = true;
        try {
            Plugin em = Bukkit.getPluginManager().getPlugin("EliteMobs");
            if (em == null || !em.isEnabled()) return false;
            Class<?> tracker = Class.forName("com.magmaguy.elitemobs.entitytracker.EntityTracker",
                    true, em.getClass().getClassLoader());
            try {
                getEliteEntityMethod = tracker.getMethod("getEliteMobEntity", Entity.class);
            } catch (NoSuchMethodException ignored) { /* boolean surface may still exist */ }
            try {
                isEliteMethod = tracker.getMethod("isEliteMob", Entity.class);
            } catch (NoSuchMethodException ignored) { /* object surface may still exist */ }
            if (getEliteEntityMethod != null) {
                Class<?> eliteEntity = Class.forName("com.magmaguy.elitemobs.mobconstructor.EliteEntity",
                        true, em.getClass().getClassLoader());
                getLevelMethod = eliteEntity.getMethod("getLevel");
            }
            return getEliteEntityMethod != null || isEliteMethod != null;
        } catch (Throwable t) {
            return false;
        }
    }
}
