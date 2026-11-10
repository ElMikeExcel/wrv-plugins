package com.winkreyes.exceldetails;

/**
 * Forensic fingerprint (FINGERPRINT-STANDARD Layer B): the build ID is
 * assembled from fragments so it survives in the compiled class; the
 * build script verifies both this and excel-build.txt in the shipped
 * jar. Rotates per distributed build; the semantic marker
 * (EXC-DETAILS) persists version to version.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class BuildInfo {
    private static final String A = "EXC-DETAILS-";
    private static final String B = "0.2.2-WRV-";
    private static final String C = "20261110-01-";
    private static final String D = "b22e10";
    public static final String BUILD_ID = A + B + C + D;

    private BuildInfo() {}
}
