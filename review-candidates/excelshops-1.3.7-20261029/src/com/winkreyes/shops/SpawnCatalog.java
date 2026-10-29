// Excel signature - WRV maintained source; original authorship and license notices preserved.
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
    /** Default offer cap when config sets none. Sized for the recorded catalog wave (~200 offers). */
    public static final int DEFAULT_MAX_ENTRIES = 250;
    /** Highest cap the config may select; a sanity bound, not a stock target. */
    public static final int ABSOLUTE_MAX_ENTRIES = 1000;
    public record Entry(String id, String name, ItemStack item, int bundleSize, long bundlePrice, String category) {
        public Entry { item = item.clone(); }
        @Override public ItemStack item() { return item.clone(); }
    }
    /** Bukkit-free view of one offer, so the mobile (Bedrock) shop can derive from the same data. */
    public record OfferData(String id, String name, int bundleSize, long bundlePrice, String category) { }
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    public SpawnCatalog(YamlConfiguration yaml) { this(yaml, DEFAULT_MAX_ENTRIES); }
    public SpawnCatalog(YamlConfiguration yaml, int maxEntries) {
        requireMaxEntries(maxEntries);
        ConfigurationSection root = Objects.requireNonNull(yaml.getConfigurationSection("offers"), "Missing offers");
        for (String id : root.getKeys(false)) {
            if (!id.matches("[a-z0-9_-]{1,40}")) throw new IllegalArgumentException("Invalid offer id");
            ConfigurationSection s = Objects.requireNonNull(root.getConfigurationSection(id));
            Material material = Material.matchMaterial(s.getString("material", ""));
            if (material == null || !material.isItem() || material.isAir()) throw new IllegalArgumentException("Invalid item: " + id);
            int amount = s.getInt("bundle-size");
            if (amount < 1 || amount > 64) throw new IllegalArgumentException("Invalid bundle size: " + id);
            long price = Money.parse(s.getString("bundle-price", ""));
            String category = s.getString("category");
            if (category != null && !category.matches("[a-z0-9_-]{1,40}")) throw new IllegalArgumentException("Invalid category: " + id);
            ItemStack item = new ItemStack(material);
            String enchant = s.getString("enchantment");
            if (enchant != null) {
                Enchantment e = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchant));
                int level = s.getInt("enchantment-level");
                if (!(item.getItemMeta() instanceof EnchantmentStorageMeta meta) || e == null || level < 1 || level > e.getMaxLevel())
                    throw new IllegalArgumentException("Invalid stored enchantment: " + id);
                meta.addStoredEnchant(e, level, false); item.setItemMeta(meta);
            }
            entries.put(id, new Entry(id, s.getString("name", material.name()), item, amount, price, category));
        }
        requireValidSize(entries.size(), maxEntries);
    }
    /** The single size rule for a parsed catalog: at least one offer, never above the configured cap. */
    static void requireValidSize(int size, int maxEntries) {
        if (size < 1 || size > maxEntries) throw new IllegalArgumentException("Catalog must contain 1.." + maxEntries + " entries");
    }
    /** Validates a configured cap (config key max-catalog-offers) before it reaches the catalog. */
    public static int requireMaxEntries(int configured) {
        if (configured < 1 || configured > ABSOLUTE_MAX_ENTRIES)
            throw new IllegalArgumentException("max-catalog-offers must be 1.." + ABSOLUTE_MAX_ENTRIES);
        return configured;
    }
    public Collection<Entry> all() { return Collections.unmodifiableCollection(entries.values()); }
    public Entry get(String id) { return entries.get(id); }
    /** Every offer as plain data, in catalog order. The mobile shop derives from this, never a second price list. */
    public List<OfferData> offerData() {
        List<OfferData> out = new ArrayList<>(entries.size());
        for (Entry e : entries.values()) out.add(new OfferData(e.id(), e.name(), e.bundleSize(), e.bundlePrice(), e.category()));
        return List.copyOf(out);
    }
}
