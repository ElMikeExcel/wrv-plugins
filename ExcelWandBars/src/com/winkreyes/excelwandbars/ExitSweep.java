package com.winkreyes.excelwandbars;

import com.elmakers.mine.bukkit.wand.WandInventory;
import com.winkreyes.excelwandbars.core.ExitSweepPlan;
import com.winkreyes.excelwandbars.core.LoadoutText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * 0.3.6: the exit-slot relocation sweep (Nov 1, 2026 investigation, Report 2).
 * Runs from TapBarHook.tryOpen's tap-bar branch, after the layout repair and
 * before the wand opens — the pages are authoritative there, and the opening
 * guard reads these same pages, so freeing an occupied exit column here is
 * what unblocks a refused open. (The SpellInventoryEvent callback itself is
 * never mutated from: TapBarHook's rule is no repairs inside Magic's opening
 * callback.)
 *
 * Destination: the wand's stored inventory via Wand.addToStoredInventory
 * whenever one exists (it is where the player's real items wait while the
 * bar is up, and where a stuck stored inventory still returns from). Before
 * an open there is normally no stored inventory yet, so the fallback is a
 * plain addItem into the player's live inventory — the very contents Magic
 * is about to store, so the item lands in the same place by the time the
 * bar is up. Item preservation is absolute: a stack the sink will not fully
 * take is only reduced by the part that provably moved (the handed clone's
 * leftover amount), and a stack that cannot move at all stays exactly
 * where it is. Nothing is ever deleted.
 */
final class ExitSweep {
    static final class Result {
        int scanned, foreign, movedStacks, leftBehind;
        boolean ran;
    }

    private ExitSweep() {}

    static Result run(ExcelWandBars plugin, Player p, com.elmakers.mine.bukkit.api.wand.Wand wandApi, MagicHook mh) {
        Result r = new Result();
        if (!(wandApi instanceof com.elmakers.mine.bukkit.wand.Wand impl)) return r;
        List<WandInventory> bars = impl.getHotbars();
        if (bars == null || bars.isEmpty()) return r;
        r.ran = true;
        Logger log = plugin.getLogger();
        boolean stored = impl.hasStoredInventory();
        List<String> movedNames = new ArrayList<>();
        String partialName = null;
        for (int b = 0; b < bars.size(); b++) {
            WandInventory bar = bars.get(b);
            if (bar == null || bar.getSize() != 8) continue;
            for (int i = 0; i < 8; i++) {
                ItemStack item = bar.getItem(i);
                r.scanned++;
                boolean empty = FlexibleInventoryGuard.empty(item);
                boolean spell = !empty && mh != null && mh.spellKeyOf(item) != null;
                if (!ExitSweepPlan.foreign(empty, spell)) continue;
                r.foreign++;
                String name = nameOf(item);
                int amount = item.getAmount();
                ItemStack clone = item.clone();
                boolean accepted = stored ? impl.addToStoredInventory(clone)
                        : p.getInventory().addItem(clone).isEmpty();
                int left = ExitSweepPlan.leftover(amount, accepted, clone.getAmount());
                if (left <= 0) {
                    bar.setItem(i, null);
                    movedNames.add(name);
                    r.movedStacks++;
                    log.info("Exit sweep: moved " + name + " x" + amount + " out of " + p.getName()
                            + "'s spell bar (page " + (b + 1) + ", slot " + (i + 1) + ") into "
                            + (stored ? "the wand's stored inventory" : "their inventory"));
                } else if (left < amount) {
                    ItemStack rest = item.clone();
                    rest.setAmount(left);
                    bar.setItem(i, rest);
                    partialName = name;
                    r.leftBehind++;
                    log.info("Exit sweep: moved " + (amount - left) + " of " + name + " x" + amount + " out of "
                            + p.getName() + "'s spell bar (page " + (b + 1) + ", slot " + (i + 1)
                            + "); " + left + " stayed (no room)");
                } else {
                    r.leftBehind++;
                    log.info("Exit sweep: " + name + " x" + amount + " is sitting in " + p.getName()
                            + "'s spell bar (page " + (b + 1) + ", slot " + (i + 1)
                            + ") and could not be moved (no room); left in place");
                }
            }
        }
        if (r.movedStacks > 0 || partialName != null) {
            String line = LoadoutText.sweptLine(movedNames, partialName, r.leftBehind > 0);
            if (line != null) p.sendMessage(Component.text(line, NamedTextColor.GRAY));
        }
        return r;
    }

    /** The item's own display name when it has one, else a readable material name. */
    private static String nameOf(ItemStack item) {
        try {
            if (item.hasItemMeta() && item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
                String s = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
                if (s != null && !s.isBlank()) return s.trim();
            }
        } catch (Throwable ignored) { }
        String n = item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return n.isEmpty() ? n : Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }
}
