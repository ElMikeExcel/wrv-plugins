package com.winkreyes.exceldeathrecord;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * 0.3.0: the money side of Lost &amp; Found, behind a tiny interface so
 * the plugin still loads (and Lost &amp; Found just says it is off) when
 * Vault or an economy is missing. VaultMoney is only class-loaded
 * after Vault is confirmed present.
 */
interface Money {
    boolean ready();
    double balance(Player p);
    /** True only when the whole amount was taken. */
    boolean withdraw(Player p, double amount);
    boolean deposit(Player p, double amount);
    String name();

    static Money find() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) return none();
            Money m = new VaultMoney();
            return m.ready() ? m : none();
        } catch (Throwable t) {
            return none();
        }
    }

    static Money none() {
        return new Money() {
            public boolean ready() { return false; }
            public double balance(Player p) { return 0; }
            public boolean withdraw(Player p, double amount) { return false; }
            public boolean deposit(Player p, double amount) { return false; }
            public String name() { return "none"; }
        };
    }
}
