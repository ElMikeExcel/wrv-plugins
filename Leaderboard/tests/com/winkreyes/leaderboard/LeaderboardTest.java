package com.winkreyes.leaderboard;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeaderboardTest {
    @Test
    void dispatchesNativeBoardsAsPlayer() {
        Leaderboard plugin = mock(Leaderboard.class, CALLS_REAL_METHODS);
        Player player = mock(Player.class);
        when(player.performCommand(anyString())).thenReturn(true);
        assertTrue(plugin.onCommand(player, mock(Command.class), "leaderboard", new String[0]));
        verify(player).performCommand("baltop");
        verify(player).performCommand("mctop");
        verify(player, never()).sendMessage(contains("unavailable"));
    }

    @Test
    void missingBoardsGetReadableFallbacks() {
        Leaderboard plugin = mock(Leaderboard.class, CALLS_REAL_METHODS);
        Player player = mock(Player.class);
        plugin.onCommand(player, mock(Command.class), "leaderboard", new String[0]);
        verify(player).sendMessage("§7Wealth board unavailable.");
        verify(player).sendMessage("§7mcMMO board unavailable.");
    }

    @Test
    void consoleGetsPlayerOnlyMessage() {
        Leaderboard plugin = mock(Leaderboard.class, CALLS_REAL_METHODS);
        CommandSender console = mock(CommandSender.class);
        assertTrue(plugin.onCommand(console, mock(Command.class), "leaderboard", new String[0]));
        verify(console).sendMessage("Only players can use /leaderboard.");
    }
}
