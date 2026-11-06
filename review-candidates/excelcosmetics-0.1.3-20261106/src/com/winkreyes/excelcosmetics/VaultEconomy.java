package com.winkreyes.excelcosmetics;

import com.winkreyes.excelcosmetics.core.PurchaseService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

/** Vault-backed EconomyGateway. Vault is a soft depend: without
 *  it the buy route reports NO_ECONOMY and grants still work. */
final class VaultEconomy implements PurchaseService.EconomyGateway {

    private Economy economy;

    /** Returns true when a Vault economy provider was found. */
    boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    @Override public boolean available() { return economy != null; }

    @Override public double balance(UUID player) {
        if (economy == null) return 0;
        OfflinePlayer op = Bukkit.getOfflinePlayer(player);
        return economy.getBalance(op);
    }

    @Override public boolean withdraw(UUID player, double amount) {
        if (economy == null) return false;
        EconomyResponse r = economy.withdrawPlayer(Bukkit.getOfflinePlayer(player), amount);
        return r != null && r.transactionSuccess();
    }

    @Override public boolean deposit(UUID player, double amount) {
        if (economy == null) return false;
        EconomyResponse r = economy.depositPlayer(Bukkit.getOfflinePlayer(player), amount);
        return r != null && r.transactionSuccess();
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
