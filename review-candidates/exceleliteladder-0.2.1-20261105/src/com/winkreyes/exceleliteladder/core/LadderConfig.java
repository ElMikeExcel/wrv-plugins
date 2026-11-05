package com.winkreyes.exceleliteladder.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Strict parser/validator for the ladder table in ladder.yml (0.2.0+).
 *
 * Mike's accepted trade (Nov 5, 2026, 2.0 decision sheet item 3): the table
 * lives in config in exchange for being "nearly as typo-proof" as jar code.
 * This class is that proof — EVERY defect refuses the whole table with the
 * exact defect named; nothing is ever partially applied, defaulted silently,
 * or coerced:
 *
 *   - the 'ladder' section must be a mapping with exactly step-1..step-5;
 *   - each step must be a mapping with exactly the six known fields
 *     (an unknown key is refused — that is where typos land);
 *   - every value must be a finite number of the right shape
 *     (cap a whole number; strings/booleans/null are refused, never parsed);
 *   - ranges: spawn in (0, 1]; cap in [1, 1000]; every multiplier in (0, 10];
 *   - monotonicity the design requires (LadderTable class doc): spawn and
 *     cap never decrease step over step, scaled damage dealt to elites
 *     (scaled-to-player) never decreases, scaled damage taken from elites
 *     (scaled-to-elite) never increases. Plateaus are allowed.
 *     The two normalized ("dungeon") values carry no monotonicity rule —
 *     they have been constant (0.5 / 2.0) in every shipped table and the
 *     design constrains them by range only.
 *
 * Pure Java (maps in, LadderTable out) so the plain-JVM suite can drive it
 * with SnakeYAML-parsed maps in exactly the shape the Bukkit glue produces.
 */
public final class LadderConfig {
    private LadderConfig() {
    }

    public static final String SECTION = "ladder";

    public static final String F_SPAWN = "spawn";
    public static final String F_CAP = "cap";
    public static final String F_NORM_ELITE = "normalized-to-elite";
    public static final String F_NORM_PLAYER = "normalized-to-player";
    public static final String F_SCALED_ELITE = "scaled-to-elite";
    public static final String F_SCALED_PLAYER = "scaled-to-player";

    private static final Set<String> FIELDS = Set.of(
            F_SPAWN, F_CAP, F_NORM_ELITE, F_NORM_PLAYER, F_SCALED_ELITE, F_SCALED_PLAYER);
    private static final String FIELDS_HUMAN =
            "spawn, cap, normalized-to-elite, normalized-to-player, scaled-to-elite, scaled-to-player";

    private static final double MULT_MAX = 10.0;
    private static final int CAP_MAX = 1000;

    /**
     * Parse a whole parsed-YAML root map: pulls the 'ladder' section out and
     * validates it. This is the single entry point both the plugin glue and
     * the test suite use.
     */
    public static LadderTable parseRoot(Map<String, ?> root) {
        if (root == null) {
            throw new LadderConfigException("ladder.yml is empty or unreadable — no 'ladder:' section found");
        }
        Object section = root.get(SECTION);
        if (section == null) {
            throw new LadderConfigException("ladder.yml has no 'ladder:' section — the table is missing entirely");
        }
        return parse(section);
    }

    /** Validate the 'ladder' section value itself (must be a map of step maps). */
    public static LadderTable parse(Object sectionValue) {
        if (!(sectionValue instanceof Map<?, ?> section)) {
            throw new LadderConfigException("'ladder:' must be a mapping of step-1..step-"
                    + LadderTable.MAX_STEP + ", found: " + describe(sectionValue));
        }
        // Unknown step keys first: a typo'd or extra row must never slip through.
        for (Object k : section.keySet()) {
            if (!(k instanceof String key) || !isStepKey(key)) {
                throw new LadderConfigException("ladder has unknown step key '" + k
                        + "' — allowed keys are step-1..step-" + LadderTable.MAX_STEP + " only");
            }
        }
        LadderStep[] steps = new LadderStep[LadderTable.MAX_STEP + 1];
        for (int n = LadderTable.MIN_STEP; n <= LadderTable.MAX_STEP; n++) {
            String key = "step-" + n;
            if (!section.containsKey(key)) {
                throw new LadderConfigException("ladder." + key + " is missing — the table must define step-1 through step-"
                        + LadderTable.MAX_STEP);
            }
            steps[n] = parseStep(key, section.get(key));
        }
        checkMonotonic(steps);
        return new LadderTable(steps);
    }

    private static boolean isStepKey(String key) {
        for (int n = LadderTable.MIN_STEP; n <= LadderTable.MAX_STEP; n++) {
            if (key.equals("step-" + n)) return true;
        }
        return false;
    }

    private static LadderStep parseStep(String key, Object rowValue) {
        if (!(rowValue instanceof Map<?, ?> row)) {
            throw new LadderConfigException("ladder." + key + " must be a mapping of the six ladder values ("
                    + FIELDS_HUMAN + "), found: " + describe(rowValue));
        }
        for (Object k : row.keySet()) {
            if (!(k instanceof String field) || !FIELDS.contains(field)) {
                throw new LadderConfigException("ladder." + key + " has unknown key '" + k
                        + "' — allowed keys are: " + FIELDS_HUMAN + " (check for a typo)");
            }
        }
        double spawn = number(key, row, F_SPAWN);
        double capRaw = number(key, row, F_CAP);
        double normElite = number(key, row, F_NORM_ELITE);
        double normPlayer = number(key, row, F_NORM_PLAYER);
        double scaledElite = number(key, row, F_SCALED_ELITE);
        double scaledPlayer = number(key, row, F_SCALED_PLAYER);

        if (spawn <= 0.0 || spawn > 1.0) {
            throw new LadderConfigException("ladder." + key + ".spawn must be > 0 and <= 1"
                    + " (it is the fraction of spawns that become elites), found: " + fmt(spawn));
        }
        if (capRaw != Math.rint(capRaw)) {
            throw new LadderConfigException("ladder." + key + ".cap must be a whole number, found: " + fmt(capRaw));
        }
        if (capRaw < 1 || capRaw > CAP_MAX) {
            throw new LadderConfigException("ladder." + key + ".cap must be between 1 and " + CAP_MAX
                    + ", found: " + fmt(capRaw));
        }
        range(key, F_NORM_ELITE, normElite);
        range(key, F_NORM_PLAYER, normPlayer);
        range(key, F_SCALED_ELITE, scaledElite);
        range(key, F_SCALED_PLAYER, scaledPlayer);
        int stepNum = key.charAt(key.length() - 1) - '0';
        return new LadderStep(stepNum, spawn, (int) capRaw, normElite, normPlayer, scaledElite, scaledPlayer);
    }

    private static void range(String key, String field, double v) {
        if (v <= 0.0 || v > MULT_MAX) {
            throw new LadderConfigException("ladder." + key + "." + field + " must be > 0 and <= " + fmt(MULT_MAX)
                    + " (it is a damage multiplier), found: " + fmt(v));
        }
    }

    /** One required numeric field: present, a Number, finite. Never coerced. */
    private static double number(String key, Map<?, ?> row, String field) {
        if (!row.containsKey(field)) {
            throw new LadderConfigException("ladder." + key + " is missing '" + field
                    + "' — every step needs all six values (" + FIELDS_HUMAN + ")");
        }
        Object v = row.get(field);
        if (v == null) {
            throw new LadderConfigException("ladder." + key + "." + field + " is blank — a number is required");
        }
        if (v instanceof Boolean || !(v instanceof Number num)) {
            throw new LadderConfigException("ladder." + key + "." + field + " must be a number, found: "
                    + describe(v));
        }
        double d = num.doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw new LadderConfigException("ladder." + key + "." + field + " must be a finite number, found: "
                    + fmt(d));
        }
        return d;
    }

    private static void checkMonotonic(LadderStep[] steps) {
        for (int n = LadderTable.MIN_STEP + 1; n <= LadderTable.MAX_STEP; n++) {
            LadderStep prev = steps[n - 1], cur = steps[n];
            if (cur.spawn() < prev.spawn()) {
                throw mono("spawn", "must not decrease", prev, cur, cur.spawn(), prev.spawn());
            }
            if (cur.cap() < prev.cap()) {
                throw mono("cap", "must not decrease", prev, cur, cur.cap(), prev.cap());
            }
            if (cur.scaledPlayer() < prev.scaledPlayer()) {
                throw mono("scaled-to-player", "must not decrease (damage dealt to elites only grows as seals break)",
                        prev, cur, cur.scaledPlayer(), prev.scaledPlayer());
            }
            if (cur.scaledElite() > prev.scaledElite()) {
                throw mono("scaled-to-elite", "must not increase (damage taken from elites only shrinks as seals break)",
                        prev, cur, cur.scaledElite(), prev.scaledElite());
            }
        }
    }

    private static LadderConfigException mono(String field, String rule, LadderStep prev, LadderStep cur,
                                              double curV, double prevV) {
        return new LadderConfigException("ladder." + field + " " + rule + ": step-" + cur.step() + " is "
                + fmt(curV) + " but step-" + prev.step() + " is " + fmt(prevV)
                + " (plateaus are fine; going backwards is not)");
    }

    private static String describe(Object v) {
        if (v == null) return "nothing (blank)";
        if (v instanceof String s) return "'" + s + "' (text)";
        if (v instanceof Boolean b) return b + " (true/false)";
        if (v instanceof Map) return "a nested mapping";
        if (v instanceof Iterable) return "a list";
        return v + " (" + v.getClass().getSimpleName() + ")";
    }

    private static String fmt(double d) {
        if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
            return Long.toString((long) d);
        }
        return Double.toString(d);
    }

    /** The shipped default table as a mutable map tree — used by tests to build fixtures. */
    public static Map<String, Object> defaultSectionMap() {
        Map<String, Object> section = new LinkedHashMap<>();
        for (LadderStep s : LadderTable.builtin().steps()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(F_SPAWN, s.spawn());
            row.put(F_CAP, s.cap());
            row.put(F_NORM_ELITE, s.normElite());
            row.put(F_NORM_PLAYER, s.normPlayer());
            row.put(F_SCALED_ELITE, s.scaledElite());
            row.put(F_SCALED_PLAYER, s.scaledPlayer());
            section.put("step-" + s.step(), row);
        }
        return section;
    }
}
