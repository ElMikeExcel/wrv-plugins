package com.winkreyes.excelquestxp;

/**
 * ExcelQuestXP build identity. Pinned here so the boot line, the build
 * record and the install packet all name the same build.
 */
public final class BuildInfo {
    public static final String VERSION = "1.1.0";
    public static final String BUILD_ID = "EXC-EMQUESTXP-1.1.0-WRV-20261120-01";
    /** EliteMobs version this build was compiled and rig-proven against.
     * CORRECTION (rig, Nov 19, 2026): the live jar FILE is named
     * EliteMobs-10.9.10.jar, but its plugin.yml reports 10.9.14 — and it is
     * byte-identical to the "held" EliteMobs-10.9.14.jar (SHA-256 below).
     * Live therefore runs 10.9.14 code in a 10.9.10-named file; the W3
     * "live is 10.9.10, NOT the held 10.9.14" line is wrong. The pin is
     * the jar BYTES (SHA-256), so this build is correct for the live file. */
    public static final String EM_PIN = "10.9.14";
    /** SHA-256 of the pinned EliteMobs-10.9.10.jar (live jar, W6 fresh pull). */
    public static final String EM_JAR_SHA256 =
            "4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd";

    private BuildInfo() {}
}
