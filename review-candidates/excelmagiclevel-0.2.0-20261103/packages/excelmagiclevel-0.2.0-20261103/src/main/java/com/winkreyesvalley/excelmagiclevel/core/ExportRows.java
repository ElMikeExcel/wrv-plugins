package com.winkreyesvalley.excelmagiclevel.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelMagicLevel — the mcMMO-shaped export (SPEC §9.3). Our store is the
 * same shape as mcMMO's model — (player UUID, skill, total XP) with level
 * derived from total XP — so a future true-mcMMO-skill migration is an
 * import, not a reset. {@code /magiclevel admin export} writes these rows;
 * this pure function is what the Phase 0 tests exercise against a fixture.
 */
public final class ExportRows {
    public static final String SKILL = "magic";

    private ExportRows() {
    }

    /** YAML rows, highest total XP first (ties by UUID for determinism). */
    public static String toYaml(Map<UUID, Long> totalXpByPlayer, Curve curve, Instant generatedAt) {
        List<Map.Entry<UUID, Long>> rows = new ArrayList<>(totalXpByPlayer.entrySet());
        rows.sort((a, b) -> {
            int byXp = Long.compare(b.getValue(), a.getValue());
            return byXp != 0 ? byXp : a.getKey().toString().compareTo(b.getKey().toString());
        });
        StringBuilder out = new StringBuilder();
        out.append("# ExcelMagicLevel export — mcMMO-shaped rows (uuid, skill, total-xp, derived-level).\n");
        out.append("# Generated ").append(generatedAt).append(" by /magiclevel admin export. Skill is always \"magic\".\n");
        out.append("rows:\n");
        for (Map.Entry<UUID, Long> row : rows) {
            out.append("  - uuid: \"").append(row.getKey()).append("\"\n");
            out.append("    skill: ").append(SKILL).append('\n');
            out.append("    total-xp: ").append(row.getValue()).append('\n');
            out.append("    level: ").append(curve.levelFor(row.getValue())).append('\n');
        }
        return out.toString();
    }
}
