package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.List;

/**
 * ExcelProtect 0.1.4 — PLAYTIME derivation: total time online for one
 * player over a window, computed from the session arcs Protect ALREADY
 * keeps — the players stream's player_join / player_leave records
 * (0.1.0). No new capture: the coverage map's row 30 gap was a missing
 * QUERY, not missing records. Pure math over scanned lines, so the
 * pairing rules are unit-tested without a server.
 *
 * Pairing rules (reality-shaped, stated plainly):
 *  - events sort by ts_ms; a join opens an arc, the next leave closes it.
 *  - a join while an arc is open (the leave was never recorded — crash,
 *    kick edge, recording toggled mid-session) closes the open arc AT
 *    that join: the gap is unknowable, and double-counting is worse.
 *  - a leave with no open arc is ignored (its join predates retention
 *    or recording) — it contributes nothing rather than a guess.
 *  - an arc still open at query time closes at `closeOpenAtMs` when the
 *    caller passes one ≥ 0 (the command layer passes `now` ONLY when
 *    the player is actually online right now); pass -1 to drop it.
 *  - totals count only each arc's OVERLAP with [fromMs, toMs] — an arc
 *    that started before the window counts from the window start.
 * Excel — Wink Reyes Valley
 */
public final class PlaytimeReport {
    private PlaytimeReport() {}

    public record Report(long totalMs, int sessions, long longestMs,
                         boolean openNow, long openSinceMs, long currentMs) {}

    /** [days] argument: default 1 (today), clamped 1–30 (retention), non-numeric → 1. */
    public static int parseDays(String raw) {
        if (raw == null || raw.isBlank()) return 1;
        try {
            int d = Integer.parseInt(raw.trim());
            return Math.max(1, Math.min(30, d));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * Derive playtime for `who` (exact name or uuid match) from scanned
     * players-stream lines over [fromMs, toMs].
     */
    public static Report build(List<String> scannedLines, String who, long fromMs, long toMs, long closeOpenAtMs) {
        List<long[]> events = new ArrayList<>(); // [tsMs, kind] kind: 1=join 0=leave
        for (String line : scannedLines) {
            String type = Json.strField(line, "type");
            boolean join = "player_join".equals(type);
            if (!join && !"player_leave".equals(type)) continue;
            String name = Json.strField(line, "player");
            String uuid = Json.strField(line, "player_uuid");
            if (!who.equals(name) && !who.equals(uuid)) continue;
            long ts = (long) Json.numField(line, "ts_ms", -1);
            if (ts < 0) continue;
            events.add(new long[]{ts, join ? 1 : 0});
        }
        events.sort((a, b) -> Long.compare(a[0], b[0]));

        long total = 0, longest = 0, openSince = -1;
        int sessions = 0;
        boolean openNow = false;
        long current = 0;
        for (long[] ev : events) {
            long ts = ev[0];
            if (ev[1] == 1) { // join
                if (openSince >= 0) { // previous leave never recorded: close at this join
                    long[] r = addArc(openSince, ts, fromMs, toMs);
                    total += r[0]; if (r[1] > 0) { sessions++; longest = Math.max(longest, r[0]); }
                }
                openSince = ts;
            } else { // leave
                if (openSince >= 0) {
                    long[] r = addArc(openSince, ts, fromMs, toMs);
                    total += r[0]; if (r[1] > 0) { sessions++; longest = Math.max(longest, r[0]); }
                    openSince = -1;
                }
            }
        }
        if (openSince >= 0 && closeOpenAtMs >= 0) {
            long[] r = addArc(openSince, closeOpenAtMs, fromMs, toMs);
            total += r[0];
            if (r[1] > 0) sessions++;
            longest = Math.max(longest, r[0]);
            openNow = true;
            current = Math.max(0, Math.min(closeOpenAtMs, toMs) - Math.max(openSince, fromMs));
        }
        return new Report(total, sessions, longest, openNow, openNow ? openSince : -1, current);
    }

    /** [overlapMs, counted 1/0] for one arc against the window. */
    private static long[] addArc(long start, long end, long fromMs, long toMs) {
        long o = Math.max(0, Math.min(end, toMs) - Math.max(start, fromMs));
        return new long[]{o, o > 0 ? 1 : 0};
    }

    /** "45s" / "12m" / "3h 12m" / "2d 4h" — console-compact durations. */
    public static String fmtDuration(long ms) {
        long s = Math.max(0, ms) / 1000;
        if (s < 60) return s + "s";
        long m = s / 60;
        if (m < 60) return m + "m";
        long h = m / 60;
        if (h < 24) return h + "h " + (m % 60) + "m";
        return (h / 24) + "d " + (h % 24) + "h";
    }
}
