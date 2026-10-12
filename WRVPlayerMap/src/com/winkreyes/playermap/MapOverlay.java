package com.winkreyes.playermap;

import java.util.*;

/** Pure geometry/layout. No chunks, block reads, world scans, tasks or network access. */
final class MapOverlay {
    record PlayerDot(UUID id, String name, double x, double z, float yaw, boolean self) {}
    record Rect(int x, int y, int w, int h) {
        boolean intersects(Rect other) { return x < other.x + other.w && x + w > other.x && y < other.y + other.h && y + h > other.y; }
    }
    record Marker(PlayerDot player, MapGeometry.Point point, int x, int y) {}
    record Result(PixelFrame frame, int labeled, int overflow) {}
    static Result render(List<PlayerDot> players, int centerX, int centerZ, int scale, String footer, long revision) {
        PixelFrame frame = new PixelFrame();
        List<PlayerDot> sorted = new ArrayList<>(players);
        sorted.sort(Comparator.comparing(PlayerDot::self).reversed().thenComparing(PlayerDot::name).thenComparing(PlayerDot::id));
        List<Marker> markers = new ArrayList<>();
        List<Rect> occupied = new ArrayList<>();
        int bottom = 116; // Reserve room even when only an overflow footer becomes necessary.
        for (PlayerDot player : sorted) {
            MapGeometry.Point p = MapGeometry.project(player.x, player.z, centerX, centerZ, scale);
            int x = p.x(), y = p.y();
            // Separate coincident symbols while retaining a leader to the exact map position.
            outer: for (int radius = 0; radius <= 40; radius += 4) {
                for (int direction = 0; direction < (radius == 0 ? 1 : 8); direction++) {
                    int cx = MapGeometry.clamp(p.x() + (int) Math.round(radius * Math.cos(direction * Math.PI / 4)), 4, 123);
                    int cy = MapGeometry.clamp(p.y() + (int) Math.round(radius * Math.sin(direction * Math.PI / 4)), 4, 123);
                    Rect r = new Rect(cx - 3, cy - 3, 7, 7);
                    if (occupied.stream().noneMatch(r::intersects)) { x = cx; y = cy; break outer; }
                }
            }
            occupied.add(new Rect(x - 3, y - 3, 7, 7));
            markers.add(new Marker(player, p, x, y));
        }
        List<Marker> labelOrder = new ArrayList<>(markers);
        Map<Marker, Rect> labels = layoutLabels(labelOrder, bottom, occupied);
        // Even a small group of long names can overflow. Rotate only on crowded layouts.
        if (labels.size() < markers.size() && !labelOrder.isEmpty()) {
            Collections.rotate(labelOrder, -(int) ((revision / 3) % labelOrder.size()));
            labels = layoutLabels(labelOrder, bottom, occupied);
        }
        for (Marker marker : markers) frame.line(marker.point.x(), marker.point.y(), marker.x, marker.y, PixelFrame.GRAY);
        for (var entry : labels.entrySet()) {
            Marker marker = entry.getKey(); Rect label = entry.getValue();
            int nearestX = MapGeometry.clamp(marker.x, label.x, label.x + label.w - 1);
            int nearestY = MapGeometry.clamp(marker.y, label.y, label.y + label.h - 1);
            frame.line(marker.x, marker.y, nearestX, nearestY, PixelFrame.GRAY);
        }
        for (var entry : labels.entrySet()) {
            Marker marker = entry.getKey(); Rect label = entry.getValue();
            String name = PixelFrame.fit((marker.player.self ? "*" : "") + marker.player.name, 122);
            frame.box(label.x, label.y, label.w, label.h, PixelFrame.BLACK);
            frame.text(label.x + 1, label.y + 1, name, marker.player.self ? PixelFrame.GREEN : PixelFrame.YELLOW);
        }
        int overflow = markers.size() - labels.size();
        String status = footer;
        if (overflow > 0) status = "+" + overflow + " labels (cycling)";
        if (!status.isEmpty()) {
            frame.box(0, 118, 128, 10, PixelFrame.BLACK);
            frame.text(1, 119, PixelFrame.fit(status, 126), PixelFrame.WHITE);
        }
        // Symbols are last so a south-edge arrow cannot disappear behind the status strip.
        for (Marker marker : markers) if (!marker.player.self) drawMarker(frame, marker);
        for (Marker marker : markers) if (marker.player.self) drawMarker(frame, marker);
        return new Result(frame, labels.size(), overflow);
    }
    private static Map<Marker, Rect> layoutLabels(List<Marker> markers, int bottom, List<Rect> initial) {
        List<Rect> occupied = new ArrayList<>(initial);
        Map<Marker, Rect> labels = new LinkedHashMap<>();
        for (Marker marker : markers) {
            String name = PixelFrame.fit((marker.player.self ? "*" : "") + marker.player.name, 122);
            Rect label = labelPosition(marker.x, marker.y, PixelFrame.width(name) + 2, 10, bottom, occupied);
            if (label != null) { occupied.add(label); labels.put(marker, label); }
        }
        return labels;
    }
    static Rect labelPosition(int x, int y, int w, int h, int bottom, List<Rect> occupied) {
        int[][] preferred = {{x + 5,y - 5},{x - w - 5,y - 5},{x - w/2,y - h - 5},{x - w/2,y + 5}};
        for (int[] position : preferred) {
            Rect r = new Rect(MapGeometry.clamp(position[0], 0, 128 - w), MapGeometry.clamp(position[1], 0, bottom - h + 1), w, h);
            if (occupied.stream().noneMatch(r::intersects)) return r;
        }
        Rect best = null; int distance = Integer.MAX_VALUE;
        for (int row = 0; row <= bottom - h + 1; row += 2) for (int col = 0; col <= 128 - w; col += 2) {
            int d = Math.abs(col + w/2 - x) + Math.abs(row + h/2 - y);
            if (d >= distance || !available(col, row, w, h, occupied)) continue;
            best = new Rect(col, row, w, h); distance = d;
        }
        return best;
    }
    private static boolean available(int x, int y, int w, int h, List<Rect> occupied) {
        for (Rect other : occupied) if (x < other.x + other.w && x + w > other.x && y < other.y + other.h && y + h > other.y) return false;
        return true;
    }
    private static void drawMarker(PixelFrame frame, Marker m) {
        byte color = m.player.self ? PixelFrame.GREEN : PixelFrame.WHITE;
        double dx = m.point.edge() ? m.point.dx() : -Math.sin(Math.toRadians(m.player.yaw));
        double dy = m.point.edge() ? m.point.dy() : Math.cos(Math.toRadians(m.player.yaw));
        double length = Math.hypot(dx, dy); if (length == 0) { dx = 0; dy = -1; length = 1; }
        dx /= length; dy /= length;
        int tipX = m.x + (int) Math.round(dx * 3), tipY = m.y + (int) Math.round(dy * 3);
        int leftX = m.x + (int) Math.round(-dx * 2 - dy * 2), leftY = m.y + (int) Math.round(-dy * 2 + dx * 2);
        int rightX = m.x + (int) Math.round(-dx * 2 + dy * 2), rightY = m.y + (int) Math.round(-dy * 2 - dx * 2);
        frame.box(m.x - 3, m.y - 3, 7, 7, PixelFrame.BLACK);
        frame.line(tipX, tipY, leftX, leftY, color); frame.line(tipX, tipY, rightX, rightY, color);
        frame.line(leftX, leftY, rightX, rightY, color); frame.pixel(m.x, m.y, color);
    }
}
