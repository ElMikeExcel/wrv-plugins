package com.winkreyesvalley.excelmagiclevel.store;

/**
 * ExcelMagicLevel — one player's stored state (SPEC §7.1). Truth is the
 * single lifetime XP number; level is always derived from it via the curve,
 * never stored. total-casts is informational; quiet is the per-player
 * feedback opt-out.
 */
public final class PlayerRecord {
    private long totalXp;
    private long totalCasts;
    private boolean quiet;

    public PlayerRecord() {
        this(0, 0, false);
    }

    public PlayerRecord(long totalXp, long totalCasts, boolean quiet) {
        this.totalXp = Math.max(0, totalXp);
        this.totalCasts = Math.max(0, totalCasts);
        this.quiet = quiet;
    }

    public long totalXp() {
        return totalXp;
    }

    public long totalCasts() {
        return totalCasts;
    }

    public boolean quiet() {
        return quiet;
    }

    public void addXp(long amount) {
        if (amount > 0) totalXp += amount;
    }

    public void addCasts(long count) {
        if (count > 0) totalCasts += count;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }
}
