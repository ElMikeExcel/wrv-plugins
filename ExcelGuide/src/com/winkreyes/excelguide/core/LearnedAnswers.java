package com.winkreyes.excelguide.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.yaml.snakeyaml.Yaml;

/**
 * Promoted ("learned") answers (0.2.1): AI answers a human reviewed
 * and promoted - from the weekly Guide report - into permanent
 * deterministic answers in a data-folder file (default learned.yml).
 * Same shape discipline as the curated intents' text entries:
 *
 *   learned:
 *     - id: tame-a-fox            # optional label; used in logs only
 *       phrases:                  # natural wordings, any case/punct
 *         - "how do i tame a fox"
 *         - "how to tame a fox"
 *       answer: "Crouch near a fox holding sweet berries ..."
 *
 * MATCHING: a question matches an entry when its normalized tokens,
 * stemmed and sorted, equal a phrase's. Same words, any order, and
 * simple -s plurals collapse (the house light stemmer: "cats" =
 * "cat") - exact-set matching, never fuzzy: a promoted answer fires
 * only for the question it was reviewed for and its listed variants.
 * The FIRST entry wins on a duplicate phrase (write specific entries
 * first, as with intents).
 *
 * Promoted answers sit in the deterministic tier: they outrank the
 * AI cache and the AI itself, and they never stale-expire - a human
 * vouched for them. They still serve privately, on the asked doors
 * only (/ask + summon), exactly like the AI answers they graduate
 * from.
 *
 * FAIL-SOFT by contract: {@link #load} NEVER throws. A missing or
 * comments-only file is an empty set with no warning; malformed
 * entries are skipped and counted in the warning; a syntax-broken
 * file is an empty set plus a warning. The glue logs the warning and
 * the Guide runs on - a bad edit to learned.yml can never take the
 * plugin down, and /guide reload picks up edits because the file is
 * re-read on every engine build.
 */
public final class LearnedAnswers {

    /** One promoted entry as written in the file. */
    public record Entry(String id, List<String> phrases, String answer) {}

    /** Load outcome: the answers, plus a human-readable warning (null = clean). */
    public record LoadResult(LearnedAnswers answers, String warning) {}

    private final List<Entry> entries;
    private final Map<String, String> byKey;

    private LearnedAnswers(List<Entry> entries, Map<String, String> byKey) {
        this.entries = List.copyOf(entries);
        this.byKey = Map.copyOf(byKey);
    }

    public static LearnedAnswers empty() {
        return new LearnedAnswers(List.of(), Map.of());
    }

    public int size() {
        return entries.size();
    }

    public List<Entry> entries() {
        return entries;
    }

    /** The promoted answer for a normalized question, or null. */
    public String match(String normalizedQuestion) {
        if (normalizedQuestion == null || normalizedQuestion.isBlank()) {
            return null;
        }
        return byKey.get(keyOf(normalizedQuestion));
    }

    /**
     * The match key: the question's tokens, stemmed, de-duplicated,
     * sorted. Two texts with the same key are "the same question" for
     * promotion purposes.
     */
    static String keyOf(String normalized) {
        Set<String> parts = new TreeSet<>();
        for (String t : TextNorm.tokenSet(normalized)) {
            parts.add(TextNorm.stem(t));
        }
        return String.join(" ", parts);
    }

    /** Parse learned.yml text. Never throws - see the class docs. */
    @SuppressWarnings("unchecked")
    public static LoadResult load(String yamlText) {
        if (yamlText == null || yamlText.isBlank()) {
            return new LoadResult(empty(), null);
        }
        Object root;
        try {
            root = new Yaml().load(yamlText);
        } catch (Exception e) {
            return new LoadResult(empty(), "learned file is not valid YAML ("
                    + e.getMessage() + "); ignoring it");
        }
        if (root == null) {
            return new LoadResult(empty(), null); // comments-only file
        }
        if ((root instanceof List<?> l0 && l0.isEmpty())
                || (root instanceof Map<?, ?> m0 && m0.isEmpty())) {
            return new LoadResult(empty(), null); // "[]" or "{}" (the Oct 24 clear): empty, no warning
        }
        if (!(root instanceof Map)) {
            return new LoadResult(empty(), "learned file root is not a map; ignoring it");
        }
        Object raw = ((Map<String, Object>) root).get("learned");
        if (raw == null) {
            return new LoadResult(empty(), null);
        }
        if (!(raw instanceof List)) {
            return new LoadResult(empty(), "learned file 'learned' is not a list; ignoring it");
        }
        List<Entry> out = new java.util.ArrayList<>();
        Map<String, String> byKey = new LinkedHashMap<>();
        int skipped = 0;
        for (Object o : (List<Object>) raw) {
            if (!(o instanceof Map)) {
                skipped++;
                continue;
            }
            Map<String, Object> m = (Map<String, Object>) o;
            String answer = m.get("answer") == null ? "" : String.valueOf(m.get("answer")).trim();
            List<String> phrases = new java.util.ArrayList<>();
            Object rawPhrases = m.get("phrases");
            if (rawPhrases instanceof List) {
                for (Object p : (List<?>) rawPhrases) {
                    String norm = TextNorm.normalize(String.valueOf(p));
                    if (!norm.isBlank()) {
                        phrases.add(norm);
                    }
                }
            }
            if (answer.isBlank() || phrases.isEmpty()) {
                skipped++;
                continue;
            }
            String id = m.get("id") == null ? phrases.get(0) : String.valueOf(m.get("id"));
            out.add(new Entry(id, List.copyOf(phrases), answer));
            for (String phrase : phrases) {
                byKey.putIfAbsent(keyOf(phrase), answer);
            }
        }
        String warning = skipped == 0 ? null
                : "skipped " + skipped + " malformed learned entries (need phrases + answer)";
        return new LoadResult(new LearnedAnswers(out, byKey), warning);
    }
}
