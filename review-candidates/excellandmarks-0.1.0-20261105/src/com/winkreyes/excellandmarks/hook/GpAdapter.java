package com.winkreyes.excellandmarks.hook;

import java.util.UUID;

/**
 * GriefPrevention subdivision port (SPEC §5). The implementation that
 * touches GP classes lives in {@link GpHook} and is only instantiated
 * when the GriefPrevention plugin is present; everything else in the
 * plugin talks to this interface and can never NoClassDefFound.
 */
public interface GpAdapter {

    /** Shared no-op: naming works, claims simply never grant. */
    GpAdapter NONE = new GpAdapter() {
        @Override public boolean available() { return false; }
        @Override public Long createSubdivision(String world, int x1, int y1, int z1,
                                                int x2, int y2, int z2, UUID owner) { return null; }
        @Override public boolean deleteSubdivision(long claimId) { return true; } // nothing exists; the end state already holds
    };

    boolean available();

    /**
     * Create a buyer-owned subdivision of the claim covering the region
     * (the big spawn admin claim is assumed; corners are clamped inside
     * it). Returns the new claim id, or null when there is no parent
     * claim or creation failed — naming still works either way.
     */
    Long createSubdivision(String world, int x1, int y1, int z1, int x2, int y2, int z2, UUID owner);

    boolean deleteSubdivision(long claimId);
}
