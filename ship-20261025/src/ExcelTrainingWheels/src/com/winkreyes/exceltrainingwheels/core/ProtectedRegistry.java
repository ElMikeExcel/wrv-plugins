package com.winkreyes.exceltrainingwheels.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.UUID;

/**
 * The protected list: player UUID -> (damage factor, display name).
 * Keyed on UUID, never on name (Bedrock/Floodgate UUIDs are stable
 * per Xbox account even across Gamertag changes). Pure logic; the
 * Bukkit glue reads/writes the config representation.
 *
 * "Protected" means LISTED — a listed player is protected even at
 * factor 1.0 (the door guard cares about listed-ness, not the factor).
 */
public final class ProtectedRegistry {

    public record Entry(double factor, String name) {}

    public record ParseResult(ProtectedRegistry registry, List<String> errors) {}

    // Sorted by UUID string so /trainingwheels list output is stable.
    private final Map<UUID, Entry> entries = new TreeMap<>((a, b) -> a.toString().compareTo(b.toString()));

    public static boolean validFactor(double f) {
        return !Double.isNaN(f) && !Double.isInfinite(f) && f >= 0.0 && f <= 1.0;
    }

    /**
     * Build from the raw config representation:
     * uuid-string -> [factor-string, name-or-null]. Bad rows are
     * skipped and reported in errors (the glue logs them as warnings);
     * one bad row never sinks the whole list.
     */
    public static ParseResult fromRaw(Map<String, String[]> raw) {
        ProtectedRegistry registry = new ProtectedRegistry();
        List<String> errors = new ArrayList<>();
        if (raw == null) return new ParseResult(registry, errors);
        List<String> keys = new ArrayList<>(raw.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            String[] row = raw.get(key);
            String factorText = row != null && row.length > 0 ? row[0] : null;
            String name = row != null && row.length > 1 ? row[1] : null;
            UUID id;
            try {
                id = UUID.fromString(key.trim());
            } catch (Exception e) {
                errors.add("protected list: '" + key + "' is not a UUID - skipped");
                continue;
            }
            double factor;
            try {
                factor = Double.parseDouble(factorText == null ? "" : factorText.trim());
            } catch (Exception e) {
                errors.add("protected list: factor for " + key + " is not a number ('" + factorText + "') - skipped");
                continue;
            }
            if (!validFactor(factor)) {
                errors.add("protected list: factor for " + key + " is out of [0,1] (" + factorText + ") - skipped");
                continue;
            }
            registry.entries.put(id, new Entry(factor, name));
        }
        return new ParseResult(registry, errors);
    }

    /** Raw representation for persisting back to config (uuid -> [factor, name]). */
    public Map<String, String[]> toRaw() {
        Map<String, String[]> out = new LinkedHashMap<>();
        for (Map.Entry<UUID, Entry> e : entries.entrySet()) {
            out.put(e.getKey().toString(),
                    new String[]{Double.toString(e.getValue().factor()), e.getValue().name()});
        }
        return out;
    }

    public boolean isProtected(UUID id) {
        return id != null && entries.containsKey(id);
    }

    public OptionalDouble factor(UUID id) {
        Entry e = id == null ? null : entries.get(id);
        return e == null ? OptionalDouble.empty() : OptionalDouble.of(e.factor());
    }

    public Optional<Entry> entry(UUID id) {
        return Optional.ofNullable(id == null ? null : entries.get(id));
    }

    public void put(UUID id, double factor, String name) {
        if (id == null) throw new IllegalArgumentException("uuid is null");
        if (!validFactor(factor)) throw new IllegalArgumentException("factor out of [0,1]: " + factor);
        entries.put(id, new Entry(factor, name));
    }

    public boolean remove(UUID id) {
        return id != null && entries.remove(id) != null;
    }

    public int size() {
        return entries.size();
    }

    /** Unmodifiable snapshot, sorted by UUID string. */
    public Map<UUID, Entry> snapshot() {
        return Collections.unmodifiableMap(new TreeMap<>(entries));
    }
}
