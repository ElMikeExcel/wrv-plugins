package com.winkreyes.exceltogether;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * When the party range line may be shown on the action bar (0.4.1). Pure state, clock passed in.
 * Modes: ALWAYS = the 0.3.0 behaviour (re-send on every tick of the indicator); CHANGES = show a new
 * in-range count once it has held for holdMs, then only as a repeatMs reminder; OFF = never.
 */
public final class BarPolicy {
    public enum Mode {
        ALWAYS, CHANGES, OFF;

        public static Mode parse(String s) {
            if (s == null) return CHANGES;
            switch (s.trim().toLowerCase(Locale.ROOT)) {
                case "always", "constant", "true", "on" -> { return ALWAYS; }
                case "off", "never", "false", "none" -> { return OFF; }
                default -> { return CHANGES; }
            }
        }
    }

    /** Per-player line state. */
    private static final class S {
        Integer shown; Integer pending; long since; long lastAt;
    }

    /** A minimum gap between two gain lines for the same player. */
    public static final class Gap {
        private final long gapMs;
        private final Map<UUID, Long> last = new HashMap<>();

        public Gap(long gapMs) { this.gapMs = Math.max(0L, gapMs); }
        public long gapMs() { return gapMs; }

        public synchronized boolean due(UUID id, long now) {
            if (gapMs < 0) return false;
            Long t = last.get(id);
            if (t != null && now - t >= 0 && now - t < gapMs) return false;
            last.put(id, now);
            return true;
        }

        public synchronized void forget(UUID id) { last.remove(id); }
    }

    private final Mode mode;
    private final long holdMs;
    private final long repeatMs;
    private final Map<UUID, S> st = new HashMap<>();

    public BarPolicy(Mode mode, long holdMs) { this(mode, holdMs, 0L); }

    public BarPolicy(Mode mode, long holdMs, long repeatMs) {
        this.mode = mode == null ? Mode.CHANGES : mode;
        this.holdMs = Math.max(0L, holdMs);
        this.repeatMs = Math.max(0L, repeatMs);
    }

    public long repeatMs() { return repeatMs; }
    public Mode mode() { return mode; }
    public long holdMs() { return holdMs; }

    /** True if the line with this in-range count may be sent to this player now. */
    public synchronized boolean show(UUID id, int n, long now) {
        if (mode == Mode.OFF) return false;
        if (mode == Mode.ALWAYS) return true;
        S s = st.computeIfAbsent(id, k -> new S());
        if (s.shown == null) { s.shown = n; s.pending = null; s.lastAt = now; return true; }
        if (n == s.shown) {
            s.pending = null;
            if (repeatMs > 0 && now - s.lastAt >= repeatMs) { s.lastAt = now; return true; }
            return false;
        }
        if (s.pending == null || s.pending != n) {
            s.pending = n; s.since = now;
            if (holdMs > 0) return false;
        }
        if (now - s.since < holdMs) return false;
        s.shown = n; s.pending = null; s.lastAt = now;
        return true;
    }

    /** Another party line (a gain line) was shown: restart the reminder clock. */
    public synchronized void touch(UUID id, long now) {
        S s = st.get(id);
        if (s != null) s.lastAt = now;
    }

    public synchronized void forget(UUID id) { st.remove(id); }

    public synchronized Integer shown(UUID id) {
        S s = st.get(id);
        return s == null ? null : s.shown;
    }
}
