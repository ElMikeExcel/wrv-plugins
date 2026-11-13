// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Placeholder resolution for one viewer: ExcelMenu's own
 * %player_name% first, then PlaceholderAPI when it is installed
 * (soft-depend — Vault balance, GuildGate and MenuJoin expansions all
 * arrive through it, exactly as they did for DeluxeMenus).
 * Unresolved tokens are left untouched, matching DeluxeMenus.
 *
 * Carried verbatim from ExcelMenu 0.1.8 (its Placeholders class) —
 * the resolution behaviour is part of the parity contract the
 * golden-render harness checks against.
 */
final class Placeholders {

    private Placeholders() {}

    static String resolve(Player player, String text) {
        if (text == null) return "";
        String out = text.replace("%player_name%", player.getName());
        if (out.indexOf('%') >= 0
                && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                out = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, out);
            } catch (Throwable ignored) {
                // A broken expansion must not break a menu render.
            }
        }
        return out;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
