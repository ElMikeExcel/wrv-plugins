package com.winkreyes.exceltogether;

import com.winkreyesvalley.excelmagiclevel.adapter.together.PartyLookup;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * ExcelTogether - publishes party membership to any plugin holding the
 * PartyLookup contract (ExcelMagicLevel's share is the first consumer).
 * Read-only: this class never mutates party state. Excel - Wink Reyes Valley.
 */
public final class MagicLevelPartyLookup implements PartyLookup {
    private final PartyManager parties;

    public MagicLevelPartyLookup(PartyManager parties) {
        this.parties = parties;
    }

    @Override
    public Set<UUID> partyMembers(UUID player) {
        PartyManager.Party party = parties.partyOf(player);
        if (party == null) {
            return Set.of(player);
        }
        // members may include offline-grace members; consumers pay only
        // online, in-range, same-world members, so return membership as-is.
        return new HashSet<>(party.members);
    }
}
