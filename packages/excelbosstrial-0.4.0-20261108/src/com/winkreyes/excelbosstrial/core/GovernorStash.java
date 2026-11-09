/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * The governor's governed output, handed to the record handler
 * (ExcelBossTrial 0.4.0, the Wither-night fix train, PM-9).
 *
 * The defect this closes: the record handler (MONITOR)
 * historically re-derived the ledger damage by dividing the
 * event's FINAL damage by the intake factor. Anything another
 * plugin did to the event between the governor (HIGH) and the
 * record (MONITOR) - a damage amplifier, a resistance shim -
 * was divided by f too, so on an over-cap pool (f ~ 0.047 for
 * the Wither's 21,900) a post-governor change was amplified
 * into the ledger by 1/f (the post-mortem's x21 risk note).
 *
 * The governor now stashes its own governed result, in LEDGER
 * space, keyed by the event object itself (the governor and
 * the record handler receive the same instance; a WeakHashMap
 * entry dies with the event, so no tick bookkeeping and no
 * cross-event collision - two explosions in one tick are two
 * objects). The record handler consumes the stash when present
 * and falls back to the historical re-derivation only when the
 * governor demonstrably did not run (chapter caps off and the
 * pool under the cap: the governor is the identity there, and
 * so is the fallback).
 *
 * Static by necessity (the dragon chapter's governor is a
 * separate class from the host's handlers); synchronized on
 * the map, and Bukkit events are main-thread here regardless.
 */
public final class GovernorStash {

    private static final Map<EntityDamageEvent, Double> STASH =
            Collections.synchronizedMap(new WeakHashMap<>());

    private GovernorStash() {
    }

    /** Stash one event's governed result, in ledger space. */
    public static void put(EntityDamageEvent event, double ledgerSpaceDamage) {
        if (event == null || Double.isNaN(ledgerSpaceDamage)) {
            return;
        }
        STASH.put(event, Math.max(0.0, ledgerSpaceDamage));
    }

    /**
     * Consume one event's stash: the governed ledger-space
     * damage, or null when the governor did not run for this
     * event (the caller then falls back to re-derivation).
     * Consuming removes the entry - one event is recorded once.
     */
    public static Double consume(EntityDamageEvent event) {
        if (event == null) {
            return null;
        }
        return STASH.remove(event);
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
