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

    // Max Organization (SPEC §15): purchases and network moves. The
    // network cause is distinguishable by type alone — a chain query
    // on the items stream never confuses a network move with a filter
    // removal or a claim.
    public static final String MAXORG_PURCHASE = "autosort_maxorg_purchase";
    public static final String NETWORK_TAKE = "autosort_network_take";
    public static final String NETWORK_DEPOSIT = "autosort_network_deposit";

    // Admin grant/revoke of Max Organization (0.2.1): staff corrections
    // from the console, distinguishable from purchases by type alone —
    // no money moved, so they never wear the purchase type.
    public static final String MAXORG_ADMIN_GRANT = "autosort_maxorg_admin_grant";
    public static final String MAXORG_ADMIN_REVOKE = "autosort_maxorg_admin_revoke";

    /** Actor kinds for a removal (SPEC §6.3 table). */
    public static final String KIND_PLAYER = "player";
    public static final String KIND_HOPPER = "hopper";
    public static final String KIND_ARM_SWEEP = "arm-sweep";

    /** Purchase kinds for a Max Organization unlock (SPEC §15.2). */
    public static final String PURCHASE_RENT = "rent";
    public static final String PURCHASE_BUY = "buy";

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

    /**
     * A Max Organization purchase (SPEC §15.2): actor, kind (rent |
     * buy), amount actually charged, and the access state AFTER the
     * purchase (owned flag + rent expiry; 0 expiry on a buy). No
     * location base — the purchase happens wherever the player stands
     * is not the fact that matters; the money is.
     */
    public static Map<String, Object> maxOrgPurchase(
            String actorUuid, String actorName, String kind, double amount,
            boolean ownedAfter, long rentUntilAfterMs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("actor", actorUuid);
        m.put("actor_name", actorName);
        m.put("kind", kind);
        m.put("amount", amount);
        m.put("owned_after", ownedAfter);
        m.put("rent_until_ms", rentUntilAfterMs);
        m.put("via", "max-org");
        return m;
    }

    /**
     * An admin grant or revoke of Max Organization (0.2.1): who ran it
     * ({@code actor} = the admin's UUID, or the literal {@code console}),
     * who it landed on (target UUID + name), and the access state
     * BEFORE and AFTER (owned flag + rent expiry each side), so the
     * correction is reconstructible from the stream alone. {@code via}
     * is {@code admin} — never {@code max-org} (no purchase happened).
     */
    public static Map<String, Object> maxOrgAdmin(
            String actorLabel, String targetUuid, String targetName,
            boolean ownedBefore, long rentUntilBeforeMs,
            boolean ownedAfter, long rentUntilAfterMs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("actor", actorLabel);
        m.put("target", targetUuid);
        m.put("target_name", targetName);
        m.put("owned_before", ownedBefore);
        m.put("rent_until_before_ms", rentUntilBeforeMs);
        m.put("owned_after", ownedAfter);
        m.put("rent_until_after_ms", rentUntilAfterMs);
        m.put("via", "admin");
        return m;
    }

    /** A stack taken OUT of a network chest into the player's hands (§15.5). */
    public static Map<String, Object> networkTake(
            String world, int x, int y, int z, String chestKey,
            String actorUuid, String actorName,
            String chestOwnerUuid, String itemKey, int count) {
        Map<String, Object> m = base(world, x, y, z, chestKey);
        m.put("actor", actorUuid);
        m.put("actor_name", actorName);
        m.put("chest_owner", chestOwnerUuid);
        m.put("item", itemKey);
        m.put("count", count);
        m.put("via", "network-view");
        return m;
    }

    /** A stack smart-deposited from the player's hands INTO a network chest (§15.5). */
    public static Map<String, Object> networkDeposit(
            String world, int x, int y, int z, String chestKey,
            String actorUuid, String actorName,
            String chestOwnerUuid, String itemKey, int count) {
        Map<String, Object> m = base(world, x, y, z, chestKey);
        m.put("actor", actorUuid);
        m.put("actor_name", actorName);
        m.put("chest_owner", chestOwnerUuid);
        m.put("item", itemKey);
        m.put("count", count);
        m.put("via", "network-view");
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
