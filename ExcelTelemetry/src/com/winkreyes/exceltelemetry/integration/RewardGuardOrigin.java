package com.winkreyes.exceltelemetry.integration;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

/**
 * Reads ExcelRewardGuard's provenance marks off an entity's PDC -
 * the exact keys its ProvenanceStore writes (excelrewardguard:
 * origin_v1 / denied_v1), verified against the 1.2/1.3 source.
 * RewardGuard deliberately does NOT record common origins
 * (NATURAL / DEFAULT / CHUNK_GEN), so an absent mark reads as
 * "natural" - its own semantics, reused rather than duplicated.
 * Pure Bukkit API (a key string is not a code dependency); absent
 * plugin simply means absent marks.
 */
public final class RewardGuardOrigin {
    private final NamespacedKey originKey = new NamespacedKey("excelrewardguard", "origin_v1");
    private final NamespacedKey deniedKey = new NamespacedKey("excelrewardguard", "denied_v1");

    public record Origin(String originClass, String raw, boolean rgDenied) {}

    /** PDC first; falls back to the spawn reason Telemetry stashed at spawn time. */
    public Origin classify(Entity entity, String stashedSpawnReason) {
        String raw = null;
        boolean denied = false;
        if (entity != null) {
            try {
                var pdc = entity.getPersistentDataContainer();
                if (pdc.has(deniedKey)) denied = true;
                if (pdc.has(originKey, PersistentDataType.STRING)) {
                    raw = pdc.get(originKey, PersistentDataType.STRING);
                }
            } catch (Throwable ignored) {
                // a damaged PDC must never cost us the kill line
            }
        }
        if (raw == null) raw = stashedSpawnReason;
        return new Origin(classOf(raw), raw, denied);
    }

    static String classOf(String raw) {
        if (raw == null || raw.isBlank()) return "natural";
        return switch (raw) {
            case "NATURAL", "DEFAULT", "CHUNK_GEN", "VILLAGE_DEFENSE", "VILLAGE_INVASION" -> "natural";
            case "SPAWNER" -> "spawner";
            case "SPAWNER_EGG" -> "spawner_egg";
            case "NETHER_PORTAL" -> "nether_portal";
            default -> "other";
        };
    }
}
