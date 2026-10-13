package com.winkreyes.weekendxp;

import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WeekendXPTest {
    public static class CustomMcMMO extends JavaPlugin {
        double multiplier;
        int writes;
        boolean supported;
        boolean rejectWrite;

        public boolean isVanillaXpBonusSupported() { return supported; }
        public void setVanillaXpEventMultiplier(double value) {
            if (rejectWrite) throw new IllegalStateException("temporary setter failure");
            multiplier = value;
            writes++;
        }
        public double getVanillaXpEventMultiplier() { return multiplier; }
    }

    private static class Fixture {
        final WeekendXP plugin = mock(WeekendXP.class, CALLS_REAL_METHODS);
        final Server server = mock(Server.class);
        final PluginManager plugins = mock(PluginManager.class);
        final BukkitScheduler scheduler = mock(BukkitScheduler.class);
        final BukkitTask task = mock(BukkitTask.class);
        final ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        final Logger logger = mock(Logger.class);

        Fixture(Plugin mcMMO) {
            doReturn(server).when(plugin).getServer();
            doReturn(logger).when(plugin).getLogger();
            when(server.getPluginManager()).thenReturn(plugins);
            when(plugins.getPlugin("mcMMO")).thenReturn(mcMMO);
            when(mcMMO.isEnabled()).thenReturn(true);
            when(server.getScheduler()).thenReturn(scheduler);
            when(server.getConsoleSender()).thenReturn(console);
            when(server.dispatchCommand(eq(console), anyString())).thenReturn(true);
            when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong()))
                    .thenReturn(task);
        }

        Runnable scheduledCheck() {
            ArgumentCaptor<Runnable> check = ArgumentCaptor.forClass(Runnable.class);
            verify(scheduler).runTaskTimer(eq(plugin), check.capture(),
                    eq(20L * 60 * 30), eq(20L * 60 * 30));
            return check.getValue();
        }
    }

    private static CustomMcMMO customMcMMO() {
        CustomMcMMO mcMMO = mock(CustomMcMMO.class, CALLS_REAL_METHODS);
        mcMMO.supported = true;
        return mcMMO;
    }

    @Test
    void startupAppliesImmediatelyAndRetainsThirtyMinuteIntervalAndCancellation() {
        CustomMcMMO mcMMO = customMcMMO();
        Fixture fixture = new Fixture(mcMMO);
        fixture.plugin.onEnable();
        assertEquals(1, mcMMO.writes);
        ArgumentCaptor<String> command = ArgumentCaptor.forClass(String.class);
        verify(fixture.server).dispatchCommand(eq(fixture.console), command.capture());
        assertEquals(command.getValue().contains("reset") ? 1.0 : 1.2, mcMMO.multiplier);
        fixture.scheduledCheck().run();
        assertEquals(1, mcMMO.writes, "An unchanged successfully applied rate is not repeated");
        verify(fixture.logger).info(contains("verified vanilla XP event multiplier"));
        fixture.plugin.onDisable();
        fixture.plugin.onDisable();
        verify(fixture.task, times(1)).cancel();
    }

    @Test
    void oldMcMMOWarnsAtStartupAndRetriesWithoutFalseSuccess() {
        Fixture fixture = new Fixture(mock(Plugin.class));
        fixture.plugin.onEnable();
        fixture.scheduledCheck().run();
        verify(fixture.server, times(2)).dispatchCommand(eq(fixture.console), anyString());
        verify(fixture.logger).warning(contains("custom mcMMO vanilla XP bridge"));
        verify(fixture.logger, never()).info(contains("verified vanilla"));
    }

    @Test
    void startupProbeWarnsAboutIncompatibilityEvenWhenCommandFails() {
        Fixture fixture = new Fixture(mock(Plugin.class));
        when(fixture.server.dispatchCommand(eq(fixture.console), anyString())).thenReturn(false);
        fixture.plugin.onEnable();
        verify(fixture.logger).warning(contains("custom mcMMO vanilla XP bridge"));
        fixture.scheduledCheck().run();
        verify(fixture.server, times(2)).dispatchCommand(eq(fixture.console), anyString());
        verify(fixture.logger, never()).info(contains("verified vanilla"));
    }

    @Test
    void temporaryVanillaFailureRetriesTheCommandAndBridge() {
        CustomMcMMO mcMMO = customMcMMO();
        mcMMO.rejectWrite = true;
        Fixture fixture = new Fixture(mcMMO);
        fixture.plugin.onEnable();
        assertEquals(0, mcMMO.writes);
        verify(fixture.logger, never()).info(contains("verified vanilla"));
        mcMMO.rejectWrite = false;
        fixture.scheduledCheck().run();
        assertEquals(1, mcMMO.writes);
        verify(fixture.server, times(2)).dispatchCommand(eq(fixture.console), anyString());
        verify(fixture.logger).info(contains("verified vanilla XP event multiplier"));
    }

    @Test
    void throwingCommandStillSchedulesRetryAndDoesNotSetVanilla() {
        CustomMcMMO mcMMO = customMcMMO();
        Fixture fixture = new Fixture(mcMMO);
        when(fixture.server.dispatchCommand(eq(fixture.console), anyString()))
                .thenThrow(new IllegalStateException("command failed")).thenReturn(true);
        fixture.plugin.onEnable();
        assertEquals(0, mcMMO.writes);
        verify(fixture.logger).warning(contains("command failed"));
        fixture.scheduledCheck().run();
        assertEquals(1, mcMMO.writes);
    }
}
