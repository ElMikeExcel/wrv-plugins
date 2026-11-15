package com.winkreyes.excelsummons;

import java.util.Set;
import java.util.UUID;

/**
 * Party membership as the bridge needs it: who is in this player's
 * party? ExcelTogether parties are a social/XP grouping with no
 * combat semantics of their own (DESIGN §3), so this is a read-only
 * seam in house style — a replaceable adapter, never a hard weld.
 *
 * Two questions ride on it. Target-sharing (partyMembersOf) fails
 * soft to "no party": if Together is absent or its shape shifts,
 * summons simply keep to their caster's own fights. Party
 * PROTECTION (membershipOf, 0.1.1) fails soft the other way — to
 * UNKNOWN, which TargetRules treats as protected: a summon must
 * never attack someone who might be in the party, so an unreadable
 * party state protects everyone it covers rather than exposing them.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public interface PartyProvider {

    /** UUIDs of the player's party members (excluding the player). */
    Set<UUID> partyMembersOf(UUID playerId);

    /** A per-event party verdict for one player against one owner:
     *  PARTY = known member of the owner's party; NOT_PARTY = known
     *  to be outside it; UNKNOWN = membership could not be determined
     *  (adapter absent, error, or an unreadable party shape). */
    enum Membership { PARTY, NOT_PARTY, UNKNOWN }

    /**
     * Is {@code playerId} in {@code ownerId}'s party? The owner
     * themself counts as PARTY. Default: UNKNOWN — a provider that
     * cannot answer protects by default (see the class note).
     */
    default Membership membershipOf(UUID ownerId, UUID playerId) {
        return Membership.UNKNOWN;
    }

    /** Shared instance that knows no parties (Together absent).
     *  Target-sharing gets "no party"; protection gets UNKNOWN. */
    PartyProvider NONE = playerId -> java.util.Collections.emptySet();
}
