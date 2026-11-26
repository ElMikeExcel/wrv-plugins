package excelemfixes.casterxp;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player class-xp-sharing state: the player's chosen
 * mode (ONE global setting, as in 0.20.0) and the one-time
 * catch-up records — one per CLASS TREE since 0.21.0 (the
 * generalization to all classes). Plain data only — the
 * module owns the YAML file this round-trips through — so
 * the state rules (defaults, a tree's catch-up firing
 * exactly once, the 0.20.0 migration) are unit-testable on
 * a plain JVM.
 *
 * The catch-up record is the audit trail Mike's rulings
 * require: the first time a player picks a non-STOCK mode
 * with a form of a tree selected, the module closes their
 * existing gap between that form's two foundation skills
 * once and records it here (when, which form, which
 * track, how much, from what totals). A tree's record is
 * written even when the tracks were already equal
 * (amount 0): the one-time grant is a single event per
 * tree in a player's life with this module, never a
 * standing equalizer that re-fires whenever the tracks
 * drift apart again.
 *
 * MIGRATION (0.20.0 -> 0.21.0, a hard requirement of the
 * D243 build): 0.20.0 stored ONE catch-up per player,
 * for the Spellcaster tree only, in flat row fields
 * (catch-up-done / -at / -track / -amount /
 * -wands-before / -staves-before). Loading such a row
 * converts a completed 0.20.0 catch-up into the
 * spellcaster tree's record, so a catch-up that fired
 * under 0.20.0 can NEVER fire again under 0.21.0; a
 * 0.20.0 row whose catch-up never fired converts to no
 * record at all, so the owed catch-up stays owed.
 * Saving mirrors the spellcaster tree's record back
 * into the legacy flat fields, so rolling back to
 * 0.20.0 keeps honoring it too.
 */
public final class CasterXpState {

    /** The tree the 0.20.0 module covered (its only one). */
    public static final String LEGACY_TREE = "spellcaster";

    /** One tree's one-time catch-up audit record. */
    public static final class CatchUpRecord {
        public String at = "";
        /** The form selected when the catch-up fired (its
         *  pair is the one that was closed). "" for a
         *  record migrated from 0.20.0, which did not
         *  store the form. */
        public String form = "";
        /** The lagging track that was raised ("" when the
         *  tracks were already equal and nothing moved). */
        public String track = "";
        public long amount = 0;
        /** The pair, in catalog order, and both tracks'
         *  totals before the grant (-1 when unknown, e.g.
         *  a migrated row that stored no totals). */
        public String skillA = "";
        public String skillB = "";
        public long beforeA = -1;
        public long beforeB = -1;
    }

    /** One player's record. */
    public static final class PlayerState {
        public CasterXpMath.Mode mode = CasterXpMath.Mode.STOCK;
        /** Catch-up records by tree id (insertion order). */
        public final Map<String, CatchUpRecord> catchUps =
                new LinkedHashMap<>();
        // Legacy flat mirror of the spellcaster record —
        // written on save, read on load for 0.20.0 rows.
        // Kept in step by syncLegacyMirror(); do not set
        // these directly outside this class.
        public boolean catchUpDone = false;
        public String catchUpAt = "";
        public String catchUpTrack = "";
        public long catchUpAmount = 0;
        public long catchUpWandsBefore = -1;
        public long catchUpStavesBefore = -1;
    }

    private final Map<UUID, PlayerState> players = new HashMap<>();
    private CasterXpMath.Mode defaultMode = CasterXpMath.Mode.STOCK;

    public void setDefaultMode(CasterXpMath.Mode mode) {
        defaultMode = mode == null ? CasterXpMath.Mode.STOCK : mode;
    }

    public CasterXpMath.Mode defaultMode() {
        return defaultMode;
    }

    /** The player's stored record, or null when they have
     *  never interacted with the module. */
    public PlayerState record(UUID uuid) {
        return players.get(uuid);
    }

    /** The player's effective mode: their stored choice, or
     *  the configured default when they have none. */
    public CasterXpMath.Mode modeOf(UUID uuid) {
        PlayerState ps = players.get(uuid);
        return ps == null ? defaultMode : ps.mode;
    }

    /** Stores the player's mode choice, creating the record
     *  on first use. Returns the record. */
    public PlayerState choose(UUID uuid, CasterXpMath.Mode mode) {
        PlayerState ps = players.computeIfAbsent(uuid, k -> new PlayerState());
        ps.mode = mode == null ? CasterXpMath.Mode.STOCK : mode;
        return ps;
    }

    /** True when the tree's one-time catch-up is consumed
     *  for this player (fired, or migrated as fired). */
    public boolean catchUpDone(UUID uuid, String tree) {
        PlayerState ps = players.get(uuid);
        return ps != null && tree != null && ps.catchUps.containsKey(tree);
    }

    /** The tree's catch-up record, or null. */
    public CatchUpRecord catchUpRecord(UUID uuid, String tree) {
        PlayerState ps = players.get(uuid);
        return ps == null || tree == null ? null : ps.catchUps.get(tree);
    }

    /**
     * Records a tree's one-time catch-up as consumed, with
     * its audit fields. Called exactly once per tree per
     * player, at their first non-STOCK selection with a
     * form of that tree selected, whether or not a grant
     * was owed.
     */
    public void recordCatchUp(UUID uuid, String tree,
                              CatchUpRecord record) {
        PlayerState ps = players.computeIfAbsent(uuid, k -> new PlayerState());
        ps.catchUps.put(tree, record == null ? new CatchUpRecord() : record);
        syncLegacyMirror(ps);
    }

    /**
     * The 0.20.0-shaped single catch-up, kept as a thin
     * delegate onto the spellcaster tree so the shipped
     * module's call and audit shape carry over unchanged.
     */
    public void recordCatchUp(UUID uuid, String at, String track,
                              long amount, long wandsBefore,
                              long stavesBefore) {
        CatchUpRecord r = new CatchUpRecord();
        r.at = at == null ? "" : at;
        r.track = track == null ? "" : track;
        r.amount = Math.max(0, amount);
        r.skillA = CasterXpMath.WANDS;
        r.skillB = CasterXpMath.STAVES;
        r.beforeA = wandsBefore;
        r.beforeB = stavesBefore;
        recordCatchUp(uuid, LEGACY_TREE, r);
    }

    /** Keeps the legacy flat fields mirroring the
     *  spellcaster tree's record (or cleared when the tree
     *  has none). */
    private static void syncLegacyMirror(PlayerState ps) {
        CatchUpRecord r = ps.catchUps.get(LEGACY_TREE);
        if (r == null) {
            ps.catchUpDone = false;
            ps.catchUpAt = "";
            ps.catchUpTrack = "";
            ps.catchUpAmount = 0;
            ps.catchUpWandsBefore = -1;
            ps.catchUpStavesBefore = -1;
            return;
        }
        ps.catchUpDone = true;
        ps.catchUpAt = r.at;
        ps.catchUpTrack = r.track;
        ps.catchUpAmount = r.amount;
        // The legacy fields are WANDS/STAVES-shaped; only a
        // WANDS+STAVES pair record maps onto them exactly.
        // The catalog's spellcaster pair order is STAVES
        // first, so map by skill name in either order.
        if (CasterXpMath.WANDS.equals(r.skillA)
                && CasterXpMath.STAVES.equals(r.skillB)) {
            ps.catchUpWandsBefore = r.beforeA;
            ps.catchUpStavesBefore = r.beforeB;
        } else if (CasterXpMath.STAVES.equals(r.skillA)
                && CasterXpMath.WANDS.equals(r.skillB)) {
            ps.catchUpStavesBefore = r.beforeA;
            ps.catchUpWandsBefore = r.beforeB;
        } else {
            ps.catchUpWandsBefore = -1;
            ps.catchUpStavesBefore = -1;
        }
    }

    public int size() {
        return players.size();
    }

    // ------------------------------------------------------ persistence

    /** Serializes to plain maps (the module writes these to
     *  its YAML data file). */
    public Map<String, Object> toMap() {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<UUID, PlayerState> e : players.entrySet()) {
            PlayerState ps = e.getValue();
            Map<String, Object> row = new HashMap<>();
            row.put("mode", ps.mode.name());
            Map<String, Object> catchUps = new LinkedHashMap<>();
            for (Map.Entry<String, CatchUpRecord> ce
                    : ps.catchUps.entrySet()) {
                CatchUpRecord r = ce.getValue();
                Map<String, Object> cr = new LinkedHashMap<>();
                cr.put("at", r.at);
                cr.put("form", r.form);
                cr.put("track", r.track);
                cr.put("amount", r.amount);
                cr.put("skill-a", r.skillA);
                cr.put("skill-b", r.skillB);
                cr.put("before-a", r.beforeA);
                cr.put("before-b", r.beforeB);
                catchUps.put(ce.getKey(), cr);
            }
            row.put("catch-ups", catchUps);
            row.put("catch-up-done", ps.catchUpDone);
            row.put("catch-up-at", ps.catchUpAt);
            row.put("catch-up-track", ps.catchUpTrack);
            row.put("catch-up-amount", ps.catchUpAmount);
            row.put("catch-up-wands-before", ps.catchUpWandsBefore);
            row.put("catch-up-staves-before", ps.catchUpStavesBefore);
            out.put(e.getKey().toString(), row);
        }
        return out;
    }

    /** Loads from plain maps (the module reads these from
     *  its YAML data file). Malformed rows are skipped, never
     *  fatal: a corrupt row costs one player their stored
     *  mode, not the module. Rows in the 0.20.0 shape (no
     *  "catch-ups" section) are migrated per the class
     *  javadoc. */
    public static CasterXpState fromMap(Map<String, Object> map) {
        CasterXpState state = new CasterXpState();
        if (map == null) return state;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            try {
                UUID uuid = UUID.fromString(e.getKey());
                if (!(e.getValue() instanceof Map<?, ?> row)) continue;
                PlayerState ps = new PlayerState();
                ps.mode = CasterXpMath.Mode.parse(
                        asString(row.get("mode"), "STOCK"));
                loadCatchUps(ps, row);
                state.players.put(uuid, ps);
            } catch (IllegalArgumentException badUuid) {
                // skip the row, keep loading the rest
            }
        }
        return state;
    }

    private static void loadCatchUps(PlayerState ps, Map<?, ?> row) {
        Object section = row.get("catch-ups");
        if (section instanceof Map<?, ?> catchUps) {
            for (Map.Entry<?, ?> ce : catchUps.entrySet()) {
                if (!(ce.getValue() instanceof Map<?, ?> cr)) continue;
                CatchUpRecord r = new CatchUpRecord();
                r.at = asString(cr.get("at"), "");
                r.form = asString(cr.get("form"), "");
                r.track = asString(cr.get("track"), "");
                r.amount = Math.max(0, asLong(cr.get("amount"), 0));
                r.skillA = asString(cr.get("skill-a"), "");
                r.skillB = asString(cr.get("skill-b"), "");
                r.beforeA = asLong(cr.get("before-a"), -1);
                r.beforeB = asLong(cr.get("before-b"), -1);
                ps.catchUps.put(String.valueOf(ce.getKey()), r);
            }
        }
        // 0.20.0 migration: a completed flat catch-up with
        // no spellcaster tree record yet becomes that
        // record — fired under 0.20.0, never to refire.
        if (!ps.catchUps.containsKey(LEGACY_TREE)
                && Boolean.TRUE.equals(row.get("catch-up-done"))) {
            CatchUpRecord r = new CatchUpRecord();
            r.at = asString(row.get("catch-up-at"), "");
            r.form = "";
            r.track = asString(row.get("catch-up-track"), "");
            r.amount = Math.max(0, asLong(row.get("catch-up-amount"), 0));
            r.skillA = CasterXpMath.WANDS;
            r.skillB = CasterXpMath.STAVES;
            r.beforeA = asLong(row.get("catch-up-wands-before"), -1);
            r.beforeB = asLong(row.get("catch-up-staves-before"), -1);
            ps.catchUps.put(LEGACY_TREE, r);
        }
        syncLegacyMirror(ps);
    }

    private static long asLong(Object value, long fallback) {
        return value instanceof Number n ? n.longValue() : fallback;
    }

    private static String asString(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}
