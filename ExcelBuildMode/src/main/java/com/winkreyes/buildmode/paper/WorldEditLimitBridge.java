package com.winkreyes.buildmode.paper;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import org.bukkit.entity.Player;

/**
 * The ONLY class in ExcelBuildMode that touches WorldEdit API types
 * (1.3.2). It is invoked exclusively from paths already gated on the
 * WorldEdit plugin being enabled (the plugin's worldEdit flag), so the
 * WorldEdit classes are never loaded on a server without WorldEdit —
 * the same softdepend discipline as the rest of the plugin, which
 * otherwise integrates with WorldEdit by permission nodes only.
 *
 * The raise targets the player's own LocalSession, obtained through
 * WorldEdit's SessionManager for the Bukkit-adapted player — the exact
 * session object WorldEdit's command layer uses (its createEditSession
 * reads getBlockChangeLimit() into the edit's maxBlocks), so a limit
 * set here is the limit the session's //copy and //paste run under.
 */
public final class WorldEditLimitBridge {
    private WorldEditLimitBridge() { }

    /** Apply a block-change limit to one WorldEdit session. */
    public static void applyTo(LocalSession session, int limit) {
        session.setBlockChangeLimit(limit);
    }

    /**
     * Raise the player's WorldEdit session block-change limit and return
     * the value the session now reports, so the caller can log the value
     * actually in effect rather than the value merely requested.
     */
    public static int raiseLimit(Player player, int limit) {
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
        applyTo(session, limit);
        return session.getBlockChangeLimit();
    }
}
