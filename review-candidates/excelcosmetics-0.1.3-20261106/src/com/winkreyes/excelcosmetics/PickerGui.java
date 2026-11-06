package com.winkreyes.excelcosmetics;

import com.winkreyes.excelcosmetics.core.Catalog;
import com.winkreyes.excelcosmetics.core.Kind;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.List;

/** The Java door of the picker: a plain inventory GUI. */
final class PickerGui {

    static final class Holder implements InventoryHolder {
        final List<PickerModel.Entry> entries;
        private Inventory inventory;
        Holder(List<PickerModel.Entry> entries) { this.entries = entries; }
        @Override public Inventory getInventory() { return inventory; }
    }

    private PickerGui() {}

    static void open(ExcelCosmetics plugin, Player player, List<PickerModel.Entry> entries) {
        Holder holder = new Holder(entries);
        int size = Math.max(9, Math.min(54, ((entries.size() + 8) / 9) * 9));
        if (entries.isEmpty()) size = 9;
        Inventory inv = Bukkit.createInventory(holder, size, Component.text("Cosmetics"));
        holder.inventory = inv;
        if (entries.isEmpty()) {
            ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = pane.getItemMeta();
            meta.displayName(Component.text("Nothing here yet")
                    .color(TextColor.color(0xAAAAAA)).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Colors, sets and effects are sold at the spawn shop.")
                    .color(TextColor.color(0x888888)).decoration(TextDecoration.ITALIC, false)));
            pane.setItemMeta(meta);
            inv.setItem(4, pane);
        } else {
            for (int i = 0; i < entries.size() && i < size; i++) {
                inv.setItem(i, itemFor(entries.get(i)));
            }
        }
        player.openInventory(inv);
    }

    private static ItemStack itemFor(PickerModel.Entry e) {
        Material material;
        switch (e.kind()) {
            case COLOR -> material = Material.LEATHER_CHESTPLATE;
            case SET -> material = switch (e.id()) {
                case "tank" -> Material.SHIELD;
                case "healer" -> Material.GOLDEN_APPLE;
                case "warlock" -> Material.BLAZE_ROD;
                case "firebender" -> Material.MAGMA_CREAM;
                case "storm" -> Material.LIGHTNING_ROD;
                default -> Material.TOTEM_OF_UNDYING;
            };
            case EFFECT -> material = e.id().equals("sparkle-accents") ? Material.NETHER_STAR : Material.NAME_TAG;
            default -> material = Material.WRITABLE_BOOK;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof LeatherArmorMeta leather && e.rgb() != null) {
            leather.setColor(Color.fromRGB(e.rgb()));
        }
        Component name = Component.text(e.title()).decoration(TextDecoration.ITALIC, false);
        if (e.rgb() != null) name = name.color(TextColor.color(e.rgb()));
        meta.displayName(name);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(e.detail()).color(TextColor.color(0xBBBBBB))
                .decoration(TextDecoration.ITALIC, false));
        if (e.equipped()) {
            lore.add(Component.text("Equipped").color(TextColor.color(0x7DFF9E))
                    .decoration(TextDecoration.ITALIC, false));
        } else if (e.equippable()) {
            lore.add(Component.text("Click to equip").color(TextColor.color(0x888888))
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /** Click handling: equip the clicked entry. Called by the listener. */
    static void handleClick(ExcelCosmetics plugin, Player player, Holder holder, int slot) {
        if (slot < 0 || slot >= holder.entries.size()) return;
        PickerModel.Entry e = holder.entries.get(slot);
        if (!e.equippable()) {
            player.sendMessage(Component.text("The Emote Pack is always on while you hold it.")
                    .color(TextColor.color(0xBBBBBB)));
            return;
        }
        plugin.equipAndSave(player, e.kind(), e.id());
        player.closeInventory();
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
