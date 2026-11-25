package excelemfixes.mcmmo;

import java.util.UUID;

/**
 * The module's window into mcMMO, behind an interface so the
 * engine never touches an mcMMO type: the implementing class
 * (the bridge, with its gain-event listeners) is only
 * constructed after the module has confirmed the mcMMO plugin
 * is present.
 *
 * <p>Callbacks the bridge needs from the engine are declared
 * here too ({@link Hooks}), so the bridge never depends on the
 * engine class either.
 */
public interface XpPort {

    /** Engine decisions the bridge's listeners consult. */
    interface Hooks {

        /** True when this PVE gain matches a planted EliteMobs hit. */
        boolean vetoGain(UUID player, String skill);

        /** True when this SHARED_PVE share derives from an EliteMobs hit. */
        boolean vetoShare(UUID receiver, String skill);

        /**
         * Every gain event the pipeline finally wrote (any
         * player, any skill, post-everything amount), reported
         * by the bridge's MONITOR observer. Feeds the XP
         * watchdog's rolling ledgers.
         */
        void observedGain(UUID player, String skill, float amount,
                          String worldName);

        /**
         * True when the watchdog's hourly cap currently blocks
         * this player+skill: the bridge's veto listener zeroes
         * the gain. Cheap by contract — a precomputed
         * blocked-until state, never a ledger scan per event.
         */
        boolean watchdogCapped(UUID player, String skill);

        /**
         * The final (post-pipeline) amount of one of the
         * module's own award grants, observed after every
         * listener has run — the figure the ceiling counts.
         */
        void grantedFinal(UUID player, String skill, float amount);
    }

    /**
     * Grants award XP through mcMMO's normal gain pipeline
     * (party sharing, rate modifiers and every other plugin's
     * gain listeners compose exactly as for any combat gain).
     * Returns false when the player is offline or not loaded
     * in mcMMO; the caller queues the award instead. A grant
     * may be attempted in any world: on an mcMMO build that
     * still gates no-XP worlds in-pipeline the pipeline
     * silently pays 0 there and fires no event, which the
     * caller detects (no observed grant) and queues instead.
     */
    boolean grant(UUID player, String skill, int amount);

    /** True when both players are in the same mcMMO party. */
    boolean sameParty(UUID first, UUID second);
}
