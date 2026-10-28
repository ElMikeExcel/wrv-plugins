package com.winkreyes.excelbedrockmenu;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * v1.5.15: forwards each Bedrock form button tap to ExcelMenuStats (clicks.csv) by reflection.
 * No compile or plugin.yml dependency; if ExcelMenuStats is absent or fails, the tap is simply not counted.
 * Never throws into the form handler.
 */
final class ClickStats {
    private ClickStats() {}
    static volatile boolean warned;

    static void tap(Player player, String formTitle, String button, int index) {
        try {
            Plugin stats = Bukkit.getPluginManager().getPlugin("ExcelMenuStats");
            if (stats == null || !stats.isEnabled()) return;
            Method m = stats.getClass().getMethod("recordTap", Player.class, String.class, String.class, int.class, String.class);
            // v1.5.21: labels carry group color codes on the form; log the plain label.
            m.invoke(stats, player, formTitle, ExcelBedrockMenu.stripCodes(button), index, "bedrock");
        } catch (Throwable t) {
            if (!warned) { warned = true; Bukkit.getLogger().warning("[ExcelBedrockMenu] ExcelMenuStats hook unavailable: " + t); }
        }
    }
}

