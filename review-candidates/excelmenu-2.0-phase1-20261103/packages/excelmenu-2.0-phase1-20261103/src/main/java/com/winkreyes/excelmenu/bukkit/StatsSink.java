// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * The one click-stats sink (DESIGN-2.0 §1.3): every dispatch from
 * every 2.0 surface — definition chests, definition forms, registry
 * chests, registry forms — is forwarded to ExcelMenuStats by
 * reflection, keyed by the item/entry ID (never by button index or
 * label, which is what ExcelBedrockMenu's ClickStats had to use).
 * Bedrock click counts therefore restart under id keys at the
 * Phase 2 switchover, exactly as the design records; Java definition
 * counts keep their item-id keys uninterrupted.
 *
 * Fail-soft by construction (the ClickStats pattern): no compile or
 * plugin.yml dependency; if ExcelMenuStats is absent or its surface
 * moved, the tap is simply not counted — one warning, never a throw
 * into a click handler.
 */
final class StatsSink {

    private static volatile boolean warned;

    private StatsSink() {}

    static void tap(Player player, String menuId, String entryId, int index, String platform) {
        try {
            Plugin stats = Bukkit.getPluginManager().getPlugin("ExcelMenuStats");
            if (stats == null || !stats.isEnabled()) return;
            Method m = stats.getClass().getMethod("recordTap", Player.class,
                    String.class, String.class, int.class, String.class);
            m.invoke(stats, player, menuId, entryId, index, platform);
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                Bukkit.getLogger().warning("[ExcelMenu] ExcelMenuStats hook unavailable: " + t);
            }
        }
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
