// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.platform.Platform;
import com.winkreyes.excelmenu.core.registry.EntryDef;
import com.winkreyes.excelmenu.core.registry.EntryResolver;
import com.winkreyes.excelmenu.core.registry.OrderMerge;
import com.winkreyes.excelmenu.core.registry.PlayerMenuPrefs;
import com.winkreyes.excelmenu.core.render.LayoutDef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shared editor data plumbing (ExcelMenu 3.0, DESIGN §3.5): the
 * editor edits the stored + default ID UNIVERSE — every registry
 * entry of the group for the player's platform, regardless of
 * guards or date windows — so a player can order a door that is
 * temporarily unavailable. Rendering merge (§3.3) intersects that
 * universe with the momentary resolved slice.
 */
final class EditorSupport {

    private EditorSupport() {}

    static final String MENU = PlayerMenuPrefs.MENU;

    /** Group keys in layout (picker) order, plus any group present
     *  in the registry but missing from the layout, appended. */
    static List<String> groups(EntryService entries, LayoutDef layout) {
        List<String> out = new ArrayList<>(layout.groups().keySet());
        for (EntryDef d : entries.registry().entries().values()) {
            if (MENU.equals(d.menu()) && !out.contains(d.group())) out.add(d.group());
        }
        return out;
    }

    static String groupName(LayoutDef layout, String group) {
        return layout.groups().getOrDefault(group, group);
    }

    /** The group's entry ids in DEFAULT order for a platform:
     *  Bedrock ranks by bedrockOrder, Java by order (DESIGN §3.2's
     *  single-list model rests on the live ranks agreeing; the
     *  editor's per-platform default is each platform's own rank,
     *  and the stored list is shared). */
    static List<String> defaultGroupIds(EntryService entries, String group, Platform platform) {
        List<EntryDef> defs = new ArrayList<>();
        for (EntryDef d : entries.registry().entries().values()) {
            if (MENU.equals(d.menu()) && group.equals(d.group())
                    && d.platforms().contains(platform)) {
                defs.add(d);
            }
        }
        if (platform == Platform.BEDROCK) {
            defs.sort(Comparator.comparingInt(EntryDef::bedrockOrder).thenComparing(EntryDef::id));
        } else {
            defs.sort(Comparator.comparingInt(EntryDef::order).thenComparing(EntryDef::id));
        }
        return defs.stream().map(EntryDef::id).toList();
    }

    /** Movable ids of a group: the default universe minus pins
     *  (pins are locked chrome, DESIGN §2.4). */
    static List<String> movableGroupIds(EntryService entries, LayoutDef layout,
                                        String group, Platform platform) {
        List<String> out = new ArrayList<>();
        for (String id : defaultGroupIds(entries, group, platform)) {
            if (!layout.pinned().contains(id)) out.add(id);
        }
        return out;
    }

    /** The group's ids in the player's EFFECTIVE order (stored
     *  merged over default; pins excluded — they are not movable). */
    static List<String> effectiveGroupIds(EntryService entries, LayoutDef layout,
                                          PlayerMenuPrefs prefs, String group,
                                          Platform platform) {
        List<String> defaults = movableGroupIds(entries, layout, group, platform);
        if (prefs == null || !prefs.hasGroup(group)) return defaults;
        return OrderMerge.mergeIds(defaults, withoutPins(prefs.groupList(group), layout));
    }

    static List<String> withoutPins(List<String> ids, LayoutDef layout) {
        List<String> out = new ArrayList<>();
        for (String id : ids) if (!layout.pinned().contains(id)) out.add(id);
        return out;
    }

    /** Effective picker group order for this player. */
    static List<String> effectiveGroupOrder(EntryService entries, LayoutDef layout,
                                            PlayerMenuPrefs prefs) {
        return OrderMerge.effectiveGroupOrder(groups(entries, layout), prefs);
    }

    static String label(EntryService entries, String id) {
        EntryDef d = entries.registry().entry(id);
        return d == null ? id : EntryResolver.stripLegacy(d.label());
    }

    static EntryDef def(EntryService entries, String id) {
        return entries.registry().entry(id);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
