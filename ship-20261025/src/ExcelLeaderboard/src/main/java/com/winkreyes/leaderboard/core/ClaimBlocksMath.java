package com.winkreyes.leaderboard.core;

import java.util.List;

/** Pure claim-block math for the Land Claimed board, mirroring how
 * GriefPrevention itself counts (verified against the installed
 * GriefPrevention 16.18.7 jar): a player's total is their accrued blocks
 * plus their bonus blocks; what they have used is the summed area of
 * their own top-level claims. Subdivisions carve up a parent claim's
 * area, so they never consume blocks of their own. Claims standing in a
 * world where GriefPrevention claims are disabled do not count either:
 * GP cannot create or honour claims there, so the land is not claimed
 * in any sense the server recognises. No Bukkit types here, so every
 * rule is unit-testable off-server with fixture claims. */
public final class ClaimBlocksMath {

    /** One claim as the board sees it: its area in blocks, whether it
     * stands in a claims-enabled world, and whether it is a subdivision
     * of another claim. */
    public record ClaimFigure(int area, boolean worldEnabled, boolean subdivision) {}

    /** claimed = blocks used by counted claims; total = accrued + bonus;
     * available = total - claimed (negative when a config change left a
     * player over-drawn; display layers clamp at zero). */
    public record Totals(long claimed, long total, long available) {}

    private ClaimBlocksMath() {}

    public static Totals summarize(long accruedBlocks, long bonusBlocks, List<ClaimFigure> claims) {
        long claimed = 0;
        for (ClaimFigure claim : claims) {
            if (claim == null || claim.subdivision() || !claim.worldEnabled()) {
                continue;
            }
            claimed += Math.max(0, claim.area());
        }
        long total = Math.max(0, accruedBlocks) + Math.max(0, bonusBlocks);
        return new Totals(claimed, total, total - claimed);
    }
}
