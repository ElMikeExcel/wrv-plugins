package excelemfixes.mcmmo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The per-player, per-skill hourly ceiling on kill-award XP:
 * a rolling 60-minute window of GRANTED amounts (the amount the
 * mcMMO pipeline finally wrote, after every multiplier that
 * composes on a normal gain — the ledger is debited from the
 * post-pipeline figure, so no multiplier can lift a player past
 * the ceiling by more than the final grant's own scaling).
 *
 * <p>The ledger is the module's own persistent store (a data
 * file in the plugin folder). mcMMO's profile cannot be written
 * from outside its gain pipeline without skipping that pipeline
 * entirely, so the module keeps its bookkeeping beside the
 * plugin and reloads it at startup: a server restart does not
 * reset the window.
 *
 * <p>Pure logic (no Bukkit): the clock is always passed in.
 * All mutating/querying methods are synchronized; the caller
 * (the module engine) runs on the main thread, but the MONITOR
 * observer that debits can in principle be reached from any
 * event thread mcMMO uses.
 */
public final class CeilingLedger {

    /** The rolling window: one hour, in milliseconds. */
    public static final long WINDOW_MS = 3_600_000L;

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
        long cutoff = nowMillis - WINDOW_MS;
        list.removeIf(e -> e.atMillis() <= cutoff);
        if (list.isEmpty()) entries.remove(key);
    }

    /** Total granted inside the rolling window for player+skill. */
    public synchronized double grantedInWindow(UUID player, String skill,
                                               long nowMillis) {
        String k = key(player, skill);
        prune(k, nowMillis);
        List<Entry> list = entries.get(k);
        if (list == null) return 0.0;
        double sum = 0.0;
        for (Entry e : list) sum += e.amount();
        return sum;
    }

    /**
     * How much of {@code requested} fits under the ceiling right
     * now: {@code min(requested, ceiling - grantedInWindow)},
     * never negative. The caller grants only the fitted part;
     * the unfitted remainder of that award is forfeited (the
     * ceiling is a hard stop, not a queue).
     */
    public synchronized int fit(UUID player, String skill, int requested,
                                int ceiling, long nowMillis) {
        if (requested <= 0) return 0;
        double remaining = ceiling - grantedInWindow(player, skill, nowMillis);
        if (remaining <= 0) return 0;
        return (int) Math.min(requested, Math.floor(remaining));
    }

    /** Records a granted amount (post-pipeline) against the window. */
    public synchronized void debit(UUID player, String skill, double amount,
                                   long nowMillis) {
        if (amount <= 0) return;
        String k = key(player, skill);
        prune(k, nowMillis);
        entries.computeIfAbsent(k, x -> new ArrayList<>())
                .add(new Entry(nowMillis, amount));
    }

    /**
     * Serializes the ledger to a plain line format, one entry per
     * line: {@code uuid \t skill \t atMillis \t amount}. Entries
     * already outside the window relative to {@code nowMillis}
     * are dropped on save.
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
     * to "less history remembered", which can only ever make the
     * ceiling more permissive for part of one window — the safe
     * direction for a data file a human might edit.
     */
    public static CeilingLedger deserialize(String text) {
        CeilingLedger ledger = new CeilingLedger();
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
