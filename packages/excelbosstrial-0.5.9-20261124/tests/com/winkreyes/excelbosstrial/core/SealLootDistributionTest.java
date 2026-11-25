/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;

/**
 * 0.2.11 (B-3): the per-champion distribution the adapter stores.
 * The ledger half is exercised end-to-end in the rig; this suite
 * pins the pure plan math the adapter executes: for a roster of N,
 * EVERY member's claim is the full signature set (gear stamped,
 * materials plain, counts exact), the elder set pays to the trio
 * roster exactly like its gear, and the dragon's per-kill materials
 * are a per-champion set — breath x4 + crystals x2 at the settled
 * config defaults — on every CREDIT kill.
 */
public final class SealLootDistributionTest {

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

    /** One champion's full set for a table: plan stacks x copies(N == 1 claim). */
    private static int setCount(List<SealLoot.PlannedStack> plan, String material) {
        int total = 0;
        for (SealLoot.PlannedStack p : plan) {
            if (p.item().material().equals(material)) {
                total += p.item().count();
            }
        }
        return total;
    }

    public static void main(String[] args) {
        // --- Wither set, per champion: sword 1 (stamped) + roses 2 (plain).
        List<SealLoot.PlannedStack> wither = SealLoot.perChampionPlan(
                SealLoot.defaultsFor(BossKind.WITHER));
        check(wither.size() == 2, "wither set has both stacks");
        check(setCount(wither, "DIAMOND_SWORD") == 1, "wither set: one Gravewhisper per champion");
        check(setCount(wither, "WITHER_ROSE") == 2, "wither set: roses x2 per champion");
        check(wither.stream().filter(p -> p.provenanceStamped()).count() == 1,
                "wither set: exactly the named gear is stamped");

        // --- Elder set, per champion (the trio roster receives it whole):
        // trim 1, heart 1, trident 1 (stamped), sponge 4 — all present.
        List<SealLoot.PlannedStack> elder = SealLoot.perChampionPlan(
                SealLoot.defaultsFor(BossKind.ELDER_GUARDIAN));
        check(elder.size() == 4, "elder set has all four stacks");
        check(setCount(elder, "TIDE_ARMOR_TRIM_SMITHING_TEMPLATE") == 1, "elder set: trim x1");
        check(setCount(elder, "HEART_OF_THE_SEA") == 1, "elder set: heart x1");
        check(setCount(elder, "TRIDENT") == 1, "elder set: one Monument's Answer");
        check(setCount(elder, "WET_SPONGE") == 4, "elder set: sponge x4 per champion");
        check(elder.stream().anyMatch(p -> p.item().material().equals("TRIDENT")
                        && p.provenanceStamped()),
                "elder set: the trident is the stamped piece");

        // --- Warden set, per champion: leggings (stamped), shards 4,
        // compass 1, disc fragments 3.
        List<SealLoot.PlannedStack> warden = SealLoot.perChampionPlan(
                SealLoot.defaultsFor(BossKind.WARDEN));
        check(warden.size() == 4, "warden set has all four stacks");
        check(setCount(warden, "DIAMOND_LEGGINGS") == 1, "warden set: Hushwalkers x1");
        check(setCount(warden, "ECHO_SHARD") == 4, "warden set: shards x4 per champion");
        check(setCount(warden, "RECOVERY_COMPASS") == 1, "warden set: compass x1");
        check(setCount(warden, "DISC_FRAGMENT_5") == 3, "warden set: disc fragments x3");

        // --- Dragon signature set: the elytra, stamped, once per champion.
        List<SealLoot.PlannedStack> dragon = SealLoot.perChampionPlan(
                SealLoot.defaultsFor(BossKind.ENDER_DRAGON));
        check(dragon.size() == 1 && setCount(dragon, "ELYTRA") == 1
                        && dragon.get(0).provenanceStamped(),
                "dragon set: one stamped First Breath per champion");

        // --- Roster math: a roster of 3 receives 3 full sets (the
        // adapter stores the plan once per roster member); a roster
        // of 0 receives nothing — no phantom shared copy survives.
        for (BossKind kind : BossKind.values()) {
            List<SealLoot.SignatureGrant> grants = SealLoot.signatureGrants(
                    SealLoot.defaultsFor(kind));
            for (SealLoot.SignatureGrant g : grants) {
                check(g.perChampion(), kind + " " + g.item().material() + " is per-champion");
                check(g.copies(3) == 3, kind + " " + g.item().material() + ": 3 champions -> 3 sets");
                check(g.copies(0) == 0, kind + " " + g.item().material() + ": 0 champions -> nothing");
            }
        }

        // --- Dragon materials: per-champion set at the settled
        // defaults, and the config switches behave.
        List<SealLoot.LootItem> mats = SealLoot.dragonMaterials(
                SealLoot.DRAGON_BREATH_BASE, SealLoot.DRAGON_CRYSTALS);
        check(mats.size() == 2, "dragon materials: breath + crystals");
        check(mats.stream().anyMatch(i -> i.material().equals("DRAGON_BREATH") && i.count() == 4),
                "dragon materials: breath x4 per champion");
        check(mats.stream().anyMatch(i -> i.material().equals("END_CRYSTAL") && i.count() == 2),
                "dragon materials: crystals x2 per champion");
        check(mats.stream().allMatch(i -> i.name() == null),
                "dragon materials are plain stacks (never provenance-stamped)");
        check(SealLoot.dragonMaterials(4, 0).size() == 1
                        && SealLoot.dragonMaterials(4, 0).get(0).material().equals("DRAGON_BREATH"),
                "crystals configured to 0 drop out");
        check(SealLoot.dragonMaterials(0, 2).size() == 1
                        && SealLoot.dragonMaterials(0, 2).get(0).material().equals("END_CRYSTAL"),
                "breath configured to 0 drops out");
        check(SealLoot.dragonMaterials(0, 0).isEmpty(), "both at 0 pay no materials");
        check(SealLoot.dragonMaterials(-1, -2).isEmpty(), "negative amounts pay no materials");

        System.out.println("SealLootDistributionTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
