// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.registry;

import com.winkreyes.excelmenu.core.platform.Platform;

/**
 * Everything the resolver is allowed to know about one viewer,
 * supplied by the Bukkit layer (or by a test). Deliberately minimal
 * and platform-free: the resolver never touches Bukkit, PlaceholderAPI
 * or any plugin directly — those are replaceable adapters behind this
 * seam (house build style).
 *
 * Placeholder contract: {@link #resolvePlaceholder} returns the value
 * the expansion produced for this viewer, or null when the token could
 * not be resolved at all (expansion absent, or the text came back with
 * the token still in it). An unresolved token is an UNKNOWN state,
 * never a guessed one.
 */
public interface ViewerContext {

    /** The renderer this viewer will be served by. */
    Platform platform();

    boolean hasPermission(String permission);

    /** True when the named plugin is installed AND enabled. */
    boolean pluginPresent(String pluginName);

    /**
     * Resolve ONE %placeholder% token (e.g. "%aimassist_status%") for
     * this viewer. Returns the resolved value, or null when
     * unresolvable.
     */
    String resolvePlaceholder(String token);
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
