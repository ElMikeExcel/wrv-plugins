package com.winkreyes.excelmagicgate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Dead-spell-key gate for the wand inventory sync (0.3.2, Nov 12,
 * 2026 — the P0 "Magic broken" defensive fix; diagnosis:
 * magic-break-diagnosis-20261112/DIAGNOSIS.md §1 + §5 Fix 1).
 *
 * The defect: a spell key that resolves to NO SpellTemplate in
 * Magic's runtime registry poisons Magic's own Wand.buildInventory()
 * — it dereferences the null template (SpellTemplate.getMode()) with
 * no guard, so every addSpell / wand save on a wand whose spell set
 * carries such a key throws an NPE part-way through. The sync cannot
 * cure a dead key (it is a wand-data / spells.yml mismatch, fixed in
 * the data), but it must stop FEEDING resolvable grants into that
 * NPE and must NAME the dead key so the mismatch can be found.
 *
 * The rule: before the sync calls addSpell for a missing key, the
 * key is resolved against Magic's controller
 * (getSpellTemplate(key), reflectively, in MagicFloorHook). A key
 * with no template is SKIPPED — never passed to addSpell, never
 * counted as granted — and logged BY NAME at WARNING. Every other
 * key syncs exactly as before: add-only, verified by re-read.
 *
 * This class is the pure decision half (no Bukkit deps): given the
 * sync's missing list and a template resolver, partition the list
 * into grantable and skipped, preserving order. The resolver is
 * FAIL-OPEN: a resolver that is absent or throws for a key leaves
 * that key grantable — a broken probe must never block a grant;
 * Magic's own addSpell path remains the judge in that case, exactly
 * as it was before 0.3.2.
 */
public final class SpellTemplateGate {

    /** The two halves of a partitioned grant batch. */
    public record Partition(List<String> grantable, List<String> skipped) {}

    private SpellTemplateGate() {}

    /**
     * Partition a missing-spell batch by template resolution.
     * Keys the resolver confirms (returns true) are grantable; keys
     * it rejects (returns false) are skipped. A null resolver, or a
     * resolver that throws for a key, leaves the key grantable
     * (fail-open, see class javadoc). Blank keys are dropped.
     * Input order is preserved in both halves.
     */
    public static Partition partition(List<String> keys, Predicate<String> hasTemplate) {
        List<String> grantable = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                if (key == null || key.isBlank()) {
                    continue;
                }
                boolean resolves = true;
                if (hasTemplate != null) {
                    try {
                        resolves = hasTemplate.test(key);
                    } catch (RuntimeException probeFailure) {
                        resolves = true; // fail-open: the probe never blocks a grant
                    }
                }
                (resolves ? grantable : skipped).add(key);
            }
        }
        return new Partition(List.copyOf(grantable), List.copyOf(skipped));
    }

    /**
     * The WARNING line for one skipped key — the instrument that
     * names a dead key at sync time on the next join of any affected
     * player (the diagnosis §1.4 unknown, settled by this line).
     * Wording is pinned by the unit suite: the key is named, and the
     * line carries the mismatch verdict verbatim.
     */
    public static String skippedWarning(String spell, String playerName) {
        return "Wand inventory sync: spell '" + spell + "' for " + playerName
                + " has no template in Magic — skipped, wand data/config mismatch. "
                + "The key stays off the wand until the wand data or spells.yml is "
                + "fixed; the rest of the sync continues.";
    }

    /**
     * The completion-accounting fragment for a sync that skipped
     * dead keys: skipped keys are reported as skipped — never folded
     * into the granted count.
     */
    public static String accounting(int granted, int unverified, List<String> skipped) {
        return granted + " granted, " + unverified + " unverified, "
                + skipped.size() + " skipped (no template in Magic: "
                + String.join(", ", skipped) + ")";
    }
}
