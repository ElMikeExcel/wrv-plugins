package excelemfixes.casterxp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player caster-xp-sharing state: the player's chosen
 * mode and the one-time catch-up record. Plain data only —
 * the module owns the YAML file this round-trips through —
 * so the state rules (defaults, the catch-up firing exactly
 * once) are unit-testable on a plain JVM.
 *
 * The catch-up record is the audit trail Mike's ruling
 * requires: the first time a player picks a non-STOCK mode,
 * the module closes their existing WANDS/STAVES gap once
 * and records it here (when, which track, how much, from
 * what totals). {@code catchUpDone} is set even when the
 * tracks were already equal (amount 0): the one-time grant
 * is a single event in a player's life with this module,
 * never a standing equalizer that re-fires whenever the
 * tracks drift apart again.
 */
public final class CasterXpState {

    /** One player's record. */
    public static final class PlayerState {
        public CasterXpMath.Mode mode = CasterXpMath.Mode.STOCK;
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

    /**
     * Marks the one-time catch-up as consumed for this
     * player, with its audit fields. Called exactly once per
     * player, at their first non-STOCK selection, whether or
     * not a grant was owed.
     */
    public void recordCatchUp(UUID uuid, String at, String track,
                              long amount, long wandsBefore,
                              long stavesBefore) {
        PlayerState ps = players.computeIfAbsent(uuid, k -> new PlayerState());
        ps.catchUpDone = true;
        ps.catchUpAt = at == null ? "" : at;
        ps.catchUpTrack = track == null ? "" : track;
        ps.catchUpAmount = Math.max(0, amount);
        ps.catchUpWandsBefore = wandsBefore;
        ps.catchUpStavesBefore = stavesBefore;
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
     *  mode, not the module. */
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
                ps.catchUpDone = Boolean.TRUE.equals(row.get("catch-up-done"));
                ps.catchUpAt = asString(row.get("catch-up-at"), "");
                ps.catchUpTrack = asString(row.get("catch-up-track"), "");
                ps.catchUpAmount = asLong(row.get("catch-up-amount"), 0);
                ps.catchUpWandsBefore = asLong(
                        row.get("catch-up-wands-before"), -1);
                ps.catchUpStavesBefore = asLong(
                        row.get("catch-up-staves-before"), -1);
                state.players.put(uuid, ps);
            } catch (IllegalArgumentException badUuid) {
                // skip the row, keep loading the rest
            }
        }
        return state;
    }

    private static long asLong(Object value, long fallback) {
        return value instanceof Number n ? n.longValue() : fallback;
    }

    private static String asString(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}
