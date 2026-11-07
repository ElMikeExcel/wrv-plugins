package com.winkreyes.excelautosort.store;

import com.winkreyes.excelautosort.core.BufferCore;

import java.util.UUID;

/**
 * One stack waiting in a player's recovery buffer (SPEC §6.2): the full
 * serialized ItemStack (Base64) plus the facts the GUI and the Protect
 * records need without deserializing — material key, count, when it was
 * removed, when it expires, which chest it came from, and why.
 */
public final class BufferEntry {

    public final String id;
    public final UUID owner;
    public final String itemB64;
    public final String materialKey;
    public final int count;
    public final long removedAtMs;
    public final long expiresAtMs;
    public final String chestKey;
    /** player | hopper | arm-sweep (SPEC §6.3 actor kinds). */
    public final String cause;

    public BufferEntry(String id, UUID owner, String itemB64, String materialKey, int count,
                       long removedAtMs, long expiresAtMs, String chestKey, String cause) {
        this.id = id;
        this.owner = owner;
        this.itemB64 = itemB64;
        this.materialKey = materialKey;
        this.count = count;
        this.removedAtMs = removedAtMs;
        this.expiresAtMs = expiresAtMs;
        this.chestKey = chestKey;
        this.cause = cause;
    }

    public BufferCore.TimedEntry timed() {
        return new BufferCore.TimedEntry(id, removedAtMs, expiresAtMs);
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
