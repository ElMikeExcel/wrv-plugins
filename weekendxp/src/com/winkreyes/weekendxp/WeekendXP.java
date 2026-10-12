package com.winkreyes.weekendxp;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Applies mcMMO weekend XP boost automatically.
 * Saturday and Sunday: /xprate 1.2
 * Monday-Friday: /xprate reset
 * Matches the Jobs WeekendBoost schedule (Sat-Sun).
 */
public class WeekendXP extends JavaPlugin {

    private static final ZoneId ZONE = ZoneId.of("America/Chicago");
    private boolean boosted = false;

    @Override
    public void onEnable() {
        applyCorrectRate();
        // Re-check every 30 minutes in case the server stays up across midnight.
        Bukkit.getScheduler().runTaskTimer(this, this::applyCorrectRate, 20L * 60 * 30, 20L * 60 * 30);
        getLogger().info("WeekendXP enabled: mcMMO 1.2x XP on Sat-Sun (America/Chicago).");
    }

    private void applyCorrectRate() {
        DayOfWeek day = LocalDate.now(ZONE).getDayOfWeek();
        boolean weekend = (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);
        if (weekend && !boosted) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "xprate 1.2");
            boosted = true;
            getLogger().info("Weekend detected (" + day + "): mcMMO XP rate set to 1.2x.");
        } else if (!weekend && boosted) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "xprate reset");
            boosted = false;
            getLogger().info("Weekday detected (" + day + "): mcMMO XP rate reset to normal.");
        }
    }
}
