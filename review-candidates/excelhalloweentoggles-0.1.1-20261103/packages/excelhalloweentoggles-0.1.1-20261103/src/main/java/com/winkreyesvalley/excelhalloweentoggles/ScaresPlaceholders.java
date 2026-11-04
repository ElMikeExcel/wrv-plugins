package com.winkreyesvalley.excelhalloweentoggles;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * ExcelHalloweenToggles — PlaceholderAPI bridge for the menu.
 *
 * %halloweentoggles_&lt;feature&gt;% returns ON / OFF, which is what the
 * ExcelMenu submenu's state-variant buttons read (the same pattern the
 * Aim Assist and Silk Touch toggles use). Registered only when
 * PlaceholderAPI is present; without it, /scares still carries the full
 * function and the menu shows its "unavailable" variant, by design.
 */
public final class ScaresPlaceholders extends PlaceholderExpansion {

    private final String version;
    private final FeatureRegistry registry;
    private final ToggleStore store;

    public ScaresPlaceholders(String version, FeatureRegistry registry, ToggleStore store) {
        this.version = version;
        this.registry = registry;
        this.store = store;
    }

    @Override
    public String getIdentifier() {
        return "halloweentoggles";
    }

    @Override
    public String getAuthor() {
        return "Excel";
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return null;
        }
        FeatureDef feature = registry.resolve(params);
        if (feature == null) {
            return null;
        }
        return store.isEnabled(player.getUniqueId(), feature.key()) ? "ON" : "OFF";
    }
}
