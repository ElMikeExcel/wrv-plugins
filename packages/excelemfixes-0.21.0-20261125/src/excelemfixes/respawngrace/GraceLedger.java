package excelemfixes.respawngrace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bukkit-free respawn-grace state machine (D195 item 3, redesigned
 * Nov 22, 2026 per Mike: the first design was "too strong" — an
 * open-ended shield until the player's first hit).
 *
 * The fuse model:
 *   TICKET    — armed by a death in an EliteMobs-scope world
 *               (carries the death's instance id, which is also
 *               tagged onto the dropped loot); valid for the
 *               return window; consumed by the one grant it buys.
 *   PRELOOT   — protection after the death-return into EM scope.
 *               No damage taken. Bounded by the max-duration cap.
 *   COUNTDOWN — entered the first time the player picks up a
 *               stack of their OWN death-drop loot (matched by
 *               the death instance id). Still protected; a short,
 *               visible, audible countdown (default 10 s) is now
 *               the fuse. Further pickups do NOT restart it.
 *   (end)     — countdown reaching zero (protection ends WITH
 *               the "Round start!" flourish), the player dealing
 *               damage at any point (ends immediately, WITH the
 *               flourish), the preloot cap expiring (ends plainly),
 *               leaving EM scope, death, or quit.
 *
 * All decisions are pure functions of (player, now) so the whole
 * machine is unit-testable on a plain JVM; the Bukkit listener
 * only translates events into these calls. Deadlines are
 * half-open (active while now &lt; deadline) and expiry is lazy:
 * reads treat a passed deadline as ended, so correctness never
 * depends on the sweep; the sweep exists to fire the end-of-grace
 * feedback exactly once (see {@link #purgeExpired}).
 */
public final class GraceLedger {

    public enum Phase { NONE, TICKET, PRELOOT, COUNTDOWN }

    /** How a protection window ended, as reported by the sweep. */
    public enum EndKind { CAP_EXPIRED, COUNTDOWN_EXPIRED }

    public record Expiry(UUID playerId, EndKind kind) {}

    private static final class State {
        Phase phase = Phase.TICKET;
        UUID deathId;
        long deadline;
    }

    private final Map<UUID, State> states = new HashMap<>();

    // ------------------------------------------------------------- ticket

    /** Arm (or re-arm) the return ticket for a player who just
     *  died in scope. The ticket lapses at now + windowMillis and
     *  carries the death instance id the dropped loot is tagged
     *  with. */
    public void armTicket(UUID playerId, UUID deathId, long nowMillis, long windowMillis) {
        if (windowMillis <= 0) {
            throw new IllegalArgumentException("windowMillis must be positive");
        }
        State s = new State();
        s.phase = Phase.TICKET;
        s.deathId = deathId;
        s.deadline = nowMillis + windowMillis;
        states.put(playerId, s);
    }

    /** True while a live (unexpired, unconsumed) ticket is held.
     *  Lazily drops a lapsed ticket. */
    public boolean hasTicket(UUID playerId, long nowMillis) {
        State s = states.get(playerId);
        if (s == null || s.phase != Phase.TICKET) {
            return false;
        }
        if (nowMillis >= s.deadline) {
            states.remove(playerId);
            return false;
        }
        return true;
    }

    /** The death instance id on the player's ticket or active
     *  grace, or null. The listener matches picked-up loot tags
     *  against this. */
    public UUID deathId(UUID playerId) {
        State s = states.get(playerId);
        return s == null ? null : s.deathId;
    }

    /** Drop any ticket (death out of scope, quit). Returns true
     *  if one was held. An active grace is NOT a ticket and is
     *  left alone. */
    public boolean clearTicket(UUID playerId) {
        State s = states.get(playerId);
        if (s == null || s.phase != Phase.TICKET) {
            return false;
        }
        states.remove(playerId);
        return true;
    }

    // -------------------------------------------------------------- grace

    /**
     * Consume the player's live ticket and enter PRELOOT
     * protection until now + capMillis. Returns false (and grants
     * nothing) when no live ticket is held — the anti-farming
     * core: no death, no grace; one death, one grant.
     */
    public boolean grantFromTicket(UUID playerId, long nowMillis, long capMillis) {
        if (capMillis <= 0) {
            throw new IllegalArgumentException("capMillis must be positive");
        }
        if (!hasTicket(playerId, nowMillis)) {
            return false;
        }
        State s = states.get(playerId);
        s.phase = Phase.PRELOOT;
        s.deadline = nowMillis + capMillis;
        return true;
    }

    /** The player's current phase at {@code nowMillis}; a passed
     *  deadline reads as NONE (lazy expiry). */
    public Phase phase(UUID playerId, long nowMillis) {
        State s = states.get(playerId);
        if (s == null) {
            return Phase.NONE;
        }
        if (s.phase != Phase.TICKET && nowMillis >= s.deadline) {
            return Phase.NONE;
        }
        return s.phase;
    }

    /** True while protected (PRELOOT or COUNTDOWN, deadline not
     *  yet reached). */
    public boolean isProtected(UUID playerId, long nowMillis) {
        Phase p = phase(playerId, nowMillis);
        return p == Phase.PRELOOT || p == Phase.COUNTDOWN;
    }

    /** Incoming damage to this player is blocked while protected. */
    public boolean damageBlocked(UUID victimId, long nowMillis) {
        return isProtected(victimId, nowMillis);
    }

    /** Milliseconds until the current phase's deadline, 0 when
     *  not in a timed phase or past it. */
    public long millisRemaining(UUID playerId, long nowMillis) {
        State s = states.get(playerId);
        if (s == null || s.phase == Phase.TICKET) {
            return 0;
        }
        return Math.max(0, s.deadline - nowMillis);
    }

    /**
     * The loot fuse: the player's first pickup of their own
     * death-drop loot moves PRELOOT into COUNTDOWN with a fresh
     * deadline of now + countdownMillis (the preloot cap no
     * longer applies — the countdown IS the fuse). Returns false
     * from any other phase: further pickups during COUNTDOWN do
     * not restart it, and a pickup with no active grace starts
     * nothing.
     */
    public boolean startCountdown(UUID playerId, long nowMillis, long countdownMillis) {
        if (countdownMillis <= 0) {
            throw new IllegalArgumentException("countdownMillis must be positive");
        }
        State s = states.get(playerId);
        if (s == null || s.phase != Phase.PRELOOT || nowMillis >= s.deadline) {
            return false;
        }
        s.phase = Phase.COUNTDOWN;
        s.deadline = nowMillis + countdownMillis;
        return true;
    }

    /**
     * An attack ends protection immediately, in either phase
     * (Nether precedent, D132 addendum: the moment the player hits
     * something they can take damage again). The triggering attack
     * itself is NOT blocked by this call — the caller lets it
     * land. Returns the phase that ended, or NONE.
     */
    public Phase onAttack(UUID playerId, long nowMillis) {
        Phase p = phase(playerId, nowMillis);
        if (p != Phase.PRELOOT && p != Phase.COUNTDOWN) {
            return Phase.NONE;
        }
        states.remove(playerId);
        return p;
    }

    /** End any protection (leaving scope, death). Returns the
     *  phase that ended, or NONE. A held ticket is untouched. */
    public Phase endGrace(UUID playerId) {
        State s = states.get(playerId);
        if (s == null || s.phase == Phase.TICKET) {
            return Phase.NONE;
        }
        Phase was = s.phase;
        states.remove(playerId);
        return was;
    }

    // ------------------------------------------------------------ cleanup

    /** Drop all state for a player (quit). */
    public void clear(UUID playerId) {
        states.remove(playerId);
    }

    /**
     * Remove every lapsed ticket and ended protection. Tickets
     * lapse silently; each protection that reached its deadline
     * is returned exactly once with its {@link EndKind} so the
     * caller can fire the right end feedback (countdown expiry
     * gets the flourish, cap expiry a plain notice).
     */
    public List<Expiry> purgeExpired(long nowMillis) {
        List<Expiry> expired = new ArrayList<>();
        Iterator<Map.Entry<UUID, State>> it = states.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, State> entry = it.next();
            State s = entry.getValue();
            if (nowMillis >= s.deadline) {
                it.remove();
                if (s.phase == Phase.PRELOOT) {
                    expired.add(new Expiry(entry.getKey(), EndKind.CAP_EXPIRED));
                } else if (s.phase == Phase.COUNTDOWN) {
                    expired.add(new Expiry(entry.getKey(), EndKind.COUNTDOWN_EXPIRED));
                }
            }
        }
        return expired;
    }

    public int ticketCount() {
        int n = 0;
        for (State s : states.values()) {
            if (s.phase == Phase.TICKET) n++;
        }
        return n;
    }

    public int graceCount() {
        int n = 0;
        for (State s : states.values()) {
            if (s.phase == Phase.PRELOOT || s.phase == Phase.COUNTDOWN) n++;
        }
        return n;
    }
}
