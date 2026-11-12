package com.winkreyes.excelmagicgate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Wand spell-inventory reconcile (0.3.0 amendment, Nov 10, 2026 —
 * Mike's build requirement, decide-and-log D2).
 *
 * The defect this closes (parent backlog, Nov 10, 2026, ~1:05 AM:
 * "SUMMONS INVISIBLE — ROOT CAUSE FOUND"): the stage-floor reconcile
 * raised wand PATHS without granting the paths' spells, so spells
 * that are defined in the live book but were never listed on a
 * player's wand stay invisible in the Spell Book forever. The Nov 8
 * summoning tree (archer / bruiser / guardian / revenant / ghast /
 * mount) is the standing example: live in the stage-4 spells file,
 * present on NO pulled wand.
 *
 * The rule (Mike, Nov 10, 2026, ~1:08 AM — "SPELLBOOK AUTO-UPDATE"):
 * this is a STANDING SYNC on the reconcile sweep, not a one-time
 * grant. On every reconcile leg (watch sweep, join, wand activation,
 * stage apply, rank-up), any spell in the player's current-rank path
 * lists that is missing from their wand is granted, so:
 *   (a) rank-ups fill the book automatically,
 *   (b) a spell added later to a rank a player already holds appears
 *       on their wand without a manual grant pass, and
 *   (c) the switch-over placement IS the first sweep: a placed
 *       player's wand fills to their placed rank the first time it
 *       is reconciled after install.
 *
 * The sync only ever ADDS. It never removes a spell (a demotion
 * lowers the frame only — the demoted player keeps their book and
 * re-earns the frame), never touches slots a spell already occupies,
 * and never runs on a serialized (pouch / not-carried) wand — those
 * defer to activation exactly like the rank move itself.
 *
 * Expected set for a target rank = the rank path's cumulative spells
 * (Magic's ProgressionPath.getSpells() already accumulates through
 * the path inherit chain) + its extra spells (getExtraSpells(), e.g.
 * beginner's aqualung) + this packet's SUPPLEMENTAL grants below.
 * The supplemental map exists because the summoning tree was added
 * to the spell book by its own packet (spell-audit
 * packet-summons-20261108) without ever joining a path list; its
 * rank placement follows the tree design of record
 * (magic-summoning-tree/DESIGN.md S2.2 + the Nov 8 re-tier
 * correction that moved the elite tier to stage 4): archer is the
 * stage-2 root (student — its |2 tier requires apprentice and its
 * |3 requires master, read from the live spells file), bruiser is
 * stage 3 (apprentice), and guardian / revenant / ghast / mount are
 * the stage-4 tier (master). The map ships as config
 * (earning.inventory-reconcile.supplemental-grants) so a ruling can
 * be corrected without a rebuild; these constants are the fallback
 * when the config section is absent, and the unit suite pins the
 * shipped config to them.
 *
 * This class is the pure decision half (no Bukkit deps): given the
 * expected set and the wand's current base keys, which spells are
 * missing, and which inventory slots the grants take. The Bukkit
 * half (MagicFloorHook.syncInventory) reads the live path + wand
 * through Magic's own API and writes grants back through it
 * (addSpell + updateSpellInventory — the same property seam the
 * tame-fix packet's grant pass mirrors in the mage files' two
 * representations, parsed spell_inventory + item custom_data).
 */
public final class WandInventorySync {

    /** Supplemental grants by the rank that FIRST unlocks them
     *  (cumulative application is expectedFor's job). */
    public static final Map<String, List<String>> DEFAULT_SUPPLEMENTAL = defaultSupplemental();

    private static Map<String, List<String>> defaultSupplemental() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("student", List.of("archer"));
        m.put("apprentice", List.of("bruiser"));
        m.put("master", List.of("guardian", "revenant", "ghast", "mount"));
        return Map.copyOf(m);
    }

    private WandInventorySync() {}

    /**
     * Normalize a spell key (possibly a tier key like "mend|4", any
     * case, padded) to its base family key — the shape path lists
     * and wand inventories both use. Mirrors SpellKey.getBaseKey()
     * semantics, as CreditPolicy.familyOf does for credit.
     */
    public static String baseKey(String key) {
        if (key == null) {
            return "";
        }
        String k = key.trim().toLowerCase(Locale.ROOT);
        int bar = k.indexOf('|');
        return bar >= 0 ? k.substring(0, bar) : k;
    }

    /** Base-key set of a raw key collection (tier keys collapsed,
     *  blanks dropped). */
    public static Set<String> baseKeys(Collection<String> keys) {
        Set<String> out = new LinkedHashSet<>();
        if (keys != null) {
            for (String k : keys) {
                String b = baseKey(k);
                if (!b.isEmpty()) {
                    out.add(b);
                }
            }
        }
        return out;
    }

    /**
     * The full expected spell set for a target rank: the path's
     * cumulative spells + extra spells (either collection may carry
     * tier keys; both are normalized) + the supplemental grants for
     * every rank up to and including the target.
     */
    public static Set<String> expectedFor(RankFloor.Rank target,
                                          Collection<String> pathSpells,
                                          Collection<String> pathExtraSpells,
                                          Map<String, List<String>> supplemental) {
        Set<String> out = new LinkedHashSet<>();
        out.addAll(baseKeys(pathSpells));
        out.addAll(baseKeys(pathExtraSpells));
        if (target != null && supplemental != null) {
            for (RankFloor.Rank r : RankFloor.Rank.values()) {
                if (r.ordinal() > target.ordinal()) {
                    break;
                }
                List<String> extra = supplemental.get(r.key());
                if (extra != null) {
                    out.addAll(baseKeys(extra));
                }
            }
        }
        return out;
    }

    /**
     * Spells in the expected set that the wand does not hold, in
     * sorted (deterministic) order — grant order never depends on
     * set iteration, so two sweeps over the same state agree and the
     * second is empty (idempotence is a property of this function:
     * missingFor(expected, held + missingFor(expected, held)) == []).
     * Comparison is by base key on both sides: a wand holding any
     * tier of a family holds the spell.
     */
    public static List<String> missingFor(Set<String> expected, Collection<String> heldKeys) {
        Set<String> held = baseKeys(heldKeys);
        Set<String> missing = new TreeSet<>();
        if (expected != null) {
            for (String want : expected) {
                String b = baseKey(want);
                if (!b.isEmpty() && !held.contains(b)) {
                    missing.add(b);
                }
            }
        }
        return new ArrayList<>(missing);
    }

    /**
     * Inventory slots for a grant batch: each granted spell takes
     * the next free slot after the wand's current maximum (or 0 on
     * an empty wand), in the batch's (sorted) order — the exact
     * placement rule the tame-fix packet's one-time grant pass
     * specifies for mage-file edits (Marvin's tame at max+1), applied
     * standing. Existing slots are never renumbered.
     */
    public static Map<String, Integer> slotsFor(List<String> grants,
                                                Map<String, Integer> currentInventory) {
        int next = 0;
        if (currentInventory != null && !currentInventory.isEmpty()) {
            int max = -1;
            for (Integer slot : currentInventory.values()) {
                if (slot != null && slot > max) {
                    max = slot;
                }
            }
            next = max + 1;
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        if (grants != null) {
            for (String g : grants) {
                String b = baseKey(g);
                if (!b.isEmpty() && !out.containsKey(b)) {
                    out.put(b, next++);
                }
            }
        }
        return out;
    }
}
