package com.winkreyes.playermap;

/** Original renderer; zero means transparent layer, never a replacement terrain image. */
final class PixelFrame {
    static final byte CLEAR = 0, BLACK = 1, WHITE = 2, GREEN = 3, YELLOW = 4, GRAY = 5;
    final byte[] pixels = new byte[128 * 128];
    void pixel(int x, int y, byte color) { if (x >= 0 && x < 128 && y >= 0 && y < 128) pixels[y * 128 + x] = color; }
    // Remove leader strokes under a reserved label; this restores the base, never paints a backing.
    void clear(int x, int y, int width, int height) {
        for (int row = Math.max(0,y); row < Math.min(128,y+height); row++)
            for (int col = Math.max(0,x); col < Math.min(128,x+width); col++) pixel(col,row,CLEAR);
    }
    void line(int x0, int y0, int x1, int y1, byte color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1, err = dx + dy;
        for (int guard = 0; guard < 256; guard++) {
            pixel(x0, y0, color); if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err; if (e2 >= dy) { err += dy; x0 += sx; } if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }
    static String safeName(String text) {
        return safeText(text, 24);
    }
    private static String safeText(String text, int limit) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length() && result.length() < limit; i++) {
            char c = text.charAt(i);
            result.append(CompactFont.supports(c) ? c : '?');
        }
        return result.toString();
    }
    static int width(String text) {
        int width = 0;
        for (char c : text.toCharArray()) width += CompactFont.glyph(c).width() + 1;
        return Math.max(0, width - 1);
    }
    static String fit(String text, int maxWidth) {
        text = safeText(text, 96); if (width(text) <= maxWidth) return text;
        if (maxWidth < width("~")) return "";
        while (!text.isEmpty() && width(text + "~") > maxWidth) text = text.substring(0, text.length() - 1);
        return text + "~";
    }
    void text(int x, int y, String text, byte color) {
        int cursor = x;
        for (char c : text.toCharArray()) {
            CompactFont.Glyph sprite = CompactFont.glyph(c);
            for (int row = 0; row < CompactFont.HEIGHT; row++) for (int col = 0; col < sprite.width(); col++)
                if (sprite.pixel(col, row)) pixel(cursor + col, y + row, color);
            cursor += sprite.width() + 1;
        }
    }
    void shadow() {
        // Foregrounds are complete first: shadows never overwrite another name or arrow.
        // Skip gray leaders so only glyph/arrow-shaped strokes receive a one-pixel shadow.
        for (int y = 126; y >= 0; y--) for (int x = 126; x >= 0; x--) {
            byte color = pixels[y * 128 + x]; int target = (y + 1) * 128 + x + 1;
            if ((color == WHITE || color == GREEN || color == YELLOW)
                    && (pixels[target] == CLEAR || pixels[target] == GRAY)) pixels[target] = BLACK;
        }
    }
}
