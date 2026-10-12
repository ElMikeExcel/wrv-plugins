package com.winkreyesvalley.menujoin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Opens the DeluxeMenus "server_menu" for each player shortly after they join,
 * unless they have opted out with /menujoin. The opt-out flag is stored in the
 * player's PersistentDataContainer, so it survives restarts with no files.
 */
public class MenuJoin extends JavaPlugin implements Listener {

    private NamespacedKey disabledKey;
    private NamespacedKey compassKey;
    private MenuJoinExpansion expansion;

    @Override
    public void onEnable() {
        disabledKey = new NamespacedKey(this, "autojoin_disabled");
        compassKey = new NamespacedKey(this, "menu_compass");
        Bukkit.getPluginManager().registerEvents(this, this);

        this.getCommand("menujoin").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can use this command.");
                return true;
            }
            boolean nowDisabled = !isDisabled(player);
            setDisabled(player, nowDisabled);
            if (nowDisabled) {
                player.sendMessage("§7The server menu will §cno longer §7open automatically when you join. Type §6/menu §7anytime to open it.");
            } else {
                player.sendMessage("§7The server menu will §aopen §7automatically when you join.");
            }
            return true;
        });

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
            try {
                expansion.unregister();
            } catch (Exception ignored) {
            }
        }
    }

    public boolean isDisabled(Player player) {
        return player.getPersistentDataContainer()
                .getOrDefault(disabledKey, PersistentDataType.BOOLEAN, false);
    }

    public void setDisabled(Player player, boolean disabled) {
        if (disabled) {
            player.getPersistentDataContainer().set(disabledKey, PersistentDataType.BOOLEAN, true);
        } else {
            player.getPersistentDataContainer().remove(disabledKey);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean autoOpen = !isDisabled(player);
        // Small delay so the client finishes loading before the inventory opens.
        // Open via the player's own /menu command: identical to the player typing
        // it themselves, so no console player-name resolution is involved.
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!player.isOnline()) {
                return;
            }
            // Everyone gets the tap-to-open compass; only auto-open the menu
            // for players who haven't opted out.
            giveMenuCompass(player);
            if (autoOpen) {
                player.performCommand("menu");
            }
        }, 60L);
    }

    @EventHandler
    public void onCompassUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!isMenuCompass(item)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        player.performCommand("menu");
    }

    /** Gives the player the Server Menu compass if they don't already carry one. */
    public void giveMenuCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isMenuCompass(item)) {
                return;
            }
        }
        player.getInventory().addItem(createMenuCompass());
    }

    private ItemStack createMenuCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.setDisplayName("§6Server Menu");
        meta.setLore(List.of("§7Tap to open the server menu"));
        meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BOOLEAN, true);
        compass.setItemMeta(meta);
        return compass;
    }

    private boolean isMenuCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(compassKey, PersistentDataType.BOOLEAN, false);
    }
}
