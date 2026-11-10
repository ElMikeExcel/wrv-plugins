package com.winkreyes.exceldetails;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player meter preferences: toggle (default ON) + chosen window.
 * Persistence is the plugin's job (data.yml); this is the live map.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class PlayerPrefs {
    private final Map<UUID, Boolean> enabled = new HashMap<>();
    private final Map<UUID, CombatStats.Window> windows = new HashMap<>();

    public boolean isEnabled(UUID id) { return enabled.getOrDefault(id, Boolean.TRUE); }
    public void setEnabled(UUID id, boolean on) { enabled.put(id, on); }
    public boolean toggle(UUID id) {
        boolean next = !isEnabled(id);
        enabled.put(id, next);
        return next;
    }

    public CombatStats.Window window(UUID id) {
        return windows.getOrDefault(id, CombatStats.Window.DAMAGE_DONE);
    }
    public void setWindow(UUID id, CombatStats.Window w) { windows.put(id, w); }

    public Map<UUID, Boolean> enabledSnapshot() { return new HashMap<>(enabled); }
    public Map<UUID, CombatStats.Window> windowSnapshot() { return new HashMap<>(windows); }
}
