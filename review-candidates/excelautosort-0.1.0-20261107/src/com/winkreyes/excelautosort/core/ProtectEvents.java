package com.winkreyes.excelautosort.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The shape of every record ExcelAutoSort publishes to ExcelProtect
 * (SPEC §6.3) — built here, in plain JDK types, so the field tables are
 * unit-tested off-server and the one adapter class that touches
 * Protect's API only carries the maps across (the BossTrial
 * ProtectTableShape pattern). Protect stamps the envelope (ts, ts_ms,
 * type) itself; the emitter supplies the location fields (world/x/y/z)
 * and the event fields below. Stream is always {@link #STREAM}.
 */
public final class ProtectEvents {

    private ProtectEvents() {}

    public static final String STREAM = "items";

    public static final String FILTER_DELETE = "autosort_filter_delete";
    public static final String FILTER_ARM = "autosort_filter_arm";
    public static final String UNMARK = "autosort_unmark";
    public static final String CHEST_RESHAPED = "autosort_chest_reshaped";
    public static final String FILTER_SUSPEND = "autosort_filter_suspend";
    public static final String FILTER_RESUME = "autosort_filter_resume";
    public static final String RECOVER_CLAIM = "autosort_recover_claim";
    public static final String BUFFER_PURGE = "autosort_buffer_purge";

    /** Actor kinds for a removal (SPEC §6.3 table). */
    public static final String KIND_PLAYER = "player";
    public static final String KIND_HOPPER = "hopper";
    public static final String KIND_ARM_SWEEP = "arm-sweep";

    private static Map<String, Object> base(String world, int x, int y, int z, String chestKey) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("world", world);
        m.put("x", x);
        m.put("y", y);
        m.put("z", z);
        m.put("chest_key", chestKey);
        return m;
    }

    /**
     * One stack removed by the filter (SPEC §6.3 field table, complete):
     * actor (UUID or null) + actor_name, actor_kind, chest_owner (+name),
     * chest_key, mode, item (material key) + item_data (serialized stack,
     * Base64), count, buffer_id, buffer_owner, expires_ms — over the
     * location base.
     */
    public static Map<String, Object> filterDelete(
            String world, int x, int y, int z, String chestKey,
            String actorUuid, String actorName, String actorKind,
            String chestOwnerUuid, String chestOwnerName,
            String mode, String itemKey, String itemDataB64, int count,
            String bufferId, String bufferOwnerUuid, long expiresMs) {
        Map<String, Object> m = base(world, x, y, z, chestKey);
        m.put("actor", actorUuid);
        m.put("actor_name", actorName);
        m.put("actor_kind", actorKind);
        m.put("chest_owner", chestOwnerUuid);
        m.put("chest_owner_name", chestOwnerName);
        m.put("mode", mode);
        m.put("item", itemKey);
        m.put("item_data", itemDataB64);
        m.put("count", count);
        m.put("buffer_id", bufferId);
        m.put("buffer_owner", bufferOwnerUuid);
        m.put("expires_ms", expiresMs);
        return m;
    }

    /** A filter armed (mode + list took effect), with the sweep summary. */
    public static Map<String, Object> filterArm(
            String world, int x, int y, int z, String chestKey,
            String actorUuid, String actorName,
            String chestOwnerUuid, String mode, int listSize,
            int sweptStacks, int sweptItems, boolean resumed) {
        Map<String, Object> m = base(world, x, y, z, chestKey);
        m.put("actor", actorUuid);
        m.put("actor_name", actorName);
        m.put("chest_owner", chestOwnerUuid);
        m.put("mode", mode);
        m.put("list_size", listSize);
        m.put("swept_stacks", sweptStacks);
        m.put("swept_items", sweptItems);
        m.put("resumed", resumed);
        return m;
    }

    /** A record removed. Causes: block-break | block-gone (§6.4, §9). */
    public static Map<String, Object> unmark(
            String world, int x, int y, int z, String chestKey,
            String actorUuid, String chestOwnerUuid, String cause) {
        Map<String, Object> m = base(world, x, y, z, chestKey);
        m.put("actor", actorUuid);
        m.put("chest_owner", chestOwnerUuid);
        m.put("cause", cause);
        return m;
    }

    /** A record's shape changed (double-chest half lost/gained, §5.5). */
    public static Map<String, Object> chestReshaped(
            String world, int x, int y, int z, String chestKey,
            String chestOwnerUuid, String detail, int keyCount) {
        Map<String, Object> m = base(world, x, y, z, chestKey);
        m.put("chest_owner", chestOwnerUuid);
        m.put("detail", detail);
        m.put("key_count", keyCount);
        return m;
    }

    /** Suspension or resumption (§6.4). Resumption reuses FILTER_RESUME. */
    public static Map<String, Object> suspend(
            String world, int x, int y, int z, String chestKey,
            String chestOwnerUuid, String reason) {
        Map<String, Object> m = base(world, x, y, z, chestKey);
        m.put("chest_owner", chestOwnerUuid);
        m.put("reason", reason);
        return m;
    }

    /** A stack claimed back out of a recovery buffer (§6.2). */
    public static Map<String, Object> recoverClaim(
            String claimerUuid, String claimerName, String bufferOwnerUuid,
            String bufferId, String itemKey, int count, String chestKey) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("actor", claimerUuid);
        m.put("actor_name", claimerName);
        m.put("buffer_owner", bufferOwnerUuid);
        m.put("buffer_id", bufferId);
        m.put("item", itemKey);
        m.put("count", count);
        m.put("chest_key", chestKey);
        return m;
    }

    /** A buffer entry permanently deleted (expiry or cap overflow, §6.2). */
    public static Map<String, Object> bufferPurge(
            String bufferOwnerUuid, String bufferId, String itemKey, int count,
            String reason, String chestKey) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("buffer_owner", bufferOwnerUuid);
        m.put("buffer_id", bufferId);
        m.put("item", itemKey);
        m.put("count", count);
        m.put("reason", reason);
        m.put("chest_key", chestKey);
        return m;
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
