package com.winkreyes.exceltogether;

import java.util.UUID;

/**
 * One-way mirror of the roster of record into the mcMMO party system (0.6.0,
 * P-PARTY-REBUILD-1 unit 2 / U2-D1). ExcelTogether is the only source of
 * truth: mcMMO is never read as a roster source and never carries invites -
 * invites live hub-side only, so the pre-0.6.0 invite/accept/cancel surface
 * is gone from this contract (that also retires the dangling-mcMMO-invite
 * half of audit D5). What remains is reads (for drift detection) plus the
 * reconcile primitives {@link McmmoSync} drives: create the deterministic
 * mirror party, add a roster member to it, remove a non-roster member from
 * it, and set its leader to the hub leader. Every call may fail or do
 * nothing; the roster of record carries on regardless. mcMMO is a soft
 * dependency: with mcMMO absent, disabled or too different, NONE is used.
 */
public interface McmmoLink {

    /** A read-only picture of one mcMMO party. UNKNOWN = could not tell right now (profile not loaded, error): never act on it. NONE = definitely in no party. */
    final class McParty {
        public final String name; public final UUID leader; public final java.util.LinkedHashMap<UUID, String> members;
        public McParty(String name, UUID leader, java.util.Map<UUID, String> members) {
            this.name = name; this.leader = leader; this.members = new java.util.LinkedHashMap<>(members);
        }
        public static final McParty NONE = new McParty(null, null, java.util.Map.of());
        public static final McParty UNKNOWN = new McParty(null, null, java.util.Map.of());
        public boolean real() { return name != null; }
    }

    boolean available();
    /** The mcMMO party this player is in right now (works for offline players too). NONE or UNKNOWN when there is none or it cannot be read. */
    default McParty partyOf(UUID id) { return McParty.UNKNOWN; }
    /** An mcMMO party by name (members as they are right now), or NONE / UNKNOWN. */
    default McParty partyByName(String name) { return McParty.UNKNOWN; }

    // ---------- one-way reconcile primitives (roster -> mcMMO only) ----------
    /**
     * Create the mirror party for a hub party, led by {@code leader}. The name
     * is deterministic (D74/Q5): the cleaned {@code hint} (the leader's name)
     * when free, else the cleaned {@code fallbackHint} (the hub-id name) when
     * free, else a numbered variant of the fallback. Returns the actual party
     * name, or null when it cannot be done right now (offline, profile not
     * loaded, mcMMO error) - the caller retries on a later poll.
     */
    default String createMirrorParty(UUID leader, String hint, String fallbackHint) { return null; }
    /** Put the player into the named mcMMO party (moving them out of any other mcMMO party first). False when it cannot be done right now. */
    default boolean addToParty(UUID id, String partyName) { return false; }
    /** Take the player out of the named mcMMO party - only if they are actually in that party. Never touches any other party. */
    default void removeFromParty(UUID id, String partyName) { }
    /** Make {@code leader} the leader of the named mcMMO party. False when it cannot be done right now. */
    default boolean setPartyLeader(String partyName, UUID leader, String leaderName) { return false; }

    McmmoLink NONE = new McmmoLink() {
        public boolean available() { return false; }
    };
}
