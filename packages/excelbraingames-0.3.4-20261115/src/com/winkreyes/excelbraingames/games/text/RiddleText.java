package com.winkreyes.excelbraingames.games.text;

import java.util.List;
import java.util.Locale;

/**
 * Riddle of the Day content + answer matching (SPEC §8). One riddle per
 * server day, the same for everyone, drawn without repeat from the
 * config list until it cycles. Answer checking is fuzzy (SPEC §8.2):
 * case-insensitive, leading articles ("a", "an", "the") ignored,
 * punctuation trimmed, and a single trailing "s" tolerated on either
 * side (plural trimming) — each riddle ALSO carries an explicit
 * accepted-answers list for its real variants.
 */
public final class RiddleText {

    /** One riddle from wordlists/riddles.yml. */
    public record Riddle(String question, List<String> answers, String hint, boolean kid) {}

    public static final long BASE_CENTS = 10_000;   // $100
    public static final long HINT_COST_CENTS = 2_500; // -$25 per hint
    public static final long FLOOR_CENTS = 2_500;   // never below $25
    public static final int MAX_ATTEMPTS = 3;
    public static final int MAX_HINTS = 3;

    private RiddleText() {}

    /** Payout for a solve with the given number of hints used (SPEC §8.2). */
    public static long payoutCents(int hintsUsed) {
        return Math.max(BASE_CENTS - HINT_COST_CENTS * Math.max(0, hintsUsed), FLOOR_CENTS);
    }

    /** Normalize for comparison: lowercase, strip punctuation/articles, collapse spaces. */
    public static String normalize(String raw) {
        if (raw == null) return "";
        String s = raw.toLowerCase(Locale.ROOT).trim();
        s = s.replaceAll("[^a-z0-9 ]", " ").replaceAll(" +", " ").trim();
        for (String article : new String[]{"the ", "a ", "an "}) {
            if (s.startsWith(article)) { s = s.substring(article.length()).trim(); break; }
        }
        return s;
    }

    /** Drop one trailing plural "s" (never from a 1-2 letter word). */
    static String deplural(String s) {
        return s.length() > 2 && s.endsWith("s") ? s.substring(0, s.length() - 1) : s;
    }

    /** Fuzzy match against the riddle's accepted-answer list. */
    public static boolean matches(String response, Riddle riddle) {
        String guess = normalize(response);
        if (guess.isEmpty()) return false;
        for (String accepted : riddle.answers()) {
            String want = normalize(accepted);
            if (guess.equals(want)) return true;
            if (deplural(guess).equals(deplural(want)) && !deplural(want).isEmpty()) return true;
        }
        return false;
    }
}
