package com.winkreyes.excelbraingames.core;

/**
 * Per-round payout adjustment, applied BEFORE the ledger's cap/streak
 * settlement (SPEC §3.4, §5.4, §9.3 + Mike's Q6 amendment, Nov 4, 2026):
 *  - Speed/par bonus: +percent% on the tier payout when earned.
 *  - Kid tiers: a kid-flagged player earns the full tier payout; anyone else
 *    earns exactly HALF, rounded down to the cent. (Supersedes the original
 *    $0-practice recommendation — Mike's decision, Nov 4, 2026.)
 * Pure logic.
 */
public final class Payouts {
    private Payouts() {}

    /**
     * @param tierPayoutCents the tier table payout
     * @param bonusPercent    speed/par bonus percent earned (0 if none)
     * @param kidTier         whether the tier is a kid tier
     * @param kidFlagged      whether the player carries the kid flag
     * @return the base cents handed to the ledger settlement
     */
    public static long adjust(long tierPayoutCents, int bonusPercent, boolean kidTier, boolean kidFlagged) {
        long base = tierPayoutCents;
        if (bonusPercent > 0) {
            base = Math.round(base * (100.0 + bonusPercent) / 100.0);
        }
        if (kidTier && !kidFlagged) {
            base = Money.halfRoundedDown(base);
        }
        return base;
    }
}
