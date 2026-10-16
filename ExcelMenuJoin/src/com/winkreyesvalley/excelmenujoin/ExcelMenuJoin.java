package com.winkreyesvalley.excelmenujoin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ExcelMenuJoin 1.3.0 - full rename of MenuJoin 1.2.0 (same features: menu
 * compass on join/respawn, compass never drops on death and cannot be thrown
 * away, /compass to replace a lost one, /excelmenujoin toggle, %menujoin_status%).
 *
 * Rename compatibility (nothing player-facing breaks):
 *  - PlaceholderAPI expansion identifier stays "menujoin" (%menujoin_status%).
 *  - /menujoin keeps working as an alias of /excelmenujoin.
 *  - The auto-open toggle is stored in the player PDC. Old data lives under
 *    menujoin:autojoin_disabled; the new key is excelmenujoin:autojoin_disabled.
 *    Reads check the new key first and fall back to the legacy key; writes set
 *    BOTH keys so the state stays consistent for old and new readers.
 *  - Menu compasses: new compasses are stamped with the new key
 *    excelmenujoin:menu_compass AND the legacy key menujoin:menu_compass.
 *    Compasses carrying only the legacy stamp are still recognized and get the
 *    new stamp added on join (restamp), keeping the legacy stamp so other
 *    plugins that check menujoin:menu_compass (e.g. ExcelShops) keep working.
 *  - On first enable, any files in the old plugins/MenuJoin data folder are
 *    copied to plugins/ExcelMenuJoin.
 */
public class ExcelMenuJoin extends JavaPlugin implements Listener {
    private NamespacedKey disabledKey;
    private NamespacedKey legacyDisabledKey; // menujoin:autojoin_disabled (pre-rename data)
    private NamespacedKey compassKey;
    private NamespacedKey legacyCompassKey; // menujoin:menu_compass - same as 1.1.0/1.2.0, existing compasses keep working
    private ExcelMenuJoinExpansion expansion;

    @Override
    public void onEnable() {
        migrateLegacyDataFolder("MenuJoin");
        disabledKey = new NamespacedKey(this, "autojoin_disabled");
        legacyDisabledKey = new NamespacedKey("menujoin", "autojoin_disabled");
        compassKey = new NamespacedKey(this, "menu_compass");
        legacyCompassKey = new NamespacedKey("menujoin", "menu_compass");
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("excelmenujoin").setExecutor(this::toggleCommand);
        getCommand("compass").setExecutor(this::compassCommand);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            expansion = new ExcelMenuJoinExpansion(this);
            expansion.register();
            getLogger().info("Hooked into PlaceholderAPI (%menujoin_status%).");
        }
        getLogger().info("ExcelMenuJoin enabled.");
    }

    @Override
    public void onDisable() {
        if (expansion != null) {
            try { expansion.unregister(); } catch (Exception ignored) { }
        }
    }

    /**
     * One-time rename migration: if our data folder has no files yet and the
     * legacy plugin's data folder (plugins/<legacyName>) exists, copy its
     * contents across so no config or data is lost in the rename.
     */
    private void migrateLegacyDataFolder(String legacyName) {
        try {
            File newDir = getDataFolder();
            File legacyDir = new File(newDir.getParentFile(), legacyName);
            if (!legacyDir.isDirectory() || legacyDir.equals(newDir)) {
                return;
            }
            if (newDir.isDirectory()) {
                try (var stream = Files.walk(newDir.toPath())) {
                    if (stream.anyMatch(p -> !p.equals(newDir.toPath()))) {
                        return; // new folder already has content; never overwrite
                    }
                }
            }
            final Path from = legacyDir.toPath();
            final Path to = newDir.toPath();
            try (var stream = Files.walk(from)) {
                stream.forEach(src -> {
                    try {
                        Path dst = to.resolve(from.relativize(src));
                        if (Files.isDirectory(src)) {
                            Files.createDirectories(dst);
                        } else {
                            Files.createDirectories(dst.getParent());
                            Files.copy(src, dst);
                        }
                    } catch (IOException e) {
                        getLogger().warning("Data migration copy failed for " + src + ": " + e.getMessage());
                    }
                });
            }
            getLogger().info("Migrated legacy data folder plugins/" + legacyName + " -> plugins/" + newDir.getName());
        } catch (Exception e) {
            getLogger().warning("Legacy data migration (" + legacyName + ") failed: " + e.getMessage());
        }
    }

    public boolean isDisabled(Player p) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        Boolean current = pdc.get(disabledKey, PersistentDataType.BOOLEAN);
        if (current != null) {
            return current;
        }
        // Pre-rename data: written by MenuJoin under the menujoin namespace.
        return pdc.getOrDefault(legacyDisabledKey, PersistentDataType.BOOLEAN, false);
    }

    public void setDisabled(Player p, boolean disabled) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (disabled) {
            // Write BOTH keys so a rollback to MenuJoin (or any legacy reader)
            // still sees the same toggle state.
            pdc.set(disabledKey, PersistentDataType.BOOLEAN, true);
            pdc.set(legacyDisabledKey, PersistentDataType.BOOLEAN, true);
        } else {
            pdc.remove(disabledKey);
            pdc.remove(legacyDisabledKey);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        boolean open = !isDisabled(p);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!p.isOnline()) return;
            restampLegacyCompasses(p);
            giveMenuCompass(p);
            if (open) p.performCommand("menu");
        }, 60L);
    }

    @EventHandler
    public void onCompassUse(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        if (!isMenuCompass(e.getItem())) return;
        // Respect an earlier plugin's cancellation (e.g. ExcelShops handling a
        // shop sign tap with the menu compass). Carried over from MenuJoin's
        // own fix list; the legacy MenuJoin 1.2.0 handler did not check this.
        if (e.isCancelled()) return;
        e.setCancelled(true);
        e.getPlayer().performCommand("menu");
    }

    // Give a compass back after respawn (1 tick later, once the player is in the world)
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (p.isOnline()) giveMenuCompass(p);
        }, 1L);
    }

    // Menu compass never drops on death (other drops untouched)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        e.getDrops().removeIf(this::isMenuCompass);
        e.getItemsToKeep().removeIf(this::isMenuCompass); // keepInventory: avoid a doubled compass
    }

    // Menu compass can't be thrown out of the inventory
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
        // New stamp under our own namespace, plus the legacy stamp so plugins
        // written against MenuJoin (e.g. ExcelShops' shop-sign check for
        // menujoin:menu_compass) keep recognizing the compass.
        meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BOOLEAN, true);
        meta.getPersistentDataContainer().set(legacyCompassKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Restamp: any carried compass that only has the legacy menujoin stamp
     * gets the new excelmenujoin stamp added. The legacy stamp is kept (see
     * the class javadoc), so recognition never regresses for any reader.
     */
    private void restampLegacyCompasses(Player p) {
        for (ItemStack it : p.getInventory().getContents()) {
            if (it == null || it.getType() != Material.COMPASS || !it.hasItemMeta()) continue;
            ItemMeta meta = it.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            boolean legacy = pdc.getOrDefault(legacyCompassKey, PersistentDataType.BOOLEAN, false);
            boolean current = pdc.getOrDefault(compassKey, PersistentDataType.BOOLEAN, false);
            if (legacy && !current) {
                pdc.set(compassKey, PersistentDataType.BOOLEAN, true);
                it.setItemMeta(meta);
            }
        }
    }

    private boolean isMenuCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(compassKey, PersistentDataType.BOOLEAN, false)
                || pdc.getOrDefault(legacyCompassKey, PersistentDataType.BOOLEAN, false);
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
