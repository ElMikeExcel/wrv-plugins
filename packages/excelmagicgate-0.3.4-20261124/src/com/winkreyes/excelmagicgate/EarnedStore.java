package com.winkreyes.excelmagicgate;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * The credited-cast ledger for the Magic Rank earning system (0.3.0),
 * persisted as earned.yml in MagicGate's data folder (DESIGN.md §6).
 *
 * Pure value + tolerant map codec, no Bukkit deps — the plugin class
 * owns the file handling, this class owns the fields and the
 * parse/serialize rules, exactly the BridgeState split.
 *
 * Design invariants (DESIGN.md §6):
 * - ONE counter, ONE writer. creditedTotal is only ever mutated by
 *   the credit hook (live accrual) or the placement loader (once).
 *   It is NEVER re-derived from Magic's cast_count after placement:
 *   Magic's counter keeps counting uncredited casts too, so the two
 *   diverge permanently and by design.
 * - placedTotal keeps the placement component separately, for audit.
 * - The daily bucket is per player, per BASE FAMILY (tiers share one
 *   bucket — otherwise |2–|6 variants would multiply the cap), keyed
 *   by America/Chicago date. Only today's bucket is kept; a stale
 *   bucket reads as empty and is dropped on the next write (rollDay).
 *   Cap state is enforcement data, not history.
 */
public final class EarnedStore {

    /** One player's ledger row. */
    public static final class Row {
        private long creditedTotal;
        private long placedTotal;
        private String dailyDate = "";                 // yyyy-MM-dd, America/Chicago
        private final Map<String, Integer> dailyPerFamily = new LinkedHashMap<>();

        public long creditedTotal() { return creditedTotal; }
        public long placedTotal() { return placedTotal; }
        public String dailyDate() { return dailyDate; }
        public Map<String, Integer> dailyPerFamily() { return dailyPerFamily; }

        /** Credits earned today in one family (0 when the bucket is stale). */
        public int dailyCount(String family, LocalDate today) {
            if (!String.valueOf(today).equals(dailyDate)) {
                return 0;
            }
            return dailyPerFamily.getOrDefault(family, 0);
        }

        /** Roll the daily bucket to today if it is stale. True when rolled. */
        public boolean rollDay(LocalDate today) {
            String t = String.valueOf(today);
            if (t.equals(dailyDate)) {
                return false;
            }
            dailyDate = t;
            dailyPerFamily.clear();
            return true;
        }

        /** Record one credited cast (the credit hook is the only live caller). */
        public void addCredit(String family, LocalDate today) {
            rollDay(today);
            creditedTotal++;
            dailyPerFamily.merge(family, 1, Integer::sum);
        }
    }

    private boolean placementDone;
    private String placementDate = "";
    private String placementSource = "";
    private final Map<String, Row> players = new TreeMap<>();

    public boolean placementDone() { return placementDone; }
    public String placementDate() { return placementDate; }
    public String placementSource() { return placementSource; }
    public int playersTracked() { return players.size(); }
    public Map<String, Row> players() { return players; }

    public Row row(String uuid) {
        return players.get(uuid);
    }

    /** Credited total for a player (0 when untracked). */
    public long creditedTotal(String uuid) {
        Row r = players.get(uuid);
        return r == null ? 0 : r.creditedTotal();
    }

    /**
     * Seed a placement row (the placement loader is the only caller,
     * and only for a uuid with no row yet — placement wins, rows are
     * never overwritten).
     */
    public boolean seedPlacement(String uuid, long creditedTotal) {
        if (uuid == null || players.containsKey(uuid)) {
            return false;
        }
        Row r = new Row();
        r.creditedTotal = Math.max(0, creditedTotal);
        r.placedTotal = r.creditedTotal;
        players.put(uuid, r);
        return true;
    }

    /**
     * Seed a fallback row for a mage with no placement row, from the
     * live Mage API sum under the same classification (DESIGN.md §6:
     * covers a player who joined between pull and install). Existing
     * rows are never touched.
     */
    public boolean seedFallback(String uuid, long creditedTotal) {
        if (uuid == null || players.containsKey(uuid)) {
            return false;
        }
        Row r = new Row();
        r.creditedTotal = Math.max(0, creditedTotal);
        r.placedTotal = 0; // not a placement row; the audit trail says so
        players.put(uuid, r);
        return true;
    }

    /** Get-or-create the live row a credit will land on. */
    public Row rowForCredit(String uuid) {
        return players.computeIfAbsent(uuid, k -> new Row());
    }

    public void markPlacementDone(String date, String source) {
        this.placementDone = true;
        this.placementDate = date == null ? "" : date;
        this.placementSource = source == null ? "" : source;
    }

    // ------------------------------------------------------------------
    // Codec (earned.yml shape per DESIGN.md §6)
    // ------------------------------------------------------------------

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("version", 1);
        Map<String, Object> placement = new LinkedHashMap<>();
        placement.put("done", placementDone);
        placement.put("date", placementDate);
        placement.put("source", placementSource);
        m.put("placement", placement);
        Map<String, Object> ps = new LinkedHashMap<>();
        for (Map.Entry<String, Row> e : players.entrySet()) {
            Row r = e.getValue();
            Map<String, Object> rm = new LinkedHashMap<>();
            rm.put("credited_total", r.creditedTotal);
            rm.put("placed_total", r.placedTotal);
            Map<String, Object> daily = new LinkedHashMap<>();
            daily.put("date", r.dailyDate);
            daily.put("per_family", new LinkedHashMap<>(r.dailyPerFamily));
            rm.put("daily", daily);
            ps.put(e.getKey(), rm);
        }
        m.put("players", ps);
        return m;
    }

    /** Tolerant restore: missing or garbage values fall back to defaults. */
    public static EarnedStore fromMap(Map<String, Object> m) {
        EarnedStore s = new EarnedStore();
        if (m == null) {
            return s;
        }
        if (m.get("placement") instanceof Map<?, ?> p) {
            s.placementDone = Boolean.TRUE.equals(p.get("done"))
                    || "true".equals(String.valueOf(p.get("done")));
            s.placementDate = p.get("date") == null ? "" : String.valueOf(p.get("date"));
            s.placementSource = p.get("source") == null ? "" : String.valueOf(p.get("source"));
        }
        if (m.get("players") instanceof Map<?, ?> ps) {
            for (Map.Entry<?, ?> e : ps.entrySet()) {
                if (e.getKey() == null || !(e.getValue() instanceof Map<?, ?> rm)) {
                    continue;
                }
                Row r = new Row();
                r.creditedTotal = Math.max(0, asLong(rm.get("credited_total"), 0));
                r.placedTotal = Math.max(0, asLong(rm.get("placed_total"), 0));
                if (rm.get("daily") instanceof Map<?, ?> d) {
                    r.dailyDate = d.get("date") == null ? "" : String.valueOf(d.get("date"));
                    if (d.get("per_family") instanceof Map<?, ?> pf) {
                        for (Map.Entry<?, ?> fe : pf.entrySet()) {
                            if (fe.getKey() != null) {
                                r.dailyPerFamily.put(String.valueOf(fe.getKey()),
                                        Math.max(0, (int) asLong(fe.getValue(), 0)));
                            }
                        }
                    }
                }
                s.players.put(String.valueOf(e.getKey()), r);
            }
        }
        return s;
    }

    private static long asLong(Object v, long fallback) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v instanceof String str) {
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }
}
