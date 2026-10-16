package com.winkreyes.excelwealthboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Mocked scheduler/API contract tests. These inspect requested server delays and
 * title arguments; they do not emulate a Java client, Geyser or network latency.
 */
class ExcelWealthBoardTimingTest {
    private static final String TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "Wink Reyes Valley";

    @Test
    void preservesTopThreeOrderingFullTextFormattingAndCadence() {
        try (Fixture f = new Fixture()) {
            OfflinePlayer first = f.balance(".VeryLongBedrockGamertag_123456789", 9000.25);
            OfflinePlayer second = f.balance("RankTwoFixture", 4300.75);
            OfflinePlayer third = f.balance("RankThreeFixture", 1200.50);
            OfflinePlayer fourth = f.balance("NotInTopThree", 1);
            f.players(third, fourth, first, second);
            f.join();
            assertEquals(List.of(200L, 100L), f.requestedDelays);
            f.runThrough(199);
            assertTrue(f.frames.isEmpty(), "No welcome title before the existing join delay");
            f.runThrough(400);
            assertEquals(List.of(200L, 100L, 0L, 60L, 120L), f.requestedDelays);
            assertEquals(List.of(200L, 260L, 320L), f.frames.stream().map(Frame::tick).toList());
            assertEquals(List.of("Richest: 1. .VeryLongBedrockGamertag_123456789 $9,000.25",
                    "2. RankTwoFixture $4,300.75", "3. RankThreeFixture $1,200.50"),
                    f.frames.stream().map(frame -> ChatColor.stripColor(frame.subtitle())).toList());
            assertTrue(f.frames.stream().allMatch(frame -> TITLE.equals(frame.title())));
            assertEquals(ChatColor.YELLOW + "" + ChatColor.BOLD + "Richest: "
                    + ChatColor.GOLD + "1. " + ChatColor.WHITE + ".VeryLongBedrockGamertag_123456789"
                    + ChatColor.GREEN + " $9,000.25", f.frames.getFirst().subtitle());
            verify(f.player, never()).sendMessage(anyString());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void onlyFinalFrameFadesAndIntermediateLifetimeExceedsUpdateInterval(int count) {
        try (Fixture f = new Fixture()) {
            OfflinePlayer[] players = new OfflinePlayer[count];
            for (int i = 0; i < count; i++) players[i] = f.balance("Player" + i, 1000 - i);
            f.players(players);
            f.join();
            f.runThrough(400);
            assertEquals(count, f.frames.size());
            for (int i = 0; i < count; i++) {
                Frame frame = f.frames.get(i);
                boolean last = i == count - 1;
                assertEquals(i == 0 ? 10 : 0, frame.fadeIn());
                assertEquals(last ? 55 : 70, frame.stay());
                assertEquals(last ? 15 : 0, frame.fadeOut());
                if (!last) {
                    Frame next = f.frames.get(i + 1);
                    assertEquals(60, next.tick() - frame.tick());
                    assertTrue(frame.tick() + frame.fadeIn() + frame.stay() > next.tick(),
                            "Current title must outlive the next requested update");
                    assertTrue(frame.tick() + frame.fadeIn() + frame.stay() >= next.tick() + 10,
                            "Keep at least ten ticks of overlap for the zero-fade-in update");
                }
            }
        }
    }

    @Test
    void rankTwoStaysOpaqueAtTheOldFadeBoundary() {
        try (Fixture f = new Fixture()) {
            f.players(f.balance("One", 30), f.balance("Two", 20), f.balance("Three", 10));
            f.join();
            f.runThrough(400);
            Frame second = f.frames.get(1);
            Frame third = f.frames.get(2);
            assertEquals(0, second.fadeOut());
            for (long tick = second.tick() + 55; tick < third.tick(); tick++) {
                assertTrue(tick < second.tick() + second.fadeIn() + second.stay(),
                        "Rank-two frame must still be in its fully opaque stay phase");
            }
        }
    }

    @Test
    void finalFadeAndTotalScheduledPresentationRemainUnchanged() {
        try (Fixture f = new Fixture()) {
            f.players(f.balance("One", 30), f.balance("Two", 20), f.balance("Three", 10));
            f.join();
            f.runThrough(400);
            Frame last = f.frames.getLast();
            assertEquals(320, last.tick());
            assertEquals(0, last.fadeIn());
            assertEquals(55, last.stay());
            assertEquals(15, last.fadeOut());
            assertEquals(390, last.tick() + last.fadeIn() + last.stay() + last.fadeOut());
        }
    }

    @Test
    void emptyBalancesKeepTheExistingTitleSubtitleAndTimings() {
        try (Fixture f = new Fixture()) {
            f.join();
            f.runThrough(400);
            assertEquals(List.of(new Frame(200, TITLE, ChatColor.GRAY + "No balances yet - go earn!",
                    10, 60, 20)), f.frames);
            assertEquals(List.of(200L, 100L), f.requestedDelays);
            verify(f.player, never()).sendMessage(anyString());
        }
    }

    @Test
    void disconnectBeforeWelcomeSuppressesThePopup() {
        try (Fixture f = new Fixture()) {
            f.players(f.balance("One", 10));
            f.join();
            when(f.player.isOnline()).thenReturn(false);
            f.runThrough(400);
            assertTrue(f.frames.isEmpty());
            assertEquals(List.of(200L, 100L), f.requestedDelays);
        }
    }

    @Test
    void disconnectDuringCycleSuppressesRemainingFrames() {
        try (Fixture f = new Fixture()) {
            f.players(f.balance("One", 30), f.balance("Two", 20), f.balance("Three", 10));
            f.join();
            f.runThrough(200);
            assertEquals(1, f.frames.size());
            when(f.player.isOnline()).thenReturn(false);
            f.runThrough(400);
            assertEquals(1, f.frames.size());
        }
    }

    @Test
    void unreadableBalancesAndUnnamedPlayersAreStillSkipped() {
        try (Fixture f = new Fixture()) {
            OfflinePlayer unnamed = f.balance(null, 10000);
            OfflinePlayer bad = f.balance("Unreadable", 5000);
            when(f.economy.getBalance(bad)).thenThrow(new IllegalStateException("fixture"));
            f.players(unnamed, bad, f.balance("Good", 10));
            f.join();
            f.runThrough(400);
            assertEquals(1, f.frames.size());
            assertEquals("Richest: 1. Good $10.00", ChatColor.stripColor(f.frames.getFirst().subtitle()));
        }
    }

    @Test
    void formatterFailureKeepsExistingIntegerFallback() {
        try (Fixture f = new Fixture()) {
            f.players(f.balance("Good", 12.75));
            when(f.economy.format(anyDouble())).thenThrow(new IllegalStateException("fixture"));
            f.join();
            f.runThrough(400);
            assertEquals("Richest: 1. Good $12", ChatColor.stripColor(f.frames.getFirst().subtitle()));
        }
    }

    private record Frame(long tick, String title, String subtitle, int fadeIn, int stay, int fadeOut) {}
    private record Queued(long due, long order, Runnable runnable) {}

    private static final class Fixture implements AutoCloseable {
        final MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        final ExcelWealthBoard plugin = mock(ExcelWealthBoard.class, CALLS_REAL_METHODS);
        final Server server = mock(Server.class);
        final Player player = mock(Player.class);
        final Economy economy = mock(Economy.class);
        final BukkitScheduler scheduler = mock(BukkitScheduler.class);
        final List<Long> requestedDelays = new ArrayList<>();
        final List<Frame> frames = new ArrayList<>();
        final PriorityQueue<Queued> queue = new PriorityQueue<>(Comparator.comparingLong(Queued::due)
                .thenComparingLong(Queued::order));
        long now;
        long order;

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
            when(economy.format(anyDouble())).thenAnswer(a -> String.format(java.util.Locale.US,
                    "$%,.2f", (double) a.getArgument(0)));
            when(player.isOnline()).thenReturn(true);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            players();
            when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong())).thenAnswer(a -> {
                long delay = a.getArgument(2);
                requestedDelays.add(delay);
                queue.add(new Queued(now + delay, order++, a.getArgument(1)));
                return mock(BukkitTask.class);
            });
            doAnswer(a -> {
                frames.add(new Frame(now, a.getArgument(0), a.getArgument(1), a.getArgument(2),
                        a.getArgument(3), a.getArgument(4)));
                return null;
            }).when(player).sendTitle(anyString(), anyString(), anyInt(), anyInt(), anyInt());
            plugin.onEnable();
        }

        OfflinePlayer balance(String name, double amount) {
            OfflinePlayer p = mock(OfflinePlayer.class);
            when(p.getName()).thenReturn(name);
            when(economy.getBalance(p)).thenReturn(amount);
            return p;
        }

        void players(OfflinePlayer... players) {
            bukkit.when(Bukkit::getOfflinePlayers).thenReturn(players);
        }

        void join() {
            PlayerJoinEvent event = mock(PlayerJoinEvent.class);
            when(event.getPlayer()).thenReturn(player);
            plugin.onJoin(event);
        }

        void runThrough(long tick) {
            while (!queue.isEmpty() && queue.peek().due() <= tick) {
                Queued next = queue.remove();
                now = next.due();
                next.runnable().run();
            }
            now = tick;
        }

        public void close() { bukkit.close(); }
    }
}
