package com.winkreyes.excelprotect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

/**
 * ExcelProtect 0.1.12 (P-PROTECT-EM-1 fix; 0.1.11 was Option A) — the Bukkit
 * glue for block attempts: MONITOR listeners WITHOUT ignoreCancelled that
 * record ONLY attempts already stopped at observation time, ONLY in
 * EM-scope worlds (Settings.attemptInScope: the em_ prefix family plus the
 * explicit config list). It reads player/block/position only — it never
 * calls setCancelled/setBuildable, never modifies any event, never touches
 * world state. Every handler body is fail-soft (Throwable guard, the
 * EliteProbe/AuditWatch house pattern): a bug here must never break a
 * player's block interaction. Closed attempts go to the Ledger's async
 * writer on the separate `attempts` stream — the event path does no IO.
 *
 * 0.1.12 — WHY THE TWO NEW HANDLERS (DIAGNOSIS, board-r432): 0.1.11 listened
 * only at the END of the break/place flows (BlockBreakEvent/BlockPlaceEvent).
 * EliteMobs 10.9.14 InstanceProtector stops both flows at their START —
 * cancelling BlockDamageEvent at LOWEST for breaks and setting
 * BlockCanBuildEvent buildable=false at LOWEST for places — so in a
 * protected EM world the terminal events are never fired at all and the
 * 0.1.11 recorder was structurally blind there. The fix observes EM's two
 * real interception points and keeps the terminal handlers as backstops:
 *
 *   onDamage   BlockDamageEvent, cancelled          -> break, via "damage"
 *   onCanBuild BlockCanBuildEvent, !isBuildable()   -> place, via "can_build"
 *              (attempted type = event.getMaterial(), NEVER the target
 *              cell's current type, which is still air/replaceable)
 *   onBreak    BlockBreakEvent, cancelled           -> break, via "break"   (backstop, unchanged)
 *   onPlace    BlockPlaceEvent, cancelled           -> place, via "place"   (backstop, unchanged)
 *
 * NO-DOUBLE-COUNT INVARIANT (DIAGNOSIS §5.1.2): one physical attempt
 * produces at most one record. If BlockDamageEvent is cancelled the dig
 * aborts and BlockBreakEvent never fires; if BlockCanBuildEvent denies,
 * BlockPlaceEvent never fires — the backstops can only fire for attempts
 * the early events did NOT stop, which is disjoint from the new handlers'
 * cases. As a second line of defence the coalescer key excludes `via`
 * (see BlockAttempts), so even a synthetic flow that fired both stages
 * would merge into ONE record, never two.
 * Excel — Wink Reyes Valley
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

    // ---- record conditions (pure; the handlers below delegate to these so
    // ---- the unit suite pins the exact conditions the glue enforces) ----

    /** Cancelled damage (EM's break interception) records, when on + in scope. */
    public static boolean recordsDamage(boolean enabled, boolean cancelled, boolean inScope) {
        return enabled && cancelled && inScope;
    }

    /** Denied can-build (EM's place interception) records, when on + in scope + a player is attempting. */
    public static boolean recordsCanBuild(boolean enabled, boolean buildable, boolean hasPlayer, boolean inScope) {
        return enabled && !buildable && hasPlayer && inScope;
    }

    /** Terminal backstops (break/place) record cancelled events only, when on + in scope. */
    public static boolean recordsTerminal(boolean enabled, boolean cancelled, boolean inScope) {
        return enabled && cancelled && inScope;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onDamage(BlockDamageEvent e) {
        try {
            Block block = e.getBlock();
            if (!recordsDamage(cfg.attemptsEnabled(), e.isCancelled(), inScope(block))) return;
            feed(e.getPlayer(), block, "break", "damage", null);
        } catch (Throwable ignored) { }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onCanBuild(BlockCanBuildEvent e) {
        try {
            Block block = e.getBlock();
            Player player = e.getPlayer();
            if (!recordsCanBuild(cfg.attemptsEnabled(), e.isBuildable(), player != null, inScope(block))) return;
            Material attempted = e.getMaterial();
            if (attempted == null) return;
            // Attempted placed type comes from the EVENT (getMaterial());
            // the target cell's own type is air/replaceable and must not be used.
            feed(player, block, "place", "can_build", attempted.getKey().toString());
        } catch (Throwable ignored) { }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent e) {
        try {
            if (!recordsTerminal(cfg.attemptsEnabled(), e.isCancelled(), inScope(e.getBlock()))) return;
            feed(e.getPlayer(), e.getBlock(), "break", "break", null);
        } catch (Throwable ignored) { }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlace(BlockPlaceEvent e) {
        try {
            if (!recordsTerminal(cfg.attemptsEnabled(), e.isCancelled(), inScope(e.getBlock()))) return;
            // Q5/D72: block = the attempted placed type, event.getBlock().
            feed(e.getPlayer(), e.getBlock(), "place", "place", null);
        } catch (Throwable ignored) { }
    }

    private boolean inScope(Block block) {
        if (block == null) return false;
        String world = block.getWorld() == null ? "" : block.getWorld().getName();
        return cfg.attemptInScope(world);
    }

    /**
     * Feed one stopped attempt. blockType == null resolves the type from the
     * block itself (damage/break/place paths); onCanBuild passes the event's
     * attempted material explicitly because the target cell is not yet that type.
     */
    private void feed(Player player, Block block, String action, String via, String blockType) {
        if (player == null || block == null) return;
        String world = block.getWorld() == null ? "" : block.getWorld().getName();
        if (!cfg.attemptInScope(world)) return;
        String type = blockType != null ? blockType : block.getType().getKey().toString();
        BlockAttempts.Attempt a = new BlockAttempts.Attempt(
                player.getUniqueId(), player.getName(), world,
                block.getX(), block.getY(), block.getZ(),
                action, type, via,
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
