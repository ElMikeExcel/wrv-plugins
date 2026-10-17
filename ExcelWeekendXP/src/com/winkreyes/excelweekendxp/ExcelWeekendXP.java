package com.winkreyes.excelweekendxp;

import java.time.Instant;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Applies skill and vanilla 1.2x weekend XP events on Friday/Saturday/Sunday in Chicago. */
public class ExcelWeekendXP extends JavaPlugin {
    private WeekendSchedule schedule;
    private VanillaXpBridge vanillaXpBridge;
    private BukkitTask checkTask;

    @Override
    public void onEnable() {
        migrateLegacyDataFolder();
        schedule = new WeekendSchedule();
        vanillaXpBridge = new VanillaXpBridge(() -> {
            Plugin mcMMO = getServer().getPluginManager().getPlugin("mcMMO");
            return mcMMO != null && mcMMO.isEnabled() ? mcMMO : null;
        }, message -> getLogger().warning(message));
        vanillaXpBridge.isSupported();
        applyCorrectRate();
        // Re-check every 30 minutes in case the server stays up across midnight.
        checkTask = getServer().getScheduler().runTaskTimer(this, this::applyCorrectRate,
                20L * 60 * 30, 20L * 60 * 30);
        getLogger().info("ExcelWeekendXP scheduler enabled: America/Chicago, every 30 minutes.");
    }

    private void migrateLegacyDataFolder() {
        java.io.File dataFolder = getDataFolder();
        if (dataFolder == null || dataFolder.getParentFile() == null) {
            return;
        }
        java.io.File legacy = new java.io.File(dataFolder.getParentFile(), "WeekendXP");
        LegacyDataMigration.migrate(legacy, dataFolder, message -> getLogger().info(message));
    }

    @Override
    public void onDisable() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
    }

    private void applyCorrectRate() {
        Instant now = Instant.now();
        try {
            WeekendSchedule.Result result = schedule.apply(now,
                    command -> getServer().dispatchCommand(getServer().getConsoleSender(), command),
                    vanillaXpBridge::applyMultiplier);
            if (result == WeekendSchedule.Result.FAILED) {
                getLogger().warning("Weekend XP schedule incomplete; will retry the mcMMO command"
                        + " and vanilla XP multiplier at the next check.");
            } else if (result == WeekendSchedule.Result.APPLIED) {
                getLogger().info(WeekendSchedule.isWeekend(now)
                        ? "Weekend detected: dispatched mcMMO 1.2x XP-rate command; verified vanilla XP event multiplier 1.2x."
                        : "Weekday detected: dispatched mcMMO XP-rate reset command; verified vanilla XP event multiplier 1.0x.");
            }
        } catch (RuntimeException e) {
            getLogger().warning("XP schedule failed; will retry at the next check: " + e.getMessage());
        }
    }
}
