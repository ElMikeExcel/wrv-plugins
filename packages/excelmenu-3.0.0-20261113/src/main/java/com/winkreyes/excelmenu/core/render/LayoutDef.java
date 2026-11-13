// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.render;

import java.util.List;
import java.util.Map;

/**
 * One registry menu's CHROME — everything about a menu that is not an
 * entry: its title, chest size(s), header panes, pagination buttons,
 * the exact slots each group's entries fill (in resolver order), and
 * how its Bedrock form is shaped. Entries live in
 * {@code entries/<id>.yml}; layouts live in
 * {@code layouts/<menu>.yml} (DESIGN-2.0 §4 Phase 1: "a slimmed
 * layout definition + entry files").
 *
 * Splitting chrome from doors is what makes the common edits cheap:
 * moving a door is an {@code order:} edit in its entry file; moving a
 * whole row is a slot-list edit here; neither touches code.
 *
 * @param form FLAT — one form, every entry a button (the compass
 *             form). GROUPED — a group picker form, then one form per
 *             group whose body lists the group's rows (the commands
 *             catalogue's proven Bedrock shape, 1.5.27).
 * @param groups group key -> display name, in picker order (GROUPED
 *             form only; may be empty for FLAT).
 * @param pinned entry ids rendered as one-tap root pins on the
 *             Bedrock picker, in declaration order (3.0, DESIGN §2);
 *             empty for layouts without the {@code pinned:} key.
 * @param pages chest pages, 1-based by list position. Each group
 *             appears in exactly one page's slot map.
 */
public record LayoutDef(
        String id,
        String title,
        Form form,
        String formTitle,
        String formContent,
        String formPickerContent,
        String formNote,
        Map<String, String> groups,
        List<Page> pages,
        List<String> pinned) {

    /** Pre-3.0 construction (no pins). */
    public LayoutDef(String id, String title, Form form, String formTitle,
                     String formContent, String formPickerContent, String formNote,
                     Map<String, String> groups, List<Page> pages) {
        this(id, title, form, formTitle, formContent, formPickerContent, formNote,
                groups, pages, List.of());
    }

    public enum Form { FLAT, GROUPED }

    /** One chest page. {@code title} null = the layout title. */
    public record Page(
            int size,
            String title,
            List<FixedItem> fixed,
            Map<String, List<Integer>> slots) {
        public Page {
            fixed = fixed == null ? List.of() : List.copyOf(fixed);
            slots = slots == null ? Map.of()
                    : java.util.Collections.unmodifiableMap(
                            new java.util.LinkedHashMap<>(slots));
        }
    }

    /**
     * A piece of chrome occupying one slot: a header pane (no action),
     * or a pagination button ({@code page} = the 1-based page it
     * turns to), or a fixed action button ({@code action} lines,
     * dispatched like an entry's). Exactly one of page/action may be
     * set; neither = pure display.
     */
    public record FixedItem(
            int slot,
            String material,
            String name,
            List<String> lore,
            Integer page,
            List<String> action) {
        public FixedItem {
            lore = lore == null ? List.of() : List.copyOf(lore);
            action = action == null ? List.of() : List.copyOf(action);
        }
    }

    public LayoutDef {
        // LinkedHashMap, NOT Map.copyOf: the groups map's iteration
        // order IS the picker order, and page slot maps are read in
        // declaration order by the renderers. (Map.copyOf makes no
        // order guarantee — caught by FormModelTest.)
        groups = groups == null ? Map.of()
                : java.util.Collections.unmodifiableMap(
                        new java.util.LinkedHashMap<>(groups));
        pages = pages == null ? List.of() : List.copyOf(pages);
        pinned = pinned == null ? List.of() : List.copyOf(pinned);
    }

    /** The page (0-based index) whose slot map carries a group, or -1. */
    public int pageOfGroup(String group) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).slots().containsKey(group)) return i;
        }
        return -1;
    }

    public String pageTitle(int pageIndex) {
        String t = pages.get(pageIndex).title();
        return t != null ? t : title;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
