package com.winkreyes.excelitems.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * The registry core (CUSTOM ITEM REGISTRY design law, Nov 17, 2026).
 * Items are registered once, by permanent ID; IDs are never reused.
 * Registration rejects a duplicate ID or a duplicate key outright —
 * a silent overwrite would re-point every shop, menu and loot entry
 * that references the ID, so it fails closed instead.
 *
 * <p>Identification is data-only and works from the marks an item
 * carries, so any plugin can recognize a custom item without trusting
 * its display name or lore (the house identity principle):
 * <ol>
 *   <li>primary mark — the excelitems:id value stamped at mint;</li>
 *   <li>legacy mark — the (namespace, key) pair the item's original
 *       plugin stamped before the registry existed, so already-held
 *       items stay recognized after migration.</li>
 * </ol>
 * A primary mark that names an unregistered ID identifies as empty
 * (never guessed from a legacy mark on the same item — a forged or
 * stale primary mark must not fall through to a second identity).
 *
 * <p>Pure JVM: the Bukkit layer reads the PDC and hands the raw mark
 * values here, which is what the plain-JVM suites pin.
 */
public final class ItemRegistry {

    private final Map<Integer, ItemDefinition> byId = new LinkedHashMap<>();
    private final Map<String, ItemDefinition> byKey = new LinkedHashMap<>();
    private final Map<String, ItemDefinition> byLegacy = new LinkedHashMap<>();

    /** Register one definition. Throws on duplicate ID or key. */
    public synchronized ItemDefinition register(ItemDefinition def) {
        if (def == null) throw new IllegalArgumentException("definition required");
        if (byId.containsKey(def.id()))
            throw new IllegalArgumentException("duplicate registry id " + def.id()
                    + " (held by " + byId.get(def.id()).key() + ", refused " + def.key() + ")");
        if (byKey.containsKey(def.key()))
            throw new IllegalArgumentException("duplicate registry key " + def.key()
                    + " (id " + byKey.get(def.key()).id() + ", refused id " + def.id() + ")");
        if (def.hasLegacyMark()) {
            String legacy = def.legacyNamespace() + ":" + def.legacyKey();
            ItemDefinition clash = byLegacy.get(legacy);
            if (clash != null && clash.id() != def.id())
                throw new IllegalArgumentException("legacy mark " + legacy
                        + " already claimed by " + clash.key() + ", refused " + def.key());
            byLegacy.put(legacy, def);
        }
        byId.put(def.id(), def);
        byKey.put(def.key(), def);
        return def;
    }

    public synchronized Optional<ItemDefinition> byId(int id) {
        return Optional.ofNullable(byId.get(id));
    }

    public synchronized Optional<ItemDefinition> byKey(String key) {
        return key == null ? Optional.empty() : Optional.ofNullable(byKey.get(key));
    }

    /** byId that fails loudly — for code paths where the ID is law. */
    public synchronized ItemDefinition requireId(int id) {
        ItemDefinition def = byId.get(id);
        if (def == null) throw new NoSuchElementException("unknown registry id: " + id);
        return def;
    }

    /** byKey that fails loudly. */
    public synchronized ItemDefinition requireKey(String key) {
        ItemDefinition def = key == null ? null : byKey.get(key);
        if (def == null) throw new NoSuchElementException("unknown registry key: " + key);
        return def;
    }

    /**
     * Identify an item from its raw marks. {@code primaryId} is the
     * excelitems:id value (null when the item carries no primary
     * mark); {@code legacyNamespace}/{@code legacyKey} describe ONE
     * candidate legacy mark to test — the Bukkit layer calls this
     * once per registered legacy mark it probes, or the registry's
     * {@link #identifyLegacy} directly. Kept single-mark so the core
     * never has to model the whole PDC.
     */
    public synchronized Optional<ItemDefinition> identify(Integer primaryId,
                                                          String legacyNamespace,
                                                          String legacyKey) {
        if (primaryId != null) return byId(primaryId);
        return identifyLegacy(legacyNamespace, legacyKey);
    }

    /** Identify from a legacy (namespace, key) mark alone. */
    public synchronized Optional<ItemDefinition> identifyLegacy(String namespace, String key) {
        if (namespace == null || key == null) return Optional.empty();
        return Optional.ofNullable(byLegacy.get(namespace + ":" + key));
    }

    /** The next never-used ID (max registered + 1; 1 when empty). */
    public synchronized int nextId() {
        int max = 0;
        for (int id : byId.keySet()) max = Math.max(max, id);
        return max + 1;
    }

    public synchronized int size() {
        return byId.size();
    }

    /** All definitions, in registration order. */
    public synchronized Collection<ItemDefinition> all() {
        return Collections.unmodifiableCollection(new ArrayList<>(byId.values()));
    }

    /**
     * Validate a config ID-assignment map (raw string keys, as YAML
     * delivers them) against this registry. The assignments are the
     * persistence shape of the law "IDs are permanent": config pins
     * which key owns which ID, and any drift between config and the
     * code-registered definitions is a problem to surface at enable,
     * never to silently absorb. Returns the problem list (empty =
     * valid): non-numeric ID, unknown ID, key mismatch, and any
     * registered ID missing from the map.
     */
    public synchronized List<String> validateAssignments(Map<String, String> raw) {
        List<String> problems = new ArrayList<>();
        if (raw == null) raw = Map.of();
        for (Map.Entry<String, String> e : raw.entrySet()) {
            int id;
            try {
                id = Integer.parseInt(e.getKey().trim());
            } catch (RuntimeException bad) {
                problems.add("assignment key is not a number: '" + e.getKey() + "'");
                continue;
            }
            ItemDefinition def = byId.get(id);
            if (def == null) {
                problems.add("assignment names unknown id " + id + " ('" + e.getValue() + "')");
            } else if (!def.key().equals(e.getValue())) {
                problems.add("assignment mismatch for id " + id + ": config says '"
                        + e.getValue() + "', registry says '" + def.key() + "'");
            }
        }
        for (ItemDefinition def : byId.values()) {
            if (!raw.containsKey(String.valueOf(def.id())))
                problems.add("registered id " + def.id() + " (" + def.key() + ") missing from assignments");
        }
        return problems;
    }
}
