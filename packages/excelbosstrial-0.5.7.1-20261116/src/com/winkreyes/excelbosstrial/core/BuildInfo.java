/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * Build identity (Fingerprint Standard, Layer B): the per-build
 * ID minted for this artifact, carried in three homes - this
 * constant (assembled from fragments at class-init, so a
 * one-string edit does not cleanly remove it), the jar-root
 * excel-build.txt resource, and the boot log line that prints
 * it as ordinary build metadata (the support surface: "tell me
 * your build line"). Passive only: the ID is read for display,
 * never transmitted, never gates anything.
 *
 * Build ID: EXC-BOSSTRIAL-0.5.7.1-WRV-20261116-01-471aa6
 * (SKU BOSSTRIAL, version 0.5.7.1, channel WRV - the family's own
 * server build, never distributed - minted Nov 16, 2026, seq 01.
 * The tag is FRESH for 0.5.7.1: neither 0.5.7's bb18, 0.5.6's
 * 3c9e56, 0.5.5's 887e6f, 0.5.4's 5371d5, 0.5.3's 35bd2b,
 * 0.5.2's 13447a nor 0.5.1's a3aa16 is ever reused, so no two
 * jars can fuse under one ID. This build is the stamp-only
 * correction of 0.5.7, whose BuildInfo carried the stale 0.5.6
 * stamp in the shipped jar.)
 */
public final class BuildInfo {

    private static final String PART_A = "EXC-BOSSTRIAL-";
    private static final String PART_B = "0.5.7.1-WRV-";
    private static final String PART_C = "20261116-01-471aa6";

    /** The full build ID for this artifact. */
    public static final String BUILD_ID = PART_A + PART_B + PART_C;

    /** The plugin version this build was minted for. */
    public static final String VERSION = "0.5.7.1";

    private BuildInfo() {
    }
}
