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

    /** lines: container_transaction events for one position; latest at/before target restores contents_after. */
    public boolean rollbackContainer(List<String> lines, long targetMs, Player actor) {
        String best = null; long bestTs = -1;
        for (String line : lines) {
            long ts = (long) Json.numField(line, "ts_ms", 0);
            if (ts <= targetMs && ts > bestTs && Json.rawField(line, "contents_after") != null) { best = line; bestTs = ts; }
        }
        if (best == null) return false;
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
        Map<String, String> slots = Map.of(
                "helmet", "helmet", "chestplate", "chestplate", "leggings", "leggings",
                "boots", "boots", "main_hand", "main", "off_hand", "off");
        for (String slot : slots.keySet()) {
            String raw = Json.rawField(eqRaw, slot);
            if (raw == null) continue;
            List<ItemFact> facts = ItemFact.listFromJson("[" + raw + "]");
            if (facts.isEmpty()) continue;
            ItemStack s = facts.get(0).restore();
            if (s == null) continue;
            switch (slot) {
                case "helmet" -> stand.getEquipment().setHelmet(s);
                case "chestplate" -> stand.getEquipment().setChestplate(s);
                case "leggings" -> stand.getEquipment().setLeggings(s);
                case "boots" -> stand.getEquipment().setBoots(s);
                case "main_hand" -> stand.getEquipment().setItemInMainHand(s);
                case "off_hand" -> stand.getEquipment().setItemInOffHand(s);
                default -> { }
            }
        }
        String name = Json.strField(line, "name");
        if (name != null) stand.setCustomName(name);
        log("stand_restore", actor, loc, "from entity_death ts=" + Ledger.isoOf((long) Json.numField(line, "ts_ms", 0)));
        return "stand restored at " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}
