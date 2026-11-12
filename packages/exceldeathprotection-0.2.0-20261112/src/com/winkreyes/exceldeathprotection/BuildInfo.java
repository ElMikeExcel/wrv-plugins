package com.winkreyes.exceldeathprotection;

/** Per-build fingerprint (house fingerprint standard, Layer B).
 *  The constant is fragment-assembled so a one-string edit does not
 *  cleanly remove it; the same ID ships in excel-build.txt at the
 *  jar root and is printed by the enable line and /dp version. */
public final class BuildInfo {

    private BuildInfo() {}

    public static final String BUILD_ID =
            "EXC-DPROTECT-" + "0.2.0-" + "WRV-20261112-" + "01-" + "d2b348";

    public static final String VERSION = "0.2.0";
}
