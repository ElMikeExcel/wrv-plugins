package com.winkreyes.excelguildgate;

import com.winkreyes.guildgate.api.GuildGateAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * PlaceholderAPI expansion (identifier "guildgate"), registered only when
 * PlaceholderAPI is installed (softdepend). Powers the DeluxeMenus
 * locked/open guild button variants in server_menu.yml:
 *
 *   %guildgate_unlocked%     -> "true" / "false"
 *   %guildgate_seals_broken% -> enabled seals broken so far
 *   %guildgate_seals_total%  -> enabled seals required
 *
 * Styled after ExcelMenuJoinExpansion (the house pattern).
 */
public class GuildGateExpansion extends PlaceholderExpansion {

    private final ExcelGuildGate plugin;

    public GuildGateExpansion(ExcelGuildGate plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "guildgate";
    }

    @Override
    public String getAuthor() {
        return "Argus";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // survive /papi reload
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        switch (params.toLowerCase(Locale.ROOT)) {
            case "unlocked":
                return String.valueOf(GuildGateAPI.isUnlocked());
            case "seals_broken":
                return String.valueOf(GuildGateAPI.sealsBrokenCount());
            case "seals_total":
                return String.valueOf(GuildGateAPI.sealsTotalCount());
            default:
                return null;
        }
    }
}
