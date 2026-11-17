// ExcelSilkWatch — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.silkstorage;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/** Same feature-owned UUID state used by command and observer. */
final class PapiHook extends PlaceholderExpansion {
    private final ExcelSilkWatch plugin;
    private PapiHook(ExcelSilkWatch plugin) { this.plugin = plugin; }
    static PapiHook install(ExcelSilkWatch plugin) {
        PapiHook hook = new PapiHook(plugin);
        return hook.register() ? hook : null;
    }
    @Override public String getIdentifier() { return "silkwarnings"; }
    @Override public String getAuthor() { return "WinkReyesValley"; }
    @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public String onRequest(OfflinePlayer player, String params) {
        return player != null && params.equalsIgnoreCase("status") ? plugin.warningStatus(player.getUniqueId()) : null;
    }
}
