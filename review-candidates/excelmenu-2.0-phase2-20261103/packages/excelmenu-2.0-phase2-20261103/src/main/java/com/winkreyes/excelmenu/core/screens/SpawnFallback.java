// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.screens;

/**
 * The Spawn door's fallback rules, ported from ExcelBedrockMenu
 * 1.5.28 (teleportToSpawn / villageFallback): the confirmed Spawn
 * runs the real EssentialsX spawn from the console; if that command
 * is unavailable, or one tick later the player is still in the guild
 * world, the player is taken to the configured village point in the
 * overworld instead. The coordinates are config keys in ExcelMenu's
 * config.yml (spawn.x / spawn.z — the same keys BedrockMenu used),
 * with these settled defaults.
 */
public final class SpawnFallback {

    public static final String GUILD_WORLD = "em_adventurers_guild";
    public static final double DEFAULT_X = -784;
    public static final double DEFAULT_Z = -272;
    /** Placeholder Y only — the fallback always lands on the highest
     *  block, like the original. */
    public static final double PLACEHOLDER_Y = 70;
    public static final String UNAVAILABLE_MESSAGE =
            "Spawn is unavailable right now. Ask an admin.";

    /** True when the console /spawn did not work and the village
     *  fallback must run. */
    public static boolean needsSpawnFallback(boolean commandDispatched,
                                             String worldNameAfter) {
        return !commandDispatched || GUILD_WORLD.equals(worldNameAfter);
    }

    /** True if a world may be used as the fallback target: a normal
     *  overworld that is not the guild world. */
    public static boolean isSpawnWorld(boolean normalEnvironment, String name) {
        return normalEnvironment && !GUILD_WORLD.equals(name);
    }

    private SpawnFallback() {}
}
