package com.winkreyes.excelsummons;

/**
 * Build identity (FINGERPRINT-STANDARD Layer B). The build id is
 * assembled from fragments so it survives casual source reading; the
 * local build verifies the assembled value against the pinned build
 * id in local-build.sh and against excel-build.txt inside the jar.
 * Semantic-marker note lives in FINGERPRINTS.md.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class BuildInfo {
    private BuildInfo() {}

    private static final String[] PARTS = {
        "EXC-SUMMONS-", "0.1.7-", "WRV-", "20261112-", "01-", "7e4a17"
    };

    public static final String BUILD_ID = String.join("", PARTS);
    public static final String VERSION = "0.1.7";
    public static final String TAG = "7e4a17";
}
