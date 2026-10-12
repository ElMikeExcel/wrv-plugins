package com.winkreyes.playermap;

/** Minecraft maps are north-up: world +X is right, world +Z is down. */
public final class MapGeometry {
    private MapGeometry() {}
    public record Point(int x, int y, boolean edge, double dx, double dy) {}
    public static Point project(double x, double z, int centerX, int centerZ, int scale) {
        if (!Double.isFinite(x) || !Double.isFinite(z) || scale < 0 || scale > 4)
            throw new IllegalArgumentException("Invalid map coordinate or scale");
        double dx = (x - centerX) / (1 << scale), dy = (z - centerZ) / (1 << scale);
        boolean edge = dx < -64 || dx >= 64 || dy < -64 || dy >= 64;
        if (edge) {
            double factor = Math.max(Math.max(dx / 59, -dx / 60), Math.max(dy / 59, -dy / 60));
            return new Point(clamp((int) Math.round(64 + dx / factor), 4, 123),
                    clamp((int) Math.round(64 + dy / factor), 4, 123), true, dx, dy);
        }
        return new Point(clamp((int) Math.round(64 + dx), 4, 123), clamp((int) Math.round(64 + dy), 4, 123), false, dx, dy);
    }
    public static int vanillaCenter(double coordinate, int scale) {
        int size = 128 << scale;
        return (int) (Math.floor((coordinate + 64) / size) * size + size / 2.0 - 64);
    }
    public static int clamp(int value, int low, int high) { return Math.max(low, Math.min(high, value)); }
}
