package com.winkreyes.excelbraingames;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Keeps the name index (used by console admin lookups) current. */
public final class JoinListener implements Listener {

    private final ExcelBrainGames plugin;

    public JoinListener(ExcelBrainGames plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.dataStore().noteName(e.getPlayer().getUniqueId(), e.getPlayer().getName());
    }
}
