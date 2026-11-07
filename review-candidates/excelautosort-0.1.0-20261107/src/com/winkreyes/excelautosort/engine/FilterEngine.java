package com.winkreyes.excelautosort.engine;

import com.winkreyes.excelautosort.ExcelAutoSort;
import com.winkreyes.excelautosort.core.ChestKeys;
import com.winkreyes.excelautosort.core.FilterCore;
import com.winkreyes.excelautosort.core.Mode;
import com.winkreyes.excelautosort.core.ProtectEvents;
import com.winkreyes.excelautosort.store.BufferEntry;
import com.winkreyes.excelautosort.store.ChestRecord;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The filter-delete engine (SPEC §5–§6). Event-driven only: interaction
 * completion, close sweep, hopper feed, arm sweep. No task ever scans
 * chests; the only lookups are one map hit per event (§3 performance
 * law). Any doubt — unknown state, dead blocks, an unserializable
 * stack — means NO deletion (§3 fail-safe law), with the reason logged
 * at most once per chest per hour.
 */
public final class FilterEngine {

    private final ExcelAutoSort plugin;
    private final Map<UUID, Pending> pending = new HashMap<>();
    private final Map<String, Long> failLoggedAt = new HashMap<>();

    private record Pending(UUID actor, String kind) {}

    public FilterEngine(ExcelAutoSort plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------ resolution

    /** The record an inventory belongs to (either half of a double), or null. */
    public ChestRecord recordFor(Inventory inv) {
        if (inv == null) return null;
        InventoryHolder holder = inv.getHolder(false);
        if (holder instanceof DoubleChest dc) {
            for (String key : doubleKeys(dc)) {
                ChestRecord r = plugin.store().byKey(key);
                if (r != null) return r;
            }
            return null;
        }
        if (holder instanceof Container c) {
            return plugin.store().byKey(keyOf(c.getBlock()));
        }
        return null;
    }

    public static String keyOf(Block b) {
        return ChestKeys.key(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
    }

    /** Both block keys of a double chest, left half first. */
    public static List<String> doubleKeys(DoubleChest dc) {
        List<String> out = new ArrayList<>();
        if (dc.getLeftSide() instanceof Container left) out.add(keyOf(left.getBlock()));
        if (dc.getRightSide() instanceof Container right) out.add(keyOf(right.getBlock()));
        return out;
    }

    /** Block keys of a chest/barrel block (1 or 2), or empty when not a container block. */
    public static List<String> keysOfBlock(Block b) {
        if (b == null || !(b.getState() instanceof Container c)) return List.of();
        if (c.getInventory().getHolder(false) instanceof DoubleChest dc) return doubleKeys(dc);
        // A block state's own inventory NEVER reports a DoubleChest
        // holder — the paired inventory exists only while a player has
        // the chest open. The pairing itself lives in the Chest block
        // data (LEFT/RIGHT + facing): resolve the partner block from
        // it, or a double chest arms as a single-key record and the
        // break/span handling silently misses the unkeyed half (§5.5;
        // rig T4/T5 caught this — the holder check alone never fires).
        if (b.getBlockData() instanceof org.bukkit.block.data.type.Chest cd
                && cd.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE) {
            // Vanilla's own pairing rule (ChestBlock.getConnectedDirection):
            // a LEFT half's partner sits clockwise of its facing, a RIGHT
            // half's counterclockwise. Mirror it exactly — no other
            // pairing is a double chest to the game (rig-verified).
            org.bukkit.block.BlockFace toward = cd.getType()
                    == org.bukkit.block.data.type.Chest.Type.LEFT
                    ? rotateClockwise(cd.getFacing()) : rotateCounterClockwise(cd.getFacing());
            Block partner = b.getRelative(toward);
            if (partner.getType() == b.getType()
                    && partner.getBlockData() instanceof org.bukkit.block.data.type.Chest pd
                    && pd.getFacing() == cd.getFacing()
                    && pd.getType() != org.bukkit.block.data.type.Chest.Type.SINGLE) {
                return ChestKeys.canonical(List.of(keyOf(b), keyOf(partner)));
            }
        }
        return List.of(keyOf(b));
    }

    private static org.bukkit.block.BlockFace rotateClockwise(org.bukkit.block.BlockFace f) {
        return switch (f) {
            case NORTH -> org.bukkit.block.BlockFace.EAST;
            case EAST -> org.bukkit.block.BlockFace.SOUTH;
            case SOUTH -> org.bukkit.block.BlockFace.WEST;
            case WEST -> org.bukkit.block.BlockFace.NORTH;
            default -> f;
        };
    }

    private static org.bukkit.block.BlockFace rotateCounterClockwise(org.bukkit.block.BlockFace f) {
        return switch (f) {
            case NORTH -> org.bukkit.block.BlockFace.WEST;
            case WEST -> org.bukkit.block.BlockFace.SOUTH;
            case SOUTH -> org.bukkit.block.BlockFace.EAST;
            case EAST -> org.bukkit.block.BlockFace.NORTH;
            default -> f;
        };
    }

    /** The live inventory behind a set of block keys, or null when no key resolves. */
    public static Inventory inventoryOf(List<String> keys) {
        for (String key : keys) {
            ChestKeys.BlockKey bk = ChestKeys.parse(key);
            if (bk == null) continue;
            World w = Bukkit.getWorld(bk.world());
            if (w == null) continue;
            Block b = w.getBlockAt(bk.x(), bk.y(), bk.z());
            if (b.getState() instanceof Container c) return c.getInventory();
        }
        return null;
    }

    /** A chest's current contents as filter-core stacks (slot order), for models and checks. */
    public static List<FilterCore.ContentStack> contentsOf(Inventory inv) {
        List<FilterCore.ContentStack> out = new ArrayList<>();
        if (inv == null) return out;
        for (ItemStack s : inv.getContents()) {
            if (s != null && s.getType() != Material.AIR && s.getAmount() > 0) {
                out.add(new FilterCore.ContentStack(s.getType().name(), s.getAmount()));
            }
        }
        return out;
    }

    // ------------------------------------------------------------ enforcement triggers

    /** §5.3.1: after a click/drag sequence changed a marked chest, check it 1 tick later. */
    public void noteInteraction(Inventory topInventory, Player actor) {
        ChestRecord r = recordFor(topInventory);
        if (r != null) schedule(r, actor == null ? null : actor.getUniqueId(), ProtectEvents.KIND_PLAYER);
    }

    /** §5.7: a hopper moved something into a marked chest. */
    public void noteHopper(Inventory destination) {
        if (!plugin.getConfig().getBoolean("filter.hoppers", true)) return;
        ChestRecord r = recordFor(destination);
        if (r == null) return;
        List<String> worlds = plugin.getConfig().getStringList("filter.hopper-worlds");
        ChestKeys.BlockKey bk = r.canonicalBlock();
        if (!worlds.isEmpty() && (bk == null || !worlds.contains(bk.world()))) return;
        schedule(r, null, ProtectEvents.KIND_HOPPER);
    }

    /** §5.3.2: the close sweep — runs before the sort pass (§4). */
    public void closeSweep(Inventory inv, Player actor) {
        ChestRecord r = recordFor(inv);
        if (r != null) runCheck(r, actor == null ? null : actor.getUniqueId(), ProtectEvents.KIND_PLAYER);
    }

    private void schedule(ChestRecord r, UUID actor, String kind) {
        Pending prev = pending.get(r.id);
        if (prev != null) {
            // Coalesce; a human actor outranks automation for buffer credit.
            if (prev.actor() == null && actor != null) pending.put(r.id, new Pending(actor, kind));
            return;
        }
        pending.put(r.id, new Pending(actor, kind));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Pending p = pending.remove(r.id);
            if (p != null) runCheck(r, p.actor(), p.kind());
        }, 1L);
    }

    // ------------------------------------------------------------ the check

    /**
     * One filter pass over a record's live inventory. Rejects move whole
     * to the buffer — credited to the depositing player, or to the chest
     * owner when automation (a hopper) fed them (§6.2).
     */
    public void runCheck(ChestRecord r, UUID actor, String kind) {
        if (r.suspended || r.mode == Mode.OFF) return;
        Inventory inv = liveInventory(r);
        if (inv == null) return;
        UUID credit = ProtectEvents.KIND_HOPPER.equals(kind) || actor == null ? r.owner : actor;
        removeRejects(inv, r, r.mode, r.items, credit, actor, kind);
    }

    /**
     * Remove every stack in {@code inv} that the given mode + list
     * rejects, whole, to the credited player's buffer, one Protect
     * record per stack. Returns {stacks, items} removed. A stack that
     * cannot be buffered stays put (§3 fail-safe).
     */
    public int[] removeRejects(Inventory inv, ChestRecord r, Mode mode, java.util.Set<String> items,
                               UUID credit, UUID actor, String kind) {
        String actorName = actor == null ? null : nameOf(actor);
        int stacks = 0, total = 0;
        ItemStack[] contents = inv.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) continue;
            if (!FilterCore.isReject(mode, items, stack.getType().name())) continue;
            BufferEntry entry = plugin.buffers().addStack(credit, stack.clone(), r.canonicalKey(), kind);
            if (entry == null) {
                failOnce(r, "a filtered stack could not be buffered; it was left in the chest");
                continue;
            }
            inv.setItem(slot, null);
            stacks++;
            total += stack.getAmount();
            ChestKeys.BlockKey bk = r.canonicalBlock();
            plugin.sink().record(ProtectEvents.FILTER_DELETE, ProtectEvents.filterDelete(
                    bk == null ? "" : bk.world(), bk == null ? 0 : bk.x(), bk == null ? 0 : bk.y(), bk == null ? 0 : bk.z(),
                    r.canonicalKey(),
                    actor == null ? null : actor.toString(), actorName, kind,
                    r.owner == null ? null : r.owner.toString(), r.ownerName,
                    mode.id(), stack.getType().getKey().toString(),
                    entry.itemB64, stack.getAmount(),
                    entry.id, credit.toString(), entry.expiresAtMs));
        }
        if (stacks > 0) plugin.store().saveBuffers();
        return new int[]{stacks, total};
    }

    /**
     * The arm sweep (§5.8): the confirmer's one-time removal of the
     * rejects the NEW mode + list creates in the chest's current
     * contents. Credited to the confirmer, kind arm-sweep.
     */
    public int[] sweepRejects(ChestRecord r, Mode mode, java.util.Set<String> items, Player confirmer) {
        Inventory inv = liveInventory(r);
        if (inv == null) return new int[]{0, 0};
        return removeRejects(inv, r, mode, items,
                confirmer.getUniqueId(), confirmer.getUniqueId(), ProtectEvents.KIND_ARM_SWEEP);
    }

    /**
     * The record's live inventory, validating its blocks on the way
     * (§9 lazy validation): dead keys drop off the record (reshaped);
     * a record with no live container left is pruned (block-gone).
     * Returns null when nothing live backs the record right now —
     * including when the chunk simply is not loaded, which is NOT
     * pruned (an unloaded chunk is not a gone block).
     */
    public Inventory liveInventory(ChestRecord r) {
        List<String> alive = new ArrayList<>();
        Inventory found = null;
        boolean anyChunkLoaded = false;
        for (String key : new ArrayList<>(r.keys)) {
            ChestKeys.BlockKey bk = ChestKeys.parse(key);
            if (bk == null) continue;
            World w = Bukkit.getWorld(bk.world());
            if (w == null) continue;
            if (!w.isChunkLoaded(bk.x() >> 4, bk.z() >> 4)) continue;
            anyChunkLoaded = true;
            Block b = w.getBlockAt(bk.x(), bk.y(), bk.z());
            if (b.getState() instanceof Container c) {
                alive.add(key);
                if (found == null) found = c.getInventory();
            }
        }
        if (!anyChunkLoaded) return null;
        if (alive.isEmpty()) {
            unmark(r, "block-gone", null);
            return null;
        }
        if (alive.size() != r.keys.size()) {
            r.keys = ChestKeys.canonical(alive);
            plugin.store().reindexRecord(r);
            r.touch(System.currentTimeMillis());
            plugin.store().saveChests();
            emitReshaped(r, "keys-pruned");
        }
        return found;
    }

    private void failOnce(ChestRecord r, String why) {
        long now = System.currentTimeMillis();
        Long last = failLoggedAt.get(r.canonicalKey());
        if (last == null || now - last > 3_600_000L) {
            failLoggedAt.put(r.canonicalKey(), now);
            plugin.getLogger().warning("ExcelAutoSort: filter at " + r.canonicalKey() + " did nothing: " + why);
        }
    }

    private String nameOf(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        return p != null ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();
    }

    // ------------------------------------------------------------ record lifecycle

    public ChestRecord createRecord(List<String> keys, UUID owner, String ownerName) {
        ChestRecord r = new ChestRecord(UUID.randomUUID());
        r.keys = ChestKeys.canonical(keys);
        r.owner = owner;
        r.ownerName = ownerName == null ? "" : ownerName;
        long now = System.currentTimeMillis();
        r.createdMs = now;
        r.updatedMs = now;
        plugin.store().indexRecord(r);
        plugin.store().saveChests();
        return r;
    }

    /** Remove a record (buffer entries are never touched, §6.4). */
    public void unmark(ChestRecord r, String cause, UUID actor) {
        plugin.store().removeRecord(r);
        plugin.store().saveChests();
        ChestKeys.BlockKey bk = r.canonicalBlock();
        plugin.sink().record(ProtectEvents.UNMARK, ProtectEvents.unmark(
                bk == null ? "" : bk.world(), bk == null ? 0 : bk.x(), bk == null ? 0 : bk.y(), bk == null ? 0 : bk.z(),
                r.canonicalKey(), actor == null ? null : actor.toString(),
                r.owner == null ? null : r.owner.toString(), cause));
    }

    public void suspend(ChestRecord r, String reason) {
        if (r.suspended) return;
        r.suspended = true;
        r.suspendReason = reason;
        r.touch(System.currentTimeMillis());
        plugin.store().saveChests();
        ChestKeys.BlockKey bk = r.canonicalBlock();
        plugin.sink().record(ProtectEvents.FILTER_SUSPEND, ProtectEvents.suspend(
                bk == null ? "" : bk.world(), bk == null ? 0 : bk.x(), bk == null ? 0 : bk.y(), bk == null ? 0 : bk.z(),
                r.canonicalKey(), r.owner == null ? null : r.owner.toString(), reason));
    }

    /** §6.4: a claim under a record was deleted or changed — suspend everything inside it. */
    public void suspendInClaim(String world, int minX, int maxX, int minZ, int maxZ, String reason) {
        for (ChestRecord r : new ArrayList<>(plugin.store().chests.values())) {
            for (String key : r.keys) {
                ChestKeys.BlockKey bk = ChestKeys.parse(key);
                if (bk != null && bk.world().equals(world)
                        && bk.x() >= minX && bk.x() <= maxX && bk.z() >= minZ && bk.z() <= maxZ) {
                    suspend(r, reason);
                    break;
                }
            }
        }
    }

    public void emitReshaped(ChestRecord r, String detail) {
        ChestKeys.BlockKey bk = r.canonicalBlock();
        plugin.sink().record(ProtectEvents.CHEST_RESHAPED, ProtectEvents.chestReshaped(
                bk == null ? "" : bk.world(), bk == null ? 0 : bk.x(), bk == null ? 0 : bk.y(), bk == null ? 0 : bk.z(),
                r.canonicalKey(), r.owner == null ? null : r.owner.toString(), detail, r.keys.size()));
    }

    // ------------------------------------------------------------ block events (called by the listener)

    /** A container block broke (player or explosion): downgrade or unmark (§5.5, §6.4). */
    public void onBlockGone(Block block) {
        String key = keyOf(block);
        ChestRecord r = plugin.store().byKey(key);
        if (r == null) return;
        if (r.keys.size() > 1) {
            r.keys.remove(key);
            plugin.store().reindexRecord(r);
            r.touch(System.currentTimeMillis());
            plugin.store().saveChests();
            emitReshaped(r, "half-broken");
        } else {
            unmark(r, "block-break", null);
        }
    }

    /** §5.5: a chest placed beside a marked single may grow the record — or suspend it. */
    public void onChestPlaced(Block placed, Player placer) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<String> keys = keysOfBlock(placed);
            if (keys.size() != 2) return;
            ChestRecord r = null;
            for (String k : keys) {
                ChestRecord hit = plugin.store().byKey(k);
                if (hit != null) { r = hit; break; }
            }
            if (r == null || r.keys.size() != 1) return;
            // A marked single just became a double.
            if (placer != null && plugin.rights().canConfigure(placer, placed.getLocation(), r.owner)) {
                r.keys = ChestKeys.canonical(keys);
                plugin.store().reindexRecord(r);
                r.touch(System.currentTimeMillis());
                plugin.store().saveChests();
                emitReshaped(r, "span-grown");
            } else {
                suspend(r, "span-changed");
            }
        }, 1L);
    }

    /** /autosort admin prune (§8): drop records with no live container; returns the count. */
    public int adminPrune() {
        int n = 0;
        for (ChestRecord r : new ArrayList<>(plugin.store().chests.values())) {
            int before = plugin.store().chests.size();
            liveInventory(r);
            if (plugin.store().chests.size() < before) n++;
        }
        return n;
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
