package com.winkreyes.excelprotect;

import org.bukkit.Bukkit;
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
        if (book != null) o.put("fight_s", CombatCore.round2(book.fightSeconds()));
        // 0.1.7 — kill-field parity with ExcelTelemetry (13b field diff; gate G3
        // compares the two mechanically). Elite attribution comes from the same
        // EliteMobs EntityTracker probe the incident streams use; the origin
        // taxonomy from RewardGuard's PDC marks (RgOrigin, a field-for-field
        // twin of Telemetry's reader) with Protect's stashed spawn reason as
        // fallback — Protect's existing lowercase "origin" field is unchanged;
        // weapon is the killer player's main hand AT DEATH TIME, the same
        // moment Telemetry snapshots it. Every read is guarded: a parity field
        // must never cost the death line.
        // Elite level: the damage-time probe cached on the book wins (see
        // DamageLedger.Book.eliteLevel); the at-death probe is the fallback
        // for deaths with no recorded damage.
        int eliteLevel = book != null && book.eliteLevel != EliteProbe.UNKNOWN
                ? book.eliteLevel : EliteProbe.levelOf(v);
        killParity(eliteLevel, book).forEach(o::putRaw);
        RgOrigin.Origin org = RgOrigin.classify(v, origin);
        o.put("origin_class", org.originClass());
        if (org.rgDenied()) o.put("rg_denied", true);
        Player killerPlayer = killerPlayer(v, book);
        if (killerPlayer != null) {
            try {
                ItemFact w = ItemFact.of(killerPlayer.getInventory().getItemInMainHand(), false);
                if (w != null) o.put("weapon", w.toJson());
            } catch (Throwable ignored) { }
        }
        Json.Obj eq = equipmentJson(v.getEquipment(), true);
        if (v instanceof ArmorStand || eq != null) o.put("equipment", eq == null ? new Json.Obj() : eq);
        ledger.emit("entities", v.getLocation(), o);
    }

    /** The killer as an online Player, when resolvable: the damage book's last
     *  attacker first (Protect's attribution), then Bukkit's own killer. */
    private static Player killerPlayer(LivingEntity v, DamageLedger.Book book) {
        try {
            if (book != null && "player".equals(book.lastKind) && book.lastUuid != null) {
                Player p = Bukkit.getPlayer(UUID.fromString(book.lastUuid));
                if (p != null) return p;
            }
            return v.getKiller();
        } catch (Throwable t) {
            return null;
        }
    }

    /** Telemetry's share rounding: round1(share * 1000) / 1000 — 4 decimal places. */
    static double roundShare(double share) { return Math.round(share * 10000.0) / 10000.0; }

    /**
     * 0.1.7 — the pure half of kill parity, unit-tested: elite serialization
     * identical to the damage/dealt incident streams (elite + elite_level /
     * elite:false / elite_probe=unavailable), the damage object extended with
     * fall_share + explosion_share (Telemetry's definitions, from the book's
     * by-cause totals), fall_softened (= raw fall share >= 0.5, always present),
     * and — for elite victims only — Telemetry's explosion integrity flag at
     * its 0.60 threshold. Decisions use the RAW shares; only the stored values
     * are rounded, exactly as Telemetry stores them.
     */
    static Json.Obj killParity(int eliteLevel, DamageLedger.Book book) {
        Json.Obj o = new Json.Obj();
        if (eliteLevel >= 0) o.put("elite", true).put("elite_level", eliteLevel);
        else if (eliteLevel == EliteProbe.NOT_ELITE) o.put("elite", false);
        else o.put("elite_probe", "unavailable");
        double fallShare = 0, explShare = 0;
        if (book != null) {
            fallShare = book.fallShare();
            explShare = book.explosionShare();
            Json.Obj dmg = book.damageJson();
            dmg.put("fall_share", roundShare(fallShare)).put("explosion_share", roundShare(explShare));
            o.put("damage", dmg);
            if (eliteLevel >= 0 && explShare >= 0.60) {
                Json.Arr flags = new Json.Arr();
                flags.add(new Json.Obj().put("type", "explosion_share_high")
                        .put("explosion_share", roundShare(explShare)).put("threshold", 0.60));
                o.put("flags", flags);
            }
        }
        o.put("fall_softened", fallShare >= 0.5);
        return o;
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
