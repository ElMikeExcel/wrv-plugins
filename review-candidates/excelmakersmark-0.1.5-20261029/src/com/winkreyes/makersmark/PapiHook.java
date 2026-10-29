/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/** %makersmark_mark% and %makersmark_stats% (ON / OFF) for the compass menu toggle buttons. Loaded only when PlaceholderAPI is on. */
final class PapiHook extends PlaceholderExpansion {
    private final ExcelMakersMark plugin;
    private PapiHook(ExcelMakersMark plugin) { this.plugin = plugin; }

    static boolean register(ExcelMakersMark plugin) { return new PapiHook(plugin).register(); }

    @Override public String getIdentifier() { return "makersmark"; }
    @Override public String getAuthor() { return "WinkReyesValley"; }
    @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";
        Prefs.P p = plugin.prefs().get(player.getUniqueId());
        return switch (params.toLowerCase()) {
            case "mark" -> p.mark && plugin.markEnabled() ? "ON" : "OFF";
            case "stats" -> p.stats && plugin.statsEnabled() ? "ON" : "OFF";
            default -> null;
        };
    }
}
