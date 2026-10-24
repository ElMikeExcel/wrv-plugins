package com.winkreyes.excelmenu.core;

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
 * Parses ExcelMenu format-1 definition files into MenuDef records.
 * Strict on purpose: unknown keys, wrong value types, duplicate YAML
 * keys, and malformed requirement/click blocks are all collected as
 * errors and the definition is refused. Nothing is defaulted
 * silently beyond the documented defaults (type chest, size 54 —
 * the DeluxeMenus defaults these menus were authored against).
 */
public final class DefinitionLoader {

    public record Result(MenuDef def, List<String> errors) {
        public boolean ok() { return def != null && errors.isEmpty(); }
    }

    private static final Set<String> MENU_KEYS = Set.of(
            "format", "title", "size", "type", "bedrock-render", "command",
            "update-interval", "open-requirement", "items");
    private static final Set<String> ITEM_KEYS = Set.of(
            "material", "slot", "amount", "data", "priority", "update",
            "name", "lore", "view-requirement", "clicks", "bedrock");
    private static final Set<String> CLICK_KEYS = Set.of(
            "left", "right", "shift-left", "shift-right", "middle", "any");

    private DefinitionLoader() {}

    public static Result load(Path file) {
        String id = file.getFileName().toString();
        if (id.endsWith(".yml")) id = id.substring(0, id.length() - 4);
        try (InputStream in = Files.newInputStream(file)) {
            return load(id, in);
        } catch (IOException e) {
            return new Result(null, List.of(id + ": cannot read file — " + e.getMessage()));
        }
    }

    public static Result load(String id, InputStream in) {
        List<String> errors = new ArrayList<>();
        Object root;
        try {
            LoaderOptions opts = new LoaderOptions();
            opts.setAllowDuplicateKeys(false);
            root = new Yaml(new SafeConstructor(opts)).load(in);
        } catch (RuntimeException e) {
            return new Result(null, List.of(id + ": YAML parse failed — " + firstLine(e)));
        }
        if (!(root instanceof Map<?, ?> map)) {
            return new Result(null, List.of(id + ": top level is not a mapping"));
        }
        for (Object k : map.keySet()) {
            if (!MENU_KEYS.contains(String.valueOf(k))) {
                errors.add(id + ": unknown menu key '" + k + "'");
            }
        }
        Object format = map.get("format");
        if (!(format instanceof Number) || ((Number) format).intValue() != 1) {
            errors.add(id + ": 'format' must be 1, found " + describe(format));
        }
        String title = str(map, "title", id, errors);
        if (title == null) errors.add(id + ": 'title' is required");
        int size = integer(map, "size", 54, id, errors);
        String type = str(map, "type", id, errors);
        if (type == null) type = "chest";
        String bedrockRender = str(map, "bedrock-render", id, errors);
        if (bedrockRender == null) bedrockRender = "form";
        String command = str(map, "command", id, errors);
        int updateInterval = integer(map, "update-interval", 0, id, errors);

        List<MenuDef.Requirement> openReqs = List.of();
        List<String> openDeny = List.of();
        if (map.get("open-requirement") != null) {
            ReqBlock rb = requirements(map.get("open-requirement"), id + " open-requirement", errors);
            openReqs = rb.reqs();
            openDeny = rb.deny();
        }

        List<MenuDef.ItemDef> items = new ArrayList<>();
        Object itemsObj = map.get("items");
        Map<?, ?> itemsMap;
        if (itemsObj instanceof Map<?, ?> im) {
            itemsMap = im;
        } else {
            errors.add(id + ": 'items' must be a mapping of item id -> item");
            itemsMap = Map.of();
        }
        for (Map.Entry<?, ?> e : itemsMap.entrySet()) {
            String itemId = String.valueOf(e.getKey());
            MenuDef.ItemDef parsed = item(id, itemId, e.getValue(), errors);
            if (parsed != null) items.add(parsed);
        }

        if (!errors.isEmpty()) return new Result(null, errors);
        return new Result(new MenuDef(id, title, size, type.toLowerCase(),
                bedrockRender.toLowerCase(), command,
                updateInterval, openReqs, openDeny, items), List.of());
    }

    private static MenuDef.ItemDef item(String menuId, String itemId, Object obj,
                                        List<String> errors) {
        String w = menuId + ":" + itemId;
        if (!(obj instanceof Map<?, ?> m)) {
            errors.add(w + ": item is not a mapping");
            return null;
        }
        int before = errors.size();
        for (Object k : m.keySet()) {
            if (!ITEM_KEYS.contains(String.valueOf(k))) {
                errors.add(w + ": unknown item key '" + k + "'");
            }
        }
        String material = str(m, "material", w, errors);
        if (material == null) errors.add(w + ": 'material' is required");
        Integer slot = integerOrNull(m, "slot", w, errors);
        if (slot == null) errors.add(w + ": 'slot' is required");
        int amount = integer(m, "amount", 1, w, errors);
        Integer data = integerOrNull(m, "data", w, errors);
        Integer priority = integerOrNull(m, "priority", w, errors);
        boolean update = Boolean.TRUE.equals(m.get("update"));
        String name = str(m, "name", w, errors);
        List<String> lore = stringList(m.get("lore"), w + " lore", errors);

        List<MenuDef.Requirement> viewReqs = List.of();
        if (m.get("view-requirement") != null) {
            viewReqs = requirements(m.get("view-requirement"), w + " view-requirement", errors).reqs();
        }

        Map<String, List<String>> clicks = new LinkedHashMap<>();
        Object clicksObj = m.get("clicks");
        if (clicksObj != null) {
            if (!(clicksObj instanceof Map<?, ?> cm)) {
                errors.add(w + ": 'clicks' must be a mapping");
            } else {
                for (Map.Entry<?, ?> ce : cm.entrySet()) {
                    String kind = String.valueOf(ce.getKey());
                    if (!CLICK_KEYS.contains(kind)) {
                        errors.add(w + ": unknown click kind '" + kind + "'");
                    }
                    clicks.put(kind, stringList(ce.getValue(), w + " clicks." + kind, errors));
                }
            }
        }

        MenuDef.BedrockDef bedrock = null;
        Object bObj = m.get("bedrock");
        if (bObj != null) {
            if (!(bObj instanceof Map<?, ?> bm)) {
                errors.add(w + ": 'bedrock' must be a mapping");
            } else {
                bedrock = new MenuDef.BedrockDef(
                        str(bm, "mode", w + " bedrock", errors),
                        str(bm, "left-label", w + " bedrock", errors),
                        str(bm, "right-label", w + " bedrock", errors),
                        str(bm, "four-label", w + " bedrock", errors));
            }
        }

        if (errors.size() != before) return null;
        return new MenuDef.ItemDef(itemId, material, slot, amount, data, priority,
                update, name, lore, viewReqs, clicks, bedrock);
    }

    private record ReqBlock(List<MenuDef.Requirement> reqs, List<String> deny) {}

    private static ReqBlock requirements(Object obj, String where, List<String> errors) {
        if (!(obj instanceof Map<?, ?> m) || !(m.get("requirements") instanceof List<?> list)) {
            errors.add(where + ": requirement block must be {requirements: [ ... ]}");
            return new ReqBlock(List.of(), List.of());
        }
        List<MenuDef.Requirement> reqs = new ArrayList<>();
        List<String> denyAll = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> rm)) {
                errors.add(where + ": requirement entry is not a mapping");
                continue;
            }
            String rid = str(rm, "id", where, errors);
            String type = str(rm, "type", where, errors);
            if (type == null) {
                errors.add(where + ": requirement is missing 'type'");
                continue;
            }
            Map<String, Object> options = new LinkedHashMap<>();
            List<String> deny = List.of();
            for (Map.Entry<?, ?> e : rm.entrySet()) {
                String k = String.valueOf(e.getKey());
                switch (k) {
                    case "id", "type" -> { /* identity, not options */ }
                    case "deny" -> deny = stringList(e.getValue(), where + " deny", errors);
                    default -> options.put(k, e.getValue());
                }
            }
            reqs.add(new MenuDef.Requirement(rid == null ? "?" : rid, type, options, deny));
            denyAll.addAll(deny);
        }
        return new ReqBlock(reqs, denyAll);
    }

    // ---------- scalar helpers ----------

    private static String str(Map<?, ?> m, String key, String where, List<String> errors) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof String s) return s;
        errors.add(where + ": '" + key + "' must be a string, found " + describe(v));
        return null;
    }

    private static int integer(Map<?, ?> m, String key, int dflt, String where,
                               List<String> errors) {
        Integer v = integerOrNull(m, key, where, errors);
        return v == null ? dflt : v;
    }

    private static Integer integerOrNull(Map<?, ?> m, String key, String where,
                                         List<String> errors) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        errors.add(where + ": '" + key + "' must be a number, found " + describe(v));
        return null;
    }

    private static List<String> stringList(Object v, String where, List<String> errors) {
        if (v == null) return List.of();
        if (!(v instanceof List<?> list)) {
            errors.add(where + ": must be a list of strings");
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            if (o == null) {
                errors.add(where + ": null entry");
            } else {
                out.add(String.valueOf(o));
            }
        }
        return out;
    }

    private static String describe(Object v) {
        return v == null ? "nothing" : v.getClass().getSimpleName() + " '" + v + "'";
    }

    private static String firstLine(Throwable t) {
        String msg = String.valueOf(t.getMessage());
        int nl = msg.indexOf('\n');
        return nl < 0 ? msg : msg.substring(0, nl);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
