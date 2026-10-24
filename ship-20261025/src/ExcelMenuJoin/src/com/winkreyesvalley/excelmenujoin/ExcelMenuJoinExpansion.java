package com.winkreyesvalley.excelmenujoin;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * Provides %menujoin_status% -> ON/OFF for the auto-open-on-join toggle.
 * The identifier stays "menujoin" on purpose: the server menu uses
 * %menujoin_status% and must keep working untouched after the rename to
 * ExcelMenuJoin.
 */
public class ExcelMenuJoinExpansion extends PlaceholderExpansion {
    private final ExcelMenuJoin plugin;
    public ExcelMenuJoinExpansion(ExcelMenuJoin plugin) { this.plugin = plugin; }
    @Override public String getIdentifier() { return "menujoin"; }
    @Override public String getAuthor() { return "WinkReyesValley"; }
    @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public String onPlaceholderRequest(Player p, String id) {
        if (p == null) return "";
        if (id.equalsIgnoreCase("status")) return plugin.isDisabled(p) ? "§cOFF" : "§aON";
        return null;
    }
}
