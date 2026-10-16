// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import com.winkreyes.shops.checkout.Money;
import java.util.*;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

/** Only data-driven items; never console give commands or client-supplied prices. */
public final class SpawnCatalog {
    public record Entry(String id, String name, ItemStack item, int bundleSize, long bundlePrice) {
        public Entry { item = item.clone(); }
        @Override public ItemStack item() { return item.clone(); }
    }
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    public SpawnCatalog(YamlConfiguration yaml) {
        ConfigurationSection root = Objects.requireNonNull(yaml.getConfigurationSection("offers"), "Missing offers");
        for (String id : root.getKeys(false)) {
            if (!id.matches("[a-z0-9_-]{1,40}")) throw new IllegalArgumentException("Invalid offer id");
            ConfigurationSection s = Objects.requireNonNull(root.getConfigurationSection(id));
            Material material = Material.matchMaterial(s.getString("material", ""));
            if (material == null || !material.isItem() || material.isAir()) throw new IllegalArgumentException("Invalid item: " + id);
            int amount = s.getInt("bundle-size");
            if (amount < 1 || amount > 64) throw new IllegalArgumentException("Invalid bundle size: " + id);
            long price = Money.parse(s.getString("bundle-price", ""));
            ItemStack item = new ItemStack(material);
            String enchant = s.getString("enchantment");
            if (enchant != null) {
                Enchantment e = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchant));
                int level = s.getInt("enchantment-level");
                if (!(item.getItemMeta() instanceof EnchantmentStorageMeta meta) || e == null || level < 1 || level > e.getMaxLevel())
                    throw new IllegalArgumentException("Invalid stored enchantment: " + id);
                meta.addStoredEnchant(e, level, false); item.setItemMeta(meta);
            }
            entries.put(id, new Entry(id, s.getString("name", material.name()), item, amount, price));
        }
        if (entries.size() > 45 || entries.isEmpty()) throw new IllegalArgumentException("Catalog must contain 1..45 entries");
    }
    public Collection<Entry> all() { return Collections.unmodifiableCollection(entries.values()); }
    public Entry get(String id) { return entries.get(id); }
}
