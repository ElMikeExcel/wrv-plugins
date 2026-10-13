// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Read-only display snapshots; never used as authorization to commit a trade. */
public final class BalanceDisplay {
    private final WRVShops plugin;
    private final Economy economy;
    public BalanceDisplay(WRVShops plugin, Economy economy) { this.plugin=plugin; this.economy=economy; }

    private BigDecimal balance(Player player, boolean worldAware) {
        try {
            RegisteredServiceProvider<Economy> current=plugin.getServer().getServicesManager().getRegistration(Economy.class);
            if(current==null || current.getProvider()!=economy || !economy.isEnabled()) return null;
            boolean account=worldAware?economy.hasAccount(player,player.getWorld().getName()):economy.hasAccount(player);
            if(!account)return null;
            double value=worldAware?economy.getBalance(player,player.getWorld().getName()):economy.getBalance(player);
            return Double.isFinite(value)?BigDecimal.valueOf(value):null;
        } catch(RuntimeException | LinkageError unavailable) { return null; }
    }
    public static String money(long cents) { return money(BigDecimal.valueOf(cents,2)); }
    private static String money(BigDecimal amount) {
        DecimalFormat format=new DecimalFormat("#,##0.00",DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return (amount.signum()<0?"-$":"$")+format.format(amount.abs());
    }
    public String current(Player player, boolean worldAware) {
        BigDecimal value=balance(player,worldAware);
        return "Current balance: "+(value==null?"unavailable":money(value));
    }
    public String title(Player player, String title, boolean worldAware) {
        BigDecimal value=balance(player,worldAware);
        return (value==null?"Balance unavailable":"Balance "+money(value))+" | "+title;
    }
    public String preview(Player player, long totalCents, boolean selling, boolean worldAware) {
        if(totalCents<0)throw new IllegalArgumentException("Negative trade total");
        BigDecimal value=balance(player,worldAware), total=BigDecimal.valueOf(totalCents,2);
        String text="Current balance: "+(value==null?"unavailable":money(value))
                +"\n"+(selling?"Sale proceeds: +":"Purchase cost: -")+money(total);
        if(value==null)return text+"\nBalance after "+(selling?"sale":"purchase")+": unavailable";
        BigDecimal projected=selling?value.add(total):value.subtract(total);
        text+="\nBalance after "+(selling?"sale":"purchase")+": "+money(projected);
        if(!selling&&projected.signum()<0)text+="\nInsufficient funds. Need "+money(projected.negate())+" more.";
        return text;
    }
}
