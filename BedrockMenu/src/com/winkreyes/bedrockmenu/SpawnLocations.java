package com.winkreyes.bedrockmenu;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.List;

/** Spawn lookup without any compile-time Essentials dependency. */
final class SpawnLocations {
    private static final int VILLAGE_X = -784;
    private static final int VILLAGE_Z = -272;

    private SpawnLocations() {}

    static Location essentialsSpawn(PluginManager plugins) throws ReflectiveOperationException {
        Plugin spawn = plugins.getPlugin("EssentialsSpawn");
        if (spawn == null || !spawn.isEnabled()) {
            spawn = plugins.getPlugin("EssentialsXSpawn");
        }
        if (spawn == null || !spawn.isEnabled()) {
            return null;
        }
        // getSpawn(String) belongs to EssentialsSpawn, not the Essentials core.
        Object result = spawn.getClass().getMethod("getSpawn", String.class)
                .invoke(spawn, "default");
        if (result instanceof Location location && location.getWorld() != null) {
            return location.clone();
        }
        return null;
    }

    static Location villageFallback(List<World> worlds, String configuredWorld) {
        World world = selectFallbackWorld(worlds, configuredWorld);
        int y = world.getHighestBlockYAt(VILLAGE_X, VILLAGE_Z) + 1;
        if (y < world.getMinHeight() || y + 1 >= world.getMaxHeight()) {
            throw new IllegalStateException("Village surface leaves no standing room in " + world.getName());
        }
        return new Location(world, VILLAGE_X, y, VILLAGE_Z);
    }

    static World selectFallbackWorld(List<World> worlds, String configuredWorld) {
        String name = configuredWorld == null ? "" : configuredWorld.trim();
        for (World world : worlds) {
            if (world.getEnvironment() == World.Environment.NORMAL
                    && (name.isEmpty() || world.getName().equals(name))) {
                return world;
            }
        }
        throw new IllegalStateException(name.isEmpty()
                ? "No loaded overworld is available"
                : "Configured fallback-world '" + name + "' is missing or is not an overworld");
    }
}
