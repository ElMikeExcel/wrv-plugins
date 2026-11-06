package com.winkreyes.excelcosmetics.core;

/**
 * One timed (or permanent) cosmetic grant. Immutable: renewal
 * produces a new Grant. expiresAtMillis == PERMANENT means the
 * own-it tier: it never expires. Activity is a half-open window
 * [grantedAt, expiresAt): at the exact expiry instant the grant
 * is already over, so chat falls back silently on the next line.
 */
public record Grant(Kind kind, String id, long grantedAtMillis, long expiresAtMillis) {

    public static final long PERMANENT = -1L;

    public boolean isPermanent() {
        return expiresAtMillis == PERMANENT;
    }

    public boolean isActiveAt(long now) {
        if (now < grantedAtMillis) return false;
        return isPermanent() || now < expiresAtMillis;
    }

    /** Milliseconds left at {@code now}; -1 when permanent, 0 when over. */
    public long remainingMillis(long now) {
        if (isPermanent()) return -1L;
        long left = expiresAtMillis - now;
        return Math.max(0L, left);
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
