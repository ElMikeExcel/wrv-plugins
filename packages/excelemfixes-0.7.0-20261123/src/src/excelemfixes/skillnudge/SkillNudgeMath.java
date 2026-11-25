package excelemfixes.skillnudge;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure decision logic for the Skill Bonus Nudge module — no
 * Bukkit, no EliteMobs, no I/O, so the plain-JVM suites can pin
 * every branch.
 *
 * The EliteMobs facts this encodes (all read from the live EM
 * 10.9.10 install + jar on Nov 22, 2026; see the module's design
 * note in the staging RECORD):
 *  - A skill's bonuses unlock in four tiers at skill levels
 *    10 / 25 / 50 / 75 (EM's own menus/skill_bonus_menu.yml:
 *    "Tier 1: Level 10" ... "Tier 4: Level 75").
 *  - At most 3 bonuses can be ACTIVE per skill at once (the
 *    menu's own refusal text "You already have 3 active
 *    skills!" and the matching size >= 3 check in
 *    SkillBonusMenu's click handler bytecode).
 *  - A player therefore has something claimable in a skill when
 *    the number of enabled bonuses their level has unlocked
 *    exceeds what they have selected, up to the 3-active cap.
 *
 * "Selected" counts come from EM's SkillBonusSelections (a JSON
 * map of skill type -> list of chosen bonus ids); "unlocked"
 * counts come from EM's own skillbonuses folder (one yml per
 * bonus, carrying skillType + unlockTier + isEnabled). When the
 * catalog is unavailable the caller passes null and this class
 * falls back to the conservative floor of one bonus per
 * unlocked tier — it can only ever UNDER-count claimable picks,
 * never invent one that does not exist... with one exception:
 * a player who selected the single tier-1 bonus the fallback
 * assumes, at a level where the real folder holds more tier-1
 * bonuses, is told nothing until the catalog is readable. The
 * fallback exists so a missing folder degrades to fewer nudges,
 * not to wrong ones in the common case.
 */
public final class SkillNudgeMath {

    private SkillNudgeMath() {}

    /** Shipped tier unlock levels (EM menu config, Nov 22, 2026). */
    public static final int[] DEFAULT_TIER_LEVELS = {10, 25, 50, 75};

    /** Shipped active-bonus cap per skill (EM menu + bytecode). */
    public static final int DEFAULT_MAX_ACTIVE = 3;

    /** How many tiers a skill level has unlocked (0..tierLevels.length). */
    public static int tiersUnlocked(int level, int[] tierLevels) {
        int n = 0;
        for (int t : tierLevels) {
            if (level >= t) n++;
        }
        return n;
    }

    /**
     * Enabled bonuses of one skill that a level has unlocked.
     * tierCounts[i] = enabled bonuses whose unlockTier == i+1.
     */
    public static int unlockedCount(int level, int[] tierCounts, int[] tierLevels) {
        int n = 0;
        int tiers = Math.min(tierCounts.length, tierLevels.length);
        for (int i = 0; i < tiers; i++) {
            if (level >= tierLevels[i]) n += Math.max(0, tierCounts[i]);
        }
        return n;
    }

    /**
     * Claimable picks in one skill: room under the active cap
     * that unlocked-but-unselected bonuses could fill. Never
     * negative. When tierCounts is null the one-per-tier floor
     * stands in for the real catalog.
     */
    public static int claimable(int level, int selectedCount,
                                int[] tierCounts, int[] tierLevels,
                                int maxActive) {
        int unlocked = tierCounts == null
                ? tiersUnlocked(level, tierLevels)
                : unlockedCount(level, tierCounts, tierLevels);
        int room = Math.min(maxActive, unlocked) - Math.max(0, selectedCount);
        return Math.max(0, room);
    }

    /** One player's evaluation across every skill. */
    public record Evaluation(int totalClaimable,
                             Map<String, Integer> perSkill,
                             String topSkill) {}

    /**
     * Evaluate a player. levels / selectedCounts / catalog are
     * keyed by EM skill type name (ARMOR, SWORDS, ...). Skills
     * missing from a map count as level 0 / 0 selected / catalog
     * fallback. topSkill is the skill with the most claimable
     * picks (first in iteration order on ties), null when the
     * total is 0.
     */
    public static Evaluation evaluate(Map<String, Integer> levels,
                                      Map<String, Integer> selectedCounts,
                                      Map<String, int[]> catalog,
                                      int[] tierLevels,
                                      int maxActive) {
        Map<String, Integer> perSkill = new LinkedHashMap<>();
        int total = 0;
        String top = null;
        int topCount = 0;
        // Iterate the union of known skills, levels first so the
        // player's strongest skills win ties deterministically.
        Map<String, Integer> ordered = new LinkedHashMap<>(levels);
        for (String s : selectedCounts.keySet()) ordered.putIfAbsent(s, 0);
        if (catalog != null) {
            for (String s : catalog.keySet()) ordered.putIfAbsent(s, 0);
        }
        for (String skill : ordered.keySet()) {
            int level = levels.getOrDefault(skill, 0);
            int selected = selectedCounts.getOrDefault(skill, 0);
            int[] counts = catalog == null ? null : catalog.get(skill);
            int c = claimable(level, selected, counts, tierLevels, maxActive);
            if (c > 0) {
                perSkill.put(skill, c);
                total += c;
                if (c > topCount) {
                    topCount = c;
                    top = skill;
                }
            }
        }
        return new Evaluation(total, perSkill, top);
    }
}
