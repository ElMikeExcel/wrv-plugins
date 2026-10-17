package com.winkreyes.exceltogether;

import org.bukkit.OfflinePlayer;

/** Answers: is another player close enough to the earner to form a party? */
public interface PartyFinder {
    boolean hasPartner(OfflinePlayer earner);
}
