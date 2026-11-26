package excelemfixes.partydrops;

/**
 * The 50-block party range test — COPIED, not called.
 *
 * ExcelTogether's {@code PartyBonus.within(sameWorld, dx, dy, dz)} is
 * {@code sameWorld && dx*dx + dy*dy + dz*dz <= radius*radius}
 * (squared distance, inclusive boundary; ExcelTogether 0.3.0,
 * {@code PartyBonus.DEFAULT_RADIUS = 50.0}). ExcelTogether exposes no
 * API another plugin can call, and its finders are anchored on a
 * player earner rather than an arbitrary point, so per
 * PARTY-DROPS-SPEC-2026-10-20 §2 this class re-implements the exact
 * same test, anchored on the elite's death location. The conformance
 * unit test (PartyRangeTest) pins the boundary cases so the drop
 * multiplier and the XP party bonus can never disagree about the
 * range DEFINITION: exactly 50.0 counts, 50.0001 does not,
 * cross-world never.
 *
 * Pure math, no Bukkit types, so it is unit tested on the plain JVM.
 */
public final class PartyRange {

    /** Mirrors ExcelTogether's DEFAULT_RADIUS. Keep in step. */
    public static final double DEFAULT_RADIUS = 50.0;

    private PartyRange() {
    }

    /** True when the two points are in the same world and within the radius (inclusive). */
    public static boolean within(boolean sameWorld, double dx, double dy, double dz, double radius) {
        if (!sameWorld) return false;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }
}

// ExcelEMFixes party-drops module — ported from ExcelPartyDrops 0.1.0 (Wave 3, D245).
