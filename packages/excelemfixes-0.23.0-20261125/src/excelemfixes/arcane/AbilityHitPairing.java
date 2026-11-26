package excelemfixes.arcane;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Pairs EliteMobs' two ability events for one hit.
 *
 * EliteMobs fires ClassAbilityAffectEliteEvent immediately before an
 * ability's damage step lands on an elite, and the damage itself then
 * flows through EliteMobDamagedByPlayerEvent (marked as custom
 * damage). The module marks (caster, elite) when the affect event
 * fires and treats a custom-damage event for the same pair inside a
 * short tick window as that ability's damage. Weapon hits are not
 * custom damage, so they can never be mistaken for ability hits even
 * inside the window.
 *
 * Pure JVM (tick values are supplied by the caller), so the window
 * semantics are unit-testable.
 */
public final class AbilityHitPairing {

    private record Pair(UUID casterId, UUID eliteId) {
    }

    private final int windowTicks;
    private final Map<Pair, Long> expiryTickByPair = new HashMap<>();

    public AbilityHitPairing(int windowTicks) {
        this.windowTicks = Math.max(1, windowTicks);
    }

    /** Records that an ability damage step from caster is about to
     *  land on elite. Refreshes the window on repeated casts. */
    public synchronized void mark(UUID casterId, UUID eliteId, long tick) {
        if (casterId == null || eliteId == null) return;
        sweep(tick);
        expiryTickByPair.put(new Pair(casterId, eliteId), tick + windowTicks);
    }

    /** Whether a damage event for this pair at this tick belongs to a
     *  marked ability cast. The mark is NOT consumed: one cast can
     *  legitimately land several damage steps (a volley), and every
     *  step inside the window is that cast's own damage. */
    public synchronized boolean isAuthorized(UUID casterId, UUID eliteId, long tick) {
        if (casterId == null || eliteId == null) return false;
        Long expiry = expiryTickByPair.get(new Pair(casterId, eliteId));
        if (expiry == null) return false;
        if (tick > expiry) {
            expiryTickByPair.remove(new Pair(casterId, eliteId));
            return false;
        }
        return true;
    }

    public synchronized void clear() {
        expiryTickByPair.clear();
    }

    private void sweep(long tick) {
        Iterator<Map.Entry<Pair, Long>> it = expiryTickByPair.entrySet().iterator();
        while (it.hasNext()) {
            if (tick > it.next().getValue()) it.remove();
        }
    }
}
