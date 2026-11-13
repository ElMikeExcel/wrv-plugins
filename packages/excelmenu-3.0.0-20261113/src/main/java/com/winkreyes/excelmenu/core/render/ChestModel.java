// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.render;

import com.winkreyes.excelmenu.core.registry.ResolvedEntry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Java chest renderer's brain — pure, Bukkit-free, unit-tested.
 * It consumes the resolver's output (already platform/date/guard/
 * state-resolved and ordered) plus the menu's layout, and produces
 * the exact slot map the Bukkit layer paints into an inventory.
 *
 * Contract:
 * <ul>
 *   <li>Entries fill their group's slot list IN RESOLVED ORDER.</li>
 *   <li>Fixed items (headers, pagination) paint at their slots.</li>
 *   <li>Overflow is an ERROR, never a silent drop: a door that does
 *       not fit must be loud (ExcelMenu's founding rule), so the
 *       build returns the error and the caller refuses the open.</li>
 *   <li>An entry whose group has no slots on the requested page is
 *       simply not on this page (the caller opens page 1; groups are
 *       whole on one page by layout law).</li>
 * </ul>
 */
public final class ChestModel {

    /** What sits at one slot: an entry door, or a piece of chrome. */
    public sealed interface Placed {
        record EntryItem(ResolvedEntry entry) implements Placed {}
        record FixedItem(LayoutDef.FixedItem fixed) implements Placed {}
    }

    public record Page(String title, int size, Map<Integer, Placed> slots) {
        public Page {
            slots = Map.copyOf(slots);
        }
    }

    public record Result(Page page, String error) {
        public boolean ok() { return page != null && error == null; }
    }

    private ChestModel() {}

    /** Build one chest page (0-based) for one viewer's resolved set. */
    public static Result build(LayoutDef layout, List<ResolvedEntry> resolved,
                               int pageIndex) {
        if (pageIndex < 0 || pageIndex >= layout.pages().size()) {
            return new Result(null, "page " + (pageIndex + 1) + " does not exist in layout '"
                    + layout.id() + "' (" + layout.pages().size() + " page(s))");
        }
        LayoutDef.Page page = layout.pages().get(pageIndex);
        Map<Integer, Placed> slots = new LinkedHashMap<>();
        for (LayoutDef.FixedItem f : page.fixed()) {
            slots.put(f.slot(), new Placed.FixedItem(f));
        }
        // Entries grouped in resolved order, filled into slot lists.
        Map<String, Integer> cursors = new LinkedHashMap<>();
        for (ResolvedEntry r : resolved) {
            String group = r.entry().group();
            List<Integer> groupSlots = page.slots().get(group);
            if (groupSlots == null) continue; // another page's group
            int cursor = cursors.getOrDefault(group, 0);
            if (cursor >= groupSlots.size()) {
                return new Result(null, "layout '" + layout.id() + "' page "
                        + (pageIndex + 1) + ": group '" + group + "' has "
                        + groupSlots.size() + " slot(s) but entry '" + r.id()
                        + "' (and possibly more) resolved — add slots to the"
                        + " layout or split the group");
            }
            slots.put(groupSlots.get(cursor), new Placed.EntryItem(r));
            cursors.put(group, cursor + 1);
        }
        return new Result(new Page(layout.pageTitle(pageIndex), page.size(), slots), null);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
