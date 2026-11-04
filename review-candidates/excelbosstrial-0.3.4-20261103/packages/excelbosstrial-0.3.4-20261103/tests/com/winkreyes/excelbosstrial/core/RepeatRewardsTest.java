/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.Map;

/**
 * The repeat reward math (0.3.0, SPEC section 4): the underdog
 * factor U(n) = clamp(4/n, 1, 2) on rolls and accelerants only,
 * spoils following the repeat verdict, the jackpot roll count,
 * the generation-scoped signature gate, and the reforged
 * transform (one tier deep, vanilla max + 1, Mending excluded).
 */
public final class RepeatRewardsTest {

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
        // Underdog factor.
        check(Underdog.factor(1) == 2.0, "U(1) = 2 (solo repeat pays double)");
        check(Underdog.factor(2) == 2.0, "U(2) = 2");
        check(Math.abs(Underdog.factor(3) - 4.0 / 3.0) < 1e-9, "U(3) = 4/3");
        check(Underdog.factor(4) == 1.0, "U(4) = 1 (the reference group)");
        check(Underdog.factor(8) == 1.0, "U(8) clamps at 1 - a crowd never dilutes");
        check(Underdog.factor(0) == 1.0, "U(0) reads as the reference group");

        // Scaled counts and XP.
        check(Underdog.scaledCount(3, 1) == 6, "count 3 solo: 6");
        check(Underdog.scaledCount(3, 4) == 3, "count 3 at reference: 3");
        check(Underdog.scaledCount(1, 3) == 1, "count 1 at U=4/3 floors to 1, never 0");
        check(Underdog.scaledCount(0, 1) == 0, "count 0 stays 0");
        check(Underdog.scaledXp(5000, 2) == 10000, "mcMMO 5,000 duo: 10,000");
        check(Underdog.scaledXp(5000, 4) == 5000, "mcMMO 5,000 at reference: 5,000");
        check(Underdog.scaledXp(250, 1) == 500, "MagicLevel 250 solo: 500");
        check(Underdog.scaledXp(0, 1) == 0, "XP 0 stays 0");

        // Spoils follow the verdict: REPEAT_CREDIT multiplies like
        // CREDIT; the window denial pays vanilla.
        check(SpoilsMath.multiplierFor(true, CreditJudge.Verdict.REPEAT_CREDIT, 3, 5) == 3,
                "repeat spoils x3 for 3 champions");
        check(SpoilsMath.multiplierFor(true, CreditJudge.Verdict.DENY_REPEAT_WINDOW, 3, 5) == 1,
                "window-denied repeat: vanilla drops");
        check(SpoilsMath.multiplierFor(false, CreditJudge.Verdict.REPEAT_CREDIT, 3, 5) == 1,
                "spoils off: repeat pays vanilla");

        // Jackpot rolls: bonus rolls + the settled extra rolls.
        check(SealLoot.repeatRolls(4, 2) == 5, "4 champions: 3 bonus + 2 jackpot = 5 rolls");
        check(SealLoot.repeatRolls(1, 2) == 2, "solo: 0 bonus + 2 jackpot = 2 rolls");
        check(SealLoot.repeatRolls(6, 2) == 6, "6 champions: capped bonus 4 + 2 = 6 rolls");
        check(SealLoot.bonusRolls(4) == 3, "first-break rolls unchanged (3 at 4 champions)");

        // The combined draw table.
        SealLoot.LootTable table = new SealLoot.LootTable(
                List.of(),
                List.of(new SealLoot.BonusEntry("DIAMOND", 1, 1, 10)),
                List.of(new SealLoot.BonusEntry("NETHERITE_SCRAP", 1, 1, 5)));
        check(table.bonusAndJackpot().size() == 2, "bonusAndJackpot combines both tables");
        check(table.bonus().size() == 1 && table.jackpot().size() == 1,
                "the source tables stay separate");

        // Generation-scoped signature gate.
        check(SealLoot.paysSignature(CreditJudge.Verdict.CREDIT, false, true, true),
                "first break pays its generation");
        check(SealLoot.paysSignature(CreditJudge.Verdict.REPEAT_CREDIT, false, true, true),
                "repeat pays the next generation while the switch is on");
        check(!SealLoot.paysSignature(CreditJudge.Verdict.REPEAT_CREDIT, false, false, true),
                "repeat signature switch off: no reforged payment");
        check(!SealLoot.paysSignature(CreditJudge.Verdict.CREDIT, true, true, true),
                "elder progress never pays a signature");
        check(!SealLoot.paysSignature(CreditJudge.Verdict.DENY_REPEAT_WINDOW, false, true, true),
                "window denial pays no signature");
        check(SealLoot.paysSignature(CreditJudge.Verdict.CREDIT, false, false),
                "legacy 3-arg gate still pays a fresh CREDIT");
        check(!SealLoot.paysSignature(CreditJudge.Verdict.CREDIT, false, true),
                "legacy 3-arg gate refuses an already-paid seal");

        // Reforged transform.
        Map<String, Integer> vanillaMax = Map.of("sharpness", 5, "mending", 1, "unbreaking", 3);
        SealLoot.LootItem sword = new SealLoot.LootItem("DIAMOND_SWORD", 1, "Test Blade",
                Map.of("sharpness", 5, "mending", 1, "unbreaking", 2), 0.5);
        SealLoot.LootItem reforged = SealLoot.reforgedItem(sword, vanillaMax);
        check(reforged.enchants().get("sharpness") == 6, "sharpness 5 -> 6 (vanilla max + 1)");
        check(reforged.enchants().get("mending") == 1, "mending never rises");
        check(reforged.enchants().get("unbreaking") == 3, "unbreaking 2 -> 3");
        check(reforged.remainingDurability() == 1.0, "reforged items are pristine");
        SealLoot.LootItem unknown = new SealLoot.LootItem("BOW", 1, null,
                Map.of("power", 4), 1.0);
        check(SealLoot.reforgedItem(unknown, vanillaMax).enchants().get("power") == 4,
                "an enchant missing from the max map keeps its level");
        SealLoot.LootItem overMax = new SealLoot.LootItem("BOW", 1, null,
                Map.of("sharpness", 7), 1.0);
        check(SealLoot.reforgedItem(overMax, vanillaMax).enchants().get("sharpness") == 6,
                "the formula is min(level+1, max+1): an over-max input clamps to the cap");

        System.out.println("RepeatRewardsTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
