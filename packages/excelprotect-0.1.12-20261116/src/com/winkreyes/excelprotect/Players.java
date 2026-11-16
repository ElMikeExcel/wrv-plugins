package com.winkreyes.excelprotect;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * ExcelProtect — players family + the audit xp/enchant markers + player DEATH
 * SNAPSHOTS (SPEC §3.5/§3.6): join/leave with location, teleports, commands, chat
 * (OFF by default — privacy), progression level changes, and at death a full
 * byte-exact snapshot (inventory/armor/offhand/ender chest/XP) into the deaths
 * stream. Excel — Wink Reyes Valley
 */
public final class Players implements Listener {
    private final Ledger ledger;
    private final Settings cfg;
    private final CombatCore combat;

    public Players(Ledger ledger, Settings cfg, CombatCore combat) {
        this.ledger = ledger; this.cfg = cfg; this.combat = combat;
    }

    private Json.Obj who(Player p) {
        return new Json.Obj().put("player", p.getName()).put("player_uuid", p.getUniqueId().toString())
                .put("platform", p.getClientBrandName() != null && p.getName().startsWith(".") ? "bedrock" : "java");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        if (cfg.joins())
            ledger.emit("players", e.getPlayer().getLocation(),
                    who(e.getPlayer()).put("type", "player_join"));
        inventorySnapshot(e.getPlayer(), "join");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        if (cfg.leaves())
            ledger.emit("players", e.getPlayer().getLocation(),
                    who(e.getPlayer()).put("type", "player_leave"));
        inventorySnapshot(e.getPlayer(), "leave");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(org.bukkit.event.player.PlayerChangedWorldEvent e) {
        inventorySnapshot(e.getPlayer(), "world_change");
    }

    /** Inventory history (Amendment 3.2): fingerprint list, no payloads — diff-able. */
    public void inventorySnapshot(Player p, String reason) {
        if (!cfg.inventorySnapshots()) return;
        Json.Arr items = new Json.Arr();
        for (ItemStack s : p.getInventory().getContents()) {
            ItemFact f = ItemFact.of(s, false);
            if (f != null) items.add(f.toJson());
        }
        ledger.emit("snapshots", p.getLocation(), who(p).put("type", "inventory_snapshot")
                .put("reason", reason).put("items", items));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (!cfg.teleports()) return;
        Json.Obj o = who(e.getPlayer()).put("type", "player_teleport")
                .put("cause", e.getCause().name().toLowerCase())
                .put("to_world", e.getTo().getWorld().getName())
                .put("to_x", Ledger.round(e.getTo().getX()))
                .put("to_y", Ledger.round(e.getTo().getY()))
                .put("to_z", Ledger.round(e.getTo().getZ()));
        ledger.emit("players", e.getFrom(), o);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!cfg.commands()) return;
        ledger.emit("players", e.getPlayer().getLocation(),
                who(e.getPlayer()).put("type", "player_command").put("command", e.getMessage()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncChatEvent e) {
        if (!cfg.chat()) return;
        ledger.emit("players", e.getPlayer().getLocation(),
                who(e.getPlayer()).put("type", "player_chat")
                        .put("message", PlainTextComponentSerializer.plainText().serialize(e.message())));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevel(PlayerLevelChangeEvent e) {
        if (!cfg.auditXp()) return;
        ledger.emit("audit", e.getPlayer().getLocation(),
                who(e.getPlayer()).put("type", "xp_level")
                        .put("old_level", e.getOldLevel()).put("new_level", e.getNewLevel()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent e) {
        if (!cfg.auditEnchants()) return;
        Json.Obj ench = new Json.Obj();
        e.getEnchantsToAdd().forEach((en, lvl) -> ench.put(en.getKey().toString(), lvl));
        ItemFact f = ItemFact.of(e.getItem(), false);
        Json.Obj o = who(e.getEnchanter()).put("type", "enchant")
                .put("cost", e.getExpLevelCost()).put("enchants", ench);
        if (f != null) o.put("item", f.toJson());
        ledger.emit("audit", e.getEnchanter().getLocation(), o);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        DamageLedger.Book book = combat.kills.take(p.getUniqueId());
        Json.Obj killer = new Json.Obj();
        if (book != null && book.lastKind != null) {
            killer.put("kind", book.lastKind);
            if (book.lastUuid != null) killer.put("uuid", book.lastUuid);
            if (book.lastLabel != null) killer.put("name", book.lastLabel);
            if (book.lastCause != null) killer.put("final_cause", book.lastCause);
        } else killer.put("kind", "environment");
        Json.Obj ev = who(p).put("type", "player_death")
                .put("death_message", e.getDeathMessage())
                .put("killer", killer)
                .put("keep_inventory", e.getKeepInventory());
        if (book != null) ev.put("fight_s", CombatCore.round2(book.fightSeconds())).put("damage", book.damageJson());
        ledger.emit("players", p.getLocation(), ev);

        if (!cfg.deathSnapshots() || !cfg.playerDeaths()) return;
        // Death snapshot (deaths stream): everything, byte-exact.
        Json.Obj snap = who(p).put("type", "death_snapshot")
                .put("death_id", System.currentTimeMillis() + ":" + p.getUniqueId())
                .put("death_message", e.getDeathMessage())
                .put("xp_level", p.getLevel())
                .put("total_experience", p.getTotalExperience())
                .put("dropped_exp", e.getDroppedExp())
                .put("gamemode", p.getGameMode().name().toLowerCase())
                .put("killer", killer);
        snap.put("inventory", stackArray(snapshotContents(p)));
        snap.put("ender_chest", stackArray(p.getEnderChest().getContents()));
        Json.Arr drops = new Json.Arr();
        for (ItemStack s : e.getDrops()) {
            ItemFact f = ItemFact.of(s, true);
            if (f != null) drops.add(f.toJson());
        }
        snap.put("drops", drops);
        ledger.emit("deaths", p.getLocation(), snap);
    }

    /** Full inventory incl. armor + offhand, slot order preserved, nulls as empty facts skipped. */
    private ItemStack[] snapshotContents(Player p) {
        List<ItemStack> all = new ArrayList<>();
        for (ItemStack s : p.getInventory().getContents()) all.add(s);
        return all.toArray(new ItemStack[0]);
    }

    private Json.Arr stackArray(ItemStack[] contents) {
        Json.Arr a = new Json.Arr();
        for (ItemStack s : contents) {
            ItemFact f = ItemFact.of(s, true);
            a.add(f == null ? new Json.Obj().put("empty", true) : f.toJson());
        }
        return a;
    }
}
