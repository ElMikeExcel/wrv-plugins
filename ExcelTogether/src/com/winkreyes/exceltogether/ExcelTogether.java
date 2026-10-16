package com.winkreyes.exceltogether;

import org.bukkit.plugin.java.JavaPlugin;

public class ExcelTogether extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        PartyBonus bonus = new PartyBonus(getConfig().getDouble("multiplier", PartyBonus.DEFAULT_MULTIPLIER),
                getConfig().getDouble("radius", PartyBonus.DEFAULT_RADIUS));
        getServer().getPluginManager().registerEvents(new PartyListener(bonus, new NearbyPartyFinder(bonus)), this);
        getLogger().info("ExcelTogether enabled: x" + bonus.multiplier() + " money and XP when another player is within "
                + bonus.radius() + " blocks.");
    }
}
