package com.winkreyes.excelbraingames.games.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * One Memory Match round: deck, flip/pair/win logic, save/resume. Pure logic
 * (SPEC §5). Card symbols are indexes into the config symbol list.
 *
 * A missed pair stays face-up until resolveMiss() — the GUI layer calls it
 * after the ~1 s pause and blocks further flips in the meantime, so the
 * player actually sees both cards before they flip back.
 */
public final class MemoryRound {

    public enum FlipResult { FIRST, MATCH, MISS, IGNORED }

    public static final byte DOWN = 0, UP = 1, MATCHED = 2;

    private final String tierId;
    private final int pairs;
    private final int[] deck;      // symbol index per card
    private final byte[] states;   // DOWN / UP / MATCHED per card
    private int firstUp = -1;
    private int missA = -1, missB = -1; // a missed pair waiting to flip back
    private int flips;
    private int matchedPairs;
    private final long startedMillis;
    private boolean paid;

    private MemoryRound(String tierId, int pairs, int[] deck, byte[] states, long startedMillis) {
        this.tierId = tierId; this.pairs = pairs;
        this.deck = deck; this.states = states;
        this.startedMillis = startedMillis;
    }

    /** Deal a fresh round: each symbol exactly twice, shuffled (SPEC §5.1). */
    public static MemoryRound deal(String tierId, int pairs, int symbolPoolSize, Random rng, long startedMillis) {
        if (pairs < 1 || pairs > symbolPoolSize) {
            throw new IllegalArgumentException("pairs " + pairs + " outside symbol pool " + symbolPoolSize);
        }
        List<Integer> symbols = new ArrayList<>();
        for (int i = 0; i < symbolPoolSize; i++) symbols.add(i);
        Collections.shuffle(symbols, rng);
        List<Integer> cards = new ArrayList<>();
        for (int i = 0; i < pairs; i++) { cards.add(symbols.get(i)); cards.add(symbols.get(i)); }
        Collections.shuffle(cards, rng);
        int[] deck = cards.stream().mapToInt(Integer::intValue).toArray();
        return new MemoryRound(tierId, pairs, deck, new byte[deck.length], startedMillis);
    }

    public FlipResult flip(int card) {
        if (card < 0 || card >= deck.length) return FlipResult.IGNORED;
        if (missA >= 0) return FlipResult.IGNORED;           // a miss is still showing
        if (states[card] != DOWN) return FlipResult.IGNORED; // face-up or matched
        states[card] = UP;
        flips++;
        if (firstUp < 0) { firstUp = card; return FlipResult.FIRST; }
        int a = firstUp;
        firstUp = -1;
        if (deck[a] == deck[card]) {
            states[a] = MATCHED;
            states[card] = MATCHED;
            matchedPairs++;
            return FlipResult.MATCH;
        }
        missA = a; missB = card;
        return FlipResult.MISS;
    }

    /** Flip a showing missed pair back down (called by the GUI after the pause). */
    public boolean resolveMiss() {
        if (missA < 0) return false;
        states[missA] = DOWN;
        states[missB] = DOWN;
        missA = missB = -1;
        return true;
    }

    public boolean missPending() { return missA >= 0; }
    public boolean isComplete() { return matchedPairs == pairs; }

    public String tierId() { return tierId; }
    public int pairs() { return pairs; }
    public int cardCount() { return deck.length; }
    public int symbolAt(int card) { return deck[card]; }
    public byte stateAt(int card) { return states[card]; }
    public int flips() { return flips; }
    public int matchedPairs() { return matchedPairs; }
    public long startedMillis() { return startedMillis; }
    public boolean paid() { return paid; }
    public void markPaid() { this.paid = true; }

    // ---------------------------------------------------------- save / resume

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tier", tierId);
        m.put("pairs", pairs);
        List<Integer> d = new ArrayList<>();
        for (int s : deck) d.add(s);
        m.put("deck", d);
        StringBuilder st = new StringBuilder();
        for (byte b : states) st.append((char) ('0' + b));
        m.put("states", st.toString());
        m.put("firstUp", firstUp);
        m.put("missA", missA);
        m.put("missB", missB);
        m.put("flips", flips);
        m.put("matched", matchedPairs);
        m.put("started", startedMillis);
        m.put("paid", paid);
        return m;
    }

    public static MemoryRound fromMap(Map<String, Object> m) {
        String tier = (String) m.get("tier");
        int pairs = ((Number) m.get("pairs")).intValue();
        List<?> raw = (List<?>) m.get("deck");
        int[] deck = new int[raw.size()];
        for (int i = 0; i < raw.size(); i++) deck[i] = ((Number) raw.get(i)).intValue();
        String st = (String) m.getOrDefault("states", "");
        byte[] states = new byte[deck.length];
        for (int i = 0; i < states.length && i < st.length(); i++) states[i] = (byte) (st.charAt(i) - '0');
        MemoryRound round = new MemoryRound(tier, pairs, deck, states,
                ((Number) m.getOrDefault("started", 0L)).longValue());
        round.firstUp = ((Number) m.getOrDefault("firstUp", -1)).intValue();
        round.missA = ((Number) m.getOrDefault("missA", -1)).intValue();
        round.missB = ((Number) m.getOrDefault("missB", -1)).intValue();
        round.flips = ((Number) m.getOrDefault("flips", 0)).intValue();
        round.matchedPairs = ((Number) m.getOrDefault("matched", 0)).intValue();
        round.paid = Boolean.TRUE.equals(m.get("paid"));
        return round;
    }
}
