package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.List;

/**
 * ExcelProtect 0.1.10 (P-PROTECT-WATCH-1) — the read side of the audit watch:
 * /excelprotect audit <player> [hours] renders one player's gameplay story
 * (casts, summon spawns/despawns, mana-reserve derivations, refusals) from
 * that player's own audit files, latest first. Pure over lines — the caller
 * reads audit/<player-key>-*.jsonl (plain or gz) and hands the lines in.
 * Excel — Wink Reyes Valley
 */
public final class AuditReport {

    private AuditReport() { }

    public record Report(int records, List<String> lines) { }

    /** Field order for the one-line rendering; absent fields are skipped. */
    private static final String[] SALIENT = {
            "spell", "summon", "mana_cost", "mana_after", "effective_mana_max", "mana_max",
            "entity_type", "branch", "origin", "reason", "hold_percent",
            "held_total_percent", "held_total", "usable_mana", "source"};

    /**
     * Build the report for one player from audit lines (any day mix) inside
     * the trailing {@code hours} window ending at {@code nowMs}. Latest
     * first, capped at {@code limit} rendered lines; records() counts every
     * matching record, rendered or not. file_header lines never count.
     */
    public static Report build(List<String> lines, String player, int hours, int limit, long nowMs) {
        long from = nowMs - Math.max(1, hours) * 3_600_000L;
        List<String[]> hits = new ArrayList<>(); // [tsMs, rendered]
        int records = 0;
        for (String line : lines) {
            if (line == null) continue;
            String type = Json.strField(line, "type");
            if (type == null || type.equals("file_header")) continue;
            String p = Json.strField(line, "player");
            if (p == null || !p.equalsIgnoreCase(player)) continue;
            long ts = (long) Json.numField(line, "ts_ms", 0);
            if (ts < from || ts > nowMs + 60_000) continue;
            records++;
            hits.add(new String[]{String.valueOf(ts), render(line, type, ts)});
        }
        hits.sort((a, b) -> Long.compare(Long.parseLong(b[0]), Long.parseLong(a[0])));
        List<String> out = new ArrayList<>();
        for (String[] h : hits) {
            if (out.size() >= Math.max(1, limit)) break;
            out.add(h[1]);
        }
        return new Report(records, out);
    }

    private static String render(String line, String type, long tsMs) {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(Ledger.isoOf(tsMs).substring(11, 19)).append("] ").append(type);
        for (String k : SALIENT) {
            String s = Json.strField(line, k);
            if (s != null) { sb.append(' ').append(k).append('=').append(s); continue; }
            double n = Json.numField(line, k, Double.NaN);
            if (!Double.isNaN(n)) sb.append(' ').append(k).append('=').append(Json.num(n));
        }
        return sb.toString();
    }
}
