package excelemfixes.arcane;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Reflection bridge into EliteMobs' timed-modifier state.
 *
 * EliteMobs 10.9.10 stores timed outgoing-damage buffs (e.g. Spell Echo)
 * in a package-private TimedCombatModifiers instance with no public
 * query API. This bridge locates that one instance by a bounded
 * object-graph walk from the EliteMobs plugin instance and reads the
 * caster's modifier list through the record's own public accessors.
 *
 * Fail-safe contract: every public method here swallows failures and
 * degrades to "no buff" (multiplier 1.0). The bridge can never break
 * EliteMobs or the hit path; worst case the layer runs scale-only.
 */
final class EmInternals {

    private static final String MODIFIERS_CLASS =
            "com.magmaguy.elitemobs.advancedcombat.abilities.TimedCombatModifiers";
    private static final int MAX_DEPTH = 8;
    private static final int MAX_VISITED = 200_000;
    private static final int MAX_CONTAINER_ELEMENTS = 5_000;
    private static final long RESOLVE_RETRY_NANOS = 30_000_000_000L; // 30 s

    private final Logger logger;

    private Object modifiersInstance;
    private Field outgoingModifiersField;
    private Field closedField;
    private Method multiplierMethod;
    private Method expiresAtMethod;
    private Method domainMethod;
    private long lastResolveAttemptNanos;
    private boolean warnedUnavailable;

    EmInternals(Logger logger) {
        this.logger = logger;
    }

    boolean isReady() {
        return modifiersInstance != null;
    }

    /**
     * The caster's strongest active outgoing-damage modifier (ANY/SPELL
     * domains), or 1.0 when none applies or the bridge is unavailable.
     * Mirrors EliteMobs' strongest-only application semantics.
     */
    double strongestOutgoingMultiplier(Object eliteMobsPlugin, UUID playerId) {
        try {
            if (modifiersInstance == null) {
                long now = System.nanoTime();
                if (now - lastResolveAttemptNanos < RESOLVE_RETRY_NANOS) return 1.0;
                lastResolveAttemptNanos = now;
                resolve(eliteMobsPlugin);
                if (modifiersInstance == null) return 1.0;
            }
            if (closedField != null && closedField.getBoolean(modifiersInstance)) {
                // EliteMobs rebuilt its combat runtime (e.g. /em reload):
                // drop the stale instance and re-resolve next call.
                modifiersInstance = null;
                return 1.0;
            }
            Object mapObject = outgoingModifiersField.get(modifiersInstance);
            if (!(mapObject instanceof Map<?, ?> map)) return 1.0;
            Object listObject = map.get(playerId);
            if (!(listObject instanceof List<?> list) || list.isEmpty()) return 1.0;
            List<CompensationMath.BuffEntry> entries = new ArrayList<>(list.size());
            for (Object modifier : list) {
                if (modifier == null) continue;
                double multiplier = ((Number) multiplierMethod.invoke(modifier)).doubleValue();
                long expiresAt = ((Number) expiresAtMethod.invoke(modifier)).longValue();
                Object domain = domainMethod.invoke(modifier);
                String domainName = domain instanceof Enum<?> e ? e.name() : String.valueOf(domain);
                entries.add(new CompensationMath.BuffEntry(multiplier, expiresAt, domainName));
            }
            return CompensationMath.strongestBuff(entries, System.nanoTime());
        } catch (Throwable failure) {
            modifiersInstance = null;
            if (!warnedUnavailable) {
                warnedUnavailable = true;
                logger.warning("Arcane Bolt module: EliteMobs buff state became unreadable ("
                        + failure.getClass().getSimpleName()
                        + "); continuing with buff factor 1.0 until it can be re-read.");
            }
            return 1.0;
        }
    }

    /** Bounded BFS over EliteMobs' own object graph for the one instance. */
    private void resolve(Object root) {
        if (root == null) return;
        try {
            Queue<Object> queue = new ArrayDeque<>();
            IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
            Map<Object, Integer> depths = new IdentityHashMap<>();
            queue.add(root);
            visited.put(root, Boolean.TRUE);
            depths.put(root, 0);
            int seen = 0;
            while (!queue.isEmpty() && seen < MAX_VISITED) {
                Object current = queue.poll();
                seen++;
                if (current.getClass().getName().equals(MODIFIERS_CLASS)) {
                    bind(current);
                    return;
                }
                int depth = depths.getOrDefault(current, 0);
                if (depth >= MAX_DEPTH) continue;
                for (Object value : fieldValues(current)) {
                    enqueue(value, depth + 1, queue, visited, depths);
                }
            }
        } catch (Throwable failure) {
            logger.warning("Arcane Bolt module: could not locate EliteMobs' timed-modifier state ("
                    + failure.getClass().getSimpleName()
                    + "); buff application disabled, damage scale still active.");
        }
    }

    private void enqueue(Object value, int depth, Queue<Object> queue,
                         IdentityHashMap<Object, Boolean> visited, Map<Object, Integer> depths) {
        if (value == null || visited.containsKey(value)) return;
        Class<?> type = value.getClass();
        if (type.getName().startsWith("com.magmaguy.")) {
            visited.put(value, Boolean.TRUE);
            depths.put(value, depth);
            queue.add(value);
        } else if (value instanceof Map<?, ?> map) {
            visited.put(value, Boolean.TRUE);
            int count = 0;
            for (Object element : map.values()) {
                if (++count > MAX_CONTAINER_ELEMENTS) break;
                enqueue(element, depth, queue, visited, depths);
            }
        } else if (value instanceof Collection<?> collection) {
            visited.put(value, Boolean.TRUE);
            int count = 0;
            for (Object element : collection) {
                if (++count > MAX_CONTAINER_ELEMENTS) break;
                enqueue(element, depth, queue, visited, depths);
            }
        } else if (type.isArray() && !type.getComponentType().isPrimitive()) {
            visited.put(value, Boolean.TRUE);
            Object[] array = (Object[]) value;
            int limit = Math.min(array.length, MAX_CONTAINER_ELEMENTS);
            for (int i = 0; i < limit; i++) {
                enqueue(array[i], depth, queue, visited, depths);
            }
        }
    }

    /** Instance field values of EliteMobs classes only; never statics. */
    private List<Object> fieldValues(Object owner) {
        List<Object> values = new ArrayList<>();
        Class<?> type = owner.getClass();
        while (type != null && type.getName().startsWith("com.magmaguy.")) {
            for (Field field : type.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                if (field.getType().isPrimitive() || field.getType() == String.class) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(owner);
                    if (value != null) values.add(value);
                } catch (Throwable ignored) {
                    // Unreadable field: skip it; the walk has other paths.
                }
            }
            type = type.getSuperclass();
        }
        return values;
    }

    private void bind(Object instance) throws ReflectiveOperationException {
        Class<?> type = instance.getClass();
        Field outgoing = type.getDeclaredField("outgoingModifiers");
        outgoing.setAccessible(true);
        Field closed = null;
        try {
            closed = type.getDeclaredField("closed");
            closed.setAccessible(true);
        } catch (NoSuchFieldException ignored) {
            // Staleness check is best-effort only.
        }
        // PlayerModifier is a record; read one entry to cache its accessors.
        Method multiplier = null;
        Method expiresAt = null;
        Method domain = null;
        Object mapObject = outgoing.get(instance);
        if (mapObject instanceof Map<?, ?> map) {
            for (Object listObject : map.values()) {
                if (listObject instanceof List<?> list && !list.isEmpty() && list.get(0) != null) {
                    Class<?> recordType = list.get(0).getClass();
                    multiplier = recordType.getMethod("multiplier");
                    expiresAt = recordType.getMethod("expiresAtNanos");
                    domain = recordType.getMethod("domain");
                    multiplier.setAccessible(true);
                    expiresAt.setAccessible(true);
                    domain.setAccessible(true);
                    break;
                }
            }
        }
        if (multiplier == null) {
            // No modifier has ever been stored yet: find the record class
            // by name so the accessors are ready before the first buff.
            Class<?> recordType = Class.forName(
                    "com.magmaguy.elitemobs.advancedcombat.abilities.TimedCombatModifiers$PlayerModifier",
                    false, type.getClassLoader());
            multiplier = recordType.getMethod("multiplier");
            expiresAt = recordType.getMethod("expiresAtNanos");
            domain = recordType.getMethod("domain");
            multiplier.setAccessible(true);
            expiresAt.setAccessible(true);
            domain.setAccessible(true);
        }
        this.modifiersInstance = instance;
        this.outgoingModifiersField = outgoing;
        this.closedField = closed;
        this.multiplierMethod = multiplier;
        this.expiresAtMethod = expiresAt;
        this.domainMethod = domain;
        logger.info("Arcane Bolt module: EliteMobs buff state located; buff application is live.");
    }
}
