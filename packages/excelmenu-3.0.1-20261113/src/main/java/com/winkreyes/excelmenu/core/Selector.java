// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Decides which item occupies each slot for one viewer: among the
 * items defined for a slot, the viewer sees the one whose view
 * requirements all pass with the LOWEST priority number (DeluxeMenus
 * semantics: priority 1 is highest; an item without a priority sorts
 * last). Both renderers consume this same map, which is what makes
 * Java = Bedrock a property of the code rather than a promise.
 */
public final class Selector {

    private Selector() {}

    public static Map<Integer, MenuDef.ItemDef> visibleItems(
            MenuDef def, RequirementEngine.Context ctx) {
        Map<Integer, List<MenuDef.ItemDef>> bySlot = new TreeMap<>();
        for (MenuDef.ItemDef it : def.items) {
            bySlot.computeIfAbsent(it.slot(), k -> new ArrayList<>()).add(it);
        }
        Map<Integer, MenuDef.ItemDef> out = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<MenuDef.ItemDef>> e : bySlot.entrySet()) {
            MenuDef.ItemDef best = null;
            for (MenuDef.ItemDef it : e.getValue()) {
                if (!RequirementEngine.allPass(it.viewRequirements(), ctx)) continue;
                if (best == null || priority(it) < priority(best)) best = it;
            }
            if (best != null) out.put(e.getKey(), best);
        }
        return out;
    }

    private static int priority(MenuDef.ItemDef it) {
        return it.priority() == null ? Integer.MAX_VALUE : it.priority();
    }

    /** Which click list runs for a click kind: the specific list if
     *  the item defines one, else the item's "any" list, else null. */
    public static List<String> clickLines(MenuDef.ItemDef item, String kind) {
        if (item == null || item.clicks() == null) return null;
        List<String> lines = item.clicks().get(kind);
        if (lines == null) lines = item.clicks().get("any");
        return lines;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
