package com.winkreyes.exceleliteladder.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The approved Elite Seal Ladder (Oct 25, 2026; Seal 5 spawn 0.12 picked 11:40 PM CDT), with the Seal 5 dungeon values
 * (normalized 0.5 / 2.0) already in from S1. Hardcoded on purpose: the table is not
 * editable in config, so a typo in a config file can never change live combat.
 */
public final class LadderTable {
    public static final int MIN_STEP = 1;
    public static final int MAX_STEP = 5;

    private static final LadderStep[] STEPS = {
        null,
        new LadderStep(1, 0.07, 120, 0.5, 2.0, 0.55, 1.75),
        new LadderStep(2, 0.08, 120, 0.5, 2.0, 0.50, 1.90),
        new LadderStep(3, 0.09, 120, 0.5, 2.0, 0.50, 2.05),
        new LadderStep(4, 0.10, 120, 0.5, 2.0, 0.45, 2.20),
        new LadderStep(5, 0.12, 120, 0.5, 2.0, 0.45, 2.20),
    };

    /** Known pre-ladder files, so a refusal can say exactly what live is. */
    public static final LadderStep OLD_S0 = new LadderStep(0, 0.07, 120, 1.0, 1.0, 0.55, 1.60);
    public static final LadderStep OLD_S1 = new LadderStep(1, 0.07, 120, 1.0, 1.0, 0.55, 1.75);

    private LadderTable() {
    }

    public static LadderStep step(int n) {
        if (n < MIN_STEP || n > MAX_STEP) {
            throw new IllegalArgumentException("no ladder step " + n);
        }
        return STEPS[n];
    }

    public static int clampSeals(int seals) {
        return Math.max(0, Math.min(MAX_STEP, seals));
    }

    public static boolean matches(Map<String, Double> live, LadderStep s) {
        for (Map.Entry<String, Double> e : s.values().entrySet()) {
            Double v = live.get(e.getKey());
            if (v == null || Math.abs(v - e.getValue()) > 1e-9) {
                return false;
            }
        }
        return true;
    }

    /** Ladder step whose six values equal the live values, or 0 when none does. */
    public static int matchStep(Map<String, Double> live) {
        for (int i = MIN_STEP; i <= MAX_STEP; i++) {
            if (matches(live, STEPS[i])) {
                return i;
            }
        }
        return 0;
    }

    /** Human reason why live is not on the ladder (used in refusals). */
    public static String describeOffLadder(Map<String, Double> live, LadderStep expected) {
        if (matches(live, OLD_S0)) {
            return "live MobCombatSettings.yml is the OLD pre-ladder S0 file (toPlayer 1.6, dungeon 1.0/1.0), not " + expected.label() + "-norm";
        }
        if (matches(live, OLD_S1)) {
            return "live MobCombatSettings.yml is the OLD S1 file without the dungeon lines (91/94 = 1.0/1.0), not " + expected.label() + "-norm";
        }
        return "live MobCombatSettings.yml is not on the ladder; differs from " + expected.label() + "-norm at: " + differences(live, expected);
    }

    public static String differences(Map<String, Double> live, LadderStep expected) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Double> e : expected.values().entrySet()) {
            Double v = live.get(e.getKey());
            if (v == null || Math.abs(v - e.getValue()) > 1e-9) {
                out.add(e.getKey() + "=" + (v == null ? "missing" : MobCombatFile.format(e.getKey(), v))
                        + " (want " + MobCombatFile.format(e.getKey(), e.getValue()) + ")");
            }
        }
        return out.isEmpty() ? "none" : String.join(", ", out);
    }
}
