package com.winkreyes.excelemfoodheal;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Material;

/**
 * Vanilla food saturation values and the D162 budget formula.
 *
 * <p>Bukkit exposes no saturation getter for plain items, so the
 * vanilla values live in this table (Minecraft wiki "Food" saturation
 * column). A config override map may replace or extend entries at
 * runtime; materials with no entry get no budget (custom items are
 * out of scope for v1). Cake never fires PlayerItemConsumeEvent
 * (it is eaten from a placed block) and is absent on purpose.
 *
 * <p>Formula (all constants config-driven, defaults shown):
 * totalPercent = clamp(saturation * 0.75, 1.0, 10.0).
 * Steak (12.8 sat) -> 9.6%; bread (6.0) -> 4.5%; carrot (3.6) -> 2.7%;
 * floor foods (<= 1.33 sat) -> the 1.0% minimum, which the instant
 * grant consumes entirely (instant-only, no HoT remainder).
 */
public final class FoodTable {

    private static final Map<Material, Double> VANILLA = new EnumMap<>(Material.class);

    static {
        // Saturation restored, vanilla values.
        VANILLA.put(Material.GOLDEN_CARROT, 14.4);
        VANILLA.put(Material.COOKED_BEEF, 12.8);
        VANILLA.put(Material.COOKED_PORKCHOP, 12.8);
        VANILLA.put(Material.RABBIT_STEW, 12.0);
        VANILLA.put(Material.GOLDEN_APPLE, 9.6);
        VANILLA.put(Material.ENCHANTED_GOLDEN_APPLE, 9.6);
        VANILLA.put(Material.COOKED_MUTTON, 9.6);
        VANILLA.put(Material.COOKED_SALMON, 9.6);
        VANILLA.put(Material.COOKED_CHICKEN, 7.2);
        VANILLA.put(Material.SUSPICIOUS_STEW, 7.2);
        VANILLA.put(Material.MUSHROOM_STEW, 7.2);
        VANILLA.put(Material.BEETROOT_SOUP, 7.2);
        VANILLA.put(Material.BREAD, 6.0);
        VANILLA.put(Material.BAKED_POTATO, 6.0);
        VANILLA.put(Material.COOKED_COD, 6.0);
        VANILLA.put(Material.COOKED_RABBIT, 6.0);
        VANILLA.put(Material.PUMPKIN_PIE, 4.8);
        VANILLA.put(Material.CARROT, 3.6);
        VANILLA.put(Material.SPIDER_EYE, 3.2);
        VANILLA.put(Material.APPLE, 2.4);
        VANILLA.put(Material.CHORUS_FRUIT, 2.4);
        VANILLA.put(Material.BEEF, 1.8);
        VANILLA.put(Material.PORKCHOP, 1.8);
        VANILLA.put(Material.RABBIT, 1.8);
        VANILLA.put(Material.BEETROOT, 1.2);
        VANILLA.put(Material.CHICKEN, 1.2);
        VANILLA.put(Material.MUTTON, 1.2);
        VANILLA.put(Material.POISONOUS_POTATO, 1.2);
        VANILLA.put(Material.HONEY_BOTTLE, 1.2);
        VANILLA.put(Material.MELON_SLICE, 1.2);
        VANILLA.put(Material.ROTTEN_FLESH, 0.8);
        VANILLA.put(Material.DRIED_KELP, 0.6);
        VANILLA.put(Material.POTATO, 0.6);
        VANILLA.put(Material.COD, 0.4);
        VANILLA.put(Material.SALMON, 0.4);
        VANILLA.put(Material.COOKIE, 0.4);
        VANILLA.put(Material.SWEET_BERRIES, 0.4);
        VANILLA.put(Material.GLOW_BERRIES, 0.4);
        VANILLA.put(Material.PUFFERFISH, 0.2);
        VANILLA.put(Material.TROPICAL_FISH, 0.2);
    }

    private FoodTable() {}

    /** Vanilla saturation for a material, or null when not a known food. */
    public static Double vanillaSaturation(Material material) {
        return VANILLA.get(material);
    }

    /** D162 budget formula: clamp(saturation * multiplier, min, max), all in % of max health. */
    public static double totalPercent(double saturation, double multiplier, double min, double max) {
        double raw = saturation * multiplier;
        if (raw < min) return min;
        if (raw > max) return max;
        return raw;
    }
}
