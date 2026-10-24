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
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import com.winkreyes.exceldeathrecord.core.TypeGlob;
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
 *
 * 0.3.0: the owner is also stamped into the dropped item ENTITY's
 * persistent data (never the item stack itself) when a player drops
 * it, when it spawns with a thrower, and when it spills from a
 * player's death. The stamp is saved with the entity, so the owner is
 * still known after a chunk unload or a restart (the in-memory drop
 * tracker forgets both). A despawning item also counts as valuable
 * when its type is on the despawn-log-types list, and a despawn that
 * another plugin cancelled is no longer recorded (the item is still
 * in the world, so recording it would let it be brought back twice).
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
        String stampUuid = "", stampName = "", throwerUuid = "";
    }

    /** 0.3.0: the death that is spilling its drops this tick. */
    private static final class DeathSpill {
        int tick;
        String uuid, name;
        Location at;
        final List<ItemStack> left = new ArrayList<>();
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
    /** 0.3.0: item entities a hopper or a mob/player picked up recently (never recorded as lost if gone). */
    private final Map<UUID, Long> taken = new LinkedHashMap<>() {
        @Override protected boolean removeEldestEntry(Map.Entry<UUID, Long> eldest) { return size() > 4096; }
    };
    private final NamespacedKey ownerKey;
    private final NamespacedKey ownerNameKey;
    private final Supplier<TypeGlob> types;
    private final Consumer<DestructionRecord> onRecorded;
    private DeathSpill spill;

    BurnWatch(ExcelDeathRecord plugin, RecordStore store, Supplier<TypeGlob> types, Consumer<DestructionRecord> onRecorded) {
        this.plugin = plugin;
        this.store = store;
        this.ownerKey = new NamespacedKey(plugin, "owner");
        this.ownerNameKey = new NamespacedKey(plugin, "owner-name");
        this.types = types;
        this.onRecorded = onRecorded;
    }

    // ---- 0.3.0: persistent owner stamp on the item entity ----

    private void stamp(Item item, UUID uuid, String name) {
        try {
            PersistentDataContainer pdc = item.getPersistentDataContainer();
            if (pdc.has(ownerKey, PersistentDataType.STRING)) return; // first owner wins
            pdc.set(ownerKey, PersistentDataType.STRING, uuid.toString());
            pdc.set(ownerNameKey, PersistentDataType.STRING, name == null ? "" : name);
        } catch (Throwable ignored) { /* never break a drop */ }
    }

    private String[] readStamp(Item item) {
        try {
            PersistentDataContainer pdc = item.getPersistentDataContainer();
            String u = pdc.get(ownerKey, PersistentDataType.STRING);
            String n = pdc.get(ownerNameKey, PersistentDataType.STRING);
            return new String[]{u == null ? "" : u, n == null ? "" : n};
        } catch (Throwable t) {
            return new String[]{"", ""};
        }
    }

    private static String throwerOf(Item item) {
        try { UUID t = item.getThrower(); return t == null ? "" : t.toString(); } catch (Throwable e) { return ""; }
    }

    /** Called from the death snapshot (MONITOR): the drops spawn later this tick. */
    void noteDeath(Player player, List<ItemStack> drops) {
        try {
            DeathSpill d = new DeathSpill();
            d.tick = Bukkit.getCurrentTick();
            d.uuid = player.getUniqueId().toString();
            d.name = player.getName();
            d.at = player.getLocation();
            for (ItemStack s : drops) if (s != null && !s.getType().isAir()) d.left.add(s.clone());
            spill = d.left.isEmpty() ? null : d;
        } catch (Throwable ignored) { spill = null; }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        try {
            UUID thrower = item.getThrower();
            if (thrower != null) {
                Player online = Bukkit.getPlayer(thrower);
                String name = online != null ? online.getName() : Bukkit.getOfflinePlayer(thrower).getName();
                stamp(item, thrower, name);
                return;
            }
            DeathSpill d = spill;
            if (d == null) return;
            if (d.tick != Bukkit.getCurrentTick()) { spill = null; return; }
            Location l = item.getLocation();
            if (d.at.getWorld() == null || l.getWorld() == null || !d.at.getWorld().equals(l.getWorld())
                    || d.at.distanceSquared(l) > 16.0) return;
            ItemStack s = item.getItemStack();
            for (int i = 0; i < d.left.size(); i++) {
                ItemStack want = d.left.get(i);
                if (want.isSimilar(s) && want.getAmount() == s.getAmount()) {
                    d.left.remove(i);
                    stamp(item, UUID.fromString(d.uuid), d.name);
                    if (d.left.isEmpty()) spill = null;
                    return;
                }
            }
        } catch (Throwable ignored) { /* never break a spawn */ }
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
        stamp(item, event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPickup(EntityPickupItemEvent event) {
        tracked.remove(event.getItem().getUniqueId());
        pending.remove(event.getItem().getUniqueId());
    }

    /**
     * 0.3.0: a hopper (or hopper minecart) took the item. It is not lost,
     * so forget any half-recorded blow: a burning item that a hopper pulls
     * out in the same tick it was hurt would otherwise look destroyed on
     * the next tick and be recorded (and so recoverable) while it actually
     * sits in the hopper. A partial pickup leaves the entity alive, and a
     * later blow is stashed again.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHopperPickup(org.bukkit.event.inventory.InventoryPickupItemEvent event) {
        UUID id = event.getItem().getUniqueId();
        pending.remove(id);
        tracked.remove(id);
        markTaken(event.getItem());
    }

    /** Remember a pickup for one tick; a partial pickup (entity still alive) is forgotten. */
    private void markTaken(Item item) {
        UUID id = item.getUniqueId();
        taken.put(id, System.currentTimeMillis());
        Bukkit.getScheduler().runTask(plugin, () -> { if (item.isValid()) taken.remove(id); });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickupClaim(EntityPickupItemEvent event) {
        markTaken(event.getItem());
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
            String[] st = readStamp(item);
            p.stampUuid = st[0]; p.stampName = st[1]; p.throwerUuid = throwerOf(item);
            pending.put(id, p);
        }
        final String burnCause = cause;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Pending p = pending.remove(id);
            if (p == null) return;
            if (taken.containsKey(id) && !item.isValid()) return; // picked up, not destroyed
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDespawn(ItemDespawnEvent event) {
        Item item = event.getEntity();
        ItemStack stack = item.getItemStack();
        if (stack == null || stack.getType().isAir()) return;
        // Vanilla /give leaves a "fake" display item that despawns on its
        // next tick (the player already got the real one). A real despawn
        // takes minutes, so anything younger than 5 s is that ghost.
        try { if (item.getTicksLived() < 100) return; } catch (Throwable ignored) { }
        DeathSnapshot.RawItem raw = ItemCapture.of("destroyed", stack);
        DeathRecord.ItemEntry entry = toEntry(raw);
        TypeGlob list = types == null ? null : types.get();
        if (!Attribution.isValuable(entry) && (list == null || !list.matches(entry.id))) return;
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
            String stampUuid, stampName, throwerUuid;
            if (pre != null) {
                raw = pre.raw; world = pre.world; x = pre.x; y = pre.y; z = pre.z;
                entityOwner = pre.entityOwner; ticksLived = pre.ticksLived;
                stampUuid = pre.stampUuid; stampName = pre.stampName; throwerUuid = pre.throwerUuid;
            } else {
                ItemStack stack = item.getItemStack();
                if (stack == null || stack.getType().isAir()) return;
                raw = ItemCapture.of("destroyed", stack);
                Location loc = item.getLocation();
                world = loc.getWorld() == null ? "" : loc.getWorld().getKey().toString();
                x = loc.getX(); y = loc.getY(); z = loc.getZ();
                entityOwner = item.getOwner();
                try { ticksLived = item.getTicksLived(); } catch (Throwable t) { ticksLived = -1; }
                String[] st = readStamp(item);
                stampUuid = st[0]; stampName = st[1]; throwerUuid = throwerOf(item);
            }

            Attribution.DropInfo dropInfo = tracked.remove(id);
            pending.remove(id);

            String entityOwnerUuid = entityOwner == null ? "" : entityOwner.toString();
            String entityOwnerName = entityOwnerUuid.isEmpty() ? "" : nameFor(entityOwner, dropInfo);
            if (stampUuid != null && !stampUuid.isEmpty() && (stampName == null || stampName.isEmpty())) {
                try { stampName = nameFor(UUID.fromString(stampUuid), dropInfo); } catch (IllegalArgumentException e) { stampUuid = ""; }
            }
            String throwerName = "";
            if (throwerUuid != null && !throwerUuid.isEmpty()) {
                try { throwerName = nameFor(UUID.fromString(throwerUuid), dropInfo); } catch (IllegalArgumentException e) { throwerUuid = ""; }
            }
            Attribution.Owner owner = Attribution.resolve(entityOwnerUuid, entityOwnerName,
                    stampUuid, stampName, throwerUuid, throwerName, dropInfo);
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
            if (onRecorded != null) {
                try { onRecorded.accept(rec); } catch (Throwable ignored) { }
            }
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
