package com.winkreyes.excelcalmbars;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tracked state of one boss bar on one player's connection.
 *
 * The gate sees every boss-bar packet for the player, so for any bar
 * whose ADD crossed the gate we hold the full display state (name
 * component, progress, colour, overlay, property flags) and can
 * rebuild the bar exactly when a hold is released. Bars first seen
 * through an UPDATE packet have partial state only; those are
 * released by replaying the raw held packets instead of a rebuild.
 *
 * All access is synchronised on the owning PlayerBars object: the
 * Netty event-loop threads (packet gate) and the server main thread
 * (combat events, flush task) share this state.
 */
public final class BarState {

    public final UUID id;

    /** The live NMS Component object for the bar's current title. */
    public Object nameComponent;
    /** Plain-text rendering of the current title (for pattern ID). */
    public String plainTitle;
    public float progress;
    /** NMS BossBarColor enum instance from the bar's ADD/style. */
    public Object color;
    /** NMS BossBarOverlay enum instance from the bar's ADD/style. */
    public Object overlay;
    public boolean darkenScreen;
    public boolean playMusic;
    public boolean createWorldFog;

    /** True once a full ADD state has been seen for this bar. */
    public boolean hasFullState;
    /** True when the client currently displays this bar. */
    public boolean clientHas;
    /** True while this bar's packets are being held back. */
    public boolean held;
    /** When the current hold began (for the max-hold cap). */
    public long heldSinceMillis;
    /** Raw held packets, replay fallback when no full state exists. */
    public final List<Object> rawPackets = new ArrayList<>();

    public BarState(UUID id) {
        this.id = id;
    }
}
