package excelemfixes.mount.core;

/**
 * The combat-window math as a pure decision (unit-testable),
 * mirroring the notion EliteMobs' ACS and the ExcelEMFixes
 * food-heal module use: a player is "in combat" from their last
 * combat mark until the window (default 20 s) has fully elapsed.
 * Summoning is refused inside the window; despawning never is.
 */
public final class CombatWindow {

    private CombatWindow() {}

    /** True when a combat mark exists and the window has not elapsed. */
    public static boolean inCombat(long lastCombatMillis, long nowMillis,
                                   long windowMillis) {
        return lastCombatMillis > 0 && nowMillis - lastCombatMillis < windowMillis;
    }

    /** Whole seconds of combat window left (0 when out of combat). */
    public static long remainingSeconds(long lastCombatMillis, long nowMillis,
                                        long windowMillis) {
        if (!inCombat(lastCombatMillis, nowMillis, windowMillis)) return 0;
        long remaining = windowMillis - (nowMillis - lastCombatMillis);
        return (remaining + 999) / 1000;
    }
}
