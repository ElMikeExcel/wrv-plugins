package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ExcelProtect 0.1.8 (§S3) — the SPELL HEALING report over the combat stream's
 * persisted `combat_heal` records: Mike's three fields, per cast.
 *   spell used — the record's `spell` (or the potion identity for throws);
 *   who was healed — the record's `target`;
 *   by how much — raw amount with its effective / overheal split.
 * Records arrive per TICK (a heal-over-time writes ~10); a `cast_id` groups
 * one cast's ticks into one cast (CombatCore stamps it from the claim/throw).
 * Records without a cast_id fall back to gap grouping (same identity + target,
 * ticks ≤ 2 s apart = one cast). Unattributed heals received by `who` (no
 * healer on the record — the genericHeal residue) render as their own line so
 * the unattributed class is visible, never silently folded in.
 * Pure assembly over scanned lines, the HealingReport pattern.
 * Excel — Wink Reyes Valley
 */
public final class HealingSpellReport {
    private HealingSpellReport() {}

    public record Report(int records, int casts, double raw, double effective, double overheal,
                         double residueRaw, double residueEffective, List<String> lines) {}

    private static final class Group {
        final String kind;      // SPELL | POTION
        final String identity;  // spell key or potion id
        final String target;
        int casts;
        double raw, eff, over;
        String lastCastId;
        long lastTs = Long.MIN_VALUE;
        Group(String kind, String identity, String target) {
            this.kind = kind; this.identity = identity; this.target = target;
        }
    }

    /** Report for healer `who` over scanned combat-stream lines. */
    public static Report build(List<String> scannedLines, String who, int limit) {
        List<String> heal = new ArrayList<>();
        for (String line : scannedLines)
            if ("combat_heal".equals(Json.strField(line, "type"))) heal.add(line);
        heal.sort((a, b) -> Long.compare(tsOf(a), tsOf(b)));

        Map<String, Group> groups = new LinkedHashMap<>();
        double residueRaw = 0, residueEff = 0;
        int records = 0, casts = 0;
        double raw = 0, eff = 0, over = 0;
        for (String line : heal) {
            String healer = Json.strField(line, "healer");
            String target = Json.strField(line, "target");
            String source = Json.strField(line, "source");
            double amt = Json.numField(line, "amount", 0);
            double e = Json.numField(line, "effective", 0);
            double o = Json.numField(line, "overheal", 0);
            if (healer == null) {
                // unattributed residue: only meaningful for the player it healed
                if (who.equals(target)) { residueRaw += amt; residueEff += e; }
                continue;
            }
            if (!who.equals(healer)) continue;
            if (!"SPELL".equals(source) && !"POTION".equals(source)) continue;
            String idn = "SPELL".equals(source) ? Json.strField(line, "spell")
                    : Json.strField(line, "potion");
            if (idn == null) idn = "SPELL".equals(source) ? "(spell)" : "(potion)";
            final String identity = idn;
            String key = source + "|" + identity + "|" + target;
            Group g = groups.computeIfAbsent(key, k -> new Group(source, identity, target));
            String castId = Json.strField(line, "cast_id");
            long ts = tsOf(line);
            boolean newCast = g.casts == 0
                    || (castId != null && g.lastCastId != null && !castId.equals(g.lastCastId))
                    || (castId == null && g.lastCastId == null && ts - g.lastTs > 2_000)
                    || (castId == null) != (g.lastCastId == null);
            if (newCast) { g.casts++; casts++; }
            g.lastCastId = castId;
            g.lastTs = ts;
            g.raw += amt; g.eff += e; g.over += o;
            records++; raw += amt; eff += e; over += o;
        }

        List<String> out = new ArrayList<>();
        for (Group g : groups.values()) {
            if (out.size() >= limit) break;
            out.add(g.identity + " → " + g.target + ": " + g.casts + (g.casts == 1 ? " cast" : " casts")
                    + ", " + Json.num(CombatCore.round2(g.raw)) + " healed ("
                    + Json.num(CombatCore.round2(g.eff)) + " effective, "
                    + Json.num(CombatCore.round2(g.over)) + " overheal)");
        }
        if (residueRaw > 0)
            out.add("unattributed → " + who + ": " + Json.num(CombatCore.round2(residueRaw))
                    + " healed (" + Json.num(CombatCore.round2(residueEff))
                    + " effective) — no cast or throw explains it");
        return new Report(records, casts, CombatCore.round2(raw), CombatCore.round2(eff),
                CombatCore.round2(over), CombatCore.round2(residueRaw),
                CombatCore.round2(residueEff), out);
    }

    private static long tsOf(String line) {
        return (long) Json.numField(line, "ts_ms", 0);
    }
}
