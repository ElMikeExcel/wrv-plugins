package com.winkreyes.excelbosstrial.dragon;

/**
 * P3 of the 0.1.1 anti-nuke release (DRAGON-CHEESE-RESEARCH.md
 * section 2): no end-crystal PLACEMENT while a trial battle is
 * active. Pure predicate, no Bukkit: the adapter supplies the rule
 * switch and the battle's phase; the interact event itself (a player
 * right-clicking with an end crystal in the End) is the adapter's
 * business.
 *
 * "Battle active" is exactly PHASE1 / TRANSITION / PHASE2. NONE and
 * DONE never block, so the crystal resummon ritual — which happens
 * when no trial dragon lives — is completely untouched, and a
 * vanilla (untrialed) resummon fight is untouched too: its machine
 * never leaves NONE.
 */
public final class CrystalPlacementRule {

    private CrystalPlacementRule() {
    }

    /** True when a crystal placement must be blocked. */
    public static boolean blocks(boolean ruleEnabled, BattlePhase phase) {
        if (!ruleEnabled || phase == null) {
            return false;
        }
        return phase == BattlePhase.PHASE1
                || phase == BattlePhase.TRANSITION
                || phase == BattlePhase.PHASE2;
    }
}

