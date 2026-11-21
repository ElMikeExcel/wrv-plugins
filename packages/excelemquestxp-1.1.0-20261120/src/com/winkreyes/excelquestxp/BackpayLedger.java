package com.winkreyes.excelquestxp;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persisted exactly-once ledger for backpay grants: player UUID + quest
 * file -&gt; paid timestamp + amounts, stored in
 * {@code backpay-paid.yml} in the plugin data folder.
 *
 * <p>This is the durable guard the in-memory session ledger cannot be: a
 * second {@code /questxp backpay} run — in the same session, after a
 * restart, or after a crash mid-run — must grant 0 for already-paid
 * quests. The paying lane writes through to disk after EVERY grant (not
 * once at the end of a run), so a crash between two quests can never
 * double-pay the first. The file is re-read from disk at the start of
 * every backpay run, so a hand-corrected file is honored.
 *
 * <p>File shape (SnakeYAML; quest filenames contain no Bukkit-loader
 * hazard here because Bukkit never parses this file):
 * <pre>
 * players:
 *   &lt;uuid&gt;:
 *     &lt;quest-file&gt;:
 *       paid: "2026-11-19T21:45:00-06:00"
 *       xp: 50
 *       armor: 16
 * </pre>
 */
public final class BackpayLedger {

    /** One paid quest: when it was paid and how much it paid. */
    public record Payment(String paidAt, long xp, long armor) {}

    private final Map<UUID, Map<String, Payment>> paid = new LinkedHashMap<>();

    /** True when this player/quest was already paid (checked BEFORE granting). */
    public synchronized boolean isPaid(UUID uuid, String questFile) {
        Map<String, Payment> perPlayer = paid.get(uuid);
        return perPlayer != null && perPlayer.containsKey(questFile);
    }

    /** Records a payment in memory. Callers persist with {@link #save(File)} right after. */
    public synchronized void markPaid(UUID uuid, String questFile,
                                     long xp, long armor, String paidAt) {
        paid.computeIfAbsent(uuid, k -> new LinkedHashMap<>())
                .put(questFile, new Payment(paidAt, xp, armor));
    }

    public synchronized Payment payment(UUID uuid, String questFile) {
        Map<String, Payment> perPlayer = paid.get(uuid);
        return perPlayer == null ? null : perPlayer.get(questFile);
    }

    public synchronized int paidCount(UUID uuid) {
        Map<String, Payment> perPlayer = paid.get(uuid);
        return perPlayer == null ? 0 : perPlayer.size();
    }

    public synchronized int paidCountTotal() {
        int total = 0;
        for (Map<String, Payment> perPlayer : paid.values()) {
            total += perPlayer.size();
        }
        return total;
    }

    /** Deep unmodifiable snapshot (for tests and status reporting). */
    public synchronized Map<UUID, Map<String, Payment>> snapshot() {
        Map<UUID, Map<String, Payment>> out = new LinkedHashMap<>();
        for (Map.Entry<UUID, Map<String, Payment>> e : paid.entrySet()) {
            out.put(e.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(e.getValue())));
        }
        return Collections.unmodifiableMap(out);
    }

    /**
     * Loads the ledger from disk; a missing file yields an empty ledger.
     * A file that EXISTS but cannot be parsed throws — callers must refuse
     * to pay rather than treat a corrupt ledger as "nothing paid yet"
     * (that failure mode would double-pay).
     */
    @SuppressWarnings("unchecked")
    public static BackpayLedger load(File file) throws java.io.IOException {
        BackpayLedger ledger = new BackpayLedger();
        if (file == null || !file.isFile()) {
            return ledger;
        }
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            Map<String, Object> yaml = new Yaml().load(reader);
            if (yaml == null || !(yaml.get("players") instanceof Map<?, ?> players)) {
                return ledger;
            }
            for (Map.Entry<?, ?> e : players.entrySet()) {
                if (!(e.getKey() instanceof String uuidText)
                        || !(e.getValue() instanceof Map<?, ?> questMap)) {
                    continue;
                }
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidText.trim());
                } catch (IllegalArgumentException bad) {
                    continue;
                }
                for (Map.Entry<?, ?> q : questMap.entrySet()) {
                    if (!(q.getKey() instanceof String questFile)
                            || !(q.getValue() instanceof Map<?, ?> raw)) {
                        continue;
                    }
                    Map<String, Object> payment = (Map<String, Object>) raw;
                    String paidAt = payment.get("paid") instanceof String s ? s : "";
                    long xp = payment.get("xp") instanceof Number n ? n.longValue() : 0;
                    long armor = payment.get("armor") instanceof Number n ? n.longValue() : 0;
                    ledger.paid.computeIfAbsent(uuid, k -> new LinkedHashMap<>())
                            .put(questFile, new Payment(paidAt, xp, armor));
                }
            }
        } catch (java.io.IOException e) {
            throw e;
        } catch (Exception e) {
            throw new java.io.IOException("backpay-paid.yml is corrupt/unparseable: " + e, e);
        }
        return ledger;
    }

    /** Writes the whole ledger to disk (atomic-ish: temp file + rename). */
    public synchronized void save(File file) throws java.io.IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> players = new LinkedHashMap<>();
        for (Map.Entry<UUID, Map<String, Payment>> e : paid.entrySet()) {
            Map<String, Object> quests = new LinkedHashMap<>();
            for (Map.Entry<String, Payment> q : e.getValue().entrySet()) {
                Map<String, Object> payment = new LinkedHashMap<>();
                payment.put("paid", q.getValue().paidAt());
                payment.put("xp", q.getValue().xp());
                payment.put("armor", q.getValue().armor());
                quests.put(q.getKey(), payment);
            }
            players.put(e.getKey().toString(), quests);
        }
        root.put("players", players);
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        String text = new Yaml(options).dump(root);
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(tmp.toPath(), StandardCharsets.UTF_8)) {
            writer.write(text);
        }
        try {
            Files.move(tmp.toPath(), file.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException notAtomic) {
            Files.move(tmp.toPath(), file.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
