package com.winkreyes.excelclaims;

import java.util.ArrayList;
import java.util.List;

/**
 * ExcelClaims 0.1.3 — claim-corner pillar planning, pure (no
 * Bukkit): given the four claim corners and each corner column's
 * surface base Y, plan the packet-only marker blocks the
 * Visualizer sends. Each corner becomes a short vertical pillar
 * so it reads from any angle and over uneven ground (0.1.2 sent
 * ONE glowstone per corner, which disappears behind the terrain
 * from many viewpoints — Marv's player-proof report, Nov 8,
 * 2026; Mike's ruling the same evening: "Make claim corners
 * taller and more visible").
 *
 * Shape contract (pinned by CornerPillarTest):
 * - a pillar is `height` blocks tall in total, standing on the
 *   corner's base Y; the TOP block is the cap, every block below
 *   it is body. Height 1 therefore sends a lone cap block.
 * - exactly one cap per non-empty pillar, always its topmost
 *   sent block — including when the world ceiling truncates the
 *   pillar (the cap rides down to the highest sendable block
 *   rather than being lost).
 * - a corner whose base Y is already above the sendable ceiling
 *   contributes no blocks (the rest still show).
 * Excel — Iris / Mike.
 */
public final class CornerPillars {

    /** One planned marker block: a world column position + cap flag. */
    public record Block(int x, int y, int z, boolean cap) { }

    private CornerPillars() { }

    /**
     * The corner base Y — the 0.1.2 rule, carried verbatim: one
     * above the surface block, but never at or below the claim's
     * own bottom corner (a claim corner clicked deep still marks
     * at its clicked floor when the surface sits lower).
     */
    public static int baseY(int highestBlockY, int claimBottomY) {
        int y = highestBlockY + 1;
        if (y <= claimBottomY) y = claimBottomY + 1;
        return y;
    }

    /**
     * Plan every corner's pillar. `corners` are the four (x, z)
     * pairs in the Visualizer's standing order; `baseYs[i]` is
     * corner i's base Y (see baseY); `maxY` is the highest Y a
     * block may be sent at (world max height − 1). Blocks come
     * back corner by corner, bottom to top.
     */
    public static List<Block> plan(int[][] corners, int[] baseYs, int height, int maxY) {
        List<Block> out = new ArrayList<>();
        if (corners == null || baseYs == null) return out;
        int h = Math.max(1, height);
        int n = Math.min(corners.length, baseYs.length);
        for (int i = 0; i < n; i++) {
            int base = baseYs[i];
            int top = Math.min(base + h - 1, maxY);
            for (int y = base; y <= top; y++) {
                out.add(new Block(corners[i][0], y, corners[i][1], y == top));
            }
        }
        return out;
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley
