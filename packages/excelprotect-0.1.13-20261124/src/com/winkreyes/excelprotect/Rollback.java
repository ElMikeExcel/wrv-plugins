package com.winkreyes.excelprotect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ExcelProtect — recovery operations (SPEC §5): block rollback to before-states
 * (sign lines reapplied), container restore from contents snapshots, death-snapshot
 * restore (once-only via Recovery marks, staff force override), armor-stand restore
 * from equipment snapshots. EVERY operation logs a system `rollback` event — never
 * silent. File scans happen before the call (async); these methods run on the main
 * thread and only touch the world. Excel — Wink Reyes Valley
 */
public final class Rollback {
    private final Ledger ledger;
    private final Settings cfg;
    private final Recovery recovery;

    public Rollback(Ledger ledger, Settings cfg, Recovery recovery) {
        this.ledger = ledger; this.cfg = cfg; this.recovery = recovery;
    }

    private void log(String action, Player actor, Location loc, String detail) {
        Json.Obj o = new Json.Obj().put("type", "rollback").put("action", action).put("detail", detail);
        if (actor != null) o.put("player", actor.getName()).put("player_uuid", actor.getUniqueId().toString());
        ledger.emit("system", loc, o);
    }

    /** Latest event per position at/before the target (the rollback selection). */
    public static Map<String, String> chooseBlocks(List<String> lines, long targetMs) {
        Map<String, String> chosen = new HashMap<>();
        for (String line : lines) {
            long ts = (long) Json.numField(line, "ts_ms", 0);
            if (ts > targetMs) continue;
            String w = Json.strField(line, "world");
            if (w == null || Json.strField(line, "before") == null) continue;
            String key = w + "@" + (long) Json.numField(line, "x", 0) + "," + (long) Json.numField(line, "y", 0)
                    + "," + (long) Json.numField(line, "z", 0);
            String prev = chosen.get(key);
            if (prev == null || ts >= (long) Json.numField(prev, "ts_ms", 0)) chosen.put(key, line);
        }
        return chosen;
    }

    /** Capture the CURRENT world state at the chosen positions as undo lines (Amendment 3.3). */
    public List<String> captureUndo(Map<String, String> chosen) {
        List<String> undo = new ArrayList<>();
        for (String line : chosen.values()) {
            World w = Bukkit.getWorld(Json.strField(line, "world"));
            if (w == null) continue;
            Block b = w.getBlockAt((int) Json.numField(line, "x", 0), (int) Json.numField(line, "y", 0),
                    (int) Json.numField(line, "z", 0));
            Json.Obj o = new Json.Obj().put("type", "block_change")
                    .put("world", w.getName())
                    .put("x", b.getX()).put("y", b.getY()).put("z", b.getZ())
                    .put("ts_ms", System.currentTimeMillis())
                    .put("before", b.getBlockData().getAsString());
            String[] lines2 = Blocks.signLines(b.getState());
            if (lines2 != null) {
                Json.Arr a = new Json.Arr();
                for (String s : lines2) a.add(s);
                o.put("sign_before", a);
            }
            undo.add(o.render());
        }
        return undo;
    }

    /** lines: block_change events (any order). Latest per position wins; applies its before-state. */
    public int rollbackBlocks(List<String> lines, long targetMs, Player actor) {
        Map<String, String> chosen = chooseBlocks(lines, targetMs);
        int applied = 0;
        for (String line : chosen.values()) {
            World w = Bukkit.getWorld(Json.strField(line, "world"));
            if (w == null) continue;
            Block b = w.getBlockAt((int) Json.numField(line, "x", 0), (int) Json.numField(line, "y", 0),
                    (int) Json.numField(line, "z", 0));
            try {
                BlockData data = Bukkit.createBlockData(Json.strField(line, "before"));
                b.setBlockData(data, false);
                String signRaw = Json.rawField(line, "sign_before");
                if (signRaw != null && b.getState() instanceof Sign sign) {
                    List<String> ls = Json.strings(signRaw);
                    for (int i = 0; i < Math.min(4, ls.size()); i++)
                        sign.getSide(Side.FRONT).setLine(i, ls.get(i));
                    sign.update(true, false);
                }
                applied++;
            } catch (Exception ignored) { }
        }
        log("blocks", actor, actor == null ? null : actor.getLocation(),
                "positions=" + chosen.size() + " applied=" + applied + " target=" + Ledger.isoOf(targetMs));
        return applied;
    }

    /** The container line a rollback to targetMs restores from, out of one position's lines: the latest at/before target carrying a contents snapshot. */
    public static String chooseContainerLine(List<String> lines, long targetMs) {
        String best = null; long bestTs = -1;
        for (String line : lines) {
            long ts = (long) Json.numField(line, "ts_ms", 0);
            if (ts <= targetMs && ts > bestTs && Json.rawField(line, "contents_after") != null) { best = line; bestTs = ts; }
        }
        return best;
    }

    /** lines: container_transaction events for one position; latest at/before target restores contents_after. */
    public boolean rollbackContainer(List<String> lines, long targetMs, Player actor) {
        String best = chooseContainerLine(lines, targetMs);
        if (best == null) return false;
        long bestTs = (long) Json.numField(best, "ts_ms", 0);
        World w = Bukkit.getWorld(Json.strField(best, "world"));
        if (w == null) return false;
        Block b = w.getBlockAt((int) Json.numField(best, "x", 0), (int) Json.numField(best, "y", 0),
                (int) Json.numField(best, "z", 0));
        BlockState st = b.getState();
        if (!(st instanceof InventoryHolder holder)) return false;
        Inventory inv = holder.getInventory();
        List<ItemFact> facts = ItemFact.listFromJson(Json.rawField(best, "contents_after"));
        inv.clear();
        int slot = 0;
        for (ItemFact f : facts) {
            ItemStack s = f.restore();
            if (s != null && slot < inv.getSize()) inv.setItem(slot++, s);
        }
        log("container", actor, b.getLocation(), "restored from ts=" + Ledger.isoOf(bestTs) + " items=" + facts.size());
        return true;
    }

    // ---------- 0.1.6: container undo capture (A3.3 completed for containers) ----------

    /** One undo line freezing a container's CURRENT contents as a contents_after snapshot. Pure builder — the world read lives in captureUndoContainers. */
    public static String containerUndoLine(String world, long x, long y, long z, Json.Arr contentsAfter, long tsMs) {
        return new Json.Obj().put("type", "container_transaction")
                .put("world", world).put("x", x).put("y", y).put("z", z).put("ts_ms", tsMs)
                .put("contents_after", contentsAfter).render();
    }

    /**
     * Capture the CURRENT contents of every container position a rollback is
     * about to touch, as undo lines in the same shape rollbackContainer
     * consumes — so /excelprotect undo reverses the op's container restores
     * too, not just its block restores (pre-0.1.6 gap: captureUndo covered
     * blocks only). Payloads are captured so the undo restore is byte-exact.
     */
    public List<String> captureUndoContainers(Map<String, List<String>> byPos) {
        List<String> undo = new ArrayList<>();
        for (List<String> group : byPos.values()) {
            if (group.isEmpty()) continue;
            String first = group.get(0);
            String wName = Json.strField(first, "world");
            World w = wName == null ? null : Bukkit.getWorld(wName);
            if (w == null) continue;
            Block b = w.getBlockAt((int) Json.numField(first, "x", 0), (int) Json.numField(first, "y", 0),
                    (int) Json.numField(first, "z", 0));
            if (!(b.getState() instanceof InventoryHolder holder)) continue;
            Json.Arr contents = new Json.Arr();
            for (ItemStack s : holder.getInventory().getContents()) {
                ItemFact f = ItemFact.of(s, true);
                if (f != null) contents.add(f.toJson());
            }
            undo.add(containerUndoLine(wName, b.getX(), b.getY(), b.getZ(), contents, System.currentTimeMillis()));
        }
        return undo;
    }

    // ---------- 0.1.6: entity rollback (generalized restore-stand) ----------

    /** The entity_death line a restore at targetMs uses: the latest at/before target. */
    public static String chooseEntityLine(List<String> lines, long targetMs) {
        String best = null; long bestTs = -1;
        for (String line : lines) {
            if (!"entity_death".equals(Json.strField(line, "type"))) continue;
            long ts = (long) Json.numField(line, "ts_ms", 0);
            if (ts <= targetMs && ts > bestTs) { best = line; bestTs = ts; }
        }
        return best;
    }

    /** Once-only mark key for an entity_death line (dupe safety, Recovery pattern). */
    public static String entityMark(String line) {
        String uuid = Json.strField(line, "entity_uuid");
        if (uuid != null) return "entity:" + uuid;
        return "entity:" + (long) Json.numField(line, "ts_ms", 0) + ":" + Json.strField(line, "entity");
    }

    private static org.bukkit.entity.EntityType entityTypeOf(String key) {
        if (key == null) return null;
        try {
            org.bukkit.NamespacedKey nk = org.bukkit.NamespacedKey.fromString(key);
            if (nk != null) {
                org.bukkit.entity.EntityType t = org.bukkit.Registry.ENTITY_TYPE.get(nk);
                if (t != null) return t;
            }
        } catch (Throwable ignored) { }
        try {
            String bare = key.substring(key.indexOf(':') + 1).toUpperCase(java.util.Locale.ROOT);
            return org.bukkit.entity.EntityType.valueOf(bare);
        } catch (Throwable ignored) { return null; }
    }

    /** Dress an equipment holder from a recorded equipment snapshot (payloads restore byte-exact stacks). Shared by restoreStand + restoreEntity. */
    private static void dressEquipment(org.bukkit.inventory.EntityEquipment eq, String eqRaw) {
        if (eq == null || eqRaw == null) return;
        for (String slot : new String[]{"helmet", "chestplate", "leggings", "boots", "main_hand", "off_hand"}) {
            String raw = Json.rawField(eqRaw, slot);
            if (raw == null) continue;
            List<ItemFact> facts = ItemFact.listFromJson("[" + raw + "]");
            if (facts.isEmpty()) continue;
            ItemStack s = facts.get(0).restore();
            if (s == null) continue;
            switch (slot) {
                case "helmet" -> eq.setHelmet(s);
                case "chestplate" -> eq.setChestplate(s);
                case "leggings" -> eq.setLeggings(s);
                case "boots" -> eq.setBoots(s);
                case "main_hand" -> eq.setItemInMainHand(s);
                case "off_hand" -> eq.setItemInOffHand(s);
                default -> { }
            }
        }
    }

    /**
     * Entity rollback (0.1.6): re-create the entity an entity_death record
     * describes — a FRESH spawn of the recorded type at the recorded
     * position, with the recorded custom name, equipment snapshot
     * (byte-exact) and, for tameables, the recorded owner. Once-only via
     * Recovery marks (key entity:<uuid>); staff force overrides.
     * HONEST BOUNDARY: the record carries no health/AI/passenger state, so
     * the restored entity is a fresh spawn at full health with default AI
     * and a NEW uuid — what the records honestly support, nothing more.
     */
    public String restoreEntity(String line, Player actor, boolean force) {
        if (!cfg.entityRestore()) return "entity restore is disabled in config";
        String mark = entityMark(line);
        if (recovery.isRestored(mark) && !force) return "already restored (staff: add force)";
        String key = Json.strField(line, "entity");
        org.bukkit.entity.EntityType type = entityTypeOf(key);
        if (type == null || !type.isSpawnable()) return "entity type not restorable from records: " + key;
        String wName = Json.strField(line, "world");
        World w = wName == null ? null : Bukkit.getWorld(wName);
        if (w == null) return "world not loaded: " + wName;
        Location loc = new Location(w, Json.numField(line, "x", 0), Json.numField(line, "y", 0),
                Json.numField(line, "z", 0));
        org.bukkit.entity.Entity ent;
        try { ent = w.spawnEntity(loc, type); }
        catch (Exception e) { return "spawn failed for " + key + ": " + e.getMessage(); }
        String name = Json.strField(line, "name");
        if (name != null) { ent.setCustomName(name); ent.setCustomNameVisible(true); }
        if (ent instanceof org.bukkit.entity.LivingEntity le) {
            dressEquipment(le.getEquipment(), Json.rawField(line, "equipment"));
            if (ent instanceof ArmorStand stand) stand.setArms(true);
            if (ent instanceof org.bukkit.entity.Tameable tame) {
                String ownerUuid = Json.strField(line, "owner_uuid");
                if (ownerUuid != null) {
                    try {
                        tame.setOwner(Bukkit.getOfflinePlayer(java.util.UUID.fromString(ownerUuid)));
                        tame.setTamed(true);
                    } catch (Throwable ignored) { }
                }
            }
        }
        recovery.markRestored(mark);
        log("entity_restore", actor, loc, "entity=" + key + (name != null ? " name=" + name : "")
                + " from entity_death ts=" + Ledger.isoOf((long) Json.numField(line, "ts_ms", 0)) + (force ? " FORCE" : ""));
        return "restored " + key + (name != null ? " '" + name + "'" : "") + " at " + loc.getBlockX() + ","
                + loc.getBlockY() + "," + loc.getBlockZ() + " (fresh spawn — new uuid, full health, default AI)";
    }

    // ---------- 0.1.6: inventory rollback from death snapshots ----------

    /** The death_snapshot line an inventory restore at targetMs uses: the latest at/before target. */
    public static String chooseSnapshot(List<String> lines, long targetMs) {
        String best = null; long bestTs = -1;
        for (String line : lines) {
            if (!"death_snapshot".equals(Json.strField(line, "type"))) continue;
            long ts = (long) Json.numField(line, "ts_ms", 0);
            if (ts <= targetMs && ts > bestTs) { best = line; bestTs = ts; }
        }
        return best;
    }

    /** {inventory stacks, ender-chest stacks} a snapshot can actually restore (payload-carrying facts only). */
    public static int[] snapshotCounts(String line) {
        return new int[]{countRestorable(Json.rawField(line, "inventory")),
                countRestorable(Json.rawField(line, "ender_chest"))};
    }

    private static int countRestorable(String raw) {
        if (raw == null) return 0;
        int n = 0;
        for (ItemFact f : ItemFact.slotListFromJson(raw)) if (f != null && f.payload() != null) n++;
        return n;
    }

    /** Write one snapshot array into an inventory SLOT-EXACTLY (recorded empties clear their slots). Returns stacks placed. */
    private static int applySlots(Inventory inv, String raw) {
        if (raw == null) return 0;
        List<ItemFact> slots = ItemFact.slotListFromJson(raw);
        ItemStack[] full = new ItemStack[inv.getSize()];
        int n = 0;
        for (int i = 0; i < Math.min(full.length, slots.size()); i++) {
            ItemFact f = slots.get(i);
            ItemStack s = f == null ? null : f.restore();
            full[i] = s;
            if (s != null) n++;
        }
        inv.setContents(full);
        return n;
    }

    /**
     * Inventory rollback (0.1.6): REPLACE the target's inventory + ender
     * chest with a death snapshot's recorded contents, slot for slot
     * (empties included) — the point-in-time counterpart to restoreDeath's
     * additive give-back. Shares restoreDeath's once-only mark
     * (death:<death_id>) so a snapshot can be materialized exactly once
     * across BOTH paths unless staff force — the dupe-safety invariant.
     * XP is NOT touched here; XP restore stays with the deaths flow.
     * Source boundary: only death snapshots carry byte-exact payloads —
     * the join/leave inventory_snapshot records are fingerprint-only
     * (Amendment 3.2, diff-able by design) and cannot materialize items.
     */
    public String restoreInventory(String line, Player target, Player actor, boolean force) {
        String deathId = Json.strField(line, "death_id");
        if (deathId == null) return "snapshot has no death_id";
        if (recovery.isRestored("death:" + deathId) && !force) return "already restored (staff: add force)";
        int invN = applySlots(target.getInventory(), Json.rawField(line, "inventory"));
        int endN = applySlots(target.getEnderChest(), Json.rawField(line, "ender_chest"));
        recovery.markRestored("death:" + deathId);
        log("inventory_restore", actor, target.getLocation(), "death_id=" + deathId + " target=" + target.getName()
                + " inventory=" + invN + " ender=" + endN + (force ? " FORCE" : ""));
        return "inventory restored from the death snapshot of " + Ledger.isoOf((long) Json.numField(line, "ts_ms", 0))
                + ": " + invN + " inventory stacks, " + endN + " ender-chest stacks (REPLACE — prior contents overwritten)";
    }

    /** lines: exactly the death_snapshot line chosen by the caller. Returns result text. */
    public String restoreDeath(String line, Player target, Player actor, boolean force) {
        String deathId = Json.strField(line, "death_id");
        if (deathId == null) return "snapshot has no death_id";
        if (recovery.isRestored("death:" + deathId) && !force) return "already restored (staff: add force)";
        List<String> restoredKinds = new ArrayList<>();
        for (String field : new String[]{"inventory", "ender_chest", "drops"}) {
            String raw = Json.rawField(line, field);
            if (raw == null) continue;
            int given = 0;
            for (ItemFact f : ItemFact.listFromJson(raw)) {
                ItemStack s = f.restore();
                if (s == null) continue;
                if (field.equals("ender_chest")) {
                    if (target.getEnderChest().firstEmpty() >= 0) target.getEnderChest().addItem(s);
                } else {
                    target.getInventory().addItem(s);
                }
                given++;
            }
            if (given > 0) restoredKinds.add(field + "=" + given);
        }
        int xp = (int) Json.numField(line, "xp_level", -1);
        if (xp >= 0) { target.setLevel(xp); restoredKinds.add("xp_level=" + xp); }
        recovery.markRestored("death:" + deathId);
        log("death_restore", actor, target.getLocation(),
                "death_id=" + deathId + " target=" + target.getName() + " " + restoredKinds + (force ? " FORCE" : ""));
        return "restored " + restoredKinds;
    }

    /** lines: an entity_death line with an equipment snapshot. Re-dresses a fresh stand. */
    public String restoreStand(String line, Player actor) {
        if (!cfg.entityRestore()) return "entity restore is disabled in config";
        String eqRaw = Json.rawField(line, "equipment");
        if (eqRaw == null) return "event has no equipment snapshot";
        World w = Bukkit.getWorld(Json.strField(line, "world"));
        if (w == null) return "world not loaded";
        Location loc = new Location(w, Json.numField(line, "x", 0), Json.numField(line, "y", 0),
                Json.numField(line, "z", 0));
        ArmorStand stand = (ArmorStand) w.spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setArms(true);
        dressEquipment(stand.getEquipment(), eqRaw);
        String name = Json.strField(line, "name");
        if (name != null) stand.setCustomName(name);
        log("stand_restore", actor, loc, "from entity_death ts=" + Ledger.isoOf((long) Json.numField(line, "ts_ms", 0)));
        return "stand restored at " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}
