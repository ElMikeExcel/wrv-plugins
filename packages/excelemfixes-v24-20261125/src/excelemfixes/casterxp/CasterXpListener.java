package excelemfixes.casterxp;

import com.magmaguy.elitemobs.api.EliteSkillXpGainEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listens for EliteMobs' own skill-XP-gain event and applies
 * the player's sharing mode to the gain when the gained
 * skill belongs to the foundation pair of the player's
 * ACTIVE FORM (resolved per event: the form from EliteMobs'
 * own profile, the pair from ClassFormTable). This class is
 * the only other EliteMobs-touching class in the module
 * (besides the bridge); the module instantiates and
 * registers it ONLY after confirming EliteMobs is present
 * and its bridge probes, so the module class itself never
 * links an EliteMobs type and the module idles cleanly
 * without EliteMobs — the pack's isolation pattern.
 *
 * WHY MONITOR PRIORITY (a deliberate, documented exception
 * to "MONITOR observes only"): EliteMobs' SkillXPHandler
 * fires EliteSkillXpGainEvent BEFORE applying the gain, and
 * applies exactly {@code event.getXp()} afterwards
 * (bytecode-verified against the 10.9.10 jar, Nov 25,
 * 2026). The final XP value is therefore only known at
 * MONITOR, after every other listener's modifications —
 * and SHARED works by trimming that very value with
 * {@code setXp} and granting the trimmed part to the other
 * track, so this listener must run last-but-one (nothing
 * runs after MONITOR). PlayerData still holds pre-gain
 * totals inside the event, which is what the CARRY math is
 * defined against. Grants go through PlayerData.addSkillXP,
 * which fires no event, so nothing here can recurse.
 *
 * Any failure is caught and logged: a sharing failure must
 * never break or eat a player's ordinary XP gain, so on any
 * error the event is left exactly as EliteMobs made it.
 * The same stand-down applies by design whenever the
 * player's form is unknown, the form is not in the probed
 * catalog, or the gained skill is not one of the form's
 * two foundation skills: EliteMobs behaves exactly as
 * shipped.
 */
public final class CasterXpListener implements Listener {

    private final CasterXpState state;
    private final Logger logger;
    private volatile boolean debug;

    public CasterXpListener(CasterXpState state, Logger logger) {
        this.state = state;
        this.logger = logger;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSkillXpGain(EliteSkillXpGainEvent event) {
        try {
            long gain = event.getXp();
            if (gain <= 0) return;
            UUID uuid = event.getPlayer().getUniqueId();
            CasterXpMath.Mode mode = state.modeOf(uuid);
            if (mode == CasterXpMath.Mode.STOCK) return;

            String gained = event.getSkillType().name();
            String formId = EmCasterBridge.activeFormId(uuid);
            if (formId == null) return;
            String[] pair = ClassFormTable.pairOf(formId);
            if (pair == null) return;
            String other = CasterXpMath.otherInPair(pair[0], pair[1], gained);
            if (other == null) return; // not a foundation skill of the form

            // Pre-gain totals: EliteMobs applies the event's
            // XP only after the event returns.
            long gainedBefore = EmCasterBridge.readXp(uuid, gained);
            long otherBefore = EmCasterBridge.readXp(uuid, other);

            switch (mode) {
                case SHARED -> {
                    long[] split = CasterXpMath.splitGain(gain);
                    if (split[1] > 0) {
                        EmCasterBridge.addXp(uuid, other, split[1]);
                    }
                    if (split[0] != gain) {
                        event.setXp(split[0]);
                    }
                    if (debug) {
                        logger.info("Class XP Sharing: " + gained
                                + " gain " + gain + " split for "
                                + event.getPlayer().getName() + " — "
                                + split[0] + " kept, " + split[1]
                                + " to " + other);
                    }
                }
                case CARRY -> {
                    long mirror = CasterXpMath.carryMirror(
                            gainedBefore, otherBefore, gain);
                    if (mirror > 0) {
                        EmCasterBridge.addXp(uuid, other, mirror);
                        if (debug) {
                            logger.info("Class XP Sharing: " + gained
                                    + " gain " + gain + " mirrored "
                                    + mirror + " to " + other + " for "
                                    + event.getPlayer().getName());
                        }
                    }
                }
                default -> { /* STOCK handled above */ }
            }
        } catch (Throwable t) {
            logger.log(Level.WARNING,
                    "Class XP Sharing: gain handling failed, left the"
                            + " gain untouched: " + t, t);
        }
    }
}
