# SOURCE — ExcelAutoSort 0.2.2

- **Plugin:** ExcelAutoSort 0.2.2 (package `com.winkreyes.excelautosort`)
- **Authors:** Iris / Mike (plugin.yml `authors: [Iris, Mike]`)
- **Ownership:** 100% house IP. The 0.2.2 increment (BB-04,
  back-button audit fix batch, board run 354) was written
  in-house on top of the 0.2.1 tree: Back buttons + relabels
  across the chest-filter Selector, the filter recovery box,
  and Max Organization (Java GUIs + Bedrock forms) — no
  upstream code.
- **Build ID:** `EXC-AUTOSORT-0.2.2-WRV-20261114-01-8fdbf2`
  (see FINGERPRINTS.md).
- **Artifacts:**
  - `ExcelAutoSort-0.2.2.jar` — 160,197 B — SHA-256
    `8b3bf5ab3b07119909e8f371361a21f32235b17039eb0f53b987f514fa18ad28`
    (80 jar entries; build + repro jars byte-identical)
  - `ExcelAutoSort-0.2.2-source.zip` — 100,207 B — SHA-256
    `c2ff6c453264c810e5fc340ade2fac0a32a940f30b958fa0427565733ca288fe`
    (40 files: src/ 36 .java + tests/ 1 .java + plugin.yml +
    config.yml + local-build.sh)
- **Build:** `local-build.sh` (version-parameterized) — YAML
  validation → JUnit → deterministic double-build,
  byte-identical, fingerprint gate PASS. Toolchain and
  dependencies as in the 0.2.1 record (Temurin JDK 25 line,
  Paper API 26.3; compile-time libs in the build tree's
  `libs/`, provided at runtime by the server; see
  BUILD-RECORD.md / RECEIPTS.md).
- **Design of record:** `BUILD-RECORD.md` in this package
  (the run-354 BB-04 RESULT, copied verbatim) +
  `INSTALL-PACKET.md`, scoped by the back-button audit
  FIXBATCH-SPLIT-R348.md §3 (BB-04 row, 12 rows). No separate
  0.2.2 SPEC increment was authored — unlike 0.2.1, this
  increment's design lives in those files; no SPEC.md is
  carried in this package.
- **Tests:** unit suite in `tests/` — 35 found / 35
  successful / 0 failed; local Paper rig PASS, SEVERE 0;
  code-trace walk 25/25 (WALK-OUTPUT.txt). Tallies in
  `BUILD-RECORD.md`.
- **Source tree of record:**
  `~/workspace/back-button-wave1-r354/BB-04/build/ExcelAutoSort-0.2.2/`
  (a copy of the 0.2.1 build tree; only 8 files differ —
  SelectorGui.java, MaxOrgGui.java, BedrockFormsHook.java,
  BuildInfo.java, config.yml, plugin.yml, local-build.sh,
  AutoSortTest.java — per RECEIPTS.md).
- **Installed:** Nov 15, 2026 (see INSTALLED.txt +
  OUTCOME.txt in this package; failsafe run scheduled
  03:03 CST, Homebase lane, placed jar + merged config
  pull-backs EXACT, SCRIPT-OUTPUT.txt 18/18 PASS).
