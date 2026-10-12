package com.winkreyes.weekendxp;

import java.time.Instant;
import org.bukkit.command.CommandException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Applies mcMMO's 1.2x weekend XP event on Saturday/Sunday in Chicago. */
public class WeekendXP extends JavaPlugin {
    private WeekendSchedule schedule;
    private BukkitTask checkTask;

    @Override
    public void onEnable() {
        schedule = new WeekendSchedule();
        applyCorrectRate();
        // Re-check every 30 minutes in case the server stays up across midnight.
        checkTask = getServer().getScheduler().runTaskTimer(this, this::applyCorrectRate,
                20L * 60 * 30, 20L * 60 * 30);
        getLogger().info("WeekendXP enabled: mcMMO 1.2x XP on Sat-Sun (America/Chicago).");
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
                    command -> getServer().dispatchCommand(getServer().getConsoleSender(), command));
            if (result == WeekendSchedule.Result.FAILED) {
                getLogger().warning("mcMMO XP-rate command could not be dispatched; will retry at the next check.");
            } else if (result == WeekendSchedule.Result.APPLIED) {
                getLogger().info(WeekendSchedule.isWeekend(now)
                        ? "Weekend detected: dispatched mcMMO 1.2x XP-rate command."
                        : "Weekday detected: dispatched mcMMO XP-rate reset command.");
            }
        } catch (CommandException e) {
            getLogger().warning("mcMMO XP-rate command failed; will retry at the next check: " + e.getMessage());
        }
    }
}
