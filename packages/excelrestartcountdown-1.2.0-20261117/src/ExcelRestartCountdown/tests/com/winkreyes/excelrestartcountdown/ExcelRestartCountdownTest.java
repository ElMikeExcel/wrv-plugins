package com.winkreyes.excelrestartcountdown;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Behavior contract for the renamed 1.1.0-line countdown (ExcelRestartCountdown
 * 1.2.0): the canonical command is excelrestartwarn, the pre-rename alias and
 * permission keep working, and the countdown flow itself is unchanged.
 * Note: the wrv-plugins PR #1 test targets the uninstalled 1.1.1 recovery
 * build (titles + shutdown task) and does not apply to this line.
 */
class ExcelRestartCountdownTest {
    private ExcelRestartCountdown plugin;
    private CommandSender sender;
    private Command command;
    private BukkitScheduler scheduler;
    private BukkitTask countdownTask;
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        plugin = mock(ExcelRestartCountdown.class, CALLS_REAL_METHODS);
        sender = mock(CommandSender.class);
        command = mock(Command.class);
        scheduler = mock(BukkitScheduler.class);
        countdownTask = mock(BukkitTask.class);
        when(command.getName()).thenReturn("excelrestartwarn");
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private void grant(String... permissions) {
        when(sender.hasPermission(anyString())).thenAnswer(inv -> {
            for (String p : permissions) {
                if (p.equals(inv.getArgument(0))) {
                    return true;
                }
            }
            return false;
        });
    }

    @Test
    void senderWithoutEitherPermissionIsRefused() {
        grant();
        assertTrue(plugin.onCommand(sender, command, "excelrestartwarn", new String[0]));
        verify(sender).sendMessage(contains("You don't have permission"));
        verifyNoInteractions(scheduler);
    }

    @Test
    void legacyPermissionAloneStillWorks() {
        grant("restartcountdown.warn");
        assertTrue(plugin.onCommand(sender, command, "restartwarn", new String[] {"cancel"}));
        verify(sender).sendMessage(contains("No countdown is running."));
    }

    @Test
    void newPermissionWorks() {
        grant("excelrestartcountdown.warn");
        assertTrue(plugin.onCommand(sender, command, "excelrestartwarn", new String[] {"cancel"}));
        verify(sender).sendMessage(contains("No countdown is running."));
    }

    @Test
    void unknownCommandNameIsIgnored() {
        when(command.getName()).thenReturn("somethingelse");
        assertFalse(plugin.onCommand(sender, command, "somethingelse", new String[0]));
        verifyNoInteractions(sender);
    }

    @Test
    void startSchedulesTimerRefusesSecondStartAndCancelStopsIt() {
        grant("excelrestartcountdown.warn");
        ArgumentCaptor<Runnable> tick = ArgumentCaptor.forClass(Runnable.class);
        when(scheduler.runTaskTimer(eq(plugin), tick.capture(), eq(0L), eq(20L))).thenReturn(countdownTask);

        assertTrue(plugin.onCommand(sender, command, "excelrestartwarn", new String[] {"maintenance"}));
        verify(sender).sendMessage(contains("Restart countdown started."));
        verify(sender).sendMessage(contains("Reason: maintenance"));

        // First tick broadcasts the 2-minute warning (with the reason suffix).
        tick.getValue().run();
        bukkit.verify(() -> Bukkit.broadcastMessage(contains("Server restarting in 2 minutes")));

        // A second start while running is refused.
        assertTrue(plugin.onCommand(sender, command, "excelrestartwarn", new String[0]));
        verify(sender).sendMessage(contains("A countdown is already running."));

        // Cancel stops the task and broadcasts the cancellation.
        assertTrue(plugin.onCommand(sender, command, "excelrestartwarn", new String[] {"cancel"}));
        verify(countdownTask).cancel();
        bukkit.verify(() -> Bukkit.broadcastMessage(contains("Restart countdown cancelled.")));
        verify(sender).sendMessage(contains("Countdown cancelled."));
    }
}
