// Excel signature — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.excelautosortbridge;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * Provides %autosort_status% -> ON/OFF for the Server Menu's
 * Auto-sort toggle (ExcelMenu states pattern — the ExcelAimAssist
 * %aimassist_status% precedent). Plain ON/OFF with no color codes:
 * the menu compares the raw string. Returns null when the state is
 * unknown, so the token stays unresolved and ExcelMenu shows its
 * "unavailable" fallback — never a fabricated state.
 */
public final class AutoSortExpansion extends PlaceholderExpansion {

    private final ExcelAutoSortBridge plugin;

    public AutoSortExpansion(ExcelAutoSortBridge plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "autosort"; }
    @Override public String getAuthor() { return "WinkReyesValley"; }
    @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player p, String id) {
        if (p == null) return "";
        if (id.equalsIgnoreCase("status")) return plugin.autoSortStatus(p.getUniqueId());
        return null;
    }
}

// ExcelAutoSortBridge — Excel (Iris), for Wink Reyes Valley
