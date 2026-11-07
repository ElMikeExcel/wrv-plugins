package com.winkreyes.excelclaims;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * ExcelClaims — one land claim (or subdivision). The engine's own model;
 * the GriefPrevention-API facade wraps these. Trust lists hold UUID
 * strings, legacy player names, or the literal "public", exactly as the
 * old flat files did, so import/export lose nothing.
 *
 * Corners are normalized: lesser = min on every axis. A claim protects
 * the column from its lesser Y up to the sky (the stored greater Y is
 * the corner as clicked/created, reported faithfully to API callers) —
 * the same protection shape players have always experienced here: a
 * claim that "ended" at the click height would leave every roof
 * unprotected. Excel — Iris / Mike.
 */
public final class ClaimRecord {
    public long id;
    public String world;
    public int lx, ly, lz, gx, gy, gz;
    /** Null = admin claim (owned by the server). */
    public UUID owner;
    /** -1 = top-level claim. */
    public long parentId = -1;
    public final Set<String> builders = new LinkedHashSet<>();
    public final Set<String> containers = new LinkedHashSet<>();
    public final Set<String> accessors = new LinkedHashSet<>();
    public final Set<String> managers = new LinkedHashSet<>();
    /** Players whose container trust is view-only (ClaimPermission.Inventory). */
    public final Set<String> inventoryOnly = new LinkedHashSet<>();
    public boolean inheritNothing;
    public boolean subclaimRestricted;
    /** True when created by the first-chest automatic claim and never grown. */
    public boolean chestClaim;
    public long createdAt;
    public long lastModifiedAt;
    /** Cached API facade for this record (one per record, so parent identity holds). */
    public transient me.ryanhamshire.GriefPrevention.Claim facade;

    public boolean isAdmin() { return owner == null; }
    public boolean isSubdivision() { return parentId >= 0; }

    public int area() { return (gx - lx + 1) * (gz - lz + 1); }
    public int widthX() { return gx - lx + 1; }
    public int widthZ() { return gz - lz + 1; }
    public int height() { return gy - ly + 1; }

    public boolean containsColumn(int x, int z) {
        return x >= lx && x <= gx && z >= lz && z <= gz;
    }

    /** Protection containment: X/Z plus at-or-above the lesser Y. */
    public boolean contains(int x, int y, int z) {
        return containsColumn(x, z) && y >= ly;
    }

    /** Strict 3D containment inside the recorded corners (facade parity). */
    public boolean containsBox(int x, int y, int z) {
        return containsColumn(x, z) && y >= ly && y <= gy;
    }

    public boolean overlapsColumn(ClaimRecord o) {
        return world.equals(o.world)
                && lx <= o.gx && o.lx <= gx
                && lz <= o.gz && o.lz <= gz;
    }

    public boolean fullyInside(ClaimRecord parent) {
        return world.equals(parent.world)
                && lx >= parent.lx && gx <= parent.gx
                && lz >= parent.lz && gz <= parent.gz;
    }

    /** All trust entries across the four lists (not inventory-only). */
    public Set<String> allTrusted() {
        Set<String> all = new LinkedHashSet<>();
        all.addAll(builders); all.addAll(containers); all.addAll(accessors); all.addAll(managers);
        return all;
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley
