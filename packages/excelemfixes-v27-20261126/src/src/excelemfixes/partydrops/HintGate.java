package excelemfixes.partydrops;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The quiet hint's cadence (PARTY-DROPS-SPEC-2026-10-20 §7, DECIDED
 * quiet hint). Mike wants players to understand that partying
 * together is beneficial, but the hint must stay quiet: a small
 * action-bar note when a multiplied elite drop lands, never a
 * broadcast, never spammy.
 *
 * Rule: a player may be shown the hint at most once per cooldown
 * (default 600 s). The first multiplied kill they are present for
 * always shows it (that is the teaching moment); after that the
 * cooldown keeps it rare. The glue only consults this gate when a
 * multiplier above 1x actually landed, so solo kills never start
 * anyone's clock.
 *
 * Pure logic over caller-supplied timestamps; unit tested.
 */
public final class HintGate {

    private final long cooldownMillis;
    private final Map<UUID, Long> lastShownMillis = new HashMap<>();

    public HintGate(long cooldownMillis) {
        this.cooldownMillis = Math.max(0L, cooldownMillis);
    }

    /** True (and records the showing) when this player may be shown the hint now. */
    public synchronized boolean tryShow(UUID player, long nowMillis) {
        if (player == null) return false;
        Long last = lastShownMillis.get(player);
        if (last != null && nowMillis - last < cooldownMillis) return false;
        lastShownMillis.put(player, nowMillis);
        return true;
    }

    /** Forgets a player (e.g. on quit) so a rejoin can be re-taught. */
    public synchronized void forget(UUID player) {
        if (player != null) lastShownMillis.remove(player);
    }

    public long cooldownMillis() {
        return cooldownMillis;
    }
}

// ExcelEMFixes party-drops module — ported from ExcelPartyDrops 0.1.0 (Wave 3, D245).
