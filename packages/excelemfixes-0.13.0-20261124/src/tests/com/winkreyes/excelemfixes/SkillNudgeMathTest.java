import excelemfixes.skillnudge.SkillNudgeMath;

import java.util.Map;

/**
 * Skill Bonus Nudge decision truth table. The catalog counts
 * and player shapes are the LIVE ones read Nov 22, 2026:
 * ARMOR bonuses per tier [3,2,3,2], WANDS [3,3,2,2] (enabled
 * files in EliteMobs' skillbonuses folder); "Jester-shaped" =
 * ARMOR level 14 + WANDS level 17 with zero selections
 * anywhere; "Marvin-shaped" = three selections in each of
 * ARMOR / SWORDS / STAVES.
 */
public final class SkillNudgeMathTest {

    private static final int[] TIERS = SkillNudgeMath.DEFAULT_TIER_LEVELS;
    private static final int CAP = SkillNudgeMath.DEFAULT_MAX_ACTIVE;
    private static final int[] ARMOR = {3, 2, 3, 2};
    private static final int[] WANDS = {3, 3, 2, 2};

    public static void run() {
        System.out.println("SkillNudgeMathTest");

        // Tier boundaries (EM menu: tiers at 10 / 25 / 50 / 75).
        Check.eq(0, SkillNudgeMath.tiersUnlocked(9, TIERS), "level 9 unlocks no tier");
        Check.eq(1, SkillNudgeMath.tiersUnlocked(10, TIERS), "level 10 unlocks tier 1");
        Check.eq(1, SkillNudgeMath.tiersUnlocked(24, TIERS), "level 24 still tier 1 only");
        Check.eq(2, SkillNudgeMath.tiersUnlocked(25, TIERS), "level 25 unlocks tier 2");
        Check.eq(3, SkillNudgeMath.tiersUnlocked(50, TIERS), "level 50 unlocks tier 3");
        Check.eq(4, SkillNudgeMath.tiersUnlocked(75, TIERS), "level 75 unlocks tier 4");
        Check.eq(4, SkillNudgeMath.tiersUnlocked(150, TIERS), "level 150 stays 4 tiers");

        // Unlocked counts against the real catalog.
        Check.eq(0, SkillNudgeMath.unlockedCount(9, ARMOR, TIERS), "armor level 9: nothing unlocked");
        Check.eq(3, SkillNudgeMath.unlockedCount(14, ARMOR, TIERS), "armor level 14: 3 tier-1 bonuses");
        Check.eq(5, SkillNudgeMath.unlockedCount(25, ARMOR, TIERS), "armor level 25: tiers 1+2 = 5");
        Check.eq(10, SkillNudgeMath.unlockedCount(75, ARMOR, TIERS), "armor level 75: all 10");

        // Claimable = room under the 3-active cap.
        Check.eq(3, SkillNudgeMath.claimable(14, 0, ARMOR, TIERS, CAP),
                "armor 14, none selected: 3 claimable (cap)");
        Check.eq(1, SkillNudgeMath.claimable(14, 2, ARMOR, TIERS, CAP),
                "armor 14, two selected: 1 claimable");
        Check.eq(0, SkillNudgeMath.claimable(14, 3, ARMOR, TIERS, CAP),
                "armor 14, three selected: cap reached, 0 claimable");
        Check.eq(0, SkillNudgeMath.claimable(9, 0, ARMOR, TIERS, CAP),
                "armor 9, none selected: nothing unlocked yet");
        Check.eq(0, SkillNudgeMath.claimable(14, 9, ARMOR, TIERS, CAP),
                "over-selected never goes negative");

        // Fallback (no catalog): one bonus per unlocked tier.
        Check.eq(1, SkillNudgeMath.claimable(14, 0, null, TIERS, CAP),
                "fallback armor 14, none selected: 1 claimable");
        Check.eq(0, SkillNudgeMath.claimable(14, 1, null, TIERS, CAP),
                "fallback armor 14, one selected: 0 claimable");
        Check.eq(2, SkillNudgeMath.claimable(30, 0, null, TIERS, CAP),
                "fallback level 30: two tiers = 2 claimable");
        Check.eq(3, SkillNudgeMath.claimable(80, 0, null, TIERS, CAP),
                "fallback level 80: capped at 3");

        // Jester-shaped: the live case that motivated the module.
        SkillNudgeMath.Evaluation jester = SkillNudgeMath.evaluate(
                Map.of("ARMOR", 14, "WANDS", 17),
                Map.of(),
                Map.of("ARMOR", ARMOR, "WANDS", WANDS),
                TIERS, CAP);
        Check.eq(6, jester.totalClaimable(), "Jester-shaped: 6 claimable across armor + wands");
        Check.eq("ARMOR", jester.topSkill(), "Jester-shaped: armor named first on the tie");

        // Marvin-shaped: everything he qualifies for is claimed.
        SkillNudgeMath.Evaluation marvin = SkillNudgeMath.evaluate(
                Map.of("ARMOR", 10, "SWORDS", 12, "STAVES", 11),
                Map.of("ARMOR", 3, "SWORDS", 3, "STAVES", 3),
                Map.of("ARMOR", ARMOR, "WANDS", WANDS,
                        "SWORDS", new int[]{3, 3, 3, 1}, "STAVES", new int[]{3, 3, 2, 2}),
                TIERS, CAP);
        Check.eq(0, marvin.totalClaimable(), "Marvin-shaped: 0 claimable — no nudge");
        Check.eq(null, marvin.topSkill(), "Marvin-shaped: no top skill");

        // A brand-new player: no levels, no selections, no nudge.
        SkillNudgeMath.Evaluation fresh = SkillNudgeMath.evaluate(
                Map.of("ARMOR", 1), Map.of(), Map.of("ARMOR", ARMOR), TIERS, CAP);
        Check.eq(0, fresh.totalClaimable(), "fresh player: 0 claimable");

        // Partial claim in one skill only.
        SkillNudgeMath.Evaluation partial = SkillNudgeMath.evaluate(
                Map.of("ARMOR", 14, "WANDS", 17),
                Map.of("ARMOR", 3),
                Map.of("ARMOR", ARMOR, "WANDS", WANDS),
                TIERS, CAP);
        Check.eq(3, partial.totalClaimable(), "armor full, wands open: 3 claimable");
        Check.eq("WANDS", partial.topSkill(), "armor full, wands open: wands is top skill");
    }
}
