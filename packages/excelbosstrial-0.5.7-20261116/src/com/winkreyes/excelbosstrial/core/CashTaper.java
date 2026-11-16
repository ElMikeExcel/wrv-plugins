/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The SEAL CASH TAPER (ExcelBossTrial 0.5.6; Mike's law, Nov 10,
 * 2026, scope confirmed Nov 11, 2026). Cash — and ONLY cash —
 * tapers with a player's PER-SEAL LIFETIME clear count:
 *
 *   clear #1: Warden $10,000; every other seal $5,000
 *   clear #2: $5,000 (every seal, Warden included)
 *   clear #3+: $2,000 (every seal)
 *
 * Counts are per player, per seal, LIFETIME: they never reset,
 * including across seal cycles (the cycle mechanism re-keys
 * cycle progress, never this ladder). The count judged is the
 * count INCLUDING the clear being paid (clear #1 is the player's
 * first paid clear of that seal).
 *
 * CASH-ONLY: mcMMO XP (5,000 SWORDS), MagicLevel XP (250), the
 * jackpot +2 extra rolls (Underdog scaling unchanged) and the
 * war-chest rows pay IN FULL at every clear count. Those amounts
 * live in the repeat / first-break specs and are not read here;
 * this class exists so no payout path can taper anything else
 * by accident, and so every cash path (first break, repeat,
 * raid companion) prices from one table.
 */
public final class CashTaper {

    public static final long WARDEN_FIRST_CLEAR_CASH = 10000L;
    public static final long FIRST_CLEAR_CASH = 5000L;
    public static final long SECOND_CLEAR_CASH = 5000L;
    public static final long FLOOR_CLEAR_CASH = 2000L;

    /** The raid seal id (GuildGate owns the raid book; BossTrial
     *  pays through the raid companion adapter). */
    public static final String RAID_SEAL_ID = "raid";

    private CashTaper() {
    }

    public static boolean isWarden(String sealId) {
        return BossKind.WARDEN.sealId().equals(sealId);
    }

    /**
     * Cash for one player's clear of one seal.
     *
     * @param sealId            the GuildGate seal id ("warden",
     *                          "wither", "elder_guardian",
     *                          "ender_dragon", "raid")
     * @param lifetimeClearNumber the player's lifetime clear count
     *                          for this seal INCLUDING this clear
     *                          (1 = first ever). Values below 1 are
     *                          clamped to the first-clear tier: a
     *                          payout path that lost its count
     *                          underpays nobody below the settled
     *                          first-clear price by accident — it
     *                          pays the first-clear price, and the
     *                          host logs the count it used.
     */
    public static long cashFor(String sealId, int lifetimeClearNumber) {
        int n = Math.max(1, lifetimeClearNumber);
        if (n == 1) {
            return isWarden(sealId) ? WARDEN_FIRST_CLEAR_CASH : FIRST_CLEAR_CASH;
        }
        if (n == 2) {
            return SECOND_CLEAR_CASH;
        }
        return FLOOR_CLEAR_CASH;
    }

    public static long cashFor(BossKind kind, int lifetimeClearNumber) {
        return cashFor(kind == null ? "" : kind.sealId(), lifetimeClearNumber);
    }
}
