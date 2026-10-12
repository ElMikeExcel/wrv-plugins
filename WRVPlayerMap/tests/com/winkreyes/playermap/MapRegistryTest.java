package com.winkreyes.playermap;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.map.MapView;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MapRegistryTest {
    @TempDir Path temp;
    World world; MapRegistry registry;
    @BeforeEach void setup() throws Exception {
        world = mock(World.class); when(world.getUID()).thenReturn(new UUID(0,1));
        registry = new MapRegistry(temp.resolve("maps.yml")); registry.load();
    }
    MapView map(int id, int scale, int x, int z) {
        MapView map = mock(MapView.class); when(map.getId()).thenReturn(id); when(map.getWorld()).thenReturn(world);
        when(map.getScale()).thenReturn(MapView.Scale.values()[scale]); when(map.getCenterX()).thenReturn(x); when(map.getCenterZ()).thenReturn(z); return map;
    }
    @Test void recordsExactVanillaIdentityWithoutChangingAnyMapProperties() throws Exception {
        MapView initial = map(10,0,-128,256); registry.trackInitial(initial); registry.trackInitial(initial);
        assertEquals(1,registry.entries().size()); assertTrue(registry.matches(initial));
        verify(initial,never()).setScale(any()); verify(initial,never()).setCenterX(anyInt()); verify(initial,never()).setCenterZ(anyInt());
        verify(initial,never()).setWorld(any()); verify(initial,never()).setLocked(anyBoolean()); verify(initial,never()).setTrackingPosition(anyBoolean());
        MapRegistry restarted = new MapRegistry(temp.resolve("maps.yml")); restarted.load();
        assertTrue(restarted.matches(initial)); assertEquals(10,restarted.entry(10).root());
    }
    @Test void allNormalZoomLevelsKeepRootLineageWithoutRequiringIntermediateHold() throws Exception {
        registry.trackInitial(map(10,0,-128,256));
        for (int scale = 1; scale <= 4; scale++) {
            MapView zoom = map(10+scale,scale,MapGeometry.vanillaCenter(-128,scale),MapGeometry.vanillaCenter(256,scale));
            assertTrue(registry.trackZoom(zoom,10)); assertTrue(registry.matches(zoom)); assertEquals(10,registry.entry(10+scale).root());
        }
        assertEquals(5,registry.entries().size());
    }
    @Test void clonesReuseSameIdentity() throws Exception {
        MapView initial = map(10,0,0,0); registry.trackInitial(initial);
        assertTrue(registry.trackZoom(initial,10)); assertEquals(1,registry.entries().size());
    }
    @Test void missingLineageWrongRegionWorldOlderIdsAndScaleDoNotClaimMaps() throws Exception {
        registry.trackInitial(map(10,0,0,0));
        assertFalse(registry.trackZoom(map(11,1,64,64),999));
        assertFalse(registry.trackZoom(map(11,1,1000,64),10));
        assertFalse(registry.trackZoom(map(9,1,64,64),10));
        assertFalse(registry.trackZoom(map(11,0,0,0),10));
        MapView other = map(11,1,64,64); World nether = mock(World.class); when(nether.getUID()).thenReturn(new UUID(0,2)); when(other.getWorld()).thenReturn(nether);
        assertFalse(registry.trackZoom(other,10)); assertEquals(1,registry.entries().size());
    }
    @Test void lockedCopiesRemainPlainAndVirtualMapsAreNeverClaimed() throws Exception {
        registry.trackInitial(map(10,0,0,0)); MapView locked = map(11,0,0,0); when(locked.isLocked()).thenReturn(true);
        assertFalse(registry.trackZoom(locked,10)); assertThrows(java.io.IOException.class, () -> registry.trackInitial(locked));
        MapView virtual = map(12,1,64,64); when(virtual.isVirtual()).thenReturn(true);
        assertFalse(registry.trackZoom(virtual,10)); assertThrows(java.io.IOException.class, () -> registry.trackInitial(virtual));
    }
    @Test void changedRegisteredMetadataFailsClosed() throws Exception {
        MapView initial = map(10,0,0,0); registry.trackInitial(initial); when(initial.getCenterX()).thenReturn(999);
        assertFalse(registry.matches(initial)); assertFalse(registry.trackZoom(initial,10)); assertThrows(java.io.IOException.class, () -> registry.trackInitial(initial));
    }
    @Test void malformedAndLegacyRegistriesFailClosed() throws Exception {
        for (String invalid : List.of("maps: []", "format: 2\nmaps: invalid", "format: 2\nmaps: [not-a-map]", "format: 2\nmaps:\n - id: 1.5\n", "unexpected: 123")) {
            Files.writeString(temp.resolve("maps.yml"),invalid); assertThrows(InvalidConfigurationException.class, () -> registry.load());
        }
    }
    @Test void impossiblePersistedZoomAncestryFailsClosed() throws Exception {
        registry.trackInitial(map(10,0,0,0)); registry.trackZoom(map(11,1,64,64),10);
        String valid = Files.readString(temp.resolve("maps.yml"));
        for (String invalid : List.of(valid.replace("center-x: 64","center-x: 1000"), valid.replace("scale: 1","scale: 0"), valid.replace("id: 11","id: 9"), valid.replace("scale: 0","scale: 2"))) {
            Files.writeString(temp.resolve("maps.yml"),invalid);
            assertThrows(InvalidConfigurationException.class, () -> new MapRegistry(temp.resolve("maps.yml")).load());
        }
    }
    @Test void malformedRootReferenceFailsClosedOnRestart() throws Exception {
        registry.trackInitial(map(10,0,0,0)); String text = Files.readString(temp.resolve("maps.yml")).replace("root: 10","root: 999");
        Files.writeString(temp.resolve("maps.yml"),text); assertThrows(InvalidConfigurationException.class, () -> new MapRegistry(temp.resolve("maps.yml")).load());
    }
}
