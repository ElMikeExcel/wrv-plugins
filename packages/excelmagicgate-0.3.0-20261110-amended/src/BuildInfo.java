package com.winkreyes.excelmagicgate;

/**
 * Per-build fingerprint (Fingerprint Standard Layer B, WRV channel).
 * The ID is assembled from fragments at class-init so it is not a
 * single literal; it is printed by /magicgate status as support
 * metadata ("tell me your build line"). Passive provenance only —
 * it never observes, reports, or changes behavior.
 */
public final class BuildInfo {
    public static final String BUILD_ID =
            "EXC-MAGICGATE-" + "0.3.0" + "-WRV-" + "20261110" + "-02-" + "d2a310";
    public static final String VERSION = "0.3.0";

    private BuildInfo() {}
}
