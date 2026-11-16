package com.winkreyes.excelprotect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

/**
 * ExcelProtect 0.1.11 (P-PROTECT-EM-1, Option A) — the Bukkit glue for
 * block attempts: a MONITOR listener WITHOUT ignoreCancelled over block
 * break/place that records ONLY events already cancelled at observation
 * time, ONLY in EM-scope worlds (Settings.attemptInScope: the em_ prefix
 * family plus the explicit config list). It reads player/block/position
 * only — it never calls setCancelled, never modifies the event, never
 * touches world state. Every handler body is fail-soft (Throwable
 * guard, the EliteProbe/AuditWatch house pattern): a bug here must
 * never break a player's block interaction. Closed attempts go to the
 * Ledger's async writer on the separate `attempts` stream — the event
 * path does no IO. Excel — Wink Reyes Valley
 */
public final class BlockAttemptWatch implements Listener {
    private final Ledger ledger;
    private final Settings cfg;
    private final BlockAttempts attempts;

    public BlockAttemptWatch(Ledger ledger, Settings cfg) {
        this.ledger = ledger;
        this.cfg = cfg;
        this.attempts = new BlockAttempts(cfg.attemptCoalesceMs());
    }

    public BlockAttempts attempts() { return attempts; }
    public int openCount() { return attempts.openCount(); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent e) {
        try {
            if (!cfg.attemptsEnabled()) return;
            if (!e.isCancelled()) return;
            feed(e.getPlayer(), e.getBlock(), "break");
        } catch (Throwable ignored) { }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlace(BlockPlaceEvent e) {
        try {
            if (!cfg.attemptsEnabled()) return;
            if (!e.isCancelled()) return;
            // Q5/D72: block = the attempted placed type, event.getBlock().
            feed(e.getPlayer(), e.getBlock(), "place");
        } catch (Throwable ignored) { }
    }

    private void feed(Player player, Block block, String action) {
        if (player == null || block == null) return;
        String world = block.getWorld() == null ? "" : block.getWorld().getName();
        if (!cfg.attemptInScope(world)) return;
        BlockAttempts.Attempt a = new BlockAttempts.Attempt(
                player.getUniqueId(), player.getName(), world,
                block.getX(), block.getY(), block.getZ(),
                action, block.getType().getKey().toString(),
                System.currentTimeMillis());
        emit(attempts.feed(a));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        try {
            emit(attempts.closePlayer(e.getPlayer().getUniqueId(), "quit"));
        } catch (Throwable ignored) { }
    }

    /** Sweep tick (1 s, main thread, map work only): close gap-expired attempts. */
    public void tick() {
        try {
            emit(attempts.tick(System.currentTimeMillis()));
        } catch (Throwable ignored) { }
    }

    /** Plugin shutdown: close everything so no attempt tail is lost. */
    public void closeAll(String reason) {
        try {
            emit(attempts.closeAll(reason));
        } catch (Throwable ignored) { }
    }

    /** Flush alias (house naming alongside closeAll). */
    public void flush() { closeAll("shutdown"); }

    private void emit(List<BlockAttempts.Record> closed) {
        for (BlockAttempts.Record rec : closed) {
            World w = rec.world == null || rec.world.isEmpty() ? null : Bukkit.getWorld(rec.world);
            Location loc = w == null ? null : new Location(w, rec.x, rec.y, rec.z);
            ledger.emit("attempts", loc, rec.render());
        }
    }
}
