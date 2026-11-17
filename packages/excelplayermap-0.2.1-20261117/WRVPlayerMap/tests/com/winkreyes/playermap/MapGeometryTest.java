package com.winkreyes.playermap;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
class MapGeometryTest {
    @Test void centerAndAxesAreNorthUpAtEveryScale() {
        for (int scale = 0; scale < 5; scale++) {
            int blocks = 1 << scale;
            assertEquals(64, MapGeometry.project(1000, -2000, 1000, -2000, scale).x());
            assertEquals(54, MapGeometry.project(1000, -2000 - 10 * blocks, 1000, -2000, scale).y());
            assertEquals(74, MapGeometry.project(1000 + 10 * blocks, -2000, 1000, -2000, scale).x());
        }
    }
    @Test void distantArrowsPointAtEdgesWithoutWrappingOrClipping() {
        assertEquals(123, MapGeometry.project(1e8, 0, 0, 0, 0).x());
        assertEquals(4, MapGeometry.project(-1e8, 0, 0, 0, 0).x());
        assertEquals(4, MapGeometry.project(0, -1e8, 0, 0, 0).y());
        assertEquals(123, MapGeometry.project(0, 1e8, 0, 0, 0).y());
        var diagonal = MapGeometry.project(1e8, 1e8, 0, 0, 0);
        assertTrue(diagonal.edge()); assertEquals(diagonal.x(), diagonal.y());
        assertFalse(MapGeometry.project(63.99, 0, 0, 0, 0).edge());
        assertTrue(MapGeometry.project(64, 0, 0, 0, 0).edge());
        assertFalse(MapGeometry.project(-64, 0, 0, 0, 0).edge());
        assertTrue(MapGeometry.project(-64.01, 0, 0, 0, 0).edge());
    }
    @Test void centersUseVanillaGridIncludingNegativeCoordinates() {
        assertEquals(0, MapGeometry.vanillaCenter(0, 0));
        assertEquals(-128, MapGeometry.vanillaCenter(-65, 0));
        assertEquals(192, MapGeometry.vanillaCenter(0, 2));
        assertEquals(-320, MapGeometry.vanillaCenter(-65, 2));
    }
    @Test void allFiniteWorldCoordinatesStayInsideFrame() {
        Random random = new Random(991);
        for (int i = 0; i < 20000; i++) {
            var p = MapGeometry.project(random.nextDouble(-3e7, 3e7), random.nextDouble(-3e7, 3e7), 192, -320, i % 5);
            assertTrue(p.x() >= 4 && p.x() <= 123); assertTrue(p.y() >= 4 && p.y() <= 123);
        }
        assertThrows(IllegalArgumentException.class, () -> MapGeometry.project(Double.NaN, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> MapGeometry.project(0, 0, 0, 0, 5));
    }
}
