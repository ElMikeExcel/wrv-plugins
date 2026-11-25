package com.winkreyes.excelmagicgate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Dead-spell-key gate for the wand inventory sync.
 *
 * History: 0.3.2 (Nov 12, 2026, P0 "Magic broken" fix) partitioned
 * the MISSING grant batch by template resolution and skipped dead
 * keys, but its probe was FAIL-OPEN and it never audited keys
 * already held on the wand. The Nov 14, 2026 Resurrection incident
 * (P-DOWNED-REVIVE-1 diagnosis, Defect 2) proved both gaps: a wand
 * could be reported "verified" for resurrection / grouprenewal
 * while Magic could not construct the spell, because presence in
 * wand data was judged as verification and a dead HELD key was
 * invisible to a gate that only probed the missing batch.
 *
 * The 0.3.3 rule (grant verification, the MagicGate half of
 * P-DOWNED-REVIVE-1; it mirrors ExcelDowned 0.1.3's
 * FixPolicy.grantVerified exactly):
 *  - A grant is VERIFIED only when BOTH the wand data carries the
 *    key AND Magic's template for the key resolves. Presence alone
 *    is not verification.
 *  - The probe is FAIL-CLOSED: a resolver that is absent, returns
 *    false, or throws for a key leaves that key NOT grantable /
 *    NOT verified, and the key is named at WARNING. A probe that
 *    cannot answer never verifies a grant. (This reverses the
 *    0.3.2 fail-open stance for the grant path, on the diagnosis's
 *    instruction: a dead template must be a named WARNING, never
 *    a silent pass.)
 *  - HELD keys are audited too: every key already on the wand is
 *    probed, and a held key with no template is named at WARNING
 *    as held-but-not-verified. It is never counted as verified,
 *    and a sync whose wand carries one does not report a plain
 *    "in sync / verified" result.
 *
 * This class is the pure decision half (no Bukkit deps). The
 * Bukkit half (MagicFloorHook) supplies the reflective resolver
 * (Magic controller getSpellTemplate(key) != null) and the held /
 * re-read wand sets, and applies the verdicts here.
 */
public final class SpellTemplateGate {

    /** The two halves of a partitioned key batch. */
    public record Partition(List<String> grantable, List<String> skipped) {}

    private SpellTemplateGate() {}

    /**
     * Partition a key batch by template resolution, FAIL-CLOSED.
     * Keys the resolver confirms (returns true) are grantable;
     * keys it rejects, keys it throws on, and every key when the
     * resolver is null are skipped. Blank keys are dropped.
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
                boolean resolves = false;
                if (hasTemplate != null) {
                    try {
                        resolves = hasTemplate.test(key);
                    } catch (RuntimeException probeFailure) {
                        resolves = false; // fail-closed: an unanswered probe verifies nothing
                    }
                }
                (resolves ? grantable : skipped).add(key);
            }
        }
        return new Partition(List.copyOf(grantable), List.copyOf(skipped));
    }

    /**
     * Audit the keys a wand ALREADY holds: returns the held keys
     * whose template does not resolve (fail-closed, same probe
     * rule as partition). These keys are present in wand data but
     * are NOT verified and must be named at WARNING.
     */
    public static List<String> deadHeldKeys(List<String> heldKeys,
                                            Predicate<String> hasTemplate) {
        return partition(heldKeys, hasTemplate).skipped();
    }

    /**
     * A grant is verified only when BOTH the wand data carries the
     * key AND its template resolves. Presence alone (the pre-0.3.3
     * re-read test) is not verification.
     */
    public static boolean grantVerified(boolean wandDataPresent, boolean templateResolves) {
        return wandDataPresent && templateResolves;
    }

    /**
     * The WARNING line for one skipped grant candidate: the key is
     * named and the line carries the mismatch verdict verbatim.
     * Wording pinned by the unit suite (carried from 0.3.2).
     */
    public static String skippedWarning(String spell, String playerName) {
        return "Wand inventory sync: spell '" + spell + "' for " + playerName
                + " has no template in Magic \u2014 skipped, wand data/config mismatch. "
                + "The key stays off the wand until the wand data or spells.yml is "
                + "fixed; the rest of the sync continues.";
    }

    /**
     * The WARNING line for one dead HELD key (0.3.3 held-key
     * audit): the key is on the wand but its template does not
     * resolve, so the held grant is NOT verified and not castable
     * as far as Magic's registry is concerned.
     */
    public static String heldWarning(String spell, String playerName) {
        return "Wand inventory sync: spell '" + spell + "' already held by " + playerName
                + " has no template in Magic \u2014 held key NOT verified, "
                + "wand data/config mismatch. The key is present in wand data but "
                + "Magic cannot construct the spell; it is not counted as verified "
                + "until the wand data or spells.yml is fixed.";
    }

    /**
     * The completion-accounting fragment for a sync that skipped
     * dead keys: skipped keys are reported as skipped, never
     * folded into the granted count.
     */
    public static String accounting(int granted, int unverified, List<String> skipped) {
        return granted + " granted, " + unverified + " unverified, "
                + skipped.size() + " skipped (no template in Magic: "
                + String.join(", ", skipped) + ")";
    }
}
