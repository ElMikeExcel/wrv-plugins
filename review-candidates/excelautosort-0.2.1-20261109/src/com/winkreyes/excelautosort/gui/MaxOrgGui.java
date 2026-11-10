package com.winkreyes.excelautosort.gui;

import com.winkreyes.excelautosort.ExcelAutoSort;
import com.winkreyes.excelautosort.Names;
import com.winkreyes.excelautosort.core.MaxOrgCore;
import com.winkreyes.excelautosort.engine.MaxOrgService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Java Max Organization screens (SPEC §15): the unlock screen
 * (state + pitch + rent/buy, confirm before any charge), the confirm
 * screen, and the paged network view. All semantics live in
 * {@link MaxOrgService}; this class only draws and routes taps.
 * Every click is cancelled — items move only through the service's
 * verified transfers, never by hand inside a menu (§15.4).
 */
public final class MaxOrgGui implements Listener {

    public static final class Holder implements InventoryHolder {
        public enum Kind { MAXORG, MAXORG_CONFIRM, NETWORK }
        public final Kind kind;
        public final MaxOrgSession session;
        public final UUID playerId;
        /** View entries by content slot, set at render (NETWORK kind). */
        public List<MaxOrgCore.ViewEntry> shown = List.of();

        public Holder(Kind kind, MaxOrgSession session, UUID playerId) {
            this.kind = kind;
            this.session = session;
            this.playerId = playerId;
        }

        @Override public Inventory getInventory() { return null; }
    }

    private final ExcelAutoSort plugin;
    private final Map<UUID, MaxOrgSession> sessions = new HashMap<>();

    public MaxOrgGui(ExcelAutoSort plugin) {
        this.plugin = plugin;
    }

    public MaxOrgSession session(UUID playerId) {
        return sessions.get(playerId);
    }

    private MaxOrgSession sessionFor(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), MaxOrgSession::new);
    }

    // ------------------------------------------------------------ openers

    public void openUnlock(Player player) {
        MaxOrgSession s = sessionFor(player);
        s.pendingBuy = null;
        renderUnlock(player, s);
    }

    public void openNetwork(Player player) {
        MaxOrgSession s = sessionFor(player);
        if (!plugin.maxOrg().entitled(player.getUniqueId())) {
            plugin.messages().send(player, plugin.maxOrg().stateKey(player.getUniqueId()),
                    "&eMax Organization isn't active for you right now.", plugin.maxOrg().stateVars(player.getUniqueId()));
            renderUnlock(player, s);
            return;
        }
        s.network = plugin.maxOrg().resolveNetwork(player);
        s.entries = plugin.maxOrg().viewOf(s.network);
        s.page = 0;
        renderNetwork(player, s);
    }

    // ------------------------------------------------------------ unlock screen

    private void renderUnlock(Player player, MaxOrgSession s) {
        Holder h = new Holder(Holder.Kind.MAXORG, s, player.getUniqueId());
        Inventory inv = Bukkit.createInventory(h, 54,
                plugin.messages().comp("gui-maxorg-title", "Max Organization"));
        MaxOrgService mo = plugin.maxOrg();
        MaxOrgCore.Access access = mo.access(player.getUniqueId());
        // State + pitch on one paper (§15.2: both prices and the lapse
        // behavior up front, on the unlock screen itself).
        List<String> lore = new ArrayList<>();
        lore.add("&f" + plugin.messages().raw(mo.stateKey(player.getUniqueId()),
                "Max Organization isn't active for you right now.", mo.stateVars(player.getUniqueId())));
        lore.add("");
        for (String line : mo.pitchLines()) lore.add("&7" + line);
        inv.setItem(4, button(Material.PAPER,
                plugin.messages().raw("maxorg-info-title", "&eMax Organization"), lore));
        if (access == MaxOrgCore.Access.OWNED) {
            inv.setItem(20, button(Material.BARRIER,
                    plugin.messages().raw("maxorg-owned-note", "&aYours forever — no rent, no expiry"),
                    List.of("&7You bought Max Organization outright")));
            inv.setItem(24, button(Material.BARRIER,
                    plugin.messages().raw("maxorg-owned-note", "&aYours forever — no rent, no expiry"),
                    List.of("&7You bought Max Organization outright")));
        } else {
            inv.setItem(20, button(Material.CLOCK,
                    plugin.messages().raw("maxorg-rent-button", "&eRent — {rent} for {days} days", mo.pitchVars()),
                    List.of("&7Turns off when it runs out — re-rent anytime",
                            access == MaxOrgCore.Access.RENTED ? "&7Renting again adds the days on" : "&7No auto-charge, ever"),
                    access == MaxOrgCore.Access.RENTED));
            inv.setItem(24, button(Material.NETHERITE_INGOT,
                    plugin.messages().raw("maxorg-buy-button", "&eBuy — {buy}, yours forever", mo.pitchVars()),
                    List.of("&7One payment, no expiry",
                            "&7Rent already paid does not count toward it")));
        }
        if (access != MaxOrgCore.Access.NONE) {
            inv.setItem(49, button(Material.CHEST,
                    plugin.messages().raw("maxorg-open-network", "&aOpen my chests"),
                    List.of("&7Every chest around you that you may set up")));
        }
        inv.setItem(53, button(Material.OAK_DOOR,
                plugin.messages().raw("done", "&aDone"), List.of()));
        fillEmpty(inv);
        player.openInventory(inv);
    }

    private void renderConfirm(Player player, MaxOrgSession s) {
        boolean buy = Boolean.TRUE.equals(s.pendingBuy);
        Holder h = new Holder(Holder.Kind.MAXORG_CONFIRM, s, player.getUniqueId());
        Inventory inv = Bukkit.createInventory(h, 27,
                plugin.messages().comp("gui-maxorg-confirm-title", "Unlock Max Organization?"));
        List<String> lore = new ArrayList<>();
        for (String line : plugin.maxOrg().confirmLines(buy)) lore.add("&f" + line);
        double bal = plugin.economy().available() ? plugin.economy().balance(player.getUniqueId()) : -1;
        if (bal >= 0) {
            lore.add("");
            lore.add("&7Your balance: &f" + MaxOrgCore.money(bal));
        }
        inv.setItem(4, button(Material.PAPER,
                plugin.messages().raw("maxorg-info-title", "&eMax Organization"), lore));
        inv.setItem(11, button(Material.LIME_DYE,
                plugin.messages().raw("maxorg-confirm-charge", "&aCharge me {price}",
                        Map.of("price", MaxOrgCore.money(buy ? plugin.maxOrg().buyPrice()
                                : plugin.maxOrg().rentPrice()))),
                List.of(), true));
        inv.setItem(15, button(Material.RED_DYE,
                plugin.messages().raw("confirm-not-yet", "&cNot yet"), List.of()));
        fillEmpty(inv);
        player.openInventory(inv);
    }

    // ------------------------------------------------------------ network view

    private void renderNetwork(Player player, MaxOrgSession s) {
        Holder h = new Holder(Holder.Kind.NETWORK, s, player.getUniqueId());
        Inventory inv = Bukkit.createInventory(h, 54,
                plugin.messages().comp("gui-network-title", "My chests around me"));
        int pageSize = plugin.maxOrg().pageSize();
        int pages = Math.max(1, (s.entries.size() + pageSize - 1) / pageSize);
        s.page = Math.min(Math.max(0, s.page), pages - 1);
        List<MaxOrgCore.ViewEntry> shown = new ArrayList<>();
        if (s.network.isEmpty()) {
            inv.setItem(22, button(Material.PAPER,
                    plugin.messages().raw("maxorg-network-empty-title", "&eNo chests in reach"),
                    plugin.messages().rawList("maxorg-network-empty", List.of(
                            "None of your chests are within reach here.",
                            "Stand closer to your storage and try again."), Map.of())));
        } else if (s.entries.isEmpty()) {
            inv.setItem(22, button(Material.PAPER,
                    plugin.messages().raw("maxorg-network-empty-title", "&eNo chests in reach"),
                    List.of("&7Your chests in reach are all empty right now")));
        } else {
            int from = s.page * pageSize;
            for (int i = 0; i < pageSize; i++) {
                int idx = from + i;
                if (idx >= s.entries.size()) { shown.add(null); continue; }
                MaxOrgCore.ViewEntry e = s.entries.get(idx);
                shown.add(e);
                inv.setItem(i, entryItem(e));
            }
        }
        h.shown = shown;
        if (pages > 1 && s.page > 0) inv.setItem(45, button(Material.ARROW, "&ePrevious page", List.of()));
        inv.setItem(46, button(Material.CLOCK,
                plugin.messages().raw("maxorg-info-title", "&eMax Organization"),
                List.of("&7" + plugin.messages().raw(plugin.maxOrg().stateKey(player.getUniqueId()),
                                "", plugin.maxOrg().stateVars(player.getUniqueId())),
                        "&7Rent, buy, or check your unlock")));
        inv.setItem(47, button(Material.HOPPER,
                plugin.messages().raw("maxorg-deposit-button", "&aPut my things away"),
                List.of("&7Sends what you are carrying into these chests,",
                        "&7each chest's filter respected")));
        inv.setItem(49, button(Material.PAPER,
                plugin.messages().raw("maxorg-network-info", "&e{chests} chests in reach",
                        Map.of("chests", String.valueOf(s.network.size()))),
                List.of("&7Page " + (s.page + 1) + " of " + pages,
                        "&7Tap a stack to take it")));
        if (pages > 1 && s.page < pages - 1) inv.setItem(51, button(Material.ARROW, "&eNext page", List.of()));
        inv.setItem(53, button(Material.OAK_DOOR,
                plugin.messages().raw("done", "&aDone"), List.of()));
        fillEmpty(inv);
        player.openInventory(inv);
    }

    private ItemStack entryItem(MaxOrgCore.ViewEntry e) {
        Material mat = Material.matchMaterial(e.stack().material());
        if (mat == null) mat = Material.BARRIER;
        ItemStack icon = new ItemStack(mat);
        icon.setAmount(Math.min(99, e.stack().count()));
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.messages().of("&f" + Names.friendly(e.stack().material())));
            meta.lore(plugin.messages().comps(List.of(
                    "&7×" + e.stack().count(),
                    "&7From the chest at &f" + e.x() + ", " + e.y() + ", " + e.z(),
                    "&aTap to take")));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    /** Re-resolve the view after a move (the window always shows the real chests). */
    private void refreshNetwork(Player player, MaxOrgSession s) {
        s.network = plugin.maxOrg().resolveNetwork(player);
        s.entries = plugin.maxOrg().viewOf(s.network);
        renderNetwork(player, s);
    }

    // ------------------------------------------------------------ item helpers

    private ItemStack button(Material mat, String legacyName, List<String> legacyLore) {
        return button(mat, legacyName, legacyLore, false);
    }

    private ItemStack button(Material mat, String legacyName, List<String> legacyLore, boolean glint) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.messages().of(legacyName));
            if (legacyLore != null && !legacyLore.isEmpty()) meta.lore(plugin.messages().comps(legacyLore));
            if (glint) meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillEmpty(Inventory inv) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            filler.setItemMeta(meta);
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }
    }

    // ------------------------------------------------------------ events

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof Holder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        int slot = e.getRawSlot();
        switch (h.kind) {
            case MAXORG -> clickUnlock(player, h, slot);
            case MAXORG_CONFIRM -> clickConfirm(player, h, slot);
            case NETWORK -> clickNetwork(player, h, slot);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Holder) e.setCancelled(true);
    }

    private void clickUnlock(Player player, Holder h, int slot) {
        MaxOrgSession s = h.session;
        if (s == null) return;
        MaxOrgCore.Access access = plugin.maxOrg().access(player.getUniqueId());
        switch (slot) {
            case 20 -> {
                if (access != MaxOrgCore.Access.OWNED) {
                    s.pendingBuy = Boolean.FALSE;
                    renderConfirm(player, s);
                }
            }
            case 24 -> {
                if (access != MaxOrgCore.Access.OWNED) {
                    s.pendingBuy = Boolean.TRUE;
                    renderConfirm(player, s);
                }
            }
            case 49 -> {
                if (access != MaxOrgCore.Access.NONE) openNetwork(player);
            }
            case 53 -> player.closeInventory();
            default -> { /* filler */ }
        }
    }

    private void clickConfirm(Player player, Holder h, int slot) {
        MaxOrgSession s = h.session;
        if (s == null) return;
        if (slot == 11) {
            // Consume the pending purchase BEFORE charging: a second
            // tap finds nothing pending and can never double-charge.
            Boolean buy = s.pendingBuy;
            s.pendingBuy = null;
            if (buy == null) { renderUnlock(player, s); return; }
            MaxOrgService.PurchaseOutcome outcome = plugin.maxOrg().purchase(player, buy);
            plugin.maxOrg().tellOutcome(player, outcome, buy);
            if (outcome == MaxOrgService.PurchaseOutcome.RENTED
                    || outcome == MaxOrgService.PurchaseOutcome.BOUGHT) {
                openNetwork(player);
            } else {
                renderUnlock(player, s);
            }
        } else if (slot == 15) {
            s.pendingBuy = null;
            renderUnlock(player, s);
        }
    }

    private void clickNetwork(Player player, Holder h, int slot) {
        MaxOrgSession s = h.session;
        if (s == null) return;
        if (slot == 45) { s.page = Math.max(0, s.page - 1); renderNetwork(player, s); return; }
        if (slot == 46) { s.pendingBuy = null; renderUnlock(player, s); return; }
        if (slot == 51) { s.page++; renderNetwork(player, s); return; }
        if (slot == 53) { player.closeInventory(); return; }
        if (slot == 47) {
            MaxOrgService.DepositReport report = plugin.maxOrg().smartDeposit(player);
            tellDeposit(player, report);
            refreshNetwork(player, s);
            return;
        }
        if (slot >= 0 && slot < h.shown.size()) {
            MaxOrgCore.ViewEntry e = h.shown.get(slot);
            if (e == null) return;
            MaxOrgService.NetChest nc = s.chestOf(e);
            if (nc == null) { refreshNetwork(player, s); return; }
            int moved = plugin.maxOrg().take(player, nc, e.slot());
            if (moved < 0) {
                plugin.messages().send(player, "maxorg-chest-gone",
                        "&eThat chest isn't in your network anymore — the view has been refreshed.");
            } else if (moved == 0) {
                plugin.messages().send(player, "maxorg-no-room",
                        "&eNo room in your inventory for that right now.");
            }
            refreshNetwork(player, s);
        }
    }

    /** The smart-deposit report (§15.5: unplaced items are named, never silent). */
    public void tellDeposit(Player player, MaxOrgService.DepositReport report) {
        if (report.networkEmpty()) {
            plugin.messages().send(player, "maxorg-network-empty-title", "&eNo chests in reach");
            return;
        }
        if (report.itemsMoved() > 0) {
            plugin.messages().send(player, "maxorg-deposited",
                    "&aPut away {count} items into {chests} chests.",
                    Map.of("count", String.valueOf(report.itemsMoved()),
                            "chests", String.valueOf(report.chests())));
        } else {
            plugin.messages().send(player, "maxorg-deposited-none",
                    "&eNothing you are carrying fits these chests right now.");
        }
        if (!report.unplaced().isEmpty()) {
            plugin.messages().send(player, "maxorg-unplaced",
                    "&eNo chest here would take: {items} — still with you.",
                    Map.of("items", String.join(", ", report.unplaced())));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder(false) instanceof Holder h
                && h.kind == Holder.Kind.MAXORG_CONFIRM && h.session != null) {
            // Closing the confirm without choosing is "Not yet" (§15.2).
            h.session.pendingBuy = null;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // Fail-closed by construction (§15.4): the session holds no
        // items, so a disconnect mid-view strands nothing — the
        // session state is simply dropped.
        sessions.remove(e.getPlayer().getUniqueId());
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
