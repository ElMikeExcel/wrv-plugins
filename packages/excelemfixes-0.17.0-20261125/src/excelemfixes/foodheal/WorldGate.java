package excelemfixes.foodheal;

/**
 * The EM-world gate as a pure decision (unit-testable): a world is
 * in scope when EliteMobs' own registry claims it OR the config
 * world-name list names it. Overworld / nether / end satisfy neither
 * signal on WRV. The Bukkit layer supplies both booleans.
 */
public final class WorldGate {

    private WorldGate() {}

    public static boolean isEliteMobsWorld(boolean registrySays, boolean listSays) {
        return registrySays || listSays;
    }
}
