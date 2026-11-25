/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.dragon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Crystal re-light at the flip (Item 35 Packet B, D-2 - the
 * dragon chapter's signature): when the phase flip fires, the
 * trial re-lights end crystals on towers that currently have
 * none, so the skyline itself changes at the halfway mark and
 * phase 2 re-opens the crystal sub-game (the dragon heals
 * again; entity-health judging makes that heal-consistent by
 * construction - design finding F-D4).
 *
 * This class is the pure choice: which towers, how many. The
 * Bukkit adapter (DragonChapter) discovers the towers (obsidian
 * pillar tops around the fountain, clustered), reports which
 * already carry a crystal, counts the trial's own still-live
 * re-lit crystals, and spawns what this class picks. Rules,
 * from the design verbatim:
 *
 * - re-light {@link #DEFAULT_COUNT} crystals (4);
 * - only on towers that currently have NO crystal;
 * - a random pick among the bare towers;
 * - never more than {@link #DEFAULT_MAX_LIVE} (6) live trial
 *   crystals - crystals this trial re-lit and has not lost.
 *
 * The P3 placement lock is untouched: players still cannot
 * PLACE crystals mid-trial; only the trial re-lights.
 */
public final class CrystalRelight {

    /** Crystals re-lit at one flip (design D-2). */
    public static final int DEFAULT_COUNT = 4;
    /** Ceiling on live trial (re-lit) crystals (design D-2). */
    public static final int DEFAULT_MAX_LIVE = 6;

    private CrystalRelight() {
    }

    /** One discovered tower: its index in the adapter's tower
     *  list and whether a live crystal already stands on it. */
    public record Tower(int index, boolean crystalPresent) {
    }

    /** Sanitize the configured re-light count: [0, 10]; a
     *  negative or junk-shaped value reads as the design count
     *  (the adapter passes typed reads, so this is the last
     *  fence, not the parser). */
    public static int sanitizedCount(int configured) {
        if (configured < 0) {
            return DEFAULT_COUNT;
        }
        return Math.min(10, configured);
    }

    /** Sanitize the configured live-trial-crystal ceiling:
     *  [0, 12]; negative reads as the design ceiling. A ceiling
     *  below the count simply caps the flip's re-light. */
    public static int sanitizedMaxLive(int configured) {
        if (configured < 0) {
            return DEFAULT_MAX_LIVE;
        }
        return Math.min(12, configured);
    }

    /**
     * How many crystals this flip may add: the configured
     * count, limited by the live-trial-crystal ceiling minus
     * the trial crystals still live, and by the bare towers
     * available. Never negative.
     */
    public static int allowance(int configuredCount, int configuredMaxLive,
                                int liveTrialCrystals, int bareTowers) {
        int want = sanitizedCount(configuredCount);
        int ceiling = sanitizedMaxLive(configuredMaxLive);
        int live = Math.max(0, liveTrialCrystals);
        int room = Math.max(0, ceiling - live);
        return Math.max(0, Math.min(want, Math.min(room, Math.max(0, bareTowers))));
    }

    /**
     * Pick the towers to re-light: a shuffled draw of the bare
     * towers, sized by {@link #allowance}. Returns tower
     * indices in draw order (the adapter spawns in this order);
     * empty when nothing may be re-lit. The draw is the ONLY
     * randomness in the feature (design: "random pick among
     * bare towers") - seeded by the caller's Random so a rig
     * can reproduce a draw.
     */
    public static List<Integer> choose(List<Tower> towers, int configuredCount,
                                       int configuredMaxLive, int liveTrialCrystals,
                                       Random rng) {
        if (towers == null || towers.isEmpty()) {
            return List.of();
        }
        List<Integer> bare = new ArrayList<>();
        for (Tower t : towers) {
            if (t != null && !t.crystalPresent()) {
                bare.add(t.index());
            }
        }
        int n = allowance(configuredCount, configuredMaxLive,
                liveTrialCrystals, bare.size());
        if (n <= 0) {
            return List.of();
        }
        List<Integer> draw = new ArrayList<>(bare);
        Collections.shuffle(draw, rng == null ? new Random() : rng);
        return List.copyOf(draw.subList(0, n));
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
