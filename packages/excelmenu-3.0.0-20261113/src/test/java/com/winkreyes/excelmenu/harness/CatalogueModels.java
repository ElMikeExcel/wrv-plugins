package com.winkreyes.excelmenu.harness;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The catalogue's two OLD columns, computed independently from the
 * sources of record (never hand-transcribed):
 *
 *   Old Java    — the ExcelMenu 0.1.8 preserved definition files
 *                 (fixtures/commands_menu.yml + _page2.yml): items
 *                 in file order, group headers tracked, pagination
 *                 chrome (nav_more / back) set aside as chrome.
 *   Old Bedrock — ExcelBedrockMenu 1.5.27's CATALOG arrays
 *                 (fixtures/bedrock-arrays.json, machine-extracted).
 */
final class CatalogueModels {

    record JavaRow(String id, String display, String group, String label,
                   List<String> lore, List<String> action) {}

    record BedrockRow(String display, String group, String run, String description) {}

    private CatalogueModels() {}

    @SuppressWarnings("unchecked")
    static List<JavaRow> oldJava(Path fixtures) throws Exception {
        List<JavaRow> rows = new ArrayList<>();
        for (String file : List.of("commands_menu.yml", "commands_menu_page2.yml")) {
            Map<String, Object> root;
            try (InputStream in = Files.newInputStream(fixtures.resolve(file))) {
                root = new Yaml().load(in);
            }
            Map<String, Object> items = (Map<String, Object>) root.get("items");
            String group = null;
            for (Map.Entry<String, Object> e : items.entrySet()) {
                Map<String, Object> m = (Map<String, Object>) e.getValue();
                String name = String.valueOf(m.get("name"));
                List<String> lore = new ArrayList<>();
                if (m.get("lore") instanceof List) {
                    for (Object l : (List<Object>) m.get("lore")) lore.add(String.valueOf(l));
                }
                if (lore.size() == 1 && lore.get(0).equals("&8Command group")) {
                    group = name.replace("&6&l", "");
                    continue;
                }
                if (e.getKey().equals("nav_more") || e.getKey().equals("back")) continue;
                List<String> action = List.of();
                if (m.get("clicks") instanceof Map) {
                    Object left = ((Map<String, Object>) m.get("clicks")).get("left");
                    if (left instanceof List) {
                        List<String> lines = new ArrayList<>();
                        for (Object l : (List<Object>) left) lines.add(String.valueOf(l));
                        action = lines;
                    }
                }
                rows.add(new JavaRow(e.getKey(), name.replace("&a&l", ""), group,
                        name, lore, action));
            }
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    static List<BedrockRow> oldBedrock(Path fixtures) throws Exception {
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(fixtures.resolve("bedrock-arrays.json"))) {
            root = new Yaml().load(in); // JSON is a YAML subset
        }
        List<String> groups = (List<String>) root.get("catalog_groups");
        List<Object> catalog = (List<Object>) root.get("catalog");
        List<BedrockRow> rows = new ArrayList<>();
        for (int g = 0; g < catalog.size(); g++) {
            for (Object o : (List<Object>) catalog.get(g)) {
                Map<String, Object> r = (Map<String, Object>) o;
                rows.add(new BedrockRow(String.valueOf(r.get("display")), groups.get(g),
                        r.get("run") == null ? null : String.valueOf(r.get("run")),
                        String.valueOf(r.get("description"))));
            }
        }
        return rows;
    }

    /** Dispatch intent of an action line list, catalogue dialect. */
    static String keyOf(List<String> action) {
        if (action == null || action.isEmpty()) return "none";
        for (String l : action) {
            if (l.startsWith("[openguimenu] teleport_confirm_spawn")) return "teleport:spawn";
            if (l.startsWith("[openguimenu] teleport_confirm_guild")) return "teleport:guild";
            if (l.startsWith("[openguimenu] ")) return "menu:"
                    + l.substring("[openguimenu] ".length()).trim();
        }
        for (String l : action) {
            if (l.startsWith("[player] ")) return "cmd:"
                    + l.substring("[player] ".length()).trim();
        }
        for (String l : action) if (l.startsWith("[message]")) return "message";
        return "none";
    }

    /** Dispatch intent of an old Bedrock catalogue row. */
    static String bedrockKeyOf(BedrockRow r) {
        if (r.run() == null) return "none";
        return switch (r.run()) {
            case "spawn" -> "teleport:spawn";
            case "ag" -> "ag-branch"; // lock-dependent: confirm when
            // unlocked, /seals page when sealed (1.5.27 runAction)
            default -> "cmd:" + r.run();
        };
    }

    static Map<String, JavaRow> byDisplay(List<JavaRow> rows) {
        Map<String, JavaRow> out = new LinkedHashMap<>();
        for (JavaRow r : rows) out.put(r.display(), r);
        return out;
    }

    static Map<String, BedrockRow> bedrockByDisplay(List<BedrockRow> rows) {
        Map<String, BedrockRow> out = new LinkedHashMap<>();
        for (BedrockRow r : rows) out.put(r.display(), r);
        return out;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
