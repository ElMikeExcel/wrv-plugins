package com.winkreyes.exceltrainingdummy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Per-player platform detection — the house pattern (ExcelTour):
 * reflection-only so the plugin enables with neither Geyser nor
 * Floodgate present: 1) Floodgate API, 2) Geyser API, 3) Floodgate
 * UUID-prefix heuristic (rendering choice only).
 */
public final class PlatformDetect {
    private PlatformDetect() {}

    public static boolean isBedrock(Player player, Logger log) {
        UUID uuid = player.getUniqueId();
        Boolean fg = floodgateCheck(uuid);
        if (fg != null) return fg;
        Boolean gy = geyserCheck(uuid);
        if (gy != null) return gy;
        if (uuid.toString().startsWith("00000000-0000-0000-0009-")) {
            log.fine("ExcelTrainingDummy: Bedrock rendering for " + player.getName() + " via UUID-prefix heuristic");
            return true;
        }
        return false;
    }

    private static Boolean floodgateCheck(UUID uuid) {
        try {
            Plugin p = Bukkit.getPluginManager().getPlugin("floodgate");
            if (p == null || !p.isEnabled()) return null;
            ClassLoader cl = p.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi", true, cl);
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object r = apiClass.getMethod("isFloodgatePlayer", UUID.class).invoke(api, uuid);
            return Boolean.TRUE.equals(r) ? Boolean.TRUE : Boolean.FALSE;
        } catch (Exception e) { return null; }
    }

    private static Boolean geyserCheck(UUID uuid) {
        try {
            Plugin p = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
            if (p == null || !p.isEnabled()) return null;
            ClassLoader cl = p.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("org.geysermc.geyser.api.GeyserApi", true, cl);
            Object api = apiClass.getMethod("api").invoke(null);
            Object r = apiClass.getMethod("isBedrockPlayer", UUID.class).invoke(api, uuid);
            return Boolean.TRUE.equals(r) ? Boolean.TRUE : Boolean.FALSE;
        } catch (Exception e) { return null; }
    }
}
