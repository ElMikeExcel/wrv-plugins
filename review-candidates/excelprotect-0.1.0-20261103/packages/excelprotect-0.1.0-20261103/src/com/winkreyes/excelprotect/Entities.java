package com.winkreyes.excelprotect;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExcelProtect — entities family: deaths with location, killer attribution and the
 * fight-window damage ledger (CombatCore), FULL EQUIPMENT SNAPSHOTS with payloads
 * for any item-wearing entity (armor stands headline), entity-change events
 * (armor-stand equipment, item-frame contents), tames, and origin-stamped spawns
 * of named/tamed/stand-class entities. Excel — Wink Reyes Valley
 */
public final class Entities implements Listener {
    private final Ledger ledger;
    private final Settings cfg;
    private final CombatCore combat;
    private final Map<UUID, String> origins = new ConcurrentHashMap<>();

    public Entities(Ledger ledger, Settings cfg, CombatCore combat) {
        this.ledger = ledger; this.cfg = cfg; this.combat = combat;
    }

    public static Json.Obj equipmentJson(EntityEquipment eq, boolean payloads) {
        if (eq == null) return null;
        Json.Obj o = new Json.Obj();
        put(o, "helmet", eq.getHelmet(), payloads);
        put(o, "chestplate", eq.getChestplate(), payloads);
        put(o, "leggings", eq.getLeggings(), payloads);
        put(o, "boots", eq.getBoots(), payloads);
        put(o, "main_hand", eq.getItemInMainHand(), payloads);
        put(o, "off_hand", eq.getItemInOffHand(), payloads);
        return o.hasAny() ? o : null;
    }

    private static void put(Json.Obj o, String key, ItemStack s, boolean payloads) {
        ItemFact f = ItemFact.of(s, payloads);
        if (f != null) o.put(key, f.toJson());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        LivingEntity en = e.getEntity();
        origins.put(en.getUniqueId(), e.getSpawnReason().name().toLowerCase());
        if (origins.size() > 20000) origins.clear();
        if (!cfg.entitySpawns()) return;
        boolean interesting = en instanceof ArmorStand || en.getCustomName() != null
                || (en instanceof Tameable t && t.isTamed());
        if (!interesting) return;
        Json.Obj o = new Json.Obj().put("type", "entity_spawn")
                .put("entity", en.getType().getKey().toString())
                .put("entity_uuid", en.getUniqueId().toString())
                .put("origin", e.getSpawnReason().name().toLowerCase());
        if (en.getCustomName() != null) o.put("name", en.getCustomName());
        ledger.emit("entities", en.getLocation(), o);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity v = e.getEntity();
        String origin = origins.remove(v.getUniqueId());
        if (!cfg.entityDeaths() || v instanceof Player) return;
        DamageLedger.Book book = combat.kills.take(v.getUniqueId());
        Json.Obj o = new Json.Obj().put("type", "entity_death")
                .put("entity", v.getType().getKey().toString())
                .put("entity_uuid", v.getUniqueId().toString());
        if (v.getCustomName() != null) o.put("name", v.getCustomName());
        if (origin != null) o.put("origin", origin);
        if (v instanceof Tameable t && t.isTamed() && t.getOwner() != null)
            o.put("owner", t.getOwner().getName()).put("owner_uuid", t.getOwner().getUniqueId().toString());
        Json.Obj killer = new Json.Obj();
        if (book != null && book.lastKind != null) {
            killer.put("kind", book.lastKind);
            if (book.lastUuid != null) killer.put("uuid", book.lastUuid);
            if (book.lastLabel != null) killer.put("name", book.lastLabel);
            if (book.lastCause != null) killer.put("final_cause", book.lastCause);
        } else if (v.getKiller() != null) {
            killer.put("kind", "player").put("uuid", v.getKiller().getUniqueId().toString())
                  .put("name", v.getKiller().getName());
        } else {
            killer.put("kind", "environment");
        }
        o.put("killer", killer);
        if (book != null) o.put("fight_s", CombatCore.round2(book.fightSeconds())).put("damage", book.damageJson());
        Json.Obj eq = equipmentJson(v.getEquipment(), true);
        if (v instanceof ArmorStand || eq != null) o.put("equipment", eq == null ? new Json.Obj() : eq);
        ledger.emit("entities", v.getLocation(), o);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStandManipulate(PlayerArmorStandManipulateEvent e) {
        if (!cfg.entityChanges()) return;
        ItemFact placed = ItemFact.of(e.getArmorStandItem(), false);
        ItemFact taken = ItemFact.of(e.getPlayerItem(), false);
        Json.Obj o = new Json.Obj().put("type", "entity_change")
                .put("change", "armor_stand_equipment")
                .put("slot", e.getSlot().name().toLowerCase())
                .put("player", e.getPlayer().getName())
                .put("player_uuid", e.getPlayer().getUniqueId().toString());
        if (placed != null) o.put("removed_from_stand", placed.toJson());
        if (taken != null) o.put("placed_on_stand", taken.toJson());
        ledger.emit("entities", e.getRightClicked().getLocation(), o);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFrameInteract(PlayerInteractEntityEvent e) {
        if (!cfg.entityChanges() || !(e.getRightClicked() instanceof ItemFrame frame)) return;
        ItemFact f = ItemFact.of(frame.getItem(), false);
        Json.Obj o = new Json.Obj().put("type", "entity_change")
                .put("change", "item_frame_content")
                .put("player", e.getPlayer().getName())
                .put("player_uuid", e.getPlayer().getUniqueId().toString())
                .put("rotation", frame.getRotation().name().toLowerCase());
        if (f != null) o.put("item", f.toJson());
        ledger.emit("entities", frame.getLocation(), o);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent e) {
        if (!cfg.entityTames()) return;
        LivingEntity en = e.getEntity();
        Json.Obj o = new Json.Obj().put("type", "entity_tame")
                .put("entity", en.getType().getKey().toString())
                .put("entity_uuid", en.getUniqueId().toString())
                .put("owner", e.getOwner() == null ? null : e.getOwner().getName());
        ledger.emit("entities", en.getLocation(), o);
    }
}
