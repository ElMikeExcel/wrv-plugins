# SOURCE — ExcelWandBars 0.3.13 (cooldown accuracy)

- **Artifact:** `ExcelWandBars-0.3.13.jar`, 544,853 B, sha256
  `7278f09ed160236b54c02d9dfaa590feeb1ba25027c901b3a17217848a1327e3`;
  build id `EXC-WANDBARS-0.3.13-WRV-20261108-01-41cd07` (BuildInfo +
  excel-build.txt, Layer B fingerprint per FINGERPRINT-STANDARD).
- **Base:** the 0.3.12 candidate tree (`../build-0.3.12/work/src`,
  itself on the live 0.3.11 content). This build adds/changes:
  `core/CooldownDisplayPlan.java` (new), `NameTagHook.java` (accuracy
  mode), `MagicHook.java` (`baseCooldownMs`), `BuildInfo.java` +
  shipped `plugin.yml` (0.3.13), `config.yml` (+`accuracy` key),
  `excel-build.txt`.
- **Toolchain:** Temurin JDK 25 (`~/workspace/.jdk25/jdk-25.0.5+8`),
  `javac` over `work/cp.txt` + `work/sources.txt` into `work/classes`;
  repack via `work/repack.py ref-0.3.11.jar work/classes work/shipped
  <out>` (sorted entries, 2026-01-01 timestamps, 5 Geyser hook classes
  pinned to shipped bytes) — the 0.3.12 method, unchanged.
- **Tests:** `tests/` — 19 suites, 507 checks, 0 failed (18 carried
  suites + `CooldownDisplayPlanTest`, 20 checks).
- **Investigation:** `investigation/` — ProbeCd probe source + jar,
  `prepcd.sh` (Moon's lifecycle fixture on the Paper 26.3-235
  skeleton, port 25797), and `evidence/` (phase A stock result,
  phase B candidate results + console). Bytecode disassembly scratch
  was ephemeral (`/tmp/magicjar`); the load-bearing excerpts are
  quoted in BUILD-RECORD.md §1.
- **Ownership:** Wink Reyes Valley (Mike Reyes / Iris), same as the
  rest of the Excel suite.
