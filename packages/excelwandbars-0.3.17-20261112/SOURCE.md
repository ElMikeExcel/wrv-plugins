# SOURCE — ExcelWandBars 0.3.17 (Shape A: effective-page-count tap bar)

- **Artifact:** `ExcelWandBars-0.3.17.jar`, 553,827 B, sha256
  `949e9cd48790cd10bb2861ce32f4cc3442410aee219e91bbc48f6cb526330c9b`;
  build id `EXC-WANDBARS-0.3.17-WRV-20261112-01-9d0b77` (BuildInfo +
  excel-build.txt, Layer B fingerprint per FINGERPRINT-STANDARD;
  suffix = first 6 hex of SHA-256 of
  "EXC-WANDBARS-0.3.17-effective-pages").
- **Base:** the shipped 0.3.16 jar (553,302 B / `897e589c…`), first
  reproduced byte-identically from the 0.3.16 sources (basecheck in
  this folder) before any change was made. This build changes:
  `core/BarLoadout.java` (capped layout overload), `core/PageLayout.java`
  (packCapped), `LoadoutHook.java` (capped overload + admin view),
  `StyleHook.java` (layoutFor targets the effective page count;
  both counter writes removed), `RankUpHook.java` (F1 hic copy
  reverted), `BuildInfo.java` + shipped `plugin.yml` (0.3.17,
  YAML-validated) + `excel-build.txt`. `config.yml` and
  `spellbook.yml` are byte-identical to 0.3.16's. Rationale:
  BUILD-NOTES.md. Proof: TEST-RESULTS.txt (23 suites / 669 checks)
  and RIG-RESULTS.md (real-Magic mirror rig, steps 1–3).
- **Toolchain:** Temurin JDK 25 (`~/workspace/.jdk25/jdk-25.0.5+8`),
  `javac` over `work/cp.txt` + `work/sources.txt` into `work/classes`;
  repack via `work/repack.py <shipped 0.3.16 jar> work/classes
  work/shipped <out>` (sorted entries, 2026-01-01 timestamps, 5 Geyser
  hook classes pinned to shipped bytes) — the 0.3.12–0.3.16 method,
  with the shipped 0.3.16 jar as the repack base. Double-built
  byte-identical (candidate-a.jar = candidate-b.jar).
- **Contents of this zip:** the full `src/` tree (96 files) exactly as
  compiled for the shipped candidate.
