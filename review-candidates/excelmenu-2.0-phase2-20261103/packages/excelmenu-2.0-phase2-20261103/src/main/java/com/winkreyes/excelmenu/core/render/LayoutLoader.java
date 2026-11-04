// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.render;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses one layout file into a {@link LayoutDef}, with the same
 * strict posture as the entry loader: closed key set, duplicate YAML
 * keys refused, wrong types refused, slot collisions refused, a
 * group claimed by two pages refused. A layout that fails is refused
 * LOUDLY by the caller while the rest of the menus still serve — a
 * dead menu must never be a silent one (ExcelMenu's founding rule).
 */
public final class LayoutLoader {

    public record Result(LayoutDef def, List<String> errors) {
        public boolean ok() { return def != null && errors.isEmpty(); }
    }

    private static final Set<String> LAYOUT_KEYS = Set.of(
            "id", "title", "form", "form-title", "form-content",
            "form-picker-content", "form-note", "groups", "pages");
    private static final Set<String> PAGE_KEYS = Set.of(
            "size", "title", "fixed", "slots");
    private static final Set<String> FIXED_KEYS = Set.of(
            "slot", "material", "name", "lore", "page", "action");
    private static final Set<String> ACTION_TAGS = Set.of(
            "console", "player", "commandevent", "message", "openguimenu",
            "close", "refresh", "json", "sound", "broadcastsound", "connect");

    private LayoutLoader() {}

    public static Result load(Path file) {
        String stem = file.getFileName().toString();
        if (stem.endsWith(".yml")) stem = stem.substring(0, stem.length() - 4);
        List<String> errors = new ArrayList<>();
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(file)) {
            LoaderOptions opts = new LoaderOptions();
            opts.setAllowDuplicateKeys(false);
            Object parsed = new Yaml(new SafeConstructor(opts)).load(in);
            if (parsed == null) {
                return new Result(null, List.of(stem + ": empty file"));
            }
            if (!(parsed instanceof Map)) {
                return new Result(null, List.of(stem + ": top level is not a map"));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) parsed;
            root = map;
        } catch (IOException | RuntimeException e) {
            return new Result(null, List.of(stem + ": cannot parse — " + e.getMessage()));
        }

        for (String key : root.keySet()) {
            if (!LAYOUT_KEYS.contains(key)) {
                errors.add(stem + ": unknown key '" + key + "'");
            }
        }
        String id = str(root.get("id"));
        if (id == null || id.isBlank()) {
            errors.add(stem + ": missing 'id'");
        } else if (!id.equals(stem)) {
            errors.add(stem + ": id '" + id + "' does not match the filename");
        }
        String title = str(root.get("title"));
        if (title == null || title.isBlank()) errors.add(stem + ": missing 'title'");

        LayoutDef.Form form = LayoutDef.Form.FLAT;
        String formRaw = str(root.get("form"));
        if (formRaw != null) {
            try {
                form = LayoutDef.Form.valueOf(formRaw.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add(stem + ": form '" + formRaw + "' is not flat|grouped");
            }
        }

        Map<String, String> groups = new LinkedHashMap<>();
        Object groupsRaw = root.get("groups");
        if (groupsRaw != null) {
            if (!(groupsRaw instanceof Map)) {
                errors.add(stem + ": 'groups' is not a map");
            } else {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) groupsRaw).entrySet()) {
                    String key = String.valueOf(e.getKey());
                    Object v = e.getValue();
                    String name = null;
                    if (v instanceof Map) {
                        Object n = ((Map<?, ?>) v).get("name");
                        if (n != null) name = String.valueOf(n);
                    } else if (v != null) {
                        name = String.valueOf(v);
                    }
                    if (name == null || name.isBlank()) {
                        errors.add(stem + ": group '" + key + "' has no name");
                    } else {
                        groups.put(key, name);
                    }
                }
            }
        }

        List<LayoutDef.Page> pages = new ArrayList<>();
        Object pagesRaw = root.get("pages");
        if (!(pagesRaw instanceof List) || ((List<?>) pagesRaw).isEmpty()) {
            errors.add(stem + ": 'pages' must be a non-empty list");
        } else {
            Set<String> claimedGroups = new java.util.HashSet<>();
            int pi = 0;
            for (Object pRaw : (List<?>) pagesRaw) {
                pi++;
                pages.add(parsePage(stem, pi, pRaw, claimedGroups, errors));
            }
        }

        if (!errors.isEmpty()) return new Result(null, errors);
        return new Result(new LayoutDef(id, title, form,
                str(root.get("form-title")), str(root.get("form-content")),
                str(root.get("form-picker-content")), str(root.get("form-note")),
                groups, pages), List.of());
    }

    private static LayoutDef.Page parsePage(String stem, int pageNo, Object raw,
                                            Set<String> claimedGroups,
                                            List<String> errors) {
        String where = stem + " page " + pageNo;
        if (!(raw instanceof Map)) {
            errors.add(where + ": not a map");
            return new LayoutDef.Page(54, null, List.of(), Map.of());
        }
        Map<?, ?> page = (Map<?, ?>) raw;
        for (Object key : page.keySet()) {
            if (!PAGE_KEYS.contains(String.valueOf(key))) {
                errors.add(where + ": unknown key '" + key + "'");
            }
        }
        int size = 54;
        Object sizeRaw = page.get("size");
        if (sizeRaw != null) {
            if (sizeRaw instanceof Number n) {
                size = n.intValue();
                if (size <= 0 || size > 54 || size % 9 != 0) {
                    errors.add(where + ": size " + size
                            + " is not a chest size (9..54, multiple of 9)");
                }
            } else {
                errors.add(where + ": size is not a number");
            }
        }
        List<LayoutDef.FixedItem> fixed = new ArrayList<>();
        Set<Integer> usedSlots = new java.util.HashSet<>();
        Object fixedRaw = page.get("fixed");
        if (fixedRaw != null) {
            if (!(fixedRaw instanceof List)) {
                errors.add(where + ": 'fixed' is not a list");
            } else {
                for (Object fRaw : (List<?>) fixedRaw) {
                    LayoutDef.FixedItem item = parseFixed(where, fRaw, errors);
                    if (item != null) {
                        if (!usedSlots.add(item.slot())) {
                            errors.add(where + ": slot " + item.slot()
                                    + " is used twice in fixed items");
                        }
                        if (item.slot() < 0 || item.slot() >= size) {
                            errors.add(where + ": fixed slot " + item.slot()
                                    + " is outside the page size " + size);
                        }
                        fixed.add(item);
                    }
                }
            }
        }
        Map<String, List<Integer>> slots = new LinkedHashMap<>();
        Object slotsRaw = page.get("slots");
        if (slotsRaw != null) {
            if (!(slotsRaw instanceof Map)) {
                errors.add(where + ": 'slots' is not a map");
            } else {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) slotsRaw).entrySet()) {
                    String group = String.valueOf(e.getKey());
                    if (!claimedGroups.add(group)) {
                        errors.add(where + ": group '" + group
                                + "' already has slots on an earlier page");
                    }
                    List<Integer> list = new ArrayList<>();
                    if (e.getValue() instanceof List) {
                        for (Object s : (List<?>) e.getValue()) {
                            if (s instanceof Number n) {
                                int slot = n.intValue();
                                if (slot < 0 || slot >= size) {
                                    errors.add(where + ": slot " + slot + " for group '"
                                            + group + "' is outside the page size " + size);
                                }
                                if (usedSlots.contains(slot)) {
                                    errors.add(where + ": slot " + slot + " for group '"
                                            + group + "' collides with a fixed item");
                                }
                                list.add(slot);
                            } else {
                                errors.add(where + ": slot for group '" + group
                                        + "' is not a number");
                            }
                        }
                    } else {
                        errors.add(where + ": slots for group '" + group + "' is not a list");
                    }
                    slots.put(group, List.copyOf(list));
                }
            }
        }
        Object titleRaw = page.get("title");
        return new LayoutDef.Page(size, titleRaw == null ? null : String.valueOf(titleRaw),
                fixed, slots);
    }

    private static LayoutDef.FixedItem parseFixed(String where, Object raw,
                                                  List<String> errors) {
        if (!(raw instanceof Map)) {
            errors.add(where + ": a fixed item is not a map");
            return null;
        }
        Map<?, ?> m = (Map<?, ?>) raw;
        for (Object key : m.keySet()) {
            if (!FIXED_KEYS.contains(String.valueOf(key))) {
                errors.add(where + ": fixed item has unknown key '" + key + "'");
            }
        }
        Object slotRaw = m.get("slot");
        if (!(slotRaw instanceof Number)) {
            errors.add(where + ": fixed item is missing a numeric 'slot'");
            return null;
        }
        String material = str(m.get("material"));
        if (material == null || material.isBlank()) {
            errors.add(where + ": fixed item at slot " + slotRaw + " has no material");
            return null;
        }
        List<String> lore = strList(m.get("lore"));
        List<String> action = strList(m.get("action"));
        if (action != null) {
            for (String line : action) {
                String tag = line.startsWith("[") && line.contains("]")
                        ? line.substring(1, line.indexOf(']')).toLowerCase()
                        : "";
                if (!ACTION_TAGS.contains(tag)) {
                    errors.add(where + ": fixed item action '" + line
                            + "' has an unknown tag");
                }
            }
        }
        Integer page = null;
        Object pageRaw = m.get("page");
        if (pageRaw instanceof Number n) page = n.intValue();
        if (page != null && action != null && !action.isEmpty()) {
            errors.add(where + ": fixed item at slot " + slotRaw
                    + " sets both 'page' and 'action' — one behaviour per item");
            return null;
        }
        return new LayoutDef.FixedItem(((Number) slotRaw).intValue(), material,
                str(m.get("name")), lore, page, action);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static List<String> strList(Object o) {
        if (o == null) return null;
        if (o instanceof List) {
            List<String> out = new ArrayList<>();
            for (Object e : (List<?>) o) out.add(String.valueOf(e));
            return out;
        }
        return List.of(String.valueOf(o));
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
