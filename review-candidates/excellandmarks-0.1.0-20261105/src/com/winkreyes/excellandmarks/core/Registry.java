package com.winkreyes.excellandmarks.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-memory spot registry; the Bukkit layer persists it (SPEC §9). */
public final class Registry {

    private final Map<String, Spot> byId = new LinkedHashMap<>();
    private final Map<String, Spot> bySign = new LinkedHashMap<>();
    private int seq = 0;

    public String nextId() {
        return "lm-" + (++seq);
    }

    /** Restore the id counter after a load so ids never collide. */
    public void restoreSeq(int value) { this.seq = Math.max(this.seq, value); }

    public void add(Spot s) {
        byId.put(s.id, s);
        bySign.put(s.signKey(), s);
    }

    public Spot remove(String id) {
        Spot s = byId.remove(id);
        if (s != null) bySign.remove(s.signKey());
        return s;
    }

    public Spot byId(String id) { return id == null ? null : byId.get(id); }

    public Spot bySign(String world, int x, int y, int z) {
        return bySign.get(world + "@" + x + "," + y + "," + z);
    }

    public List<Spot> all() { return new ArrayList<>(byId.values()); }

    public List<Spot> ownedBy(java.util.UUID owner) {
        List<Spot> out = new ArrayList<>();
        for (Spot s : byId.values()) {
            if (owner != null && owner.equals(s.ownerId)) out.add(s);
        }
        return out;
    }

    public int size() { return byId.size(); }
}
