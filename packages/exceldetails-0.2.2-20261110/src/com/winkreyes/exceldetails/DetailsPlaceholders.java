package com.winkreyes.exceldetails;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Menu bridge (0.2.0): the Details Settings menu's checkbox pairs read
 * these placeholders to render ✔ ON / ✘ OFF and re-render in place
 * after a click (MENU-PACKET-DETAILS-SETTINGS.md §0/§2 — the menu's
 * view-requirements compare against 'ON', the house idiom of
 * %menujoin_status% / %silkwarnings_status% / %aimassist_status%).
 *
 *   %details_show_damage%  %details_show_taken%  %details_show_healing%
 *
 * Registered only when PlaceholderAPI is present (softdepend); this
 * class is never loaded otherwise.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class DetailsPlaceholders extends PlaceholderExpansion {

    private final PlayerPrefs prefs;

    public DetailsPlaceholders(PlayerPrefs prefs) { this.prefs = prefs; }

    @Override
    public @NotNull String getIdentifier() { return "details"; }

    @Override
    public @NotNull String getAuthor() { return "Iris / Mike"; }

    @Override
    public @NotNull String getVersion() { return "0.2.2"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "OFF";
        CombatStats.Window w = switch (params.toLowerCase()) {
            case "show_damage" -> CombatStats.Window.DAMAGE_DONE;
            case "show_taken" -> CombatStats.Window.DAMAGE_TAKEN;
            case "show_healing" -> CombatStats.Window.HEALING_DONE;
            default -> null;
        };
        if (w == null) return null;
        return prefs.shows(player.getUniqueId(), w) ? "ON" : "OFF";
    }
}
