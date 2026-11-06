package com.winkreyes.excelcosmetics.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * UUID-keyed store of every player's cosmetic state. UUID-keying
 * is what makes grant-while-offline a non-event: the shop can
 * grant a player who is not online and the state is simply
 * waiting for them at next login. Methods are synchronized —
 * chat rendering reads from async threads while commands write
 * on the main thread.
 */
public final class GrantStore {

    private final Map<UUID, PlayerState> states = new LinkedHashMap<>();

    /** State for reads; null when the player has never had one. */
    public synchronized PlayerState peek(UUID uuid) {
        return states.get(uuid);
    }

    /** State for writes; creates an empty one on first use. */
    public synchronized PlayerState state(UUID uuid) {
        return states.computeIfAbsent(uuid, k -> new PlayerState());
    }

    public synchronized Grant grant(UUID uuid, Kind kind, String id, long now,
                                    long durationMillis, boolean permanent) {
        return state(uuid).applyGrant(kind, id, now, durationMillis, permanent);
    }

    /** Snapshot one player's state (0.1.3); null when no state exists. */
    public synchronized PlayerState.Snapshot snapshotPlayer(UUID uuid) {
        PlayerState s = states.get(uuid);
        return s == null ? null : s.snapshot();
    }

    /** Restore a snapshot exactly (0.1.3). A null snapshot means the
     *  player had NO state before — drop any state created since. */
    public synchronized void restorePlayer(UUID uuid, PlayerState.Snapshot snap) {
        if (snap == null) { states.remove(uuid); return; }
        state(uuid).restore(snap);
    }

    /** Drop a player's state outright (0.1.3 first-purchase compensation). */
    public synchronized void drop(UUID uuid) { states.remove(uuid); }

    /**
     * Grant + persist as one honest step (0.1.3): snapshot BEFORE
     * granting; if persist returns false, restore the snapshot
     * exactly (synchronously, inside this synchronized call, before
     * returning false) so false truly means nothing changed.
     * Restore also runs if the grant itself throws.
     */
    public synchronized boolean grantAndPersist(UUID uuid, Kind kind, String id, long now,
            long durationMillis, boolean permanent, java.util.function.BooleanSupplier persist) {
        PlayerState.Snapshot snap = snapshotPlayer(uuid);
        try {
            state(uuid).applyGrant(kind, id, now, durationMillis, permanent);
        } catch (RuntimeException e) {
            restorePlayer(uuid, snap);
            throw e;
        }
        boolean ok;
        try { ok = persist.getAsBoolean(); } catch (RuntimeException e) { ok = false; }
        if (!ok) restorePlayer(uuid, snap);
        return ok;
    }

    public synchronized boolean revoke(UUID uuid, Kind kind, String id) {
        PlayerState s = states.get(uuid);
        return s != null && s.revoke(kind, id);
    }

    /** Normalize every state (drop lapsed grants). Returns the
     *  number of grants dropped across all players. */
    public synchronized int pruneAll(long now) {
        int removed = 0;
        for (PlayerState s : states.values()) removed += s.normalize(now);
        return removed;
    }

    public synchronized Map<UUID, PlayerState> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(states));
    }

    public synchronized int playerCount() { return states.size(); }

    /** Drop all in-memory state (reload path — the file is re-read
     *  into this same instance so live listeners keep working). */
    public synchronized void clear() { states.clear(); }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
