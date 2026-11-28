package excelemfixes.bedrockabilities;

import com.magmaguy.elitemobs.advancedcombat.AdvancedCombatModule;

/**
 * The only class the plugin's startup path uses to touch EliteMobs, so
 * that a moved or removed EliteMobs internal (an EliteMobs update
 * rearranging its unpublished advancedcombat package) surfaces here as
 * a clean UNAVAILABLE instead of a class-loading failure in the main
 * plugin class. Every call is guarded with Throwable: NoClassDefFoundError
 * and friends are an expected, handled outcome of riding on internals.
 */
public final class AcsProbe {

    public enum Result {
        /** Module class present and initialized. */
        READY,
        /** Module class present, but ACS has not initialized. */
        NOT_INITIALIZED,
        /** Module class could not be reached at all (EM absent/moved). */
        UNAVAILABLE
    }

    private AcsProbe() {}

    public static Result probe() {
        try {
            return AdvancedCombatModule.isInitialized() ? Result.READY : Result.NOT_INITIALIZED;
        } catch (Throwable t) {
            return Result.UNAVAILABLE;
        }
    }
}
