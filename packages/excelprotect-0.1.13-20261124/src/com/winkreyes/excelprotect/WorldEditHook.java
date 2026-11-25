package com.winkreyes.excelprotect;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * ExcelProtect — WorldEdit logging (0.1.6, SPEC §5 parity gap closed):
 * WorldEdit's writes bypass Bukkit block events, so without this hook the
 * biggest world changes on the server were the ones Protect never saw.
 * The hook subscribes to WorldEdit's EditSessionEvent and wraps the
 * session extent (the standard extent-delegate pattern): every block the
 * session actually changes is recorded into the blocks stream as a
 * block_change with before/after Bukkit blockstate strings (the exact
 * shape Blocks.java writes, so rollback/preview/undo treat WorldEdit
 * edits like any other change) and the session actor attributed
 * (player name + uuid for player actors, actor name for console).
 *
 * FAIL-SOFT by construction: this class is only loaded when the
 * WorldEdit plugin is present and enabled and log.blocks.worldedit is
 * on; registration failure leaves Protect running without the hook.
 * Compiled against the WorldEdit 7.2.x API surface (EditSessionEvent,
 * extent delegate, BukkitAdapter — stable through 7.4.x).
 * Iris / Mike — Wink Reyes Valley
 */
public final class WorldEditHook {
    private final Ledger ledger;

    public WorldEditHook(Ledger ledger) { this.ledger = ledger; }

    public void register() {
        WorldEdit.getInstance().getEventBus().register(this);
    }

    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        try {
            if (event.getStage() != EditSession.Stage.BEFORE_CHANGE) return;
            Actor actor = event.getActor();
            org.bukkit.entity.Player bp = actor instanceof BukkitPlayer bpl ? bpl.getPlayer() : null;
            String worldName = event.getWorld() != null ? event.getWorld().getName()
                    : bp != null ? bp.getWorld().getName() : null;
            if (worldName == null) return;
            String actorName = bp != null ? bp.getName() : actor != null ? actor.getName() : null;
            UUID actorUuid = bp != null ? bp.getUniqueId() : null;
            event.setExtent(new RecordingExtent(event.getExtent(), worldName, actorName, actorUuid));
        } catch (Throwable ignored) { }
    }

    /** Build one blocks-stream line for a WorldEdit change. Pure — unit-tested. */
    static Json.Obj changeObj(String before, String after, String actorName, UUID actorUuid) {
        Json.Obj o = new Json.Obj().put("type", "block_change")
                .put("action", "worldedit")
                .put("block", blockId(after))
                .put("before", before).put("after", after)
                .put("cause", "worldedit");
        if (actorUuid != null) o.put("player", actorName).put("player_uuid", actorUuid.toString());
        else if (actorName != null) o.put("actor", actorName);
        return o;
    }

    private static String blockId(String blockData) {
        if (blockData == null) return null;
        int i = blockData.indexOf('[');
        return i < 0 ? blockData : blockData.substring(0, i);
    }

    private void record(String worldName, BlockVector3 pos, String before, String after,
                        String actorName, UUID actorUuid) {
        World w = Bukkit.getWorld(worldName);
        if (w == null) return;
        ledger.emit("blocks", new Location(w, pos.getX(), pos.getY(), pos.getZ()),
                changeObj(before, after, actorName, actorUuid));
    }

    private final class RecordingExtent extends AbstractDelegateExtent {
        private final String worldName, actorName;
        private final UUID actorUuid;

        RecordingExtent(Extent extent, String worldName, String actorName, UUID actorUuid) {
            super(extent);
            this.worldName = worldName;
            this.actorName = actorName;
            this.actorUuid = actorUuid;
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 pos, T block)
                throws com.sk89q.worldedit.WorldEditException {
            String before = null;
            try { before = BukkitAdapter.adapt(getExtent().getBlock(pos)).getAsString(); }
            catch (Throwable ignored) { }
            boolean ok = super.setBlock(pos, block);
            if (ok && before != null) {
                try {
                    String after = BukkitAdapter.adapt(block.toImmutableState()).getAsString();
                    if (!after.equals(before)) record(worldName, pos, before, after, actorName, actorUuid);
                } catch (Throwable ignored) { }
            }
            return ok;
        }
    }
}
