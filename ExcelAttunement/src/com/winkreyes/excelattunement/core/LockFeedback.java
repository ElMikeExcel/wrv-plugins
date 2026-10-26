package com.winkreyes.excelattunement.core;

/**
 * Per-player throttle for blocked-attempt feedback (0.1.4): the title
 * shows once per stance (per epoch), the sound at most every 400 ms so
 * a scroll wheel or a held key cannot spam it.
 */
public final class LockFeedback {
    public static final long SOUND_GAP_MS = 400L;

    private long titleEpoch = -1L;
    private long lastSoundAt = Long.MIN_VALUE;
    private long lastQuietAt = Long.MIN_VALUE;
    public static final long QUIET_GAP_MS = 1000L;

    /** True exactly once for each stance epoch. */
    public boolean takeTitle(long epoch) {
        if (epoch == titleEpoch) return false;
        titleEpoch = epoch;
        return true;
    }

    /** Quiet feedback (pickup tries fire every tick while standing on the item): once a second. */
    public boolean takeQuiet(long now) {
        if (lastQuietAt != Long.MIN_VALUE && now - lastQuietAt < QUIET_GAP_MS) return false;
        lastQuietAt = now;
        return true;
    }

    public boolean takeSound(long now) {
        if (lastSoundAt != Long.MIN_VALUE && now - lastSoundAt < SOUND_GAP_MS) return false;
        lastSoundAt = now;
        return true;
    }
}
