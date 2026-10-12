/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.winkreyes.weekendxp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class WeekendXP
extends JavaPlugin {
    private static final ZoneId ZONE = ZoneId.of("America/Chicago");
    private boolean boosted = false;

    public void onEnable() {
        this.applyCorrectRate();
        Bukkit.getScheduler().runTaskTimer((Plugin)this, this::applyCorrectRate, 36000L, 36000L);
        this.getLogger().info("WeekendXP enabled: mcMMO 1.2x XP on Sat-Sun (America/Chicago).");
    }

    private void applyCorrectRate() {
        boolean bl;
        DayOfWeek dayOfWeek = LocalDate.now(ZONE).getDayOfWeek();
        boolean bl2 = bl = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        if (bl && !this.boosted) {
            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)"xprate 1.2");
            this.boosted = true;
            this.getLogger().info("Weekend detected (" + String.valueOf(dayOfWeek) + "): mcMMO XP rate set to 1.2x.");
        } else if (!bl && this.boosted) {
            Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)"xprate reset");
            this.boosted = false;
            this.getLogger().info("Weekday detected (" + String.valueOf(dayOfWeek) + "): mcMMO XP rate reset to normal.");
        }
    }
}

