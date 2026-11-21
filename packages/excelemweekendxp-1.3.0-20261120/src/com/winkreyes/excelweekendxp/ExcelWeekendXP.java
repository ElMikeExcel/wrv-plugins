package com.winkreyes.excelweekendxp;

import java.time.Instant;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Applies mcMMO, vanilla, and EliteMobs skill 1.2x weekend XP on Friday/Saturday/Sunday in Chicago. */
public class ExcelWeekendXP extends JavaPlugin {
    private WeekendSchedule schedule;
    private VanillaXpMultiplier vanillaXp;
    private EmSkillXpMultiplier emSkillXp;
    private BukkitTask checkTask;

    @Override
    public void onEnable() {
        migrateLegacyDataFolder();
        schedule = new WeekendSchedule();
        // 1.2.1 (F-1 fix): WeekendXP owns the vanilla multiplier itself via a
        // PlayerExpChangeEvent listener. The 1.2.0 reflective mcMMO bridge is gone:
        // the live patched mcMMO never exposed that API, so the vanilla leg could
        // never engage. The mcmmo:xprate leg below is unchanged.
        vanillaXp = new VanillaXpMultiplier();
        getServer().getPluginManager().registerEvents(vanillaXp, this);
        // 1.3.0 (D155): third leg — EliteMobs skill XP via EliteMobs' own
        // EliteSkillXpGainEvent. Registered ONLY when EliteMobs is present and
        // enabled (plugin.yml softdepends on it, so it loads first when it
        // exists). Absent EM is not an error: the plugin runs with the two
        // existing legs and the schedule treats the EM leg as a no-op.
        Plugin eliteMobs = getServer().getPluginManager().getPlugin("EliteMobs");
        if (eliteMobs != null && eliteMobs.isEnabled()) {
            emSkillXp = new EmSkillXpMultiplier();
            getServer().getPluginManager().registerEvents(emSkillXp, this);
            getLogger().info("EliteMobs detected: EliteMobs skill XP weekend multiplier leg enabled.");
        } else {
            getLogger().info("EliteMobs not present or not enabled:"
                    + " running with the mcMMO and vanilla XP legs only.");
        }
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
                    vanillaXp::applyMultiplier,
                    emSkillXp == null ? multiplier -> true : emSkillXp::applyMultiplier);
            if (result == WeekendSchedule.Result.FAILED) {
                getLogger().warning("Weekend XP schedule incomplete; will retry the mcMMO command"
                        + ", vanilla XP multiplier, and EliteMobs skill XP multiplier at the next check.");
            } else if (result == WeekendSchedule.Result.APPLIED) {
                boolean weekend = WeekendSchedule.isWeekend(now);
                String rate = weekend ? "1.2" : "1.0";
                String message = (weekend
                        ? "Weekend detected: dispatched mcMMO 1.2x XP-rate command; verified vanilla XP event multiplier 1.2x."
                        : "Weekday detected: dispatched mcMMO XP-rate reset command; verified vanilla XP event multiplier 1.0x.");
                if (emSkillXp != null) {
                    message += " Verified EliteMobs skill XP event multiplier " + rate + "x.";
                }
                getLogger().info(message);
            }
        } catch (RuntimeException e) {
            getLogger().warning("XP schedule failed; will retry at the next check: " + e.getMessage());
        }
    }
}
