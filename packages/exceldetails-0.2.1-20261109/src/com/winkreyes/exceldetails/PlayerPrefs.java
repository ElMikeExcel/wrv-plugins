package com.winkreyes.exceldetails;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player meter preferences: master toggle (default ON), the focus
 * window (0.1.0's single-window pref, kept as bookkeeping), and — new
 * in 0.2.0 — the MIX: three independent shown booleans, one per metric
 * (Mike's checkbox ruling, Nov 9, 2026). Defaults: damage done shown,
 * damage taken + healing hidden. Persistence is the plugin's job
 * (data.yml via DataFile); this is the live map.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class PlayerPrefs {
    private final Map<UUID, Boolean> enabled = new HashMap<>();
    private final Map<UUID, CombatStats.Window> windows = new HashMap<>();
    private final Map<UUID, EnumSet<CombatStats.Window>> shown = new HashMap<>();

    public boolean isEnabled(UUID id) { return enabled.getOrDefault(id, Boolean.TRUE); }
    public void setEnabled(UUID id, boolean on) { enabled.put(id, on); }
    public boolean toggle(UUID id) {
        boolean next = !isEnabled(id);
        enabled.put(id, next);
        return next;
    }

    /** The focus window: status anchor + where /details next cycles from. */
    public CombatStats.Window window(UUID id) {
        return windows.getOrDefault(id, CombatStats.Window.DAMAGE_DONE);
    }
    public void setWindow(UUID id, CombatStats.Window w) { windows.put(id, w); }

    // ----- the 0.2.0 mix -----

    /** Which metrics this player displays. Default: damage done only. */
    public EnumSet<CombatStats.Window> shownWindows(UUID id) {
        EnumSet<CombatStats.Window> s = shown.get(id);
        return s == null ? EnumSet.of(CombatStats.Window.DAMAGE_DONE) : EnumSet.copyOf(s);
    }

    public boolean shows(UUID id, CombatStats.Window w) { return shownWindows(id).contains(w); }

    public void setShown(UUID id, CombatStats.Window w, boolean on) {
        EnumSet<CombatStats.Window> s = shownWindows(id);
        if (on) s.add(w); else s.remove(w);
        shown.put(id, s);
    }

    /** Flip one metric's checkbox; returns its new state. */
    public boolean toggleShown(UUID id, CombatStats.Window w) {
        boolean next = !shows(id, w);
        setShown(id, w, next);
        return next;
    }

    /** Quick path (window / next): show exactly this metric, and focus it. */
    public void showOnly(UUID id, CombatStats.Window w) {
        shown.put(id, EnumSet.of(w));
        windows.put(id, w);
    }

    public Map<UUID, Boolean> enabledSnapshot() { return new HashMap<>(enabled); }
    public Map<UUID, CombatStats.Window> windowSnapshot() { return new HashMap<>(windows); }
    public Map<UUID, EnumSet<CombatStats.Window>> shownSnapshot() {
        Map<UUID, EnumSet<CombatStats.Window>> out = new HashMap<>();
        shown.forEach((k, v) -> out.put(k, EnumSet.copyOf(v)));
        return out;
    }
}
