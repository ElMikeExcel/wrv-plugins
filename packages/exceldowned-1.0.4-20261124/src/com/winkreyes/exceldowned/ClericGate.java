package com.winkreyes.exceldowned;

import org.bukkit.entity.Player;

/**
 * The seam between ExcelDowned and EliteMobs' Advanced Combat System
 * for the Cleric "Raise Ally" companion ability (design B — board-r506/
 * revive/B-CLERIC-REVIVE-DESIGN.md; GRACE-spend spike board-r507/W5;
 * decisions D106/D107/D108).
 *
 * All EliteMobs types live behind this interface (see EmClericGate —
 * the ONLY class in this plugin that names an EliteMobs type, the
 * bridge's two-class discipline applied as one seam class + probe);
 * everything on this side speaks this plugin's own types, which is
 * what lets the gate logic (ClericRevivePolicy) be unit-tested on a
 * plain JVM.
 *
 * Implementations must never throw: any EliteMobs-side problem (EM
 * absent, ACS not yet initialized, an internal moved by an EM update)
 * is reported through the return values, and the ability stands down
 * cleanly — ExcelDowned itself is never affected.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public interface ClericGate {

    /** Fail-soft probe states (house discipline, AcsProbe-shaped). */
    enum Probe {
        /** Module present and initialized — reads and spends may run. */
        READY,
        /** Module present, but ACS has not initialized yet (normal at
         *  enable: ACS initializes in EM's post-init phase). */
        NOT_INITIALIZED,
        /** Module class could not be reached at all (EM absent/moved),
         *  or the spend controller could not be obtained. */
        UNAVAILABLE
    }

    /**
     * A caster's EliteMobs class state as read live from the module,
     * in this plugin's own types. Nothing here is stored or mirrored;
     * a fresh view is built on every cast attempt (the gate re-reads
     * live state at attempt time, never a rendered form's snapshot).
     *
     * @param hasActiveClass whether EM reports an active class
     * @param rootId         lineage root form id (e.g. "cleric"), or null
     * @param activeFormId   active form id (e.g. "priest"), or null
     * @param resourceType   active resource type name (e.g. "GRACE"), or null
     * @param resourceAmount current resource amount, or null when unreported
     * @param resourceMax    resource pool maximum, or null when unreported
     * @param emLevel        active lineage effective level
     *                       (AdvancedCombatModule.ClassIdentity.level),
     *                       or -1 when unreported
     */
    record ClericView(boolean hasActiveClass, String rootId, String activeFormId,
                      String resourceType, Double resourceAmount, Double resourceMax,
                      int emLevel) {

        public static ClericView noClass() {
            return new ClericView(false, null, null, null, null, null, -1);
        }
    }

    /** Probe the module. Never throws. */
    Probe probe();

    /** Live class state for the player. Never null; noClass() when
     *  none (or when the module is not READY). Never throws. */
    ClericView view(Player player);

    /**
     * Spend resource through EM's own charge path
     * (ClassResourceController.trySpend — D107: the identical invoke
     * EliteMobs itself makes for a stock cast, reached via one
     * reflective field read on the public module singleton, behind
     * this probe discipline). Returns true only when the spend
     * actually deducted. Never throws: any failure (probe not READY,
     * controller unobtainable, insufficient balance, session closed)
     * returns false and charges nothing further.
     */
    boolean trySpend(Player player, double amount);
}
