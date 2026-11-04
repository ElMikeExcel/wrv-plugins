package com.winkreyesvalley.excelmagiclevel.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelMagicLevel — the in-memory anti-abuse windows (SPEC §4.2/§4.3, §7.1).
 * Deliberately NOT persisted: relogging resets a player's windows. That is
 * a named, accepted seam — the gain from relog-cycling is at most one
 * full-weight rotation per relog, and relogging is slow and visible.
 *
 * <p>Semantics mirror Magic's own SP earn code (BaseSpell.finish): the
 * per-spell timestamp records the last XP-PAYING cast, and it is only
 * updated when a cast actually pays &gt; 0 — a 0-pay spam cast does not
 * push the window back, so elapsed keeps growing toward a real payment.</p>
 *
 * <p>Pure Java, clock injected by the caller: fully unit-testable.</p>
 */
public final class AntiAbuseWindows {
    private record SpellKey(UUID player, String spell) {
    }

    private record TargetKey(UUID player, UUID target) {
    }

    private final Map<SpellKey, Long> lastSpellPay = new HashMap<>();
    private final Map<TargetKey, Long> lastTargetPay = new HashMap<>();

    /** Ms since this player's last paying cast of this spell, or null if none. */
    public Long spellElapsed(UUID player, String spellKey, long nowMillis) {
        Long last = lastSpellPay.get(new SpellKey(player, spellKey));
        return last == null ? null : nowMillis - last;
    }

    /** True if this player already paid against this target inside the window. */
    public boolean isTargetRepeat(UUID player, UUID target, long nowMillis, long targetWindowMillis) {
        if (target == null || targetWindowMillis <= 0) return false;
        Long last = lastTargetPay.get(new TargetKey(player, target));
        return last != null && (nowMillis - last) < targetWindowMillis;
    }

    /**
     * Record a paying cast. Call ONLY when the computed payment is &gt; 0 —
     * that is what keeps a 0-pay cast from resetting the ramp (see class doc).
     */
    public void recordPay(UUID player, String spellKey, UUID target, long nowMillis) {
        lastSpellPay.put(new SpellKey(player, spellKey), nowMillis);
        if (target != null) {
            lastTargetPay.put(new TargetKey(player, target), nowMillis);
        }
    }

    /** Forget a player's windows (e.g. on quit — hygiene, not correctness). */
    public void forget(UUID player) {
        lastSpellPay.keySet().removeIf(k -> k.player().equals(player));
        lastTargetPay.keySet().removeIf(k -> k.player().equals(player));
    }
}
