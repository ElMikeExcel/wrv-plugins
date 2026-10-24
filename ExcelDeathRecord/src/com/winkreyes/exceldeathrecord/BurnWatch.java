package com.winkreyes.exceldeathrecord;

import com.winkreyes.exceldeathrecord.core.Attribution;
import com.winkreyes.exceldeathrecord.core.BurnCause;
import com.winkreyes.exceldeathrecord.core.BurnFormat;
import com.winkreyes.exceldeathrecord.core.BurnIndex;
import com.winkreyes.exceldeathrecord.core.DeathRecord;
import com.winkreyes.exceldeathrecord.core.DeathSnapshot;
import com.winkreyes.exceldeathrecord.core.DestructionGuard;
import com.winkreyes.exceldeathrecord.core.DestructionRecord;
import com.winkreyes.exceldeathrecord.core.RecordName;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Burn Ledger: records item stacks destroyed in the world
 * by LAVA, FIRE, or VOID — the losses that leave no drops to
 * audit, and the reason this ledger exists (a sword that burns
 * is gone from the world a tick after it lands; the record must
 * already be complete).
 *
 * Three capture paths watch each item, deduped to exactly one
 * record by the core DestructionGuard:
 *   1. EntityRemoveEvent — DEATH removals take their cause from
 *      the item's last damage; OUT_OF_WORLD removals are VOID.
 *   2. EntityDamageEvent at MONITOR for LAVA/FIRE/FIRE_TICK/VOID
 *      damage: the stack is serialized AT the blow, and a
 *      next-tick follow-up records it if the entity is gone —
 *      covers any build where the remove event's cause differs.
 *   3. A half-second sweep for item entities already below
 *      their world's minimum height: void falls, recorded on
 *      the way down regardless of event plumbing.
 * EntityCombustEvent is deliberately NOT a trigger: combustion
 * alone destroys nothing (the item can still be rescued); the
 * lethal outcome of burning arrives as FIRE/FIRE_TICK damage
 * and is covered by paths 1 and 2.
 *
 * Natural DESPAWN is recorded only for enchanted or
 * custom-named items (ItemDespawnEvent + the core's valuable
 * filter); plain despawn is noise, by the owner's call.
 *
 * Attribution: the entity's own owner (Item#getOwner — set for
 * player drops, and death drops carry the deceased) first, the
 * PlayerDropItemEvent memory second, unknown when neither
 * knows. All state here is main-thread (events + tasks), so
 * plain maps suffice.
 */
final class BurnWatch implements Listener {

    /** What the lethal-damage path stashes at the blow, in case
     *  the entity is unreadable by the time the record writes. */
    private static final class Pending {
        DeathSnapshot.RawItem raw;
        String world;
        double x, y, z;
        UUID entityOwner;
        long ticksLived;
    }

    private final ExcelDeathRecord plugin;
    private final RecordStore store;
    private final DestructionGuard guard = new DestructionGuard(10 * 60 * 1000L);
    private final Map<UUID, Attribution.DropInfo> tracked = new LinkedHashMap<>() {
        @Override protected boolean removeEldestEntry(Map.Entry<UUID, Attribution.DropInfo> eldest) {
            return size() > 4096; // bounded memory on a long-running server
        }
    };
    private final Map<UUID, Pending> pending = new HashMap<>();

    BurnWatch(ExcelDeathRecord plugin, RecordStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    void startVoidSweep() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::sweepVoid, 20L, 10L);
    }

    // ---- drop tracking (attribution) ----

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
        Attribution.DropInfo info = new Attribution.DropInfo();
        info.ownerUuid = event.getPlayer().getUniqueId().toString();
        info.ownerName = event.getPlayer().getName();
        info.droppedEpochMillis = System.currentTimeMillis();
        tracked.put(item.getUniqueId(), info);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPickup(EntityPickupItemEvent event) {
        tracked.remove(event.getItem().getUniqueId());
        pending.remove(event.getItem().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUnload(EntitiesUnloadEvent event) {
        for (Entity e : event.getEntities()) {
            if (e instanceof Item) {
                tracked.remove(e.getUniqueId());
                pending.remove(e.getUniqueId());
            }
        }
    }

    // ---- capture path 1: the remove event ----

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(EntityRemoveEvent event) {
        if (!(event.getEntity() instanceof Item item)) return;
        UUID id = item.getUniqueId();
        switch (event.getCause()) {
            case PICKUP, UNLOAD -> {
                tracked.remove(id);
                pending.remove(id);
            }
            case OUT_OF_WORLD -> record(item, BurnCause.VOID, null);
            case DEATH -> {
                EntityDamageEvent last = item.getLastDamageCause();
                String cause = BurnCause.fromDamageCause(last == null ? null : last.getCause().name());
                if (cause != null) record(item, cause, null);
            }
            default -> { /* MERGE/DESPAWN/PLUGIN/... are not destructions this ledger claims */ }
        }
    }

    // ---- capture path 2: lethal-damage follow-up ----

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item item)) return;
        String cause = BurnCause.fromDamageCause(event.getCause().name());
        if (cause == null) return;
        UUID id = item.getUniqueId();
        if (guard.isClaimed(id, System.currentTimeMillis())) return;
        if (!pending.containsKey(id)) {
            Pending p = new Pending();
            ItemStack stack = item.getItemStack();
            if (stack == null || stack.getType().isAir()) return;
            p.raw = ItemCapture.of("destroyed", stack);
            Location loc = item.getLocation();
            p.world = loc.getWorld() == null ? "" : loc.getWorld().getKey().toString();
            p.x = loc.getX(); p.y = loc.getY(); p.z = loc.getZ();
            p.entityOwner = item.getOwner();
            try { p.ticksLived = item.getTicksLived(); } catch (Throwable t) { p.ticksLived = -1; }
            pending.put(id, p);
        }
        final String burnCause = cause;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Pending p = pending.remove(id);
            if (p == null) return;
            if (!item.isValid() || item.isDead()) {
                record(item, burnCause, p);
            }
            // else: the item survived the blow (rescued) — the
            // pending snapshot is dropped; a later blow re-stashes.
        });
    }

    // ---- capture path 3: the void sweep ----

    private void sweepVoid() {
        for (World world : Bukkit.getWorlds()) {
            double floor = world.getMinHeight();
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (item.getLocation().getY() < floor) {
                    record(item, BurnCause.VOID, null);
                }
            }
        }
    }

    // ---- despawn: valuable items only ----

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDespawn(ItemDespawnEvent event) {
        Item item = event.getEntity();
        ItemStack stack = item.getItemStack();
        if (stack == null || stack.getType().isAir()) return;
        // Vanilla /give leaves a "fake" display item that despawns on its
        // next tick (the player already got the real one). A real despawn
        // takes minutes, so anything younger than 5 s is that ghost.
        try { if (item.getTicksLived() < 100) return; } catch (Throwable ignored) { }
        DeathSnapshot.RawItem raw = ItemCapture.of("destroyed", stack);
        if (!Attribution.isValuable(toEntry(raw))) return;
        record(item, BurnCause.DESPAWN, null);
    }

    // ---- the write ----

    private void record(Item item, String cause, Pending pre) {
        try {
            UUID id = item.getUniqueId();
            long now = System.currentTimeMillis();
            if (!guard.claim(id, now)) return;

            DeathSnapshot.RawItem raw;
            String world;
            double x, y, z;
            UUID entityOwner;
            long ticksLived;
            if (pre != null) {
                raw = pre.raw; world = pre.world; x = pre.x; y = pre.y; z = pre.z;
                entityOwner = pre.entityOwner; ticksLived = pre.ticksLived;
            } else {
                ItemStack stack = item.getItemStack();
                if (stack == null || stack.getType().isAir()) return;
                raw = ItemCapture.of("destroyed", stack);
                Location loc = item.getLocation();
                world = loc.getWorld() == null ? "" : loc.getWorld().getKey().toString();
                x = loc.getX(); y = loc.getY(); z = loc.getZ();
                entityOwner = item.getOwner();
                try { ticksLived = item.getTicksLived(); } catch (Throwable t) { ticksLived = -1; }
            }

            Attribution.DropInfo dropInfo = tracked.remove(id);
            pending.remove(id);

            String entityOwnerUuid = entityOwner == null ? "" : entityOwner.toString();
            String entityOwnerName = entityOwnerUuid.isEmpty() ? "" : nameFor(entityOwner, dropInfo);
            Attribution.Owner owner = Attribution.resolve(entityOwnerUuid, entityOwnerName, dropInfo);
            long age = Attribution.ageSeconds(dropInfo, now, ticksLived >= 0 ? ticksLived / 20 : -1);

            DestructionRecord rec = new DestructionRecord();
            rec.ownerUuid = owner.uuid;
            rec.ownerName = owner.name;
            rec.epochMillis = now;
            rec.timeText = RecordName.timeText(now, store.zone());
            rec.cause = cause;
            rec.world = world;
            rec.x = x; rec.y = y; rec.z = z;
            rec.ageSeconds = age;
            rec.item = toEntry(raw);

            String text = BurnFormat.write(rec);
            plugin.writeAsync(() -> {
                try {
                    store.writeBurn(rec, text);
                } catch (Exception e) {
                    plugin.getLogger().warning("ExcelDeathRecord [Burn Ledger]: could not write record: " + e.getMessage());
                }
            });
            plugin.getLogger().info("ExcelDeathRecord [Burn Ledger]: " + BurnIndex.summaryLine(rec));
        } catch (Throwable t) {
            // Recording a loss must never break the world tick.
            plugin.getLogger().warning("ExcelDeathRecord [Burn Ledger]: capture failed: " + t);
        }
    }

    private String nameFor(UUID uuid, Attribution.DropInfo dropInfo) {
        if (dropInfo != null && uuid.toString().equals(dropInfo.ownerUuid) && !dropInfo.ownerName.isEmpty()) {
            return dropInfo.ownerName;
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() == null ? "" : op.getName();
    }

    static DeathRecord.ItemEntry toEntry(DeathSnapshot.RawItem raw) {
        DeathRecord.ItemEntry e = new DeathRecord.ItemEntry();
        e.slot = raw.slot;
        e.id = raw.id;
        e.count = raw.count;
        e.damage = raw.damage;
        e.enchants = raw.enchants;
        e.customName = raw.customName;
        e.bytesB64 = raw.bytes == null ? "" : Base64.getEncoder().encodeToString(raw.bytes);
        return e;
    }
}
