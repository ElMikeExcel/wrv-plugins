package com.winkreyes.restartcountdown;

import java.util.List;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RestartCountdownTest {
    private RestartCountdown plugin;
    private Server server;
    private BukkitScheduler scheduler;
    private BukkitTask countdown;
    private BukkitTask shutdown;
    private CommandSender sender;
    private Command command;
    private Player player;
    private Runnable tick;
    private Runnable stopServer;

    @BeforeEach
    void setUp() {
        plugin = mock(RestartCountdown.class, CALLS_REAL_METHODS);
        server = mock(Server.class);
        scheduler = mock(BukkitScheduler.class);
        countdown = mock(BukkitTask.class);
        shutdown = mock(BukkitTask.class);
        sender = mock(CommandSender.class);
        command = mock(Command.class);
        player = mock(Player.class);
        doReturn(server).when(plugin).getServer();
        when(server.getScheduler()).thenReturn(scheduler);
        doReturn(List.of(player)).when(server).getOnlinePlayers();
        when(command.getName()).thenReturn("restartwarn");
        when(sender.hasPermission("restartcountdown.warn")).thenReturn(true);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(20L)))
                .thenAnswer(invocation -> {
                    tick = invocation.getArgument(1);
                    return countdown;
                });
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), eq(40L)))
                .thenAnswer(invocation -> {
                    stopServer = invocation.getArgument(1);
                    return shutdown;
                });
    }

    private void start() {
        assertTrue(plugin.onCommand(sender, command, "restartwarn", new String[] {"menu", "repair"}));
    }

    private void finishCountdown() {
        for (int second = 120; second >= 0; second--) {
            tick.run();
        }
    }

    @Test
    void deliversEveryWarningInChatAndTitlesBeforeDelayedShutdown() {
        start();
        for (int second = 120; second > 0; second--) {
            tick.run();
        }
        assertNull(stopServer, "Shutdown must not be queued before all 120 seconds elapse");
        tick.run();
        ArgumentCaptor<String> chats = ArgumentCaptor.forClass(String.class);
        verify(server, times(14)).broadcastMessage(chats.capture());
        assertTrue(chats.getAllValues().stream().allMatch(message -> message.contains("menu repair")));
        verify(player).sendTitle("§cRestart in 1 minute", "§7menu repair", 0, 80, 0);
        for (int second = 10; second > 0; second--) {
            verify(player).sendTitle("§cRestart in " + second, "§7menu repair", 0, 20, 0);
        }
        verify(player).sendTitle("§cRestarting now!", "§7menu repair", 0, 40, 0);
        verify(countdown).cancel();
        verify(server, never()).shutdown();
        assertNotNull(stopServer);
        stopServer.run();
        verify(server).shutdown();
    }

    @Test
    void cancellationStopsAnActiveTimer() {
        start();
        plugin.onCommand(sender, command, "restartwarn", new String[] {"cancel"});
        verify(countdown).cancel();
        verify(player).sendTitle("§eRestart cancelled", "§7", 0, 60, 0);
        verify(scheduler, never()).runTaskLater(eq(plugin), any(Runnable.class), anyLong());
        verify(server, never()).shutdown();
    }

    @Test
    void queuedShutdownCanStillBeCancelledAndBlocksAnotherCountdown() {
        start();
        finishCountdown();
        start();
        verify(scheduler, times(1)).runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(20L));
        plugin.onCommand(sender, command, "restartwarn", new String[] {"cancel"});
        verify(shutdown).cancel();
        start();
        verify(scheduler, times(2)).runTaskTimer(eq(plugin), any(Runnable.class), eq(0L), eq(20L));
    }

    @Test
    void disableCancelsQueuedShutdown() {
        start();
        finishCountdown();
        plugin.onDisable();
        verify(shutdown).cancel();
        verify(server, never()).shutdown();
    }

    @Test
    void disableCancelsActiveCountdown() {
        start();
        plugin.onDisable();
        verify(countdown).cancel();
        assertNull(stopServer);
    }

    @Test
    void unauthorizedSenderCannotStartOrCancel() {
        when(sender.hasPermission("restartcountdown.warn")).thenReturn(false);
        plugin.onCommand(sender, command, "restartwarn", new String[] {"maintenance"});
        plugin.onCommand(sender, command, "restartwarn", new String[] {"cancel"});
        verifyNoInteractions(scheduler);
        verify(sender, times(2)).sendMessage("§cYou don't have permission to do that.");
    }

    @Test
    void cancellationWhenIdleDoesNotScheduleAnything() {
        plugin.onCommand(sender, command, "restartwarn", new String[] {"cancel"});
        verify(sender).sendMessage("§eNo countdown is running.");
        verifyNoInteractions(scheduler);
    }
}
