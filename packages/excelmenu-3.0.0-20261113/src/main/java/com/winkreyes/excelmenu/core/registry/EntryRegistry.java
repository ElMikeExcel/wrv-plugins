// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.registry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * The merged registry: entry files from {@code entries/} plus API
 * registrations from other plugins, as ONE resolved namespace.
 *
 * Collision law (DESIGN.md §2.2): an id claimed by two files, or by a
 * file and an API registration, is a LOUD error — the colliding
 * entries are refused, never silently shadowed, and everything else
 * still loads. A dead button must be impossible to miss.
 */
public final class EntryRegistry {

    /** loaded: entry id -> def. failed: file stem / id -> errors. */
    public record LoadOutcome(Map<String, EntryDef> loaded,
                              Map<String, List<String>> failed) {}

    private volatile Map<String, EntryDef> entries = Map.of();
    private final Map<String, EntryDef> apiEntries = new LinkedHashMap<>();

    public synchronized LoadOutcome loadDirectory(Path dir) {
        Map<String, List<String>> failed = new TreeMap<>();
        Map<String, EntryDef> files = new LinkedHashMap<>();
        Map<String, String> fileOf = new LinkedHashMap<>();

        List<Path> paths = new ArrayList<>();
        if (dir != null && Files.isDirectory(dir)) {
            try (Stream<Path> s = Files.list(dir)) {
                s.filter(p -> p.getFileName().toString().endsWith(".yml"))
                        .sorted().forEach(paths::add);
            } catch (IOException e) {
                failed.put("<folder>", List.of("cannot list entries folder " + dir
                        + " — " + e.getMessage()));
            }
        }
        for (Path file : paths) {
            EntryLoader.Result r = EntryLoader.load(file);
            String stem = file.getFileName().toString().replaceFirst("\\.yml$", "");
            if (!r.ok()) {
                failed.put(stem, r.errors());
                continue;
            }
            EntryDef def = r.def();
            if (files.containsKey(def.id())) {
                String other = fileOf.get(def.id());
                failed.put(def.id(), List.of(def.id() + ": duplicate entry id — claimed by "
                        + other + " and " + file.getFileName() + "; BOTH refused"));
                failed.put(other.replaceFirst("\\.yml$", ""), List.of(def.id()
                        + ": duplicate entry id — claimed by " + other + " and "
                        + file.getFileName() + "; BOTH refused"));
                files.remove(def.id());
                fileOf.remove(def.id());
                continue;
            }
            files.put(def.id(), def);
            fileOf.put(def.id(), file.getFileName().toString());
        }
        // API registrations merge over the file set; a collision is an
        // error for the API side only (the file of record stays).
        Map<String, EntryDef> merged = new LinkedHashMap<>(files);
        synchronized (this) {
            for (EntryDef api : apiEntries.values()) {
                if (merged.containsKey(api.id())) {
                    failed.put(api.id(), List.of(api.id() + ": API registration collides"
                            + " with the entry file of the same id; the API entry is"
                            + " refused (DESIGN.md §2.2: never a silent shadow)"));
                } else {
                    merged.put(api.id(), api);
                }
            }
        }
        return new LoadOutcome(Map.copyOf(merged), Map.copyOf(failed));
    }

    /**
     * Install a loaded (and caller-validated) set as the live
     * registry. Kept separate from {@link #loadDirectory} so the
     * Bukkit layer can prune entries that fail live-only checks
     * (materials) and can reject an empty reload BEFORE anything
     * swaps.
     */
    public synchronized void installLoaded(Map<String, EntryDef> loaded) {
        this.entries = Map.copyOf(loaded);
    }

    /**
     * Register an entry for the caller's own lifetime (the Halloween
     * toggles pattern). Returns null on success, or the refusal
     * reason. A collision with an existing file or API id is refused.
     */
    public synchronized String registerApi(EntryDef def) {
        if (def == null || def.id() == null || def.id().isBlank()) {
            return "entry id is required";
        }
        if (entries.containsKey(def.id())) {
            return def.id() + ": id already registered ("
                    + entries.get(def.id()).origin() + ") — registration refused";
        }
        EntryDef copy = new EntryDef(def.id(), def.menu(), def.group(), def.order(),
                def.orderBedrock(), def.icon(), def.label(), def.lore(), def.action(),
                def.platforms(), def.requiresPlugin(), def.requiresPermission(),
                def.statusPlaceholder(), def.states(), def.visibleFrom(), def.visibleUntil(),
                EntryDef.Origin.API);
        apiEntries.put(copy.id(), copy);
        Map<String, EntryDef> merged = new LinkedHashMap<>(entries);
        merged.put(copy.id(), copy);
        this.entries = Map.copyOf(merged);
        return null;
    }

    /** Remove an API registration. File entries are never removed here. */
    public synchronized boolean unregisterApi(String id) {
        if (apiEntries.remove(id) == null) return false;
        Map<String, EntryDef> merged = new LinkedHashMap<>(entries);
        merged.remove(id);
        this.entries = Map.copyOf(merged);
        return true;
    }

    /** The current resolved namespace (immutable snapshot). */
    public Map<String, EntryDef> entries() {
        return entries;
    }

    public EntryDef entry(String id) {
        return entries.get(id);
    }

    /**
     * Semantic check the loader cannot do alone (it needs the live
     * material set): every entry icon — base and variants — must name
     * a real material. Returns one error string per problem.
     */
    public static List<String> validateMaterials(Map<String, EntryDef> defs,
                                                 Set<String> materials) {
        List<String> errors = new ArrayList<>();
        for (EntryDef def : defs.values()) {
            if (!materials.contains(def.icon())) {
                errors.add(def.id() + ": unknown material '" + def.icon() + "'");
            }
            for (Map.Entry<String, EntryDef.Variant> e : def.states().entrySet()) {
                String icon = e.getValue().icon();
                if (icon != null && !materials.contains(icon)) {
                    errors.add(def.id() + " states." + e.getKey()
                            + ": unknown material '" + icon + "'");
                }
            }
        }
        return errors;
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
