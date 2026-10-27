package com.winkreyes.excelwandbars.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Rank-up by casting (2026-10-26): the wand rank ladder and the SP each step costs.
 * Pure (no Bukkit), so the pacing and the text are unit tested.
 */
public final class RankLadder {
    /** Rank key (wand template) in order, lower case. */
    private final List<String> order = new ArrayList<>();
    /** SP needed to step UP INTO this rank (the first rank has none). */
    private final Map<String, Long> cost = new LinkedHashMap<>();
    /** Optional: lowest seal stage (ExcelMagicGate appliedStage) that opens this rank. Empty = no stage limit. */
    private final Map<String, Integer> minStage = new LinkedHashMap<>();

    public RankLadder add(String key, long spToReach) {
        String k = key.toLowerCase(Locale.ROOT);
        order.add(k);
        cost.put(k, Math.max(0, spToReach));
        return this;
    }

    public RankLadder minStage(String key, int stage) { minStage.put(key.toLowerCase(Locale.ROOT), stage); return this; }

    public static RankLadder wrvDefault() {
        return new RankLadder().add("beginner", 0).add("student", 800).add("apprentice", 1600).add("master", 1800);
    }

    public boolean has(String key) { return key != null && order.contains(key.toLowerCase(Locale.ROOT)); }
    public List<String> order() { return order; }

    /** The rank after this one, or null at the top (or not a ladder wand). */
    public String next(String key) {
        if (key == null) return null;
        int i = order.indexOf(key.toLowerCase(Locale.ROOT));
        return i < 0 || i + 1 >= order.size() ? null : order.get(i + 1);
    }

    public long cost(String key) { Long c = key == null ? null : cost.get(key.toLowerCase(Locale.ROOT)); return c == null ? 0 : c; }

    /** Is this rank open at the current seal stage? stage < 0 = unknown, treat as open. */
    public boolean openAt(String key, int stage) {
        Integer need = key == null ? null : minStage.get(key.toLowerCase(Locale.ROOT));
        return need == null || stage < 0 || stage >= need;
    }

    public enum Step { TOP, NOT_OPEN, SAVING, READY }

    public Step step(String rank, long sp, int stage) {
        String n = next(rank);
        if (n == null) return Step.TOP;
        if (!openAt(n, stage)) return Step.NOT_OPEN;
        return sp >= cost(n) ? Step.READY : Step.SAVING;
    }

    /** Total SP from the first rank up to and including this one. */
    public long total(String key) {
        long t = 0;
        for (String k : order) { t += cost(k); if (k.equalsIgnoreCase(key)) return t; }
        return t;
    }
}

