package excelemfixes.mount.core;

import java.util.Map;

/**
 * The purchase-gate math as a pure decision (unit-testable):
 * a player passes when ANY single package is at least the
 * configured fraction complete — "halfway through Primis OR
 * halfway through any one expansion" (D204). Progress in
 * different packages never combines; the gate looks for one
 * package genuinely half done, not a smear of starts.
 */
public final class QuestGateMath {

    private QuestGateMath() {}

    /** One package's standing: how many of its quests the player
     *  finished, how many it has, and how many the gate asks
     *  for. A package with no quests can never pass — an empty
     *  denominator is a content problem, not a free pass. */
    public record Progress(String packageId, int completed, int total,
                           int required) {
        public boolean passes() {
            return total > 0 && completed >= required;
        }

        /** Share of the requirement met, 0.0–1.0+ (the "best
         *  progress" measure for picking which package to show
         *  the player). Zero when the package has no quests. */
        public double shareOfRequired() {
            if (total <= 0 || required <= 0) return 0.0;
            return (double) completed / (double) required;
        }
    }

    /** Completions required: the ceiling of total × fraction,
     *  at least 1 for a non-empty package (a 1-quest package
     *  asks for that quest, never for zero). */
    public static int required(int total, double fraction) {
        if (total <= 0) return 0;
        double f = fraction <= 0.0 ? 0.5 : Math.min(fraction, 1.0);
        return Math.max(1, (int) Math.ceil(total * f));
    }

    /** The package the player is furthest through, measured
     *  against its own requirement; ties break toward more
     *  completions, then toward the first configured package.
     *  Null when no packages are configured at all. */
    public static Progress best(Map<String, Integer> totals,
                                  Map<String, Integer> completed,
                                  double fraction) {
        if (totals == null || totals.isEmpty()) return null;
        Progress best = null;
        for (Map.Entry<String, Integer> entry : totals.entrySet()) {
            int total = entry.getValue() == null ? 0 : entry.getValue();
            int done = completed == null ? 0
                    : completed.getOrDefault(entry.getKey(), 0);
            Progress candidate = new Progress(entry.getKey(), done, total,
                    required(total, fraction));
            if (best == null
                    || candidate.shareOfRequired()
                            > best.shareOfRequired()
                    || (candidate.shareOfRequired() == best.shareOfRequired()
                            && candidate.completed() > best.completed())) {
                best = candidate;
            }
        }
        return best;
    }

    /** The gate itself: the best package passes. */
    public static boolean passes(Map<String, Integer> totals,
                                 Map<String, Integer> completed,
                                 double fraction) {
        Progress best = best(totals, completed, fraction);
        return best != null && best.passes();
    }
}
