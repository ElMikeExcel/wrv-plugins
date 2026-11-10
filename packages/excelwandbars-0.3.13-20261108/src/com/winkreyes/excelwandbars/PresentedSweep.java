package com.winkreyes.excelwandbars;

import com.winkreyes.excelwandbars.core.ExitSweepPlan;
import com.winkreyes.excelwandbars.core.LoadoutText;
import com.winkreyes.excelwandbars.core.SlotGuardPlan;
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
 * 0.3.12: the while-open companion of the 0.3.6 exit sweep (ExitSweep).
 * ExitSweep relocates foreign items out of the wand's SAVED pages when
 * the bar opens; this sweep relocates them out of the LIVE presented
 * inventory — player-inventory slots 0–35 — while the bar is open: the
 * state the Nov 7, 2026 fight intake convicted. With the bar up, a
 * ground pickup or a programmatic give fills hotbar-first, the exit
 * slot is the first empty slot by design, and the stack then sits in a
 * protected wand slot until the next open, deadening the empty-tap
 * exit and risking being baked into the saved page by the next flip.
 *
 * Scope is the whole presented inventory, and that is safe precisely
 * because of how Magic's inventory mode works: while the bar is open
 * the player's real items are NOT in slots 0–35 at all — they wait in
 * the wand's stored inventory (this sweep does not even run when there
 * is no stored inventory) — and every slot of the presentation is wand
 * paint: the hotbar row is the current page, the grid is the spell
 * list. A real item found anywhere in 0–35 while the bar is open is
 * therefore foreign by construction, wherever it landed. Landing is
 * NOT hotbar-only in practice: Bukkit's addItem fills the first empty
 * storage slot in order, so when the row is full (the normal state of
 * a working bar: icons plus the wand) a give overflows into the grid —
 * the first 0.3.12 rig pass caught exactly that (a 7-arrow give at
 * grid slot 16, row full, never swept by a row-only scan; the same
 * leftover then failed the pickup case's presentation-wide census).
 * The click/drag/shift/swap blocks (TapBarHook) stop a player's own
 * gestures from putting a foreign stack there; this sweep is the net
 * for the paths no gesture travels (gives, pickups, merchant results).
 * Armor and the offhand are not storage slots and are never scanned.
 *
 * Destination: the wand's stored inventory via
 * Wand.addToStoredInventory — where the player's real items already
 * wait while the bar is up, so a relocated stack is simply a real item
 * that arrived early; it comes back with everything else when the bar
 * closes. When there is no stored inventory there is nowhere safe to
 * move a stack (the live inventory IS the presentation), so the sweep
 * does not run. Item preservation is absolute, exactly as in
 * ExitSweep: a stack the sink will not fully take is only reduced by
 * the part that provably moved (ExitSweepPlan.leftover), and a stack
 * that cannot move at all stays exactly where it is. Nothing is ever
 * deleted.
 *
 * Exclusions (SlotGuardPlan.foreign): spell icons (the presentation's
 * own content), the pager buttons (PagerHook owns them; its sweep
 * removes them before saves), wand items, and empty slots.
 */
final class PresentedSweep {
    static final class Result {
        int scanned, foreign, movedStacks, leftBehind;
        boolean ran;
        /** The bar's paint is not in place yet (see run): nothing was judged. */
        boolean unpainted;
    }

    private PresentedSweep() {}

    /**
     * @param verbose true for event-driven sweeps (pickup, pre-flip):
     *                per-stack log lines plus the one chat line. The
     *                10-tick poll passes false — it runs constantly
     *                while a bar is open, so it counts silently and the
     *                event-driven passes do the reporting.
     */
    static Result run(ExcelWandBars plugin, Player p, com.elmakers.mine.bukkit.api.wand.Wand wandApi,
                      MagicHook mh, boolean verbose) {
        Result r = new Result();
        if (!(wandApi instanceof com.elmakers.mine.bukkit.wand.Wand impl)) return r;
        if (!impl.hasStoredInventory()) return r;
        PagerHook pager = plugin.pager();
        Logger log = plugin.getLogger();
        List<String> movedNames = new ArrayList<>();
        String partialName = null;
        // Painted-presentation invariant (rig-caught, 0.3.12): Magic's
        // store-and-paint lands a tick or more after the wand first
        // reads as open — longer on a relog restore — and until it
        // lands the inventory still shows the player's REAL items,
        // which are indistinguishable from foreign ones. A presentation
        // with no spell icon anywhere in it is not painted yet: judge
        // nothing. (TapBarHook's settle gate is the timer belt; this is
        // the exact check.) A genuinely spell-less bar has no icons to
        // protect and its exit slot is re-painted by the exit painter,
        // so standing down there costs nothing.
        boolean painted = false;
        for (int slot = 0; slot < 36 && !painted; slot++) {
            ItemStack item = p.getInventory().getItem(slot);
            if (!FlexibleInventoryGuard.empty(item) && mh != null && mh.spellKeyOf(item) != null) painted = true;
        }
        if (!painted) { r.unpainted = true; return r; }
        r.ran = true;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = p.getInventory().getItem(slot);
            r.scanned++;
            boolean empty = FlexibleInventoryGuard.empty(item);
            boolean spell = !empty && mh != null && mh.spellKeyOf(item) != null;
            boolean button = !empty && pager != null && pager.isPagerButton(item);
            boolean wandItem = !empty && mh != null && mh.isWandItem(item);
            if (!SlotGuardPlan.foreign(empty, spell, button, wandItem)) continue;
            r.foreign++;
            String name = nameOf(item);
            int amount = item.getAmount();
            ItemStack clone = item.clone();
            boolean accepted = impl.addToStoredInventory(clone);
            int left = ExitSweepPlan.leftover(amount, accepted, clone.getAmount());
            if (left <= 0) {
                p.getInventory().setItem(slot, null);
                movedNames.add(name);
                r.movedStacks++;
                if (verbose) log.info("Slot guard: moved " + name + " x" + amount + " out of " + p.getName()
                        + "'s open spell bar (slot " + (slot + 1) + ") into the wand's stored inventory");
            } else if (left < amount) {
                ItemStack rest = item.clone();
                rest.setAmount(left);
                p.getInventory().setItem(slot, rest);
                partialName = name;
                r.leftBehind++;
                if (verbose) log.info("Slot guard: moved " + (amount - left) + " of " + name + " x" + amount + " out of "
                        + p.getName() + "'s open spell bar (slot " + (slot + 1) + "); " + left + " stayed (no room)");
            } else {
                r.leftBehind++;
                if (verbose) log.info("Slot guard: " + name + " x" + amount + " is sitting in " + p.getName()
                        + "'s open spell bar (slot " + (slot + 1) + ") and could not be moved (no room); left in place");
            }
        }
        if (verbose && (r.movedStacks > 0 || partialName != null)) {
            String line = LoadoutText.sweptLine(movedNames, partialName, r.leftBehind > 0);
            if (line != null) p.sendMessage(Component.text(line, NamedTextColor.GRAY));
        }
        return r;
    }

    /** The item's own display name when it has one, else a readable material name. (ExitSweep's helper, same wording.) */
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
