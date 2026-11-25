package com.winkreyes.excelprotect;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

/**
 * ExcelProtect 0.1.7 — kill-line origin taxonomy, ported field-for-field from
 * ExcelTelemetry's RewardGuardOrigin (the signal Telemetry's kill records use;
 * gate G3 requires the two to stay mechanically comparable, so this class is a
 * deliberate twin, not a reinvention). Reads ExcelRewardGuard's provenance
 * marks off an entity's PDC — the exact keys its ProvenanceStore writes
 * (excelrewardguard:origin_v1 / denied_v1). RewardGuard deliberately does NOT
 * record common origins (NATURAL / DEFAULT / CHUNK_GEN), so an absent mark
 * reads as "natural" — its own semantics, reused rather than duplicated.
 * Pure Bukkit API (a key string is not a code dependency); absent plugin
 * simply means absent marks. Excel — Wink Reyes Valley
 */
public final class RgOrigin {
    private static final NamespacedKey ORIGIN_KEY = new NamespacedKey("excelrewardguard", "origin_v1");
    private static final NamespacedKey DENIED_KEY = new NamespacedKey("excelrewardguard", "denied_v1");

    private RgOrigin() {}

    public record Origin(String originClass, String raw, boolean rgDenied) {}

    /** PDC first; falls back to the spawn reason Protect stashed at spawn time
     *  (Entities.origins stores it lowercased, so it is uppercased back to the
     *  raw Bukkit form the taxonomy switches on). */
    public static Origin classify(Entity entity, String stashedSpawnReasonLower) {
        String raw = null;
        boolean denied = false;
        if (entity != null) {
            try {
                var pdc = entity.getPersistentDataContainer();
                if (pdc.has(DENIED_KEY)) denied = true;
                if (pdc.has(ORIGIN_KEY, PersistentDataType.STRING)) {
                    raw = pdc.get(ORIGIN_KEY, PersistentDataType.STRING);
                }
            } catch (Throwable ignored) {
                // a damaged PDC must never cost us the kill line
            }
        }
        if (raw == null && stashedSpawnReasonLower != null && !stashedSpawnReasonLower.isBlank()) {
            raw = stashedSpawnReasonLower.toUpperCase(java.util.Locale.ROOT);
        }
        return new Origin(classOf(raw), raw, denied);
    }

    /** The taxonomy, character-identical to Telemetry's RewardGuardOrigin.classOf. */
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
