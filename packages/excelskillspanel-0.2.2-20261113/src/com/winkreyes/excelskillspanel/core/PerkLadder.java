package com.winkreyes.excelskillspanel.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The verified mcMMO perk ladders (ExcelSkillsPanel 0.2.0, menu
 * programme Item 8; decision-log D32, Nov 13, 2026).
 *
 * DATA: the embedded table below is the VERIFIED table of record
 * from ~/workspace/skillspanel-020-design-20261113/LADDER-DATA.md
 * \u00a73 \u2014 every row cross-checked at build time three ways:
 * (1) the G1-receipted live skillranks.yml (supervisor pull
 * bm278: live file VALUE-EQUAL to the live jar's embedded
 * fd7cc0f1\u2026 copy), (2) the LADDER-DATA tables parsed back out
 * of the spec document, and (3) SubSkillType numRanks in the WRV
 * fork source. 82 perks across all 19 skills; Smelting's Flux
 * Mining is OMITTED entirely (D32 \u2014 no code path exists in
 * this fork; LADDER-DATA \u00a75).
 *
 * RUNTIME VERIFICATION: this table is never rendered on its own
 * authority. The mcMMO provider re-reads every ranked perk's
 * unlock levels from the LIVE mcMMO data through the fork's own
 * surface (RankConfig.getSubSkillUnlockLevel, which reads the
 * live skillranks.yml in the server's current mode) and every
 * perk's rank count from SubSkillType.getNumRanks, and calls
 * {@link #matchesLive} per skill. Only a skill whose live values
 * match this table exactly renders its ladder; any mismatch (a
 * drifted live skillranks.yml, a mode change, a fork update)
 * fails soft to the standing website pointer line for that skill
 * alone \u2014 an unverified ladder is never rendered.
 *
 * EARNED STATE (LADDER-DATA \u00a73, RankUtils.getRank semantics):
 * at skill level L a perk's rank is the highest rank whose Retro
 * unlock \u2264 L (0 = not yet earned). A perk with numRanks = 0
 * has no ranks: RankUtils returns \u22121 and
 * hasUnlockedSubskill is true at any level, so it renders as
 * always available. For the child skills (Salvage, Smelting) L
 * is the DERIVED level the provider already reads through
 * ExperienceAPI.getLevel \u2014 the same rule, no special case.
 * Graceful Roll is a facet of Roll (no SubSkillType of its own):
 * always available with Roll.
 *
 * Rendering (D32): one line per perk \u2014 name, unlock
 * requirement, earned/rank state. NO perk descriptions in-panel;
 * they stay on the website Skill Trees page.
 *
 * 0.2.1 (menu programme Item 10, Mike, Nov 13, 2026): each perk
 * ALSO carries a short effect description, rendered as an
 * indented line under its name/unlock line on both surfaces.
 * DATA RULE (the Item 8 \u00a77.3 honesty rule, restated for
 * Item 10): every description is the fork's OWN perk text \u2014
 * the {@code <Skill>.SubSkill.<Perk>.Description} value in the
 * live mcMMO fork's locale
 * ({@code locale_en_US.properties} in the fork source, verified
 * byte-equal against the same file inside the live jar
 * {@code 4276bd3e\u2026} \u2014 all 93 {@code .Description} keys
 * compared, zero differences; per-perk key + line citations in
 * the build folder's DESCRIPTIONS-SOURCE.md). All 82 perks have
 * a verifiable locale description, so all 82 render one. A perk
 * with NO verifiable description would keep name+unlock only \u2014
 * an effect is never invented (the fallback is harness-proven
 * with a synthetic description-less perk). The one textual
 * normalization is whitespace: the properties file's {@code \n}
 * escape in Smelting's Understanding the Art renders as a space
 * so the description stays one panel line. Fork spellings are
 * otherwise verbatim (including Trick Shot's "Richochet").
 *
 * Excel \u2014 Wink Reyes Valley (Iris / Mike).
 */
public final class PerkLadder {
    private PerkLadder() {}

    /** One display perk. {@code subSkillEnum} is the SubSkillType
     *  constant name, or null for Graceful Roll (a facet of Roll
     *  with no constant of its own). {@code retroUnlocks} holds
     *  the Retro unlock level of each rank in order; empty for a
     *  numRanks = 0 perk. {@code description} is the fork locale's
     *  own effect text for the perk (0.2.1, Item 10), or null /
     *  blank when no description is verifiable \u2014 such a perk
     *  renders name+unlock only, never an invented effect. */
    public record Perk(String name, String subSkillEnum, int[] retroUnlocks,
                       String description) {
        public Perk {
            retroUnlocks = retroUnlocks == null ? new int[0] : retroUnlocks.clone();
        }
        @Override public int[] retroUnlocks() { return retroUnlocks.clone(); }
        public int numRanks() { return retroUnlocks.length; }
        /** Rank_1 unlock, or 0 for a no-rank perk. */
        public int unlock() { return retroUnlocks.length == 0 ? 0 : retroUnlocks[0]; }
        /** True when this perk carries a verifiable description. */
        public boolean hasDescription() {
            return description != null && !description.isBlank();
        }
    }

    private static Perk perk(String name, String subSkillEnum, int[] retroUnlocks,
                             String description) {
        return new Perk(name, subSkillEnum, retroUnlocks, description);
    }

    private static final Map<String, List<Perk>> LADDERS = new LinkedHashMap<>();

    static {
        LADDERS.put("acrobatics", List.of(
                perk("Roll", "ACROBATICS_ROLL", new int[0], "Land strategically to avoid damage."),
                perk("Graceful Roll", null, new int[0], "Twice as effective as a normal Roll"),
                perk("Dodge", "ACROBATICS_DODGE", new int[]{1}, "Reduce attack damage by half")));
        LADDERS.put("alchemy", List.of(
                perk("Catalysis", "ALCHEMY_CATALYSIS", new int[]{0}, "Increases potion brewing speed"),
                perk("Concoctions", "ALCHEMY_CONCOCTIONS", new int[]{0, 100, 200, 350, 500, 750, 900, 1000}, "Brew potions with more ingredients")));
        LADDERS.put("archery", List.of(
                perk("Daze", "ARCHERY_DAZE", new int[0], "Disorients foes and deals extra DMG"),
                perk("Skill Shot", "ARCHERY_SKILL_SHOT", new int[]{1, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 750, 800, 850, 900, 950, 1000}, "Increases damage done with bows"),
                perk("Arrow Retrieval", "ARCHERY_ARROW_RETRIEVAL", new int[]{1}, "Chance to retrieve arrows from corpses"),
                perk("Archery Limit Break", "ARCHERY_ARCHERY_LIMIT_BREAK", new int[]{100, 200, 300, 400, 500, 600, 700, 800, 900, 1000}, "Breaking your limits. Increased damage against tough opponents. Intended for PVP, up to server settings for whether it will boost damage in PVE.")));
        LADDERS.put("axes", List.of(
                perk("Critical Strikes", "AXES_CRITICAL_STRIKES", new int[]{1}, "Double Damage"),
                perk("Armor Impact", "AXES_ARMOR_IMPACT", new int[]{1, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 750, 800, 850, 900, 950, 1000}, "Strike with enough force to shatter armor"),
                perk("Axe Mastery", "AXES_AXE_MASTERY", new int[]{50, 100, 150, 200}, "Adds bonus DMG"),
                perk("Skull Splitter", "AXES_SKULL_SPLITTER", new int[]{50}, "Deal AoE Damage"),
                perk("Axes Limit Break", "AXES_AXES_LIMIT_BREAK", new int[]{100, 200, 300, 400, 500, 600, 700, 800, 900, 1000}, "Breaking your limits. Increased damage against tough opponents. Intended for PVP, up to server settings for whether it will boost damage in PVE."),
                perk("Greater Impact", "AXES_GREATER_IMPACT", new int[]{250}, "Deal bonus damage to unarmored foes")));
        LADDERS.put("crossbows", List.of(
                perk("Powered Shot", "CROSSBOWS_POWERED_SHOT", new int[]{1, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 750, 800, 850, 900, 950, 1000}, "Increases damage done with crossbows"),
                perk("Trick Shot", "CROSSBOWS_TRICK_SHOT", new int[]{50, 200, 400}, "Richochet arrows with steep angles"),
                perk("Crossbows Limit Break", "CROSSBOWS_CROSSBOWS_LIMIT_BREAK", new int[]{100, 200, 300, 400, 500, 600, 700, 800, 900, 1000}, "Breaking your limits. Increased damage against tough opponents. Intended for PVP, up to server settings for whether it will boost damage in PVE.")));
        LADDERS.put("excavation", List.of(
                perk("Archaeology", "EXCAVATION_ARCHAEOLOGY", new int[]{1, 250, 350, 500, 650, 750, 850, 1000}, "Unearth the secrets of the land! High skill levels increase your odds of finding experience orbs when you find treasure!"),
                perk("Giga Drill Breaker", "EXCAVATION_GIGA_DRILL_BREAKER", new int[]{50}, "3x Drop Rate, 3x EXP, +Speed")));
        LADDERS.put("fishing", List.of(
                perk("Treasure Hunter", "FISHING_TREASURE_HUNTER", new int[]{1, 250, 350, 500, 650, 750, 850, 1000}, "Fish up misc. objects"),
                perk("Master Angler", "FISHING_MASTER_ANGLER", new int[]{1, 200, 300, 400, 600, 700, 800, 900}, "Fish are caught more frequently, works better when fishing from a boat."),
                perk("Ice Fishing", "FISHING_ICE_FISHING", new int[]{50}, "Allows you to fish in icy biomes"),
                perk("Shake", "FISHING_SHAKE", new int[]{150, 200, 250, 300, 400, 500, 600, 700}, "Shake items off of mobs or players w/ fishing pole"),
                perk("Magic Hunter", "FISHING_MAGIC_HUNTER", new int[]{200}, "Find Enchanted Items"),
                perk("Fisherman's Diet", "FISHING_FISHERMANS_DIET", new int[]{200, 400, 600, 800, 1000}, "Improves hunger restored from fished foods")));
        LADDERS.put("herbalism", List.of(
                perk("Hylian Luck", "HERBALISM_HYLIAN_LUCK", new int[0], "Gives a small chance of finding rare items"),
                perk("Shroom Thumb", "HERBALISM_SHROOM_THUMB", new int[0], "Spread mycelium to dirt & grass"),
                perk("Double Drops", "HERBALISM_DOUBLE_DROPS", new int[]{1}, "Skillfully harvest double the loot"),
                perk("Green Terra", "HERBALISM_GREEN_TERRA", new int[]{50}, "Spread the Terra, 3x Drops, Boosts Green Thumb"),
                perk("Farmer's Diet", "HERBALISM_FARMERS_DIET", new int[]{200, 400, 600, 800, 1000}, "Improves hunger restored from farmed foods"),
                perk("Green Thumb", "HERBALISM_GREEN_THUMB", new int[]{250, 500, 750, 1000}, "Auto-Plants crops when harvesting with hoe"),
                perk("Verdant Bounty", "HERBALISM_VERDANT_BOUNTY", new int[]{1000}, "Masterfully harvest triple the loot")));
        LADDERS.put("maces", List.of(
                perk("Cripple", "MACES_CRIPPLE", new int[]{50, 200, 400, 800}, "Adds a chance to cripple your target."),
                perk("Crush", "MACES_CRUSH", new int[]{100, 250, 750, 900}, "Adds bonus damage to your attacks."),
                perk("Maces Limit Break", "MACES_MACES_LIMIT_BREAK", new int[]{100, 200, 300, 400, 500, 600, 700, 800, 900, 1000}, "Breaking your limits. Increased damage against tough opponents. Intended for PVP, up to server settings for whether it will boost damage in PVE.")));
        LADDERS.put("mining", List.of(
                perk("Double Drops", "MINING_DOUBLE_DROPS", new int[]{1}, "Skillfully mine double the loot"),
                perk("Super Breaker", "MINING_SUPER_BREAKER", new int[]{50}, "Speed+, Triple Drop Chance"),
                perk("Blast Mining", "MINING_BLAST_MINING", new int[]{100, 250, 350, 500, 650, 750, 850, 1000}, "Bonuses to mining with TNT"),
                perk("Bigger Bombs", "MINING_BIGGER_BOMBS", new int[]{100}, "Increases TNT explosion radius"),
                perk("Demolitions Expertise", "MINING_DEMOLITIONS_EXPERTISE", new int[]{100}, "Decreases damage from TNT explosions"),
                perk("Mother Lode", "MINING_MOTHER_LODE", new int[]{1000}, "Masterfully mine triple the loot")));
        LADDERS.put("repair", List.of(
                perk("Repair Mastery", "REPAIR_REPAIR_MASTERY", new int[]{1}, "Increased repair amount"),
                perk("Arcane Forging", "REPAIR_ARCANE_FORGING", new int[]{100, 250, 350, 500, 650, 750, 850, 1000}, "Repair magic items"),
                perk("Super Repair", "REPAIR_SUPER_REPAIR", new int[]{400}, "Double effectiveness")));
        LADDERS.put("salvage", List.of(
                perk("Scrap Collector", "SALVAGE_SCRAP_COLLECTOR", new int[]{1, 100, 150, 200, 250, 300, 350, 400}, "Salvage materials from an item, a perfect salvage depends on skill and luck."),
                perk("Arcane Salvage", "SALVAGE_ARCANE_SALVAGE", new int[]{100, 250, 350, 500, 650, 750, 850, 1000}, "Extract enchantments from items")));
        LADDERS.put("smelting", List.of(
                perk("Second Smelt", "SMELTING_SECOND_SMELT", new int[0], "Double the resources gained from smelting"),
                perk("Fuel Efficiency", "SMELTING_FUEL_EFFICIENCY", new int[]{100, 500, 750}, "Increase the burn time of fuel used in furnaces when smelting"),
                perk("Understanding the Art", "SMELTING_UNDERSTANDING_THE_ART", new int[]{100, 250, 350, 500, 650, 750, 850, 1000}, "Maybe you're spending a bit too much time smelting in the caves. Powers up various properties of Smelting.")));
        LADDERS.put("spears", List.of(
                perk("Momentum", "SPEARS_MOMENTUM", new int[]{1, 100, 150, 200, 250, 400, 500, 600, 800, 950}, "Adds a chance to increase movement speed for a short duration when attacking."),
                perk("Spear Mastery", "SPEARS_SPEAR_MASTERY", new int[]{50, 150, 300, 450, 600, 750, 900, 1000}, "Adds bonus damage to your attacks."),
                perk("Spears Limit Break", "SPEARS_SPEARS_LIMIT_BREAK", new int[]{100, 200, 300, 400, 500, 600, 700, 800, 900, 1000}, "Breaking your limits. Increased damage against tough opponents. Intended for PVP, up to server settings for whether it will boost damage in PVE.")));
        LADDERS.put("swords", List.of(
                perk("Rupture", "SWORDS_RUPTURE", new int[]{1, 150, 750, 900}, "A damage over time effect that ends explosively"),
                perk("Serrated Strikes", "SWORDS_SERRATED_STRIKES", new int[]{50}, "Deal partial damage in an AOE with a chance to apply Rupture!"),
                perk("Swords Limit Break", "SWORDS_SWORDS_LIMIT_BREAK", new int[]{100, 200, 300, 400, 500, 600, 700, 800, 900, 1000}, "Breaking your limits. Increased damage against tough opponents. Intended for PVP, up to server settings for whether it will boost damage in PVE."),
                perk("Counter Attack", "SWORDS_COUNTER_ATTACK", new int[]{200}, "Reflect a portion of damage when attacked!"),
                perk("Stab", "SWORDS_STAB", new int[]{750, 1000}, "Adds bonus damage to your attacks.")));
        LADDERS.put("taming", List.of(
                perk("Beast Lore", "TAMING_BEAST_LORE", new int[]{1}, "Bone-whacking inspects wolves & ocelots"),
                perk("Call of the Wild", "TAMING_CALL_OF_THE_WILD", new int[]{1}, "Summon an animal to your side"),
                perk("Environmentally Aware", "TAMING_ENVIRONMENTALLY_AWARE", new int[]{100}, "Cactus/Lava Phobia, Fall DMG Immune"),
                perk("Gore", "TAMING_GORE", new int[]{150}, "Critical Strike that applies Rupture"),
                perk("Fast Food Service", "TAMING_FAST_FOOD_SERVICE", new int[]{200}, "Chance for wolves to heal on attack"),
                perk("Pummel", "TAMING_PUMMEL", new int[]{200}, "Your Wolves have a chance of knocking back foes"),
                perk("Thick Fur", "TAMING_THICK_FUR", new int[]{250}, "DMG Reduction, Fire Resistance"),
                perk("Holy Hound", "TAMING_HOLY_HOUND", new int[]{350}, "Healed by Magic & Poison"),
                perk("Shock Proof", "TAMING_SHOCK_PROOF", new int[]{500}, "Explosive Damage Reduction"),
                perk("Sharpened Claws", "TAMING_SHARPENED_CLAWS", new int[]{750}, "Damage Bonus")));
        LADDERS.put("tridents", List.of(
                perk("Impale", "TRIDENTS_IMPALE", new int[]{50, 150, 250, 350, 450, 550, 650, 750, 850, 1000}, "Increases damage done with tridents"),
                perk("Tridents Limit Break", "TRIDENTS_TRIDENTS_LIMIT_BREAK", new int[]{100, 200, 300, 400, 500, 600, 700, 800, 900, 1000}, "Breaking your limits. Increased damage against tough opponents. Intended for PVP, up to server settings for whether it will boost damage in PVE.")));
        LADDERS.put("unarmed", List.of(
                perk("Block Cracker", "UNARMED_BLOCK_CRACKER", new int[0], "Break rock with your fists"),
                perk("Steel Arm Style", "UNARMED_STEEL_ARM_STYLE", new int[]{1, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 750, 800, 850, 900, 950, 1000}, "Hardens your arm over time"),
                perk("Berserk", "UNARMED_BERSERK", new int[]{50}, "+50% DMG, Breaks weak materials"),
                perk("Unarmed Limit Break", "UNARMED_UNARMED_LIMIT_BREAK", new int[]{100, 200, 300, 400, 500, 600, 700, 800, 900, 1000}, "Breaking your limits. Increased damage against tough opponents. Intended for PVP, up to server settings for whether it will boost damage in PVE."),
                perk("Arrow Deflect", "UNARMED_ARROW_DEFLECT", new int[]{200}, "Deflect arrows"),
                perk("Disarm", "UNARMED_DISARM", new int[]{250}, "Drops the foes item held in hand"),
                perk("Iron Grip", "UNARMED_IRON_GRIP", new int[]{600}, "Prevents you from being disarmed")));
        LADDERS.put("woodcutting", List.of(
                perk("Harvest Lumber", "WOODCUTTING_HARVEST_LUMBER", new int[]{1}, "Skillfully extract up to double the Lumber"),
                perk("Tree Feller", "WOODCUTTING_TREE_FELLER", new int[]{50}, "Make trees explode"),
                perk("Leaf Blower", "WOODCUTTING_LEAF_BLOWER", new int[]{150}, "Blow Away Leaves"),
                perk("Knock on Wood", "WOODCUTTING_KNOCK_ON_WOOD", new int[]{300, 600}, "Find additional goodies when using Tree Feller"),
                perk("Clean Cuts", "WOODCUTTING_CLEAN_CUTS", new int[]{1000}, "Masterfully extract up to triple the Lumber")));
    }

    /** The verified ladder for a skill id (the provider's
     *  lowercase PrimarySkillType name), or an empty list when
     *  the panel has no verified ladder for it. */
    public static List<Perk> perksFor(String skillId) {
        List<Perk> perks = LADDERS.get(skillId);
        return perks == null ? List.of() : perks;
    }

    /** Every skill id that carries a verified ladder. */
    public static List<String> skillIds() {
        return List.copyOf(LADDERS.keySet());
    }

    /** Total display perks across all ladders (82 in 0.2.0). */
    public static int totalPerks() {
        int n = 0;
        for (List<Perk> perks : LADDERS.values()) n += perks.size();
        return n;
    }

    /** RankUtils.getRank semantics, pure: the highest rank whose
     *  unlock \u2264 {@code level}; \u22121 for a numRanks = 0
     *  perk (mcMMO's "no unlockable levels, therefore unlocked"
     *  sentinel); 0 when no rank's unlock is met yet. */
    public static int rankAt(Perk perk, int level) {
        int[] unlocks = perk.retroUnlocks();
        if (unlocks.length == 0) return -1;
        for (int rank = unlocks.length; rank >= 1; rank--) {
            if (level >= unlocks[rank - 1]) return rank;
        }
        return 0;
    }

    /** Earned at this level: rank \u2265 1, or the numRanks = 0
     *  always-unlocked sentinel. */
    public static boolean earnedAt(Perk perk, int level) {
        int rank = rankAt(perk, level);
        return rank == -1 || rank >= 1;
    }

    /** One ladder line: marker + name + unlock + rank state.
     *  ASCII "%"-free by construction; the percent handling of
     *  the surrounding detail view is unchanged (0.1.1 rules). */
    public static String line(Perk perk, int level) {
        int rank = rankAt(perk, level);
        if (rank == -1) {
            return "\u2713 " + perk.name() + " \u2014 always available";
        }
        if (rank == 0) {
            return "\u2717 " + perk.name() + " \u2014 Lv " + perk.unlock()
                    + " \u00b7 Rank 0 of " + perk.numRanks();
        }
        return "\u2713 " + perk.name() + " \u2014 Lv " + perk.unlock()
                + " \u00b7 Rank " + rank + " of " + perk.numRanks();
    }

    /** The description line for one perk (0.2.1, Item 10): the
     *  fork locale's effect text, indented two spaces so it reads
     *  as the sub-line of the perk's name/unlock line above it.
     *  Null when the perk carries no verifiable description
     *  \u2014 the Item 10 fallback: name+unlock only, never an
     *  invented effect. */
    public static String descriptionLine(Perk perk) {
        if (perk == null || !perk.hasDescription()) return null;
        return "  " + perk.description().strip();
    }

    /** The ladder lines for an explicit perk list at one level:
     *  one name/unlock line per perk, each followed by its
     *  description line when it has one. Split out from
     *  {@link #ladderLines} so the harness can prove the
     *  no-description fallback on a synthetic perk. */
    public static List<String> linesFor(List<Perk> perks, int level) {
        List<String> out = new java.util.ArrayList<>();
        for (Perk perk : perks) {
            out.add(line(perk, level));
            String desc = descriptionLine(perk);
            if (desc != null) out.add(desc);
        }
        return List.copyOf(out);
    }

    /** The detail-view ladder block for one skill at one level:
     *  a "Perks:" header plus, per perk, its name/unlock line
     *  and (0.2.1) its description line when verifiable, in
     *  ladder order. */
    public static List<String> ladderLines(String skillId, int level) {
        List<Perk> perks = perksFor(skillId);
        if (perks.isEmpty()) return List.of();
        List<String> out = new java.util.ArrayList<>();
        out.add("Perks:");
        out.addAll(linesFor(perks, level));
        return List.copyOf(out);
    }

    /**
     * The runtime verification gate: does the LIVE mcMMO data for
     * this skill match the verified table exactly?
     *
     * @param liveUnlocksByEnum per-SubSkillType-constant live
     *        state, as read by the provider from the fork's own
     *        surfaces: the constant's name maps to its live
     *        unlock levels in rank order (an empty array for a
     *        numRanks = 0 constant). A constant ABSENT from the
     *        map means its live read failed or the constant does
     *        not exist in the live fork \u2014 unverified.
     * @return true only when every perk in the verified ladder is
     *         present live with exactly the same rank count and
     *         unlock levels. Graceful Roll (no constant) is
     *         verified by its parent perk Roll being verified.
     */
    public static boolean matchesLive(String skillId, Map<String, int[]> liveUnlocksByEnum) {
        List<Perk> perks = perksFor(skillId);
        if (perks.isEmpty() || liveUnlocksByEnum == null) return false;
        for (Perk perk : perks) {
            if (perk.subSkillEnum() == null) continue;
            int[] live = liveUnlocksByEnum.get(perk.subSkillEnum());
            if (live == null) return false;
            if (!java.util.Arrays.equals(live, perk.retroUnlocks())) return false;
        }
        return true;
    }
}
