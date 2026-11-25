package excelemfixes.mcmmo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The XP watchdog's rolling gain history: EVERY mcMMO gain the
 * module observes (any skill, any source — combat, gathering,
 * the module's own awards alike), kept per player and per skill
 * over a rolling hour, with minute and hour totals on demand.
 *
 * <p>Why this exists: the connector's rules are target-based
 * (EliteMobs entities pay kill awards, never per-damage XP).
 * A gain path nobody foresaw — a new plugin, a new mcMMO
 * channel, a content-pack trick — would not break those rules
 * so much as walk around them. The watchdog is the safety net
 * for the unknown: it watches the TOTALS, flags any skill
 * gaining faster than a generous threshold, and hard-caps a
 * skill whose rolling hour passes the cap, whatever produced
 * the gains. Every legitimate path sits far below the shipped
 * thresholds (the largest single award is 10,000 XP and kill
 * awards are separately ceilinged at 60,000 per hour; the
 * November 2026 dummy farm ran about 52,800 XP per minute).
 *
 * <p>The ledger persists beside the ceiling ledger so a
 * restart does not reset a farm in progress.
 *
 * <p>Pure logic (no Bukkit): the clock is always passed in.
 * All methods are synchronized; gain events can in principle
 * be observed from any event thread mcMMO uses.
 */
public final class WatchdogLedger {

    /** The flag window: one minute, in milliseconds. */
    public static final long MINUTE_MS = 60_000L;

    /** The cap window: one hour, in milliseconds. */
    public static final long HOUR_MS = 3_600_000L;

    private record Entry(long atMillis, double amount) {
    }

    /** Key: uuid + "|" + skill name. */
    private final Map<String, List<Entry>> entries = new HashMap<>();

    private static String key(UUID player, String skill) {
        return player + "|" + skill;
    }

    private void prune(String key, long nowMillis) {
        List<Entry> list = entries.get(key);
        if (list == null) return;
        long cutoff = nowMillis - HOUR_MS;
        list.removeIf(e -> e.atMillis() <= cutoff);
        if (list.isEmpty()) entries.remove(key);
    }

    /** Records one observed gain. Non-positive amounts are ignored. */
    public synchronized void record(UUID player, String skill,
                                    long nowMillis, double amount) {
        if (player == null || skill == null || amount <= 0) return;
        String k = key(player, skill);
        entries.computeIfAbsent(k, x -> new ArrayList<>())
                .add(new Entry(nowMillis, amount));
        prune(k, nowMillis);
    }

    /** Total gained inside the last {@code windowMs} for player+skill. */
    public synchronized double totalLast(UUID player, String skill,
                                         long nowMillis, long windowMs) {
        String k = key(player, skill);
        prune(k, nowMillis);
        List<Entry> list = entries.get(k);
        if (list == null) return 0.0;
        long cutoff = nowMillis - windowMs;
        double sum = 0.0;
        for (Entry e : list) {
            if (e.atMillis() > cutoff) sum += e.amount();
        }
        return sum;
    }

    /**
     * Serializes to the same plain line format as the ceiling
     * ledger: {@code uuid \t skill \t atMillis \t amount}, one
     * entry per line. Entries outside the hour window relative
     * to {@code nowMillis} are dropped on save.
     */
    public synchronized String serialize(long nowMillis) {
        StringBuilder sb = new StringBuilder();
        for (String k : new ArrayList<>(entries.keySet())) {
            prune(k, nowMillis);
        }
        for (Map.Entry<String, List<Entry>> mapEntry : entries.entrySet()) {
            String k = mapEntry.getKey();
            int bar = k.indexOf('|');
            String uuid = k.substring(0, bar);
            String skill = k.substring(bar + 1);
            for (Entry e : mapEntry.getValue()) {
                sb.append(uuid).append('\t').append(skill).append('\t')
                        .append(e.atMillis()).append('\t')
                        .append(Double.toString(e.amount())).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Loads a ledger written by {@link #serialize}. Malformed
     * lines are skipped, never fatal: a corrupt ledger degrades
     * to "less history remembered", which can only make the
     * watchdog more permissive for part of one window — the
     * safe direction for a data file a human might edit.
     */
    public static WatchdogLedger deserialize(String text) {
        WatchdogLedger ledger = new WatchdogLedger();
        if (text == null || text.isBlank()) return ledger;
        for (String line : text.split("\n")) {
            String[] parts = line.trim().split("\t");
            if (parts.length != 4) continue;
            try {
                UUID uuid = UUID.fromString(parts[0].trim());
                String skill = parts[1].trim();
                long at = Long.parseLong(parts[2].trim());
                double amount = Double.parseDouble(parts[3].trim());
                if (!skill.isEmpty() && amount > 0) {
                    ledger.entries.computeIfAbsent(key(uuid, skill),
                            x -> new ArrayList<>()).add(new Entry(at, amount));
                }
            } catch (IllegalArgumentException ignored) {
                // Skip the bad line; see class docs.
            }
        }
        return ledger;
    }
}
