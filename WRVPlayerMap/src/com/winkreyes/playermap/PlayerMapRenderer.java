package com.winkreyes.playermap;

import org.bukkit.entity.Player;
import org.bukkit.map.*;
import java.awt.Color;
import java.util.*;

final class PlayerMapRenderer extends MapRenderer {
    private static final Color[] COLORS = {null, new Color(12,12,12), Color.WHITE, new Color(127,255,0), new Color(255,255,85), new Color(150,150,150)};
    private record State(long revision, byte[] pixels, boolean permitted, UUID viewer) {}
    private final PlayerMapPlugin plugin;
    private final Map<MapCanvas, State> frames = new WeakHashMap<>();
    PlayerMapRenderer(PlayerMapPlugin plugin) { super(true); this.plugin = plugin; }
    @Override public void render(MapView view, MapCanvas canvas, Player viewer) {
        boolean permitted = viewer.isOnline() && viewer.hasPermission("wrv.playermap.use") && plugin.isHeld(viewer, view.getId());
        State old = frames.get(canvas);
        if (old != null && old.revision == plugin.revision() && old.permitted == permitted) return;
        PixelFrame frame = permitted ? plugin.overlay(view, viewer) : new PixelFrame();
        apply(canvas, old == null ? null : old.pixels, frame.pixels);
        frames.put(canvas, new State(plugin.revision(), frame.pixels, permitted, viewer.getUniqueId()));
    }
    static int apply(MapCanvas canvas, byte[] previous, byte[] next) {
        int changes = 0;
        for (int i = 0; i < next.length; i++) if (previous == null || previous[i] != next[i]) {
            canvas.setPixelColor(i % 128, i / 128, COLORS[next[i]]); changes++;
        }
        return changes;
    }
    void forget(UUID viewer) { frames.values().removeIf(state -> state.viewer.equals(viewer)); }
    void clearCache() { frames.clear(); }
}
