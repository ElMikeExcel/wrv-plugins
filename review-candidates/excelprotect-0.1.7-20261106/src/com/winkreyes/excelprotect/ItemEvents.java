package com.winkreyes.excelprotect;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExcelProtect — items family: drop + pickup for ALL items (log-everything amendment),
 * and the destruction paths (despawn / burn / void) feeding the burn ledger in the
 * deaths stream. Owner attribution (audit §1.1): the dropper is stamped into the item
 * entity's PDC at drop time, backed by a drop-memory map — the chain that keeps
 * armor-stand-class drops attributed. Excel — Wink Reyes Valley
 */
public final class ItemEvents implements Listener {
    private final Ledger ledger;
    private final Settings cfg;
    private final Recovery recovery;
    private final NamespacedKey ownerKey;
    private final Map<UUID, UUID> dropMemory = new ConcurrentHashMap<>();
    private volatile Chains chains; // 0.1.5: chain stamping (set by the main class)

    public ItemEvents(Plugin plugin, Ledger ledger, Settings cfg, Recovery recovery) {
        this.ledger = ledger; this.cfg = cfg; this.recovery = recovery;
        this.ownerKey = new NamespacedKey(plugin, "owner");
    }

    public void setChains(Chains c) { this.chains = c; }

    /** Stamp chain + link onto a record when a chain claims the event (SPEC §2). */
    private void stamp(Json.Obj o, String[] m) {
        if (m != null) o.put("chain", m[0]).put("link", m[1]);
    }

    // ---- provenance (Amendment 3.4): stamp fingerprint-class items at first sight ----
    static boolean fingerprintClass(org.bukkit.inventory.ItemStack s) {
        if (s == null || s.getType() == org.bukkit.Material.AIR) return false;
        org.bukkit.inventory.meta.ItemMeta m = s.getItemMeta();
        if (m != null && (m.hasDisplayName() || m.hasEnchants())) return true;
        String id = s.getType().getKey().toString();
        for (String suf : new String[]{"_helmet", "_chestplate", "_leggings", "_boots", "_sword",
                "_pickaxe", "_axe", "_shovel", "_hoe", "_bow", "_crossbow", "_shield"})
            if (id.endsWith(suf)) return true;
        return id.equals("minecraft:elytra") || id.equals("minecraft:totem_of_undying")
                || id.endsWith("shulker_box");
    }

    /** Stamp the stack with a Protect record ID if it is fingerprint-class and unstamped. */
    static org.bukkit.inventory.ItemStack stamp(org.bukkit.inventory.ItemStack s) {
        if (ItemFact.RID_KEY == null || !fingerprintClass(s)) return s;
        org.bukkit.inventory.meta.ItemMeta m = s.getItemMeta();
        if (m == null) return s;
        if (m.getPersistentDataContainer().has(ItemFact.RID_KEY, PersistentDataType.STRING)) return s;
        String rid = "ep" + Long.toString(System.currentTimeMillis(), 36) + "-"
                + UUID.randomUUID().toString().substring(0, 6);
        m.getPersistentDataContainer().set(ItemFact.RID_KEY, PersistentDataType.STRING, rid);
        org.bukkit.inventory.ItemStack out = s.clone();
        out.setItemMeta(m);
        return out;
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraftStamp(org.bukkit.event.inventory.CraftItemEvent e) {
        org.bukkit.inventory.ItemStack stamped = stamp(e.getRecipe().getResult());
        if (stamped != e.getRecipe().getResult()) e.getInventory().setResult(stamped);
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(org.bukkit.event.inventory.CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemFact f = ItemFact.of(e.getRecipe().getResult(), false);
        if (f == null || f.rid() == null) return;
        ledger.emit("items", p.getLocation(), new Json.Obj().put("type", "item_craft")
                .put("player", p.getName()).put("player_uuid", p.getUniqueId().toString())
                .put("item", f.toJson()));
    }

    public UUID ownerOf(Item item) {
        String stamped = item.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (stamped != null) { try { return UUID.fromString(stamped); } catch (Exception ignored) { } }
        UUID mem = dropMemory.get(item.getUniqueId());
        if (mem != null) return mem;
        return item.getThrower();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        Item item = e.getItemDrop();
        Player p = e.getPlayer();
        org.bukkit.inventory.ItemStack stamped = stamp(item.getItemStack());
        if (stamped != item.getItemStack()) item.setItemStack(stamped);
        item.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, p.getUniqueId().toString());
        dropMemory.put(item.getUniqueId(), p.getUniqueId());
        if (!cfg.itemDrops()) return;
        ItemFact f = ItemFact.of(item.getItemStack(), false);
        if (f == null) return;
        Json.Obj o = new Json.Obj().put("type", "item_drop")
                .put("player", p.getName()).put("player_uuid", p.getUniqueId().toString())
                .put("item", f.toJson());
        Chains ch = chains;
        if (ch != null) stamp(o, ch.match(p.getUniqueId(), Chains.of(f), "drop", System.currentTimeMillis()));
        ledger.emit("items", item.getLocation(), o);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        Item item = e.getItem();
        UUID owner = ownerOf(item);
        if (e.getEntity() instanceof Player p) dropMemory.put(item.getUniqueId(), p.getUniqueId());
        if (!cfg.itemPickups()) return;
        ItemFact f = ItemFact.of(item.getItemStack(), false);
        if (f == null) return;
        Json.Obj o = new Json.Obj().put("type", "item_pickup").put("item", f.toJson());
        if (e.getEntity() instanceof Player p) o.put("player", p.getName()).put("player_uuid", p.getUniqueId().toString());
        else o.put("entity", e.getEntityType().getKey().toString());
        if (owner != null) o.put("owner_uuid", owner.toString());
        o.put("remaining", e.getRemaining());
        // 0.1.5: a PLAYER pickup resolves the chain (picker's chains
        // first, then the item owner's — a recovered stolen drop still
        // closes against the victim's chain). Mob pickups do not.
        Chains ch = chains;
        if (ch != null && e.getEntity() instanceof Player p)
            stamp(o, ch.matchPickup(p.getUniqueId(), owner, Chains.of(f), System.currentTimeMillis()));
        ledger.emit("items", item.getLocation(), o);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDespawn(ItemDespawnEvent e) {
        if (!cfg.itemDespawns()) return;
        Item item = e.getEntity();
        ItemFact f = ItemFact.of(item.getItemStack(), false);
        if (f != null) {
            Json.Obj o = new Json.Obj().put("type", "item_despawn").put("item", f.toJson())
                    .put("reason", "despawn");
            UUID owner = ownerOf(item);
            if (owner != null) o.put("owner_uuid", owner.toString());
            Chains ch = chains;
            if (ch != null && owner != null)
                stamp(o, ch.match(owner, Chains.of(f), "despawn", System.currentTimeMillis()));
            ledger.emit("items", item.getLocation(), o);
        }
        destroy(item, "DESPAWN");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Item item)) return;
        if (item.getHealth() - e.getFinalDamage() > 0) return; // not lethal
        String cause = switch (e.getCause()) {
            case LAVA -> "LAVA";
            case FIRE, FIRE_TICK -> "FIRE";
            case VOID -> "VOID";
            case ENTITY_EXPLOSION, BLOCK_EXPLOSION -> "EXPLOSION";
            case CONTACT -> "CACTUS";
            default -> null;
        };
        if (cause == null) return;
        ItemFact f = ItemFact.of(item.getItemStack(), false);
        if (f != null) {
            Json.Obj o = new Json.Obj().put("type", "item_destroy").put("item", f.toJson())
                    .put("reason", cause.toLowerCase());
            UUID owner = ownerOf(item);
            if (owner != null) o.put("owner_uuid", owner.toString());
            Chains ch = chains;
            if (ch != null && owner != null)
                stamp(o, ch.match(owner, Chains.of(f), "destroy", System.currentTimeMillis()));
            ledger.emit("items", item.getLocation(), o);
        }
        destroy(item, cause);
    }

    /** EntityRemove OUT_OF_WORLD — the void path (event-driven; no main-thread sweep, audit R1). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRemove(com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent e) {
        if (!(e.getEntity() instanceof Item item)) return;
        if (item.getLocation().getY() > item.getWorld().getMinHeight() - 32) return; // not a void fall
        destroy(item, "VOID");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUnload(EntitiesUnloadEvent e) {
        for (org.bukkit.entity.Entity en : e.getEntities())
            if (en instanceof Item item) dropMemory.remove(item.getUniqueId());
    }

    private void destroy(Item item, String cause) {
        UUID ownerUuid = ownerOf(item);
        Player owner = ownerUuid == null ? null : org.bukkit.Bukkit.getPlayer(ownerUuid);
        if (owner != null) {
            recovery.recordDestroyed(owner, cause, item.getLocation(), item.getItemStack(), item.getTicksLived());
        } else if (ownerUuid != null) {
            recovery.recordDestroyedOffline(ownerUuid,
                    org.bukkit.Bukkit.getOfflinePlayer(ownerUuid).getName(),
                    cause, item.getLocation(), item.getItemStack(), item.getTicksLived());
        } else {
            recovery.recordDestroyed(null, cause, item.getLocation(), item.getItemStack(), item.getTicksLived());
        }
        dropMemory.remove(item.getUniqueId());
    }
}
