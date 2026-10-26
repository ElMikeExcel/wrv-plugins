package com.winkreyes.excelwandbars;

import com.elmakers.mine.bukkit.api.magic.Mage;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.winkreyes.excelwandbars.core.CompassDedupe;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Menu compass guard. While a Magic (elmakers) inventory-mode wand bar is
 * open, the player's real inventory is stored away, so a join/respawn/compass
 * give from ExcelMenuJoin cannot see the existing compass and adds another;
 * when the bar closes Magic puts the real inventory back and the two stack.
 * This guard never gives anything. A few ticks after the moments a bar can
 * close (and after join/respawn), if no wand bar is open, it trims menu
 * compasses (items tagged menujoin:menu_compass or excelmenujoin:menu_compass)
 * to exactly one. It only ever removes extra copies of that one tagged item.
 */
final class CompassGuard implements Listener {
    private final ExcelWandBars plugin;
    private final MagicAPI api;
    private final List<NamespacedKey> keys = new ArrayList<>();
    private final Set<UUID> pending = new HashSet<>();
    private long trimmed = 0;

    CompassGuard(ExcelWandBars plugin, MagicAPI api, List<String> keyNames) {
        this.plugin = plugin;
        this.api = api;
        for (String k : keyNames) {
            NamespacedKey nk = k == null ? null : NamespacedKey.fromString(k.trim());
            if (nk != null) keys.add(nk);
        }
    }

    long trimmed() { return trimmed; }
    List<NamespacedKey> keys() { return keys; }

    boolean isMenuCompass(ItemStack it) {
        if (it == null || it.getType().isAir() || !it.hasItemMeta()) return false;
        ItemMeta m = it.getItemMeta();
        if (m == null) return false;
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        for (NamespacedKey k : keys) if (pdc.getKeys().contains(k)) return true;
        return false;
    }

    boolean barOpen(Player p) {
        try {
            Mage mage = api.getMage(p);
            Wand w = mage == null ? null : mage.getActiveWand();
            return w != null && w.isInventoryOpen();
        } catch (Throwable t) {
            return true; // unsure: do nothing
        }
    }

    /** Check soon (coalesced per player). */
    void later(Player p, long ticks) {
        if (p == null || !pending.add(p.getUniqueId())) return;
        UUID id = p.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pending.remove(id);
            Player q = Bukkit.getPlayer(id);
            if (q != null && q.isOnline()) check(q);
        }, ticks);
    }

    /** Returns how many extra compasses were removed (0 if none or a bar is open). */
    int check(Player p) {
        if (barOpen(p)) return 0; // real inventory is stored away; try again on the next trigger
        PlayerInventory inv = p.getInventory();
        ItemStack[] st = inv.getStorageContents();
        ItemStack off = inv.getItemInOffHand();
        int[] amounts = new int[st.length + 1];
        for (int i = 0; i < st.length; i++) amounts[i] = isMenuCompass(st[i]) ? st[i].getAmount() : 0;
        amounts[st.length] = isMenuCompass(off) ? off.getAmount() : 0;
        int[] plan = CompassDedupe.plan(amounts);
        if (plan == null) return 0;
        int removed = CompassDedupe.removed(amounts);
        for (int i = 0; i < st.length; i++) {
            if (amounts[i] == plan[i]) continue;
            if (plan[i] <= 0) inv.setItem(i, null);
            else { ItemStack c = st[i].clone(); c.setAmount(plan[i]); inv.setItem(i, c); }
        }
        if (amounts[st.length] != plan[st.length]) {
            if (plan[st.length] <= 0) inv.setItemInOffHand(null);
            else { ItemStack c = off.clone(); c.setAmount(plan[st.length]); inv.setItemInOffHand(c); }
        }
        trimmed += removed;
        plugin.getLogger().info("Menu compass tidy: removed " + removed + " extra for " + p.getName());
        return removed;
    }

    // Moments a Magic bar can close or a compass can arrive.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent e) { later(e.getPlayer(), 3); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent e) { later(e.getPlayer(), 3); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwap(PlayerSwapHandItemsEvent e) { later(e.getPlayer(), 3); }
    // 0.2.5: ignoreCancelled. A slot change ExcelAttunement refused (stance lock) changed nothing,
    // so there is nothing to re-check (audit 2026-10-26 finding 3). The other triggers still run.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent e) { later(e.getPlayer(), 3); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent e) { if (e.getPlayer() instanceof Player p) later(p, 3); }
    // ExcelMenuJoin gives 60 ticks after join and 1 tick after respawn.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) { Player p = e.getPlayer(); Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) later(p, 1); }, 80); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) { later(e.getPlayer(), 10); }
    @EventHandler
    public void onQuit(PlayerQuitEvent e) { pending.remove(e.getPlayer().getUniqueId()); }
}
