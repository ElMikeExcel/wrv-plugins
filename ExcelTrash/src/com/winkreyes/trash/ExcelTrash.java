package com.winkreyes.trash;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public final class ExcelTrash extends JavaPlugin implements Listener, TabExecutor {
    private TrashStore store;
    private TrashLog log;

    /** Marks our GUI; identity check via holder, never by title. */
    static final class TrashHolder implements InventoryHolder {
        Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    @Override public void onEnable() {
        store = new TrashStore(getDataFolder().toPath().resolve("batches.dat"), System::currentTimeMillis);
        store.load();
        log = new TrashLog(getDataFolder().toPath().resolve("trash.log"));
        PluginCommand c = Objects.requireNonNull(getCommand("trash"));
        c.setExecutor(this); c.setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, () -> store.purgeExpired(), 1200L, 1200L);
        getLogger().info("ExcelTrash 0.1.0 enabled; " + "undo window 5 minutes");
    }

    @Override public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof TrashHolder) p.closeInventory();
        }
        if (store != null) store.save();
    }

    @Override public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!(s instanceof Player p)) { s.sendMessage("Players only."); return true; }
        if (args.length >= 1 && args[0].equalsIgnoreCase("undo")) { undo(p); return true; }
        open(p);
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        return a.length == 1 && "undo".startsWith(a[0].toLowerCase()) ? List.of("undo") : List.of();
    }

    void open(Player p) {
        TrashHolder h = new TrashHolder();
        Inventory inv = Bukkit.createInventory(h, SlotPolicy.SIZE, Component.text("Trash"));
        h.inv = inv;
        ItemStack filler = button(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = SlotPolicy.STORAGE; i < SlotPolicy.SIZE; i++) inv.setItem(i, filler);
        inv.setItem(SlotPolicy.UNDO_SLOT, button(Material.LIME_DYE, "Recover last trash",
                store.has(p.getUniqueId()) ? "Gives back your last trash (5 min)" : "Nothing to recover"));
        p.openInventory(inv);
    }

    private static ItemStack button(Material m, String name, String lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GREEN));
        if (lore != null) meta.lore(List.of(Component.text(lore, NamedTextColor.GRAY)));
        it.setItemMeta(meta);
        return it;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof TrashHolder)) return;
        int raw = e.getRawSlot();
        boolean topClick = raw >= 0 && raw < SlotPolicy.SIZE;
        if (topClick && SlotPolicy.isButton(raw)) {
            e.setCancelled(true);
            if (raw == SlotPolicy.UNDO_SLOT && e.getWhoClicked() instanceof Player p) {
                Bukkit.getScheduler().runTask(this, () -> { p.closeInventory(); undo(p); });
            }
            return;
        }
        // Shift-click from player inventory could land in a button slot; block if storage can't take it all.
        if (!topClick && e.isShiftClick()) {
            e.setCancelled(true);
            ItemStack cur = e.getCurrentItem();
            if (cur == null || cur.getType().isAir()) return;
            Inventory top = e.getView().getTopInventory();
            ItemStack left = cur.clone();
            for (int i = 0; i < SlotPolicy.STORAGE && left.getAmount() > 0; i++) {
                ItemStack slot = top.getItem(i);
                if (slot == null || slot.getType().isAir()) { top.setItem(i, left.clone()); left.setAmount(0); }
                else if (slot.isSimilar(left) && slot.getAmount() < slot.getMaxStackSize()) {
                    int mv = Math.min(slot.getMaxStackSize() - slot.getAmount(), left.getAmount());
                    slot.setAmount(slot.getAmount() + mv); left.setAmount(left.getAmount() - mv);
                }
            }
            e.setCurrentItem(left.getAmount() > 0 ? left : null);
            return;
        }
        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR || e.getClick() == ClickType.NUMBER_KEY && topClick && SlotPolicy.isButton(raw)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof TrashHolder)) return;
        for (int raw : e.getRawSlots()) if (SlotPolicy.isButton(raw)) { e.setCancelled(true); return; }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof TrashHolder)) return;
        if (!(e.getPlayer() instanceof Player p)) return;
        Inventory inv = e.getInventory();
        List<String> ser = new ArrayList<>();
        int items = 0;
        Map<Material, Integer> counts = new TreeMap<>();
        for (int i = 0; i < SlotPolicy.STORAGE; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType().isAir()) continue;
            ser.add(Base64.getEncoder().encodeToString(it.serializeAsBytes()));
            items += it.getAmount();
            counts.merge(it.getType(), it.getAmount(), Integer::sum);
            inv.setItem(i, null);
        }
        for (int i = SlotPolicy.STORAGE; i < SlotPolicy.SIZE; i++) inv.setItem(i, null);
        if (ser.isEmpty()) return; // ignore empty closes; keeps the previous batch
        store.put(p.getUniqueId(), ser);
        log.write("TRASH", p.getName(), p.getUniqueId(), ser.size(), items, summary(counts));
        p.sendMessage(Component.text("Trashed " + items + " items. Changed your mind? /trash undo within 5 minutes.", NamedTextColor.GRAY));
    }

    void undo(Player p) {
        Optional<List<String>> got = store.take(p.getUniqueId());
        if (got.isEmpty()) { p.sendMessage(Component.text("Nothing to recover (only your last trash, for 5 minutes).", NamedTextColor.RED)); return; }
        int items = 0, dropped = 0;
        Map<Material, Integer> counts = new TreeMap<>();
        for (String s : got.get()) {
            ItemStack it;
            try { it = ItemStack.deserializeBytes(Base64.getDecoder().decode(s)); } catch (RuntimeException ex) { continue; }
            items += it.getAmount();
            counts.merge(it.getType(), it.getAmount(), Integer::sum);
            for (ItemStack over : p.getInventory().addItem(it).values()) {
                dropped += over.getAmount();
                p.getWorld().dropItemNaturally(p.getLocation(), over);
            }
        }
        log.write("UNDO", p.getName(), p.getUniqueId(), got.get().size(), items, "dropped=" + dropped + " " + summary(counts));
        p.sendMessage(Component.text("Recovered " + items + " items" + (dropped > 0 ? " (" + dropped + " dropped at your feet)." : "."), NamedTextColor.GREEN));
    }

    static String summary(Map<Material, Integer> c) {
        return c.entrySet().stream().map(e -> e.getKey().name() + "x" + e.getValue()).collect(Collectors.joining(",", "[", "]"));
    }
}
