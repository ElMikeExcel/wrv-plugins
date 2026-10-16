package com.winkreyes.excelnophantoms;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Prevents phantoms from spawning, period.
 */
public class ExcelNoPhantoms extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        migrateLegacyDataFolder();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ExcelNoPhantoms enabled: phantoms will not spawn.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.PHANTOM) {
            event.setCancelled(true);
        }
    }

    private void migrateLegacyDataFolder() {
        java.io.File dataFolder = getDataFolder();
        if (dataFolder == null || dataFolder.getParentFile() == null) {
            return;
        }
        java.io.File legacy = new java.io.File(dataFolder.getParentFile(), "NoPhantoms");
        LegacyDataMigration.migrate(legacy, dataFolder, message -> getLogger().info(message));
    }
}
