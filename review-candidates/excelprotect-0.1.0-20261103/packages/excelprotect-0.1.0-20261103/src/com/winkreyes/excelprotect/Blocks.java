package com.winkreyes.excelprotect;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExcelProtect — blocks family (CoreProtect parity): player place/break with before/after
 * blockstate strings and SIGN TEXT, natural break/decay, burn, explosions (one line per
 * block under one event id), form/fade/grow, liquid flow, pistons — per sub-toggles.
 * Also owns the bounded inspect index (latest block event per position).
 * Excel — Wink Reyes Valley
 */
public final class Blocks implements Listener {
    private final Ledger ledger;
    private final Settings cfg;
    private volatile Watch watch;
    private final Map<String, String> inspectIndex = new ConcurrentHashMap<>();

    public Blocks(Ledger ledger, Settings cfg) { this.ledger = ledger; this.cfg = cfg; }
    public void setWatch(Watch w) { this.watch = w; }

    private static String key(Location l) {
        return l.getWorld().getName() + "@" + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    static String[] signLines(org.bukkit.block.BlockState st) {
        if (st instanceof Sign s) {
            try { return s.getSide(Side.FRONT).getLines(); } catch (Throwable t) { return null; }
        }
        return null;
    }

    private void change(Block b, String action, Player actor, String before, String after,
                        String[] signBefore, String cause, String eventId) {
        Json.Obj o = new Json.Obj().put("type", "block_change")
                .put("action", action)
                .put("block", b.getType().getKey().toString())
                .put("before", before).put("after", after);
        if (actor != null) o.put("player", actor.getName()).put("player_uuid", actor.getUniqueId().toString());
        if (cause != null) o.put("cause", cause);
        if (eventId != null) o.put("event_id", eventId);
        if (signBefore != null) {
            Json.Arr a = new Json.Arr();
            for (String line : signBefore) a.add(line);
            o.put("sign_before", a);
        }
        ledger.emit("blocks", b.getLocation(), o);
        String summary = action + " " + b.getType().getKey()
                + (actor != null ? " by " + actor.getName() : cause != null ? " (" + cause + ")" : "")
                + " before=" + before;
        inspectIndex.put(key(b.getLocation()), summary);
        if (inspectIndex.size() > cfg.inspectIndexCap()) inspectIndex.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!cfg.blocksPlayer()) return;
        Block b = e.getBlock();
        change(b, "place", e.getPlayer(), e.getBlockReplacedState().getBlockData().getAsString(),
                b.getBlockData().getAsString(), signLines(e.getBlockReplacedState()), null, null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (!cfg.blocksPlayer()) return;
        Block b = e.getBlock();
        change(b, "break", e.getPlayer(), b.getBlockData().getAsString(), "minecraft:air",
                signLines(b.getState()), null, null);
        Watch w = watch;
        if (w != null) w.blockBreak(e.getPlayer(), b.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSign(SignChangeEvent e) {
        if (!cfg.blocksPlayer()) return;
        Block b = e.getBlock();
        Json.Arr a = new Json.Arr();
        for (String line : e.getLines()) a.add(line == null ? "" : line);
        ledger.emit("blocks", b.getLocation(), new Json.Obj().put("type", "block_change")
                .put("action", "sign_edit").put("block", b.getType().getKey().toString())
                .put("player", e.getPlayer().getName())
                .put("player_uuid", e.getPlayer().getUniqueId().toString())
                .put("sign_after", a));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (!cfg.blocksBurn()) return;
        Block b = e.getBlock();
        change(b, "burn", null, b.getBlockData().getAsString(), "minecraft:air",
                signLines(b.getState()), "burn", null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent e) {
        if (!cfg.blocksNatural()) return;
        Block b = e.getBlock();
        change(b, "break", null, b.getBlockData().getAsString(), "minecraft:air", null, "decay", null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFade(BlockFadeEvent e) {
        if (!cfg.blocksFormFadeGrow()) return;
        change(e.getBlock(), "fade", null, e.getBlock().getBlockData().getAsString(),
                e.getNewState().getBlockData().getAsString(), null, "fade", null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onForm(BlockFormEvent e) {
        if (!cfg.blocksFormFadeGrow()) return;
        change(e.getBlock(), "form", null, e.getBlock().getBlockData().getAsString(),
                e.getNewState().getBlockData().getAsString(), null, "form", null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        if (!cfg.blocksFormFadeGrow()) return;
        change(e.getBlock(), "form", null, e.getBlock().getBlockData().getAsString(),
                e.getNewState().getBlockData().getAsString(), null, "spread", null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGrow(BlockGrowEvent e) {
        if (!cfg.blocksFormFadeGrow()) return;
        change(e.getBlock(), "grow", null, e.getBlock().getBlockData().getAsString(),
                e.getNewState().getBlockData().getAsString(), null, "grow", null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLiquid(BlockFromToEvent e) {
        if (!cfg.blocksLiquid()) return;
        change(e.getToBlock(), "flow", null, e.getToBlock().getBlockData().getAsString(),
                e.getBlock().getBlockData().getAsString(), null, "liquid", null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) { piston(e.getBlocks(), "extend"); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) { piston(e.getBlocks(), "retract"); }

    private void piston(List<Block> blocks, String dir) {
        if (!cfg.blocksPistons()) return;
        String id = UUID.randomUUID().toString();
        for (Block b : blocks)
            change(b, "piston_" + dir, null, b.getBlockData().getAsString(),
                    b.getBlockData().getAsString(), null, "piston", id);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (!cfg.blocksExplosions()) return;
        explode(e.blockList(), "block_explosion");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        if (!cfg.blocksExplosions()) return;
        explode(e.blockList(), e.getEntityType().getKey().toString());
    }

    private void explode(List<Block> blocks, String cause) {
        String id = UUID.randomUUID().toString();
        for (Block b : blocks)
            change(b, "break", null, b.getBlockData().getAsString(), "minecraft:air",
                    signLines(b.getState()), cause, id);
    }

    /** Latest-event summary for /excelprotect inspect, or null. */
    public String inspect(Location l) { return inspectIndex.get(key(l)); }
}
