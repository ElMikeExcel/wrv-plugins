// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.registry;

import com.winkreyes.excelmenu.core.platform.Platform;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parses one registry entry file into an {@link EntryDef}. Strict on
 * purpose, in the ExcelMenu tradition: unknown keys, wrong types,
 * duplicate YAML keys, contradictory alias fields, unknown action
 * tags, bad dates and an id that does not match the filename are all
 * collected as errors and the entry is refused. Nothing is defaulted
 * silently beyond the documented defaults (platforms = both).
 *
 * Alias discipline (one fact, one value): {@code permission:} is
 * sugar for {@code requires-permission:}, and {@code states.source}
 * for {@code status-placeholder:}. Both spellings may appear only if
 * they AGREE; a disagreement is a load error, never a silent winner.
 */
public final class EntryLoader {

    public record Result(EntryDef def, List<String> errors) {
        public boolean ok() { return def != null && errors.isEmpty(); }
    }

    private static final Set<String> ENTRY_KEYS = Set.of(
            "id", "menu", "group", "order", "order-bedrock", "icon", "label",
            "lore", "action", "platforms", "permission", "requires-plugin",
            "requires-permission", "status-placeholder", "states",
            "visible-from", "visible-until");
    private static final Set<String> VARIANT_KEYS = Set.of(
            "icon", "label", "lore", "action");
    /** The bracket vocabulary, identical to ExcelMenu's ActionParser. */
    private static final Set<String> ACTION_TAGS = Set.of(
            "console", "player", "commandevent", "message", "openguimenu",
            "close", "refresh", "json", "sound", "broadcastsound", "connect");

    private EntryLoader() {}

    public static Result load(Path file) {
        String id = file.getFileName().toString();
        if (id.endsWith(".yml")) id = id.substring(0, id.length() - 4);
        try (InputStream in = Files.newInputStream(file)) {
            return load(id, in);
        } catch (IOException e) {
            return new Result(null, List.of(id + ": cannot read file — " + e.getMessage()));
        }
    }

    public static Result load(String expectedId, InputStream in) {
        List<String> errors = new ArrayList<>();
        Object root;
        try {
            LoaderOptions opts = new LoaderOptions();
            opts.setAllowDuplicateKeys(false);
            root = new Yaml(new SafeConstructor(opts)).load(in);
        } catch (RuntimeException e) {
            return new Result(null, List.of(expectedId + ": YAML parse failed — " + firstLine(e)));
        }
        if (!(root instanceof Map<?, ?> map)) {
            return new Result(null, List.of(expectedId + ": top level is not a mapping"));
        }
        String w = expectedId;
        for (Object k : map.keySet()) {
            if (!ENTRY_KEYS.contains(String.valueOf(k))) {
                errors.add(w + ": unknown entry key '" + k + "'");
            }
        }
        String id = str(map, "id", w, errors);
        if (id == null) {
            errors.add(w + ": 'id' is required (and must equal the filename)");
        } else if (!id.equals(expectedId)) {
            errors.add(w + ": 'id' is '" + id + "' but the filename says '" + expectedId + "'");
        }
        String menu = str(map, "menu", w, errors);
        if (menu == null || menu.isBlank()) errors.add(w + ": 'menu' is required");
        String group = str(map, "group", w, errors);
        if (group == null || group.isBlank()) errors.add(w + ": 'group' is required");
        Integer order = integerOrNull(map, "order", w, errors);
        if (order == null) errors.add(w + ": 'order' is required");
        else if (order < 0) errors.add(w + ": 'order' must be >= 0");
        Integer orderBedrock = integerOrNull(map, "order-bedrock", w, errors);
        if (orderBedrock != null && orderBedrock < 0) {
            errors.add(w + ": 'order-bedrock' must be >= 0");
        }
        String icon = str(map, "icon", w, errors);
        if (icon == null || icon.isBlank()) errors.add(w + ": 'icon' is required");
        String label = str(map, "label", w, errors);
        if (label == null) errors.add(w + ": 'label' is required");
        List<String> lore = stringList(map.get("lore"), w + " lore", errors);
        List<String> action = actionLines(map.get("action"), w + " action", errors);

        Set<Platform> platforms = new LinkedHashSet<>();
        Object platformsObj = map.get("platforms");
        if (platformsObj == null) {
            platforms.add(Platform.JAVA);
            platforms.add(Platform.BEDROCK);
        } else if (platformsObj instanceof List<?> list) {
            if (list.isEmpty()) errors.add(w + ": 'platforms' must not be empty");
            for (Object o : list) {
                Platform p = Platform.parse(o == null ? null : String.valueOf(o));
                if (p == null) {
                    errors.add(w + ": unknown platform '" + o + "' (java|bedrock)");
                } else {
                    platforms.add(p);
                }
            }
        } else {
            errors.add(w + ": 'platforms' must be a list");
        }

        String permission = str(map, "permission", w, errors);
        String requiresPermission = str(map, "requires-permission", w, errors);
        if (permission != null && requiresPermission != null
                && !permission.equals(requiresPermission)) {
            errors.add(w + ": 'permission' ('" + permission + "') and 'requires-permission'"
                    + " ('" + requiresPermission + "') disagree — state the gate once");
        }
        String effectivePermission = requiresPermission != null ? requiresPermission : permission;
        String requiresPlugin = str(map, "requires-plugin", w, errors);

        // states + its source (status-placeholder, or states.source as alias)
        String statusPlaceholder = str(map, "status-placeholder", w, errors);
        Map<String, EntryDef.Variant> states = new LinkedHashMap<>();
        Object statesObj = map.get("states");
        if (statesObj != null) {
            if (!(statesObj instanceof Map<?, ?> sm)) {
                errors.add(w + ": 'states' must be a mapping of state value -> variant");
            } else {
                for (Map.Entry<?, ?> e : sm.entrySet()) {
                    if (e.getKey() instanceof Boolean b) {
                        // Unquoted ON/OFF/true/false arrive as YAML
                        // booleans, indistinguishable from each other —
                        // refuse loudly and teach the fix instead of
                        // silently keying a variant "true".
                        errors.add(w + ": state key '" + b + "' parsed as a boolean —"
                                + " quote state keys ('ON', 'OFF', 'true', 'false')"
                                + " so YAML keeps them as text");
                        continue;
                    }
                    String key = String.valueOf(e.getKey());
                    if (key.equals("source")) {
                        String src = e.getValue() == null ? null : String.valueOf(e.getValue());
                        if (statusPlaceholder != null && src != null
                                && !statusPlaceholder.equals(src)) {
                            errors.add(w + ": 'status-placeholder' ('" + statusPlaceholder
                                    + "') and 'states.source' ('" + src + "') disagree");
                        }
                        if (statusPlaceholder == null) statusPlaceholder = src;
                        continue;
                    }
                    EntryDef.Variant v = variant(w + " states." + key, e.getValue(), errors);
                    if (v != null) states.put(key, v);
                }
            }
        }
        if (!states.isEmpty() && statusPlaceholder == null) {
            errors.add(w + ": 'states' needs a source — set 'status-placeholder'"
                    + " (or 'states.source')");
        }
        if (statusPlaceholder != null && states.isEmpty()) {
            errors.add(w + ": 'status-placeholder' is set but 'states' is empty —"
                    + " a state source with no variants can never select anything");
        }
        if (statusPlaceholder != null && !isPlaceholderToken(statusPlaceholder)) {
            errors.add(w + ": 'status-placeholder' must be a %token%, found '"
                    + statusPlaceholder + "'");
        }

        LocalDate from = date(map.get("visible-from"), w + " visible-from", errors);
        LocalDate until = date(map.get("visible-until"), w + " visible-until", errors);
        if (from != null && until != null && until.isBefore(from)) {
            errors.add(w + ": 'visible-until' (" + until + ") is before 'visible-from' ("
                    + from + ")");
        }

        if (!errors.isEmpty()) return new Result(null, errors);
        return new Result(new EntryDef(expectedId, menu, group, order, orderBedrock,
                icon, label, lore, action, platforms, requiresPlugin, effectivePermission,
                statusPlaceholder, states, from, until, EntryDef.Origin.FILE), List.of());
    }

    private static EntryDef.Variant variant(String w, Object obj, List<String> errors) {
        if (!(obj instanceof Map<?, ?> m)) {
            errors.add(w + ": variant is not a mapping");
            return null;
        }
        int before = errors.size();
        for (Object k : m.keySet()) {
            if (!VARIANT_KEYS.contains(String.valueOf(k))) {
                errors.add(w + ": unknown variant key '" + k + "'");
            }
        }
        String icon = str(m, "icon", w, errors);
        String label = str(m, "label", w, errors);
        List<String> lore = m.containsKey("lore")
                ? stringList(m.get("lore"), w + " lore", errors) : null;
        List<String> action = m.containsKey("action")
                ? actionLines(m.get("action"), w + " action", errors) : null;
        if (errors.size() != before) return null;
        return new EntryDef.Variant(icon, label, lore, action);
    }

    // ---------- field helpers ----------

    private static boolean isPlaceholderToken(String s) {
        return s.length() > 2 && s.startsWith("%") && s.endsWith("%")
                && s.indexOf('%', 1) == s.length() - 1;
    }

    private static List<String> actionLines(Object v, String where, List<String> errors) {
        List<String> lines = new ArrayList<>();
        if (v == null) return lines;
        if (v instanceof String s) {
            lines.add(s);
        } else if (v instanceof List<?> list) {
            for (Object o : list) {
                if (o == null) errors.add(where + ": null action line");
                else lines.add(String.valueOf(o));
            }
        } else {
            errors.add(where + ": must be a string or a list of strings");
            return lines;
        }
        List<String> checked = new ArrayList<>();
        for (String line : lines) {
            String t = line.trim();
            if (!t.startsWith("[") || t.indexOf(']') < 0) {
                errors.add(where + ": action line has no [tag]: '" + line + "'");
                continue;
            }
            String tag = t.substring(1, t.indexOf(']')).toLowerCase(Locale.ROOT);
            if (!ACTION_TAGS.contains(tag)) {
                errors.add(where + ": unknown action tag [" + tag + "] in '" + line + "'");
                continue;
            }
            checked.add(line);
        }
        return checked;
    }

    private static LocalDate date(Object v, String where, List<String> errors) {
        if (v == null) return null;
        if (v instanceof LocalDate ld) return ld;
        if (v instanceof Date d) {
            // SnakeYAML hands back a UTC-midnight Date for yyyy-MM-dd;
            // read it in UTC so the calendar day never shifts.
            return d.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        }
        String s = String.valueOf(v).trim();
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            errors.add(where + ": must be a date (yyyy-MM-dd), found '" + v + "'");
            return null;
        }
    }

    private static String str(Map<?, ?> m, String key, String where, List<String> errors) {
        Object v = m.get(key);
        if (v == null) return null;
        if (v instanceof String s) return s;
        errors.add(where + ": '" + key + "' must be a string, found " + describe(v));
        return null;
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
            if (o == null) errors.add(where + ": null entry");
            else out.add(String.valueOf(o));
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
