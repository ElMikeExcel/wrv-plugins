// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.registry;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * The registry's own settings — every value here is a config.yml key
 * in the Bukkit layer (adjustability law: balance/presentation values
 * live in config, never in code).
 *
 * @param zone             the zone entry dates are read in
 *                         (America/Chicago on WRV)
 * @param unavailableMessage the standard plain message a guarded entry
 *                         falls back to (Amendment A), as an action
 *                         line body WITHOUT the [message] tag
 * @param groupOrder       menu id -> ordered group names (the layout's
 *                         row sequence; Java packing follows it)
 * @param groupColors      group name -> legacy color prefix used on
 *                         Bedrock form buttons (BedrockMenu 1.5.21's
 *                         group colouring, now data)
 * @param pinColor         legacy color prefix for the root PIN
 *                         buttons (Spawn + Teleport to a player,
 *                         3.0.1): blue is owned by the pins — the
 *                         pins keep dark blue even though Travel
 *                         itself is green now (Mike, Nov 13, 2026).
 *                         Blank/missing = "&1" (dark blue, the
 *                         pre-3.0.1 rendering, preserved exactly).
 */
public record RegistryConfig(
        ZoneId zone,
        String unavailableMessage,
        Map<String, List<String>> groupOrder,
        Map<String, String> groupColors,
        boolean rearrangeEnabled,
        List<String> rearrangeMenus,
        String pinColor) {

    /** Pre-3.0 construction: rearrange on, server_menu only
     *  (DESIGN §3.7 code defaults). */
    public RegistryConfig(ZoneId zone, String unavailableMessage,
                          Map<String, List<String>> groupOrder,
                          Map<String, String> groupColors) {
        this(zone, unavailableMessage, groupOrder, groupColors, true,
                List.of("server_menu"), "&1");
    }

    public static final ZoneId SERVER_ZONE = ZoneId.of("America/Chicago");
    public static final String DEFAULT_UNAVAILABLE = "&eThat isn't available right now.";

    public RegistryConfig {
        if (zone == null) zone = SERVER_ZONE;
        if (unavailableMessage == null || unavailableMessage.isBlank()) {
            unavailableMessage = DEFAULT_UNAVAILABLE;
        }
        groupOrder = groupOrder == null ? Map.of() : Map.copyOf(groupOrder);
        groupColors = groupColors == null ? Map.of() : Map.copyOf(groupColors);
        pinColor = pinColor == null || pinColor.isBlank() ? "&1" : pinColor;
        rearrangeMenus = rearrangeMenus == null || rearrangeMenus.isEmpty()
                ? List.of("server_menu") : List.copyOf(rearrangeMenus);
    }

    /** True when the rearrange editor + prefs merge serve a menu. */
    public boolean rearrangeServes(String menuId) {
        return rearrangeEnabled && rearrangeMenus.contains(menuId);
    }

    public static RegistryConfig defaults() {
        return new RegistryConfig(SERVER_ZONE, DEFAULT_UNAVAILABLE, Map.of(), Map.of());
    }

    /** Group sequence for a menu; unknown groups sort after, by name. */
    public List<String> groupsFor(String menuId) {
        return groupOrder.getOrDefault(menuId, List.of());
    }

    public String groupColor(String group) {
        return groupColors.getOrDefault(group, "");
    }

    /** The full fallback action line list for a failed guard. */
    public List<String> unavailableAction() {
        return List.of("[message] " + unavailableMessage);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
