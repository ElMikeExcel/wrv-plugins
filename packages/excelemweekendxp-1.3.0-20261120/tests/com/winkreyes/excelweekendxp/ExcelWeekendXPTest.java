package com.winkreyes.excelweekendxp;

import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 1.2.1 (F-1 fix): the plugin no longer probes mcMMO for a bridge API. The fixture
 * therefore uses a plain mcMMO mock exposing NO bridge methods — exactly like the
 * live patched mcMMO (WRV-XP-2) — and the plugin must still enable cleanly and
 * apply the vanilla multiplier through its own listener.
 */
class ExcelWeekendXPTest {
    private static class Fixture {
        final ExcelWeekendXP plugin = mock(ExcelWeekendXP.class, CALLS_REAL_METHODS);
        final Server server = mock(Server.class);
        final PluginManager plugins = mock(PluginManager.class);
        final BukkitScheduler scheduler = mock(BukkitScheduler.class);
        final BukkitTask task = mock(BukkitTask.class);
        final ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        final Logger logger = mock(Logger.class);

        Fixture() {
            doReturn(server).when(plugin).getServer();
            doReturn(logger).when(plugin).getLogger();
            when(server.getPluginManager()).thenReturn(plugins);
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

        VanillaXpMultiplier registeredListener() {
            ArgumentCaptor<Listener> listener = ArgumentCaptor.forClass(Listener.class);
            verify(plugins).registerEvents(listener.capture(), eq(plugin));
            assertInstanceOf(VanillaXpMultiplier.class, listener.getValue());
            return (VanillaXpMultiplier) listener.getValue();
        }
    }

    @Test
    void startupAppliesImmediatelyAndRetainsThirtyMinuteIntervalAndCancellation() {
        Fixture fixture = new Fixture();
        fixture.plugin.onEnable();
        ArgumentCaptor<String> command = ArgumentCaptor.forClass(String.class);
        verify(fixture.server).dispatchCommand(eq(fixture.console), command.capture());
        double expected = command.getValue().contains("reset") ? 1.0 : 1.2;
        assertEquals(expected, fixture.registeredListener().getMultiplier(),
                "Listener multiplier must match the dispatched mcMMO command leg");
        fixture.scheduledCheck().run();
        verify(fixture.server, times(1)).dispatchCommand(eq(fixture.console), anyString());
        assertEquals(expected, fixture.registeredListener().getMultiplier(),
                "An unchanged successfully applied rate is not repeated");
        verify(fixture.logger).info(contains("verified vanilla XP event multiplier"));
        fixture.plugin.onDisable();
        fixture.plugin.onDisable();
        verify(fixture.task, times(1)).cancel();
    }

    @Test
    void mcMMOWithoutBridgeApiEnablesCleanlyWithNoGreenbarWarning() {
        // F-1 regression: the live patched mcMMO exposes no vanilla-bridge methods.
        // 1.2.1 must not warn about a missing bridge and must still verify the rate.
        Fixture fixture = new Fixture();
        fixture.plugin.onEnable();
        verify(fixture.logger, never()).warning(contains("bridge"));
        verify(fixture.logger, never()).warning(contains("greenbar"));
        verify(fixture.logger, never()).warning(contains("schedule incomplete"));
        verify(fixture.logger).info(contains("verified vanilla XP event multiplier"));
    }

    @Test
    void failedCommandDispatchRetriesInsteadOfClaimingSuccess() {
        Fixture fixture = new Fixture();
        when(fixture.server.dispatchCommand(eq(fixture.console), anyString())).thenReturn(false);
        fixture.plugin.onEnable();
        verify(fixture.logger).warning(contains("schedule incomplete"));
        verify(fixture.logger, never()).info(contains("verified vanilla"));
        fixture.scheduledCheck().run();
        verify(fixture.server, times(2)).dispatchCommand(eq(fixture.console), anyString());
        verify(fixture.logger, never()).info(contains("verified vanilla"));
    }

    @Test
    void eliteMobsPresentRegistersEmListenerWithMatchingRate() {
        // 1.3.0 (D155): with EliteMobs present and enabled, onEnable registers
        // the EM listener as a second listener and the schedule drives its
        // multiplier to the same rate as the mcMMO command leg.
        Fixture fixture = new Fixture();
        Plugin eliteMobs = mock(Plugin.class);
        when(eliteMobs.isEnabled()).thenReturn(true);
        when(fixture.plugins.getPlugin("EliteMobs")).thenReturn(eliteMobs);
        fixture.plugin.onEnable();
        ArgumentCaptor<Listener> listeners = ArgumentCaptor.forClass(Listener.class);
        verify(fixture.plugins, times(2)).registerEvents(listeners.capture(), eq(fixture.plugin));
        EmSkillXpMultiplier em = listeners.getAllValues().stream()
                .filter(EmSkillXpMultiplier.class::isInstance)
                .map(EmSkillXpMultiplier.class::cast)
                .findFirst().orElseThrow(() -> new AssertionError("No EM listener registered"));
        ArgumentCaptor<String> command = ArgumentCaptor.forClass(String.class);
        verify(fixture.server).dispatchCommand(eq(fixture.console), command.capture());
        double expected = command.getValue().contains("reset") ? 1.0 : 1.2;
        assertEquals(expected, em.getMultiplier(),
                "EM listener multiplier must match the dispatched mcMMO command leg");
        verify(fixture.logger).info(contains("EliteMobs detected"));
        verify(fixture.logger).info(contains("Verified EliteMobs skill XP event multiplier"));
    }

    @Test
    void eliteMobsPresentButDisabledRunsTwoLegsOnly() {
        // A present-but-disabled EliteMobs must not register the EM listener
        // (its event class may not be loadable) and must not fail the enable.
        Fixture fixture = new Fixture();
        Plugin eliteMobs = mock(Plugin.class);
        when(eliteMobs.isEnabled()).thenReturn(false);
        when(fixture.plugins.getPlugin("EliteMobs")).thenReturn(eliteMobs);
        fixture.plugin.onEnable();
        verify(fixture.plugins, times(1)).registerEvents(any(Listener.class), eq(fixture.plugin));
        verify(fixture.logger).info(contains("EliteMobs not present or not enabled"));
        verify(fixture.logger).info(contains("verified vanilla XP event multiplier"));
    }

    @Test
    void throwingCommandStillSchedulesRetryAndThenApplies() {
        Fixture fixture = new Fixture();
        when(fixture.server.dispatchCommand(eq(fixture.console), anyString()))
                .thenThrow(new IllegalStateException("command failed")).thenReturn(true);
        fixture.plugin.onEnable();
        assertEquals(1.0, fixture.registeredListener().getMultiplier(),
                "A failed command must not set the vanilla multiplier");
        verify(fixture.logger).warning(contains("command failed"));
        fixture.scheduledCheck().run();
        ArgumentCaptor<String> command = ArgumentCaptor.forClass(String.class);
        verify(fixture.server, times(2)).dispatchCommand(eq(fixture.console), command.capture());
        double expected = command.getValue().contains("reset") ? 1.0 : 1.2;
        assertEquals(expected, fixture.registeredListener().getMultiplier());
        verify(fixture.logger).info(contains("verified vanilla XP event multiplier"));
    }
}
