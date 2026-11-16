package com.winkreyesvalley.excelmagiclevel.adapter.together;

import java.util.Set;
import java.util.UUID;

/** Party membership lookup. Implementations return the given player's
 *  party INCLUDING the player themselves, or a set containing only the
 *  player when they are solo / membership cannot be determined. */
public interface PartyLookup {
    Set<UUID> partyMembers(UUID player);
}
