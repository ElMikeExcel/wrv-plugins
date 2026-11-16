/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;

/**
 * The grant plan under Reward Law variant B (0.2.11, B-3 —
 * superseding 0.2.5's fairness-rule split): EVERY signature stack
 * is granted per champion standing at the fall; the named/material
 * classification survives only as the provenance stamp on named
 * gear. These checks pin the PLAN the adapter executes
 * (SealLoot.signatureGrants + perChampionPlan), on the dragon and
 * on a ground-drop seal (the wither), including the bystander
 * case: zero champions standing means zero copies of anything.
 */
public final class SignatureGrantTest {

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

    /** Copies of one material the plan produces for a champion count. */
    private static int copies(List<SealLoot.SignatureGrant> plan, String material, int champions) {
        int total = 0;
        for (SealLoot.SignatureGrant g : plan) {
            if (g.item().material().equals(material)) {
                total += g.copies(champions) * g.item().count();
            }
        }
        return total;
    }

    private static SealLoot.SignatureGrant grantFor(List<SealLoot.SignatureGrant> plan,
                                                    String material) {
        for (SealLoot.SignatureGrant g : plan) {
            if (g.item().material().equals(material)) {
                return g;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        // --- The stamp classification (all that survives of the
        // retired named/shared split): named gear is provenance-
        // stamped per champion; material stacks are plain.
        var dragonPlan = SealLoot.perChampionPlan(SealLoot.defaultsFor(BossKind.ENDER_DRAGON));
        check(dragonPlan.size() == 1 && dragonPlan.get(0).provenanceStamped(),
                "First Breath is provenance-stamped");
        var witherPlan = SealLoot.perChampionPlan(SealLoot.defaultsFor(BossKind.WITHER));
        check(witherPlan.stream().anyMatch(p -> p.item().material().equals("DIAMOND_SWORD")
                        && p.provenanceStamped()),
                "Gravewhisper is provenance-stamped");
        check(witherPlan.stream().anyMatch(p -> p.item().material().equals("WITHER_ROSE")
                        && !p.provenanceStamped()),
                "wither roses are plain, unstamped");

        // --- Dragon: First Breath per champion.
        List<SealLoot.SignatureGrant> dragon = SealLoot.signatureGrants(
                SealLoot.defaultsFor(BossKind.ENDER_DRAGON));
        check(dragon.size() == 1, "dragon plan has the one signature stack");
        check(grantFor(dragon, "ELYTRA") != null && grantFor(dragon, "ELYTRA").perChampion(),
                "First Breath is a per-champion grant");
        check(copies(dragon, "ELYTRA", 1) == 1, "dragon, 1 champion -> 1 elytra");
        check(copies(dragon, "ELYTRA", 2) == 2, "dragon, 2 champions -> 2 elytrae");
        check(copies(dragon, "ELYTRA", 5) == 5, "dragon, 5 champions -> 5 elytrae");
        check(copies(dragon, "ELYTRA", 0) == 0,
                "dragon, 0 champions standing (bystanders only) -> 0 elytrae");

        // --- Wither (a ground-drop seal): EVERY stack per
        // champion under variant B — Gravewhisper AND the roses.
        List<SealLoot.SignatureGrant> wither = SealLoot.signatureGrants(
                SealLoot.defaultsFor(BossKind.WITHER));
        check(grantFor(wither, "DIAMOND_SWORD") != null
                        && grantFor(wither, "DIAMOND_SWORD").perChampion(),
                "Gravewhisper is a per-champion grant");
        check(grantFor(wither, "WITHER_ROSE") != null
                        && grantFor(wither, "WITHER_ROSE").perChampion(),
                "wither roses are per-champion under variant B");
        check(copies(wither, "DIAMOND_SWORD", 1) == 1, "wither, 1 champion -> 1 Gravewhisper");
        check(copies(wither, "DIAMOND_SWORD", 3) == 3, "wither, 3 champions -> 3 Gravewhispers");
        check(copies(wither, "DIAMOND_SWORD", 0) == 0,
                "wither, 0 champions standing -> 0 Gravewhispers");
        check(copies(wither, "WITHER_ROSE", 1) == 2, "roses pay x2 with one champion");
        check(copies(wither, "WITHER_ROSE", 5) == 10,
                "roses pay x2 PER champion with five champions (variant B)");
        check(copies(wither, "WITHER_ROSE", 0) == 0,
                "roses pay nothing with zero champions standing");

        // --- Elder + warden: the same shape — every stack,
        // gear and material alike, per champion.
        List<SealLoot.SignatureGrant> elder = SealLoot.signatureGrants(
                SealLoot.defaultsFor(BossKind.ELDER_GUARDIAN));
        check(copies(elder, "TRIDENT", 2) == 2, "elder, 2 champions -> 2 tridents");
        check(copies(elder, "WET_SPONGE", 4) == 16,
                "elder sponge pays its x4 stack per champion (variant B)");
        check(copies(elder, "HEART_OF_THE_SEA", 4) == 4,
                "elder heart pays per champion (variant B)");
        List<SealLoot.SignatureGrant> warden = SealLoot.signatureGrants(
                SealLoot.defaultsFor(BossKind.WARDEN));
        check(copies(warden, "DIAMOND_LEGGINGS", 2) == 2,
                "warden, 2 champions -> 2 Hushwalkers");
        check(copies(warden, "ECHO_SHARD", 3) == 12,
                "warden shards pay their x4 stack per champion (variant B)");

        // --- Plan completeness: every signature stack appears
        // in the plan exactly once, in table order.
        for (BossKind kind : BossKind.values()) {
            SealLoot.LootTable t = SealLoot.defaultsFor(kind);
            List<SealLoot.SignatureGrant> plan = SealLoot.signatureGrants(t);
            check(plan.size() == t.signature().size(),
                    kind + " plan covers every signature stack");
            boolean order = true;
            for (int i = 0; i < plan.size(); i++) {
                if (plan.get(i).item() != t.signature().get(i)) {
                    order = false;
                }
            }
            check(order, kind + " plan preserves table order");
        }
        check(SealLoot.signatureGrants(null).isEmpty(), "null table -> empty plan");
        check(SealLoot.signatureGrants(SealLoot.LootTable.EMPTY).isEmpty(),
                "empty table -> empty plan");

        System.out.println("SignatureGrantTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

