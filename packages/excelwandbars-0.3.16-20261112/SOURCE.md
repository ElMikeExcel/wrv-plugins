# SOURCE — ExcelWandBars 0.3.16 (hotbar-counter fix)

- **Artifact:** `ExcelWandBars-0.3.16.jar`, 553,302 B, sha256
  `897e589c93be86161400755f233cdcf10324c0ea9d2b021fb2690a5ef17a5055`;
  build id `EXC-WANDBARS-0.3.16-WRV-20261112-01-6a4cab` (BuildInfo +
  excel-build.txt, Layer B fingerprint per FINGERPRINT-STANDARD;
  suffix = first 6 hex of SHA-256 of "EXC-WANDBARS-0.3.16-hotbar-counter").
- **Base:** the shipped 0.3.15 jar (550,474 B / `61b278b4…`), itself
  first reproduced byte-identically from the 0.3.15 sources
  (basecheck in this folder) before any change was made. This build
  changes: `core/ApplyCoalescePlan.java` (new), `StyleHook.java`
  (F1 counter write, F2 verdict detail, F4 coalescing + self-apply
  stamp), `RankUpHook.java` (F1 template key list),
  `TapBarHook.java` (F3 INFO logging, F4 input-stamp accessor),
  `BuildInfo.java` + shipped `plugin.yml` (0.3.16),
  `excel-build.txt`. `config.yml` and `spellbook.yml` are
  byte-identical to 0.3.15's. Per-file rationale mapped to the
  diagnosis FIX SPEC items F1–F5: see BUILD-NOTES.md.
- **Toolchain:** Temurin JDK 25 (`~/workspace/.jdk25/jdk-25.0.5+8`),
  `javac` over `work/cp.txt` + `work/sources.txt` into `work/classes`;
  repack via `work/repack.py <shipped 0.3.15 jar> work/classes
  work/shipped <out>` (sorted entries, 2026-01-01 timestamps, 5 Geyser
  hook classes pinned to shipped bytes) — the 0.3.12–0.3.15 method,
  with the shipped 0.3.15 jar as the repack base.
- **Tests:** `tests/` — 22 suites, 643 checks, 0 failed (21 carried
  suites, 624 checks + `ApplyCoalescePlanTest`, 19 checks).
- **Desk rig:** `rig-probe/HotbarCounterRig.java` (build folder
  only, not packaged) — the F5 gates on Mike's actual saved
  student-wand item (fixture generated from the diagnosis evidence
  pull; `pull/fixture-student-wand.json` + `rig-probe` WandFixture)
  against a line-faithful port of Magic's buildInventory; 40
  checks, all PASS, output summarized in TEST-RESULTS.txt with
  its limits stated. The carried `VerifyRaceRig` (21 checks) also
  passes.
- **Ownership:** Wink Reyes Valley (Mike Reyes / Iris), same as the
  rest of the Excel suite.
