/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The elder-guardian chapter's monument book (settled Q6, Mike,
 * Oct 18, 2026): the elder seal requires ALL THREE elder guardians
 * of ONE monument, each kill individually fought for. Three elders
 * spawn per monument (top room + two wing rooms) and never respawn
 * in vanilla, so progress is tracked per monument:
 *
 * - an elder is assigned to a monument the first time it is
 *   encountered, by clustering home positions (same world, within
 *   CLUSTER_RADIUS blocks horizontally - a monument is ~58 blocks
 *   wide, so its three elders always cluster together, and two
 *   monuments never do);
 * - a qualifying kill marks that elder COUNTED toward its monument;
 * - a kill that does not count (failed gate, or the seal on
 *   cooldown) schedules a plugin respawn of that elder at its home
 *   position after RESPAWN delay, so the trio stays completable;
 * - the trio is complete at 3 counted elders of one monument.
 *
 * Pure logic; the adapter persists the snapshot in state.yml so
 * progress and pending respawns survive restarts.
 */
public final class MonumentTracker {

    public static final double CLUSTER_RADIUS = 100.0;
    public static final int TRIO_SIZE = 3;

    public enum ElderStatus {
        TRACKED,
        COUNTED,
        PENDING_RESPAWN
    }

    /** One elder's record. */
    public static final class ElderRecord {
        public final UUID uuid;
        public final String monumentId;
        public final String world;
        public final double x;
        public final double y;
        public final double z;
        public ElderStatus status;
        public long respawnDueMs;
        /** 0.5.6 (seal-cycle law (a)): when this elder was
         *  COUNTED, epoch millis; 0 = not counted, or counted
         *  before timestamps existed (the adapter migrates 0s
         *  to the boot time so no trio expires instantly at
         *  first boot — design section 3.5). */
        public long countedAtMs;

        ElderRecord(UUID uuid, String monumentId, String world,
                    double x, double y, double z) {
            this.uuid = uuid;
            this.monumentId = monumentId;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.status = ElderStatus.TRACKED;
            this.respawnDueMs = 0L;
            this.countedAtMs = 0L;
        }
    }

    private final Map<UUID, ElderRecord> elders = new LinkedHashMap<>();
    private final Map<String, double[]> anchors = new LinkedHashMap<>(); // monumentId -> {x, z} + world in map below
    private final Map<String, String> anchorWorlds = new LinkedHashMap<>();
    private int monumentSeq = 0;

    /**
     * Register an elder at its home position (first encounter), or
     * return its existing record. Returns the record either way.
     */
    public synchronized ElderRecord register(UUID elderUuid, String world,
                                             double x, double y, double z) {
        ElderRecord existing = elders.get(elderUuid);
        if (existing != null) {
            return existing;
        }
        String monumentId = null;
        for (Map.Entry<String, double[]> e : anchors.entrySet()) {
            if (!world.equals(anchorWorlds.get(e.getKey()))) {
                continue;
            }
            double[] a = e.getValue();
            double dx = a[0] - x;
            double dz = a[1] - z;
            if (dx * dx + dz * dz <= CLUSTER_RADIUS * CLUSTER_RADIUS) {
                monumentId = e.getKey();
                break;
            }
        }
        if (monumentId == null) {
            monumentId = "monument-" + (++monumentSeq);
            anchors.put(monumentId, new double[]{x, z});
            anchorWorlds.put(monumentId, world);
        }
        ElderRecord rec = new ElderRecord(elderUuid, monumentId, world, x, y, z);
        elders.put(elderUuid, rec);
        return rec;
    }

    public synchronized ElderRecord record(UUID elderUuid) {
        return elders.get(elderUuid);
    }

    /** The monument an elder belongs to, or null when unknown. */
    public synchronized String monumentOf(UUID elderUuid) {
        ElderRecord r = elders.get(elderUuid);
        return r == null ? null : r.monumentId;
    }

    /** Counted elders of one monument (its trio progress, 0..3). */
    public synchronized int progress(String monumentId) {
        int n = 0;
        for (ElderRecord r : elders.values()) {
            if (r.monumentId.equals(monumentId) && r.status == ElderStatus.COUNTED) {
                n++;
            }
        }
        return n;
    }

    public synchronized boolean trioComplete(String monumentId) {
        return progress(monumentId) >= TRIO_SIZE;
    }

    /**
     * Mark an elder's kill as COUNTED toward its monument.
     *
     * @return the monument's progress after this kill (1..3), or -1
     *         when the elder is unknown or was already counted
     */
    public synchronized int markCounted(UUID elderUuid) {
        return markCounted(elderUuid, System.currentTimeMillis());
    }

    /** Mark counted, stamping the progress window (0.5.6). */
    public synchronized int markCounted(UUID elderUuid, long nowMs) {
        ElderRecord r = elders.get(elderUuid);
        if (r == null || r.status == ElderStatus.COUNTED) {
            return -1;
        }
        r.status = ElderStatus.COUNTED;
        r.respawnDueMs = 0L;
        r.countedAtMs = nowMs;
        return progress(r.monumentId);
    }

    /**
     * The monument's progress window start (0.5.6, design E-1):
     * the EARLIEST countedAtMs among its COUNTED elders — the
     * window opens at the FIRST counted kill and does NOT slide
     * when later elders fall (design T-3's reading of the law:
     * the trio must be finished inside the window). 0 when the
     * monument has no counted elder, or when every counted elder
     * predates timestamps (migrated by the adapter at boot).
     */
    public synchronized long windowStartMs(String monumentId) {
        long start = 0L;
        for (ElderRecord r : elders.values()) {
            if (r.monumentId.equals(monumentId) && r.status == ElderStatus.COUNTED
                    && r.countedAtMs > 0L) {
                start = start == 0L ? r.countedAtMs : Math.min(start, r.countedAtMs);
            }
        }
        return start;
    }

    /**
     * Is this monument's partial progress expired at nowMs
     * (0.5.6, design E-2)? Only a PARTIAL trio (1–2 counted)
     * can expire; a complete trio never does (generation
     * turnover owns it), and expiryMs <= 0 disables expiry
     * (the config escape hatch: 0 = never, today's pre-0.5.6
     * behavior). A window that has not opened (start 0 —
     * pre-timestamp records awaiting migration) never reads
     * as expired.
     */
    public synchronized boolean progressExpired(String monumentId, long nowMs, long expiryMs) {
        if (expiryMs <= 0L) {
            return false;
        }
        int n = progress(monumentId);
        if (n <= 0 || n >= TRIO_SIZE) {
            return false;
        }
        long start = windowStartMs(monumentId);
        return start > 0L && nowMs - start >= expiryMs;
    }

    /** Remaining window for a partial trio, or -1 when no
     *  window is running / expiry is disabled (0.5.6, E-5). */
    public synchronized long windowRemainingMs(String monumentId, long nowMs, long expiryMs) {
        if (expiryMs <= 0L) {
            return -1L;
        }
        long start = windowStartMs(monumentId);
        if (start <= 0L || progress(monumentId) <= 0 || progress(monumentId) >= TRIO_SIZE) {
            return -1L;
        }
        return Math.max(0L, expiryMs - (nowMs - start));
    }

    /**
     * Expire one monument's PARTIAL progress (0.5.6, design
     * E-2): its COUNTED elders flip to PENDING_RESPAWN due at
     * dueMs — the existing respawn maintenance then respawns
     * them at home, which is the law's "the killed guardians
     * respawn" — and progress returns to 0/3. The guard lives
     * HERE, mirroring completeTrioRespawn: a complete trio
     * refuses (returns 0), so no caller can expire a seal the
     * verdict already consumed.
     *
     * @return how many elders flipped (1–2), or 0 when the
     *         monument holds no partial counted progress.
     */
    public synchronized int expirePartialTrio(String monumentId, long dueMs) {
        int n = progress(monumentId);
        if (n <= 0 || n >= TRIO_SIZE) {
            return 0;
        }
        int flipped = 0;
        for (ElderRecord r : elders.values()) {
            if (r.monumentId.equals(monumentId) && r.status == ElderStatus.COUNTED) {
                r.status = ElderStatus.PENDING_RESPAWN;
                r.respawnDueMs = dueMs;
                r.countedAtMs = 0L;
                flipped++;
            }
        }
        return flipped;
    }

    /**
     * Migration (0.5.6, design section 3.5): counted elders
     * restored without a timestamp (pre-0.5.6 state) receive
     * nowMs as their window start — a fresh full window from
     * first boot, never an instant expiry.
     *
     * @return how many records were stamped.
     */
    public synchronized int migrateMissingCountedAt(long nowMs) {
        int stamped = 0;
        for (ElderRecord r : elders.values()) {
            if (r.status == ElderStatus.COUNTED && r.countedAtMs <= 0L) {
                r.countedAtMs = nowMs;
                stamped++;
            }
        }
        return stamped;
    }

    /**
     * Mark an elder's kill as NOT counting: schedule its respawn.
     *
     * @return the record (with respawnDueMs set), or null if unknown
     *         or already counted
     */
    public synchronized ElderRecord markPendingRespawn(UUID elderUuid, long dueMs) {
        ElderRecord r = elders.get(elderUuid);
        if (r == null || r.status == ElderStatus.COUNTED) {
            return null;
        }
        r.status = ElderStatus.PENDING_RESPAWN;
        r.respawnDueMs = dueMs;
        return r;
    }

    /**
     * Generation turnover (0.3.0, SPEC section 2.3): when a trio
     * completes, flip the monument's COUNTED elders to
     * PENDING_RESPAWN due at {@code dueMs} - the adapter's
     * maintenance loop respawns each at its home (through the
     * existing completeRespawn path) and the next trio is a fresh
     * generation on the tracker's own books. This is deliberately
     * NOT markPendingRespawn's shape: that method refuses COUNTED
     * elders because a DENIED elder must never un-count a kill the
     * seal already owns; a COMPLETED trio is the opposite case -
     * the count is spent, by a verdict, and the generation turns
     * over. Returns how many elders flipped (TRIO_SIZE for a
     * complete trio; 0 for anything less - the guard lives HERE,
     * not in caller discipline, so no caller can half-flip a
     * monument whose trio is still assembling).
     */
    public synchronized int completeTrioRespawn(String monumentId, long dueMs) {
        if (!trioComplete(monumentId)) {
            return 0;
        }
        int flipped = 0;
        for (ElderRecord r : elders.values()) {
            if (r.monumentId.equals(monumentId) && r.status == ElderStatus.COUNTED) {
                r.status = ElderStatus.PENDING_RESPAWN;
                r.respawnDueMs = dueMs;
                r.countedAtMs = 0L;
                flipped++;
            }
        }
        return flipped;
    }

    /**
     * The still-TRACKED siblings of one elder (Packet B, E-1 -
     * the trio relay's candidate set): every OTHER elder of the
     * same monument whose status is TRACKED - not counted, not
     * pending respawn. These are the elders who can still be
     * fought, and so the ones the monument answers through.
     * Empty when the elder is unknown.
     */
    public synchronized List<ElderRecord> trackedSiblings(UUID elderUuid) {
        ElderRecord self = elders.get(elderUuid);
        if (self == null) {
            return List.of();
        }
        List<ElderRecord> out = new ArrayList<>();
        for (ElderRecord r : elders.values()) {
            if (r != self && r.monumentId.equals(self.monumentId)
                    && r.status == ElderStatus.TRACKED) {
                out.add(r);
            }
        }
        return out;
    }

    /** Monuments whose trio currently stands complete (all COUNTED). */
    public synchronized java.util.Set<String> monumentsWithCountedTrio() {
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        for (String id : anchors.keySet()) {
            if (trioComplete(id)) {
                out.add(id);
            }
        }
        return out;
    }

    /** Elders whose respawn is due at or before nowMs. */
    public synchronized List<ElderRecord> dueRespawns(long nowMs) {
        List<ElderRecord> out = new ArrayList<>();
        for (ElderRecord r : elders.values()) {
            if (r.status == ElderStatus.PENDING_RESPAWN && r.respawnDueMs <= nowMs) {
                out.add(r);
            }
        }
        return out;
    }

    /**
     * A pending elder has been respawned by the plugin: the slot
     * re-opens under the new entity's UUID, same monument, same home.
     *
     * @return the new record, or null when the old one was not pending
     */
    public synchronized ElderRecord completeRespawn(UUID oldUuid, UUID newUuid) {
        ElderRecord r = elders.get(oldUuid);
        if (r == null || r.status != ElderStatus.PENDING_RESPAWN) {
            return null;
        }
        elders.remove(oldUuid);
        ElderRecord fresh = new ElderRecord(newUuid, r.monumentId, r.world, r.x, r.y, r.z);
        elders.put(newUuid, fresh);
        return fresh;
    }

    /** Every known monument id (first-seen order). */
    public synchronized List<String> monumentIds() {
        return new ArrayList<>(anchors.keySet());
    }

    // -----------------------------------------------------------
    // Persistence (plain maps; the adapter owns the YAML)
    // -----------------------------------------------------------

    public synchronized Map<String, Object> snapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("monumentSeq", monumentSeq);
        List<Object> list = new ArrayList<>();
        for (ElderRecord r : elders.values()) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("uuid", r.uuid.toString());
            e.put("monument", r.monumentId);
            e.put("world", r.world);
            e.put("x", r.x);
            e.put("y", r.y);
            e.put("z", r.z);
            e.put("status", r.status.name());
            e.put("respawnDueMs", r.respawnDueMs);
            e.put("countedAtMs", r.countedAtMs);
            list.add(e);
        }
        m.put("elders", list);
        return m;
    }

    @SuppressWarnings("unchecked")
    public synchronized void restore(Map<String, Object> snapshot) {
        elders.clear();
        anchors.clear();
        anchorWorlds.clear();
        monumentSeq = 0;
        if (snapshot == null) {
            return;
        }
        if (snapshot.get("monumentSeq") instanceof Number n) {
            monumentSeq = n.intValue();
        }
        Object raw = snapshot.get("elders");
        if (!(raw instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> e)) {
                continue;
            }
            try {
                UUID uuid = UUID.fromString(String.valueOf(e.get("uuid")));
                String monumentId = String.valueOf(e.get("monument"));
                String world = String.valueOf(e.get("world"));
                double x = num(e.get("x"));
                double y = num(e.get("y"));
                double z = num(e.get("z"));
                ElderRecord r = new ElderRecord(uuid, monumentId, world, x, y, z);
                r.status = ElderStatus.valueOf(String.valueOf(e.get("status")));
                r.respawnDueMs = (long) num(e.get("respawnDueMs"));
                r.countedAtMs = (long) num(e.get("countedAtMs"));
                elders.put(uuid, r);
                anchors.putIfAbsent(monumentId, new double[]{x, z});
                anchorWorlds.putIfAbsent(monumentId, world);
            } catch (Exception ignored) {
                // A bad record is skipped; the trio is re-derivable
                // from the remaining elders and future encounters.
            }
        }
    }

    private static double num(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0.0;
    }
}

