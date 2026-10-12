package com.winkreyes.wealthboard;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WealthBoardTest {
    private WealthBoard plugin;
    private Server server;
    private Economy economy;
    private Player player;
    private PluginManager pluginManager;
    private List<Runnable> delayed;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        plugin = mock(WealthBoard.class, CALLS_REAL_METHODS);
        server = mock(Server.class);
        economy = mock(Economy.class);
        player = mock(Player.class);
        pluginManager = mock(PluginManager.class);
        ServicesManager services = mock(ServicesManager.class);
        RegisteredServiceProvider<Economy> provider = mock(RegisteredServiceProvider.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        doReturn(server).when(plugin).getServer();
        doReturn(Logger.getAnonymousLogger()).when(plugin).getLogger();
        when(server.getServicesManager()).thenReturn(services);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(server.getScheduler()).thenReturn(scheduler);
        when(server.getOfflinePlayers()).thenReturn(new OfflinePlayer[0]);
        when(services.getRegistration(Economy.class)).thenReturn(provider);
        when(provider.getProvider()).thenReturn(economy);
        when(player.isOnline()).thenReturn(true);
        delayed = new ArrayList<>();
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong())).thenAnswer(invocation -> {
            delayed.add(invocation.getArgument(1));
            return mock(org.bukkit.scheduler.BukkitTask.class);
        });
        plugin.onEnable();
    }

    private void join() {
        PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        when(event.getPlayer()).thenReturn(player);
        plugin.onJoin(event);
    }

    @Test
    void disconnectedPlayerGetsNeitherPopupNorJobEnrollment() {
        join();
        when(player.isOnline()).thenReturn(false);
        delayed.forEach(Runnable::run);
        verify(player, never()).sendTitle(anyString(), anyString(), anyInt(), anyInt(), anyInt());
        verify(pluginManager, never()).isPluginEnabled("Jobs");
    }

    @Test
    void absentJobsDoesNotPreventWealthPopup() {
        join();
        delayed.forEach(Runnable::run);
        verify(player).sendTitle(anyString(), contains("No balances yet"), eq(10), eq(60), eq(20));
        verify(pluginManager).isPluginEnabled("Jobs");
    }

    @Test
    void popupRanksTopThreeAndSkipsInvalidBalances() {
        OfflinePlayer alice = balance("Alice", 200.0);
        OfflinePlayer bob = balance("Bob", 400.0);
        OfflinePlayer carol = balance("Carol", 300.0);
        OfflinePlayer dave = balance("Dave", 100.0);
        OfflinePlayer invalid = balance("Invalid", Double.NaN);
        OfflinePlayer infinite = balance("Infinite", Double.POSITIVE_INFINITY);
        when(server.getOfflinePlayers()).thenReturn(new OfflinePlayer[] {alice, invalid, dave, bob, infinite, carol});
        when(economy.format(anyDouble())).thenAnswer(invocation -> "$" + invocation.getArgument(0));
        join();
        delayed.get(0).run();
        ArgumentCaptor<String> subtitle = ArgumentCaptor.forClass(String.class);
        verify(player).sendTitle(anyString(), subtitle.capture(), eq(10), eq(60), eq(20));
        String text = subtitle.getValue();
        assertTrue(text.indexOf("Bob") < text.indexOf("Carol"));
        assertTrue(text.indexOf("Carol") < text.indexOf("Alice"));
        assertFalse(text.contains("Dave"));
        assertFalse(text.contains("Invalid"));
        assertFalse(text.contains("Infinite"));
    }

    private OfflinePlayer balance(String name, double amount) {
        OfflinePlayer offline = mock(OfflinePlayer.class);
        when(offline.getName()).thenReturn(name);
        when(economy.getBalance(offline)).thenReturn(amount);
        return offline;
    }

    @Test
    void providerCurrencyAndNegativeSignArePreserved() {
        when(economy.format(-123.45)).thenReturn("-€123,45");
        assertEquals("-€123,45", WealthBoard.formatMoney(economy, -123.45));
    }

    @Test
    void formatterFailurePreservesCentsAndLargeBalances() {
        when(economy.format(anyDouble())).thenThrow(new IllegalStateException("formatter unavailable"));
        assertEquals("-123.45", WealthBoard.formatMoney(economy, -123.45));
        assertEquals("3,000,000,000.25", WealthBoard.formatMoney(economy, 3000000000.25));
    }

    @Test
    void nullFormattingFallsBackToNumber() {
        assertEquals("12.50", WealthBoard.formatMoney(economy, 12.5));
    }
}
