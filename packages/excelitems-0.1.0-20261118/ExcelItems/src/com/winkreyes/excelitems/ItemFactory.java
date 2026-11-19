package com.winkreyes.excelitems;

import com.winkreyes.excelitems.core.ItemDefinition;
import com.winkreyes.excelitems.core.ItemRegistry;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Bukkit mint + identify layer. Minting stamps BOTH identities:
 * the primary mark (excelitems:id, INTEGER) that every plugin reads
 * through the API, and — when the definition carries one — the legacy
 * mark its owning plugin has always used (same namespace, key, BYTE 1
 * value, plus the legacy lore-version stamp), so a registry-minted
 * item is indistinguishable to the legacy owner. Identification reads
 * the primary mark first; only an item with NO primary mark falls
 * back to the legacy marks — that is how already-held pre-registry
 * items stay recognized.
 */
public final class ItemFactory {

    /** The primary identity key: namespace "excelitems", key "id". */
    public static final String ID_KEY = "id";

    private final ExcelItems plugin;
    private final ItemRegistry registry;
    private final NamespacedKey primaryKey;

    ItemFactory(ExcelItems plugin, ItemRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.primaryKey = new NamespacedKey(plugin, ID_KEY);
    }

    public NamespacedKey primaryKey() {
        return primaryKey;
    }

    /** Mint {@code amount} (1..64) of one definition. */
    public ItemStack create(ItemDefinition def, int amount) {
        if (amount < 1 || amount > 64)
            throw new IllegalArgumentException("amount must be 1..64: " + amount);
        Material material = Material.valueOf(def.material());
        ItemStack stack = new ItemStack(material, amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;
        meta.setDisplayName(translate(def.displayName()));
        List<String> lore = new ArrayList<>(def.lore().size());
        for (String line : def.lore()) lore.add(translate(line));
        meta.setLore(lore);
        if (def.glint()) meta.setEnchantmentGlintOverride(true);
        if (meta instanceof PotionMeta potion) {
            if (def.potionColorRgb() != null) potion.setColor(Color.fromRGB(def.potionColorRgb()));
            if (def.basePotionType() != null) {
                try {
                    potion.setBasePotionType(PotionType.valueOf(def.basePotionType()));
                } catch (IllegalArgumentException unknown) {
                    plugin.getLogger().warning("Unknown base potion type '"
                            + def.basePotionType() + "' for " + def.key() + "; minting without it.");
                }
            }
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(primaryKey, PersistentDataType.INTEGER, def.id());
        if (def.hasLegacyMark()) {
            NamespacedKey legacy = new NamespacedKey(def.legacyNamespace(), def.legacyKey());
            pdc.set(legacy, PersistentDataType.BYTE, (byte) 1);
            if (def.legacyLoreKey() != null) {
                NamespacedKey loreKey = new NamespacedKey(def.legacyNamespace(), def.legacyLoreKey());
                pdc.set(loreKey, PersistentDataType.INTEGER, def.loreVersion());
            }
        }
        stack.setItemMeta(meta);
        return stack;
    }

    /** The definition this stack identifies as, from its own data. */
    public Optional<ItemDefinition> identify(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return Optional.empty();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return Optional.empty();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer primary = pdc.has(primaryKey, PersistentDataType.INTEGER)
                ? pdc.get(primaryKey, PersistentDataType.INTEGER) : null;
        if (primary != null) return registry.identify(primary, null, null);
        for (ItemDefinition def : registry.all()) {
            if (!def.hasLegacyMark()) continue;
            NamespacedKey legacy = new NamespacedKey(def.legacyNamespace(), def.legacyKey());
            if (pdc.has(legacy, PersistentDataType.BYTE)) {
                return registry.identifyLegacy(def.legacyNamespace(), def.legacyKey());
            }
        }
        return Optional.empty();
    }

    private static String translate(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
