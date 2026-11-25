package excelemfixes.enderman;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Remembers, per player and weapon skill category, the size of
 * the last magic-weapon damage application EliteMobs was seen to
 * make for that player (the pre-formula amount on the damage
 * event a wand/staff application produces). A cast whose bolt
 * whiffed because its enderman blinked leaves no amount of its
 * own to re-apply, so the re-application is seeded with the
 * player's own most recent real application — their wand's real
 * weight, not a guessed constant. Bounded and purely in-memory:
 * a restart simply relearns from the first landed cast.
 */
public final class LearnedAmounts {

    private static final int MAX_ENTRIES = 256;

    private final Map<String, Double> amounts = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Double> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private static String key(UUID playerId, String category) {
        return playerId + "|" + (category == null ? "" : category);
    }

    public void remember(UUID playerId, String category, double amount) {
        if (playerId == null) return;
        if (!Double.isFinite(amount) || amount <= 0.0 || amount > 1000.0) return;
        amounts.put(key(playerId, category), amount);
    }

    /** The remembered amount, or {@code fallback} when none. */
    public double get(UUID playerId, String category, double fallback) {
        if (playerId == null) return fallback;
        Double value = amounts.get(key(playerId, category));
        return value == null ? fallback : value;
    }

    public boolean knows(UUID playerId, String category) {
        return playerId != null && amounts.containsKey(key(playerId, category));
    }

    public void clear() {
        amounts.clear();
    }

    public int size() {
        return amounts.size();
    }
}
