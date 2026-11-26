package excelemfixes.mount.core;

import java.util.List;

/**
 * The mount world gate as a pure decision (unit-testable): a world
 * is BLOCKED for summoning when its name is on the blocked list or
 * starts with a blocked prefix; every other world is an open world
 * where the charm works. Name rules only, so a future open
 * adventure world works the day it lands with no config edit, and
 * EliteMobs' instanced worlds — always named em_id_* — are blocked
 * by construction.
 *
 * <p>Defaults (config): prefix {@code em_id_} (EM instanced
 * dungeons/arenas), name {@code em_adventurers_guild} (the guild
 * hub is an interior social space; horses there are clutter).
 */
public final class WorldRules {

    private WorldRules() {}

    public static boolean isBlocked(String worldName,
                                      List<String> blockedPrefixes,
                                      List<String> blockedNames) {
        if (worldName == null) return true;
        if (blockedNames != null) {
            for (String name : blockedNames) {
                if (name != null && name.equals(worldName)) return true;
            }
        }
        if (blockedPrefixes != null) {
            for (String prefix : blockedPrefixes) {
                if (prefix != null && !prefix.isEmpty()
                        && worldName.startsWith(prefix)) return true;
            }
        }
        return false;
    }
}
