package com.winkreyes.playermap;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.map.MapView;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MapRegistryTest {
    @TempDir Path temp;
    Server server; World world; MapRegistry registry; Map<Integer,MapView> maps; AtomicInteger next;
    @BeforeEach void setup() throws Exception {
        server = mock(Server.class); world = mock(World.class); when(world.getUID()).thenReturn(new UUID(0,1));
        maps = new HashMap<>(); next = new AtomicInteger();
        when(server.createMap(any())).thenAnswer(invocation -> newMap(invocation.getArgument(0)));
        when(server.getMap(anyInt())).thenAnswer(invocation -> maps.get(invocation.<Integer>getArgument(0)));
        registry = new MapRegistry(server,temp.resolve("maps.yml")); registry.load();
    }
    MapView newMap(World w) {
        MapView map = mock(MapView.class); int id = next.getAndIncrement(); when(map.getId()).thenReturn(id); when(map.getWorld()).thenReturn(w);
        AtomicInteger x = new AtomicInteger(), z = new AtomicInteger(); AtomicReference<MapView.Scale> scale = new AtomicReference<>(MapView.Scale.NORMAL);
        when(map.getCenterX()).thenAnswer(i -> x.get()); when(map.getCenterZ()).thenAnswer(i -> z.get()); when(map.getScale()).thenAnswer(i -> scale.get());
        doAnswer(i -> { x.set(i.getArgument(0)); return null; }).when(map).setCenterX(anyInt());
        doAnswer(i -> { z.set(i.getArgument(0)); return null; }).when(map).setCenterZ(anyInt());
        doAnswer(i -> { scale.set(i.getArgument(0)); return null; }).when(map).setScale(any()); maps.put(id,map); return map;
    }
    @Test void reusesSameIdAtEveryLocationAndAfterRegistryReload() throws Exception {
        UUID owner = UUID.randomUUID(); MapView first = registry.getOrCreate(owner,world,2,0,0);
        assertSame(first,registry.getOrCreate(owner,world,2,2e7,-2e7));
        MapRegistry restarted = new MapRegistry(server,temp.resolve("maps.yml")); restarted.load();
        assertSame(first,restarted.getOrCreate(owner,world,2,2e7,-2e7)); verify(server,times(1)).createMap(world);
        assertEquals(192,first.getCenterX()); assertEquals(192,first.getCenterZ());
        verify(first,never()).removeRenderer(any()); verify(first,never()).setLocked(anyBoolean());
    }
    @Test void ownerWorldAndEachVanillaScaleHaveSeparateFiniteIds() throws Exception {
        UUID owner = UUID.randomUUID();
        for (int scale = 0; scale < 5; scale++) registry.getOrCreate(owner,world,scale,0,0);
        World nether = mock(World.class); when(nether.getUID()).thenReturn(new UUID(0,2));
        registry.getOrCreate(owner,nether,2,0,0); registry.getOrCreate(UUID.randomUUID(),world,2,0,0);
        assertEquals(7,registry.entries().size()); assertEquals(7,next.get());
    }
    @Test void unregisteredAndChangedMapsAreNeverClaimed() throws Exception {
        MapView unrelated = newMap(world); assertFalse(registry.matches(unrelated));
        UUID owner = UUID.randomUUID(); MapView map = registry.getOrCreate(owner,world,2,0,0); map.setCenterX(999);
        assertFalse(registry.matches(map)); assertThrows(java.io.IOException.class, () -> registry.getOrCreate(owner,world,2,0,0));
        assertEquals(2,next.get());
    }
    @Test void malformedRegistryFailsClosedInsteadOfAllocatingReplacementMaps() throws Exception {
        Files.writeString(temp.resolve("maps.yml"),"maps:\n - owner: corrupt\n   id: 1\n");
        assertThrows(InvalidConfigurationException.class, () -> registry.load()); verify(server,never()).createMap(any());
    }
    @Test void invalidRegistryShapesFailClosed() throws Exception {
        for (String invalid : List.of("maps: definitely-not-a-list", "maps: [not-a-map]", "unexpected: 123")) {
            Files.writeString(temp.resolve("maps.yml"),invalid);
            assertThrows(InvalidConfigurationException.class, () -> registry.load());
        }
        verify(server,never()).createMap(any());
    }
    @Test void scaleOutsideVanillaRangeCannotAllocate() {
        assertThrows(IllegalArgumentException.class, () -> registry.getOrCreate(UUID.randomUUID(),world,5,0,0));
        verify(server,never()).createMap(any());
    }
}
