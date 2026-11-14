package com.winkreyes.exceleitemobsabilitybedrockbridge;

import org.bukkit.entity.Player;

/**
 * The seam between the bridge and EliteMobs' Advanced Combat System.
 * All EliteMobs types live behind this interface (see EmAcsGateway);
 * everything on this side speaks the bridge's own types, which is what
 * lets the composition/mapping logic be unit-tested on a plain JVM.
 *
 * Implementations must never throw: any EliteMobs-side problem (module
 * gone, execution blowing up) is reported through the return values.
 */
public interface AcsGateway {

    /** True when the EliteMobs class module is present and initialized. */
    boolean moduleReady();

    /** Live class state for the player. Never null; noClass() when none. */
    ClassView classView(Player player);

    /**
     * Fires one ability through EliteMobs' own entry point. EliteMobs
     * enforces class, controls, cost, targeting and threat; the bridge
     * adds nothing. Never throws.
     */
    FireOutcome fire(Player player, BridgeSlot slot);
}
