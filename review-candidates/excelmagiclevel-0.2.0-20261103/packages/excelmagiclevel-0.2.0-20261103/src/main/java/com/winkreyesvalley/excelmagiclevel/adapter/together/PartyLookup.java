package com.winkreyesvalley.excelmagiclevel.adapter.together;

import java.util.Set;
import java.util.UUID;

/**
 * ExcelMagicLevel — party membership, read through a replaceable adapter
 * (house build style). The source of truth is ExcelTogether's own party
 * data (SPEC §9.4: even an mcMMO party-sync break cannot reach us, because
 * we never read mcMMO's mirror). Phase 2 fills this seam in, after the
 * SPEC §0.3 prerequisite check on ExcelTogether 0.4.3's published surface.
 * Fail-soft law: a lookup failure degrades share to off, silently — it
 * never errors into the cast path.
 */
public interface PartyLookup {
    /** All party members of this player (including the player), or just the player if none/unknown. */
    Set<UUID> partyMembers(UUID player);
}
