package com.winkreyes.excelclaims;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;

/**
 * ExcelClaims — Vault economy access by reflection (no compile-time
 * Vault dependency). Only exercised when the config turns claim-block
 * buying/selling ON; the live configuration has both at 0.0 (off), so
 * on Wink Reyes Valley these paths stay dormant and the commands
 * answer with the economy-disabled message. Excel — Iris / Mike.
 */
public final class VaultBridge {

    private VaultBridge() { }

    private static Object economy() {
        try {
            Class<?> ecoClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(ecoClass);
            return rsp == null ? null : rsp.getProvider();
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean charge(Player p, double amount) {
        Object eco = economy();
        if (eco == null || amount <= 0) return false;
        try {
            Method has = eco.getClass().getMethod("has", org.bukkit.OfflinePlayer.class, double.class);
            if (!(boolean) has.invoke(eco, p, amount)) return false;
            Method withdraw = eco.getClass().getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class);
            Object response = withdraw.invoke(eco, p, amount);
            Method success = response.getClass().getMethod("transactionSuccess");
            return (boolean) success.invoke(response);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void pay(Player p, double amount) {
        Object eco = economy();
        if (eco == null || amount <= 0) return;
        try {
            Method deposit = eco.getClass().getMethod("depositPlayer", org.bukkit.OfflinePlayer.class, double.class);
            deposit.invoke(eco, p, amount);
        } catch (Throwable ignored) { }
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley
