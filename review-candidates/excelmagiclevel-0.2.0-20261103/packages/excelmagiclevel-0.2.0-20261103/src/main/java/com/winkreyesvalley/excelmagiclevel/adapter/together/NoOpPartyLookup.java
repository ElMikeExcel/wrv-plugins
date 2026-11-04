package com.winkreyesvalley.excelmagiclevel.adapter.together;

import java.util.Set;
import java.util.UUID;

/**
 * ExcelMagicLevel — Phase 1 party seam: nobody is in a party. Own XP is
 * unaffected; share simply has no one to copy to until Phase 2 wires the
 * real ExcelTogether adapter.
 */
public final class NoOpPartyLookup implements PartyLookup {
    @Override
    public Set<UUID> partyMembers(UUID player) {
        return Set.of(player);
    }
}
