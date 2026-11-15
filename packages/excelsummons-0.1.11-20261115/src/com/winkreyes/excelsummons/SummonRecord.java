package com.winkreyes.excelsummons;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One registered summon: which entity, whose it is, what spell made it,
 * which branch of the summoning tree it belongs to, and when (if ever)
 * the bridge must take it away. Pure data — no Bukkit types — so the
 * registry and its codec are unit-testable off-server.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class SummonRecord {

    public enum Branch {
        COMPANION, PACK, COMBAT, MOUNT;

        public static Branch fromConfig(String s) {
            if (s == null) return COMBAT;
            try {
                return Branch.valueOf(s.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return COMBAT;
            }
        }
    }

    /**
     * How this summon came to be registered (0.1.4, FIX-PACKET-DESIGN
     * §2.4): CAST — a Magic cast the bridge captured; ADOPTED — a
     * hand-tamed vanilla pet the sweep merely registered. The
     * distinction is a safety boundary: `dismiss pets` REMOVES the
     * entity, which is correct for a conjured spell pet and wrong
     * for a player's own tamed animal, so only CAST pets are
     * dismiss-pets targets. Legacy rows with no origin key restore
     * as ADOPTED — the safe direction: never dismiss-removable
     * until proven cast-made.
     */
    public enum Origin {
        CAST, ADOPTED;

        public static Origin fromConfig(String s) {
            if (s == null) return ADOPTED;
            try {
                return Origin.valueOf(s.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ADOPTED;
            }
        }
    }

    private final UUID entityId;
    private final UUID ownerId;
    private final String spellKey;      // base key, tiers folded (archer|2 -> archer)
    private final Branch branch;
    private final Long expiresAtMillis; // null = no bridge expiry (pets, mounts)
    private final String homeWorld;
    private final long registeredAtMillis;
    private final Origin origin;

    public SummonRecord(UUID entityId, UUID ownerId, String spellKey, Branch branch,
                        Long expiresAtMillis, String homeWorld, long registeredAtMillis) {
        this(entityId, ownerId, spellKey, branch, expiresAtMillis, homeWorld,
                registeredAtMillis, Origin.ADOPTED);
    }

    public SummonRecord(UUID entityId, UUID ownerId, String spellKey, Branch branch,
                        Long expiresAtMillis, String homeWorld, long registeredAtMillis,
                        Origin origin) {
        this.entityId = entityId;
        this.ownerId = ownerId;
        this.spellKey = spellKey;
        this.branch = branch;
        this.expiresAtMillis = expiresAtMillis;
        this.homeWorld = homeWorld;
        this.registeredAtMillis = registeredAtMillis;
        this.origin = origin == null ? Origin.ADOPTED : origin;
    }

    public UUID entityId() { return entityId; }
    public UUID ownerId() { return ownerId; }
    public String spellKey() { return spellKey; }
    public Branch branch() { return branch; }
    public Long expiresAtMillis() { return expiresAtMillis; }
    public String homeWorld() { return homeWorld; }
    public long registeredAtMillis() { return registeredAtMillis; }
    public Origin origin() { return origin; }

    /** True when the bridge owes this entity a despawn at {@code now}. */
    public boolean expiryDue(long nowMillis) {
        return expiresAtMillis != null && nowMillis >= expiresAtMillis;
    }

    /** Fold a tiered spell key (archer|2) to its family base (archer). */
    public static String baseKey(String spellKey) {
        if (spellKey == null) return "";
        int bar = spellKey.indexOf('|');
        return bar >= 0 ? spellKey.substring(0, bar) : spellKey;
    }

    // -- persistence codec (plain maps, YamlConfiguration-friendly) ------

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("entity", entityId.toString());
        m.put("owner", ownerId.toString());
        m.put("spell", spellKey);
        m.put("branch", branch.name().toLowerCase());
        m.put("expires", expiresAtMillis); // may be null -> key written as null
        m.put("world", homeWorld);
        m.put("registered", registeredAtMillis);
        m.put("origin", origin.name().toLowerCase());
        return m;
    }

    public static SummonRecord fromMap(Map<String, Object> m) {
        if (m == null) return null;
        try {
            UUID entity = UUID.fromString(String.valueOf(m.get("entity")));
            UUID owner = UUID.fromString(String.valueOf(m.get("owner")));
            String spell = String.valueOf(m.get("spell"));
            Branch branch = Branch.fromConfig(String.valueOf(m.get("branch")));
            Object exp = m.get("expires");
            Long expires = (exp instanceof Number) ? ((Number) exp).longValue() : null;
            Object world = m.get("world");
            Object reg = m.get("registered");
            long registered = (reg instanceof Number) ? ((Number) reg).longValue() : 0L;
            Object org = m.get("origin");
            // A legacy row with no origin key restores as ADOPTED
            // (Origin.fromConfig(null) == ADOPTED) — never
            // dismiss-removable until proven cast-made (§2.4).
            Origin origin = Origin.fromConfig(org == null ? null : String.valueOf(org));
            return new SummonRecord(entity, owner, spell, branch, expires,
                    world == null ? "" : String.valueOf(world), registered, origin);
        } catch (RuntimeException e) {
            return null; // a corrupt row is dropped, never fatal (registry sweep discipline)
        }
    }
}
