package com.winkreyes.excelwandbars.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.2.2 wording for spell progress: the one action-bar line after a counted
 * cast, and the rank header at the top of the Spell Book. Pure text (no
 * Bukkit or Magic types), unit-tested on the plain JVM.
 *
 * Rank-up rule (Magic 11.2.5 source, checked on the test rig Oct 24, 2026):
 * a spell ranks up after its required number of successful casts only when
 * the wand has a Magic path that allows the next rank (hasPath). WRV's rank
 * wands (beginner, student, apprentice, master) have no path and the server
 * has no default_wand_path, so on WRV casting never ranks a spell up: each
 * spell's rank follows the wand's rank (level_spells_to_path). This class
 * shows cast counts only where they really lead to a rank (a wand with a
 * path), and otherwise says the next rank comes with the next wand rank.
 * Nothing here ever mentions seals.
 */
public final class ProgressText {
    private ProgressText() {}

    /** Action-bar line after a cast, or null when there is nothing to count (top rank, no cast rule). */
    public static String line(SpellFacts f) {
        if (f == null || f.rank >= f.maxRank || f.nextRankCasts <= 0 || f.rankByWand) return null;
        long have = Math.min(Math.max(0, f.castCount), f.nextRankCasts);
        String base = f.name + ": " + have + "/" + f.nextRankCasts + " casts to Rank " + (f.rank + 1);
        if (f.pathOk) return base;
        String need = SpellInfoText.cap(f.nextRankPath);
        if (have >= f.nextRankCasts) return f.name + ": " + have + "/" + f.nextRankCasts + " casts. Rank " + (f.rank + 1) + " opens at " + need + " rank";
        return base + " (opens at " + need + " rank)";
    }

    /** Action-bar line when a spell just went up a rank. */
    public static String rankedUp(String name, int newRank) {
        return name + " reached Rank " + newRank + "!";
    }

    /** One spell that can move the wand up a rank right now. */
    public static final class Candidate {
        public final String name; public final long have; public final long need; public final int nextRank;
        public Candidate(String name, long have, long need, int nextRank) { this.name = name; this.have = have; this.need = need; this.nextRank = nextRank; }
        long left() { return Math.max(0, need - have); }
    }

    /** Spells whose next rank is open at the current wand rank and still needs casts. */
    public static List<Candidate> candidates(List<SpellFacts> book) {
        List<Candidate> out = new ArrayList<>();
        if (book == null) return out;
        for (SpellFacts f : book) {
            if (f == null || !f.pathOk || f.rank >= f.maxRank || f.nextRankCasts <= 0) continue;
            long have = Math.min(Math.max(0, f.castCount), f.nextRankCasts);
            if (have >= f.nextRankCasts) continue;
            out.add(new Candidate(f.name, have, f.nextRankCasts, f.rank + 1));
        }
        out.sort((a, b) -> a.left() != b.left() ? Long.compare(a.left(), b.left()) : a.name.compareToIgnoreCase(b.name));
        return out;
    }

    /**
     * Rank header lines. current/next: rank names ("Beginner", "Student"; next null = top rank).
     * byCasts: Magic would move the wand up as soon as one spell ranks up (the wand
     * already holds every spell of its rank). Never mentions seals.
     */
    public static List<String> header(String current, String next, boolean byCasts, List<Candidate> cands) {
        return header(current, next, byCasts, false, cands);
    }

    /**
     * byWand: the wand has no Magic path (WRV rank wands), so casts never rank a spell
     * up or move the wand up; spell ranks follow the wand's rank. Says only that.
     */
    public static List<String> header(String current, String next, boolean byCasts, boolean byWand, List<Candidate> cands) {
        List<String> out = new ArrayList<>();
        if (current == null || current.isBlank()) return out;
        if (next == null || next.isBlank()) {
            out.add("Your rank: " + current + " (top rank).");
            return out;
        }
        out.add("Your rank: " + current + ". Next rank: " + next + ".");
        if (byWand) {
            out.add("Your spells move up a rank when you reach " + next + " rank.");
            return out;
        }
        if (byCasts && cands != null && !cands.isEmpty()) {
            Candidate c = cands.get(0);
            if (cands.size() == 1) {
                out.add("To become " + article(next) + next + ", get " + c.name + " to Rank " + c.nextRank + ": " + c.have + "/" + c.need + " casts.");
            } else {
                out.add("To become " + article(next) + next + ", get any spell to its next rank. Closest: " + c.name + ", " + c.have + "/" + c.need + " casts.");
            }
        } else {
            out.add("Keep casting your spells. Each spell below shows its progress to its next rank.");
        }
        return out;
    }

    private static String article(String word) {
        if (word == null || word.isEmpty()) return "";
        char c = Character.toLowerCase(word.charAt(0));
        return "aeiou".indexOf(c) >= 0 ? "an " : "a ";
    }
}
