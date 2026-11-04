// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.platform.Detection;
import com.winkreyes.excelmenu.core.platform.Platform;
import com.winkreyes.excelmenu.core.registry.ViewerContext;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * The Bukkit adapter behind the core's {@link ViewerContext} seam:
 * platform from the unified bridge, permissions and plugin presence
 * from Bukkit, placeholder values from PlaceholderAPI (via
 * {@link Placeholders}).
 *
 * Platform mapping: the bridge's tri-state collapses to the two
 * renderers here — BEDROCK renders as Bedrock; JAVA and UNKNOWN both
 * render as Java (the chest GUI, Geyser-translated when needed). The
 * UNKNOWN case stays visible in the bridge's own logging; it is never
 * silently re-labelled Java upstream.
 */
final class BukkitViewerContext implements ViewerContext {

    private final Player player;
    private final Detection.State detection;

    BukkitViewerContext(Player player, PlatformBridge bridge) {
        this.player = player;
        this.detection = bridge.detect(player);
    }

    /** The raw tri-state, for diagnostics (/excelmenu entries). */
    Detection.State detection() {
        return detection;
    }

    @Override
    public Platform platform() {
        return detection == Detection.State.BEDROCK ? Platform.BEDROCK : Platform.JAVA;
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public boolean pluginPresent(String pluginName) {
        Plugin p = Bukkit.getPluginManager().getPlugin(pluginName);
        return p != null && p.isEnabled();
    }

    @Override
    public String resolvePlaceholder(String token) {
        String resolved = Placeholders.resolve(player, token);
        // Unresolved = the token came back still wearing itself.
        return resolved == null || resolved.contains(token) ? null : resolved;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
