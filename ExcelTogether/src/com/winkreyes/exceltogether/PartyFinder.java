package com.winkreyes.exceltogether;

import org.bukkit.OfflinePlayer;

import java.util.List;

/** Answers: is another player close enough to the earner to form a party? */
public interface PartyFinder {
    boolean hasPartner(OfflinePlayer earner);
    /** Name to show in the "+25% party bonus (with NAME)" indicator. Null when the bonus is not active. */
    default String partnerName(OfflinePlayer earner) { return hasPartner(earner) ? "your party" : null; }
    /** Party members who also receive a copy of the earner's gain (online, same world, in range, active). */
    default List<OfflinePlayer> sharers(OfflinePlayer earner) { return List.of(); }
}
