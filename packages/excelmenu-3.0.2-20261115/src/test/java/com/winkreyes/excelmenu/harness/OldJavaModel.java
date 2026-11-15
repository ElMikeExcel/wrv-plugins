package com.winkreyes.excelmenu.harness;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The OLD Java column, computed independently of the 2.0 code: the
 * live server_menu.yml parsed raw, visible items chosen by the 0.1.8
 * Selector's rule (per slot, lowest priority whose view requirements
 * all pass). Layout panes (the five group headers) are not entries
 * and are excluded — in 2.0 they stay in the menu definition's
 * layout, which is Phase 1's file, not the registry's.
 */
final class OldJavaModel {

    private record Item(String itemId, String material, int slot, int priority,
                        String name, Map<String, String> requirements,
                        List<String> clickLines) {}

    private final List<Item> items = new ArrayList<>();

    @SuppressWarnings("unchecked")
    OldJavaModel(Path serverMenuYml) throws Exception {
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(serverMenuYml)) {
            root = new Yaml().load(in);
        }
        Map<String, Object> itemsMap = (Map<String, Object>) root.get("items");
        for (Map.Entry<String, Object> e : itemsMap.entrySet()) {
            Map<String, Object> m = (Map<String, Object>) e.getValue();
            String material = String.valueOf(m.get("material"));
            if (material.endsWith("_STAINED_GLASS_PANE")) continue; // layout header
            int slot = ((Number) m.get("slot")).intValue();
            int priority = m.get("priority") == null
                    ? Integer.MAX_VALUE : ((Number) m.get("priority")).intValue();
            Map<String, String> reqs = new LinkedHashMap<>();
            Map<String, Object> vr = (Map<String, Object>) m.get("view-requirement");
            if (vr != null) {
                for (Map<String, Object> r
                        : (List<Map<String, Object>>) vr.get("requirements")) {
                    String type = String.valueOf(r.get("type"));
                    if (!type.equals("string-equals")) {
                        throw new IllegalStateException(
                                "harness only models string-equals, found " + type);
                    }
                    reqs.put(String.valueOf(r.get("input")),
                            String.valueOf(r.get("output")));
                }
            }
            List<String> lines = List.of();
            Map<String, Object> clicks = (Map<String, Object>) m.get("clicks");
            if (clicks != null) {
                Object chosen = clicks.get("left");
                if (chosen == null) chosen = clicks.get("any");
                if (chosen == null) chosen = clicks.get("right");
                if (chosen instanceof List<?> list) {
                    List<String> tmp = new ArrayList<>();
                    for (Object o : list) tmp.add(String.valueOf(o));
                    lines = List.copyOf(tmp);
                }
            }
            items.add(new Item(e.getKey(), material, slot, priority,
                    m.get("name") == null ? null : String.valueOf(m.get("name")),
                    reqs, lines));
        }
    }

    List<HarnessSupport.Row> visible(HarnessSupport.ViewerState s) {
        Map<String, String> placeholders = Map.of(
                "%guildgate_unlocked%", s.guildLocked() ? "false" : "true",
                "%menujoin_status%", s.autojoin(),
                "%silkwarnings_status%", s.silk(),
                "%aimassist_status%", s.aim());
        Map<Integer, Item> bySlot = new TreeMap<>();
        for (Item it : items) {
            boolean pass = true;
            for (Map.Entry<String, String> req : it.requirements().entrySet()) {
                String actual = placeholders.getOrDefault(req.getKey(), req.getKey());
                if (!actual.equals(req.getValue())) { pass = false; break; }
            }
            if (!pass) continue;
            Item current = bySlot.get(it.slot());
            if (current == null || it.priority() < current.priority()) {
                bySlot.put(it.slot(), it);
            }
        }
        List<HarnessSupport.Row> rows = new ArrayList<>();
        for (Item it : bySlot.values()) {
            rows.add(new HarnessSupport.Row(HarnessSupport.javaItemToEntry(it.itemId()),
                    it.name(), HarnessSupport.dispatchKey(it.clickLines())));
        }
        return rows;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
