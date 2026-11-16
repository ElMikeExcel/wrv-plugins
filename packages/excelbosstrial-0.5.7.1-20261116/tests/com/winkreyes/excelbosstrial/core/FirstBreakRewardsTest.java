/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.time.LocalDate;
import java.util.List;

/**
 * The Warden first-break layer (0.5.5): the eligibility matrix
 * (fires exactly once, on the break itself only), the settled
 * slate values, the EM kit contents exact, and the title /
 * trophy item specs. The host's crash-safety order (once-flag
 * flushed before payment) is pinned by the matrix's second
 * half: once the flag stands, no later verdict - not even a
 * fresh CREDIT after an admin revoke + re-break - fires again.
 */
public final class FirstBreakRewardsTest {

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
        FirstBreakRewards.Spec spec = FirstBreakRewards.Spec.settled();

        // Eligibility matrix: fires exactly on the break itself.
        check(FirstBreakRewards.eligible(CreditJudge.Verdict.CREDIT, BossKind.WARDEN,
                        false, false, spec),
                "CREDIT + warden + unpaid fires");
        check(!FirstBreakRewards.eligible(CreditJudge.Verdict.REPEAT_CREDIT, BossKind.WARDEN,
                        false, false, spec),
                "REPEAT_CREDIT never fires (repeat breaks unchanged)");
        check(!FirstBreakRewards.eligible(CreditJudge.Verdict.CREDIT, BossKind.WARDEN,
                        false, true, spec),
                "CREDIT + once-flag paid never fires again");
        check(!FirstBreakRewards.eligible(CreditJudge.Verdict.CREDIT, BossKind.WITHER,
                        false, false, spec),
                "non-Warden CREDIT never fires (wither)");
        check(!FirstBreakRewards.eligible(CreditJudge.Verdict.CREDIT, BossKind.ELDER_GUARDIAN,
                        false, false, spec),
                "non-Warden CREDIT never fires (elder)");
        check(!FirstBreakRewards.eligible(CreditJudge.Verdict.CREDIT, BossKind.ENDER_DRAGON,
                        false, false, spec),
                "non-Warden CREDIT never fires (dragon)");
        check(!FirstBreakRewards.eligible(CreditJudge.Verdict.CREDIT, BossKind.WARDEN,
                        true, false, spec),
                "elder-progress-shaped CREDIT never fires");
        check(!FirstBreakRewards.eligible(CreditJudge.Verdict.DENY_REPEAT_WINDOW, BossKind.WARDEN,
                        false, false, spec),
                "window denial never fires");
        check(!FirstBreakRewards.eligible(CreditJudge.Verdict.DENY_SHARE, BossKind.WARDEN,
                        false, false, spec),
                "share denial never fires");
        FirstBreakRewards.Spec off = new FirstBreakRewards.Spec(false, 10000L, 2, 5000L, 250L,
                2.0, true, true, "ECHO_SHARD", "Sealbreakers", spec.emKit());
        check(!FirstBreakRewards.eligible(CreditJudge.Verdict.CREDIT, BossKind.WARDEN,
                        false, false, off),
                "layer disabled in config never fires");
        check(!FirstBreakRewards.eligible(CreditJudge.Verdict.CREDIT, BossKind.WARDEN,
                        false, false, null),
                "null spec never fires");

        // Exactly-once across a simulated revoke + re-break: the
        // verdict layer alone would judge CREDIT again (the seal
        // stands unbroken after a revoke), but the durable flag
        // the host flushed before payment holds the line.
        boolean paid = false;
        check(FirstBreakRewards.eligible(CreditJudge.Verdict.CREDIT, BossKind.WARDEN,
                        false, paid, spec),
                "first break fires while unpaid");
        paid = true; // host: flag written + saveState() before any reward moves
        check(!FirstBreakRewards.eligible(CreditJudge.Verdict.CREDIT, BossKind.WARDEN,
                        false, paid, spec),
                "revoke + re-break CREDIT stays silent (flag)");
        check(!FirstBreakRewards.eligible(CreditJudge.Verdict.REPEAT_CREDIT, BossKind.WARDEN,
                        false, paid, spec),
                "post-revoke repeat stays on the ordinary repeat slate");

        // Settled slate values (D6: $10,000 TOTAL, replacing the
        // $5,000 repeat rate - never $15,000).
        check(spec.enabled(), "settled spec is enabled");
        check(spec.cashPerChampion() == 10000L, "first-break cash is $10,000 per champion");
        check(spec.cashPerChampion() == 2 * 5000L, "first-break cash is exactly double the repeat rate (replace, not add)");
        check(spec.jackpotExtraRolls() == 2, "first-break jackpot is +2 rolls");
        check(spec.mcMmoXp() == 5000L, "mcMMO accelerant is 5,000 (same as repeat)");
        check(spec.magicLevelXp() == 250L, "MagicLevel accelerant is 250 (same as repeat)");
        check(spec.claimflyHours() == 2.0, "war-chest claim fly is 2 hours (same as repeat)");
        check(spec.deathProtectionPremium(), "war-chest death-protection premium is on (same as repeat)");
        check(spec.healerRecognitionEnabled(), "healer recognition is on (same as repeat)");
        check("ECHO_SHARD".equals(spec.cosmeticMaterial()), "seal cosmetic is the Warden Tribute ECHO_SHARD");
        check("Sealbreakers".equals(spec.title()), "party title is Sealbreakers");

        // Rolls: bonus rolls + the jackpot extras, the repeat shape.
        check(FirstBreakRewards.rolls(4, spec) == 5, "4 champions: 3 bonus + 2 jackpot = 5 rolls");
        check(FirstBreakRewards.rolls(1, spec) == 2, "solo: 0 bonus + 2 jackpot = 2 rolls");
        check(FirstBreakRewards.rolls(6, spec) == 6, "6 champions: capped bonus 4 + 2 = 6 rolls");
        check(SealLoot.bonusRolls(4) == 3, "ordinary first-break rolls elsewhere unchanged (3 at 4 champions)");

        // EM kit contents, exact (design section 2.4).
        List<FirstBreakRewards.KitItem> kit = spec.emKit();
        check(kit.size() == 9, "EM kit carries exactly 9 rows");
        check(kit.get(0).equals(FirstBreakRewards.KitItem.plain("GOLDEN_CARROT", 16)),
                "kit row 1: 16 golden carrots");
        check(kit.get(1).equals(FirstBreakRewards.KitItem.plain("ENCHANTED_GOLDEN_APPLE", 1)),
                "kit row 2: 1 enchanted golden apple");
        check(kit.get(2).equals(FirstBreakRewards.KitItem.plain("GOLDEN_APPLE", 4)),
                "kit row 3: 4 golden apples");
        check(kit.get(3).equals(FirstBreakRewards.KitItem.plain("TOTEM_OF_UNDYING", 1)),
                "kit row 4: 1 totem of undying");
        check(kit.get(4).equals(FirstBreakRewards.KitItem.potion("SPLASH_POTION", 4, "HEALING", 2)),
                "kit row 5: 4 splash potions of Healing II");
        check(kit.get(5).equals(FirstBreakRewards.KitItem.potion("POTION", 2, "STRENGTH", 2)),
                "kit row 6: 2 potions of Strength II");
        check(kit.get(6).equals(FirstBreakRewards.KitItem.potion("POTION", 2, "SWIFTNESS", 2)),
                "kit row 7: 2 potions of Swiftness II");
        check(kit.get(7).equals(FirstBreakRewards.KitItem.plain("MILK_BUCKET", 2)),
                "kit row 8: 2 milk buckets");
        check(kit.get(8).equals(FirstBreakRewards.KitItem.plain("EXPERIENCE_BOTTLE", 16)),
                "kit row 9: 16 bottles o' enchanting");
        boolean anyGear = kit.stream().anyMatch(k -> k.material().contains("SWORD")
                || k.material().contains("CHESTPLATE") || k.material().contains("LEGGINGS")
                || k.material().contains("HELMET") || k.material().contains("BOOTS")
                || k.material().contains("BOW") || k.material().contains("SHIELD"));
        check(!anyGear, "EM kit carries NO gear (consumables only)");

        // Title + trophy specs (design section 2.2).
        LocalDate date = LocalDate.of(2026, 11, 14);
        List<String> titleLore = FirstBreakRewards.titleLore(date);
        check(titleLore.size() == 2, "title lore is two lines");
        check(titleLore.get(0).equals("Awarded for the first breaking of the Warden seal."),
                "title lore line 1 is the award line");
        check(titleLore.get(1).equals("The Deep listened \u2014 and the party answered. November 14, 2026."),
                "title lore line 2 carries the Deep line + the break date");
        check("ECHO_SHARD".equals(FirstBreakRewards.TROPHY_MATERIAL),
                "trophy material is ECHO_SHARD (a display item, not gear)");
        check(FirstBreakRewards.TROPHY_NAME.equals("Heart of the Deep \u2014 Warden Trophy"),
                "trophy name is Heart of the Deep - Warden Trophy");
        check("first-break-v1".equals(FirstBreakRewards.DISTRIBUTION_ID),
                "claims distribution id is first-break-v1");
        List<String> trophyLore = FirstBreakRewards.trophyLore("Mike",
                date, List.of("Mike", "Kailey", "Rylie"));
        check(trophyLore.size() == 3, "trophy lore is provenance (2) + party (1)");
        check(trophyLore.get(0).equals("Earned by Mike"), "trophy provenance names the earner");
        check(trophyLore.get(1).equals("November 14, 2026"), "trophy provenance carries the date");
        check(trophyLore.get(2).equals("First break party: Mike, Kailey, Rylie"),
                "trophy party line lists the whole party");
        check(FirstBreakRewards.partyLine(List.of()).equals("First break party: a champion"),
                "empty party degrades to 'a champion', never blank");
        check(FirstBreakRewards.partyLine(null).equals("First break party: a champion"),
                "null party degrades the same way");

        // Kit row hygiene: clamps hold whatever config hands over.
        check(new FirstBreakRewards.KitItem("POTION", 0, "healing", 9).count() == 1,
                "kit count clamps to at least 1");
        check(new FirstBreakRewards.KitItem("POTION", 1, "healing", 9).level() == 2,
                "kit potion level clamps to tier 2");
        check("HEALING".equals(new FirstBreakRewards.KitItem("POTION", 1, "healing", 1).potion()),
                "kit potion keys normalize to upper case");
        check(new FirstBreakRewards.KitItem("POTION", 1, " ", 1).potion() == null,
                "blank potion key reads as a plain row");

        System.out.println("FirstBreakRewardsTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
