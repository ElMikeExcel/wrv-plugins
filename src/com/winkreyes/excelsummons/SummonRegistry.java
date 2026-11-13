package com.winkreyes.excelsummons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The summon -> owner registry. One map, four jobs (attribution,
 * allegiance, explosion safety, lifetime) all read from here.
 *
 * Persistence is the Wolf lesson made law: wolves outlive the session
 * that summoned them, so this registry is written to disk on every
 * change by the plugin shell and reloaded at startup. Entries whose
 * entities no longer exist are swept — the sweep removes ENTRIES,
 * never entities (a pet is never deleted by bookkeeping).
 *
 * Pure Java (no Bukkit) so the whole surface is unit-tested off-server.
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class SummonRegistry {

    private final Map<UUID, SummonRecord> byEntity = new LinkedHashMap<>();

    public synchronized void register(SummonRecord rec) {
        byEntity.put(rec.entityId(), rec);
    }

    public synchronized SummonRecord unregister(UUID entityId) {
        return byEntity.remove(entityId);
    }

    public synchronized SummonRecord get(UUID entityId) {
        return entityId == null ? null : byEntity.get(entityId);
    }

    public synchronized boolean isRegistered(UUID entityId) {
        return entityId != null && byEntity.containsKey(entityId);
    }

    /** The attribution question: who does this entity fight for? */
    public synchronized UUID summonerOf(UUID entityId) {
        SummonRecord r = get(entityId);
        return r == null ? null : r.ownerId();
    }

    public synchronized int size() {
        return byEntity.size();
    }

    public synchronized Collection<SummonRecord> all() {
        return Collections.unmodifiableList(new ArrayList<>(byEntity.values()));
    }

    public synchronized List<SummonRecord> ofOwner(UUID ownerId) {
        List<SummonRecord> out = new ArrayList<>();
        for (SummonRecord r : byEntity.values()) {
            if (r.ownerId().equals(ownerId)) out.add(r);
        }
        return out;
    }

    /** Count one owner's live summons of one spell family (tiers folded). */
    public synchronized int countSpell(UUID ownerId, String baseSpellKey) {
        int n = 0;
        for (SummonRecord r : byEntity.values()) {
            if (r.ownerId().equals(ownerId)
                    && SummonRecord.baseKey(r.spellKey()).equals(baseSpellKey)) n++;
        }
        return n;
    }

    /** Count one owner's live summons in one branch. */
    public synchronized int countBranch(UUID ownerId, SummonRecord.Branch branch) {
        int n = 0;
        for (SummonRecord r : byEntity.values()) {
            if (r.ownerId().equals(ownerId) && r.branch() == branch) n++;
        }
        return n;
    }

    /** Count one owner's live summons across a set of branches. */
    public synchronized int countBranches(UUID ownerId,
                                          java.util.Set<SummonRecord.Branch> branches) {
        int n = 0;
        for (SummonRecord r : byEntity.values()) {
            if (r.ownerId().equals(ownerId) && branches.contains(r.branch())) n++;
        }
        return n;
    }

    /** The branches Mike's total summon cap counts (0.1.4,
     *  FIX-PACKET-DESIGN §3.1): COMBAT + COMPANION + PACK. The
     *  wolf's branch is PACK, not COMPANION — counting only two
     *  branch names would silently exempt the wolf. MOUNT is never
     *  counted (persistent transport, per-spell cap 1). */
    public static final java.util.Set<SummonRecord.Branch> COUNTED_BRANCHES =
            java.util.Set.of(SummonRecord.Branch.COMBAT, SummonRecord.Branch.COMPANION,
                    SummonRecord.Branch.PACK);

    /** Count one owner's summons toward the unified total cap:
     *  every branch except MOUNT (§3.1). */
    public synchronized int countCounted(UUID ownerId) {
        return countBranches(ownerId, COUNTED_BRANCHES);
    }

    /** Count every live summon in one branch, server-wide. */
    public synchronized int countBranchServerWide(SummonRecord.Branch branch) {
        int n = 0;
        for (SummonRecord r : byEntity.values()) {
            if (r.branch() == branch) n++;
        }
        return n;
    }

    /** Records whose bridge expiry is due at {@code nowMillis}. */
    public synchronized List<SummonRecord> expiryDue(long nowMillis) {
        List<SummonRecord> out = new ArrayList<>();
        for (SummonRecord r : byEntity.values()) {
            if (r.expiryDue(nowMillis)) out.add(r);
        }
        return out;
    }

    /**
     * Drop entries whose entities are gone. {@code liveEntityIds} is the
     * set of entity UUIDs the server currently knows. Returns the dropped
     * records (for logging). Entities themselves are never touched here.
     */
    public synchronized List<SummonRecord> sweepMissing(Set<UUID> liveEntityIds) {
        List<SummonRecord> dropped = new ArrayList<>();
        byEntity.values().removeIf(r -> {
            if (liveEntityIds.contains(r.entityId())) return false;
            dropped.add(r);
            return true;
        });
        return dropped;
    }

    // -- persistence -----------------------------------------------------

    public synchronized List<Map<String, Object>> snapshot() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SummonRecord r : byEntity.values()) out.add(r.toMap());
        return out;
    }

    public synchronized void restore(List<Map<String, Object>> rows) {
        byEntity.clear();
        if (rows == null) return;
        for (Map<String, Object> row : rows) {
            SummonRecord r = SummonRecord.fromMap(row);
            if (r != null) byEntity.put(r.entityId(), r);
        }
    }
}
