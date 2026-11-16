package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ExcelProtect 0.1.3 — text damage-DEALT reports for ANY sender (console
 * included), the DamageReport pattern: pure assembly over scanned ledger
 * lines. Aggregates (total dealt, elite share, per-victim totals) answer
 * "who's doing what, and how much" at a glance — the contribution question —
 * and the latest incident lines give the per-engagement detail underneath.
 * Excel — Wink Reyes Valley
 */
public final class DealtReport {
    private DealtReport() {}

    public record Report(int total, double totalDamage, double eliteDamage,
                         List<String> byVictim, List<String> lines) {}

    /** Aggregates + latest-first incident lines over scanned `dealt` stream lines. */
    public static Report build(List<String> scannedLines, int limit) {
        List<String> incidents = new ArrayList<>();
        for (String line : scannedLines)
            if ("damage_dealt_incident".equals(Json.strField(line, "type"))) incidents.add(line);
        double total = 0, elite = 0;
        Map<String, double[]> byVictim = new LinkedHashMap<>(); // victim type -> [damage, incidents]
        for (String line : incidents) {
            double d = Json.numField(line, "total_damage", 0);
            total += d;
            if (line.contains("\"elite\":true")) elite += d;
            String v = Json.strField(line, "victim");
            double[] agg = byVictim.computeIfAbsent(v == null ? "?" : shortId(v), k -> new double[2]);
            agg[0] += d; agg[1] += 1;
        }
        List<Map.Entry<String, double[]>> ranked = new ArrayList<>(byVictim.entrySet());
        ranked.sort((a, b) -> Double.compare(b.getValue()[0], a.getValue()[0]));
        List<String> byLines = new ArrayList<>();
        for (Map.Entry<String, double[]> e : ranked)
            byLines.add(e.getKey() + " — " + Json.num(CombatCore.round2(e.getValue()[0]))
                    + " dmg across " + (long) e.getValue()[1] + " incident(s)");
        incidents.sort((a, b) -> Long.compare(startMs(b), startMs(a)));
        List<String> out = new ArrayList<>();
        for (String line : incidents) {
            if (out.size() >= limit) break;
            String s = summarize(line);
            if (s != null) out.add(s);
        }
        return new Report(incidents.size(), CombatCore.round2(total), CombatCore.round2(elite),
                byLines, out);
    }

    private static long startMs(String line) {
        double v = Json.numField(line, "start_ts_ms", -1);
        if (v < 0) v = Json.numField(line, "ts_ms", -1);
        return (long) v;
    }

    /** One line: "21:14:05 zombie ELITE Lv25 — 132.5 dmg in 9 hits, victim health 850→0 DIED". */
    public static String summarize(String line) {
        if (!"damage_dealt_incident".equals(Json.strField(line, "type"))) return null;
        String start = Json.strField(line, "start_ts");
        String clock = start != null && start.length() >= 19 ? start.substring(11, 19) : "?";
        StringBuilder sb = new StringBuilder();
        sb.append(clock).append(' ').append(shortId(Json.strField(line, "victim")));
        if (line.contains("\"elite\":true"))
            sb.append(" ELITE Lv").append((long) Json.numField(line, "elite_level", 0));
        String name = Json.strField(line, "victim_name");
        if (name != null) sb.append(" \"").append(name).append('"');
        if (Json.strField(line, "projectile") != null)
            sb.append(" (").append(shortId(Json.strField(line, "projectile"))).append(')');
        if (Json.strField(line, "spell") != null)
            sb.append(" [spell: ").append(Json.strField(line, "spell")).append(']');
        sb.append(" — ").append(Json.num(Json.numField(line, "total_damage", 0))).append(" dmg in ")
          .append((long) Json.numField(line, "hit_count", 0)).append(" hits, victim health ")
          .append(Json.num(Json.numField(line, "victim_health_start", 0))).append("→")
          .append(Json.num(Json.numField(line, "victim_health_end", 0)));
        if (line.contains("\"died\":true")) sb.append(" DIED");
        return sb.toString();
    }

    private static String shortId(String key) {
        if (key == null) return "?";
        return key.startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
    }
}
