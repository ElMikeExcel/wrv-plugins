package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.List;

/**
 * ExcelProtect 0.1.2 — text damage-incident reports for ANY sender (console
 * included), the InspectReport pattern: pure assembly over scanned ledger
 * lines, so the query form is fully unit-tested without a server. One compact
 * line per incident — totals and health, never bulky payloads (incident
 * records carry none). Excel — Wink Reyes Valley
 */
public final class DamageReport {
    private DamageReport() {}

    public record Report(int total, List<String> lines) {}

    /** [last-hours] argument: default 24, non-numeric → 24, clamped 0.1–720 (30 days = retention). */
    public static double parseHours(String raw) {
        if (raw == null || raw.isBlank()) return 24;
        try {
            double h = Double.parseDouble(raw.trim());
            if (Double.isNaN(h)) return 24;
            return Math.max(0.1, Math.min(720, h));
        } catch (NumberFormatException e) {
            return 24;
        }
    }

    public static String fmtHours(double h) {
        return (h == Math.rint(h) ? Long.toString((long) h) : Double.toString(h)) + "h";
    }

    /** Latest-first report over scanned `damage` stream lines; non-incident lines are skipped. */
    public static Report build(List<String> scannedLines, int limit) {
        List<String> incidents = new ArrayList<>();
        for (String line : scannedLines)
            if ("damage_incident".equals(Json.strField(line, "type"))) incidents.add(line);
        incidents.sort((a, b) -> Long.compare(startMs(b), startMs(a)));
        List<String> out = new ArrayList<>();
        for (String line : incidents) {
            if (out.size() >= limit) break;
            String s = summarize(line);
            if (s != null) out.add(s);
        }
        return new Report(incidents.size(), out);
    }

    private static long startMs(String line) {
        double v = Json.numField(line, "start_ts_ms", -1);
        if (v < 0) v = Json.numField(line, "ts_ms", -1);
        return (long) v;
    }

    /** One line: "21:14:05 zombie ELITE Lv17 — 34.5 dmg in 5 ticks, health 20→3.5 NEAR DEATH". */
    public static String summarize(String line) {
        if (!"damage_incident".equals(Json.strField(line, "type"))) return null;
        String start = Json.strField(line, "start_ts");
        String clock = start != null && start.length() >= 19 ? start.substring(11, 19) : "?";
        String kind = Json.strField(line, "attacker_kind");
        String cause = Json.strField(line, "cause");
        String source;
        if ("mob".equals(kind) || "entity".equals(kind)) {
            source = shortId(Json.strField(line, "attacker_entity"));
            if (line.contains("\"elite\":true"))
                source += " ELITE Lv" + (long) Json.numField(line, "elite_level", 0);
            if (Json.strField(line, "projectile") != null)
                source += " (" + shortId(Json.strField(line, "projectile")) + ")";
        } else if ("player".equals(kind)) {
            source = "player " + Json.strField(line, "attacker");
        } else if ("projectile".equals(kind)) {
            source = shortId(Json.strField(line, "projectile"));
        } else {
            source = cause == null ? "environment" : cause.toLowerCase().replace('_', ' ');
        }
        StringBuilder sb = new StringBuilder();
        sb.append(clock).append(' ').append(source).append(" — ")
          .append(Json.num(Json.numField(line, "total_damage", 0))).append(" dmg in ")
          .append((long) Json.numField(line, "tick_count", 0)).append(" ticks, health ")
          .append(Json.num(Json.numField(line, "health_start", 0))).append("→")
          .append(Json.num(Json.numField(line, "health_end", 0)));
        if (line.contains("\"died\":true")) sb.append(" DIED");
        else if (line.contains("\"near_death\":true")) sb.append(" NEAR DEATH");
        return sb.toString();
    }

    private static String shortId(String key) {
        if (key == null) return "?";
        return key.startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
    }
}
