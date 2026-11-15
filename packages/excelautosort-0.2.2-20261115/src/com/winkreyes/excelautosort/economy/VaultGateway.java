package com.winkreyes.excelautosort.economy;

import com.winkreyes.excelautosort.hook.EconomyPort;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

/**
 * The Vault economy adapter — the only class in the plugin that names
 * Vault types (SPEC §15.2 isolation rule, same shape as the house
 * Landmark economy gateway). Instantiated only when the Vault plugin
 * is present; if no economy provider is registered, every call fails
 * soft so a purchase attempt ends in the plain economy-down line
 * instead of a stack trace.
 */
public final class VaultGateway implements EconomyPort {

    private Economy economy;

    public VaultGateway() {
        resolve();
    }

    /**
     * Resolve the Economy provider — LAZILY, and retried while it is
     * missing: plugin enable order is not a contract (ExcelAutoSort
     * may wake before the economy provider registers), so the lookup
     * repeats on use until it succeeds, then caches. A purchase
     * attempted in the gap refuses cleanly instead of binding a
     * permanently-dead gateway at boot.
     */
    private Economy resolve() {
        if (economy != null) return economy;
        try {
            RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        } catch (Throwable t) {
            economy = null;
        }
        return economy;
    }

    @Override
    public boolean available() {
        return resolve() != null;
    }

    @Override
    public double balance(UUID player) {
        Economy econ = resolve();
        if (econ == null || player == null) return -1;
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(player);
            return econ.getBalance(op);
        } catch (Throwable t) {
            return -1;
        }
    }

    @Override
    public boolean withdraw(UUID player, double amount) {
        Economy econ = resolve();
        if (econ == null || player == null || amount <= 0) return false;
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(player);
            EconomyResponse r = econ.withdrawPlayer(op, amount);
            return r != null && r.transactionSuccess();
        } catch (Throwable t) {
            return false;
        }
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
