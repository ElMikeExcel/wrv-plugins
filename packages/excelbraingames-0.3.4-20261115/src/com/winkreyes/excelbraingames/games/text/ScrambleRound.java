package com.winkreyes.excelbraingames.games.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Word Scramble round (SPEC §7) — pure logic, no Bukkit. Five words
 * drawn from the tier's word pool (Kid: 3–4 letters, 3-choice tap only,
 * retries until correct, $10 per word; Standard: 5–8 letters, typed,
 * one attempt per word, $20 per word). Each word is letter-shuffled so
 * the scramble never equals the word. A Hint (Standard) reveals the
 * first letter and halves that word's value. Partial credit is real:
 * the round banks what it earned whenever it ends — including a
 * player-chosen "finish now" (SPEC §7.3).
 */
public final class ScrambleRound {

    public static final int WORDS = 5;

    private final String tierId;
    private final boolean kid;
    private final long perWordCents;
    private final List<String> words;      // the answers, in play order
    private final List<String> scrambled;  // the shown shuffles
    private final List<List<String>> choiceSets; // kid tier: 3 choices per word
    private int index;
    private long earnedCents;
    private boolean hintUsed; // for the CURRENT word (Standard)
    private boolean paid;
    private final long startedMillis;

    private ScrambleRound(String tierId, boolean kid, long perWordCents, List<String> words,
                          List<String> scrambled, List<List<String>> choiceSets, int index,
                          long earnedCents, boolean hintUsed, boolean paid, long startedMillis) {
        this.tierId = tierId;
        this.kid = kid;
        this.perWordCents = perWordCents;
        this.words = words;
        this.scrambled = scrambled;
        this.choiceSets = choiceSets;
        this.index = index;
        this.earnedCents = earnedCents;
        this.hintUsed = hintUsed;
        this.paid = paid;
        this.startedMillis = startedMillis;
    }

    /**
     * Draw 5 words of the tier's length band from the pool. Returns null
     * when the pool can't fill a round (caller surfaces a friendly error
     * instead of a broken round).
     */
    public static ScrambleRound generate(String tierId, boolean kid, long perWordCents,
                                         List<String> pool, Random rng, long startedMillis) {
        int minLen = kid ? 3 : 5, maxLen = kid ? 4 : 8;
        List<String> band = new ArrayList<>();
        for (String w : pool) {
            String word = w.trim().toUpperCase(java.util.Locale.ROOT);
            if (word.length() >= minLen && word.length() <= maxLen && word.chars().allMatch(Character::isLetter)
                    && !band.contains(word)) band.add(word);
        }
        if (band.size() < WORDS) return null;
        Collections.shuffle(band, rng);
        List<String> words = new ArrayList<>(band.subList(0, WORDS));
        List<String> scrambled = new ArrayList<>();
        List<List<String>> choiceSets = new ArrayList<>();
        for (String word : words) {
            scrambled.add(scramble(word, rng));
            if (kid) {
                List<String> choices = new ArrayList<>();
                choices.add(word);
                // decoys: other words from the band, preferring same length
                List<String> decoys = new ArrayList<>(band);
                decoys.remove(word);
                Collections.shuffle(decoys, rng);
                decoys.sort((a, b) -> Integer.compare(Math.abs(a.length() - word.length()),
                        Math.abs(b.length() - word.length())));
                for (String d : decoys) {
                    if (choices.size() >= 3) break;
                    choices.add(d);
                }
                Collections.shuffle(choices, rng);
                choiceSets.add(choices);
            } else {
                choiceSets.add(List.of());
            }
        }
        return new ScrambleRound(tierId, kid, perWordCents, words, scrambled, choiceSets,
                0, 0, false, false, startedMillis);
    }

    /** Shuffle letters until the result differs from the word itself. */
    static String scramble(String word, Random rng) {
        if (word.length() < 2) return word;
        List<Character> letters = new ArrayList<>();
        for (char c : word.toCharArray()) letters.add(c);
        for (int attempt = 0; attempt < 50; attempt++) {
            Collections.shuffle(letters, rng);
            StringBuilder sb = new StringBuilder();
            for (char c : letters) sb.append(c);
            if (!sb.toString().equals(word)) return sb.toString();
        }
        // Degenerate word (e.g. all letters but one identical): rotate.
        return word.substring(1) + word.charAt(0);
    }

    // ------------------------------------------------------------ play

    public boolean finished() { return index >= words.size(); }
    public String currentWord() { return finished() ? "" : words.get(index); }
    public String currentScrambled() { return finished() ? "" : scrambled.get(index); }
    public List<String> currentChoices() { return finished() ? List.of() : choiceSets.get(index); }

    /**
     * Answer the current word. Returns: 1 = correct (banked, advanced),
     * 0 = wrong. Standard: one attempt, advances either way. Kid: a
     * wrong tap does NOT advance (retries until correct, SPEC §7.2) —
     * callers distinguish via {@link #kid()}.
     */
    public int answer(String response) {
        if (finished() || response == null) return 0;
        boolean ok = response.trim().equalsIgnoreCase(words.get(index));
        if (ok) {
            earnedCents += hintUsed ? perWordCents / 2 : perWordCents;
            advance();
            return 1;
        }
        if (!kid) advance();
        return 0;
    }

    private void advance() { index++; hintUsed = false; }

    /** Standard-tier Hint: first letter, halves this word's value. No-op on kid tier or when already used. */
    public boolean useHint() {
        if (kid || finished() || hintUsed) return false;
        hintUsed = true;
        return true;
    }

    public char hintLetter() { return finished() ? '?' : words.get(index).charAt(0); }

    // ------------------------------------------------------------ accessors

    public String tierId() { return tierId; }
    public boolean kid() { return kid; }
    public long perWordCents() { return perWordCents; }
    public int index() { return index; }
    public long earnedCents() { return earnedCents; }
    public boolean hintUsed() { return hintUsed; }
    public boolean paid() { return paid; }
    public void markPaid() { paid = true; }
    public long startedMillis() { return startedMillis; }
    public List<String> words() { return Collections.unmodifiableList(words); }
    public List<String> scrambled() { return Collections.unmodifiableList(scrambled); }

    // ------------------------------------------------------------ persistence

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tier", tierId);
        m.put("kid", kid);
        m.put("perWord", perWordCents);
        m.put("words", new ArrayList<>(words));
        m.put("scrambled", new ArrayList<>(scrambled));
        List<List<String>> cs = new ArrayList<>();
        for (List<String> c : choiceSets) cs.add(new ArrayList<>(c));
        m.put("choiceSets", cs);
        m.put("index", index);
        m.put("earned", earnedCents);
        m.put("hintUsed", hintUsed);
        m.put("paid", paid);
        m.put("started", startedMillis);
        return m;
    }

    @SuppressWarnings("unchecked")
    public static ScrambleRound fromMap(Map<String, Object> m) {
        List<String> words = (List<String>) m.getOrDefault("words", List.of());
        List<List<String>> cs = new ArrayList<>();
        for (Object o : (List<Object>) m.getOrDefault("choiceSets", List.of())) {
            cs.add(new ArrayList<>((List<String>) o));
        }
        while (cs.size() < words.size()) cs.add(List.of());
        return new ScrambleRound(
                (String) m.getOrDefault("tier", "?"),
                Boolean.TRUE.equals(m.get("kid")),
                ((Number) m.getOrDefault("perWord", 0L)).longValue(),
                words,
                (List<String>) m.getOrDefault("scrambled", List.of()),
                cs,
                ((Number) m.getOrDefault("index", 0)).intValue(),
                ((Number) m.getOrDefault("earned", 0L)).longValue(),
                Boolean.TRUE.equals(m.get("hintUsed")),
                Boolean.TRUE.equals(m.get("paid")),
                ((Number) m.getOrDefault("started", 0L)).longValue());
    }
}
