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

    /** Full tooltip, one fact per line. */
    public static List<String> lines(SpellFacts f) {
        List<String> out = new ArrayList<>();
        out.add(f.name + "  (" + rankText(f) + ")");
        if (f.description != null && !f.description.isBlank()) out.add(f.description.trim());
        out.add("Cost: " + costText(f));
        if (f.toggle) out.add("Toggle: cast again to stop. Ends when your mana runs out.");
        if (f.stopsManaRegen) out.add("Mana does not refill while it is on.");
        out.add("Cooldown: " + (f.cooldownMs > 0 ? Durations.ms(f.cooldownMs) : "none"));
        String r = rangeText(f);
        if (!r.isEmpty()) out.add("Range: " + r);
        if (f.durationMs > 0) out.add("Lasts: " + Durations.ms(f.durationMs));
        if (f.bar > 0) out.add("On bar " + f.bar);
        if (f.rank < f.maxRank) {
            StringBuilder n = new StringBuilder("Next rank: ");
            if (f.nextRankCasts > 0) {
                n.append("cast it ").append(f.nextRankCasts).append(" times");
                if (f.castCount > 0) n.append(" (you have ").append(Math.min(f.castCount, f.nextRankCasts)).append(")");
            } else {
                n.append("keep casting it");
            }
            if (f.nextRankPath != null && !f.nextRankPath.isBlank()) n.append(", needs ").append(cap(f.nextRankPath)).append(" rank");
            out.add(n.toString());
        } else if (f.maxRank > 1) {
            out.add("Top rank reached");
        }
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
