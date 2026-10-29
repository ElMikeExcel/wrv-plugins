/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark;

import com.winkreyes.makersmark.core.Gear;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.inventory.ComplexRecipe;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

/**
 * Signing and rebuild points. The crafting result is signed while it is still a preview
 * (PrepareItemCraftEvent), so a shift-click that makes several items signs every one; CraftItemEvent
 * double-checks the item that is actually taken. Anvil, smithing and grindstone results are rebuilt
 * from the tag so the line cannot be faked or stripped.
 */
final class CraftHooks implements Listener {
    private final ExcelMakersMark plugin;
    private final Marks marks;

    CraftHooks(ExcelMakersMark plugin) { this.plugin = plugin; this.marks = plugin.marks(); }

    private static Player viewer(HumanEntity h) { return h instanceof Player p ? p : null; }
    private static boolean empty(ItemStack it) { return it == null || it.getType().isAir(); }
    private static boolean gear(ItemStack it) { return Gear.markable(it.getType().name(), it.getMaxStackSize()); }

    private void sign(Marks.Data d, Player p) {
        d.by = p.getUniqueId().toString();
        d.name = plugin.crafterName(p);
        d.date = plugin.today();
    }

    private static boolean isRepairRecipe(Recipe r) {
        return r instanceof ComplexRecipe cr && cr.getKey().getKey().equals("repair_item");
    }

    // ---------- crafting grid ----------
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        CraftingInventory inv = e.getInventory();
        ItemStack res = inv.getResult();
        if (empty(res) || !gear(res) || plugin.excluded(res)) return;
        Player p = viewer(e.getView().getPlayer());
        if (e.isRepair()) {
            // Grid repair: copy the mark from the first marked input; counters keep the higher value.
            Marks.Data merged = null;
            for (ItemStack in : inv.getMatrix()) {
                if (empty(in) || in.getType() != res.getType()) continue;
                if (plugin.excluded(in)) return;
                Marks.Data d = marks.read(in);
                merged = merged == null ? d : Marks.merge(merged, d);
            }
            if (merged == null || merged.empty()) return;
            ItemStack out = res.clone();
            marks.apply(out, merged, plugin.showsStats(p));
            inv.setResult(out);
            return;
        }
        if (p == null || !plugin.signs(p)) return;
        ItemStack out = res.clone();
        Marks.Data d = marks.read(out);
        if (d.hasProvenance()) return; // a plugin recipe already carries a mark
        sign(d, p);
        marks.apply(out, d, plugin.showsStats(p));
        inv.setResult(out);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        Player p = viewer(e.getWhoClicked());
        ItemStack cur = e.getCurrentItem();
        if (p == null || empty(cur) || !plugin.eligible(cur)) return;
        Marks.Data d = marks.read(cur);
        if (!isRepairRecipe(e.getRecipe()) && plugin.signs(p) && !d.hasProvenance()) {
            // Safety net: the preview was not signed (another plugin replaced the result late).
            ItemStack out = cur.clone();
            sign(d, p);
            marks.apply(out, d, plugin.showsStats(p));
            e.setCurrentItem(out);
        }
        if (d.marked() && p.getUniqueId().toString().equals(d.by)) Bukkit.getScheduler().runTask(plugin, () -> plugin.tips().use(p));
    }

    // ---------- smithing ----------
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareSmith(PrepareSmithingEvent e) {
        ItemStack res = e.getResult();
        if (empty(res) || !gear(res) || plugin.excluded(res)) return;
        Player p = viewer(e.getView().getPlayer());
        ItemStack base = e.getInventory().getInputEquipment();
        ItemStack out = res.clone();
        Marks.Data d = marks.read(out); // vanilla copies the base item's tag, so a netherite upgrade keeps the original crafter
        if (!empty(base)) {
            if (plugin.excluded(base)) return;
            d = Marks.mergeResult(marks.read(base), d);
        }
        boolean upgrade = !empty(base) && base.getType() != res.getType();
        if (!d.hasProvenance() && upgrade && p != null && plugin.signs(p) && plugin.getConfig().getBoolean("mark.sign-smithing-upgrades", true)) sign(d, p);
        marks.apply(out, d, plugin.showsStats(p));
        e.setResult(out);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSmith(SmithItemEvent e) {
        Player p = viewer(e.getWhoClicked());
        ItemStack cur = e.getCurrentItem();
        if (p == null || empty(cur) || !plugin.eligible(cur)) return;
        Marks.Data d = marks.read(cur);
        if (d.marked() && p.getUniqueId().toString().equals(d.by)) Bukkit.getScheduler().runTask(plugin, () -> plugin.tips().use(p));
    }

    // ---------- anvil ----------
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        ItemStack res = e.getResult();
        if (empty(res) || !gear(res) || plugin.excluded(res)) return;
        ItemStack right = e.getInventory().getSecondItem();
        Marks.Data d = null;
        ItemStack left = e.getInventory().getFirstItem();
        if (!empty(left) && left.getType() == res.getType()) {
            if (plugin.excluded(left)) return;
            d = marks.read(left);
        }
        if (!empty(right) && right.getType() == res.getType()) {
            if (plugin.excluded(right)) return;
            d = d == null ? marks.read(right) : Marks.merge(d, marks.read(right));
        }
        d = d == null ? marks.read(res) : Marks.mergeResult(d, marks.read(res));
        ItemStack out = res.clone();
        marks.apply(out, d, plugin.showsStats(viewer(e.getView().getPlayer())));
        e.setResult(out);
    }

    // ---------- grindstone ----------
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareGrindstone(PrepareGrindstoneEvent e) {
        ItemStack res = e.getResult();
        if (empty(res) || !gear(res) || plugin.excluded(res)) return;
        ItemStack up = e.getInventory().getUpperItem(), low = e.getInventory().getLowerItem();
        Marks.Data d = null;
        for (ItemStack in : new ItemStack[]{up, low}) {
            if (!empty(in) && in.getType() == res.getType()) {
                if (plugin.excluded(in)) return;
                d = d == null ? marks.read(in) : Marks.merge(d, marks.read(in));
            }
        }
        d = d == null ? marks.read(res) : Marks.mergeResult(d, marks.read(res));
        ItemStack out = res.clone();
        marks.apply(out, d, plugin.showsStats(viewer(e.getView().getPlayer())));
        e.setResult(out);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) plugin.tips().inventoryClosed(p);
    }
}
