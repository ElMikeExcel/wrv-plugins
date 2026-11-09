package com.winkreyes.excelprotect;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * ExcelProtect — pure heal-attribution math (㉚.10, 2026-11-08): the effective/overheal
 * split, splash-potion instant-health amounts, spell-key folding, and cast-claim
 * matching. No Bukkit types, so ProtectTest can pin every rule here directly.
 *
 * The matching model follows BossTrial 0.3.1's FightTracker (the model CombatCore's
 * original pairing claimed to copy but did not): a claim names a caster and, when
 * Magic's cast context provides one, a specific target; matching is BIDIRECTIONAL in
 * time (|claim − heal| ≤ window) because Magic fires CastEvent from
 * BaseSpell.finish() — after HealAction has already fired the CUSTOM regain inside
 * the cast — and a claim is NOT consumed by a match, because the WRV `heal` spell
 * is a heal-over-time (delay 500 ms, repeat 10, total_duration 5000 ms, 1 HP/tick)
 * whose ticks must all resolve against the one cast.
 * Excel — Wink Reyes Valley
 */
public final class HealAttribution {
    private HealAttribution() { }

    /** One heal, split: effective = health actually restored; overheal = the rest. */
    public record Split(double effective, double overheal) { }

    /**
     * Split a raw heal amount against the target's health BEFORE the heal lands
     * (EntityRegainHealthEvent handlers at MONITOR still see pre-heal health for
     * Magic's HealAction and for vanilla regains — the same assumption BossTrial's
     * FightTracker.recordHealing makes). Effective is capped at the health actually
     * missing; a full-health target turns the whole amount into overheal.
     */
    public static Split split(double rawAmount, double healthBefore, double maxHealth) {
        if (Double.isNaN(rawAmount) || rawAmount <= 0) return new Split(0, 0);
        double missing = maxHealth - healthBefore;
        if (Double.isNaN(missing) || missing <= 0) return new Split(0, rawAmount);
        double eff = Math.min(rawAmount, missing);
        return new Split(eff, rawAmount - eff);
    }

    /** Vanilla instant-health strength before splash intensity: 4 HP × 2^amplifier (amp 0 = 4, amp 1 = 8). */
    public static double instantHealthAmount(int amplifier) {
        int amp = Math.max(0, Math.min(amplifier, 10));
        return 4.0 * (1 << amp);
    }

    /**
     * What vanilla actually APPLIES for a splash instant-health effect:
     * (int)(healing × intensity + 0.5) — whole hit points only
     * (HealOrHarmMobEffect.applyInstantaneousEffect, bytecode-verified against
     * Paper 26.2, and confirmed on the heal rig: intensity 0.43625 × 8 = 3.49
     * on paper, exactly 3.0 HP restored in game). Crediting the unrounded
     * product would overstate every off-center splash — the exact data-truth
     * failure this item exists to fix — so the credited raw amount is this one.
     */
    public static double splashInstantAmount(int amplifier, double intensity) {
        if (!(intensity > 0)) return 0;
        return (int) (instantHealthAmount(amplifier) * intensity + 0.5);
    }

    /** A spell key's base form: lower-cased, leveled variants ("heal|2") folded to their base ("heal"). */
    public static String baseSpellKey(String key) {
        if (key == null) return "";
        String base = key.trim().toLowerCase(Locale.ROOT);
        int pipe = base.indexOf('|');
        return pipe >= 0 ? base.substring(0, pipe) : base;
    }

    public static boolean isHealSpell(String key, Set<String> healSpells) {
        return key != null && healSpells != null && healSpells.contains(baseSpellKey(key));
    }

    /**
     * One Magic cast claim. target is nullable: null = the cast context named no
     * player target (area heals), and matching falls back to proximity.
     * x/z/world are the caster's position at cast time (targetless-claim proximity).
     */
    public record Claim(UUID caster, String casterName, UUID target, String spell,
                        long ms, String world, double x, double z, long id) { }

    /**
     * Best claim explaining one regain, or null. Rules (BossTrial's model, adapted):
     * - Only CUSTOM regains match any claim (Magic HealAction's reason); MAGIC_REGEN
     *   regains match only claims for known heal spells (regen buffs from a cast).
     *   Every other reason never matches here — potions resolve via the splash path.
     * - |claim.ms − healMs| ≤ windowMs, in EITHER direction (CastEvent is post-cast).
     * - A target-specific claim must name this target, and then needs no proximity
     *   check — the spell itself chose the target.
     * - A targetless claim needs same world + proximity (distSq ≤ radiusSq), and a
     *   caster's targetless claim never explains their OWN regain unless the spell
     *   is a known heal spell (a damage caster standing in their own fire is not
     *   healing themselves).
     * - Exact-target claims beat targetless ones; ties break on smallest |Δt|.
     * - Claims are never consumed: one cast may explain many regains (HoT ticks).
     */
    /**
     * Is this projectile entity key a thrown potion? Both names occur: Bukkit's
     * ThrownPotion entity reports "minecraft:splash_potion" on current Paper,
     * while Magic's NMS-built fake potion (and older records) report
     * "minecraft:thrown_potion". The costume test must accept both, or a real
     * splash potion reads as a generic projectile (rig-caught, 2026-11-08).
     */
    public static boolean isPotionProjectile(String projectileKey) {
        return "minecraft:thrown_potion".equals(projectileKey)
                || "minecraft:splash_potion".equals(projectileKey);
    }

    /**
     * Damage-side claim matching (0.1.8 §S1): which cast explains a damage event.
     * Pure. Rules: the caster must match; a HEAL-spell claim never labels damage;
     * |hit − claim| ≤ window (bidirectional — CastEvent is post-cast, so the claim
     * usually lands just AFTER the hit); a claim aimed at a different entity never
     * matches; an exact-target claim beats a targetless one; a targetless claim
     * matches only when {@code allowTargetless} (no projectile-item evidence —
     * a thrown-potion-shaped hit demands an exact target, because a real potion
     * throw must never wear a spell label). Freshest wins among equals.
     */
    public static Claim bestDamageClaim(List<Claim> claims, UUID caster, UUID victim,
            long hitMs, long windowMs, boolean allowTargetless, Set<String> healSpells) {
        Claim best = null; boolean bestExact = false; long bestAbs = Long.MAX_VALUE;
        for (Claim c : claims) {
            if (c.caster() == null || !c.caster().equals(caster)) continue;
            if (isHealSpell(c.spell(), healSpells)) continue;
            long dt = Math.abs(hitMs - c.ms());
            if (dt > windowMs) continue;
            boolean exact = victim != null && victim.equals(c.target());
            if (!exact && c.target() != null) continue;
            if (!exact && !allowTargetless) continue;
            if (best == null || (exact && !bestExact)
                    || (exact == bestExact && (dt < bestAbs
                        || (dt == bestAbs && c.ms() > best.ms())))) {
                best = c; bestExact = exact; bestAbs = dt;
            }
        }
        return best;
    }

    public static Claim bestClaim(List<Claim> claims, UUID target, String reason,
                                  String world, double x, double z, long healMs,
                                  long windowMs, double radiusSq, Set<String> healSpells) {
        if (claims == null || target == null || reason == null) return null;
        boolean custom = reason.equals("CUSTOM");
        boolean magicRegen = reason.equals("MAGIC_REGEN");
        if (!custom && !magicRegen) return null;
        Claim best = null;
        boolean bestExact = false;
        long bestDelta = Long.MAX_VALUE;
        for (Claim c : claims) {
            if (magicRegen && !isHealSpell(c.spell(), healSpells)) continue;
            long delta = Math.abs(c.ms() - healMs);
            if (delta > windowMs) continue;
            boolean exact = c.target() != null;
            if (exact) {
                if (!c.target().equals(target)) continue;
            } else {
                if (c.world() == null || world == null || !c.world().equals(world)) continue;
                double dx = c.x() - x, dz = c.z() - z;
                if (dx * dx + dz * dz > radiusSq) continue;
                if (c.caster().equals(target) && !isHealSpell(c.spell(), healSpells)) continue;
            }
            if (best == null || (exact && !bestExact) || (exact == bestExact && delta < bestDelta)) {
                best = c; bestExact = exact; bestDelta = delta;
            }
        }
        return best;
    }
}
