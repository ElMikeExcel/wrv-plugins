package com.winkreyes.excelshopvillager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * ExcelShopVillager 1.1.0 - full rename of ShopVillager 1.0.0 (package, main
 * class, plugin name, command canonical name). No behavior changes: the
 * villager binding tag ("wrv_spawnshop", stored on the entities themselves),
 * the open-command config, and all handlers are identical. On first enable,
 * the old plugins/ShopVillager data folder (config.yml) is copied across.
 * /shopvillager keeps working as an alias of /excelshopvillager.
 */
public final class ExcelShopVillager extends JavaPlugin implements Listener {
    static final String TAG = "wrv_spawnshop";
    private final Map<UUID, Long> last = new HashMap<>();

    @Override public void onEnable() {
        migrateLegacyDataFolder("ShopVillager");
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("ExcelShopVillager enabled: tap a 'Spawn Shop' villager to open the shop.");
    }

    /**
     * One-time rename migration: if our data folder has no files yet and the
     * legacy plugin's data folder (plugins/<legacyName>) exists, copy its
     * contents across so no config or data is lost in the rename. Runs before
     * saveDefaultConfig() so a migrated config.yml is never overwritten.
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

    static boolean isShop(Entity e) { return e != null && e.getScoreboardTags().contains(TAG); }

    private void open(Player p) {
        long now = System.currentTimeMillis();
        Long prev = last.get(p.getUniqueId());
        if (prev != null && now - prev < 500) return; // one tap fires several events
        last.put(p.getUniqueId(), now);
        String cmd = getConfig().getString("open-command", "dm open spawnshop %player%").replace("%player%", p.getName());
        Bukkit.getScheduler().runTask(this, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEntityEvent e) {
        if (getConfig().getBoolean("debug", false)) getLogger().info("interact "+e.getClass().getSimpleName()+" "+e.getRightClicked().getType()+" hand="+e.getHand()+" tagged="+isShop(e.getRightClicked()));
        if (!isShop(e.getRightClicked())) return;
        e.setCancelled(true);
        if (e.getHand() == EquipmentSlot.HAND) open(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteractAt(PlayerInteractAtEntityEvent e) {
        if (isShop(e.getRightClicked())) e.setCancelled(true);
    }

    // Safety net: never let a vanilla trade window open for a tagged villager.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onOpen(InventoryOpenEvent e) {
        if (e.getInventory().getType() == InventoryType.MERCHANT && e.getInventory() instanceof MerchantInventory mi
                && mi.getHolder() instanceof Entity ent && isShop(ent)) e.setCancelled(true);
    }

    @Override public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (!(s instanceof Player p)) { s.sendMessage("Run this in-game."); return true; }
        String sub = a.length > 0 ? a[0].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "create" -> {
                Villager v = p.getWorld().spawn(p.getLocation(), Villager.class, vil -> {
                    vil.setAI(false); vil.setInvulnerable(true); vil.setSilent(true); vil.setPersistent(true);
                    vil.setRemoveWhenFarAway(false); vil.setCollidable(false); vil.setCanPickupItems(false);
                    vil.setProfession(Villager.Profession.CARTOGRAPHER); vil.setVillagerLevel(5);
                    vil.setRecipes(new ArrayList<>());
                    vil.setCustomName(ChatColor.GREEN + "" + ChatColor.BOLD + "Spawn Shop"); vil.setCustomNameVisible(true);
                    vil.addScoreboardTag(TAG);
                });
                p.sendMessage(ChatColor.GREEN + "Spawn Shop villager created. Tap it to open the shop.");
            }
            case "remove" -> {
                int n = 0;
                for (Entity e : p.getNearbyEntities(3, 3, 3)) if (isShop(e)) { e.remove(); n++; }
                p.sendMessage(ChatColor.YELLOW + "Removed " + n + " Spawn Shop villager(s) within 3 blocks.");
            }
            case "tag" -> {
                Entity t = p.getTargetEntity(5);
                if (t instanceof Villager) { t.addScoreboardTag(TAG); p.sendMessage(ChatColor.GREEN + "That villager now opens the Spawn Shop."); }
                else p.sendMessage(ChatColor.RED + "Look at a villager within 5 blocks.");
            }
            default -> p.sendMessage(ChatColor.YELLOW + "/shopvillager create | remove | tag");
        }
        return true;
    }
}
