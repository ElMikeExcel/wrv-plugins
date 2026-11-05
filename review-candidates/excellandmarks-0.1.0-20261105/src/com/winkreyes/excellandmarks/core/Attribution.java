package com.winkreyes.excellandmarks.core;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelProtect placement attribution (SPEC §5): Protect's blocks stream
 * writes one flat JSON object per line —
 * {world, x, y, z, action, block, player_uuid, ts_ms, ...} — where action
 * is "place" / "break" / ... and block is a namespaced material key.
 * The tally reduces the stream to the LATEST action per position inside
 * a spot's region; a position counts as the renter's material when its
 * latest action is a place by the renter. Field order is not assumed;
 * unknown or malformed lines are skipped, never fatal (fail-soft).
 */
public final class Attribution {

    public record Entry(String world, int x, int y, int z, String action, String block,
                        UUID player, long tsMs) { }

    private Attribution() { }

    /** Parse one blocks-stream line; null when it is not a usable record. */
    public static Entry parseLine(String line) {
        if (line == null) return null;
        String world = strField(line, "world");
        String action = strField(line, "action");
        String block = strField(line, "block");
        String uuid = strField(line, "player_uuid");
        Long x = numField(line, "x"), y = numField(line, "y"), z = numField(line, "z");
        Long ts = numField(line, "ts_ms");
        if (world == null || action == null || x == null || y == null || z == null) return null;
        UUID player = null;
        if (uuid != null) {
            try { player = UUID.fromString(uuid); } catch (IllegalArgumentException ignored) { }
        }
        return new Entry(world, x.intValue(), y.intValue(), z.intValue(), action,
                block == null ? "" : block, player, ts == null ? 0L : ts);
    }

    private static String key(Entry e) {
        return e.world() + "@" + e.x() + "," + e.y() + "," + e.z();
    }

    /**
     * Reduce a stream of entries to the latest action per position.
     * Entries are consumed in file order; a later line wins unless its
     * ts_ms is strictly older than the recorded one (day files are read
     * oldest first, so this only guards same-line ties).
     */
    public static Map<String, Entry> latestPerPosition(Iterable<Entry> entries) {
        Map<String, Entry> latest = new HashMap<>();
        for (Entry e : entries) {
            if (e == null) continue;
            String k = key(e);
            Entry cur = latest.get(k);
            if (cur == null || e.tsMs() >= cur.tsMs()) latest.put(k, e);
        }
        return latest;
    }

    /**
     * Materials tally for one renter in one spot region: material key →
     * count, over positions whose latest action is a place by the renter.
     * Positions are returned too (the sweep must clear exactly these).
     */
    public static final class Tally {
        public final Map<String, Integer> materials = new LinkedHashMap<>();
        public final Map<String, Entry> positions = new LinkedHashMap<>();
        public int totalBlocks() {
            int n = 0;
            for (int c : materials.values()) n += c;
            return n;
        }
    }

    public static Tally tally(Map<String, Entry> latest, Spot spot, UUID renter,
                              int yBelow, int yAbove) {
        Tally t = new Tally();
        for (Entry e : latest.values()) {
            if (!"place".equals(e.action())) continue;
            if (e.player() == null || !e.player().equals(renter)) continue;
            if (!spot.inRegion(e.world(), e.x(), e.y(), e.z(), yBelow, yAbove)) continue;
            if (e.block().isEmpty() || e.block().endsWith(":air")) continue;
            t.materials.merge(e.block(), 1, Integer::sum);
            t.positions.put(key(e), e);
        }
        return t;
    }

    // ---- minimal flat-JSON field extraction (Protect lines are flat) ----

    static String strField(String json, String field) {
        String needle = "\"" + field + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int colon = json.indexOf(':', i + needle.length());
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon);
        if (q1 < 0) return null;
        StringBuilder sb = new StringBuilder();
        for (int p = q1 + 1; p < json.length(); p++) {
            char c = json.charAt(p);
            if (c == '\\' && p + 1 < json.length()) { sb.append(json.charAt(++p)); continue; }
            if (c == '"') return sb.toString();
            sb.append(c);
        }
        return null;
    }

    static Long numField(String json, String field) {
        String needle = "\"" + field + "\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int colon = json.indexOf(':', i + needle.length());
        if (colon < 0) return null;
        int p = colon + 1;
        while (p < json.length() && Character.isWhitespace(json.charAt(p))) p++;
        int start = p;
        if (p < json.length() && (json.charAt(p) == '-' )) p++;
        while (p < json.length() && (Character.isDigit(json.charAt(p)) || json.charAt(p) == '.')) p++;
        if (p == start) return null;
        try {
            String s = json.substring(start, p);
            int dot = s.indexOf('.');
            if (dot >= 0) s = s.substring(0, dot);
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
