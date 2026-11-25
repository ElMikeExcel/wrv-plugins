package excelemfixes.mount.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grouping EliteMobs quest files into content packages, as a
 * pure decision (unit-testable, no Bukkit, no EliteMobs types).
 *
 * <p>A quest's identity is its bare config filename (for example
 * {@code primis_main_quest_1.yml}); EliteMobs stores a player's
 * completions under the lockout permission
 * {@code elitequest.<filename>} in its quest-cooldown registry,
 * so filenames are the join key between "what a package
 * contains" and "what a player has finished".
 *
 * <p>Packages are defined by filename-prefix rules from config
 * (first matching rule wins), so a new content package enrolls
 * by config alone: count the package's enabled quest files,
 * count how many of those same files the player has completed,
 * and the gate math (see {@link QuestGateMath}) compares the
 * two. Files matching no rule belong to no package — guild-hub
 * one-offs, seasonal quests and test quests never inflate a
 * package's denominator.
 */
public final class QuestCensus {

    private QuestCensus() {}

    /** One content package: an id plus the filename prefixes
     *  that belong to it (for example id "primis", prefix
     *  "primis_"). */
    public record PackageRule(String id, List<String> prefixes) {}

    /** The package a quest filename belongs to, or null when no
     *  rule claims it. First matching rule wins. */
    public static String packageOf(String filename,
                                     List<PackageRule> rules) {
        if (filename == null || rules == null) return null;
        for (PackageRule rule : rules) {
            if (rule == null || rule.prefixes() == null) continue;
            for (String prefix : rule.prefixes()) {
                if (prefix != null && !prefix.isEmpty()
                        && filename.startsWith(prefix)) {
                    return rule.id();
                }
            }
        }
        return null;
    }

    /** Enabled quest files per package (the denominators). Every
     *  configured package appears, even at zero, so a package
     *  whose content is missing reads as an honest zero. */
    public static Map<String, Integer> totals(
            Collection<String> enabledFilenames,
            List<PackageRule> rules) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (rules != null) {
            for (PackageRule rule : rules) {
                if (rule != null && rule.id() != null) {
                    out.putIfAbsent(rule.id(), 0);
                }
            }
        }
        if (enabledFilenames == null) return out;
        for (String filename : enabledFilenames) {
            String pkg = packageOf(filename, rules);
            if (pkg != null) out.merge(pkg, 1, Integer::sum);
        }
        return out;
    }

    /** Completed quest files per package: a completion counts
     *  only when the quest is in the package's enabled set, so a
     *  completion for a quest that no longer exists (or was never
     *  part of the package) never inflates the numerator. */
    public static Map<String, Integer> completed(
            Collection<String> enabledFilenames,
            Set<String> completedFilenames,
            List<PackageRule> rules) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (rules != null) {
            for (PackageRule rule : rules) {
                if (rule != null && rule.id() != null) {
                    out.putIfAbsent(rule.id(), 0);
                }
            }
        }
        if (enabledFilenames == null || completedFilenames == null) {
            return out;
        }
        for (String filename : enabledFilenames) {
            if (!completedFilenames.contains(filename)) continue;
            String pkg = packageOf(filename, rules);
            if (pkg != null) out.merge(pkg, 1, Integer::sum);
        }
        return out;
    }
}
