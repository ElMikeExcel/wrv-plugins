// SPDX-License-Identifier: MIT
package com.winkreyes.menustats;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.yaml.snakeyaml.Yaml;

/** Read-only index of plugins/ExcelMenu/menus/*.yml: menu id, title, size and each item key's slot(s) and name. */
public final class MenuIndex {
    public record Item(String key, String name) { }
    public record Menu(String id, String title, int size, Map<Integer, List<Item>> slots) { }
    private final List<Menu> menus = new ArrayList<>();
    private final List<String> failed = new ArrayList<>();

    public static MenuIndex load(Path dir) {
        MenuIndex idx = new MenuIndex();
        if (!Files.isDirectory(dir)) return idx;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.yml")) {
            List<Path> files = new ArrayList<>(); ds.forEach(files::add); Collections.sort(files);
            for (Path p : files) {
                String id = p.getFileName().toString().replaceFirst("\\.yml$", "");
                try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) { idx.menus.add(parse(id, new Yaml().load(r))); }
                catch (Exception e) { idx.failed.add(id + " (" + e.getMessage() + ")"); }
            }
        } catch (IOException e) { idx.failed.add("menus folder: " + e.getMessage()); }
        return idx;
    }
    @SuppressWarnings("unchecked")
    static Menu parse(String id, Object root) {
        Map<String, Object> m = root instanceof Map ? (Map<String, Object>) root : Map.of();
        String title = String.valueOf(m.getOrDefault("title", m.getOrDefault("menu_title", "")));
        int size = m.get("size") instanceof Number n ? n.intValue() : 0;
        Map<Integer, List<Item>> slots = new TreeMap<>();
        if (m.get("items") instanceof Map<?, ?> items) {
            for (Map.Entry<?, ?> e : items.entrySet()) {
                if (!(e.getValue() instanceof Map<?, ?> it)) continue;
                String key = String.valueOf(e.getKey());
                Object nm = it.get("name") != null ? it.get("name") : it.get("display_name");
                Item item = new Item(key, nm == null ? "" : String.valueOf(nm));
                for (int s : slotsOf(it)) slots.computeIfAbsent(s, k -> new ArrayList<>()).add(item);
            }
        }
        return new Menu(id, title, size, slots);
    }
    static List<Integer> slotsOf(Map<?, ?> it) {
        List<Integer> out = new ArrayList<>();
        if (it.get("slot") instanceof Number n) out.add(n.intValue());
        Object many = it.get("slots");
        if (many instanceof List<?> l) for (Object o : l) {
            if (o instanceof Number n) out.add(n.intValue());
            else { String s = String.valueOf(o).trim(); if (s.matches("\\d+-\\d+")) { String[] ab = s.split("-"); for (int i = Integer.parseInt(ab[0]); i <= Integer.parseInt(ab[1]); i++) out.add(i); } else if (s.matches("\\d+")) out.add(Integer.parseInt(s)); }
        }
        return out;
    }
    public List<Menu> menus() { return menus; }
    public List<String> failed() { return failed; }

    /** Strip & and § colour codes for comparison. */
    public static String plain(String s) { return s == null ? "" : s.replaceAll("(?i)[&\u00a7]([0-9a-fk-orx]|#[0-9a-f]{6})", "").trim(); }

    /** Find the menu shown with this title and size, or null. */
    public Menu byTitle(String shownTitle, int size) {
        String t = plain(shownTitle); Menu loose = null;
        for (Menu m : menus) if (plain(m.title()).equals(t)) { if (m.size() == size) return m; if (loose == null) loose = m; }
        return loose;
    }
    /**
     * Map a Bedrock form tap to the Java menu id + item key so both platforms share one row.
     * Order: explicit override, then form title == menu title (colour codes ignored) with the label
     * equal to an item name, then the label as a prefix of an item name (e.g. "Auto-open on join").
     * When two items share a name (view_requirement variants), the first in file order wins.
     * Returns null when nothing matches (the tap keeps its readable title/label).
     */
    public String[] mapForm(String title, String label, Map<String, String> overrides) {
        String t = plain(title), l = plain(label);
        String o = overrides == null ? null : overrides.get(t + " > " + l);
        if (o != null && o.contains(" > ")) { String[] ab = o.split(" > ", 2); return new String[]{ab[0].trim(), ab[1].trim()}; }
        for (int pass = 0; pass < 2; pass++)
            for (Menu m : menus) {
                if (!plain(m.title()).equalsIgnoreCase(t)) continue;
                for (List<Item> items : m.slots().values()) for (Item i : items) {
                    String n = plain(i.name());
                    if (pass == 0 ? n.equalsIgnoreCase(l) : (!l.isEmpty() && n.toLowerCase().startsWith(l.toLowerCase()))) return new String[]{m.id(), i.key()};
                }
            }
        return null;
    }
    /** Overrides file lines: "Form Title > Button Label: menu_id > button_key" (# comments). */
    public static Map<String, String> loadOverrides(Path file) {
        Map<String, String> out = new HashMap<>();
        try { if (Files.exists(file)) for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String s = line.strip(); if (s.isEmpty() || s.startsWith("#")) continue;
            int c = s.lastIndexOf(": "); if (c > 0) out.put(s.substring(0, c).strip(), s.substring(c + 2).strip());
        } } catch (IOException ignored) { }
        return out;
    }

    public Menu byId(String id) { for (Menu m : menus) if (m.id().equals(id)) return m; return null; }

    /** Which item key sits in this slot; ties (view_requirement variants) are broken by the shown item name. */
    public static String button(Menu menu, int slot, String shownName) {
        List<Item> c = menu.slots().get(slot);
        if (c == null || c.isEmpty()) return "slot" + slot;
        if (c.size() == 1) return c.get(0).key();
        String n = shownName == null ? null : shownName.trim();
        if (n != null) {
            for (Item i : c) if (i.name().replace('&', '\u00a7').equals(n) || i.name().equals(n)) return i.key();
            for (Item i : c) if (plain(i.name()).equals(plain(n))) return i.key();
        }
        StringJoiner j = new StringJoiner("|"); c.forEach(i -> j.add(i.key())); return j.toString();
    }
}
