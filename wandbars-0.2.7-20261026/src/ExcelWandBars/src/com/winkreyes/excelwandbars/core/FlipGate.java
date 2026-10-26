package com.winkreyes.excelwandbars.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pure decision for "a sneak press flips the open spell bar".
 * No Bukkit / Magic types, so it is unit-tested on the plain JVM.
 */
public final class FlipGate {

    /** Why a sneak press did or did not flip the bar. */
    public enum Result {
        FLIP, NOT_SNEAK_START, NO_WAND, CLASSIC_STYLE, WRONG_MODE, BAR_CLOSED, ONE_BAR,
        SKIP_WORLD, SKIP_VEHICLE, SKIP_SWIMMING, SKIP_GLIDING, SKIP_FLYING, DEBOUNCED, DISABLED
    }

    /** Snapshot of the player's state at the sneak event (filled by the Bukkit/Magic glue). */
    public static final class State {
        public boolean sneakStart;      // PlayerToggleSneakEvent.isSneaking() == true
        public boolean hasActiveWand;   // Magic Mage.getActiveWand() != null
        public boolean barsStyle = true; // 0.2.0: the player picked Spell bars (/wandstyle bars); classic players never flip
        public boolean inventoryMode;   // wand mode == inventory (chest/cycle wands are left alone)
        public boolean barOpen;         // Wand.isInventoryOpen()
        public int barCount;            // number of hotbars the wand really has
        public boolean skipWorld;       // configured skip world (EliteMobs worlds, belt and braces)
        public boolean inVehicle;
        public boolean swimming;
        public boolean gliding;
        public boolean flying;
    }

    /** Options from config.yml. */
    public static final class Options {
        public boolean enabled = true;
        public long debounceMs = 250;
        public boolean skipVehicle = true;
        public boolean skipSwimming = true;
        public boolean skipGliding = true;
        public boolean skipFlying = true;
    }

    private final Map<UUID, Long> lastFlip = new HashMap<>();

    public Result decide(UUID player, State s, Options o, long nowMs) {
        if (!o.enabled) return Result.DISABLED;
        if (!s.sneakStart) return Result.NOT_SNEAK_START;
        if (!s.hasActiveWand) return Result.NO_WAND;
        if (!s.barsStyle) return Result.CLASSIC_STYLE;
        if (!s.inventoryMode) return Result.WRONG_MODE;
        if (!s.barOpen) return Result.BAR_CLOSED;
        if (s.barCount < 2) return Result.ONE_BAR;
        if (s.skipWorld) return Result.SKIP_WORLD;
        if (o.skipVehicle && s.inVehicle) return Result.SKIP_VEHICLE;
        if (o.skipSwimming && s.swimming) return Result.SKIP_SWIMMING;
        if (o.skipGliding && s.gliding) return Result.SKIP_GLIDING;
        if (o.skipFlying && s.flying) return Result.SKIP_FLYING;
        Long last = lastFlip.get(player);
        if (last != null && nowMs - last >= 0 && nowMs - last < o.debounceMs) return Result.DEBOUNCED;
        lastFlip.put(player, nowMs);
        return Result.FLIP;
    }

    public void forget(UUID player) { lastFlip.remove(player); }

    public int tracked() { return lastFlip.size(); }

    /** Next bar index (0-based), wrapping 1 -> 2 -> 3 -> 1. Mirrors Magic's Wand.cycleHotbar(1). */
    public static int next(int current, int count) {
        if (count <= 0) return 0;
        int n = (current + 1) % count;
        return n < 0 ? n + count : n;
    }
}
