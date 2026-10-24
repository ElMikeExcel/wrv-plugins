package com.winkreyes.excelguide.core;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Chat-text normalization shared by the intent matcher and retrieval.
 * Rules: lowercase; apostrophes are dropped ("can't" -&gt; "cant");
 * every other non-alphanumeric character becomes a space; whitespace
 * collapses. Intent phrase entries in intents.yml are written in this
 * same normalized form.
 */
public final class TextNorm {

    private TextNorm() {}

    /** Words that carry no topic weight for retrieval scoring. */
    public static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "is", "are", "was", "were", "be", "been",
            "do", "does", "did", "i", "me", "my", "we", "you", "your",
            "he", "she", "it", "its", "they", "them", "their", "to", "of",
            "in", "on", "at", "for", "with", "and", "or", "but", "if",
            "so", "can", "could", "should", "would", "will", "just",
            "how", "what", "when", "where", "who", "whom", "this", "that",
            "there", "here", "am", "have", "has", "had", "get", "got",
            "im", "ive", "dont", "cant", "wont", "whats", "please", "pls",
            "about", "into", "from", "by", "as", "than", "then");

    /** Openings that make a chat line question-shaped (SPEC rule 1). */
    private static final Set<String> QUESTION_OPENERS = Set.of(
            "how", "what", "where", "when", "why", "can", "could", "do",
            "does", "is", "are", "was", "were", "should", "would", "who",
            "anyone", "anybody", "which");

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        boolean lastSpace = true;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c == '\'' || c == '’') {
                continue; // apostrophes vanish: can't -> cant
            }
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                out.append(c);
                lastSpace = false;
            } else if (!lastSpace) {
                out.append(' ');
                lastSpace = true;
            }
        }
        return out.toString().trim();
    }

    /** The set of whole tokens in a normalized string. */
    public static Set<String> tokenSet(String normalized) {
        Set<String> out = new LinkedHashSet<>();
        if (normalized == null || normalized.isBlank()) {
            return out;
        }
        for (String t : normalized.split(" ")) {
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    /** Query terms for retrieval: tokens minus stopwords and 1-char noise. */
    public static Set<String> queryTerms(String normalized) {
        Set<String> out = new LinkedHashSet<>();
        for (String t : tokenSet(normalized)) {
            if (t.length() >= 2 && !STOPWORDS.contains(t)) {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * Does a normalized line look like a question (SPEC guardrail 1)?
     * True when the raw line ended in a question mark or the normalized
     * line opens with a question word.
     */
    public static boolean questionShaped(String raw, String normalized) {
        if (raw != null && raw.contains("?")) {
            return true;
        }
        if (normalized == null || normalized.isBlank()) {
            return false;
        }
        String first = normalized.split(" ", 2)[0];
        return QUESTION_OPENERS.contains(first);
    }

    /** Light stem, shared with retrieval: parties -> party, shops -> shop. */
    public static String stem(String term) {
        if (term.length() > 4 && term.endsWith("ies")) {
            return term.substring(0, term.length() - 3) + "y";
        }
        if (term.length() > 3 && term.endsWith("s") && !term.endsWith("ss")) {
            return term.substring(0, term.length() - 1);
        }
        return term;
    }

    /**
     * Does one intent phrase entry match? Single words match whole
     * tokens, with one-directional stemming: a player token whose stem
     * equals the entry also matches (entry "tree" matches the token
     * "trees"; entry "party" matches "parties"). The reverse does NOT
     * apply - entry "jobs" does not match the token "job", because
     * intents are authored in canonical forms and players pluralize.
     * Multi-word entries match as a padded substring of the normalized
     * text.
     */
    public static boolean entryMatches(String entry, String normalized, Set<String> tokens) {
        if (entry == null || entry.isBlank()) {
            return false;
        }
        if (entry.indexOf(' ') < 0) {
            if (tokens.contains(entry)) {
                return true;
            }
            for (String tok : tokens) {
                if (stem(tok).equals(entry)) {
                    return true;
                }
            }
            return false;
        }
        return (" " + normalized + " ").contains(" " + entry + " ");
    }
}
