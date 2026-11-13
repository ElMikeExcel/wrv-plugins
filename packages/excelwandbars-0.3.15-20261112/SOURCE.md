# SOURCE — ExcelWandBars 0.3.15 (verify-race fix)

- **Artifact:** `ExcelWandBars-0.3.15.jar`, 550,474 B, sha256
  `61b278b46d8708dff04eb08a0e99a87ee25ac78b19310eeb10ba0b2411b700f2`;
  build id `EXC-WANDBARS-0.3.15-WRV-20261112-01-9bc99f` (BuildInfo +
  excel-build.txt, Layer B fingerprint per FINGERPRINT-STANDARD;
  suffix = first 6 hex of SHA-256 of "EXC-WANDBARS-0.3.15-verify-race").
- **Base:** the shipped 0.3.14 jar (545,907 B / `c87f6005…`), itself
  first reproduced byte-identically from the 0.3.14 sources
  (`~/workspace/wandbars0314-drive/`, tree verified identical to the
  src zip and to the 0.3.14 build tree) before any change was made.
  This build changes: `core/VerifyRetryPlan.java` (new),
  `StyleHook.java` (delayed verify retry, conditional inventory
  close, branch logging), `TapBarHook.java` (one call site names its
  "bar open" trigger), `BuildInfo.java` + shipped `plugin.yml`
  (0.3.15), `excel-build.txt`. `config.yml` and `spellbook.yml` are
  byte-identical to 0.3.14's. Per-file rationale mapped to the
  diagnosis FIX SPEC items 1–4: see BUILD-NOTES.md.
- **Toolchain:** Temurin JDK 25 (`~/workspace/.jdk25/jdk-25.0.5+8`),
  `javac` over `work/cp.txt` + `work/sources.txt` into `work/classes`;
  repack via `work/repack.py <shipped 0.3.14 jar> work/classes
  work/shipped <out>` (sorted entries, 2026-01-01 timestamps, 5 Geyser
  hook classes pinned to shipped bytes) — the 0.3.12–0.3.14 method,
  with the shipped 0.3.14 jar as the repack base.
- **Tests:** `tests/` — 21 suites, 624 checks, 0 failed (20 carried
  0.3.14 suites, 577 checks + `VerifyRetryPlanTest`, 47 checks).
- **Desk rig:** `rig-probe/VerifyRaceRig.java` (build folder only,
  not packaged) — simulates the diagnosed item-lag race through the
  shipped `StyleProfile` + `VerifyRetryPlan` classes; 21 checks,
  output summarized in TEST-RESULTS.txt with its limits stated.
- **Ownership:** Wink Reyes Valley (Mike Reyes / Iris), same as the
  rest of the Excel suite.
