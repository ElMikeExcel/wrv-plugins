package com.winkreyes.bedrockmenu;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpawnLocationsTest {
    /** Represents the real EssentialsSpawn public API without a dependency. */
    public interface SpawnLookup {
        Location getSpawn(String group);
    }

    @Test
    void fallbackSkipsNetherAndEndEvenWhenTheyWereLoadedFirst() {
        World nether = world("world_nether", World.Environment.NETHER);
        World end = world("world_the_end", World.Environment.THE_END);
        World overworld = world("world", World.Environment.NORMAL);
        assertSame(overworld, SpawnLocations.selectFallbackWorld(List.of(nether, end, overworld), ""));
    }

    @Test
    void explicitFallbackCanSelectAnotherOverworld() {
        World first = world("world", World.Environment.NORMAL);
        World village = world("village", World.Environment.NORMAL);
        assertSame(village, SpawnLocations.selectFallbackWorld(List.of(first, village), " village "));
    }

    @Test
    void absentOrNonOverworldConfigurationFailsRatherThanChangingWorlds() {
        World nether = world("world_nether", World.Environment.NETHER);
        World overworld = world("world", World.Environment.NORMAL);
        assertThrows(IllegalStateException.class,
                () -> SpawnLocations.selectFallbackWorld(List.of(nether, overworld), "missing"));
        assertThrows(IllegalStateException.class,
                () -> SpawnLocations.selectFallbackWorld(List.of(nether, overworld), "world_nether"));
    }

    @Test
    void noOverworldFailsClosed() {
        assertThrows(IllegalStateException.class,
                () -> SpawnLocations.selectFallbackWorld(List.of(), ""));
        assertThrows(IllegalStateException.class,
                () -> SpawnLocations.selectFallbackWorld(List.of(world("nether", World.Environment.NETHER)), null));
    }

    @Test
    void villageUsesOriginalCoordinatesAndSelectedWorldSurface() {
        World overworld = world("world", World.Environment.NORMAL);
        when(overworld.getHighestBlockYAt(-784, -272)).thenReturn(82);
        when(overworld.getMinHeight()).thenReturn(-64);
        when(overworld.getMaxHeight()).thenReturn(320);
        Location destination = SpawnLocations.villageFallback(List.of(overworld), "");
        assertSame(overworld, destination.getWorld());
        assertEquals(-784, destination.getX());
        assertEquals(83, destination.getY());
        assertEquals(-272, destination.getZ());
    }

    @Test
    void villageRejectsSurfaceWithoutPlayerHeadroom() {
        World overworld = world("world", World.Environment.NORMAL);
        when(overworld.getHighestBlockYAt(-784, -272)).thenReturn(318);
        when(overworld.getMinHeight()).thenReturn(-64);
        when(overworld.getMaxHeight()).thenReturn(320);
        assertThrows(IllegalStateException.class,
                () -> SpawnLocations.villageFallback(List.of(overworld), ""));
    }

    @Test
    void spawnCallsCorrectPluginApiAndReturnsIndependentLocation() throws Exception {
        World overworld = world("world", World.Environment.NORMAL);
        Location original = new Location(overworld, 11, 72, 42, 90, 0);
        Plugin spawn = spawnPlugin(original);
        PluginManager plugins = mock(PluginManager.class);
        when(plugins.getPlugin("EssentialsSpawn")).thenReturn(spawn);
        Location destination = SpawnLocations.essentialsSpawn(plugins);
        assertEquals(original, destination);
        assertNotSame(original, destination);
        verify((SpawnLookup) spawn).getSpawn("default");
        verify(plugins, never()).getPlugin("Essentials");
    }

    @Test
    void legacyPluginNameIsSupported() throws Exception {
        PluginManager plugins = mock(PluginManager.class);
        Location location = new Location(world("world", World.Environment.NORMAL), 0, 70, 0);
        Plugin spawn = spawnPlugin(location);
        when(plugins.getPlugin("EssentialsXSpawn")).thenReturn(spawn);
        assertEquals(location, SpawnLocations.essentialsSpawn(plugins));
    }

    @Test
    void missingOrDisabledSpawnPluginRequestsVillageFallback() throws Exception {
        PluginManager plugins = mock(PluginManager.class);
        assertNull(SpawnLocations.essentialsSpawn(plugins));
        Plugin disabled = mock(Plugin.class);
        when(plugins.getPlugin("EssentialsSpawn")).thenReturn(disabled);
        assertNull(SpawnLocations.essentialsSpawn(plugins));
    }

    @Test
    void nullOrUnavailableWorldRequestsVillageFallback() throws Exception {
        PluginManager plugins = mock(PluginManager.class);
        Plugin nullLocation = spawnPlugin(null);
        when(plugins.getPlugin("EssentialsSpawn")).thenReturn(nullLocation);
        assertNull(SpawnLocations.essentialsSpawn(plugins));
        Plugin nullWorld = spawnPlugin(new Location(null, 0, 70, 0));
        when(plugins.getPlugin("EssentialsSpawn")).thenReturn(nullWorld);
        assertNull(SpawnLocations.essentialsSpawn(plugins));
    }

    @Test
    void incompatibleSpawnApiIsReportedToCaller() {
        PluginManager plugins = mock(PluginManager.class);
        Plugin incompatible = mock(Plugin.class);
        when(incompatible.isEnabled()).thenReturn(true);
        when(plugins.getPlugin("EssentialsSpawn")).thenReturn(incompatible);
        assertThrows(NoSuchMethodException.class, () -> SpawnLocations.essentialsSpawn(plugins));
    }

    private static Plugin spawnPlugin(Location location) {
        Plugin plugin = mock(Plugin.class, withSettings().extraInterfaces(SpawnLookup.class));
        when(plugin.isEnabled()).thenReturn(true);
        when(((SpawnLookup) plugin).getSpawn("default")).thenReturn(location);
        return plugin;
    }

    private static World world(String name, World.Environment environment) {
        World world = mock(World.class);
        when(world.getName()).thenReturn(name);
        when(world.getEnvironment()).thenReturn(environment);
        return world;
    }
}
