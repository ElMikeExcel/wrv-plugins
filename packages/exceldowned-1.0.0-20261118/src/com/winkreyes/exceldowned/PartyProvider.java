package com.winkreyes.exceldowned;

import java.util.Set;
import java.util.UUID;

/**
 * Party-of-record lookups for Resurrection targeting. Implementations are
 * fail-soft: any doubt answers UNKNOWN, and UNKNOWN never counts as party
 * for a revive (ExcelSummons 0.1.1's protection rule, carried over).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public interface PartyProvider {

    enum Membership { PARTY, NOT_PARTY, UNKNOWN }

    /** Other members of the player's party (never includes the player). */
    Set<UUID> partyMembersOf(UUID playerId);

    /** Per-pair verdict; PARTY only when positively determinable. */
    Membership membershipOf(UUID ownerId, UUID playerId);
}
