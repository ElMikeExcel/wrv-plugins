package com.winkreyes.excelwandbars;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * 0.3.11: the quit close's early hook point. Paper fires
 * PlayerConnectionCloseEvent synchronously from the connection teardown,
 * BEFORE PlayerQuitEvent is dispatched — and therefore before Magic's own
 * quit handling reads the wand's open state for its next-join restore
 * decision (Magic's handler runs first among quit handlers and, at its
 * default logout_delay of 0, deactivates the mage synchronously, after
 * which no quit handler can reach the wand). At connection close the
 * player object still resolves and the mage is still active, so the close
 * lands in time on every logout.
 *
 * Kept in its own class and registered in its own try/catch (the house
 * isolation pattern for platform-optional hooks): on a server without
 * this event the feature degrades to the TapBarHook quit-handler belt
 * plus the join guard, never to a failed enable.
 */
final class QuitCloseHook implements Listener {
    private final TapBarHook tapBar;

    QuitCloseHook(TapBarHook tapBar) { this.tapBar = tapBar; }

    @EventHandler
    public void onConnectionClose(PlayerConnectionCloseEvent event) {
        Player p = Bukkit.getPlayer(event.getPlayerUniqueId());
        if (p != null) tapBar.closeBarAtExit(p, "connection close");
    }
}
