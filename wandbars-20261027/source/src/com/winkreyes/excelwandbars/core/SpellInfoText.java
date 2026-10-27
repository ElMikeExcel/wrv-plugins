package com.winkreyes.excelwandbars.core;

import java.util.ArrayList;
import java.util.List;

/** WoW-style tooltip text for a spell, built from SpellFacts. Plain text, no em dashes. */
public final class SpellInfoText {
    private SpellInfoText() {}

    public static String rankText(SpellFacts f) {
        return f.maxRank > 1 ? "Rank " + f.rank + " of " + f.maxRank : "Rank " + f.rank;
    }

    /** Button label in the spell list: "Mend (Rank 1)". */
    public static String button(SpellFacts f) {
        return f.name + " (" + rankText(f) + ")";
    }

    public static String costText(SpellFacts f) {
        StringBuilder sb = new StringBuilder();
        if (f.costs.isEmpty()) sb.append("Free");
        else sb.append(String.join(", ", f.costs));
        if (!f.activeCosts.isEmpty()) {
            sb.append(f.costs.isEmpty() ? " to start, then " : " to start, then ");
            sb.append(String.join(", ", f.activeCosts)).append(" per sec");
        }
        return sb.toString();
    }

    public static String rangeText(SpellFacts f) {
        if (f.selfOnly) return "Self";
        if (f.range <= 0) return "";
        return Durations.number(f.range) + " blocks";
    }

    /** One compact line, e.g. "Mend: 2 Mana | 1 sec cooldown | 16 blocks". */
    public static String summary(SpellFacts f) {
        StringBuilder sb = new StringBuilder(f.name).append(": ").append(costText(f));
        if (f.cooldownMs > 0) sb.append(" | ").append(Durations.ms(f.cooldownMs)).append(" cooldown");
        String r = rangeText(f);
        if (!r.isEmpty()) sb.append(" | ").append(r);
        return sb.toString();
    }

    /** Max characters in a generated line (Bedrock forms on phones, item lore). */
    public static final int LINE = 44;

    /** "Cooldown: none, just a 1 sec pause" style line(s). */
    static List<String> cooldownLines(SpellFacts f) {
        List<String> out = new ArrayList<>();
        if (f.cooldownMs > 0) {
            out.add("Cooldown: " + Durations.ms(f.cooldownMs));
            if (f.mageCooldownMs > 0) out.add("Other spells wait " + Durations.ms(f.mageCooldownMs) + " after it");
        } else if (f.mageCooldownMs > 0) {
            out.add("Cooldown: none, just a " + Durations.ms(f.mageCooldownMs) + " pause");
        } else {
            out.add("Cooldown: none");
        }
        return out;
    }

    /**
     * Full tooltip, one fact per line. Spell info 2026-10-26: the real effects
     * (hearts, effects, targets, radius) from the spell's config, an accurate
     * cooldown, and on rank wands what the next rank changes and how to get it.
     * Nothing here ever mentions seals.
     */
    public static List<String> lines(SpellFacts f) {
        List<String> out = new ArrayList<>();
        out.add(f.name + "  (" + rankText(f) + ")");
        if (f.description != null && !f.description.isBlank()) out.add(f.description.trim());
        for (String e : f.effects) out.addAll(SpellEffects.wrap(e, LINE));
        out.add("Cost: " + costText(f));
        if (f.toggle) out.add("Toggle: cast again to stop. Ends when your mana runs out.");
        if (f.stopsManaRegen) out.add("Mana does not refill while it is on.");
        out.addAll(cooldownLines(f));
        String r = rangeText(f);
        if (!r.isEmpty()) out.add("Range: " + r);
        if (f.durationMs > 0) out.add("Lasts: " + Durations.ms(f.durationMs));
        if (f.bar > 0) out.add("On bar " + f.bar);
        if (f.rank < f.maxRank) {
            // 0.2.2: always the count so far ("12 of 40 casts"), and the rank note only
            // when the wand's rank does not allow the next spell rank yet.
            StringBuilder n = new StringBuilder("Next rank: ");
            if (f.rankByWand) {
                // The wand has no Magic path: casting never ranks this spell up, the wand's next rank does.
                if (f.nextWandRank != null && !f.nextWandRank.isBlank() && f.nextWandRankLevel > f.rank) {
                    out.add("Next rank: Rank " + f.nextWandRankLevel + " at " + f.nextWandRank + " rank");
                    if (!f.nextChanges.isEmpty()) {
                        out.add("Rank " + f.nextWandRankLevel + " changes:");
                        for (String c : f.nextChanges) for (String w : SpellEffects.wrap(c, LINE - 2)) out.add("- " + w);
                    }
                    if (f.rankUpHow != null && !f.rankUpHow.isBlank()) {
                        out.add("How to reach " + f.nextWandRank + " rank:");
                        out.addAll(SpellEffects.wrap(f.rankUpHow.trim(), LINE));
                    }
                } else {
                    out.add("Next rank: comes with " + cap(f.nextRankPath) + " rank");
                }
                return out;
            }
            if (f.nextRankCasts > 0) {
                n.append(Math.min(Math.max(0, f.castCount), f.nextRankCasts)).append(" of ").append(f.nextRankCasts).append(" casts");
            } else {
                n.append("keep casting it");
            }
            if (!f.pathOk && f.nextRankPath != null && !f.nextRankPath.isBlank()) n.append(", opens at ").append(cap(f.nextRankPath)).append(" rank");
            out.add(n.toString());
        } else if (f.maxRank > 1) {
            out.add("Top rank reached");
        }
        return out;
    }

    /**
     * The lines one rank shows that can change between ranks (effects, cost,
     * cooldown, range). Used to list what the next rank changes.
     */
    public static List<String> rankLines(SpellFacts f) {
        List<String> out = new ArrayList<>(f.effects);
        out.add("Cost: " + costText(f));
        out.addAll(cooldownLines(f));
        String r = rangeText(f);
        if (!r.isEmpty()) out.add("Range: " + r);
        return out;
    }

    public static String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Strip Minecraft legacy colour codes (section sign or ampersand). */
    public static String strip(String s) {
        if (s == null) return "";
        return s.replaceAll("(?i)[\u00a7&][0-9a-fk-orx]", "").replaceAll("\\s+", " ").trim();
    }
}

