package com.winkreyes.excelprotect;

import org.bukkit.Location;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelProtect — containers family: open-snapshot + close-diff transactions for block
 * containers AND ender chests (audit §1.3: the ender chest's holder is the player — it
 * is tracked explicitly). Every transaction stores the full resulting contents
 * (fingerprints + payloads) — the snapshot container rollback restores from.
 * Only item movements are recorded; nothing is ever cancelled. Excel — Wink Reyes Valley
 */
public final class Containers implements Listener {
    private final Ledger ledger;
    private final Settings cfg;
    private volatile Watch watch;
    private final Map<UUID, Open> open = new HashMap<>();

    private record Open(Location loc, String kind, List<ItemFact> before) { }

    public Containers(Ledger ledger, Settings cfg) { this.ledger = ledger; this.cfg = cfg; }
    public void setWatch(Watch w) { this.watch = w; }

    static List<ItemFact> facts(ItemStack[] contents, boolean payloads) {
        List<ItemFact> out = new ArrayList<>();
        for (ItemStack s : contents) {
            ItemFact f = ItemFact.of(s, payloads);
            if (f != null) out.add(f);
        }
        return out;
    }

    /** Merge identical fingerprints for compact diffs (payload of the first kept). */
    static Map<String, ItemFact> merged(List<ItemFact> facts) {
        Map<String, ItemFact> m = new HashMap<>();
        for (ItemFact f : facts) {
            String k = f.id() + "|" + (f.name() == null ? "" : f.name()) + "|" + f.damage()
                    + "|" + (f.enchants() == null ? "" : f.enchants().toString());
            ItemFact prev = m.get(k);
            m.put(k, prev == null ? f : new ItemFact(f.id(), prev.count() + f.count(), f.damage(),
                    f.name(), f.enchants(), f.trimMaterial(), f.trimPattern(),
                    prev.payload() != null ? prev.payload() : f.payload(), f.rid()));
        }
        return m;
    }

    private Location locate(Inventory inv) {
        InventoryHolder h = inv.getHolder();
        if (h instanceof Container c) return c.getLocation();
        if (h instanceof DoubleChest dc) return dc.getLocation();
        if (h instanceof Player p) return p.getLocation();
        return null;
    }

    private boolean tracked(Inventory inv) {
        if (inv.getType() == InventoryType.ENDER_CHEST) return true;
        InventoryHolder h = inv.getHolder();
        return h instanceof Container || h instanceof DoubleChest;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent e) {
        if (!cfg.containers() || !(e.getPlayer() instanceof Player p)) return;
        Inventory inv = e.getInventory();
        if (!tracked(inv)) return;
        Location loc = locate(inv);
        boolean ender = inv.getType() == InventoryType.ENDER_CHEST;
        open.put(p.getUniqueId(), new Open(loc, ender ? "ender_chest" : inv.getType().name().toLowerCase(),
                facts(inv.getContents(), false)));
        if (ender && cfg.auditEnderchest())
            ledger.emit("audit", p.getLocation(), new Json.Obj().put("type", "enderchest_open")
                    .put("player", p.getName()).put("player_uuid", p.getUniqueId().toString()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent e) {
        if (!cfg.containers() || !(e.getPlayer() instanceof Player p)) return;
        Open o = open.remove(p.getUniqueId());
        if (o == null) return;
        Inventory inv = e.getInventory();
        if (!tracked(inv)) return;
        List<ItemFact> after = facts(inv.getContents(), true);
        Map<String, ItemFact> b = merged(o.before()), a = merged(after);
        Json.Arr dep = new Json.Arr(), wd = new Json.Arr();
        for (Map.Entry<String, ItemFact> en : a.entrySet()) {
            ItemFact bf = b.get(en.getKey());
            int d = en.getValue().count() - (bf == null ? 0 : bf.count());
            if (d > 0) dep.add(new ItemFact(en.getValue().id(), d, en.getValue().damage(), en.getValue().name(),
                    en.getValue().enchants(), en.getValue().trimMaterial(), en.getValue().trimPattern(), null, null).toJson());
        }
        for (Map.Entry<String, ItemFact> en : b.entrySet()) {
            ItemFact af = a.get(en.getKey());
            int d = en.getValue().count() - (af == null ? 0 : af.count());
            if (d > 0) wd.add(new ItemFact(en.getValue().id(), d, en.getValue().damage(), en.getValue().name(),
                    en.getValue().enchants(), en.getValue().trimMaterial(), en.getValue().trimPattern(), null, null).toJson());
        }
        Location loc = o.loc() != null ? o.loc() : p.getLocation();
        Watch w = watch;
        if (w != null) w.containerAccess(p, loc);
        Json.Obj ev = new Json.Obj().put("type", "container_transaction")
                .put("container", o.kind())
                .put("player", p.getName()).put("player_uuid", p.getUniqueId().toString());
        if (dep.size() > 0) ev.put("deposits", dep);
        if (wd.size() > 0) ev.put("withdrawals", wd);
        Json.Arr contents = new Json.Arr();
        for (ItemFact f : after) contents.add(f.toJson());
        ev.put("contents_after", contents);
        ledger.emit("containers", loc, ev);
        if (o.kind().equals("ender_chest") && cfg.auditEnderchest())
            ledger.emit("audit", p.getLocation(), new Json.Obj().put("type", "enderchest_close")
                    .put("player", p.getName()).put("player_uuid", p.getUniqueId().toString()));
    }
}
