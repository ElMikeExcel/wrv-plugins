package com.winkreyes.excelskillspanel;

/**
 * Build identity (FINGERPRINT-STANDARD Layer B). The build id is
 * assembled from fragments so it survives casual source reading; the
 * local build verifies the assembled value against the pinned build
 * id in local-build.sh and against excel-build.txt inside the jar.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class BuildInfo {
    private BuildInfo() {}

    private static final String[] PARTS = {
        "EXC-SKILLSPANEL-", "0.2.2-", "WRV-", "20261113-", "01-", "c7d2e9"
    };

    public static final String BUILD_ID = String.join("", PARTS);
    public static final String VERSION = "0.2.2";
    public static final String TAG = "c7d2e9";
}
