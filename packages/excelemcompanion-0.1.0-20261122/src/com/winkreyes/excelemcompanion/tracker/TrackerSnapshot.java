package com.winkreyes.excelemcompanion.tracker;

import java.util.List;
import java.util.Set;

/**
 * One player's progress, read from EliteMobs' own data (EmData) —
 * pure data, so menus and tests never touch the EM API.
 *
 * Honesty rules (verified data semantics, Nov 21, 2026):
 * - questCompletions / dungeonRuns are TOTAL counters: repeats
 *   count, and the menus label them as completions / runs.
 * - distinctQuests comes from the quest-cooldown registry (quests
 *   finished at least once) — a different number, labeled as such.
 * - lockoutBossFiles are the boss files in the player's dungeon
 *   boss-lockout list: proof a boss was downed, not a count.
 */
public record TrackerSnapshot(
        boolean available,
        int questCompletions,
        Set<String> distinctQuests,
        List<String> activeQuestFiles,
        int dungeonRuns,
        Set<String> lockoutBossFiles,
        int kills,
        int highestKilled,
        int deaths,
        int playerLevel,
        List<FormProgress> forms,
        List<SkillProgress> skills) {

    public record FormProgress(String formId, long xp, int level,
                               boolean selected) {}

    public record SkillProgress(String type, long xp, int level) {}

    public TrackerSnapshot {
        distinctQuests = distinctQuests == null
                ? Set.of() : Set.copyOf(distinctQuests);
        activeQuestFiles = activeQuestFiles == null
                ? List.of() : List.copyOf(activeQuestFiles);
        lockoutBossFiles = lockoutBossFiles == null
                ? Set.of() : Set.copyOf(lockoutBossFiles);
        forms = forms == null ? List.of() : List.copyOf(forms);
        skills = skills == null ? List.of() : List.copyOf(skills);
    }

    public static TrackerSnapshot unavailable() {
        return new TrackerSnapshot(false, 0, Set.of(), List.of(), 0,
                Set.of(), 0, 0, 0, 0, List.of(), List.of());
    }
}
