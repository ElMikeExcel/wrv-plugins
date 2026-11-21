package com.winkreyes.excelquestxp;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The backpay table: player UUID -&gt; name, QuestsCompleted snapshot, quest file -&gt; XP.
 *
 * <p>Generated from the staged backpay computation
 * ({@code backpay-table.json}, BACKPAY-RECORD.md §3) and shipped as the
 * {@code backpay.yml} resource. Armor XP is derived per quest as
 * floor(XP/3) — the same rule live grants use — so the table carries weapon
 * XP only. Pure logic, no Bukkit calls, so the plain-JVM test suite can
 * exercise it without a server.
 *
 * <p>Pre-pay guard: the paying lane must refuse a player whose live
 * {@code QuestsCompleted} counter (EliteMobs {@code PlayerData}) no longer
 * matches the snapshot in this table — a moved counter means the player
 * completed more quests since staging, and the table must be recomputed
 * from a fresh pull before anyone is paid.
 */
public final class BackpayTable {

    /** One player's backpay entry. Quests keep file order (LinkedHashMap). */
    public record Entry(UUID uuid, String name, int questsCompleted,
                        Map<String, Long> quests) {
        /** Sum of weapon XP across this player's quests. */
        public long weaponTotal() {
            long total = 0;
            for (long xp : quests.values()) {
                total += xp;
            }
            return total;
        }

        /** Sum of Armor XP: floor(XP/3) per quest, matching the staged table. */
        public long armorTotal() {
            long total = 0;
            for (long xp : quests.values()) {
                total += GrantPolicy.armorXp(xp);
            }
            return total;
        }

        /** XP for one quest file, or 0 when the file is not in this entry. */
        public long xpForFile(String questFile) {
            Long xp = quests.get(questFile);
            return xp == null ? 0 : xp;
        }
    }

    private final Map<UUID, Entry> byUuid = new LinkedHashMap<>();
    private final Map<String, Entry> byNameLower = new LinkedHashMap<>();
    private int skipped;

    public static BackpayTable empty() {
        return new BackpayTable();
    }

    /**
     * Parses a SnakeYAML-loaded {@code backpay.yml} root map:
     * {@code players: { <uuid>: { name, questsCompleted, quests: { file: xp } } }}.
     * Malformed player entries are skipped (counted in {@link #skipped()});
     * a missing/empty {@code players} section yields an empty table.
     */
    @SuppressWarnings("unchecked")
    public static BackpayTable parse(Map<String, Object> yaml) {
        BackpayTable table = new BackpayTable();
        if (yaml == null) {
            return table;
        }
        Object playersObj = yaml.get("players");
        if (!(playersObj instanceof Map<?, ?> players)) {
            return table;
        }
        for (Map.Entry<?, ?> e : players.entrySet()) {
            if (!(e.getKey() instanceof String uuidText)
                    || !(e.getValue() instanceof Map<?, ?> raw)) {
                table.skipped++;
                continue;
            }
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidText.trim());
            } catch (IllegalArgumentException bad) {
                table.skipped++;
                continue;
            }
            Map<String, Object> entry = (Map<String, Object>) raw;
            String name = entry.get("name") instanceof String s ? s : uuidText;
            int counter = entry.get("questsCompleted") instanceof Number n
                    ? n.intValue() : -1;
            Map<String, Long> quests = new LinkedHashMap<>();
            if (entry.get("quests") instanceof Map<?, ?> questMap) {
                for (Map.Entry<?, ?> q : questMap.entrySet()) {
                    if (q.getKey() instanceof String file
                            && q.getValue() instanceof Number xp) {
                        quests.put(file, xp.longValue());
                    }
                }
            }
            if (counter < 0 || quests.isEmpty()) {
                table.skipped++;
                continue;
            }
            Entry parsed = new Entry(uuid, name, counter,
                    java.util.Collections.unmodifiableMap(quests));
            table.byUuid.put(uuid, parsed);
            table.byNameLower.put(name.toLowerCase(java.util.Locale.ROOT), parsed);
        }
        return table;
    }

    public Entry forUuid(UUID uuid) {
        return byUuid.get(uuid);
    }

    /** Case-insensitive name lookup (names come from the staged table). */
    public Entry forName(String name) {
        if (name == null) {
            return null;
        }
        return byNameLower.get(name.toLowerCase(java.util.Locale.ROOT));
    }

    public int playerCount() {
        return byUuid.size();
    }

    /** Malformed player entries skipped at parse time. */
    public int skipped() {
        return skipped;
    }

    /** The pre-pay guard: live counter must equal the staged snapshot. */
    public static boolean counterMatches(int liveQuestsCompleted, int snapshot) {
        return liveQuestsCompleted == snapshot;
    }
}
