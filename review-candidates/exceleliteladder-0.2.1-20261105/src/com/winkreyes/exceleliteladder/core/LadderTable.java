package com.winkreyes.exceleliteladder.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The approved Elite Seal Ladder (Oct 25, 2026; Seal 5 spawn 0.12 picked 11:40 PM CDT), with the Seal 5 dungeon values
 * (normalized 0.5 / 2.0) already in from S1.
 *
 * 0.1.x: hardcoded on purpose — the table was not editable in config, so a typo in
 * a config file could never change live combat.
 *
 * 0.2.0 (Nov 5, 2026): Mike's 2.0 decision (item 3) moves the table into ladder.yml
 * in exchange for strict validation — LadderConfig refuses any malformed table and
 * the last-good table keeps running, "nearly as typo-proof" with no jar cycles.
 * This class is now an instance: {@link #builtin()} carries the shipped default
 * values as the fallback when ladder.yml is missing/unreadable/refused, and as the
 * reference the shipped default ladder.yml must equal value-for-value
 * (proven by the equality suite).
 *
 * 0.1.2 (Nov 1, 2026): Mike re-decided the ladder - it TOPS OUT AT SEAL 4, COMPRESSED
 * (2:30 PM CST, superseding the Oct 21 S5-parity final and the Oct 25 Seal 5 picks).
 * S4 carries the full end-state values; the S5 row is IDENTICAL to S4, so a Seal 5
 * trigger is a safe no-op: at 5 of 5 the players transfer to the EliteMobs guild
 * world and an overworld step landing then would never be felt.
 *
 * 0.2.1 (Nov 5, 2026): Mike's retune REPLACES the built-in values with the retuned
 * table (spawn 0.10/0.12/0.14/0.17/0.17; scaled-to-elite 0.50/0.45/0.40/0.35/0.35;
 * scaled-to-player 1.58/1.71/1.85/1.94/1.94; cap and dungeon values unchanged).
 * The fallback must never silently resurrect pre-retune tuning if the live
 * ladder.yml is lost or malformed, so the built-in moves with the shipped
 * default — the pre-retune 0.1.x/0.2.0 values live in the records, not here.
 * ExcelEliteLadder - Excel
 */
public final class LadderTable {
    public static final int MIN_STEP = 1;
    public static final int MAX_STEP = 5;

    /** The shipped default table (0.2.1: the Nov 5, 2026 retune) — fallback and equality reference. */
    private static final LadderStep[] BUILTIN_STEPS = {
        null,
        new LadderStep(1, 0.10, 120, 0.5, 2.0, 0.50, 1.58),
        new LadderStep(2, 0.12, 120, 0.5, 2.0, 0.45, 1.71),
        new LadderStep(3, 0.14, 120, 0.5, 2.0, 0.40, 1.85),
        new LadderStep(4, 0.17, 150, 0.5, 2.0, 0.35, 1.94),
        new LadderStep(5, 0.17, 150, 0.5, 2.0, 0.35, 1.94),
    };

    /** Known pre-ladder files, so a refusal can say exactly what live is. */
    public static final LadderStep OLD_S0 = new LadderStep(0, 0.07, 120, 1.0, 1.0, 0.55, 1.60);
    public static final LadderStep OLD_S1 = new LadderStep(1, 0.07, 120, 1.0, 1.0, 0.55, 1.75);

    private final LadderStep[] steps; // index 1..MAX_STEP

    /**
     * @param steps array indexed by step number: length MAX_STEP+1, slots 1..MAX_STEP
     *              non-null with matching step numbers. Defensively copied. Anything
     *              else is a programming error, not user input — user input arrives
     *              via LadderConfig, which validates before constructing.
     */
    public LadderTable(LadderStep[] steps) {
        if (steps == null || steps.length != MAX_STEP + 1) {
            throw new IllegalArgumentException("ladder table must hold exactly steps 1.." + MAX_STEP);
        }
        this.steps = new LadderStep[MAX_STEP + 1];
        for (int n = MIN_STEP; n <= MAX_STEP; n++) {
            LadderStep s = steps[n];
            if (s == null || s.step() != n) {
                throw new IllegalArgumentException("ladder table slot " + n + " must hold step " + n);
            }
            this.steps[n] = s;
        }
    }

    /** The built-in default table: the shipped default values (0.2.1: the Nov 5, 2026 retune). */
    public static LadderTable builtin() {
        return new LadderTable(BUILTIN_STEPS.clone());
    }

    public LadderStep step(int n) {
        if (n < MIN_STEP || n > MAX_STEP) {
            throw new IllegalArgumentException("no ladder step " + n);
        }
        return steps[n];
    }

    /** The steps S1..S5 in order. */
    public java.util.List<LadderStep> steps() {
        java.util.List<LadderStep> out = new ArrayList<>(MAX_STEP);
        for (int n = MIN_STEP; n <= MAX_STEP; n++) {
            out.add(steps[n]);
        }
        return java.util.List.copyOf(out);
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
    public int matchStep(Map<String, Double> live) {
        for (int i = MIN_STEP; i <= MAX_STEP; i++) {
            if (matches(live, steps[i])) {
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
