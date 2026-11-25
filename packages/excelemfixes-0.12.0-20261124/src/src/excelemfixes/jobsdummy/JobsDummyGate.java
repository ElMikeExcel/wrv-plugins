package excelemfixes.jobsdummy;

import com.gamingmesh.jobs.api.JobsExpGainEvent;
import com.gamingmesh.jobs.api.JobsPrePaymentEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * The veto itself: LOWEST handlers on Jobs' own payment events
 * (the pattern proven by ExcelTrainingDummy 1.0.2's JobsGate),
 * keyed on EliteMobs boss identity instead of a plugin's own
 * dummy stamp.
 *
 * <p>Why this layer: Jobs pays a Kill by ENTITY TYPE from its
 * EntityDeathEvent handler at MONITOR priority, and a death
 * event cannot be cancelled — so no damage- or death-layer
 * handling can stop the payment. Jobs does expose a cancellable
 * gate of its own: every payment passes through
 * JobsPrePaymentEvent (money / points / XP) and every XP grant
 * through JobsExpGainEvent, and both events carry the entity
 * the payment is about. Zeroing and cancelling there is
 * absolute.
 *
 * <p>Identity timing: Jobs fires its payment events from inside
 * its MONITOR death handler, and EliteMobs may already have
 * untracked the dead elite by then. The gate therefore resolves
 * identity twice over: a death watch at HIGHEST (which runs
 * before Jobs' MONITOR handler) resolves the dying entity's
 * boss filename while EliteMobs' tracker certainly still holds
 * it and remembers vetoed deaths briefly; the payment handlers
 * check that memory first and fall back to resolving the event's
 * entity live.
 *
 * <p>This class references Jobs API classes directly: it is
 * only class-loaded and registered when the Jobs plugin is
 * present (see JobsDummyGateModule), and Jobs is a softdepend
 * in plugin.yml.
 */
public final class JobsDummyGate implements Listener {

    /** How long a vetoed death is remembered for its payment. */
    private static final long DEATH_MEMORY_MS = 10_000L;

    /** A remembered vetoed death: its boss filename + expiry. */
    private record DeathMark(String filename, long expiryMs) {
    }

    private final Supplier<DummyBossList> list;
    private final Supplier<Boolean> debug;
    private final Logger logger;
    private final Map<UUID, DeathMark> vetoedDeaths = new ConcurrentHashMap<>();

    public JobsDummyGate(Supplier<DummyBossList> list,
                         Supplier<Boolean> debug, Logger logger) {
        this.list = list;
        this.debug = debug;
        this.logger = logger;
    }

    // ------------------------------------------------ death watch

    /**
     * Resolve the dying entity's boss identity while EliteMobs
     * still tracks it, and remember the vetoed ones for the
     * payment events Jobs is about to fire at MONITOR.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(EntityDeathEvent event) {
        String filename = EmBossIdentity.bossFilename(event.getEntity());
        if (filename == null || !list.get().isVetoed(filename)) return;
        long now = System.currentTimeMillis();
        vetoedDeaths.put(event.getEntity().getUniqueId(),
                new DeathMark(filename, now + DEATH_MEMORY_MS));
        vetoedDeaths.values().removeIf(mark -> mark.expiryMs() < now);
        if (Boolean.TRUE.equals(debug.get())) {
            logger.info("[JobsDummyGate] vetoed death remembered: " + filename);
        }
    }

    // ------------------------------------------------------ vetoes

    /** Any Jobs payment (money, points, XP) about a listed dummy is cancelled outright. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPrePayment(JobsPrePaymentEvent event) {
        String filename = aboutListedDummy(event.getLivingEntity(), event.getEntity());
        if (filename == null) return;
        event.setAmount(0);
        event.setPoints(0);
        event.setExp(0);
        event.setCancelled(true);
        logger.info("[JobsDummyGate] Blocked a Jobs payment (job "
                + (event.getJob() == null ? "?" : event.getJob().getName())
                + ") for EliteMobs boss " + filename
                + " — listed training dummies pay nothing.");
    }

    /** The XP grant travels on its own event as well; zero and cancel it too. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onExpGain(JobsExpGainEvent event) {
        String filename = aboutListedDummy(event.getLivingEntity(), event.getEntity());
        if (filename == null) return;
        event.setExp(0);
        event.setCancelled(true);
        logger.info("[JobsDummyGate] Blocked Jobs XP (job "
                + (event.getJob() == null ? "?" : event.getJob().getName())
                + ") for EliteMobs boss " + filename
                + " — listed training dummies pay nothing.");
    }

    // ----------------------------------------------------- identity

    /**
     * Jobs carries the paid-about entity in BOTH accessors
     * depending on the action (for a Kill, getEntity() is the
     * killer player and getLivingEntity() is the mob that died —
     * bytecode-verified against Jobs 5.2.6.3). The veto fires if
     * EITHER is a listed dummy; a player never resolves to a boss
     * filename, so the killer-side value can never false-positive.
     *
     * @return the matched boss filename, or null when neither
     *         entity is a listed dummy
     */
    private String aboutListedDummy(Entity a, Entity b) {
        String hit = listedFilename(a);
        return hit != null ? hit : listedFilename(b);
    }

    private String listedFilename(Entity entity) {
        if (entity == null) return null;
        DeathMark mark = vetoedDeaths.get(entity.getUniqueId());
        if (mark != null) {
            if (mark.expiryMs() >= System.currentTimeMillis()) return mark.filename();
            vetoedDeaths.remove(entity.getUniqueId());
        }
        String filename = EmBossIdentity.bossFilename(entity);
        return filename != null && list.get().isVetoed(filename) ? filename : null;
    }
}
