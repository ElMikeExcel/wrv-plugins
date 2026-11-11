package com.winkreyes.excelsummons;

import java.util.Map;
import java.util.UUID;

/**
 * Cap decisions, pure and unit-tested: per-spell family cap, the
 * unified per-caster TOTAL cap, and the server-wide combat ceiling.
 * Enforcement happens BEFORE the cast (Magic PreCastEvent) so a
 * refused cast spends no mana; capture-time enforcement in the
 * plugin is the backstop for casts that never pass PreCast
 * (console/API casts).
 *
 * 0.1.4 (FIX-PACKET-DESIGN §3, Mike, Nov 10, 2026, 10:11 AM; cap
 * AMENDED the same morning, 10:35 AM ruling, ladder LOCKED
 * 10:37 AM): the per-caster combat cap is REPLACED by a unified
 * total that is RANK-SCALED — at most {@code summon-cap-by-rank}
 * active spell summons per player (Beginner 1 · Student 2 ·
 * Apprentice 3 · Master 5), counting COMBAT + COMPANION + PACK
 * together (the wolf's branch is PACK), read from the player's
 * EARNED rank (RankCap: MagicGate's earned.yml credited total
 * through the locked thresholds). The MOUNT is not counted
 * (persistent transport, per-spell cap 1 still binds). Per-spell
 * caps still bind lower. The total cap applies to EVERY managed
 * cast including wolf, and is NOT gated on a rule's enforceCap
 * flag — that flag exempts the wolf's family cap, which Magic's
 * builtin class owns; it must not exempt Mike's total cap, which
 * the bridge owns. The old COMBAT_CAP refusal is retired
 * (subsumed: any combat-only count is <= the total count, so a
 * separate combat cap could never bind first). An unreadable rank
 * fails to the LOWEST rung (RankCap.capForRank).
 *
 * Check order is message accuracy (§3.2): SPELL_CAP first (only
 * when the rule enforces its family cap), then TOTAL_CAP, then the
 * combat-only SERVER_CEILING.
 *
 * The numbers live in the bridge config and mirror the summon_cap /
 * summon_duration_ms annotation keys on the spells themselves —
 * one number, two readers (DESIGN §6.1).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class CapPolicy {

    /** One managed spell family's bridge-side rule. */
    public static final class SpellRule {
        public final SummonRecord.Branch branch;
        public final int cap;              // per caster; <=0 = no per-spell cap
        public final long durationMillis;  // <=0 = no bridge expiry
        public final boolean enforceCap;   // false for wolf: its class owns the cap

        public SpellRule(SummonRecord.Branch branch, int cap, long durationMillis,
                         boolean enforceCap) {
            this.branch = branch;
            this.cap = cap;
            this.durationMillis = durationMillis;
            this.enforceCap = enforceCap;
        }
    }

    public enum Refusal { NONE, SPELL_CAP, TOTAL_CAP, SERVER_CEILING }

    private final Map<String, Integer> capByRank;
    private final int serverCombatCeiling;

    public CapPolicy(Map<String, Integer> capByRank, int serverCombatCeiling) {
        this.capByRank = capByRank == null ? Map.of() : Map.copyOf(capByRank);
        this.serverCombatCeiling = serverCombatCeiling;
    }

    /** The counted-summon cap for one earned rank (fail-lowest). */
    public int capForRank(String rank) {
        return RankCap.capForRank(capByRank, rank);
    }

    public Map<String, Integer> capByRank() { return capByRank; }
    public int serverCombatCeiling() { return serverCombatCeiling; }

    /**
     * Would one more summon of {@code rule}'s family, cast by
     * {@code owner} (whose earned rank is {@code rank}, null when
     * unreadable), break a cap? Checked in design order: the
     * spell's own cap, then the rank-scaled total cap, then the
     * server combat ceiling.
     */
    public Refusal check(SummonRegistry reg, UUID owner, String baseSpellKey,
                         SpellRule rule, String rank) {
        if (rule == null) return Refusal.NONE;
        if (rule.enforceCap && rule.cap > 0
                && reg.countSpell(owner, baseSpellKey) >= rule.cap) {
            return Refusal.SPELL_CAP;
        }
        // The mount is never counted and never total-cap-refused
        // (§3.2): recasting dismisses it (listener), and a fresh
        // mount cast at counted cap still succeeds (§5.3 step 4).
        if (rule.branch != SummonRecord.Branch.MOUNT
                && reg.countCounted(owner) >= capForRank(rank)) {
            return Refusal.TOTAL_CAP;
        }
        if (rule.branch == SummonRecord.Branch.COMBAT
                && serverCombatCeiling > 0
                && reg.countBranchServerWide(SummonRecord.Branch.COMBAT) >= serverCombatCeiling) {
            return Refusal.SERVER_CEILING;
        }
        return Refusal.NONE;
    }

    /**
     * Can the player release at least one counted summon by their
     * own action? Combat summons expire and leave via
     * `dismiss all`; CAST-origin pets leave via `dismiss pets`.
     * Only ADOPTED pets have no player release (death /
     * reconciliation only) — the §4 TOTAL_CAP message branches on
     * this so it never tells a player to "dismiss" a summon that
     * cannot be dismissed.
     */
    public static boolean anyCountedReleasable(SummonRegistry reg, UUID owner) {
        for (SummonRecord rec : reg.ofOwner(owner)) {
            if (!SummonRegistry.COUNTED_BRANCHES.contains(rec.branch())) continue;
            if (rec.branch() == SummonRecord.Branch.COMBAT) return true;
            if (rec.origin() == SummonRecord.Origin.CAST) return true;
        }
        return false;
    }

    /** Display names of the owner's counted summons, registry
     *  order — the "what is currently summoned" the 10:34 AM
     *  refusal ruling requires the cap message to name. */
    public static java.util.List<String> countedNames(SummonRegistry reg, UUID owner) {
        java.util.List<String> names = new java.util.ArrayList<>();
        if (reg == null) return names;
        for (SummonRecord rec : reg.ofOwner(owner)) {
            if (SummonRegistry.COUNTED_BRANCHES.contains(rec.branch())) {
                names.add(Nameplate.spellDisplayName(rec.spellKey()));
            }
        }
        return names;
    }

    /** The TOTAL_CAP text (§4.2 as amended by the 10:34/10:35 AM
     *  rulings): the rank, the rank's limit, the live count, and
     *  what is currently summoned — then how to free a slot, or
     *  the adopted-pets truth when nothing counted can be
     *  dismissed. */
    public static String totalCapMessage(String rank, int counted, int cap,
                                         java.util.List<String> names,
                                         boolean anyReleasable) {
        StringBuilder sb = new StringBuilder("Summon limit reached — your rank (")
                .append(RankCap.display(rank)).append(") allows ").append(cap)
                .append(cap == 1 ? " summon" : " summons")
                .append(", and you have ").append(counted).append(" out");
        if (names != null && !names.isEmpty()) {
            sb.append(": ").append(String.join(", ", names));
        }
        sb.append(". ");
        sb.append(anyReleasable
                ? "Dismiss one (or let a combat summon expire) before summoning another."
                : "Your summons are pets that can only leave by death.");
        return sb.toString();
    }

    /** The SPELL_CAP text (§4.2): display name + live numbers. */
    public static String spellCapMessage(String spellDisplayName, int count, int cap) {
        return "Your " + spellDisplayName + " limit is reached — " + count
                + " of " + cap + " already out.";
    }

    /** The SERVER_CEILING text (§4.2): concept kept, numbers added. */
    public static String serverCeilingMessage(int ceiling) {
        return "The valley is holding as many combat summons as it can right now ("
                + ceiling + "). Try again shortly.";
    }

    /**
     * Full-context refusal message (§4.2). The listener computes
     * the numbers from the registry at refusal time so the text a
     * player reads is the state that refused them.
     */
    public String refusalMessage(Refusal r, SummonRegistry reg, UUID owner,
                                 String baseSpellKey, SpellRule rule, String rank) {
        switch (r) {
            case SPELL_CAP:
                return spellCapMessage(Nameplate.spellDisplayName(baseSpellKey),
                        reg == null ? 0 : reg.countSpell(owner, baseSpellKey),
                        rule == null ? 0 : rule.cap);
            case TOTAL_CAP:
                return totalCapMessage(rank, reg == null ? 0 : reg.countCounted(owner),
                        capForRank(rank), countedNames(reg, owner),
                        reg != null && anyCountedReleasable(reg, owner));
            case SERVER_CEILING:
                return serverCeilingMessage(serverCombatCeiling);
            default:
                return "";
        }
    }

    /** Bare refusal message (no registry context). Prefer the
     *  full-context overload — §4 requires the live numbers and
     *  the rank in the text. */
    public static String refusalMessage(Refusal r, String spellKey) {
        switch (r) {
            case SPELL_CAP:
                return "Your " + Nameplate.spellDisplayName(spellKey)
                        + " limit is reached.";
            case TOTAL_CAP:
                return "Summon limit reached — dismiss one (or let a combat "
                        + "summon expire) before summoning another.";
            case SERVER_CEILING:
                return "The valley is holding as many combat summons as it can "
                        + "right now. Try again shortly.";
            default:
                return "";
        }
    }
}
