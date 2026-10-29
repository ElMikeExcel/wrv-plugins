/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * SEAL LOOT (ExcelBossTrial 0.2, spec BOSSTRIAL-0.2-SPEC-2026-10-20.md
 * section 4; Mike's shaping + decisions settled Oct 20, 2026): the
 * payoff side of the first-attempt doctrine. Curated TROPHY-tier
 * rewards on CREDIT kills only - a denied kill drops plain vanilla
 * loot, exactly as settled. Curated gear is never multiplied by
 * the participant count (the xN spoils rule stays on the boss's
 * VANILLA drops and the dragon's XP, where it was settled).
 *
 * Two governing rules (spec section 4.0):
 *  1. THE SIGNATURE PAYS ONCE PER SEAL. Each boss has one
 *     signature trophy pool; it drops on the kill that BREAKS the
 *     seal (for the elder: the trio-completing kill) - once,
 *     server-wide, tracked in state.yml so it survives restarts.
 *     0.2.5 (spec 4.6, the FAIRNESS RULE): the named gear in the
 *     pool is granted PER CHAMPION standing at the fall - one
 *     copy each, stamped with its earner's provenance - while
 *     the material stacks stay a single shared drop. "Once per
 *     seal" still binds the pool as a whole: no second paying
 *     kill, however the copies divide.
 *  2. BONUS ROLLS SCALE MILDLY. On every credit kill, one bonus
 *     roll per champion beyond the first (cap +4), drawn from the
 *     boss's bonus table of materials and curios only - never
 *     gear, never a second signature.
 *
 * Pure data + math like the rest of core: item specs are plain
 * material names / enchant keys / counts; the Bukkit adapter
 * turns them into ItemStacks. Rolls take an injected Random so
 * tests are deterministic.
 */
public final class SealLoot {

    /** One curated stack spec. {@code name} is the display name
     *  (null = the material's own name); {@code enchants} maps
     *  enchant keys (e.g. "smite") to levels - for an
     *  ENCHANTED_BOOK these are stored enchants; {@code
     *  remainingDurability} is the fraction of durability the item
     *  keeps (1.0 = fresh, 0.6 = pre-worn to ~60%). */
    public record LootItem(String material, int count, String name,
                           Map<String, Integer> enchants, double remainingDurability) {
        public LootItem {
            count = Math.max(1, count);
            enchants = enchants == null ? Map.of() : Map.copyOf(enchants);
            if (Double.isNaN(remainingDurability)) {
                remainingDurability = 1.0;
            }
            remainingDurability = Math.max(0.01, Math.min(1.0, remainingDurability));
        }

        public static LootItem plain(String material, int count) {
            return new LootItem(material, count, null, Map.of(), 1.0);
        }
    }

    /** One bonus-table row: a material stack of min..max, picked
     *  with probability weight/totalWeight per roll. */
    public record BonusEntry(String material, int minCount, int maxCount, int weight) {
        public BonusEntry {
            minCount = Math.max(1, minCount);
            maxCount = Math.max(minCount, maxCount);
            weight = Math.max(0, weight);
        }
    }

    /** One boss's curated tables (signature pool + bonus table). */
    public record LootTable(List<LootItem> signature, List<BonusEntry> bonus) {
        public LootTable {
            signature = signature == null ? List.of() : List.copyOf(signature);
            bonus = bonus == null ? List.of() : List.copyOf(bonus);
        }

        public static final LootTable EMPTY = new LootTable(List.of(), List.of());
    }

    /** The outcome of one bonus roll. */
    public record RolledBonus(String material, int count) {
    }

    /**
     * One signature stack's grant shape (0.2.5, the fairness
     * rule): a NAMED item is per-champion gear - it is granted
     * once per standing champion, each copy its earner's own.
     * An unnamed item is a material stack - it drops once,
     * shared, however many champions stood at the fall.
     */
    public record SignatureGrant(LootItem item, boolean perChampion) {
        /** Copies of this stack the kill produces for the given
         *  champion count standing at the fall. */
        public int copies(int champions) {
            return perChampion ? Math.max(0, champions) : 1;
        }
    }

    /** Bonus rolls never exceed this many per kill (spec 4.0). */
    public static final int BONUS_ROLL_CAP = 4;

    // The dragon's per-credit-kill numbers (spec 4.4): breath
    // scales mildly with the party; crystals are a flat pair.
    public static final int DRAGON_BREATH_BASE = 4;
    public static final int DRAGON_BREATH_PER_CHAMPION = 2;
    public static final int DRAGON_BREATH_CAP = 12;
    public static final int DRAGON_CRYSTALS = 2;

    private SealLoot() {
    }

    /** The settled default tables (spec sections 4.1-4.4). The
     *  dragon's bonus table is empty - its per-kill pay is the
     *  breath/crystal numbers - but since the spec 4.6 amendment
     *  it carries a real signature: "First Breath". */
    public static LootTable defaultsFor(BossKind kind) {
        return switch (kind) {
            case WITHER -> new LootTable(
                    List.of(
                            new LootItem("DIAMOND_SWORD", 1, "Gravewhisper",
                                    Map.of("smite", 5, "sharpness", 2, "looting", 2, "unbreaking", 3, "mending", 1), 0.60),
                            LootItem.plain("WITHER_ROSE", 2)),
                    List.of(
                            new BonusEntry("WITHER_ROSE", 1, 1, 40),
                            new BonusEntry("COAL", 8, 8, 30),
                            new BonusEntry("BONE_BLOCK", 2, 2, 30)));
            case ELDER_GUARDIAN -> new LootTable(
                    List.of(
                            LootItem.plain("TIDE_ARMOR_TRIM_SMITHING_TEMPLATE", 1),
                            LootItem.plain("HEART_OF_THE_SEA", 1),
                            new LootItem("TRIDENT", 1, "Monument's Answer",
                                    Map.of("impaling", 5, "loyalty", 3, "unbreaking", 3, "mending", 1), 0.50),
                            LootItem.plain("WET_SPONGE", 4)),
                    List.of(
                            new BonusEntry("PRISMARINE_CRYSTALS", 4, 4, 50),
                            new BonusEntry("WET_SPONGE", 2, 2, 30),
                            new BonusEntry("NAUTILUS_SHELL", 1, 1, 20)));
            case WARDEN -> new LootTable(
                    List.of(
                            new LootItem("DIAMOND_LEGGINGS", 1, "Hushwalkers",
                                    Map.of("protection", 4, "swift_sneak", 3, "unbreaking", 3, "mending", 1), 0.60),
                            LootItem.plain("ECHO_SHARD", 4),
                            LootItem.plain("RECOVERY_COMPASS", 1),
                            LootItem.plain("DISC_FRAGMENT_5", 3)),
                    List.of(
                            new BonusEntry("ECHO_SHARD", 1, 2, 50),
                            new BonusEntry("AMETHYST_SHARD", 4, 4, 30),
                            new BonusEntry("SCULK_CATALYST", 1, 1, 20)));
            case ENDER_DRAGON -> new LootTable(
                    List.of(
                            new LootItem("ELYTRA", 1, "First Breath",
                                    Map.of("unbreaking", 3, "mending", 1), 1.0)),
                    List.of());
        };
    }

    /** Named gear is what the fairness rule pays per champion: a
     *  signature item with a display name of its own (Gravewhisper,
     *  Monument's Answer, Hushwalkers, First Breath). Unnamed
     *  signature stacks are materials and curios - they drop
     *  once, shared. */
    public static boolean isNamedGear(LootItem item) {
        return item != null && item.name() != null;
    }

    /**
     * The grant plan for one paying signature kill: each stack
     * of the table, in table order, marked per-champion (named
     * gear) or shared (materials). The adapter executes exactly
     * this plan - named copies stamped and granted to their
     * champions, shared stacks dropped once at the kill - so the
     * plan the tests pin IS the plan the server runs.
     */
    public static List<SignatureGrant> signatureGrants(LootTable table) {
        List<SignatureGrant> out = new java.util.ArrayList<>();
        if (table == null) {
            return out;
        }
        for (LootItem item : table.signature()) {
            out.add(new SignatureGrant(item, isNamedGear(item)));
        }
        return out;
    }

    /**
     * The signature-once gate (spec 4.0 rule 1): the signature
     * pool pays when the kill CREDITS, is not an elder
     * trio-PROGRESS kill (the elder signature waits for the
     * trio-completing kill, which arrives as a full CREDIT with
     * elderProgress false), and the seal's signature has not
     * already been paid.
     */
    public static boolean paysSignature(CreditJudge.Verdict verdict, boolean elderProgress,
                                        boolean alreadyPaid) {
        return verdict == CreditJudge.Verdict.CREDIT && !elderProgress && !alreadyPaid;
    }

    /** Bonus rolls for a credit kill: one per champion beyond
     *  the first, capped at +4 (spec 4.0 rule 2). */
    public static int bonusRolls(int presentParticipants) {
        return Math.max(0, Math.min(BONUS_ROLL_CAP, presentParticipants - 1));
    }

    /**
     * Draw {@code rolls} bonus entries from the table. Each roll
     * picks one entry weighted by its weight (a roll over an
     * empty or all-zero-weight table yields nothing), then a
     * uniform count in the entry's min..max.
     */
    public static java.util.List<RolledBonus> rollBonus(LootTable table, int rolls, Random rng) {
        java.util.List<RolledBonus> out = new java.util.ArrayList<>();
        if (table == null || rolls <= 0) {
            return out;
        }
        int totalWeight = 0;
        for (BonusEntry e : table.bonus()) {
            totalWeight += e.weight();
        }
        if (totalWeight <= 0) {
            return out;
        }
        for (int i = 0; i < rolls; i++) {
            int pick = rng.nextInt(totalWeight);
            BonusEntry chosen = null;
            for (BonusEntry e : table.bonus()) {
                if (pick < e.weight()) {
                    chosen = e;
                    break;
                }
                pick -= e.weight();
            }
            if (chosen == null) {
                continue;
            }
            int count = chosen.minCount() == chosen.maxCount()
                    ? chosen.minCount()
                    : chosen.minCount() + rng.nextInt(chosen.maxCount() - chosen.minCount() + 1);
            out.add(new RolledBonus(chosen.material(), count));
        }
        return out;
    }

    /** Dragon's breath bottles for a credit kill: base + per
     *  champion beyond the first, capped (spec 4.4). */
    public static int dragonBreath(int presentParticipants) {
        int extra = Math.max(0, presentParticipants - 1);
        return Math.min(DRAGON_BREATH_CAP, DRAGON_BREATH_BASE + DRAGON_BREATH_PER_CHAMPION * extra);
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

