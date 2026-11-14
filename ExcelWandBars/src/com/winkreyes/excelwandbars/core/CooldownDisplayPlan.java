package com.winkreyes.excelwandbars.core;

/**
 * 0.3.13: the accurate-cooldown display decision (Mike's directive,
 * Nov 8 2026 — "We should develop a new system so cooldowns are
 * accurate"). Pure (no Bukkit/Magic types), unit-tested on the plain JVM.
 *
 * The defect this answers (COOLDOWN-ACCURACY investigation, Nov 8 2026,
 * bytecode + rig evidence): the number Magic writes on a hotbar spell
 * icon is NOT reliably the spell's cooldown. Mage.updateHotbarStatus
 * writes ceil(getTimeToCast()/1000) clamped to [1, 99] (Geyser flattens
 * Bedrock to 64), and BaseSpell.getTimeToCast() is the MAX of three
 * different waits — remaining cooldown, charge recharge, and MANA WAIT
 * (time for regen to refill the cast cost) — or NULL when the spell
 * cannot cast at all right now, in which case Magic writes 1. Worse,
 * cooldowns live in PER-KEY stores: a cast records its cooldown under
 * the spell's BASE key, while rank variants (heal|2, heal|3) keep
 * separate stores a cast never writes. Mike's specimen, from the Nov 7
 * Wither fight: after a Heal cast the base key's remaining cooldown —
 * the record that gates recasting, and the one Magic's cast-failure
 * reminder reports — read 7,638 ms decaying over ~7.6 s, while the
 * variant-resolving lookup the display path performs read 638 ms and
 * hit 0 within ~1.3 s (rig phase A). The icon said "ready" for six
 * seconds while the spell was still gated: "heal never shows the
 * cooldown accurately." And with the caster mana-starved, the same
 * number instead tracks mana wait for spells whose cooldown is already
 * 0 (rig: flashheal, cooldown 0, number 4 -> 3 purely from regen).
 *
 * The rule here: the name tag (the 0.3.10 channel, extended) shows the
 * spell's TRUE REMAINING COOLDOWN — Spell.getRemainingCooldown(), the
 * same value the cast-failure reminder reports — whenever the icon
 * number cannot be trusted to show it:
 *  - the number is computed from a different quantity (time-to-cast
 *    differs from the cooldown: mana wait or charge recharge is the
 *    binding wait, or time-to-cast is null while a cooldown runs), or
 *  - the cooldown in seconds exceeds what the number channel can carry
 *    on the player's platform (99 Java / 64 Bedrock via Geyser).
 * When the number IS the cooldown (equal, under the cap), the name
 * stays clean and the number does the talking — today's truthful range
 * is unchanged. When no cooldown remains, there is never a tag, even
 * if a mana wait remains: cooldown is the quantity the tag names, and
 * mana has its own bar.
 *
 * With accuracy off, the plan reproduces the 0.3.10 rule exactly
 * (tag the time-to-cast past the threshold) — the legacy behavior is a
 * config flip away, not a rebuild.
 */
public final class CooldownDisplayPlan {
    private CooldownDisplayPlan() {}

    /** What the tag should show, decided from the two Magic values. */
    public static final class Decision {
        public final boolean tag;
        /** Seconds to render when tag is true (the cooldown, ceil). */
        public final long seconds;
        Decision(boolean tag, long seconds) { this.tag = tag; this.seconds = seconds; }
    }

    /** Platform caps on the icon-number channel: Magic clamps to 99; Geyser flattens Bedrock to 64. */
    public static final int JAVA_CAP_SECONDS = 99;
    public static final int BEDROCK_CAP_SECONDS = 64;

    /**
     * @param accuracyOn     the 0.3.13 behavior; false = legacy 0.3.10 rule
     * @param cooldownMs     Spell.getRemainingCooldown() — the pure cooldown; null = unreadable
     * @param timeToCastMs   BaseSpell.getTimeToCast() — what Magic's number is computed from; null = Magic writes 1
     * @param capSeconds     the icon-number cap on this player's platform (99 Java / 64 Bedrock)
     * @param thresholdSeconds legacy threshold, used only when accuracyOn is false
     */
    public static Decision decide(boolean accuracyOn, Long cooldownMs, Long timeToCastMs,
                                  int capSeconds, int thresholdSeconds) {
        if (!accuracyOn) {
            boolean tag = CooldownTagPlan.shouldTag(timeToCastMs, thresholdSeconds);
            return new Decision(tag, tag ? CooldownTagPlan.secondsShown(timeToCastMs) : 0);
        }
        if (cooldownMs == null || cooldownMs <= 0) return new Decision(false, 0);
        long cdSec = CooldownTagPlan.secondsShown(cooldownMs);
        boolean numberTruthful = timeToCastMs != null
                && CooldownTagPlan.secondsShown(timeToCastMs) == cdSec
                && cdSec <= Math.max(1, capSeconds);
        return new Decision(!numberTruthful, cdSec);
    }
}
