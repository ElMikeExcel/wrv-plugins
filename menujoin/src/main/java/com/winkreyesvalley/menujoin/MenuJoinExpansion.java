package com.winkreyesvalley.menujoin;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/** Provides %menujoin_status% -> ON/OFF for the auto-open-on-join toggle. */
public class MenuJoinExpansion extends PlaceholderExpansion {

    private final MenuJoin plugin;

    public MenuJoinExpansion(MenuJoin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "menujoin";
    }

    @Override
    public String getAuthor() {
        return "WinkReyesValley";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }
        if (params.equalsIgnoreCase("status")) {
            return plugin.isDisabled(player) ? "§cOFF" : "§aON";
        }
        return null;
    }
}
