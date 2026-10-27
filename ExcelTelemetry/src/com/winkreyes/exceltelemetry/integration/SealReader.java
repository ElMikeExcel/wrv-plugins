package com.winkreyes.exceltelemetry.integration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Reads ExcelGuildGate's per-seal state by reflection - the house
 * pattern, mirroring ExcelNoPhantoms 1.2.0 / ExcelMagicGate: load
 * GuildGateAPI from GuildGate's own classloader, read its private
 * static `state` field, invoke SealState.isBroken(String).
 * Any failure returns null (unknown) and telemetry carries on:
 * seal stamps are garnish on the encounter record, never a reason
 * to lose the record itself.
 */
public final class SealReader {
    private Class<?> apiClass;
    private Field stateField;
    private Method isBrokenMethod;
    private boolean warned;

    /** TRUE/FALSE when GuildGate answered; null when unavailable. */
    public synchronized Boolean isBroken(String sealId) {
        try {
            Plugin gg = Bukkit.getPluginManager().getPlugin("ExcelGuildGate");
            if (gg == null || !gg.isEnabled()) return null;
            if (apiClass == null || stateField == null) {
                apiClass = Class.forName("com.winkreyes.guildgate.api.GuildGateAPI",
                        true, gg.getClass().getClassLoader());
                stateField = apiClass.getDeclaredField("state");
                stateField.setAccessible(true);
            }
            Object state = stateField.get(null);
            if (state == null) return null;
            if (isBrokenMethod == null || isBrokenMethod.getDeclaringClass() != state.getClass()) {
                isBrokenMethod = state.getClass().getMethod("isBroken", String.class);
            }
            Object result = isBrokenMethod.invoke(state, sealId);
            return result instanceof Boolean b ? b : null;
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                Bukkit.getLogger().warning("ExcelTelemetry: could not read the ExcelGuildGate seal state ("
                        + t.getClass().getSimpleName() + ": " + t.getMessage()
                        + ") - seal stamps will be absent. This warning fires once.");
            }
            return null;
        }
    }
}
