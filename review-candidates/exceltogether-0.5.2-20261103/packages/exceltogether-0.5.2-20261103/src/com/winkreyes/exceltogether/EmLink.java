package com.winkreyes.exceltogether;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Optional mesh leg into the EliteMobs party system (0.5.0, the party mesh). EliteMobs is the FACE of
 * the one party (its sidebar, menus, invite popups); ExcelTogether's PartyManager stays the hub and
 * the mcMMO party stays the roster of record. Every call here may fail or do nothing: with EliteMobs
 * absent, its parties disabled, or its API shifted, NONE is used and the mesh degrades quietly to the
 * pre-0.5.0 two-way behaviour. Shaped exactly like {@link McmmoLink}; EM is touched only through
 * {@code com.magmaguy.elitemobs.parties.PartyManager}'s public statics, and only inside the adapter.
 */
public interface EmLink {
    enum EmMirror {
        OK,                      // done (including the idempotent end-states of DESIGN §1.4)
        ABSENT,                  // EliteMobs missing, parties off, link switched off, or API latched off
        TARGET_IN_OTHER_PARTY,   // the other player is in a different EliteMobs party
        FULL,                    // EliteMobs party is at its hard cap of 5
        DISABLED,                // EliteMobs parties are disabled in its Party.yml
        NO_PERMISSION,           // the acting player lacks elitemobs.party
        TARGET_NO_PERMISSION,    // the other player lacks elitemobs.party
        NOT_READY,               // transient: offline, no pending invite, invite expired - retry later
        DEFERRED,                // held by the dungeon freeze; the reconciler retries it as an op
        ERROR                    // anything unexpected; logged, never shown to players
    }

    /** A read-only picture of one EliteMobs party. UNKNOWN = could not tell right now: never act on it. NONE = definitely in no party. */
    final class EmParty {
        public final UUID leader;                 // null on NONE/UNKNOWN
        public final List<UUID> members;          // display order (leader first), empty on NONE/UNKNOWN
        public final Object partyKey;             // opaque: EM Party.getId(); equality is all the mesh uses
        public EmParty(UUID leader, List<UUID> members, Object partyKey) {
            this.leader = leader;
            this.members = members == null ? List.of() : List.copyOf(members);
            this.partyKey = partyKey;
        }
        public boolean real() { return partyKey != null; }
        public static final EmParty NONE = new EmParty(null, List.of(), null);
        public static final EmParty UNKNOWN = new EmParty(null, List.of(), null);
    }

    boolean available();
    /** One player's EliteMobs party right now. NONE = definitely none. UNKNOWN = could not read; never act on it. */
    default EmParty partyOf(UUID id) { return EmParty.UNKNOWN; }
    /** Every EliteMobs party right now, for the poll's union set. Empty when unreadable (see the adapter's last-read flag). */
    default List<EmParty> allParties() { return List.of(); }
    /** Drive calls. Each returns the mapped result (DESIGN §1.4). All may fail soft. */
    default EmMirror createParty(UUID leader) { return EmMirror.ABSENT; }
    default EmMirror invite(UUID inviter, UUID target) { return EmMirror.ABSENT; }
    default EmMirror accept(UUID target) { return EmMirror.ABSENT; }
    default EmMirror leave(UUID id) { return EmMirror.ABSENT; }
    /** True while any listed player is inside an instanced dungeon/arena (DESIGN §7, freeze F1). */
    default boolean dungeonBusy(Collection<UUID> members) { return false; }
    /** True while a live adapter-driven invite from inviter to target exists (DESIGN §5 ledger). */
    default boolean inviteDriven(UUID inviter, UUID target) { return false; }

    EmLink NONE = new EmLink() {
        public boolean available() { return false; }
    };
}
