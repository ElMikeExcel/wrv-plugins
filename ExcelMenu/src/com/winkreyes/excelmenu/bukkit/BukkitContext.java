package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.RequirementEngine;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

/** RequirementEngine.Context backed by a live player. */
final class BukkitContext implements RequirementEngine.Context {

    private final Player player;

    BukkitContext(Player player) {
        this.player = player;
    }

    @Override
    public String resolve(String text) {
        return Placeholders.resolve(player, text);
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public double balance() {
        try {
            RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp == null) return 0;
            return rsp.getProvider().getBalance(player);
        } catch (Throwable t) {
            return 0;
        }
    }

    @Override
    public boolean hasItem(String material, int amount) {
        Material mat;
        try {
            mat = Material.valueOf(material.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }
        return player.getInventory().containsAtLeast(new ItemStack(mat), Math.max(1, amount));
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
