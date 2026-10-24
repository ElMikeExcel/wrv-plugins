package com.winkreyes.excelcosmetics;

import com.winkreyes.excelcosmetics.core.Catalog;
import com.winkreyes.excelcosmetics.core.ColorMath;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

/** config.yml -> core Catalog. All validation lives in the core;
 *  this class only translates Bukkit's config tree into plain maps. */
final class ConfigCatalog {

    private ConfigCatalog() {}

    static Catalog from(FileConfiguration cfg) {
        Map<String, Catalog.ColorItem> colors = new LinkedHashMap<>();
        ConfigurationSection colorSec = cfg.getConfigurationSection("colors");
        if (colorSec != null) {
            for (String id : colorSec.getKeys(false)) {
                ConfigurationSection s = colorSec.getConfigurationSection(id);
                if (s == null) continue;
                int rgb = ColorMath.parseHex(s.getString("hex", ""));
                if (rgb < 0) throw new IllegalArgumentException("color " + id + " has an invalid hex");
                colors.put(id, new Catalog.ColorItem(id, s.getString("name", id), rgb,
                        s.getBoolean("for-sale", true)));
            }
        }

        Map<String, Catalog.EffectItem> effects = new LinkedHashMap<>();
        ConfigurationSection effectSec = cfg.getConfigurationSection("effects");
        if (effectSec != null) {
            for (String id : effectSec.getKeys(false)) {
                ConfigurationSection s = effectSec.getConfigurationSection(id);
                if (s == null) continue;
                Catalog.EffectKind kind;
                try {
                    kind = Catalog.EffectKind.valueOf(s.getString("kind", "").trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("effect " + id + " has unknown kind '" + s.getString("kind") + "'");
                }
                int from = s.contains("from") ? ColorMath.parseHex(s.getString("from", "")) : -1;
                int to = s.contains("to") ? ColorMath.parseHex(s.getString("to", "")) : -1;
                if (kind == Catalog.EffectKind.GRADIENT && (from < 0 || to < 0)) {
                    throw new IllegalArgumentException("gradient effect " + id + " has invalid from/to hex");
                }
                effects.put(id, new Catalog.EffectItem(id, s.getString("name", id), kind, from, to));
            }
        }

        Map<String, Catalog.SetItem> sets = new LinkedHashMap<>();
        ConfigurationSection setSec = cfg.getConfigurationSection("sets");
        if (setSec != null) {
            for (String id : setSec.getKeys(false)) {
                ConfigurationSection s = setSec.getConfigurationSection(id);
                if (s == null) continue;
                int rgb = ColorMath.parseHex(s.getString("hex", ""));
                if (rgb < 0) throw new IllegalArgumentException("set " + id + " has an invalid hex");
                sets.put(id, new Catalog.SetItem(id, s.getString("name", id), rgb,
                        s.getString("effect"), s.getString("flair", "")));
            }
        }

        Catalog.Prices prices = new Catalog.Prices(
                cfg.getDouble("prices.color-week", 200),
                cfg.getDouble("prices.set-week", 600),
                cfg.getDouble("prices.effect-week", 400),
                cfg.getDouble("prices.emotes-week", 300),
                cfg.getDouble("prices.four-week-multiplier", 3.5),
                cfg.getDouble("prices.color-own", 15000),
                cfg.getDouble("prices.set-own", 30000),
                cfg.getDouble("prices.effect-own", 25000),
                cfg.getDouble("prices.emotes-own", 20000),
                cfg.getInt("prices.rental-days-per-week", 7));

        Map<String, String> basic = readTokens(cfg.getConfigurationSection("tokens.basic"));
        ConfigurationSection premiumSec = cfg.getConfigurationSection("tokens.premium");
        boolean premiumEnabled = premiumSec != null && premiumSec.getBoolean("enabled", false);
        Map<String, String> premium = premiumSec != null
                ? readTokens(premiumSec.getConfigurationSection("map")) : Map.of();

        return Catalog.build(colors, effects, sets, prices, basic, premium, premiumEnabled);
    }

    private static Map<String, String> readTokens(ConfigurationSection sec) {
        Map<String, String> out = new LinkedHashMap<>();
        if (sec == null) return out;
        for (String key : sec.getKeys(false)) {
            String value = sec.getString(key);
            if (value != null && !value.isEmpty()) out.put(key, value);
        }
        return out;
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
