package com.winkreyes.wealthboard;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WealthBoardJoinTest {
    @Test
    void topThreeBecomeSeparateCompleteChatEntries() {
        try (Fixture f = new Fixture()) {
            OfflinePlayer first = f.balance(".VeryLongBedrockGamertag_123456789", 9000.25);
            OfflinePlayer second = f.balance("MobilePlayerTwo", 4706.23);
            OfflinePlayer third = f.balance("玩家名字", 1234.56);
            OfflinePlayer fourth = f.balance("NotInTopThree", 1);
            f.bukkit.when(Bukkit::getOfflinePlayers).thenReturn(new OfflinePlayer[]{third, fourth, first, second});
            f.join();
            f.delayed.getFirst().run();
            verify(f.player).sendTitle(WealthPopupText.TITLE, WealthPopupText.SUBTITLE, 10, 60, 20);
            ArgumentCaptor<String> chat = ArgumentCaptor.forClass(String.class);
            verify(f.player, times(8)).sendMessage(chat.capture());
            List<String> lines = chat.getAllValues().stream().map(WealthBoardJoinTest::plain).toList();
            assertEquals(List.of("Richest players", "1. .VeryLongBedrockGamertag_123456789", "  $9,000.25", "2. MobilePlayerTwo", "  $4,706.23", "3. 玩家名字", "  $1,234.56", "Full boards: /leaderboard"), lines);
            verify(f.scheduler).runTaskLater(eq(f.plugin), any(Runnable.class), eq(40L));
            verify(f.scheduler).runTaskLater(eq(f.plugin), any(Runnable.class), eq(100L));
        }
    }

    @Test
    void emptyBalancesKeepTitleCompactAndExplainInChat() {
        try (Fixture f = new Fixture()) {
            f.join();
            f.delayed.getFirst().run();
            verify(f.player).sendTitle(WealthPopupText.TITLE, WealthPopupText.EMPTY_SUBTITLE, 10, 60, 20);
            verify(f.player).sendMessage("\u00a77No balances yet - go earn!");
        }
    }

    @Test
    void disconnectBeforePopupProducesNoMessages() {
        try (Fixture f = new Fixture()) {
            f.join();
            when(f.player.isOnline()).thenReturn(false);
            f.delayed.getFirst().run();
            verify(f.player, never()).sendTitle(anyString(), anyString(), anyInt(), anyInt(), anyInt());
            verify(f.player, never()).sendMessage(anyString());
        }
    }

    private static String plain(String text) { return text.replaceAll("\u00a7[0-9a-fk-or]", ""); }

    private static final class Fixture implements AutoCloseable {
        final MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        final WealthBoard plugin = mock(WealthBoard.class, CALLS_REAL_METHODS);
        final Server server = mock(Server.class);
        final Player player = mock(Player.class);
        final Economy economy = mock(Economy.class);
        final BukkitScheduler scheduler = mock(BukkitScheduler.class);
        final List<Runnable> delayed = new ArrayList<>();

        @SuppressWarnings("unchecked")
        Fixture() {
            doReturn(server).when(plugin).getServer();
            doReturn(Logger.getAnonymousLogger()).when(plugin).getLogger();
            ServicesManager services = mock(ServicesManager.class);
            RegisteredServiceProvider<Economy> provider = mock(RegisteredServiceProvider.class);
            when(server.getServicesManager()).thenReturn(services);
            when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
            when(services.getRegistration(Economy.class)).thenReturn(provider);
            when(provider.getProvider()).thenReturn(economy);
            when(economy.format(anyDouble())).thenAnswer(a -> String.format(java.util.Locale.US, "$%,.2f", (double)a.getArgument(0)));
            when(player.isOnline()).thenReturn(true);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(Bukkit::getOfflinePlayers).thenReturn(new OfflinePlayer[0]);
            when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong())).thenAnswer(a -> {
                delayed.add(a.getArgument(1));
                return mock(org.bukkit.scheduler.BukkitTask.class);
            });
            plugin.onEnable();
        }

        OfflinePlayer balance(String name, double amount) {
            OfflinePlayer p = mock(OfflinePlayer.class);
            when(p.getName()).thenReturn(name);
            when(economy.getBalance(p)).thenReturn(amount);
            return p;
        }

        void join() {
            PlayerJoinEvent event = mock(PlayerJoinEvent.class);
            when(event.getPlayer()).thenReturn(player);
            plugin.onJoin(event);
        }

        public void close() { bukkit.close(); }
    }
}
