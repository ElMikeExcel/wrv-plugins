package com.winkreyes.excelautosort.hook;

import com.winkreyes.excelautosort.ExcelAutoSort;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * %autosort_status% → ON/OFF — the same contract the retired bridge
 * served the menu (SPEC P9), now read from ExcelAutoSort's own live
 * state, plus %autosort_buffer% for the recovery entry's label (§8).
 * Plain strings, no color: the menu compares the raw value. The
 * fail-closed case is unchanged: with the plugin absent the token is
 * unresolvable and the menu shows its "unavailable" variant.
 */
public final class AutoSortExpansion extends PlaceholderExpansion {

    private final ExcelAutoSort plugin;

    public AutoSortExpansion(ExcelAutoSort plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "autosort"; }
    @Override public String getAuthor() { return "Iris, Mike"; }
    @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player p, String id) {
        if (p == null) return "";
        if (id.equalsIgnoreCase("status")) return plugin.sorter().statusOf(p.getUniqueId());
        if (id.equalsIgnoreCase("buffer")) return String.valueOf(plugin.buffers().countOf(p.getUniqueId()));
        // %autosort_maxorg% → OWNED / RENTED / OFF — the menu entry for
        // Max Organization shows the player's CURRENT entitlement (§15).
        if (id.equalsIgnoreCase("maxorg")) return plugin.maxOrg().access(p.getUniqueId()).name();
        return null;
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
