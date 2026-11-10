package com.winkreyes.excelmagicgate;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The credited-cast definition for the Magic Rank earning system
 * (0.3.0) — pure logic, no Bukkit deps. Design of record:
 * ~/workspace/magic-rank-packet-20261110/DESIGN.md §3 (classification)
 * and §4 (effect gates).
 *
 * A cast earns rank credit only when it does REAL WORK (Mike's
 * meaningful-cast rule, Nov 9, 2026):
 * - Pure movement / teleport / passive / cosmetic / idle families
 *   NEVER credit (the no-credit set — Fling is the type specimen:
 *   Jester's 5,301 family casts of self-teleport are exactly the
 *   farm this rule exists to kill).
 * - Heal families credit only when a target actually regained health
 *   (Magic's HealAction refuses full-health targets, so a CUSTOM
 *   EntityRegainHealthEvent correlated with the cast PROVES work).
 * - Damage families credit only when a non-caster entity was
 *   targeted (strict), or — for projectile families whose hits
 *   resolve through child contexts — when a correlated Bukkit damage
 *   event lands in the post-cast window.
 * - Control families (push / pull / force) credit only when a
 *   non-caster entity was targeted.
 * - Everything else credited-by-default falls to the spec's layer 3:
 *   successful cast + per-family DAILY CAP (50/day, tiers share one
 *   bucket) as the catch-all bound.
 * - Damage credit against a PLAYER target is additionally bounded by
 *   a pair window (same caster→target pair, K credits per window),
 *   the provable countermeasure to two-account hit-trading. Mob
 *   targets are unaffected: farming mobs is playing the game.
 *
 * Classification ships as MagicGate CONFIG (earning.* in config.yml),
 * never hardcoded, so a ruling can be corrected without a rebuild.
 * The DEFAULT_* lists below are the packet's proposed config content
 * (DESIGN.md §3) and are what the shipped config.yml carries; the
 * unit suite asserts the shipped config and these defaults agree.
 *
 * feed is CREDITED (heal-gated): it is a targeted support spell with
 * a work target and a 60 s cooldown, and crediting it is the only
 * reading consistent with the locked placements (Marvin = Master at
 * 2,178 credited; excluding feed places him at 1,986 — DESIGN.md §3).
 */
public final class CreditPolicy {

    public enum GateMode {
        NO_CREDIT,          // family never earns credit
        HEAL_GATE,          // credit only when a heal actually landed
        DAMAGE_STRICT,      // credit only when a non-caster entity was targeted
        DAMAGE_CORRELATED,  // strict, or a correlated damage event in the window
        TARGET_GATE,        // credit only when a non-caster entity was targeted
        SUCCESS_ONLY        // successful cast credits (daily cap is the bound)
    }

    public enum Decision {
        CREDIT,
        DENIED_EXCLUDED_FAMILY,
        DENIED_NOT_SUCCESS,
        DENIED_SUBCONTEXT,
        DENIED_HEAL_NOT_OBSERVED,
        DENIED_NO_TARGET,
        DENIED_DAILY_CAP,
        DENIED_PAIR_WINDOW
    }

    /** Everything the credit hook observed about one cast. */
    public record Observation(
            String spellKey,          // full tier key, e.g. "mend|4"
            boolean successful,       // SpellResult.isSuccess()
            boolean baseContext,      // base context (not a Repeat/Delay sub-cast)
            boolean healObserved,     // a CUSTOM regain landed since pre-cast
            boolean targetedNonCaster,// context targeted entities include a non-caster
            boolean correlatedDamage, // a damage event by this caster landed in-window
            boolean playerTarget,     // a targeted entity is a player (pair-window scope)
            int dailyCountForFamily,  // credits already earned today in this family
            int pairCreditsInWindow)  // credits already earned vs this target in the window
    {}

    // ------------------------------------------------------------------
    // The DESIGN.md §3 classification (defaults; config carries the same)
    // ------------------------------------------------------------------

    /** No-credit: movement / teleport (DESIGN.md §3 table 1). */
    public static final List<String> DEFAULT_NO_CREDIT_MOVEMENT = List.of(
            "fling", "blink", "backflip", "evade", "glide",
            "hulkleap", "skating", "recall", "recall_death", "gather");

    /** No-credit: passives — not deliberate player casts at all. */
    public static final List<String> DEFAULT_NO_CREDIT_PASSIVE = List.of(
            "critical", "aqualung", "healing");

    /** No-credit: cosmetic / idle / zero-work-target utility. */
    public static final List<String> DEFAULT_NO_CREDIT_UTILITY = List.of(
            "rolldice", "music", "sparkle", "hat", "wearskates", "peek",
            "torch", "magictorch", "throwmagictorch", "workbench", "stash",
            "disposal", "admission", "repairitem", "repairequipment",
            "platform", "tent", "tiny_house", "giant_house", "sponge",
            "harden", "rubberize", "growth", "pollinate", "grow", "flare",
            "day", "night", "time", "weather", "localrain",
            "restorelocalweather", "eat_food", "gasp");

    /** The full default no-credit set: DESIGN.md §3's three tables
     *  enumerate 47 families (the design text says 46; seven of the
     *  listed families have zero casts in the Nov 9 fixtures, so the
     *  discrepancy cannot move any placement — the placement-fixture
     *  test reproducing 11,231 / 2,178 / 1,210 / 12 / 0 EXACTLY is
     *  the regression net for this set). */
    public static Set<String> defaultNoCredit() {
        Set<String> out = new LinkedHashSet<>();
        out.addAll(DEFAULT_NO_CREDIT_MOVEMENT);
        out.addAll(DEFAULT_NO_CREDIT_PASSIVE);
        out.addAll(DEFAULT_NO_CREDIT_UTILITY);
        return Set.copyOf(out);
    }

    /** Heal-gated families (DESIGN.md §4): credit requires an observed
     *  CUSTOM health regain correlated with the cast. feed included. */
    public static final List<String> DEFAULT_HEAL_GATE = List.of(
            "heal", "mend", "flashheal", "greaterheal", "superheal",
            "grouprenewal", "layonhands", "holylight", "feed");

    /** Damage families gated strictly on targeted entities. dash is
     *  here per the DESIGN.md §3 uncertain table (movement shape, but
     *  it carries a Damage action — combat dash, damage-gated). */
    public static final List<String> DEFAULT_DAMAGE_STRICT = List.of(
            "chainlightning", "earth", "frost", "fury", "lightning",
            "masterattack", "powerstone", "smite", "stormshackle",
            "stream", "stun", "sweep", "wave", "whip",
            "wrv_frostbite", "wrv_spark", "anvil", "dash");

    /** Projectile damage families: strict OR correlated damage event. */
    public static final List<String> DEFAULT_DAMAGE_CORRELATED = List.of(
            "arrow", "arrow_ice", "arrow_light", "arrow_regular",
            "arrow_shock", "arrowrain", "boomerang", "missile", "throwsword");

    /** Control families: credit only with a non-caster target. */
    public static final List<String> DEFAULT_TARGET_GATE = List.of(
            "push", "pull", "force");

    public static final int DEFAULT_DAILY_CAP = 50;
    public static final int DEFAULT_PAIR_WINDOW_MAX = 5;
    public static final int DEFAULT_PAIR_WINDOW_MINUTES = 10;

    // ------------------------------------------------------------------
    // Policy instance (built from config by the plugin class)
    // ------------------------------------------------------------------

    private final Set<String> noCredit;
    private final Set<String> healGate;
    private final Set<String> damageStrict;
    private final Set<String> damageCorrelated;
    private final Set<String> targetGate;
    private final int dailyCap;
    private final int pairWindowMax;

    public CreditPolicy(Set<String> noCredit, Set<String> healGate,
                        Set<String> damageStrict, Set<String> damageCorrelated,
                        Set<String> targetGate, int dailyCap, int pairWindowMax) {
        this.noCredit = noCredit == null ? Set.of() : Set.copyOf(noCredit);
        this.healGate = healGate == null ? Set.of() : Set.copyOf(healGate);
        this.damageStrict = damageStrict == null ? Set.of() : Set.copyOf(damageStrict);
        this.damageCorrelated = damageCorrelated == null ? Set.of() : Set.copyOf(damageCorrelated);
        this.targetGate = targetGate == null ? Set.of() : Set.copyOf(targetGate);
        this.dailyCap = Math.max(1, dailyCap);
        this.pairWindowMax = Math.max(1, pairWindowMax);
    }

    /** The packet defaults (DESIGN.md §3/§4), used when config is absent. */
    public static CreditPolicy defaults() {
        return new CreditPolicy(defaultNoCredit(),
                new LinkedHashSet<>(DEFAULT_HEAL_GATE),
                new LinkedHashSet<>(DEFAULT_DAMAGE_STRICT),
                new LinkedHashSet<>(DEFAULT_DAMAGE_CORRELATED),
                new LinkedHashSet<>(DEFAULT_TARGET_GATE),
                DEFAULT_DAILY_CAP, DEFAULT_PAIR_WINDOW_MAX);
    }

    public Set<String> noCredit() { return noCredit; }
    public int dailyCap() { return dailyCap; }
    public int pairWindowMax() { return pairWindowMax; }

    /**
     * The classification family of a spell key: the base key with any
     * tier suffix removed, mirroring Magic's SpellKey semantics
     * (getBaseKey()). "mend|4" -&gt; "mend"; "stormshackle" -&gt;
     * "stormshackle". Null/blank -&gt; "".
     */
    public static String familyOf(String spellKey) {
        if (spellKey == null) {
            return "";
        }
        String k = spellKey.trim().toLowerCase(Locale.ROOT);
        int bar = k.indexOf('|');
        return bar >= 0 ? k.substring(0, bar) : k;
    }

    public GateMode modeFor(String family) {
        if (family == null || family.isEmpty() || noCredit.contains(family)) {
            return GateMode.NO_CREDIT;
        }
        if (healGate.contains(family)) {
            return GateMode.HEAL_GATE;
        }
        if (damageStrict.contains(family)) {
            return GateMode.DAMAGE_STRICT;
        }
        if (damageCorrelated.contains(family)) {
            return GateMode.DAMAGE_CORRELATED;
        }
        if (targetGate.contains(family)) {
            return GateMode.TARGET_GATE;
        }
        return GateMode.SUCCESS_ONLY;
    }

    /**
     * The credit decision for one observed cast. Order matters: the
     * cheap structural denials (excluded family, failed cast,
     * sub-context echo) land before any effect gate, and the daily
     * cap is the final catch-all every credited family passes through.
     */
    public Decision decide(Observation o) {
        GateMode mode = modeFor(familyOf(o.spellKey()));
        if (mode == GateMode.NO_CREDIT) {
            return Decision.DENIED_EXCLUDED_FAMILY;
        }
        if (!o.successful()) {
            return Decision.DENIED_NOT_SUCCESS;
        }
        if (!o.baseContext()) {
            return Decision.DENIED_SUBCONTEXT;
        }
        switch (mode) {
            case HEAL_GATE -> {
                if (!o.healObserved()) {
                    return Decision.DENIED_HEAL_NOT_OBSERVED;
                }
            }
            case DAMAGE_STRICT, TARGET_GATE -> {
                if (!o.targetedNonCaster()) {
                    return Decision.DENIED_NO_TARGET;
                }
            }
            case DAMAGE_CORRELATED -> {
                if (!o.targetedNonCaster() && !o.correlatedDamage()) {
                    return Decision.DENIED_NO_TARGET;
                }
            }
            default -> {
                // SUCCESS_ONLY: no effect gate; the cap below bounds it.
            }
        }
        // Hit-trading bound: damage credit against a player target is
        // pair-windowed (mob targets never are).
        if ((mode == GateMode.DAMAGE_STRICT || mode == GateMode.DAMAGE_CORRELATED)
                && o.playerTarget() && o.pairCreditsInWindow() >= pairWindowMax) {
            return Decision.DENIED_PAIR_WINDOW;
        }
        if (o.dailyCountForFamily() >= dailyCap) {
            return Decision.DENIED_DAILY_CAP;
        }
        return Decision.CREDIT;
    }
}
