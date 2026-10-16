package com.winkreyes.rewardguard;

import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/** Own namespaced metadata only; no entity removal, damage, loot or XP mutation. */
public final class ProvenanceStore {
    private final NamespacedKey origin = new NamespacedKey("excelrewardguard", "origin_v1");
    private final NamespacedKey denied = new NamespacedKey("excelrewardguard", "denied_v1");
    private final NamespacedKey reason = new NamespacedKey("excelrewardguard", "reason_v1");
    private static final Set<String> UNRECORDED_COMMON_ORIGINS = Set.of("NATURAL", "DEFAULT", "CHUNK_GEN");

    public FarmVerdict inspect(PersistentDataContainer data) {
        if (data == null) return FarmVerdict.unavailable();
        // Any existing denial marker is sticky, including a malformed value/type: never silently clear it.
        if (data.has(denied)) {
            String value = data.get(reason, PersistentDataType.STRING);
            return new FarmVerdict("DENY", value == null ? "stored_denial" : value);
        }
        return FarmVerdict.unknown();
    }

    public void recordSpawn(PersistentDataContainer data, String spawnReason) {
        if (data == null || spawnReason == null || spawnReason.isBlank()) return;
        if (!UNRECORDED_COMMON_ORIGINS.contains(spawnReason) && !data.has(origin)) {
            data.set(origin, PersistentDataType.STRING, spawnReason);
        }
        FarmVerdict verdict = FarmVerdict.forOrigin(spawnReason);
        if (verdict.decision().equals("DENY")) markDenied(data, verdict.reason());
    }

    public void copy(PersistentDataContainer source, PersistentDataContainer target) {
        if (source == null || target == null) return;
        String previousOrigin = source.get(origin, PersistentDataType.STRING);
        if (previousOrigin != null && !target.has(origin)) target.set(origin, PersistentDataType.STRING, previousOrigin);
        FarmVerdict verdict = inspect(source);
        if (verdict.decision().equals("DENY")) markDenied(target, verdict.reason());
    }

    private void markDenied(PersistentDataContainer data, String denialReason) {
        if (!data.has(denied)) {
            data.set(denied, PersistentDataType.BYTE, (byte) 1);
            data.set(reason, PersistentDataType.STRING, denialReason);
        }
    }
}
