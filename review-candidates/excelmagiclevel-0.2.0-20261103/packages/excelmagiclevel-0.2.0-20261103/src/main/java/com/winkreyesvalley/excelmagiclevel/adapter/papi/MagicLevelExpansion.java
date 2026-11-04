package com.winkreyesvalley.excelmagiclevel.adapter.papi;

import com.winkreyesvalley.excelmagiclevel.core.Boost;
import com.winkreyesvalley.excelmagiclevel.core.Curve;
import com.winkreyesvalley.excelmagiclevel.core.Percents;
import com.winkreyesvalley.excelmagiclevel.core.RewardTable;
import com.winkreyesvalley.excelmagiclevel.store.PlayerRecord;
import com.winkreyesvalley.excelmagiclevel.store.PlayerStore;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/**
 * ExcelMagicLevel — the PlaceholderAPI surface (SPEC §0.3/§6.1):
 * {@code %excelmagiclevel_level%}, {@code %excelmagiclevel_xp%} (XP into
 * the current level), {@code %excelmagiclevel_next%} (XP still needed
 * for the next level; 0 at the cap), {@code %excelmagiclevel_total_xp%}
 * (lifetime), {@code %excelmagiclevel_mana_boost%} (current mana-pool
 * boost as a percent number). This is the placeholder ExcelMenu 2.0's
 * guarded-entry contract consumes as a {@code status-placeholder:}.
 *
 * <p>Adapter law (SPEC §9.2): this class is only ever loaded when the
 * PlaceholderAPI plugin is present — the plugin checks first and treats
 * any registration failure as "expansion skipped", everything else
 * unaffected. All values derive from the store + curve on read, so a
 * curve retune is reflected immediately.</p>
 */
public final class MagicLevelExpansion extends PlaceholderExpansion {
    private final PlayerStore store;
    private final Curve curve;
    private final RewardTable rewards;
    private final String version;

    public MagicLevelExpansion(PlayerStore store, Curve curve, RewardTable rewards, String version) {
        this.store = store;
        this.curve = curve;
        this.rewards = rewards;
        this.version = version;
    }

    @Override
    public String getIdentifier() {
        return "excelmagiclevel";
    }

    @Override
    public String getAuthor() {
        return "Excel (Wink Reyes Valley)";
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
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return null;
        PlayerRecord record = store.find(player.getUniqueId());
        long totalXp = record == null ? 0 : record.totalXp();
        int level = curve.levelFor(totalXp);
        return switch (params.toLowerCase()) {
            case "level" -> String.valueOf(level);
            case "xp" -> String.valueOf(curve.xpIntoLevel(totalXp));
            case "next" -> String.valueOf(curve.xpToNextLevel(totalXp));
            case "total_xp" -> String.valueOf(totalXp);
            case "mana_boost" -> {
                Boost boost = rewards.boostAt(level);
                yield Percents.format(boost.manaMaxBoost());
            }
            default -> null;
        };
    }
}
