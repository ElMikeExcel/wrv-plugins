package excelemfixes.mount;

import excelemfixes.mount.core.CombatWindow;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The module's combat marks, shared by every mount service.
 * One player's combat state is one fact, whichever charm they
 * reach for: damage taken or dealt while the horse is out must
 * still bar calling the ghast (and the reverse), or the combat
 * gate would have a hole exactly the size of the second charm.
 * The module owns one instance and hands it to both services.
 * The math itself is the pure {@link CombatWindow}.
 */
public final class CombatMarks {

    private final Map<UUID, Long> lastCombatMillis = new HashMap<>();

    public void mark(UUID player, long nowMillis) {
        lastCombatMillis.put(player, nowMillis);
    }

    public void clear(UUID player) {
        lastCombatMillis.remove(player);
    }

    public boolean inCombat(UUID player, long nowMillis, long windowMillis) {
        return CombatWindow.inCombat(
                lastCombatMillis.getOrDefault(player, 0L), nowMillis,
                windowMillis);
    }

    public long remainingSeconds(UUID player, long nowMillis,
                                 long windowMillis) {
        return CombatWindow.remainingSeconds(
                lastCombatMillis.getOrDefault(player, 0L), nowMillis,
                windowMillis);
    }
}
