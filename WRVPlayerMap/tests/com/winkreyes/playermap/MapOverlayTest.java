package com.winkreyes.playermap;
import org.bukkit.map.MapCanvas;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MapOverlayTest {
    private List<MapOverlay.PlayerDot> family(int count, boolean overlap) {
        List<MapOverlay.PlayerDot> players = new ArrayList<>();
        for (int i = 0; i < count; i++) players.add(new MapOverlay.PlayerDot(new UUID(0,i), "Family" + i,
                overlap ? 0 : (i % 3 - 1) * 38, overlap ? 0 : (i / 3 - 1) * 30, i * 45, i == 0));
        return players;
    }
    @Test void labelsAllEightFamilyMembersEvenAtOneLocation() {
        var result = MapOverlay.render(family(8,true), 0,0,0,"",0);
        assertEquals(8, result.labeled()); assertEquals(0, result.overflow());
        assertTrue(Arrays.stream(toInts(result.frame().pixels)).filter(x -> x != 0).count() < 8000);
    }
    @Test void emptyOverlayLeavesEveryTerrainPixelTransparent() {
        assertArrayEquals(new byte[16384], MapOverlay.render(List.of(),0,0,0,"",0).frame().pixels);
    }
    @Test void removalAndMovementChangePixelOverlayWithoutPaintingTerrain() {
        byte[] before = MapOverlay.render(family(2,false),0,0,0,"",0).frame().pixels;
        byte[] after = MapOverlay.render(family(1,false),0,0,0,"",1).frame().pixels;
        int cleared = 0; for (int i = 0; i < before.length; i++) if (before[i] != 0 && after[i] == 0) cleared++;
        assertTrue(cleared > 20);
        MapCanvas canvas = mock(MapCanvas.class);
        assertTrue(PlayerMapRenderer.apply(canvas,before,after) > 20);
        verify(canvas, atLeastOnce()).setPixelColor(anyInt(), anyInt(), isNull());
        clearInvocations(canvas); assertEquals(0, PlayerMapRenderer.apply(canvas,after,after)); verifyNoInteractions(canvas);
    }
    @Test void firstRenderClearsAnyStaleCanvasAfterReconnectCachePurge() {
        MapCanvas canvas = mock(MapCanvas.class);
        assertEquals(16384, PlayerMapRenderer.apply(canvas,null,new byte[16384]));
        verify(canvas,times(16384)).setPixelColor(anyInt(),anyInt(),isNull());
    }
    @Test void renderIsDeterministicAndUnsupportedFormattingCannotEscapeLabels() {
        assertArrayEquals(MapOverlay.render(family(6,false),0,0,0,"Other worlds: 2",0).frame().pixels,
                MapOverlay.render(family(6,false),0,0,0,"Other worlds: 2",0).frame().pixels);
        assertEquals("name?12;?", PixelFrame.safeName("name\u00a712;\n"));
        assertTrue(PixelFrame.width(PixelFrame.fit("VeryLongBedrockPlayerName000",100)) <= 100);
    }
    @Test void crowdedMapsReportOverflowAndCycleNamePriority() {
        var a = MapOverlay.render(family(40,true),0,0,0,"",0);
        var b = MapOverlay.render(family(40,true),0,0,0,"",3);
        assertTrue(a.overflow() > 0); assertEquals(40, a.labeled() + a.overflow());
        assertFalse(Arrays.equals(a.frame().pixels,b.frame().pixels));
    }
    @Test void sixLongNamesCycleWhenTheyOverflow() {
        int[][] coordinates = {{18,-44},{-30,-17},{-19,17},{63,49},{-3,-31},{-10,56}};
        List<MapOverlay.PlayerDot> players = new ArrayList<>();
        for (int i = 0; i < 6; i++) players.add(new MapOverlay.PlayerDot(new UUID(0,i), "WWWWWWWWWWWWWWW" + i, coordinates[i][0],coordinates[i][1],0,i==0));
        var a = MapOverlay.render(players,0,0,0,"",0); var b = MapOverlay.render(players,0,0,0,"",3);
        if (a.overflow() > 0) assertFalse(Arrays.equals(a.frame().pixels,b.frame().pixels));
        else assertEquals(6, a.labeled());
    }
    @Test void otherWorldsFooterCannotEraseSouthEdgeSelfArrow() {
        var result = MapOverlay.render(List.of(new MapOverlay.PlayerDot(new UUID(0,1),"SELF",0,200,0,true)),0,0,0,"1 in other worlds",0);
        int green = 0; for (int y = 118; y < 128; y++) for (int x = 60; x < 69; x++) if(result.frame().pixels[y*128+x] == PixelFrame.GREEN) green++;
        assertTrue(green > 0);
    }
    private int[] toInts(byte[] data) { int[] out = new int[data.length]; for (int i = 0; i < data.length; i++) out[i] = data[i]; return out; }
}
