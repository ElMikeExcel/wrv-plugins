package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.List;

/**
 * ExcelProtect 0.1.3 — text HEALING reports for ANY sender (console included),
 * the DamageReport pattern: pure assembly over scanned ledger lines. One scan
 * serves the whole question because the Exporter player filter matches both
 * the `healer` and the `target` fields; this builder separates the three
 * directions for the queried player:
 *   self        — they healed themselves (healer == target)
 *   done        — they healed OTHER players (contribution: who keeps whom up)
 *   received    — other players healed them, split into attributed healers
 *                 and unattributed CUSTOM regains (healer = null)
 * Totals are event amounts with the cap-adjusted effective sum alongside.
 * Excel — Wink Reyes Valley
 */
public final class HealingReport {
    private HealingReport() {}

    public record Report(int total, double selfHealed, double selfEffective,
                         double doneHealed, double doneEffective,
                         double receivedHealed, double receivedUnknown,
                         List<String> lines) {}

    /** Direction-split report over scanned `healing` stream lines for player `who`. */
    public static Report build(List<String> scannedLines, String who, int limit) {
        List<String> incidents = new ArrayList<>();
        for (String line : scannedLines)
            if ("healing_incident".equals(Json.strField(line, "type"))) incidents.add(line);
        double self = 0, selfEff = 0, done = 0, doneEff = 0, recv = 0, recvUnknown = 0;
        for (String line : incidents) {
            String target = Json.strField(line, "target");
            String healer = Json.strField(line, "healer");
            double amt = Json.numField(line, "total_healed", 0);
            double eff = Json.numField(line, "total_effective", 0);
            boolean isSelf = line.contains("\"self\":true");
            if (who.equals(healer) && isSelf) { self += amt; selfEff += eff; }
            else if (who.equals(healer)) { done += amt; doneEff += eff; }
            else if (who.equals(target) && healer != null) { recv += amt; }
            else if (who.equals(target)) { recvUnknown += amt; }
        }
        incidents.sort((a, b) -> Long.compare(startMs(b), startMs(a)));
        List<String> out = new ArrayList<>();
        for (String line : incidents) {
            if (out.size() >= limit) break;
            String s = summarize(line, who);
            if (s != null) out.add(s);
        }
        return new Report(incidents.size(),
                CombatCore.round2(self), CombatCore.round2(selfEff),
                CombatCore.round2(done), CombatCore.round2(doneEff),
                CombatCore.round2(recv), CombatCore.round2(recvUnknown), out);
    }

    private static long startMs(String line) {
        double v = Json.numField(line, "start_ts_ms", -1);
        if (v < 0) v = Json.numField(line, "ts_ms", -1);
        return (long) v;
    }

    /** One line: "21:14:05 self REGEN — 6 healed (4.5 effective) in 3 events, health 10→14.5". */
    public static String summarize(String line, String who) {
        if (!"healing_incident".equals(Json.strField(line, "type"))) return null;
        String start = Json.strField(line, "start_ts");
        String clock = start != null && start.length() >= 19 ? start.substring(11, 19) : "?";
        String target = Json.strField(line, "target");
        String healer = Json.strField(line, "healer");
        String source = Json.strField(line, "source");
        String spell = Json.strField(line, "spell");
        StringBuilder sb = new StringBuilder();
        sb.append(clock).append(' ');
        if (line.contains("\"self\":true")) sb.append("self ").append(source);
        else if (who.equals(healer)) sb.append("→ ").append(target).append(' ').append(source);
        else if (healer != null) sb.append("← ").append(healer).append(' ').append(source);
        else sb.append("unattributed ").append(source);
        if (spell != null) sb.append(" [").append(spell).append(']');
        sb.append(" — ").append(Json.num(Json.numField(line, "total_healed", 0))).append(" healed (")
          .append(Json.num(Json.numField(line, "total_effective", 0))).append(" effective) in ")
          .append((long) Json.numField(line, "event_count", 0)).append(" events, health ")
          .append(Json.num(Json.numField(line, "health_start", 0))).append("→")
          .append(Json.num(Json.numField(line, "health_end", 0)));
        return sb.toString();
    }
}
