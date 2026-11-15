// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.api;

import com.winkreyes.excelmenu.core.registry.EntryDef;
import com.winkreyes.excelmenu.core.registry.ResolvedEntry;
import com.winkreyes.excelmenu.core.registry.ViewerContext;

import java.util.List;
import java.util.Map;

/**
 * The tiny registry API (DESIGN.md §2.2): a plugin contributes an
 * entry for exactly its own lifetime — register at enable, unregister
 * at disable — and the entry renders on both platforms from the next
 * menu open. Deliberately minimal: no rendering hooks, no click
 * callbacks beyond the entry's own action strings. A plugin that
 * needs bespoke behaviour keeps its own screens and registers a plain
 * entry that opens them.
 *
 * Registered as a Bukkit service under this interface; also reachable
 * from the ExcelMenu plugin instance.
 */
public interface ExcelMenuApi {

    /**
     * Register an entry. Returns null on success, or the refusal
     * reason (an id collision is refused, never a silent shadow).
     */
    String registerEntry(EntryDef def);

    /** Remove a previously API-registered entry. False when absent. */
    boolean unregisterEntry(String id);

    /** The entry with this id, from files or API, or null. */
    EntryDef entry(String id);

    /** Every registered entry, by id (immutable snapshot). */
    Map<String, EntryDef> entries();

    /**
     * The visible entries for a menu and viewer, resolved NOW
     * (platform, date window, guards, states, ordering) — the exact
     * list a renderer would draw.
     */
    List<ResolvedEntry> entriesFor(String menuId, ViewerContext viewer);
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
