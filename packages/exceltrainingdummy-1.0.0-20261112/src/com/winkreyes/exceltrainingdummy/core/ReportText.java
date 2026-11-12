package com.winkreyes.exceltrainingdummy.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Chat report rendering (DESIGN §3): checkpoint lines at each 25%
 * of the pool and the final report — duration, total damage, average
 * DPS, the per-source breakdown, and (hazardous tests) the tester's
 * damage taken + healing / HPS. Identical text on Java and Bedrock
 * (DESIGN §8): both platforms print these lines to chat.
 *
 * Boss-armor preset (DESIGN §4): when on, every damage number is
 * presented through the boss display scale (x1000 by default, as on
 * seal bosses) so practice numbers read like fight-night numbers.
 * The scale changes presentation only; the ledgers stay raw.
 */
public final class ReportText {
    private ReportText() {}

    public static String num(double v) {
        if (v == Math.floor(v) && Math.abs(v) < 1e15) return String.format(Locale.ROOT, "%,d", (long) v);
        return String.format(Locale.ROOT, "%,.1f", v);
    }

    public static String oneDp(double v) {
        return String.format(Locale.ROOT, "%,.1f", v);
    }

    private static double scaled(double raw, boolean bossArmor, double scaleFactor) {
        return bossArmor ? raw * scaleFactor : raw;
    }

    public static String checkpointLine(TestSession s, UUID attacker, long nowMillis,
                                        double fraction, boolean bossArmor, double scaleFactor) {
        int pct = (int) Math.round(fraction * 100);
        return "[Training Dummy] Checkpoint " + pct + "% — your DPS so far: "
                + oneDp(scaled(s.dps(attacker, nowMillis), bossArmor, scaleFactor))
                + " (" + num(scaled(s.consumed(), bossArmor, scaleFactor)) + " of "
                + num(scaled(s.pool(), bossArmor, scaleFactor)) + " damage, "
                + oneDp(s.elapsedSeconds(nowMillis)) + "s)";
    }

    /** The final report lines for one reader (their own breakdown). */
    public static List<String> finalReport(TestSession s, UUID reader, long nowMillis,
                                           Hazard hazard, boolean lethal,
                                           boolean bossArmor, double scaleFactor) {
        List<String> out = new ArrayList<>();
        String mode = s.endless() ? "Endless lap " + s.lap() : s.tier().displayName + " dummy";
        out.add("[Training Dummy] Test complete — " + mode
                + " (" + num(scaled(s.pool(), bossArmor, scaleFactor)) + " HP pool)");
        out.add("[Training Dummy] Duration: " + oneDp(s.elapsedSeconds(nowMillis))
                + "s · Total damage: " + num(scaled(s.totalDamage(), bossArmor, scaleFactor))
                + " · Average DPS: " + oneDp(scaled(s.dps(reader, nowMillis), bossArmor, scaleFactor)));
        DamageLedger mine = s.ledger(reader);
        if (mine != null && mine.total() > 0) {
            StringBuilder b = new StringBuilder("[Training Dummy] Your damage by source: ");
            boolean first = true;
            for (DamageLedger.Category c : DamageLedger.Category.values()) {
                double v = mine.of(c);
                if (v <= 0) continue;
                if (!first) b.append(" · ");
                first = false;
                b.append(c.label).append(' ')
                        .append(num(scaled(v, bossArmor, scaleFactor)))
                        .append(" (").append(Math.round(100.0 * v / mine.total())).append("%)");
            }
            out.add(b.toString());
        }
        if (hazard != Hazard.OFF) {
            out.add("[Training Dummy] Hazard " + hazard.displayName + (lethal ? " (lethal)" : "")
                    + " — you took " + num(s.hazardDamageTaken())
                    + " damage · healed " + num(s.healingDone())
                    + " (HPS " + oneDp(s.hps(nowMillis)) + ")");
        }
        if (bossArmor) {
            out.add("[Training Dummy] Boss-armor preset on: numbers shown at boss display scale (x"
                    + num(scaleFactor) + ").");
        }
        return out;
    }

    /** Live status for a non-owner tapping the dummy (DESIGN, two doors §2). */
    public static List<String> statusReadout(TestSession s, String ownerName, long nowMillis,
                                             Hazard hazard, boolean bossArmor, double scaleFactor) {
        List<String> out = new ArrayList<>();
        out.add("[Training Dummy] " + ownerName + "'s " + s.tier().displayName + " dummy"
                + (s.endless() ? " (Endless, lap " + s.lap() + ")" : "")
                + " — " + num(scaled(s.remaining(), bossArmor, scaleFactor)) + " of "
                + num(scaled(s.pool(), bossArmor, scaleFactor)) + " HP left");
        out.add("[Training Dummy] Total damage so far: "
                + num(scaled(s.totalDamage(), bossArmor, scaleFactor))
                + " · running " + oneDp(s.elapsedSeconds(nowMillis)) + "s"
                + (hazard != Hazard.OFF ? " · Hazard: " + hazard.displayName : ""));
        return out;
    }
}
