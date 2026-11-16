package com.winkreyes.exceltogether;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * mcMMO skill XP sharing rules, with no mcMMO or Bukkit types so they can be tested.
 * When a player's skill XP gain lands (final amount, after mcMMO's own multipliers and ExcelRewardGuard's
 * kill scaling), every active nearby party member gets a full copy of it in the SAME skill, applied as a
 * party-member gain so mcMMO does not boost it again. A re-entry guard stops a copy from being copied.
 */
public final class XpShareCore {
    public interface Applier { boolean apply(UUID receiver, Object skill, float xp, Object reason) throws Exception; }
    private final Applier applier; private final double fraction;
    private boolean sharing = false;
    public int shared = 0;

    /** Earner's final gain with the earner's own multipliers divided out. NaN when unusable (caller then shares a plain copy). */
    public static float baseOf(float finalXp, double earnerMod, double together) {
        double div = earnerMod * together;
        if (!(div > 0) || !Double.isFinite(div) || !(finalXp > 0)) return Float.NaN;
        return (float) (finalXp / div);
    }
    /** What a receiver gets: base through THEIR multipliers (rate such as weekend, perks, modifiers), then the party multiplier. */
    public static float receiverAmount(float base, double receiverMod, double partyMult) {
        return (float) (base * receiverMod * partyMult);
    }

    public XpShareCore(Applier applier, double percent) {
        this.applier = applier;
        this.fraction = Double.isFinite(percent) ? Math.max(0, Math.min(percent, 100)) / 100.0 : 0;
    }

    /** @param mcmmoSharesItself true when the earner's mcMMO party has XP share mode EQUAL (mcMMO already splits it; we stay out). */
    public int onGain(UUID earner, Object skill, float xp, Object reason, boolean mcmmoSharesItself, Supplier<List<UUID>> receivers) {
        if (sharing || fraction <= 0 || !(xp > 0) || Float.isInfinite(xp) || skill == null || mcmmoSharesItself) return 0;
        int n = 0;
        sharing = true;
        try {
            for (UUID r : receivers.get()) {
                if (r == null || r.equals(earner)) continue;
                try { if (applier.apply(r, skill, (float) (xp * fraction), reason)) { n++; shared++; } } catch (Throwable ignored) { }
            }
        } finally { sharing = false; }
        return n;
    }
}
