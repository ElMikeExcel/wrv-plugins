package com.winkreyes.excelskillspanel.provider.wrv;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

/**
 * The ExcelMagicLevel value its PlaceholderAPI surface does not
 * expose in 0.2.0 (the optional placeholder additions in DESIGN
 * §7.4 were not built): the reward table's regen / cost
 * per-level fractions, read from ExcelMagicLevel's live config.
 * Fail-soft; a failed read omits, never substitutes.
 *
 * 0.2.2 (Item 13): the totalCasts read that used to live here is
 * REMOVED. ExcelMagicLevel's PlayerRecord.totalCasts is a
 * different counter from the rank-driving credited-cast count
 * (694 vs 1,215 for Mike) — the panel reads the credited count
 * from MagicGate's ledger now (MagicGateReads) and never touches
 * totalCasts again.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class MagicLevelStoreReads {
    private MagicLevelStoreReads() {}

    /** The reward table's per-level fractions from ExcelMagicLevel's
     *  live config: [manaRegenerationBoost, costReduction], or null
     *  when the config cannot be read. */
    public static double[] perLevelBoosts() {
        Plugin eml = emlPlugin();
        if (eml == null) return null;
        try {
            File file = new File(eml.getDataFolder(), "config.yml");
            if (!file.isFile()) return null;
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            double regen = yaml.getDouble("rewards.per-level.mana-regeneration-boost", Double.NaN);
            double cost = yaml.getDouble("rewards.per-level.cost-reduction", Double.NaN);
            if (Double.isNaN(regen) || Double.isNaN(cost)) return null;
            return new double[]{regen, cost};
        } catch (Throwable t) {
            return null;
        }
    }

    private static Plugin emlPlugin() {
        Plugin eml = Bukkit.getPluginManager().getPlugin("ExcelMagicLevel");
        return eml != null && eml.isEnabled() ? eml : null;
    }
}
