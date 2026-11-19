package com.winkreyes.exceldeathprotection;

/**
 * Pure banked-premium-credit arithmetic (decision D9, Nov 10, 2026):
 * one credit is one premium protection, banked as a count — never an
 * inventory item. This class is Bukkit-free so the semantics are
 * unit-testable without a server; the plugin class owns the PDC
 * storage and presentation around it.
 *
 * Credit rule:
 *   - no active protection: the first credited unit activates a
 *     premium protection immediately, the remainder banks;
 *   - any active protection (basic or premium): the full amount
 *     banks — an active protection is never overwritten or wasted.
 * Banked credits are spent by {@link #rearm}: after a death consumes
 * the active protection, one banked credit re-arms a premium
 * protection at respawn and the bank decrements by one.
 */
public final class PremiumCredits {

    private PremiumCredits() {}

    /** Outcome of one credit call. */
    public record CreditResult(boolean credited, boolean activateNow, int effectiveAmount, int newBanked) {
        static CreditResult rejected(int banked) {
            return new CreditResult(false, false, 0, banked);
        }
    }

    /** Outcome of the respawn re-arm check. */
    public record RearmResult(boolean rearm, int newBanked) {}

    /**
     * Apply a credit.
     *
     * @param active       whether any protection tier is currently active
     * @param banked       current banked count (>= 0)
     * @param amount       requested amount; values < 1 are rejected,
     *                     values above maxPerCall are clamped to it
     * @param enabled      the premium seam master switch
     * @param maxPerCall   per-call clamp; <= 0 means no clamp
     * @param maxBanked    bank cap; <= 0 means uncapped
     */
    public static CreditResult applyCredit(boolean active, int banked, int amount,
                                           boolean enabled, int maxPerCall, int maxBanked) {
        int safeBanked = Math.max(0, banked);
        if (!enabled || amount < 1) return CreditResult.rejected(safeBanked);
        int effective = maxPerCall > 0 ? Math.min(amount, maxPerCall) : amount;
        if (!active) {
            int remainder = effective - 1;
            int newBanked = cap(safeBanked + remainder, maxBanked);
            return new CreditResult(true, true, effective, newBanked);
        }
        int newBanked = cap(safeBanked + effective, maxBanked);
        if (newBanked == safeBanked) return CreditResult.rejected(safeBanked);
        return new CreditResult(true, false, newBanked - safeBanked, newBanked);
    }

    /**
     * Spend one banked credit at respawn, only when a protection was
     * actually consumed by the death and auto-rearm is enabled.
     */
    public static RearmResult rearm(int banked, boolean protectionWasUsed, boolean autoRearm) {
        int safeBanked = Math.max(0, banked);
        if (protectionWasUsed && autoRearm && safeBanked > 0) {
            return new RearmResult(true, safeBanked - 1);
        }
        return new RearmResult(false, safeBanked);
    }

    /** Remove up to {@code amount} banked credits (admin revoke); returns the new bank. */
    public static int revoke(int banked, int amount) {
        int safeBanked = Math.max(0, banked);
        if (amount < 1) return safeBanked;
        return Math.max(0, safeBanked - amount);
    }

    private static int cap(int value, int maxBanked) {
        return maxBanked > 0 ? Math.min(value, maxBanked) : value;
    }
}
