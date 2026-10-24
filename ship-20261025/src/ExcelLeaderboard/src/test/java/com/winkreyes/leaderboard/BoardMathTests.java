package com.winkreyes.leaderboard;

import com.winkreyes.leaderboard.core.BoardMath;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BoardMathTests {
    private static int checks = 0;

    private static void eq(Object actual, Object expected, String label) {
        checks++;
        if (!actual.equals(expected)) {
            throw new AssertionError(label + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static UUID id(int n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    public static void main(String[] args) {
        // Ranking: value desc, ties share rank, listed by name.
        var entries = List.of(
                new BoardMath.Entry(id(1), "Zed", 100),
                new BoardMath.Entry(id(2), "amy", 250),
                new BoardMath.Entry(id(3), "Bob", 250),
                new BoardMath.Entry(id(4), "Kim", 40));
        var ranked = BoardMath.rank(entries);
        eq(ranked.get(0).name(), "amy", "tie broken by name asc");
        eq(ranked.get(1).name(), "Bob", "tie second");
        eq(BoardMath.rankOf(ranked, id(2)), 1, "tied rank 1 (amy)");
        eq(BoardMath.rankOf(ranked, id(3)), 1, "tied rank 1 (Bob)");
        eq(BoardMath.rankOf(ranked, id(1)), 3, "rank skips after tie");
        eq(BoardMath.rankOf(ranked, id(4)), 4, "last rank");
        eq(BoardMath.rankOf(ranked, id(99)), -1, "absent player");
        eq(BoardMath.top(ranked, 2).size(), 2, "top n");
        eq(BoardMath.top(ranked, 99).size(), 4, "top n beyond size");

        // Formatting.
        eq(BoardMath.formatPlaytime(0), "0m", "playtime zero");
        eq(BoardMath.formatPlaytime(20L * 60 * 59), "59m", "playtime under hour");
        eq(BoardMath.formatPlaytime(20L * 60 * 60), "1h 0m", "playtime one hour");
        eq(BoardMath.formatPlaytime(20L * 60 * (42 * 60 + 7)), "42h 7m", "playtime long");
        eq(BoardMath.formatDistanceKm(0), "0.0 km", "distance zero");
        eq(BoardMath.formatDistanceKm(150_000), "1.5 km", "distance 1.5km");
        eq(BoardMath.formatDistanceKm(12_345_678), "123.5 km", "distance rounding");
        eq(BoardMath.formatCount(1234567), "1,234,567", "count separators");
        eq(BoardMath.formatMoney(1500.4), "$1,500", "money whole dollars");
        eq(BoardMath.formatMoney(-5), "$0", "money clamps negative");

        // Placed blocks: block items only.
        Set<String> blocks = Set.of("STONE", "OAK_PLANKS", "DIRT");
        Map<String, Integer> use = Map.of(
                "STONE", 10, "OAK_PLANKS", 5, "BOW", 99, "COOKED_BEEF", 3, "DIRT", 2);
        eq(BoardMath.sumPlacedBlocks(use, blocks::contains), 17L, "placed filter");

        // Build Mode adjustment math.
        eq(BoardMath.adjustedMined(1000, 300), 700L, "miner adjustment");
        eq(BoardMath.adjustedMined(100, 300), 0L, "adjustment clamps at zero");
        eq(BoardMath.adjustedMined(100, -5), 100L, "negative adjustment ignored");
        eq(BoardMath.sessionDelta(500, 800), 300L, "session delta");
        eq(BoardMath.sessionDelta(800, 500), 0L, "stat reset yields zero delta");

        System.out.println("BoardMathTests: " + checks + " assertions passed");
    }
}
