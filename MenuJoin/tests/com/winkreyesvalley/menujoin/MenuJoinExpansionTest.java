package com.winkreyesvalley.menujoin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

class MenuJoinExpansionTest {
    @Test
    void versionMatchesLoadedPluginMetadata() {
        MenuJoin plugin = mock(MenuJoin.class);
        when(plugin.getDescription()).thenReturn(new PluginDescriptionFile("ExcelMenuJoin", "1.2.1", MenuJoin.class.getName()));
        assertEquals("1.2.1", new MenuJoinExpansion(plugin).getVersion());
    }

    @Test
    void statusPreservesPlaceholderCompatibility() {
        MenuJoin plugin = mock(MenuJoin.class);
        Player player = mock(Player.class);
        MenuJoinExpansion expansion = new MenuJoinExpansion(plugin);
        assertEquals("menujoin", expansion.getIdentifier());
        assertTrue(expansion.persist());
        assertEquals("§aON", expansion.onPlaceholderRequest(player, "status"));
        when(plugin.isDisabled(player)).thenReturn(true);
        assertEquals("§cOFF", expansion.onPlaceholderRequest(player, "STATUS"));
        assertEquals("", expansion.onPlaceholderRequest(null, "status"));
        assertNull(expansion.onPlaceholderRequest(player, "unknown"));
    }
}
