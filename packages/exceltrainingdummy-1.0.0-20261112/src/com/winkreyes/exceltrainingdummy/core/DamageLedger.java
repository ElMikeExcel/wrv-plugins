package com.winkreyes.exceltrainingdummy.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * One attacker's damage against one dummy, split by source category.
 * Categories are what a bare Bukkit damage event can honestly prove:
 * the damager and the cause. Individual spell NAMES are not carried
 * by damage events and neither ExcelDetails 0.2.2 nor Magic exposes a
 * public per-target segment API to this plugin, so the report breaks
 * down by category (melee / spells / projectiles / summons / other)
 * rather than inventing spell attribution. See BUILD-RECORD.md.
 */
public final class DamageLedger {

    public enum Category {
        MELEE("Melee"),
        SPELLS("Spells"),
        PROJECTILES("Projectiles"),
        SUMMONS("Summons"),
        OTHER("Other");

        public final String label;
        Category(String label) { this.label = label; }
    }

    private final EnumMap<Category, Double> byCategory = new EnumMap<>(Category.class);
    private double total;

    public void add(Category category, double amount) {
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) return;
        byCategory.merge(category, amount, Double::sum);
        total += amount;
    }

    public double total() { return total; }

    public double of(Category c) { return byCategory.getOrDefault(c, 0.0); }

    public Map<Category, Double> snapshot() { return new EnumMap<>(byCategory); }

    public void clear() {
        byCategory.clear();
        total = 0;
    }
}
