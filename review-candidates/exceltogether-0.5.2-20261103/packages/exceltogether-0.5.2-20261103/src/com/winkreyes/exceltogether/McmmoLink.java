package com.winkreyes.exceltogether;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Optional mirror of an ExcelTogether party into the mcMMO party system. ExcelTogether is always the
 * source of truth: every call here may fail or do nothing, and the ExcelTogether party carries on regardless.
 * mcMMO is a soft dependency: with mcMMO absent, disabled or too different, NONE is used.
 */
public interface McmmoLink {
    enum Mirror {
        OK,                     // mcMMO invite set, or player joined the mcMMO party
        ABSENT,                 // mcMMO missing, party system off, or link switched off
        TARGET_IN_OTHER_PARTY,  // the other player is already in a different mcMMO party
        FULL,                   // mcMMO party size limit reached
        LOCKED,                 // mcMMO party is locked and the inviter is not its leader
        NOT_READY,              // a player or their mcMMO profile is not loaded right now
        ERROR                   // anything unexpected; logged, never shown to players
    }

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
    /** A hand-made mcMMO join was mirrored into ExcelTogether: remember the player as linked so a later ExcelTogether leave also takes them out of mcMMO. */
    default void adopted(UUID id, String mcPartyName) { }
    /** Called after ExcelTogether accepted a new invite. Creates the inviter's mcMMO party if needed and sets the invite. */
    Mirror invite(UUID inviter, UUID target, String partyNameHint);
    /** Called after the invitee accepted in ExcelTogether. */
    Mirror accept(UUID target, UUID inviter, String partyNameHint);
    /** Invite declined or expired: clear the mcMMO invite we set. */
    void cancel(UUID target, UUID inviter);
    /** Player left, was kicked, or the party ended: take them out of the mcMMO party, only if we put them there. */
    void leave(UUID id);
    /** Housekeeping: drop mcMMO parties we created that never got a second member. */
    void cleanup(Predicate<UUID> inviterHasOpenInvites);

    McmmoLink NONE = new McmmoLink() {
        public boolean available() { return false; }
        public Mirror invite(UUID a, UUID b, String h) { return Mirror.ABSENT; }
        public Mirror accept(UUID a, UUID b, String h) { return Mirror.ABSENT; }
        public void cancel(UUID a, UUID b) {}
        public void leave(UUID a) {}
        public void cleanup(Predicate<UUID> p) {}
    };
}
