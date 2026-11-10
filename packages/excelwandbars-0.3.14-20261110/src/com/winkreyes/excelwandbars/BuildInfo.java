package com.winkreyes.excelwandbars;

/**
 * Layer B per-build fingerprint (FINGERPRINT-STANDARD.md): this build's
 * unique ID, also carried at the jar root in excel-build.txt and printed
 * in the enable log so support can ask for it ("tell me your build
 * line"). The constant is assembled from fragments at class-init, so a
 * one-string edit does not cleanly remove it, while the source stays
 * perfectly legible. Passive only: the ID is read for the log line and
 * nothing else — it never gates, reports, or changes behavior.
 *
 * Minted for this artifact line only; a replacement artifact gets a NEW
 * build ID (standard §4.12). Semantic (Layer C) markers persist version
 * to version — see the package's FINGERPRINTS.md sheet.
 */
public final class BuildInfo {
    private BuildInfo() {}

    public static final String BUILD_ID = "EXC-WANDBARS-" + "0.3.14-WRV-" + "20261110-01-5a68bd";

    public static String buildId() { return BUILD_ID; }
}
