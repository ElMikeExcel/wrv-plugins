package com.winkreyes.acrobatics.core;

import java.util.List;

/** Payout math for water landings, mirroring what mcMMO would have paid
 * for the same fall onto land, plus Mike's style bonus and the
 * diminishing same-spot repeats he asked for (Oct 14, 2026). Pure and
 * unit-testable; no Bukkit types. */
public final class PayoutMath {

    public record Settings(
            double xpPerDamagePoint,
            double styleStartBlocks,
            double stylePerBlock,
            double styleCap,
            List<Double> repeatMultipliers,
            int maxXpPerLanding,
            double featherFallXpMultiplier) {}

    private PayoutMath() {}

    /** Vanilla fall damage points for a fall: nothing for the first 3
     * blocks, then 1 point (half a heart) per block, floored like the game. */
    public static double fallDamagePoints(double fallDistance) {
        if (fallDistance <= 3.0) {
            return 0;
        }
        return Math.floor(fallDistance - 3.0);
    }

    /** Feather Falling reduces fall damage by 12% per level, capped at an
     * 80% reduction like the game's protection cap. */
    public static double featherFallAdjustedDamage(double damagePoints, int featherFallLevel) {
        if (featherFallLevel <= 0 || damagePoints <= 0) {
            return Math.max(0, damagePoints);
        }
        double reduction = Math.min(0.80, 0.12 * featherFallLevel);
        return damagePoints * (1.0 - reduction);
    }

    /** Style multiplier: 1.0 up to the start height, then grows per block,
     * capped. Defaults: start 10 blocks, +5% per block, cap +100% (2.0x at
     * 30+ blocks). */
    public static double styleMultiplier(double fallDistance, double startBlocks,
                                         double perBlock, double cap) {
        double bonus = Math.max(0, fallDistance - startBlocks) * perBlock;
        return 1.0 + Math.min(cap, bonus);
    }

    /** Diminishing repeats: index by how many times this spot already paid
     * inside the memory window; the last table entry is the floor. */
    public static double repeatMultiplier(int priorLandings, List<Double> table) {
        if (table == null || table.isEmpty()) {
            return 1.0;
        }
        int index = Math.min(Math.max(0, priorLandings), table.size() - 1);
        return table.get(index);
    }

    /** The full award. mcMMO fidelity notes: on land, mcMMO pays on the
     * fall's final damage (so Feather Falling's damage reduction applies),
     * and its own config multiplies Acrobatics XP by 2.0 when Feather
     * Falling boots are worn, which is mirrored here. */
    public static int awardXp(double fallDistance, int featherFallLevel,
                              int priorRepeatsAtSpot, Settings settings) {
        double damage = featherFallAdjustedDamage(fallDamagePoints(fallDistance), featherFallLevel);
        if (damage <= 0) {
            return 0;
        }
        double xp = damage * settings.xpPerDamagePoint();
        if (featherFallLevel > 0) {
            xp *= settings.featherFallXpMultiplier();
        }
        xp *= styleMultiplier(fallDistance, settings.styleStartBlocks(),
                settings.stylePerBlock(), settings.styleCap());
        xp *= repeatMultiplier(priorRepeatsAtSpot, settings.repeatMultipliers());
        long rounded = Math.round(xp);
        return (int) Math.max(0, Math.min(settings.maxXpPerLanding(), rounded));
    }
}
