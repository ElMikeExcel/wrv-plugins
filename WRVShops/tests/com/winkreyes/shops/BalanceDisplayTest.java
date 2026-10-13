// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BalanceDisplayTest {
    WRVShops plugin; Economy economy; Player player; ServicesManager services;
    RegisteredServiceProvider<Economy> registration; BalanceDisplay display;
    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        plugin=mock(WRVShops.class); economy=mock(Economy.class); player=mock(Player.class);
        Server server=mock(Server.class); services=mock(ServicesManager.class); registration=mock(RegisteredServiceProvider.class);
        when(plugin.getServer()).thenReturn(server); when(server.getServicesManager()).thenReturn(services);
        when(services.getRegistration(Economy.class)).thenReturn(registration); when(registration.getProvider()).thenReturn(economy);
        when(economy.isEnabled()).thenReturn(true); when(economy.hasAccount(player)).thenReturn(true);
        when(economy.getBalance(player)).thenReturn(1500.0);
        World world=mock(World.class); when(player.getWorld()).thenReturn(world); when(world.getName()).thenReturn("world");
        when(economy.hasAccount(player,"world")).thenReturn(true); when(economy.getBalance(player,"world")).thenReturn(2500.0);
        display=new BalanceDisplay(plugin,economy);
    }
    @Test void buyShowsCurrentCostAndExactRemainingWithoutMutation() {
        assertEquals("Current balance: $1,500.00\nPurchase cost: -$1,000.00\nBalance after purchase: $500.00",display.preview(player,100000,false,false));
        verify(economy,never()).withdrawPlayer(any(org.bukkit.OfflinePlayer.class),anyDouble());
        verify(economy,never()).depositPlayer(any(org.bukkit.OfflinePlayer.class),anyDouble());
    }
    @Test void insufficientFundsShowsShortfallInsteadOfClampingToZero() {
        when(economy.getBalance(player)).thenReturn(999.99);
        assertTrue(display.preview(player,100000,false,false).endsWith("Balance after purchase: -$0.01\nInsufficient funds. Need $0.01 more."));
    }
    @Test void zeroBalanceAndExactFundsAreValid() {
        when(economy.getBalance(player)).thenReturn(0.0); assertEquals("Current balance: $0.00",display.current(player,false));
        when(economy.getBalance(player)).thenReturn(1000.0); assertTrue(display.preview(player,100000,false,false).endsWith("Balance after purchase: $0.00"));
    }
    @Test void sellCreditsWorldAwareAccount() {
        assertEquals("Current balance: $2,500.00\nSale proceeds: +$5.25\nBalance after sale: $2,505.25",display.preview(player,525,true,true));
        verify(economy,never()).getBalance(player);
    }
    @Test void staleProviderDoesNotInventAZero() {
        when(registration.getProvider()).thenReturn(mock(Economy.class));
        assertEquals("Current balance: unavailable",display.current(player,false)); verify(economy,never()).getBalance(player);
    }
    @Test void disabledOrMissingAccountIsUnavailable() {
        when(economy.isEnabled()).thenReturn(false); assertEquals("Current balance: unavailable",display.current(player,false));
        when(economy.isEnabled()).thenReturn(true); when(economy.hasAccount(player)).thenReturn(false);
        assertTrue(display.preview(player,100,false,false).endsWith("Balance after purchase: unavailable"));
    }
    @ParameterizedTest @ValueSource(doubles={Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY})
    void nonFiniteBalanceIsUnavailable(double bad) {
        when(economy.getBalance(player)).thenReturn(bad); assertEquals("Current balance: unavailable",display.current(player,false));
    }
    @Test void providerFailureIsUnavailable() {
        when(economy.getBalance(player)).thenThrow(new IllegalStateException("offline"));
        assertEquals("Balance unavailable | Shop",display.title(player,"Shop",false));
    }
    @Test void eachDisplayReadsFreshValueAndSupportsBalancesAboveTransactionLimit() {
        when(economy.getBalance(player)).thenReturn(1500.0,2500.0,9999999999.99);
        assertEquals("Balance $1,500.00 | Shop",display.title(player,"Shop",false));
        assertEquals("Current balance: $2,500.00",display.current(player,false));
        assertEquals("Current balance: $9,999,999,999.99",display.current(player,false));
    }
    @Test void formatterHandlesFreeAndNegativeDisplayAmounts() {
        assertEquals("$0.00",BalanceDisplay.money(0)); assertEquals("-$0.01",BalanceDisplay.money(-1));
    }
}
