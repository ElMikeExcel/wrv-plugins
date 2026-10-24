package com.winkreyes.exceldeathrecord;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Vault-backed Money (Essentials economy on WRV). Loaded only when Vault is present. */
final class VaultMoney implements Money {
    private final Economy eco;

    VaultMoney() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        eco = rsp == null ? null : rsp.getProvider();
    }

    public boolean ready() { return eco != null && eco.isEnabled(); }
    public double balance(Player p) { return eco.getBalance(p); }
    public boolean withdraw(Player p, double amount) {
        if (amount <= 0) return true;
        if (!eco.has(p, amount)) return false;
        EconomyResponse r = eco.withdrawPlayer(p, amount);
        return r != null && r.transactionSuccess() && Math.abs(r.amount - amount) < 0.005;
    }
    public boolean deposit(Player p, double amount) {
        if (amount <= 0) return true;
        EconomyResponse r = eco.depositPlayer(p, amount);
        return r != null && r.transactionSuccess();
    }
    public String name() { return eco == null ? "none" : eco.getName(); }
}
