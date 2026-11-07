package com.winkreyes.excelclaims;

import java.util.UUID;

/**
 * ExcelClaims — one player's claim-block account. Accrued blocks come
 * from playtime, bonus blocks from staff grants (buying is off in the
 * live config), and "used" is always computed from the claims the
 * player owns, never stored — the same accounting the old system used.
 * Excel — Iris / Mike.
 */
public final class PlayerRecord {
    public final UUID id;
    public int accrued;
    public int bonus;
    /** Per-player accrued cap override; -1 = use the global config cap. */
    public int accruedLimitOverride = -1;
    public long lastSeenAt;
    /** Accrual fraction carried between minutes, in sixtieths of a block (see Accrual). */
    public int playMinutesCarry;

    public PlayerRecord(UUID id, int accrued, int bonus) {
        this.id = id;
        this.accrued = accrued;
        this.bonus = bonus;
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley
