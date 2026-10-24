package com.winkreyes.excelguide.core;

import java.util.Locale;

/**
 * Wording for the claim-blocks answers (0.2.2). Mike's live-test call
 * (Oct 18, 2026): asking "how much land can I claim?" should get BOTH
 * his own numbers AND the general rule, plus how to check it himself.
 * The combined line is composed here - not in the glue - so the exact
 * player-facing wording is unit-tested: the personal numbers first,
 * then the same rule sentence the generic claim-amounts intent
 * carries, then the self-check pointer. The facts (start 100, +100
 * per hour of play, cap 80,000, blocks from playtime only) are the
 * live GriefPrevention facts, identical to intents.yml #28 and the
 * website's Claim blocks section; the self-check command is GP's own
 * /claimslist, which the website already teaches.
 */
public final class ClaimText {

    private ClaimText() {}

    /** The general rule + self-check, shared shape with the generic intent text. */
    static final String RULE =
            "Everyone starts with 100 claim blocks and earns 100 more for every hour of play, "
            + "up to 80,000 - blocks come from playtime only, they can't be bought.";

    static final String SELF_CHECK = "Check yours any time with /claimslist.";

    /**
     * The combined personal answer for {accrued, bonus, used,
     * remaining}. Remaining is clamped at 0 for display (GP never
     * lets used exceed the total in practice; a display clamp keeps a
     * corrupt read from printing a negative allowance).
     */
    public static String combined(long accrued, long bonus, long used, long remaining) {
        StringBuilder out = new StringBuilder("You have ");
        out.append(num(Math.max(0, remaining))).append(" claim blocks left: you earned ")
                .append(num(Math.max(0, accrued))).append(" from playtime");
        if (bonus > 0) {
            out.append(" plus ").append(num(bonus)).append(" bonus");
        }
        out.append(" and used ").append(num(Math.max(0, used))).append(" on claims. ");
        out.append(RULE).append(' ').append(SELF_CHECK);
        return out.toString();
    }

    /** Chat-friendly thousands separators, locale-pinned so tests are stable. */
    static String num(long v) {
        return String.format(Locale.US, "%,d", v);
    }
}
