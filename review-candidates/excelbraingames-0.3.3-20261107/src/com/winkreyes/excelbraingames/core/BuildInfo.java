package com.winkreyes.excelbraingames.core;

/**
 * Build identity (house fingerprint standard, Layer B — adopted by
 * BrainGames in 0.3.2; see FINGERPRINTS.md in the build folder).
 * Format: EXC-BRAINGAMES-&lt;version&gt;-WRV-&lt;yyyymmdd&gt;-&lt;seq&gt;-&lt;hash6&gt;,
 * where hash6 is the first 6 hex chars of the SHA-256 over the build's
 * sorted per-file source hashes (this carrier file excluded from the
 * hash set) — anyone rebuilding from the deposited source reproduces
 * the same id (build.sh verifies all three homes: this constant,
 * excel-build.txt at the jar root, and the recomputed source hash).
 */
public final class BuildInfo {

    public static final String BUILD_ID = "EXC-BRAINGAMES-0.3.3-WRV-20261107-01-3bfc61";

    private BuildInfo() {}
}
