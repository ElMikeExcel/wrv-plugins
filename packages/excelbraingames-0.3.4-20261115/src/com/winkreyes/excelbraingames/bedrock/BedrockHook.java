package com.winkreyes.excelbraingames.bedrock;

import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * The ONLY class in the plugin that touches Floodgate (0.3.2). It is
 * loaded only when the floodgate plugin is present AND enabled (the
 * main class checks first, in a guarded bind); plugin.yml declares
 * {@code softdepend: [floodgate]} so load order is right when it
 * exists, and the plugin runs unchanged when it does not.
 *
 * <p>Used for the platform decisions keyed off core/WsGeometry: which
 * Word Search geometry a NEW round is dealt in (0.3.2), and whether
 * Sudoku presents its box view to this viewer (0.3.3). A player whose
 * platform cannot be determined gets the Java side of both — the safe
 * default.
 */
public final class BedrockHook {

    /** True when Floodgate knows this player (they joined through Geyser). */
    public boolean isBedrock(Player p) {
        try {
            return FloodgateApi.getInstance().getPlayer(p.getUniqueId()) != null;
        } catch (Throwable t) {
            return false;
        }
    }
}
