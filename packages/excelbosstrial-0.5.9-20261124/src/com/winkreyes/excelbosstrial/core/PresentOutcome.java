/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The auto-present outcome classifier (ExcelBossTrial 0.3.0, SPEC
 * section 5): at the Oct 31 elder verdict the rewards menu
 * auto-presented for three champions and silently not for the
 * fourth, and TeamClaims.attemptPresent's eight bare return paths
 * said nothing about which one fired. This classifier names every
 * outcome and owns the priority order, so the adapter's branches
 * only gather facts and the decision table is unit-testable:
 *
 *   1. player offline or gone          -> PLAYER_GONE
 *   2. not survival/adventure          -> GAMEMODE
 *   3. a foreign container is open     -> FOREIGN_INVENTORY
 *   4. Magic holds the wand inventory  -> MAGIC_INVENTORY_STATE
 *   5. batch already presented         -> ALREADY_PRESENTED
 *   6. nothing unconsumed to show      -> NOTHING_TO_PRESENT
 *   7. otherwise                       -> PRESENTED (the open is attempted)
 *
 * PRESENTED is the classifier's word for "go ahead and open": the
 * adapter still checks the UI's answer (an open the UI refuses
 * after all is logged under the adapter's OPEN_REFUSED line, the
 * one outcome only the UI can know).
 *
 * Transient vs terminal drives the retry queue (section 5.2): a
 * transient outcome (gone, gamemode, a container in the way, a
 * Magic inventory state) is retried on the config cadence until
 * the batch presents or the retry window closes; NOTHING_TO_PRESENT
 * and ALREADY_PRESENTED are terminal - there is nothing to retry.
 */
public enum PresentOutcome {
    PRESENTED,
    PLAYER_GONE,
    GAMEMODE,
    FOREIGN_INVENTORY,
    MAGIC_INVENTORY_STATE,
    NOTHING_TO_PRESENT,
    ALREADY_PRESENTED;

    /** The six facts the adapter gathers, in the priority order's terms. */
    public record Facts(boolean playerOnline, boolean survivalOrAdventure,
                        boolean foreignContainerOpen, boolean magicInventoryBlocked,
                        boolean hasUnconsumed, boolean alreadyPresented) {
    }

    /** Classify one auto-present attempt from its facts. */
    public static PresentOutcome classify(Facts facts) {
        if (facts == null || !facts.playerOnline()) {
            return PLAYER_GONE;
        }
        if (!facts.survivalOrAdventure()) {
            return GAMEMODE;
        }
        if (facts.foreignContainerOpen()) {
            return FOREIGN_INVENTORY;
        }
        if (facts.magicInventoryBlocked()) {
            return MAGIC_INVENTORY_STATE;
        }
        if (facts.alreadyPresented()) {
            return ALREADY_PRESENTED;
        }
        if (!facts.hasUnconsumed()) {
            return NOTHING_TO_PRESENT;
        }
        return PRESENTED;
    }

    /** True when a later retry could still present this batch. */
    public boolean transientFailure() {
        return switch (this) {
            case PLAYER_GONE, GAMEMODE, FOREIGN_INVENTORY, MAGIC_INVENTORY_STATE -> true;
            case PRESENTED, NOTHING_TO_PRESENT, ALREADY_PRESENTED -> false;
        };
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
