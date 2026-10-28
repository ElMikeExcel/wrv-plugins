package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.Random;

/**
 * Seal loot (0.2 spec section 4): the settled default tables
 * pinned item for item, the signature-once gate, the bonus-roll
 * cap, deterministic seeded rolls, and the dragon's breath math.
 */
public final class SealLootTest {

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

    private static SealLoot.LootItem find(List<SealLoot.LootItem> items, String material) {
        for (SealLoot.LootItem i : items) {
            if (i.material().equals(material)) {
                return i;
            }
        }
        return null;
    }

    /** The spec 4.5 OUT list, with the scoped 4.6 elytra rule:
     *  the hard-banned materials are illegal everywhere; ELYTRA
     *  is legal ONLY in the dragon's signature list. */
    private static final String[] OUT_BANNED = {"NETHERITE_INGOT", "NETHERITE_SWORD",
            "DRAGON_HEAD", "ENCHANTED_GOLDEN_APPLE", "TOTEM_OF_UNDYING", "SHULKER_BOX",
            "BEACON", "SHULKER_SHELL"};

    private static boolean outListViolation(BossKind kind, SealLoot.LootTable t) {
        for (SealLoot.LootItem i : t.signature()) {
            for (String b : OUT_BANNED) {
                if (i.material().equals(b)) {
                    return true;
                }
            }
            if (i.material().equals("ELYTRA") && kind != BossKind.ENDER_DRAGON) {
                return true;
            }
        }
        for (SealLoot.BonusEntry e : t.bonus()) {
            for (String b : OUT_BANNED) {
                if (e.material().equals(b)) {
                    return true;
                }
            }
            if (e.material().equals("ELYTRA")) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        // --- The wither signature pool, pinned to spec 4.1.
        SealLoot.LootTable wither = SealLoot.defaultsFor(BossKind.WITHER);
        check(wither.signature().size() == 2, "wither signature has 2 stacks");
        SealLoot.LootItem grave = find(wither.signature(), "DIAMOND_SWORD");
        check(grave != null, "Gravewhisper is in the wither signature");
        check(grave != null && "Gravewhisper".equals(grave.name()), "the sword is named Gravewhisper");
        check(grave != null && grave.enchants().getOrDefault("smite", 0) == 5, "Gravewhisper Smite V");
        check(grave != null && grave.enchants().getOrDefault("sharpness", 0) == 2, "Gravewhisper Sharpness II (the Oct 23 umph)");
        check(grave != null && grave.enchants().getOrDefault("mending", 0) == 1, "Gravewhisper Mending (the Oct 23 umph)");
        check(grave != null && grave.enchants().getOrDefault("looting", 0) == 2, "Gravewhisper Looting II");
        check(grave != null && grave.enchants().getOrDefault("unbreaking", 0) == 3, "Gravewhisper Unbreaking III");
        check(grave != null && Math.abs(grave.remainingDurability() - 0.60) < 1e-9,
                "Gravewhisper pre-worn to ~60%");
        SealLoot.LootItem roses = find(wither.signature(), "WITHER_ROSE");
        check(roses != null && roses.count() == 2, "wither signature pays 2 roses");
        // Wither bonus table: rose 40 / coal 30 / bone block 30.
        check(wither.bonus().size() == 3, "wither bonus table has 3 rows");
        int wSum = wither.bonus().stream().mapToInt(SealLoot.BonusEntry::weight).sum();
        check(wSum == 100, "wither bonus weights sum to 100");

        // --- The elder signature pool, pinned to spec 4.2.
        SealLoot.LootTable elder = SealLoot.defaultsFor(BossKind.ELDER_GUARDIAN);
        check(elder.signature().size() == 4, "elder signature has 4 stacks");
        check(find(elder.signature(), "TIDE_ARMOR_TRIM_SMITHING_TEMPLATE") != null,
                "elder signature pays the tide trim");
        check(find(elder.signature(), "HEART_OF_THE_SEA") != null,
                "elder signature pays a Heart of the Sea");
        SealLoot.LootItem answer = find(elder.signature(), "TRIDENT");
        check(answer != null && "Monument's Answer".equals(answer.name()),
                "the trident is named Monument's Answer");
        check(answer != null && answer.enchants().getOrDefault("impaling", 0) == 5,
                "Monument's Answer Impaling V (trophy grade, TestPlayerA's Oct 23 amendment)");
        check(answer != null && answer.enchants().getOrDefault("unbreaking", 0) == 3
                && answer.enchants().getOrDefault("mending", 0) == 1,
                "Monument's Answer Unbreaking III + Mending (trophy grade)");
        check(answer != null && answer.enchants().getOrDefault("loyalty", 0) == 3,
                "Monument's Answer Loyalty III");
        check(answer != null && Math.abs(answer.remainingDurability() - 0.50) < 1e-9,
                "Monument's Answer pre-worn to ~50%");
        SealLoot.LootItem sponge = find(elder.signature(), "WET_SPONGE");
        check(sponge != null && sponge.count() == 4, "elder signature pays 4 wet sponge");

        // --- The warden signature pool, pinned to spec 4.3.
        SealLoot.LootTable warden = SealLoot.defaultsFor(BossKind.WARDEN);
        check(warden.signature().size() == 4, "warden signature has 4 stacks");
        SealLoot.LootItem hush = find(warden.signature(), "DIAMOND_LEGGINGS");
        check(hush != null && "Hushwalkers".equals(hush.name()),
                "warden signature pays the Hushwalkers (trophy grade, TestPlayerA's Oct 23 amendment)");
        check(hush != null && hush.enchants().getOrDefault("swift_sneak", 0) == 3
                && hush.enchants().getOrDefault("protection", 0) == 4
                && hush.enchants().getOrDefault("mending", 0) == 1,
                "Hushwalkers Swift Sneak III + Protection IV + Mending");
        SealLoot.LootItem shards = find(warden.signature(), "ECHO_SHARD");
        check(shards != null && shards.count() == 4, "warden signature pays 4 echo shards");
        check(find(warden.signature(), "RECOVERY_COMPASS") != null,
                "warden signature pays a recovery compass");
        SealLoot.LootItem frag = find(warden.signature(), "DISC_FRAGMENT_5");
        check(frag != null && frag.count() == 3, "warden signature pays 3 disc-5 fragments");

        // --- The dragon signature (spec 4.4 as amended by 4.6):
        // "First Breath" - the bonus table stays empty; the
        // per-kill pay remains the breath/crystal numbers.
        SealLoot.LootTable dragon = SealLoot.defaultsFor(BossKind.ENDER_DRAGON);
        check(dragon.signature().size() == 1, "dragon signature has 1 stack (spec 4.6)");
        SealLoot.LootItem breath = find(dragon.signature(), "ELYTRA");
        check(breath != null, "First Breath is in the dragon signature");
        check(breath != null && "First Breath".equals(breath.name()),
                "the elytra is named First Breath");
        check(breath != null && breath.enchants().getOrDefault("unbreaking", 0) == 3,
                "First Breath Unbreaking III");
        check(breath != null && breath.enchants().getOrDefault("mending", 0) == 1,
                "First Breath Mending");
        check(breath != null && breath.count() == 1, "First Breath pays one elytra per copy");
        check(breath != null && Math.abs(breath.remainingDurability() - 1.0) < 1e-9,
                "First Breath spawns whole (durability 1.0)");
        check(dragon.bonus().isEmpty(), "dragon bonus table stays empty (spec 4.6)");

        // --- The OUT list (spec 4.5): none of these materials may
        // appear in ANY signature or bonus table. ELYTRA is the
        // scoped 4.6 amendment: permitted ONLY as the dragon
        // signature, nowhere else - pinned below both ways.
        for (BossKind kind : BossKind.values()) {
            SealLoot.LootTable t = SealLoot.defaultsFor(kind);
            check(!outListViolation(kind, t), kind + " defaults respect the OUT list");
            for (SealLoot.LootItem i : t.signature()) {
                // Mending BOOKS stay OUT (4.5/4.6): applied
                // Mending on named signature gear is the 4.6
                // amendment (the per-item pins above carry the
                // exact levels); a book would print tradeable
                // treasure and undercut the villager economy.
                check(!(i.material().equals("ENCHANTED_BOOK")
                                && i.enchants().containsKey("mending")),
                        kind + " signature carries no Mending book");
            }
        }
        // The elytra exemption, scoped and negative-pinned: the
        // dragon signature is the ONLY legal home for an elytra.
        check(!outListViolation(BossKind.ENDER_DRAGON,
                        SealLoot.defaultsFor(BossKind.ENDER_DRAGON)),
                "elytra in the dragon signature is legal (4.6 amendment)");
        SealLoot.LootTable elytraWither = new SealLoot.LootTable(
                List.of(new SealLoot.LootItem("ELYTRA", 1, "First Breath",
                        java.util.Map.of("unbreaking", 3, "mending", 1), 1.0)),
                List.of());
        check(outListViolation(BossKind.WITHER, elytraWither),
                "elytra in the WITHER signature violates the OUT list");
        SealLoot.LootTable elytraDragonBonus = new SealLoot.LootTable(
                List.of(),
                List.of(new SealLoot.BonusEntry("ELYTRA", 1, 1, 10)));
        check(outListViolation(BossKind.ENDER_DRAGON, elytraDragonBonus),
                "elytra in the DRAGON BONUS table violates the OUT list");
        SealLoot.LootTable elytraWarden = new SealLoot.LootTable(
                List.of(new SealLoot.LootItem("ELYTRA", 1, null,
                        java.util.Map.of(), 1.0)),
                List.of());
        check(outListViolation(BossKind.WARDEN, elytraWarden),
                "elytra in the WARDEN signature violates the OUT list");

        // --- Signature-once gate (spec 4.0 rule 1).
        check(SealLoot.paysSignature(CreditJudge.Verdict.CREDIT, false, false),
                "first credit kill pays the signature");
        check(!SealLoot.paysSignature(CreditJudge.Verdict.CREDIT, false, true),
                "a second credit kill does NOT re-pay the signature");
        check(!SealLoot.paysSignature(CreditJudge.Verdict.CREDIT, true, false),
                "an elder trio-progress kill does not pay the signature early");
        check(!SealLoot.paysSignature(CreditJudge.Verdict.DENY_SHARE, false, false),
                "a denied kill pays no signature");
        check(!SealLoot.paysSignature(CreditJudge.Verdict.DENY_PRESENCE, false, false),
                "a presence denial pays no signature");
        check(!SealLoot.paysSignature(CreditJudge.Verdict.DENY_COOLDOWN, false, false),
                "a cooldown denial pays no signature");

        // --- Bonus rolls: one per champion beyond the first, cap +4.
        check(SealLoot.bonusRolls(0) == 0, "no champions, no rolls");
        check(SealLoot.bonusRolls(1) == 0, "solo credit kill: no bonus rolls");
        check(SealLoot.bonusRolls(2) == 1, "duo: one roll");
        check(SealLoot.bonusRolls(3) == 2, "trio: two rolls");
        check(SealLoot.bonusRolls(5) == 4, "five champions: four rolls");
        check(SealLoot.bonusRolls(9) == 4, "rolls cap at +4");

        // --- Seeded rolls are deterministic and in-table.
        List<SealLoot.RolledBonus> a = SealLoot.rollBonus(wither, 4, new Random(42L));
        List<SealLoot.RolledBonus> b = SealLoot.rollBonus(wither, 4, new Random(42L));
        check(a.equals(b), "same seed, same rolls (deterministic)");
        check(a.size() == 4, "four rolls over a full-weight table pay four stacks");
        for (SealLoot.RolledBonus r : a) {
            boolean inTable = wither.bonus().stream().anyMatch(e ->
                    e.material().equals(r.material())
                            && r.count() >= e.minCount() && r.count() <= e.maxCount());
            check(inTable, "rolled stack " + r.material() + " x" + r.count() + " is in the table");
        }
        // Ranged counts (warden echo shard 1-2) stay in range.
        List<SealLoot.RolledBonus> many = SealLoot.rollBonus(
                SealLoot.defaultsFor(BossKind.WARDEN), 200, new Random(7L));
        boolean sawEcho = false;
        for (SealLoot.RolledBonus r : many) {
            if (r.material().equals("ECHO_SHARD")) {
                sawEcho = true;
                check(r.count() >= 1 && r.count() <= 2, "echo shard roll within 1-2");
            }
        }
        check(sawEcho, "200 warden rolls include echo shards");
        // Empty inputs pay nothing.
        check(SealLoot.rollBonus(SealLoot.LootTable.EMPTY, 4, new Random(1L)).isEmpty(),
                "empty table pays no rolls");
        check(SealLoot.rollBonus(wither, 0, new Random(1L)).isEmpty(),
                "zero rolls pay nothing");

        // --- Dragon breath math (spec 4.4): 4 + 2 per extra, cap 12.
        check(SealLoot.dragonBreath(1) == 4, "solo dragon breath 4");
        check(SealLoot.dragonBreath(2) == 6, "duo dragon breath 6");
        check(SealLoot.dragonBreath(3) == 8, "trio dragon breath 8");
        check(SealLoot.dragonBreath(5) == 12, "five champions dragon breath 12");
        check(SealLoot.dragonBreath(9) == 12, "dragon breath caps at 12");
        check(SealLoot.DRAGON_CRYSTALS == 2, "dragon pays 2 end crystals per credit kill");

        System.out.println("SealLootTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

