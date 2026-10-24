package com.winkreyes.excelwandbars.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 0.2.2 throttle for the action-bar progress line. Pure logic.
 * A line goes out after a counted cast only when the count changed, and at
 * most once per minGapMs per player, except that a rank-up or the cast that
 * fills the count always shows. The line is sent once (never repeated), so
 * ExcelAttunement's stance line wins on its next update; while a stance is
 * on, the plugin skips the line altogether (STANCE).
 */
public final class ProgressGate {
    public enum Result { SHOW, RANKED_UP, SAME, THROTTLED, STANCE, DISABLED }

    public boolean enabled = true;
    public long minGapMs = 4000;
    public boolean skipDuringStance = true;

    private final Map<UUID, Long> lastShown = new HashMap<>();
    private final Map<String, long[]> seen = new HashMap<>(); // uuid|spell -> {rank, count}

    public Result decide(UUID id, String spell, int rank, long count, long need, boolean inStance, long nowMs) {
        if (!enabled) return Result.DISABLED;
        String k = id + "|" + spell;
        long[] prev = seen.get(k);
        seen.put(k, new long[] {rank, count});
        boolean rankedUp = prev != null && rank > prev[0];
        if (!rankedUp && prev != null && prev[0] == rank && prev[1] == count) return Result.SAME;
        if (inStance && skipDuringStance) return Result.STANCE;
        boolean milestone = need > 0 && count >= need;
        Long last = lastShown.get(id);
        if (!rankedUp && !milestone && last != null && nowMs - last >= 0 && nowMs - last < minGapMs) return Result.THROTTLED;
        lastShown.put(id, nowMs);
        return rankedUp ? Result.RANKED_UP : Result.SHOW;
    }

    public void forget(UUID id) {
        lastShown.remove(id);
        String p = id + "|";
        seen.keySet().removeIf(s -> s.startsWith(p));
    }
}
