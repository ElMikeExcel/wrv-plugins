package com.winkreyes.exceldeathrecord.core;

/**
 * Who a destroyed item belonged to, resolved as far as the
 * platform knows — never guessed.
 *
 * Two knowledge sources, in trust order:
 *   1. the item entity's own owner (Item#getOwner — the player
 *      who dropped it; death drops carry the deceased);
 *   2. the plugin's drop tracking (PlayerDropItemEvent memory:
 *      entity UUID -> who dropped it and when).
 * When both know, they name the same latest dropper and source
 * 1 wins. When neither knows, the owner is recorded as unknown
 * (empty strings) — an honest gap, not a guess.
 *
 * The drop-tracking entry also carries the age basis: a tracked
 * drop's world age is (destruction time - drop time); an
 * untracked item falls back to its lived ticks, which the
 * adapter supplies as the age when it has nothing better.
 */
public final class Attribution {

    /** What the drop tracker remembers about one dropped item entity. */
    public static final class DropInfo {
        public String ownerUuid = "";
        public String ownerName = "";
        public long droppedEpochMillis = 0;
    }

    /** Resolution outcome: empty uuid/name means unknown. */
    public static final class Owner {
        public String uuid = "";
        public String name = "";
    }

    private Attribution() {}

    public static Owner resolve(String entityOwnerUuid, String entityOwnerName, DropInfo tracked) {
        Owner o = new Owner();
        if (entityOwnerUuid != null && !entityOwnerUuid.isEmpty()) {
            o.uuid = entityOwnerUuid;
            o.name = entityOwnerName == null ? "" : entityOwnerName;
            return o;
        }
        if (tracked != null && tracked.ownerUuid != null && !tracked.ownerUuid.isEmpty()) {
            o.uuid = tracked.ownerUuid;
            o.name = tracked.ownerName == null ? "" : tracked.ownerName;
            return o;
        }
        return o;
    }

    /**
     * 0.3.0 resolution with the two persistent sources added, in
     * trust order: the entity's owner, then the owner stamp this
     * plugin writes into the item ENTITY's persistent data when a
     * player drops it (or dies and it spills) - which survives chunk
     * unloads and restarts - then the entity's thrower, then the
     * in-memory drop tracker. Unknown when none knows.
     */
    public static Owner resolve(String entityOwnerUuid, String entityOwnerName,
                                String stampUuid, String stampName,
                                String throwerUuid, String throwerName, DropInfo tracked) {
        if (entityOwnerUuid != null && !entityOwnerUuid.isEmpty()) return resolve(entityOwnerUuid, entityOwnerName, null);
        if (stampUuid != null && !stampUuid.isEmpty()) return resolve(stampUuid, stampName, null);
        if (throwerUuid != null && !throwerUuid.isEmpty()) return resolve(throwerUuid, throwerName, null);
        return resolve(null, null, tracked);
    }

    /** World age in seconds at destruction: tracked drops age by
     *  the clock; anything else by the adapter's fallback (lived
     *  ticks / 20), or -1 when neither is known. */
    public static long ageSeconds(DropInfo tracked, long destroyedEpochMillis, long fallbackSeconds) {
        if (tracked != null && tracked.droppedEpochMillis > 0 && destroyedEpochMillis >= tracked.droppedEpochMillis) {
            return (destroyedEpochMillis - tracked.droppedEpochMillis) / 1000L;
        }
        return fallbackSeconds;
    }

    /** The despawn valuable-filter: a naturally despawning item
     *  reaches the Burn Ledger only when it was worth something —
     *  enchanted (incl. stored enchants) or custom-named. Lava,
     *  fire, and void records ignore this filter entirely: every
     *  destruction by those causes is the audit case. */
    public static boolean isValuable(DeathRecord.ItemEntry item) {
        return item != null && (!item.enchants.isEmpty() || !item.customName.isEmpty());
    }
}
