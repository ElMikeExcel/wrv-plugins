// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.registry;

import com.winkreyes.excelmenu.core.platform.Platform;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One registry entry: a single navigation door, as DATA (DESIGN-2.0
 * §2, DESIGN.md §2.3). One file per entry under
 * {@code plugins/ExcelMenu/entries/<id>.yml}; plugins may also
 * register entries through the API for exactly their own lifetime.
 *
 * The schema (closed key set — the loader rejects anything else):
 * <pre>
 * id: halloween_scares            # = filename; unique across files + API
 * menu: server_menu               # which navigation menu draws it
 * group: help                     # row/group inside that menu
 * order: 15                       # weight within the group (Java packing)
 * order-bedrock: 185              # optional FLAT weight for the Bedrock
 *                                 # list (global, not per-group: the
 *                                 # 1.5.27 BUTTONS order interleaves
 *                                 # groups, so only a flat weight can
 *                                 # reproduce it exactly)
 * icon: JACK_O_LANTERN            # Bukkit material name
 * label: '&amp;6&amp;lHalloween Scares'
 * lore: [ ... ]                   # Java-side; Bedrock forms use the label
 * action: '[player] scares'       # string or list; the bracket vocabulary
 * platforms: [java, bedrock]      # default both
 * permission: ~                   # sugar alias of requires-permission
 * requires-plugin: ~              # Amendment A guard
 * requires-permission: ~          # Amendment A guard
 * status-placeholder: '%x%'       # Amendment A guard = states source
 * states: { ON: {...}, OFF: {...}, default: {...} }
 * visible-from: 2026-10-31        # optional; America/Chicago, inclusive
 * visible-until: 2026-11-05       # optional; last day rendered
 * </pre>
 *
 * A {@link Variant} overrides icon/label/lore/action for one state
 * value; a null component inherits the base entry's. The reserved
 * variant key {@code default} is the presentation used when a guard
 * fails (Amendment A) — see {@link EntryResolver}.
 */
public record EntryDef(
        String id,
        String menu,
        String group,
        int order,
        Integer orderBedrock,
        String icon,
        String label,
        List<String> lore,
        List<String> action,
        Set<Platform> platforms,
        String requiresPlugin,
        String requiresPermission,
        String statusPlaceholder,
        Map<String, Variant> states,
        LocalDate visibleFrom,
        LocalDate visibleUntil,
        Origin origin) {

    public enum Origin { FILE, API }

    /** One state variant of an entry. Null components inherit the base. */
    public record Variant(String icon, String label, List<String> lore, List<String> action) {}

    public static final String DEFAULT_VARIANT = "default";

    public EntryDef {
        lore = lore == null ? List.of() : List.copyOf(lore);
        action = action == null ? List.of() : List.copyOf(action);
        platforms = platforms == null || platforms.isEmpty()
                ? Set.of(Platform.JAVA, Platform.BEDROCK) : Set.copyOf(platforms);
        states = states == null ? Map.of() : Map.copyOf(states);
    }

    /** True when this entry carries state variants (incl. a default). */
    public boolean stateful() {
        return statusPlaceholder != null && !states.isEmpty();
    }

    /** The variant for a resolved state value, or null when unlisted. */
    public Variant variant(String stateValue) {
        return stateValue == null ? null : states.get(stateValue);
    }

    public Variant defaultVariant() {
        return states.get(DEFAULT_VARIANT);
    }

    /** Effective Bedrock flat weight: the override when present, else order. */
    public int bedrockOrder() {
        return orderBedrock != null ? orderBedrock : order;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
