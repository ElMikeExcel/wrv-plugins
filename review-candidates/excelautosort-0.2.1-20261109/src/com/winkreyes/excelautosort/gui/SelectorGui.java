package com.winkreyes.excelautosort.gui;

import com.winkreyes.excelautosort.ExcelAutoSort;
import com.winkreyes.excelautosort.Names;
import com.winkreyes.excelautosort.core.BufferCore;
import com.winkreyes.excelautosort.core.FilterCore;
import com.winkreyes.excelautosort.core.Mode;
import com.winkreyes.excelautosort.core.SelectorModel;
import com.winkreyes.excelautosort.engine.SelectorFlow;
import com.winkreyes.excelautosort.store.BufferEntry;
import com.winkreyes.excelautosort.store.ChestRecord;
import com.winkreyes.excelautosort.store.ItemCodec;
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
 * The Java selector + confirm + picker + recovery inventories
 * (SPEC §7.2–§7.4, §6.2). All semantics live in {@link SelectorFlow}
 * and {@code BufferService}; this class only draws them and routes
 * taps. Every click in one of these inventories is cancelled — nothing
 * real ever moves by hand inside a menu.
 */
public final class SelectorGui implements Listener {

    public static final class Holder implements InventoryHolder {
        public enum Kind { SELECTOR, CONFIRM, PICKER, RECOVERY }
        public final Kind kind;
        public final SelectorSession session;   // null for RECOVERY
        public final UUID playerId;
        public int page;
        public List<String> shown = List.of();  // materials/buffer-ids by slot, set at render

        public Holder(Kind kind, SelectorSession session, UUID playerId) {
            this.kind = kind;
            this.session = session;
            this.playerId = playerId;
        }

        @Override public Inventory getInventory() { return null; }
    }

    private static final int GRID_START = 9;
    private static final int GRID_SLOTS = 28;

    private final ExcelAutoSort plugin;
    private final Map<UUID, SelectorSession> sessions = new HashMap<>();

    public SelectorGui(ExcelAutoSort plugin) {
        this.plugin = plugin;
    }

    public SelectorSession session(UUID playerId) {
        return sessions.get(playerId);
    }

    // ------------------------------------------------------------ openers

    public void openSelector(Player player, List<String> targetKeys) {
        SelectorSession s = plugin.flow().open(player, targetKeys);
        if (s == null) { // §6.4 refusal: plain line, no menu, no settings leak.
            plugin.messages().send(player, "no-permission", "&cYou don't have permission for that.");
            return;
        }
        sessions.put(player.getUniqueId(), s);
        renderSelector(player, s);
    }

    public void openSelector(Player player, SelectorSession s) {
        sessions.put(player.getUniqueId(), s);
        renderSelector(player, s);
    }

    public void openRecovery(Player player) {
        Holder h = new Holder(Holder.Kind.RECOVERY, null, player.getUniqueId());
        renderRecovery(player, h);
    }

    // ------------------------------------------------------------ rendering

    private void renderSelector(Player player, SelectorSession s) {
        Holder h = new Holder(Holder.Kind.SELECTOR, s, player.getUniqueId());
        Inventory inv = Bukkit.createInventory(h, 54,
                plugin.messages().comp("gui-selector-title", "Chest filter"));
        // Mode row — the CURRENT mode is named and ticked (menu-toggle standard, §7.2).
        inv.setItem(0, modeButton(s, Mode.OFF));
        inv.setItem(1, modeButton(s, Mode.WHITELIST));
        inv.setItem(2, modeButton(s, Mode.BLACKLIST));
        inv.setItem(4, infoItem(player, s));
        // Contents-seeded grid.
        List<SelectorModel.Entry> all = plugin.flow().entriesOf(s);
        int pageSize = plugin.getConfig().getInt("selector.page-size", 28);
        List<SelectorModel.Entry> page = SelectorModel.page(all, s.page, pageSize);
        List<String> shown = new ArrayList<>();
        for (int i = 0; i < GRID_SLOTS; i++) {
            if (i < page.size()) {
                SelectorModel.Entry e = page.get(i);
                inv.setItem(GRID_START + i, entryItem(s, e));
                shown.add(e.material());
            } else {
                shown.add(null);
            }
        }
        h.shown = shown;
        // Action row.
        inv.setItem(45, handButton(player));
        inv.setItem(46, button(Material.CHEST,
                plugin.messages().raw("add-from-inventory", "&eAdd from my inventory"),
                List.of("&7Pick from what you are carrying")));
        int pages = SelectorModel.pageCount(all.size(), pageSize);
        if (pages > 1 && s.page > 0) {
            inv.setItem(48, button(Material.ARROW, "&ePrevious page", List.of()));
        }
        if (pages > 1 && s.page < pages - 1) {
            inv.setItem(50, button(Material.ARROW, "&eNext page", List.of()));
        }
        int waiting = plugin.buffers().countOf(player.getUniqueId());
        inv.setItem(52, button(Material.ENDER_CHEST,
                plugin.messages().raw("recovery-button", "&eRecovery box — {count} waiting",
                        Map.of("count", String.valueOf(waiting))),
                List.of("&7Items your filters removed, kept safe")));
        inv.setItem(53, button(Material.OAK_DOOR,
                plugin.messages().raw("done", "&aDone"), List.of()));
        fillEmpty(inv);
        player.openInventory(inv);
    }

    private ItemStack modeButton(SelectorSession s, Mode mode) {
        boolean current = s.workMode == mode;
        String name = (current ? "&a✔ " : "&7") + modeName(mode);
        List<String> lore = new ArrayList<>();
        lore.add(switch (mode) {
            case OFF -> "&7The chest deletes nothing";
            case WHITELIST -> "&7The chest keeps only what is on the list";
            case BLACKLIST -> "&7The chest removes what is on the list";
        });
        if (current) lore.add("&aThis is the current mode");
        if (plugin.flow().isSuspended(s) && current && mode != Mode.OFF) {
            lore.add("&ePaused after a claim change — tap to re-confirm");
        }
        return button(current ? Material.LIME_DYE : Material.GRAY_DYE, name, lore, current);
    }

    public String modeName(Mode mode) {
        return switch (mode) {
            case OFF -> plugin.messages().raw("mode-off", "Filter: OFF");
            case WHITELIST -> plugin.messages().raw("mode-whitelist", "Keep only these (whitelist)");
            case BLACKLIST -> plugin.messages().raw("mode-blacklist", "Throw away these (blacklist)");
        };
    }

    public String modeWords(Mode mode, int listSize) {
        return switch (mode) {
            case WHITELIST -> plugin.messages().raw("confirm-whitelist",
                    "Keep only the {count} kinds on the list. Anything else put in this chest will be removed to the recovery box.",
                    Map.of("count", String.valueOf(listSize)));
            case BLACKLIST -> plugin.messages().raw("confirm-blacklist",
                    "Throw away the {count} kinds on the list. Everything else put in this chest stays.",
                    Map.of("count", String.valueOf(listSize)));
            case OFF -> plugin.messages().raw("confirm-off", "The filter is off. This chest removes nothing.");
        };
    }

    private ItemStack infoItem(Player player, SelectorSession s) {
        List<String> lore = new ArrayList<>();
        ChestRecord r = plugin.flow().recordOf(s);
        if (r == null) {
            lore.add("&7Not set up yet — pick a mode to start");
        } else {
            lore.add("&7Set up by: &f" + (r.ownerName.isEmpty() ? "someone" : r.ownerName));
            lore.add("&7Mode: &f" + modeName(r.mode).replace("&a✔ ", ""));
            if (r.suspended) lore.add("&ePaused: " + plugin.flow().suspendReason(s));
        }
        lore.add("&7On the list: &f" + s.workItems.size() + " kinds");
        return button(Material.PAPER,
                plugin.messages().raw("info-title", "&eThis chest's filter"), lore);
    }

    private ItemStack entryItem(SelectorSession s, SelectorModel.Entry e) {
        Material mat = Material.matchMaterial(e.material());
        if (mat == null) mat = Material.BARRIER;
        ItemStack item = new ItemStack(mat);
        if (e.inChest() > 0) item.setAmount(Math.min(99, e.inChest()));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.messages().of((e.onList() ? "&a" : "&f") + Names.friendly(e.material())));
            List<String> lore = new ArrayList<>();
            if (e.inChest() > 0) lore.add("&7×" + e.inChest() + " in this chest");
            else lore.add("&7Not in this chest right now");
            lore.add(e.onList() ? "&aON the list — tap to take off" : "&7Not on the list — tap to add");
            meta.lore(plugin.messages().comps(lore));
            if (e.onList()) meta.setEnchantmentGlintOverride(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack handButton(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            return button(Material.BARRIER,
                    plugin.messages().raw("add-from-hand", "&eAdd from my hand"),
                    List.of("&7Your hand is empty right now"));
        }
        ItemStack icon = hand.clone();
        icon.setAmount(1);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.messages().of(
                    plugin.messages().raw("add-from-hand", "&eAdd from my hand")));
            meta.lore(plugin.messages().comps(List.of("&7Adds: &f" + Names.friendly(hand.getType().name()))));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private void renderConfirm(Player player, SelectorSession s) {
        Holder h = new Holder(Holder.Kind.CONFIRM, s, player.getUniqueId());
        Inventory inv = Bukkit.createInventory(h, 27,
                plugin.messages().comp("gui-confirm-title", "Turn the filter on?"));
        List<FilterCore.RejectLine> rejects = s.hasPending()
                ? plugin.flow().rejectsOf(s, s.pendingMode, s.pendingItems) : List.of();
        List<String> lore = new ArrayList<>();
        lore.add("&f" + modeWords(s.pendingMode, s.pendingItems == null ? 0 : s.pendingItems.size()));
        if (!rejects.isEmpty()) {
            lore.add("");
            lore.add("&eThese are in the chest and don't pass:");
            int shownLines = 0;
            for (FilterCore.RejectLine l : rejects) {
                if (shownLines++ >= 18) {
                    lore.add("&7… and " + (rejects.size() - 18) + " more kinds");
                    break;
                }
                lore.add("&7- " + Names.friendly(l.material()) + " ×" + l.totalCount());
            }
            lore.add("&eTotal: " + FilterCore.totalItems(rejects) + " items in "
                    + FilterCore.totalStacks(rejects) + " stacks");
        }
        lore.add("");
        lore.add("&7They wait in your recovery box for "
                + BufferCore.timeLeft(System.currentTimeMillis()
                        + plugin.getConfig().getLong("recovery.hours", 168) * 3_600_000L,
                        System.currentTimeMillis()) + ".");
        inv.setItem(4, button(Material.PAPER, "&eCheck before it starts", lore));
        for (int i = 0; i < Math.min(9, rejects.size()); i++) {
            Material mat = Material.matchMaterial(rejects.get(i).material());
            if (mat != null) inv.setItem(9 + i, new ItemStack(mat));
        }
        inv.setItem(21, button(Material.LIME_DYE, rejects.isEmpty()
                        ? plugin.messages().raw("confirm-on", "&aTurn the filter on")
                        : plugin.messages().raw("confirm-sweep",
                                "&aMove those to my recovery box and turn the filter on"),
                List.of(), true));
        inv.setItem(23, button(Material.RED_DYE,
                plugin.messages().raw("confirm-not-yet", "&cNot yet"), List.of()));
        fillEmpty(inv);
        player.openInventory(inv);
    }

    private void renderPicker(Player player, SelectorSession s) {
        Holder h = new Holder(Holder.Kind.PICKER, s, player.getUniqueId());
        Inventory inv = Bukkit.createInventory(h, 54,
                plugin.messages().comp("gui-picker-title", "Add from my inventory"));
        Map<String, Integer> distinct = new java.util.LinkedHashMap<>();
        for (ItemStack st : player.getInventory().getContents()) {
            if (st != null && st.getType() != Material.AIR) {
                distinct.merge(st.getType().name(), st.getAmount(), Integer::sum);
            }
        }
        List<String> shown = new ArrayList<>();
        int slot = 0;
        for (Map.Entry<String, Integer> e : distinct.entrySet()) {
            if (slot >= 45) break;
            boolean onList = s.workItems.contains(e.getKey());
            ItemStack icon = new ItemStack(Material.matchMaterial(e.getKey()));
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(plugin.messages().of("&f" + Names.friendly(e.getKey())));
                meta.lore(plugin.messages().comps(List.of(
                        "&7You are carrying ×" + e.getValue(),
                        onList ? "&aAlready on the list" : "&7Tap to add to the list")));
                if (onList) meta.setEnchantmentGlintOverride(true);
                icon.setItemMeta(meta);
            }
            inv.setItem(slot++, icon);
            shown.add(e.getKey());
        }
        h.shown = shown;
        inv.setItem(53, button(Material.ARROW, "&eBack", List.of()));
        fillEmpty(inv);
        player.openInventory(inv);
    }

    private void renderRecovery(Player player, Holder h) {
        Inventory inv = Bukkit.createInventory(h, 54,
                plugin.messages().comp("gui-recovery-title", "Filter recovery box"));
        List<BufferEntry> entries = plugin.buffers().entriesOf(player.getUniqueId());
        int pageSize = 45;
        int pages = Math.max(1, (entries.size() + pageSize - 1) / pageSize);
        h.page = Math.min(h.page, pages - 1);
        List<String> shown = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < pageSize; i++) {
            int idx = h.page * pageSize + i;
            if (idx >= entries.size()) { shown.add(null); continue; }
            BufferEntry e = entries.get(idx);
            ItemStack icon = ItemCodec.fromB64(e.itemB64);
            if (icon == null) icon = new ItemStack(Material.BARRIER);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(plugin.messages().of("&7From the chest at &f" + e.chestKey));
                lore.add(plugin.messages().of("&7Time left: &f" + BufferCore.timeLeft(e.expiresAtMs, now)));
                lore.add(plugin.messages().of("&aTap to claim it back"));
                meta.lore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(i, icon);
            shown.add(e.id);
        }
        h.shown = shown;
        inv.setItem(45, button(Material.CHEST,
                plugin.messages().raw("claim-all", "&aClaim everything"), List.of()));
        if (pages > 1 && h.page > 0) inv.setItem(48, button(Material.ARROW, "&ePrevious page", List.of()));
        if (pages > 1 && h.page < pages - 1) inv.setItem(50, button(Material.ARROW, "&eNext page", List.of()));
        inv.setItem(53, button(Material.OAK_DOOR, plugin.messages().raw("done", "&aDone"), List.of()));
        fillEmpty(inv);
        player.openInventory(inv);
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
            case SELECTOR -> clickSelector(player, h, slot);
            case CONFIRM -> clickConfirm(player, h, slot);
            case PICKER -> clickPicker(player, h, slot);
            case RECOVERY -> clickRecovery(player, h, slot);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Holder) e.setCancelled(true);
    }

    private void clickSelector(Player player, Holder h, int slot) {
        SelectorSession s = h.session;
        if (s == null) return;
        switch (slot) {
            case 0 -> afterMode(player, s, plugin.flow().requestMode(s, Mode.OFF));
            case 1 -> afterMode(player, s, plugin.flow().requestMode(s, Mode.WHITELIST));
            case 2 -> afterMode(player, s, plugin.flow().requestMode(s, Mode.BLACKLIST));
            case 45 -> {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand == null || hand.getType() == Material.AIR) {
                    plugin.messages().send(player, "hand-empty", "&eYour hand is empty — hold the item you want to add.");
                } else {
                    afterEdit(player, s, plugin.flow().addItem(s, hand.getType().name()));
                }
            }
            case 46 -> renderPicker(player, s);
            case 48 -> { s.page = Math.max(0, s.page - 1); renderSelector(player, s); }
            case 50 -> { s.page++; renderSelector(player, s); }
            case 52 -> openRecovery(player);
            case 53 -> player.closeInventory();
            default -> {
                if (slot >= GRID_START && slot < GRID_START + GRID_SLOTS) {
                    String mat = h.shown.get(slot - GRID_START);
                    if (mat != null) afterEdit(player, s, plugin.flow().toggleItem(s, mat));
                }
            }
        }
    }

    private void afterMode(Player player, SelectorSession s, SelectorFlow.Result r) {
        switch (r) {
            case APPLIED, NOOP -> renderSelector(player, s);
            case NEEDS_CONFIRM -> renderConfirm(player, s);
            case DENIED -> {
                plugin.messages().send(player, "no-permission", "&cYou don't have permission for that.");
                renderSelector(player, s);
            }
        }
    }

    private void afterEdit(Player player, SelectorSession s, SelectorFlow.Result r) {
        switch (r) {
            case APPLIED, NOOP -> renderSelector(player, s);
            case NEEDS_CONFIRM -> renderConfirm(player, s);
            case DENIED -> {
                plugin.messages().send(player, "no-permission", "&cYou don't have permission for that.");
                renderSelector(player, s);
            }
        }
    }

    private void clickConfirm(Player player, Holder h, int slot) {
        SelectorSession s = h.session;
        if (s == null) return;
        if (slot == 21) {
            int[] swept = plugin.flow().confirm(player, s);
            plugin.messages().send(player, "armed", "&aFilter on: {mode}",
                    Map.of("mode", Names.friendly(s.workMode.id())));
            if (swept[0] > 0) {
                plugin.messages().send(player, "swept", "&eMoved {count} items to your recovery box.",
                        Map.of("count", String.valueOf(swept[1])));
            }
            renderSelector(player, s);
        } else if (slot == 23) {
            plugin.flow().cancel(s);
            renderSelector(player, s);
        }
    }

    private void clickPicker(Player player, Holder h, int slot) {
        SelectorSession s = h.session;
        if (s == null) return;
        if (slot == 53) {
            renderSelector(player, s);
            return;
        }
        if (slot >= 0 && slot < h.shown.size()) {
            String mat = h.shown.get(slot);
            if (mat != null) {
                SelectorFlow.Result r = plugin.flow().addItem(s, mat);
                if (r == SelectorFlow.Result.NEEDS_CONFIRM) renderConfirm(player, s);
                else renderSelector(player, s);
            }
        }
    }

    private void clickRecovery(Player player, Holder h, int slot) {
        if (slot == 45) {
            int n = plugin.buffers().claimAll(player);
            plugin.messages().send(player, "claimed", "&aClaimed {count} stacks from your recovery box.",
                    Map.of("count", String.valueOf(n)));
            renderRecovery(player, h);
            return;
        }
        if (slot == 48) { h.page = Math.max(0, h.page - 1); renderRecovery(player, h); return; }
        if (slot == 50) { h.page++; renderRecovery(player, h); return; }
        if (slot == 53) { player.closeInventory(); return; }
        if (slot >= 0 && slot < h.shown.size()) {
            String id = h.shown.get(slot);
            if (id != null) {
                plugin.buffers().claim(player, id);
                renderRecovery(player, h);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder(false) instanceof Holder h
                && h.kind == Holder.Kind.CONFIRM && h.session != null && h.session.hasPending()) {
            // Closing the confirm without choosing is "Not yet" (§7.4).
            plugin.flow().cancel(h.session);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        sessions.remove(e.getPlayer().getUniqueId());
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
