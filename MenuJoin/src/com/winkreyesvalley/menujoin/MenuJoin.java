package com.winkreyesvalley.menujoin;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** MenuJoin 1.2.0 - rebuilt from decompiled 1.1.0 (Argus) plus respawn/death/drop/compass features. */
public class MenuJoin extends JavaPlugin implements Listener {
    private NamespacedKey disabledKey;
    private NamespacedKey compassKey; // menujoin:menu_compass - same as 1.1.0, existing compasses keep working
    private MenuJoinExpansion expansion;

    @Override
    public void onEnable() {
        disabledKey = new NamespacedKey(this, "autojoin_disabled");
        compassKey = new NamespacedKey(this, "menu_compass");
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("menujoin").setExecutor(this::toggleCommand);
        getCommand("compass").setExecutor(this::compassCommand);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            expansion = new MenuJoinExpansion(this);
            expansion.register();
            getLogger().info("Hooked into PlaceholderAPI (%menujoin_status%).");
        }
        getLogger().info("MenuJoin enabled.");
    }

    @Override
    public void onDisable() {
        if (expansion != null) {
            try { expansion.unregister(); } catch (Exception ignored) { }
        }
    }

    public boolean isDisabled(Player p) {
        return p.getPersistentDataContainer().getOrDefault(disabledKey, PersistentDataType.BOOLEAN, false);
    }

    public void setDisabled(Player p, boolean disabled) {
        if (disabled) p.getPersistentDataContainer().set(disabledKey, PersistentDataType.BOOLEAN, true);
        else p.getPersistentDataContainer().remove(disabledKey);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        boolean open = !isDisabled(p);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!p.isOnline()) return;
            giveMenuCompass(p);
            if (open) p.performCommand("menu");
        }, 60L);
    }

    @EventHandler
    public void onCompassUse(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        if (!isMenuCompass(e.getItem())) return;
        e.setCancelled(true);
        e.getPlayer().performCommand("menu");
    }

    // 1.2.0: give a compass back after respawn (1 tick later, once the player is in the world)
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (p.isOnline()) giveMenuCompass(p);
        }, 1L);
    }

    // 1.2.0: menu compass never drops on death (other drops untouched)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        e.getDrops().removeIf(this::isMenuCompass);
        e.getItemsToKeep().removeIf(this::isMenuCompass); // keepInventory: avoid a doubled compass
    }

    // 1.2.0: menu compass can't be thrown out of the inventory
    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (isMenuCompass(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§7Your §6Server Menu §7compass stays with you. Tap it to open the menu!");
        }
    }

    public boolean hasMenuCompass(Player p) {
        for (ItemStack it : p.getInventory().getContents()) if (isMenuCompass(it)) return true;
        return isMenuCompass(p.getItemOnCursor());
    }

    /** Returns true if a new compass was given. */
    public boolean giveMenuCompass(Player p) {
        if (hasMenuCompass(p)) return false;
        p.getInventory().addItem(createMenuCompass()).values()
                .forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
        return true;
    }

    private ItemStack createMenuCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Server Menu");
        meta.setLore(List.of("§7Tap to open the server menu"));
        meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isMenuCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(compassKey, PersistentDataType.BOOLEAN, false);
    }

    private boolean toggleCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Only players can use this command."); return true; }
        boolean disable = !isDisabled(p);
        setDisabled(p, disable);
        if (disable) p.sendMessage("§7The server menu will §cno longer §7open automatically when you join. Type §6/menu §7anytime to open it.");
        else p.sendMessage("§7The server menu will §aopen §7automatically when you join.");
        return true;
    }

    private boolean compassCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Only players can use this command."); return true; }
        if (giveMenuCompass(p)) p.sendMessage("§aHere's a fresh §6Server Menu §acompass! Tap it to open the menu.");
        else p.sendMessage("§7You already have your §6Server Menu §7compass. Check your inventory!");
        return true;
    }
}
