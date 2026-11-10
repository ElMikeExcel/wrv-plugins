# SOURCE — ExcelWandBars 0.3.14 (form contrast)

- **Artifact:** `ExcelWandBars-0.3.14.jar`, 545,907 B, sha256
  `c87f6005f92539993d05ec8f9f52726a0b3c99a129c36f94153f7fd723774d63`;
  build id `EXC-WANDBARS-0.3.14-WRV-20261110-01-5a68bd` (BuildInfo +
  excel-build.txt, Layer B fingerprint per FINGERPRINT-STANDARD).
- **Base:** the shipped 0.3.13 jar (544,853 B / `7278f09e…`), itself
  first reproduced byte-identically from the `src0313` tree
  (`~/workspace/wandbars-renewal-tag-fix/src0313/`, verified identical
  to the 0.3.13 build tree) before any change was made. This build
  changes: `core/FormContrast.java` (new), `core/SpellbookText.java`
  (locked ink + hint wording), `BedrockForms.java` (renderer
  normalization), `BuildInfo.java` + shipped `plugin.yml` (0.3.14),
  `excel-build.txt`. `config.yml` and `spellbook.yml` are
  byte-identical to 0.3.13's.
- **Toolchain:** Temurin JDK 25 (`~/workspace/.jdk25/jdk-25.0.5+8`),
  `javac` over `work/cp.txt` + `work/sources.txt` into `work/classes`;
  repack via `work/repack.py <shipped 0.3.13 jar> work/classes
  work/shipped <out>` (sorted entries, 2026-01-01 timestamps, 5 Geyser
  hook classes pinned to shipped bytes) — the 0.3.12/0.3.13 method,
  with the shipped 0.3.13 jar as the repack base.
- **Tests:** `tests/` — 20 suites, 577 checks, 0 failed (19 carried
  0.3.13 suites, 507 checks + `FormContrastTest`, 70 checks).
- **Desk probe:** `rig-probe/FormProbe.java` (build folder only, not
  packaged) — builds a real Cumulus SimpleForm through the patched
  renderer; output in TEST-RESULTS.txt.
- **Ownership:** Wink Reyes Valley (Mike Reyes / Iris), same as the
  rest of the Excel suite.
