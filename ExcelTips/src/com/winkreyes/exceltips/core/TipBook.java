package com.winkreyes.exceltips.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per player, per feature tip state plus the "may this tip show now?" rules.
 * Pure Java (no Bukkit), so it is unit-tested on the plain JVM.
 *
 * Stored (survives restarts): shows (how many times the tip was shown) and
 * acked (the player said OK). Session only: last show per feature, last tip of
 * any kind, last combat, last Bedrock pop-up, last tip shown (for /tips ok).
 *
 * A tip shows on a use while: not acked, shows < maxShows (0 = no cap, keep
 * showing until OK), the same feature did not show within useGapMs (one burst
 * of uses = one tip), no combat within combatQuietMs, and no other tip within
 * minGapAnyMs (two features at once never stack). Blocked uses are not counted.
 */
public final class TipBook {
    public enum Result { SHOW, ACKED, DONE, GAP, COMBAT, BUSY, DISABLED, UNKNOWN }

    public static final class State {
        public int shows;
        public boolean acked;
        public State(int shows, boolean acked) { this.shows = shows; this.acked = acked; }
    }

    private final Map<UUID, Map<String, State>> data = new HashMap<>();
    private final Map<UUID, Map<String, Long>> lastShowByFeature = new HashMap<>();
    private final Map<UUID, Long> lastAny = new HashMap<>();
    private final Map<UUID, Long> lastCombat = new HashMap<>();
    private final Map<UUID, Long> lastForm = new HashMap<>();
    private final Map<UUID, String> lastShown = new HashMap<>();
    private final Map<UUID, Long> lastShownAt = new HashMap<>();
    private boolean dirty;

    public static String norm(String feature) { return feature == null ? "" : feature.trim().toLowerCase(java.util.Locale.ROOT); }

    public State state(UUID id, String feature) {
        Map<String, State> m = data.get(id);
        return m == null ? null : m.get(norm(feature));
    }
    public int shows(UUID id, String f) { State s = state(id, f); return s == null ? 0 : s.shows; }
    public boolean acked(UUID id, String f) { State s = state(id, f); return s != null && s.acked; }
    /** Finished = acked, or the cap was reached. */
    public boolean finished(UUID id, String f, int maxShows) { return acked(id, f) || (maxShows > 0 && shows(id, f) >= maxShows); }

    private State mut(UUID id, String f) {
        return data.computeIfAbsent(id, k -> new LinkedHashMap<>()).computeIfAbsent(norm(f), k -> new State(0, false));
    }

    public void combat(UUID id, long now) { lastCombat.put(id, now); }
    /** Death ends the fight: a respawned player is out of combat. */
    public void clearCombat(UUID id) { lastCombat.remove(id); }
    public boolean inCombat(UUID id, long now, long quietMs) {
        Long c = lastCombat.get(id);
        return c != null && now - c >= 0 && now - c < quietMs;
    }

    /** A use of a feature happened. SHOW means show it now; the show is counted. */
    public Result onUse(UUID id, String feature, long now, boolean enabled, int maxShows, long useGapMs, long minGapAnyMs, long combatQuietMs) {
        String f = norm(feature);
        if (!enabled) return Result.DISABLED;
        if (acked(id, f)) return Result.ACKED;
        if (maxShows > 0 && shows(id, f) >= maxShows) return Result.DONE;
        Map<String, Long> lf = lastShowByFeature.get(id);
        Long last = lf == null ? null : lf.get(f);
        if (last != null && now - last >= 0 && now - last < useGapMs) return Result.GAP;
        if (inCombat(id, now, combatQuietMs)) return Result.COMBAT;
        Long any = lastAny.get(id);
        if (any != null && now - any >= 0 && now - any < minGapAnyMs) return Result.BUSY;
        State s = mut(id, f);
        s.shows++;
        dirty = true;
        lastShowByFeature.computeIfAbsent(id, k -> new HashMap<>()).put(f, now);
        lastAny.put(id, now);
        lastShown.put(id, f);
        lastShownAt.put(id, now);
        return Result.SHOW;
    }

    /** May a Bedrock pop-up go out now? Yes marks it sent. */
    public boolean takeForm(UUID id, long now, long cooldownMs, long combatQuietMs) {
        if (inCombat(id, now, combatQuietMs)) return false;
        Long lf = lastForm.get(id);
        if (lf != null && now - lf >= 0 && now - lf < cooldownMs) return false;
        lastForm.put(id, now);
        return true;
    }

    /** The feature of the most recent tip this session, or null. */
    public String lastShown(UUID id) { return lastShown.get(id); }
    public long lastShownAt(UUID id) { Long t = lastShownAt.get(id); return t == null ? 0 : t; }

    /** Player said OK. Returns true when new. */
    public boolean ack(UUID id, String feature) {
        if (id == null || norm(feature).isEmpty()) return false;
        State s = mut(id, feature);
        if (s.acked) return false;
        s.acked = true;
        dirty = true;
        return true;
    }

    /** Admin reset: one feature, or all when feature is null. Returns how many were cleared. */
    public int reset(UUID id, String feature) {
        Map<String, State> m = data.get(id);
        int n = 0;
        if (m != null) {
            if (feature == null) { n = m.size(); data.remove(id); }
            else if (m.remove(norm(feature)) != null) n = 1;
        }
        Map<String, Long> lf = lastShowByFeature.get(id);
        if (lf != null) { if (feature == null) lastShowByFeature.remove(id); else lf.remove(norm(feature)); }
        if (feature == null || norm(feature).equals(lastShown.get(id))) { lastShown.remove(id); lastShownAt.remove(id); }
        lastAny.remove(id);
        lastForm.remove(id);
        if (n > 0) dirty = true;
        return n;
    }

    public void endSession(UUID id) {
        lastShowByFeature.remove(id); lastAny.remove(id); lastCombat.remove(id);
        lastForm.remove(id); lastShown.remove(id); lastShownAt.remove(id);
    }

    public void put(UUID id, String feature, int shows, boolean acked) {
        if (id == null || norm(feature).isEmpty()) return;
        if (shows <= 0 && !acked) return;
        State s = mut(id, feature); s.shows = Math.max(0, shows); s.acked = acked;
    }

    public Map<UUID, Map<String, State>> all() { return data; }
    public Set<UUID> players() { return new HashSet<>(data.keySet()); }
    public void clearAll() { data.clear(); lastShowByFeature.clear(); lastAny.clear(); lastCombat.clear(); lastForm.clear(); lastShown.clear(); lastShownAt.clear(); dirty = false; }
    public boolean dirty() { return dirty; }
    public void saved() { dirty = false; }
    public void markDirty() { dirty = true; }
}
