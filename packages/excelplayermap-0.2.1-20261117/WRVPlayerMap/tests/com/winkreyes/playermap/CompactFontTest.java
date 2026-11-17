package com.winkreyes.playermap;

import org.bukkit.map.MinecraftFont;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class CompactFontTest {
    @Test void printableAsciiFitsFiveRowsWithInterLetterSpace() {
        for (char c = 32; c <= 126; c++) {
            assertTrue(CompactFont.supports(c), "missing " + c);
            var glyph = CompactFont.glyph(c);
            assertTrue(glyph.width() >= 1 && glyph.width() <= 5);
            assertEquals(5, glyph.rows().length);
            for (int bits : glyph.rows()) assertTrue(bits >= 0 && bits < (1 << glyph.width()));
        }
        assertEquals(5, CompactFont.HEIGHT);
    }
    @Test void screenshotNamesAreSmallerAndCaseIsPreserved() {
        for (String name : new String[]{"SampleFriend01", "*.MapViewer"}) {
            assertTrue(PixelFrame.width(name) < MinecraftFont.Font.getWidth(name) * 0.8, name);
            assertEquals(name, PixelFrame.fit(name, 122));
        }
        assertTrue(CompactFont.HEIGHT < MinecraftFont.Font.getHeight());
        assertFalse(Arrays.equals(CompactFont.glyph('a').rows(), CompactFont.glyph('A').rows()));
    }
    @Test void textWritesOnlyGlyphStrokesAndTheirOnePixelShadow() {
        PixelFrame frame = new PixelFrame(); frame.text(10, 10, "O O", PixelFrame.GREEN); frame.shadow();
        int width = PixelFrame.width("O O"); int ink = 0;
        for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) {
            byte pixel = frame.pixels[y * 128 + x];
            assertTrue(pixel == PixelFrame.CLEAR || pixel == PixelFrame.GREEN || pixel == PixelFrame.BLACK);
            if (pixel != 0) { ink++; assertTrue(x >= 10 && x <= 10 + width && y >= 10 && y < 16); }
            if (pixel == PixelFrame.BLACK) assertEquals(PixelFrame.GREEN,frame.pixels[(y-1)*128+x-1]);
        }
        assertTrue(ink > 0 && ink < (width+1) * 6 * 0.6);
        assertEquals(PixelFrame.CLEAR, frame.pixels[12 * 128 + 14]); // Space glyph.
    }
    @Test void shadowClipsAtRightBottomAndForegroundWins() {
        PixelFrame frame=new PixelFrame();frame.text(124,123,"O",PixelFrame.GREEN); frame.shadow();
        assertEquals(PixelFrame.GREEN,frame.pixels[124*128+124]);
        assertEquals(PixelFrame.BLACK,frame.pixels[127*128+127]);
        assertDoesNotThrow(()->frame.text(127,127,"A",PixelFrame.GREEN));
    }
    @Test void namesCannotInjectFormattingAndTinyWidthNeverOverflows() {
        assertEquals("A??B", PixelFrame.safeName("A\u00a7\nB"));
        assertEquals("?", PixelFrame.safeName("\u2603"));
        for (int width = 0; width < 128; width++)
            assertTrue(PixelFrame.width(PixelFrame.fit("VeryLongBedrockPlayerName000", width)) <= width);
    }
    @Test void edgeTextClipsSafelyWithoutWrappingOrPaintingTheBackground() {
        PixelFrame frame = new PixelFrame(); frame.text(127, 127, "AB", PixelFrame.YELLOW);
        for (int i = 0; i < 16383; i++) assertEquals(0, frame.pixels[i]);
        assertDoesNotThrow(() -> frame.text(-5, -5, "A", PixelFrame.YELLOW));
    }
}
