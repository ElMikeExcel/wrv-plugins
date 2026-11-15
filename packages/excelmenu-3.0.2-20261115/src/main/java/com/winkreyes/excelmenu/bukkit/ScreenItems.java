// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Small shared builders for the ported screens (DESIGN-2.0 §2.4):
 * item stacks in the house style (legacy-section components over
 * Text.colorize, exactly as MenuService.buildStack), and player
 * display names as plain text — the visible name (Essentials nick
 * included), colour codes stripped, which is what the old screens
 * computed with ChatColor.stripColor(getDisplayName()).
 */
final class ScreenItems {

    private ScreenItems() {}

    static Component legacy(String raw) {
        return LegacyComponentSerializer.legacySection().deserialize(Text.colorize(raw));
    }

    static ItemStack item(Material material, String name, String... loreLines) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (name != null) meta.displayName(legacy(name));
            if (loreLines.length > 0) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreLines) lore.add(legacy(line));
                meta.lore(lore);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** The player's visible name as plain text (nick when set). */
    static String displayName(Player player) {
        String s = PlainTextComponentSerializer.plainText()
                .serialize(player.displayName());
        return s == null || s.isEmpty() ? player.getName() : s;
    }

    /** Plain-text form of a display name (already plain in 2.0). */
    static String stripColor(String s) {
        return s == null ? "" : s.replaceAll("§.", "");
    }
}
