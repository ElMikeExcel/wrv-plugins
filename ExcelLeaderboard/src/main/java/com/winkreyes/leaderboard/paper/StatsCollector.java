package com.winkreyes.leaderboard.paper;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;

import java.util.Map;

/** Vanilla statistic reads for one player. MINE_BLOCK and USE_ITEM are
 * summed per material; the block filter is Material#isBlock, which matches
 * the spec (Top Builder counts block items only). */
public final class StatsCollector {

    private StatsCollector() {}

    public static long minedTotal(OfflinePlayer player) {
        long total = 0;
        for (Material material : Material.values()) {
            if (material.isBlock()) {
                total += safe(() -> player.getStatistic(Statistic.MINE_BLOCK, material));
            }
        }
        return total;
    }

    public static Map<String, Integer> useItemByMaterial(OfflinePlayer player) {
        Map<String, Integer> values = new java.util.HashMap<>();
        for (Material material : Material.values()) {
            if (!material.isItem()) {
                continue;
            }
            int value = safe(() -> player.getStatistic(Statistic.USE_ITEM, material));
            if (value > 0) {
                values.put(material.name(), value);
            }
        }
        return values;
    }

    public static long placedTotal(OfflinePlayer player, java.util.function.Predicate<String> isBlockItem) {
        long total = 0;
        for (Map.Entry<String, Integer> e : useItemByMaterial(player).entrySet()) {
            if (isBlockItem.test(e.getKey())) {
                total += e.getValue();
            }
        }
        return total;
    }

    public static boolean isBlockItem(String materialName) {
        try {
            return Material.valueOf(materialName).isBlock();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static long playtimeTicks(OfflinePlayer player) {
        return safe(() -> player.getStatistic(Statistic.PLAY_ONE_MINUTE));
    }

    public static long mobKills(OfflinePlayer player) {
        return safe(() -> player.getStatistic(Statistic.MOB_KILLS));
    }

    public static long deaths(OfflinePlayer player) {
        return safe(() -> player.getStatistic(Statistic.DEATHS));
    }

    private static final Statistic[] DISTANCE_STATS = {
            Statistic.WALK_ONE_CM,
            Statistic.SPRINT_ONE_CM,
            Statistic.WALK_ON_WATER_ONE_CM,
            Statistic.WALK_UNDER_WATER_ONE_CM,
            Statistic.SWIM_ONE_CM,
            Statistic.FLY_ONE_CM,
            Statistic.AVIATE_ONE_CM,
            Statistic.BOAT_ONE_CM,
            Statistic.HORSE_ONE_CM,
            Statistic.MINECART_ONE_CM,
            Statistic.PIG_ONE_CM,
            Statistic.STRIDER_ONE_CM,
    };

    public static long distanceTotalCm(OfflinePlayer player) {
        long total = 0;
        for (Statistic stat : DISTANCE_STATS) {
            total += safe(() -> player.getStatistic(stat));
        }
        return total;
    }

    private interface StatRead {
        int run();
    }

    private static int safe(StatRead read) {
        try {
            return read.run();
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
