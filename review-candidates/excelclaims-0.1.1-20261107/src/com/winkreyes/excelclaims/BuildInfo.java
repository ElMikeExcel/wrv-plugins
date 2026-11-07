package com.winkreyes.excelclaims;

/**
 * ExcelClaims — build identity (Fingerprint Standard Layer B: the
 * build ID also lives in excel-build.txt at the jar root and is
 * printed by /excelclaims status, so it is load-bearing for support).
 * Excel — Iris / Mike.
 */
public final class BuildInfo {
    public static final String BUILD_ID =
            "EXC-" + "CLAIMS-" + "0.1.1-" + "WRV-" + "20261107-" + "02-" + "5a1e9c";
    public static final String PRODUCT = "ExcelClaims";
    public static final String VERSION = "0.1.1";

    private BuildInfo() { }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley
