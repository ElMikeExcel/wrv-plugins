package com.winkreyes.exceltrainingwheels.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bedrock loading-screen grace window (0.1.6, Mike Oct 25, 2026).
 *
 * The Bedrock loading-screen hit: a Bedrock (Floodgate) player is
 * still on a loading screen after joining, respawning or changing
 * world, but is already in the world and can be hit. For a short
 * window after those moments incoming damage is cancelled, except
 * causes that must still apply so nobody gets stuck (VOID, KILL,
 * SUICIDE, and LAVA unless configured off). The window ends early
 * when the player attacks something or breaks a block.
 *
 * Pure logic with a caller-supplied millisecond clock; the Bukkit
 * glue owns Bedrock detection, events and the action bar.
 */
public final class BedrockGrace {

    public static final long DEFAULT_SECONDS = 4L;
    public static final long MAX_SECONDS = 30L;

    private final long windowMillis;
    private final Map<UUID, Long> until = new HashMap<>();

    public BedrockGrace(long windowMillis) {
        this.windowMillis = Math.max(0L, windowMillis);
    }

    /** Config seconds -> 0..MAX_SECONDS (0 = window off). */
    public static long sanitizeSeconds(long raw) {
        if (raw < 0) return 0L;
        return Math.min(raw, MAX_SECONDS);
    }

    /** Floodgate's own UUID scheme: Bedrock players have most-significant bits 0. */
    public static boolean looksLikeFloodgateUuid(UUID uuid) {
        return uuid != null && uuid.getMostSignificantBits() == 0L;
    }

    /** Damage causes that still apply inside the window (Bukkit DamageCause names). */
    public static boolean causeStillApplies(String cause, boolean allowLava) {
        if (cause == null) return false;
        return switch (cause) {
            case "VOID", "KILL", "SUICIDE" -> true;
            case "LAVA" -> allowLava;
            default -> false;
        };
    }

    public long windowMillis() {
        return windowMillis;
    }

    /** Starts (or restarts) the window. Returns false when the window is off. */
    public boolean start(UUID uuid, long nowMillis) {
        if (uuid == null || windowMillis <= 0L) return false;
        until.put(uuid, nowMillis + windowMillis);
        return true;
    }

    public boolean active(UUID uuid, long nowMillis) {
        if (uuid == null) return false;
        Long end = until.get(uuid);
        if (end == null) return false;
        if (nowMillis >= end) {
            until.remove(uuid);
            return false;
        }
        return true;
    }

    /** Ends the window now (attack, block break, quit). True if one was running. */
    public boolean end(UUID uuid) {
        return uuid != null && until.remove(uuid) != null;
    }

    public int size() {
        return until.size();
    }

    public void clear() {
        until.clear();
    }
}
