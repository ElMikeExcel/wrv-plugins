package com.winkreyes.excelsealfreeze.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The arm-time baseline snapshot: one line per player in a plain text file
 * owned by ExcelSealFreeze (plugins/ExcelSealFreeze/baseline.txt).
 *
 * Format:
 *   # ExcelSealFreeze baseline v1
 *   # armedAt=<epochMillis>
 *   uuid|hlk|score|kills|currencyV2|currencyCents|xp0,xp1,...,xp10
 *
 * Pure serialize/parse so the round-trip is unit-tested. Malformed lines are
 * skipped on parse (a corrupt line must never widen or narrow another
 * player's clamp); a missing entry means the zero baseline (see ClampPolicy).
 */
public final class BaselineStore {

    public static final String HEADER = "# ExcelSealFreeze baseline v1";

    private BaselineStore() {
    }

    public static String serialize(Map<String, Progression.ProgRow> baseline, long armedAtMillis) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append('\n');
        sb.append("# armedAt=").append(armedAtMillis).append('\n');
        for (Progression.ProgRow r : baseline.values()) {
            sb.append(r.uuid).append('|')
                    .append(r.highestLevelKilled).append('|')
                    .append(r.score).append('|')
                    .append(r.kills).append('|')
                    .append(Double.toString(r.currencyV2)).append('|')
                    .append(r.currencyCents).append('|');
            for (int i = 0; i < r.skillXp.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(r.skillXp[i]);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public static Map<String, Progression.ProgRow> parse(String text) {
        Map<String, Progression.ProgRow> out = new LinkedHashMap<>();
        if (text == null) return out;
        for (String line : text.split("\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            try {
                String[] p = t.split("\\|", -1);
                if (p.length != 7) continue;
                String[] xp = p[6].split(",", -1);
                if (xp.length != Progression.SKILL_COLUMNS.length) continue;
                long[] skill = new long[xp.length];
                for (int i = 0; i < xp.length; i++) skill[i] = Long.parseLong(xp[i].trim());
                Progression.ProgRow row = new Progression.ProgRow(
                        p[0].trim(),
                        Long.parseLong(p[1].trim()),
                        Long.parseLong(p[2].trim()),
                        Long.parseLong(p[3].trim()),
                        Double.parseDouble(p[4].trim()),
                        Long.parseLong(p[5].trim()),
                        skill);
                out.put(row.uuid, row);
            } catch (NumberFormatException ignored) {
                // Skip the malformed line; never guess a player's baseline.
            }
        }
        return out;
    }
}
