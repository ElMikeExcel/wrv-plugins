package excelemfixes.timedevents;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * One timed event's tracking state, pure (the bridge owns the
 * live entities; this class owns the bookkeeping): which event
 * file it is, which boss files belong to it, which spawned
 * bosses have been bound to it, and which of those are gone.
 *
 * Lifecycle: created PENDING when EliteMobs fires its
 * custom-event start signal (the boss may still be seconds
 * away — EliteMobs searches for a spawn location first). The
 * first bound boss makes it ACTIVE and earns the broadcast.
 * When every bound boss is gone, or the failsafe age passes,
 * the bridge closes and forgets it.
 */
public final class TrackedEvent {

    private final String eventFilename;
    private final Set<String> bossFilenames = new HashSet<>();
    private final Set<UUID> boundBosses = new HashSet<>();
    private final Set<UUID> goneBosses = new HashSet<>();
    private final long startedAtMillis;
    private boolean broadcastSent;

    public TrackedEvent(String eventFilename, Iterable<String> bossFilenames,
                        long startedAtMillis) {
        this.eventFilename = eventFilename;
        if (bossFilenames != null) {
            for (String name : bossFilenames) {
                if (name != null) this.bossFilenames.add(name.toLowerCase(Locale.ROOT));
            }
        }
        this.startedAtMillis = startedAtMillis;
    }

    public String eventFilename() {
        return eventFilename;
    }

    /** Does this boss config file belong to this event? */
    public boolean matchesBoss(String bossFilename) {
        return bossFilename != null
                && bossFilenames.contains(bossFilename.toLowerCase(Locale.ROOT));
    }

    /** Bind a spawned boss entity to this event. */
    public void bind(UUID entityId) {
        if (entityId != null) boundBosses.add(entityId);
    }

    public boolean isBound(UUID entityId) {
        return boundBosses.contains(entityId);
    }

    public boolean hasBound() {
        return !boundBosses.isEmpty();
    }

    /** A snapshot of the bound boss entity ids. */
    public Set<UUID> boundBosses() {
        return Set.copyOf(boundBosses);
    }

    /** Record a bound boss as dead / removed / invalid. */
    public void markGone(UUID entityId) {
        if (boundBosses.contains(entityId)) goneBosses.add(entityId);
    }

    public boolean isGone(UUID entityId) {
        return goneBosses.contains(entityId);
    }

    /** The event is over when it has bosses and all are gone. */
    public boolean allBoundGone() {
        return !boundBosses.isEmpty() && goneBosses.containsAll(boundBosses);
    }

    public boolean broadcastSent() {
        return broadcastSent;
    }

    public void markBroadcastSent() {
        broadcastSent = true;
    }

    /** A pending event that never produced a boss is dropped. */
    public boolean pendingExpired(long nowMillis, long timeoutMillis) {
        return !hasBound() && nowMillis - startedAtMillis > timeoutMillis;
    }

    /** The failsafe: no tracked event outlives this age. */
    public boolean expired(long nowMillis, long maxMillis) {
        return nowMillis - startedAtMillis > maxMillis;
    }
}
