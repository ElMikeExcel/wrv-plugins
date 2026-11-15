package com.winkreyes.excelattunement.core;

/**
 * Per-player rate limit for the ward line, so a passive spell that
 * keeps trying to fire (aqualung underwater) or a player spamming the
 * wand does not flood the action bar or chat. Clock injected.
 */
public final class WardNotice {
    private final long cooldownMs;
    private long lastAt = Long.MIN_VALUE;

    public WardNotice(long cooldownSeconds) {
        this.cooldownMs = Math.max(0L, cooldownSeconds) * 1000L;
    }

    /** True when the line may be shown now (and records it). */
    public boolean take(long now) {
        if (lastAt != Long.MIN_VALUE && now - lastAt < cooldownMs) return false;
        lastAt = now;
        return true;
    }

    /** Entering a warded world always shows the line, then restarts the cooldown. */
    public void force(long now) {
        lastAt = now;
    }
}
