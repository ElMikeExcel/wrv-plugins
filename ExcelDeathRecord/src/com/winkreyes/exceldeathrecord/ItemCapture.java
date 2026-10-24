package com.winkreyes.exceldeathrecord;

import com.winkreyes.exceldeathrecord.core.DeathSnapshot;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * ItemStack -> DeathSnapshot.RawItem: the summary fields plus
 * the exact serialized bytes (Paper's serializeAsBytes — the
 * lossless form a later restore deserializes). A serialization
 * failure never loses the item from the record: the summary
 * still lands and the byte payload stays empty, by the core's
 * design.
 */
final class ItemCapture {

    private ItemCapture() {}

    static DeathSnapshot.RawItem of(String slot, ItemStack stack) {
        DeathSnapshot.RawItem ri = new DeathSnapshot.RawItem();
        ri.slot = slot;
        ri.id = stack.getType().getKey().toString();
        ri.count = stack.getAmount();

        ItemMeta meta = stack.hasItemMeta() ? stack.getItemMeta() : null;

        if (stack.getType().getMaxDurability() > 0 && meta instanceof Damageable dmg) {
            ri.damage = dmg.getDamage();
        }

        Map<String, Integer> enchants = new TreeMap<>();
        if (meta instanceof EnchantmentStorageMeta stored && stored.hasStoredEnchants()) {
            stored.getStoredEnchants().forEach((e, lvl) -> enchants.put(e.getKey().toString(), lvl));
        } else {
            stack.getEnchantments().forEach((e, lvl) -> enchants.put(e.getKey().toString(), lvl));
        }
        ri.enchants = new LinkedHashMap<>(enchants);

        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            ri.customName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }

        try {
            ri.bytes = stack.serializeAsBytes();
        } catch (Throwable t) {
            ri.bytes = null;
        }
        return ri;
    }
}
