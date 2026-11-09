package com.winkreyes.excelmagicgate;

import java.util.List;
import java.util.Map;

/**
 * Per-stage announcement content (pure, no Bukkit deps).
 *
 * Defaults come from the WRV-070 five-seal roadmap (ROADMAP.md): the Nth
 * fallen seal opens the Nth beat. The plugin's config.yml can override
 * any field per stage; resolve() merges config scalars/lists over these
 * defaults field by field, so a missing or blank override never blanks
 * out an announcement. Placeholders {stage}, {name} and {unlocks} are
 * substituted by the plugin at announce time. Color codes use the
 * legacy '&' convention, translated by the plugin.
 */
public final class StageMessages {

    public record StageMessage(String name, String title, String subtitle,
                               String unlocks, List<String> broadcast) {}

    private StageMessages() {}

    /** The built-in announcement for a stage, or null for stage 0 / out of range. */
    public static StageMessage defaultsFor(int stage) {
        return switch (stage) {
            case 1 -> new StageMessage(
                    "The Healer",
                    "&6&lThe Healer",
                    "&eMagic returns to the valley",
                    "Heal, Cure, Blessing, Boon and Storm Shackle",
                    List.of(
                            "&6— Magic returns: &fThe Healer &6—",
                            "&eStage {stage} of 5 is open. New magic: &f{unlocks}&e.",
                            "&7Climb the healer ladder on the website's Skill Trees page."));
            case 2 -> new StageMessage(
                    "The Pathfinder",
                    "&6&lThe Pathfinder",
                    "&eThe valley opens up",
                    "Blink, Glide, Lift, and light and vision spells",
                    List.of(
                            "&6— Magic grows: &fThe Pathfinder &6—",
                            "&eStage {stage} of 5 is open. New magic: &f{unlocks}&e.",
                            "&7See the pathfinder ladder on the website's Skill Trees page."));
            case 3 -> new StageMessage(
                    "The Wonder",
                    "&6&lThe Wonder",
                    "&eThe storm answers",
                    "Lightning and Chain Lightning at full strength",
                    List.of(
                            "&6— Magic surges: &fThe Wonder &6—",
                            "&eStage {stage} of 5 is open. New magic: &f{unlocks}&e.",
                            "&7See the wonder ladder on the website's Skill Trees page."));
            case 4 -> new StageMessage(
                    "The Masons",
                    "&6&lThe Masons",
                    "&eStone and timber obey",
                    "the construction school, on builder wands",
                    List.of(
                            "&6— Magic builds: &fThe Masons &6—",
                            "&eStage {stage} of 5 is open. New magic: &f{unlocks}&e.",
                            "&7See the masons ladder on the website's Skill Trees page."));
            case 5 -> new StageMessage(
                    "The Guild Opens, the Schools Open",
                    "&6&lThe Schools Open",
                    "&eEvery seal is broken",
                    "master ranks and the remaining schools",
                    List.of(
                            "&6— Magic unleashed: &fThe Guild Opens, the Schools Open &6—",
                            "&eStage {stage} of 5 is open. New magic: &f{unlocks}&e.",
                            "&7The full ladders are on the website's Skill Trees page."));
            default -> null;
        };
    }

    /**
     * Merge config overrides over the defaults for a stage.
     * scalars keys: name, title, subtitle, unlocks (blank = keep default).
     * lists key "broadcast": a non-empty list replaces the default lines.
     * Returns null when the stage has no announcement (stage 0).
     */
    public static StageMessage resolve(int stage, Map<String, String> scalars,
                                       Map<String, List<String>> lists) {
        StageMessage base = defaultsFor(stage);
        if (base == null) {
            return null;
        }
        if (scalars == null && lists == null) {
            return base;
        }
        String name = pick(scalars, "name", base.name());
        String title = pick(scalars, "title", base.title());
        String subtitle = pick(scalars, "subtitle", base.subtitle());
        String unlocks = pick(scalars, "unlocks", base.unlocks());
        List<String> broadcast = base.broadcast();
        if (lists != null) {
            List<String> over = lists.get("broadcast");
            if (over != null && !over.isEmpty()) {
                broadcast = List.copyOf(over);
            }
        }
        return new StageMessage(name, title, subtitle, unlocks, broadcast);
    }

    private static String pick(Map<String, String> scalars, String key, String fallback) {
        if (scalars == null) {
            return fallback;
        }
        String v = scalars.get(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
