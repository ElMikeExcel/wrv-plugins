// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.registry;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The on-disk codec for {@link PlayerMenuPrefs} (DESIGN §3.8):
 * one small YAML document per player. Pure (string in, string out)
 * so the round-trip and the corrupt-file posture are unit-tested
 * without Bukkit. A parse failure yields no prefs plus an error
 * string — the caller logs SEVERE once and serves the default
 * order; a menu open never fails over a prefs file.
 *
 * Ids naming entries that no longer exist survive a save/load
 * cycle verbatim: dropping them is the merge's job, not the
 * codec's (DESIGN §7, PrefsStoreTest).
 */
public final class PrefsCodec {

    public record Result(PlayerMenuPrefs prefs, String error) {
        public boolean ok() { return prefs != null && error == null; }
        static Result bad(String error) { return new Result(null, error); }
    }

    private PrefsCodec() {}

    public static String toYaml(PlayerMenuPrefs prefs) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", prefs.version());
        root.put("menu", prefs.menu());
        if (!prefs.groupOrder().isEmpty()) {
            root.put("group-order", new ArrayList<>(prefs.groupOrder()));
        }
        if (!prefs.groups().isEmpty()) {
            Map<String, Object> groups = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> e : prefs.groups().entrySet()) {
                groups.put(e.getKey(), new ArrayList<>(e.getValue()));
            }
            root.put("groups", groups);
        }
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(new Representer(opts), opts).dump(root);
    }

    public static Result parse(String text) {
        Object parsed;
        try {
            LoaderOptions opts = new LoaderOptions();
            opts.setAllowDuplicateKeys(false);
            parsed = new Yaml(new SafeConstructor(opts)).load(text);
        } catch (RuntimeException e) {
            return Result.bad("cannot parse — " + e.getMessage());
        }
        if (parsed == null) return Result.bad("empty file");
        if (!(parsed instanceof Map<?, ?> root)) return Result.bad("top level is not a map");

        Object versionRaw = root.get("version");
        if (!(versionRaw instanceof Number n)) return Result.bad("missing or non-numeric 'version'");
        int version = n.intValue();
        if (version != PlayerMenuPrefs.CURRENT_VERSION) {
            return Result.bad("unsupported version " + version);
        }
        Object menuRaw = root.get("menu");
        if (!(menuRaw instanceof String menu) || menu.isBlank()) {
            return Result.bad("missing 'menu'");
        }
        List<String> groupOrder;
        try {
            groupOrder = strList(root.get("group-order"), "group-order");
        } catch (IllegalArgumentException e) {
            return Result.bad(e.getMessage());
        }
        Map<String, List<String>> groups = new LinkedHashMap<>();
        Object groupsRaw = root.get("groups");
        if (groupsRaw != null) {
            if (!(groupsRaw instanceof Map<?, ?> gm)) return Result.bad("'groups' is not a map");
            for (Map.Entry<?, ?> e : gm.entrySet()) {
                String key = String.valueOf(e.getKey());
                try {
                    groups.put(key, strList(e.getValue(), "groups." + key));
                } catch (IllegalArgumentException ex) {
                    return Result.bad(ex.getMessage());
                }
            }
        }
        return new Result(new PlayerMenuPrefs(version, menu, groupOrder, groups), null);
    }

    private static List<String> strList(Object raw, String where) {
        if (raw == null) return List.of();
        if (!(raw instanceof List<?> list)) {
            throw new IllegalArgumentException("'" + where + "' is not a list");
        }
        List<String> out = new ArrayList<>();
        for (Object e : list) {
            if (!(e instanceof String s) || s.isBlank()) {
                throw new IllegalArgumentException("'" + where + "' holds a blank or non-string id");
            }
            out.add(s);
        }
        return List.copyOf(out);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
