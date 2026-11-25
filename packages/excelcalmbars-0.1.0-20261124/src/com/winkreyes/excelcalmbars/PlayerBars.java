package com.winkreyes.excelcalmbars;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * All boss-bar gate state for one player. Instances of this class
 * double as the lock object for their contents: every read/write of
 * the bar map and the combat timestamp happens inside
 * {@code synchronized (bars)} blocks, because the Netty gate threads
 * and the server main thread both touch it.
 */
public final class PlayerBars {

    public final UUID playerId;
    /** Boss packets the gate has seen (interception proof). */
    public long seenPackets;
    /** Last combat event (damage dealt or taken) in epoch millis. */
    public long lastCombatMillis;
    public final Map<UUID, BarState> bars = new HashMap<>();

    public PlayerBars(UUID playerId) {
        this.playerId = playerId;
    }
}
