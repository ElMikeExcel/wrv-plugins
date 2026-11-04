package com.winkreyes.excelwandbars.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player wand style choice. Players not in the map get the default
 * (CLASSIC unless config says otherwise). Only players who used
 * /wandstyle are stored (whatever they picked, so an explicit Classic stays
 * Classic even if the default ever changes); everyone who never chose
 * follows the default.
 */
public final class StylePrefs {
    private final Map<UUID, WandStyle> picked = new HashMap<>();
    private WandStyle defaultStyle = WandStyle.CLASSIC;

    public WandStyle defaultStyle() { return defaultStyle; }
    public void setDefault(WandStyle s) { defaultStyle = s == null ? WandStyle.CLASSIC : s; }

    public WandStyle get(UUID id) {
        WandStyle s = id == null ? null : picked.get(id);
        return s == null ? defaultStyle : s;
    }

    /** Store a choice. Returns true when the player's effective style changed. */
    public boolean set(UUID id, WandStyle s) {
        if (id == null || s == null) return false;
        WandStyle before = get(id);
        picked.put(id, s);
        return before != s;
    }

    /** Forget a player's choice (back to the default). Returns true when something was stored. */
    public boolean clear(UUID id) { return id != null && picked.remove(id) != null; }

    public boolean hasChoice(UUID id) { return id != null && picked.containsKey(id); }

    public Map<UUID, WandStyle> all() { return Collections.unmodifiableMap(picked); }

    /** Load from string pairs (uuid -> "classic"/"bars"); bad lines are skipped and counted. */
    public int load(Map<String, String> raw) {
        picked.clear();
        int bad = 0;
        if (raw == null) return 0;
        for (Map.Entry<String, String> e : raw.entrySet()) {
            try {
                UUID id = UUID.fromString(e.getKey());
                WandStyle s = WandStyle.parse(e.getValue());
                if (s == null) { bad++; continue; }
                picked.put(id, s);
            } catch (IllegalArgumentException ex) { bad++; }
        }
        return bad;
    }

    public Map<String, String> save() {
        Map<String, String> out = new java.util.TreeMap<>();
        for (Map.Entry<UUID, WandStyle> e : picked.entrySet()) out.put(e.getKey().toString(), e.getValue().key());
        return out;
    }

    public int size() { return picked.size(); }
}

