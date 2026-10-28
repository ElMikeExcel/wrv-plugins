package com.winkreyes.excelbosstrial.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Who placed what (0.2.2, seal-hardening option 3A - Mike,
 * Oct 22, 2026): the placement registry that lets end crystals
 * and respawn anchors answer to their PLACER, exactly like
 * player-lit TNT answers to its lighter.
 *
 * Why this exists: Bukkit ties a TNT blast to its lighter
 * (TNTPrimed.getSource), but an End Crystal carries no placer
 * and a respawn anchor's blast arrives as a bare
 * BLOCK_EXPLOSION with no damager at all. Unregistered, both
 * resolved to NOBODY - a free capped-60 opener off every fresh
 * boss that counted against the share and could never earn
 * anything (PlayerB opened every fresh warden of
 * Oct 22 with one). The documented share definition already
 * counts "player-lit/placed explosions" as player damage;
 * this registry is how crystals and anchors join it.
 *
 * Two halves:
 *
 * - PLACEMENTS. A crystal is recorded at EntityPlaceEvent
 *   (entity UUID -> placer) and forgotten when the entity is
 *   removed (it exploded, was broken, or despawned). An anchor
 *   is recorded at BlockPlaceEvent (world + block position ->
 *   placer) and forgotten when the block breaks or explodes.
 *   Whoever DETONATES a registered crystal or anchor is
 *   irrelevant - the blast answers to the placer, mirroring
 *   TNT, whose lighter is judged even when someone else's
 *   explosion chain sets it off.
 *
 * - DETONATIONS. A crystal's damage events name the crystal
 *   itself as damager, so its placements resolve directly. An
 *   anchor's blast names nobody, so when a registered anchor
 *   explodes the adapter records a DETONATION (placer + block
 *   position + time), and a BLOCK_EXPLOSION damage event near
 *   that spot within the window resolves to the placer - the
 *   BedIgnitionLog pattern, widened from beds to anchors. The
 *   most recent matching detonation wins.
 *
 * Session-scoped BY DESIGN, like the encounter ledgers and
 * the launch snapshots: nothing here is persisted, so a
 * restart forgets placements exactly as it forgets ledgers.
 * A crystal placed before a restart explodes unattributed
 * afterward - the safe direction (it counts against, never
 * for). Age prunes in the adapter's maintenance pass are leak
 * insurance only; the lifecycle events above are the real
 * bookkeeping.
 *
 * Pure storage, Bukkit-free: the adapter supplies identities,
 * positions, and timestamps.
 */
public final class PlacementRegistry {

    /** How near (3D blocks) an anchor detonation explains an explosion. */
    public static final double DETONATION_RADIUS_BLOCKS = 12.0;
    /** How long after detonation an explosion still names its placer. */
    public static final long DETONATION_WINDOW_MS = 1_000L;
    /** Detonation entries are pruned once far outside the window. */
    public static final long DETONATION_PRUNE_MS = 5_000L;

    /**
     * Does this use of a respawn anchor DETONATE it? Vanilla's
     * rule: a use charges the anchor when the item is
     * glowstone and the anchor is below its charge cap; every
     * other use of a charged anchor detonates it in a
     * dimension where anchors explode (everywhere except the
     * nether, where the use sets the respawn point instead and
     * the anchor stays). An uncharged anchor never detonates.
     */
    public static boolean anchorUseDetonates(boolean glowstoneUse, int charges,
                                             int maxCharges,
                                             boolean explodingDimension) {
        if (!explodingDimension || charges < 1) {
            return false;
        }
        return !(glowstoneUse && charges < maxCharges);
    }

    private record CrystalEntry(UUID placer, long placedMs) {
    }

    private record AnchorEntry(UUID placer, long placedMs) {
    }

    private record DetonationEntry(UUID placer, String world,
                                   double x, double y, double z, long nowMs) {
    }

    /** One resolved anchor detonation: who answers, and where the blast was. */
    public record Detonation(UUID placer, double x, double y, double z) {
    }

    private final Map<UUID, CrystalEntry> crystals = new HashMap<>();
    private final Map<String, AnchorEntry> anchors = new HashMap<>();
    private final List<DetonationEntry> detonations = new ArrayList<>();

    private static String anchorKey(String world, int x, int y, int z) {
        return world + ";" + x + ";" + y + ";" + z;
    }

    // --------------------------------------------------------------
    // End crystals (entity-keyed)
    // --------------------------------------------------------------

    /** Record (or overwrite) who placed an end crystal. */
    public synchronized void recordCrystal(UUID crystal, UUID placer, long nowMs) {
        if (crystal == null || placer == null) {
            return;
        }
        crystals.put(crystal, new CrystalEntry(placer, nowMs));
    }

    /** The placer of a crystal, or null when it was never registered. */
    public synchronized UUID crystalPlacer(UUID crystal) {
        CrystalEntry e = crystal == null ? null : crystals.get(crystal);
        return e == null ? null : e.placer();
    }

    /** Forget a crystal (it exploded, broke, or despawned). */
    public synchronized void forgetCrystal(UUID crystal) {
        if (crystal != null) {
            crystals.remove(crystal);
        }
    }

    /** Drop crystal entries older than {@code maxAgeMs} (leak insurance). */
    public synchronized void pruneCrystals(long nowMs, long maxAgeMs) {
        crystals.entrySet().removeIf(e -> nowMs - e.getValue().placedMs() > maxAgeMs);
    }

    /** Crystals currently registered (status/tests). */
    public synchronized int crystalCount() {
        return crystals.size();
    }

    // --------------------------------------------------------------
    // Respawn anchors (block-keyed)
    // --------------------------------------------------------------

    /** Record (or overwrite) who placed the anchor at a block. */
    public synchronized void recordAnchor(String world, int x, int y, int z,
                                          UUID placer, long nowMs) {
        if (world == null || placer == null) {
            return;
        }
        anchors.put(anchorKey(world, x, y, z), new AnchorEntry(placer, nowMs));
    }

    /** The placer of the anchor at a block, or null when unregistered. */
    public synchronized UUID anchorPlacer(String world, int x, int y, int z) {
        AnchorEntry e = world == null ? null : anchors.get(anchorKey(world, x, y, z));
        return e == null ? null : e.placer();
    }

    /** Forget the anchor at a block (it broke or exploded). */
    public synchronized void forgetAnchor(String world, int x, int y, int z) {
        if (world != null) {
            anchors.remove(anchorKey(world, x, y, z));
        }
    }

    /** Drop anchor entries older than {@code maxAgeMs} (leak insurance). */
    public synchronized void pruneAnchors(long nowMs, long maxAgeMs) {
        anchors.entrySet().removeIf(e -> nowMs - e.getValue().placedMs() > maxAgeMs);
    }

    /** Anchors currently registered (status/tests). */
    public synchronized int anchorCount() {
        return anchors.size();
    }

    // --------------------------------------------------------------
    // Anchor detonations (the BedIgnitionLog pattern)
    // --------------------------------------------------------------

    /** Record that a registered anchor just exploded (adapter: block center). */
    public synchronized void recordAnchorDetonation(UUID placer, String world,
                                                    double x, double y, double z,
                                                    long nowMs) {
        if (placer == null || world == null) {
            return;
        }
        pruneDetonations(nowMs);
        detonations.add(new DetonationEntry(placer, world, x, y, z, nowMs));
    }

    /**
     * The most recent anchor detonation in the same world, within
     * {@link #DETONATION_RADIUS_BLOCKS} of the point and
     * {@link #DETONATION_WINDOW_MS} of the time - or null when no
     * registered anchor explains the blast (a natural explosion,
     * a bed, an unregistered anchor: all stay unattributed).
     */
    public synchronized Detonation anchorDetonationNear(String world, double x,
                                                        double y, double z, long nowMs) {
        DetonationEntry best = null;
        for (DetonationEntry d : detonations) {
            if (!d.world().equals(world)) {
                continue;
            }
            long dt = nowMs - d.nowMs();
            if (dt < 0 || dt > DETONATION_WINDOW_MS) {
                continue;
            }
            double dx = d.x() - x;
            double dy = d.y() - y;
            double dz = d.z() - z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist <= DETONATION_RADIUS_BLOCKS
                    && (best == null || d.nowMs() >= best.nowMs())) {
                best = d;
            }
        }
        return best == null ? null
                : new Detonation(best.placer(), best.x(), best.y(), best.z());
    }

    /** Drop detonation entries far outside the attribution window. */
    public synchronized void pruneDetonations(long nowMs) {
        detonations.removeIf(d -> nowMs - d.nowMs() > DETONATION_PRUNE_MS);
    }

    /** Detonations currently held (status/tests). */
    public synchronized int detonationCount() {
        return detonations.size();
    }

    /** Forget everything (admin reset). */
    public synchronized void clear() {
        crystals.clear();
        anchors.clear();
        detonations.clear();
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

