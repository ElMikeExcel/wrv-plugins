package com.winkreyes.excelautosort.engine;

import com.winkreyes.excelautosort.ExcelAutoSort;
import com.winkreyes.excelautosort.core.BufferCore;
import com.winkreyes.excelautosort.core.ProtectEvents;
import com.winkreyes.excelautosort.store.BufferEntry;
import com.winkreyes.excelautosort.store.ItemCodec;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The recovery buffer (SPEC §6.2): every stack the filter removes lands
 * here first, whole, with its full payload, for the configured window
 * (default 7 days — DECIDED Nov 6, 2026). Expiry and cap overflow are
 * the only permanent deletions, and both are Protect-logged. Claiming
 * is logged too, so the chain of custody is complete end to end.
 */
public final class BufferService {

    private final ExcelAutoSort plugin;

    public BufferService(ExcelAutoSort plugin) {
        this.plugin = plugin;
    }

    private long windowMs() {
        return plugin.getConfig().getLong("recovery.hours", 168) * 3_600_000L;
    }

    private int maxStacks() {
        return plugin.getConfig().getInt("recovery.max-stacks", 162);
    }

    /**
     * Move one whole stack into a player's buffer. Returns the entry, or
     * null when the stack could not be serialized — the caller MUST then
     * leave the stack where it was (§3 fail-safe law).
     */
    public BufferEntry addStack(UUID creditTo, ItemStack stack, String chestKey, String cause) {
        String b64;
        try {
            b64 = ItemCodec.toB64(stack);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("ExcelAutoSort: could not serialize a filtered stack ("
                    + stack.getType() + ") — leaving it in the chest");
            return null;
        }
        if (b64.isEmpty()) return null;
        long now = System.currentTimeMillis();
        BufferEntry entry = new BufferEntry(
                UUID.randomUUID().toString(), creditTo, b64,
                ItemCodec.materialKeyOf(stack), stack.getAmount(),
                now, now + windowMs(), chestKey, cause);
        plugin.store().buffers.computeIfAbsent(creditTo, k -> new ArrayList<>()).add(entry);
        purgeOwner(creditTo, now);
        plugin.store().saveBuffers();
        return entry;
    }

    /** Live (unexpired) entries for a player, display order; expired ones purge first. */
    public List<BufferEntry> entriesOf(UUID owner) {
        purgeOwner(owner, System.currentTimeMillis());
        List<BufferEntry> list = plugin.store().buffers.getOrDefault(owner, List.of());
        List<BufferCore.TimedEntry> order = BufferCore.displayOrder(
                list.stream().map(BufferEntry::timed).toList());
        List<BufferEntry> out = new ArrayList<>();
        for (BufferCore.TimedEntry t : order) {
            for (BufferEntry e : list) {
                if (e.id.equals(t.id())) { out.add(e); break; }
            }
        }
        return out;
    }

    public int countOf(UUID owner) {
        purgeOwner(owner, System.currentTimeMillis());
        return plugin.store().buffers.getOrDefault(owner, List.of()).size();
    }

    /** Claim one entry: into the inventory, overflow at the player's feet (§6.2). */
    public boolean claim(Player player, String entryId) {
        List<BufferEntry> list = plugin.store().buffers.get(player.getUniqueId());
        if (list == null) return false;
        BufferEntry found = null;
        for (BufferEntry e : list) if (e.id.equals(entryId)) { found = e; break; }
        if (found == null) return false;
        ItemStack stack = ItemCodec.fromB64(found.itemB64);
        if (stack == null) {
            plugin.getLogger().warning("ExcelAutoSort: a buffer entry would not deserialize ("
                    + found.id + ") — leaving it in the buffer");
            return false;
        }
        list.remove(found);
        if (list.isEmpty()) plugin.store().buffers.remove(player.getUniqueId());
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        plugin.sink().record(ProtectEvents.RECOVER_CLAIM, ProtectEvents.recoverClaim(
                player.getUniqueId().toString(), player.getName(),
                found.owner.toString(), found.id, found.materialKey, found.count, found.chestKey));
        plugin.store().saveBuffers();
        return true;
    }

    /** Claim everything; returns how many stacks were claimed. */
    public int claimAll(Player player) {
        List<BufferEntry> entries = entriesOf(player.getUniqueId());
        int n = 0;
        for (BufferEntry e : entries) if (claim(player, e.id)) n++;
        return n;
    }

    /** Apply the purge plan for one owner, emitting a record per permanent deletion. */
    public void purgeOwner(UUID owner, long nowMs) {
        List<BufferEntry> list = plugin.store().buffers.get(owner);
        if (list == null || list.isEmpty()) return;
        List<BufferCore.TimedEntry> timed = list.stream().map(BufferEntry::timed).toList();
        List<BufferCore.Purge> plan = BufferCore.purgePlan(timed, nowMs, maxStacks());
        if (plan.isEmpty()) return;
        for (BufferCore.Purge p : plan) {
            Iterator<BufferEntry> it = list.iterator();
            while (it.hasNext()) {
                BufferEntry e = it.next();
                if (e.id.equals(p.id())) {
                    it.remove();
                    plugin.sink().record(ProtectEvents.BUFFER_PURGE, ProtectEvents.bufferPurge(
                            owner.toString(), e.id, e.materialKey, e.count,
                            p.reason() == BufferCore.PurgeReason.EXPIRED ? "expired" : "overflow",
                            e.chestKey));
                    break;
                }
            }
        }
        if (list.isEmpty()) plugin.store().buffers.remove(owner);
        plugin.store().saveBuffers();
    }

    /** The hourly sweep (§3): over the in-memory index only — never the world. */
    public void sweepAll() {
        long now = System.currentTimeMillis();
        for (UUID owner : new ArrayList<>(plugin.store().buffers.keySet())) {
            purgeOwner(owner, now);
        }
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
