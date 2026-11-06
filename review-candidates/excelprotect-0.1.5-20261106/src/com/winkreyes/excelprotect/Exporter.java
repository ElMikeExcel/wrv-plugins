package com.winkreyes.excelprotect;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ExcelProtect — the query surface (SPEC §5): filtered scans across the daily JSONL
 * streams (plain + gzipped transparently). Used by /excelprotect export and rollback.
 * Filters: from/to (ms), stream, type (csv), player (name/uuid, any actor field),
 * match (substring), world, radius+at, encounter. Excel — Wink Reyes Valley
 */
public final class Exporter {
    public static final List<String> STREAMS = List.of("blocks", "containers", "entities",
            "items", "players", "locations", "economy", "audit", "combat", "damage", "dealt", "healing", "deaths", "system", "snapshots", "admin");

    public static final class Query {
        public long fromMs = -1, toMs = -1;
        public String stream, types, player, match, world, encounter;
        public double radius = -1, atX, atY, atZ;
        public int max = 100_000;

        public static Query parse(String[] tokens) {
            Query q = new Query();
            for (String t : tokens) {
                int eq = t.indexOf('=');
                if (eq < 0) continue;
                String k = t.substring(0, eq), v = t.substring(eq + 1);
                try {
                    switch (k) {
                        case "hours" -> { q.toMs = System.currentTimeMillis(); q.fromMs = q.toMs - (long) (Double.parseDouble(v) * 3_600_000); }
                        case "from" -> q.fromMs = Ledger.ZONE == null ? 0 : parseLocal(v);
                        case "to" -> q.toMs = parseLocal(v);
                        case "stream" -> q.stream = v;
                        case "type" -> q.types = v;
                        case "player" -> q.player = v;
                        case "match" -> q.match = v;
                        case "world" -> q.world = v;
                        case "encounter" -> q.encounter = v;
                        case "radius" -> q.radius = Double.parseDouble(v);
                        case "at" -> { String[] p = v.split(","); q.atX = Double.parseDouble(p[0]); q.atY = Double.parseDouble(p[1]); q.atZ = Double.parseDouble(p[2]); }
                        case "max" -> q.max = Integer.parseInt(v);
                        default -> { }
                    }
                } catch (Exception ignored) { }
            }
            return q;
        }

        private static long parseLocal(String v) {
            return java.time.LocalDateTime.parse(v.replace('T', ' '))
                    .atZone(Ledger.ZONE).toInstant().toEpochMilli();
        }

        boolean accepts(String line) {
            long ts = (long) Json.numField(line, "ts_ms", -1);
            if (fromMs >= 0 && ts < fromMs) return false;
            if (toMs >= 0 && ts > toMs) return false;
            if (types != null) {
                String type = Json.strField(line, "type");
                boolean ok = false;
                for (String t : types.split(",")) if (t.equals(type)) { ok = true; break; }
                if (!ok) return false;
            }
            if (match != null && !line.contains(match)) return false;
            if (world != null && !world.equals(Json.strField(line, "world"))) return false;
            if (encounter != null && !encounter.equals(Json.strField(line, "encounter"))
                    && !encounter.equals(Json.strField(line, "seal"))) return false;
            if (player != null) {
                boolean ok = false;
                // any actor field (0.1.4: attacker/victim/target uuids join the
                // list — combat lines name their actors in those fields, and a
                // player= scan that misses the combat stream is a false answer)
                for (String f : new String[]{"player", "player_uuid", "owner", "owner_uuid", "name", "healer", "healer_uuid", "target", "target_uuid", "credit", "attacker", "attacker_uuid", "victim_uuid"}) {
                    String val = Json.strField(line, f);
                    if (val != null && (val.equals(player) || val.contains(player))) { ok = true; break; }
                }
                if (!ok) return false;
            }
            if (radius >= 0) {
                if (!line.contains("\"x\":")) return false;
                double x = Json.numField(line, "x", Double.NaN), y = Json.numField(line, "y", Double.NaN),
                       z = Json.numField(line, "z", Double.NaN);
                if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) return false;
                double d2 = (x - atX) * (x - atX) + (y - atY) * (y - atY) + (z - atZ) * (z - atZ);
                if (d2 > radius * radius) return false;
            }
            return true;
        }
    }

    /** Scan matching lines in file order. Caller runs this async. */
    public static List<String> scan(Ledger ledger, Query q) {
        List<String> out = new ArrayList<>();
        List<String> streams = q.stream != null ? List.of(q.stream) : STREAMS;
        Map<String, Integer> perStream = new HashMap<>();
        for (String s : streams) {
            for (File f : ledger.streamFiles(s)) {
                try (var r = Ledger.open(f)) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (out.size() >= q.max) return out;
                        if (q.accepts(line)) out.add(line);
                    }
                } catch (Exception ignored) { }
            }
            perStream.put(s, out.size());
        }
        return out;
    }
}
