package excelemfixes.mount.core;

import java.util.UUID;

/**
 * A player's horse always looks like THEIR horse: colour and
 * markings are derived deterministically from the owner's UUID
 * (stable across summons, no storage needed). The Bukkit layer
 * maps these indexes into Horse.Color / Horse.Style, whose
 * ordinals follow the counts pinned here (7 colours, 5 styles —
 * guarded by a unit test against accidental drift in this file,
 * and by the rig proof for the live mapping).
 */
public final class HorseVariant {

    public static final int COLOR_COUNT = 7;
    public static final int STYLE_COUNT = 5;

    private HorseVariant() {}

    public static int colorIndex(UUID owner) {
        return Math.floorMod(mix(owner, 0x9E3779B97F4A7C15L), COLOR_COUNT);
    }

    public static int styleIndex(UUID owner) {
        return Math.floorMod(mix(owner, 0xC2B2AE3D27D4EB4FL), STYLE_COUNT);
    }

    /** SplitMix-style finalizer over the UUID bits plus a salt. */
    private static long mix(UUID id, long salt) {
        long h = id.getMostSignificantBits() ^ id.getLeastSignificantBits() ^ salt;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }
}
