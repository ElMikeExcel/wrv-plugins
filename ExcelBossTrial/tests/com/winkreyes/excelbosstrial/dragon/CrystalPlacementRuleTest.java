package com.winkreyes.excelbosstrial.dragon;

import com.winkreyes.excelbosstrial.dragon.BattlePhase;
import com.winkreyes.excelbosstrial.dragon.CrystalPlacementRule;

/**
 * P3 (0.1.1): crystal placement is blocked exactly while a trial
 * battle is active — and the resummon ritual, which happens with no
 * dragon alive, is never touched.
 */
public final class CrystalPlacementRuleTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        check(CrystalPlacementRule.blocks(true, BattlePhase.PHASE1),
                "blocked during phase 1");
        check(CrystalPlacementRule.blocks(true, BattlePhase.TRANSITION),
                "blocked during the flip transition");
        check(CrystalPlacementRule.blocks(true, BattlePhase.PHASE2),
                "blocked during phase 2");
        check(!CrystalPlacementRule.blocks(true, BattlePhase.NONE),
                "allowed with no battle (fresh world, ritual untouched)");
        check(!CrystalPlacementRule.blocks(true, BattlePhase.DONE),
                "allowed after the battle (resummon ritual untouched)");

        for (BattlePhase p : BattlePhase.values()) {
            check(!CrystalPlacementRule.blocks(false, p),
                    "rule off: never blocks (" + p + ")");
        }
        check(!CrystalPlacementRule.blocks(true, null), "null phase never blocks");

        System.out.println("CrystalPlacementRuleTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
