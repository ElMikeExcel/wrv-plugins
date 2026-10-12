package com.winkreyesvalley.menujoin;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class MenuJoinExpansion extends PlaceholderExpansion {
    private final MenuJoin plugin;
    public MenuJoinExpansion(MenuJoin plugin) { this.plugin = plugin; }
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
