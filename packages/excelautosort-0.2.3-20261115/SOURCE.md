# SOURCE — ExcelAutoSort 0.2.3

- **Plugin:** ExcelAutoSort 0.2.3 (package `com.winkreyes.excelautosort`)
- **Authors:** Iris / Mike (plugin.yml `authors: [Iris, Mike]`)
- **Ownership:** 100% house IP. The 0.2.3 increment
  (P-AUTOSORT-FILTER-1, board-manager run 362) was written
  in-house on top of the 0.2.2 (BB-04) tree: a direct Filter
  ON/OFF toggle inside the Chest filter dialog itself plus
  the ExcelMenu Auto-sort entry move to Chest Tools. New pure
  core `core/FilterToggleCore.java`; the toggle routes through
  the existing `SelectorFlow.requestMode` path (OFF applies at
  once and persists; ON re-arms in the last active mode through
  the existing confirm gate) — no upstream code.
- **Build ID:** `EXC-AUTOSORT-0.2.3-WRV-20261114-01-c41a07`
  (see FINGERPRINTS.md).
- **Artifacts:**
  - `ExcelAutoSort-0.2.3.jar` — 161,791 B — SHA-256
    `0457557e15c3de52033be300bc7f5bb32991268f3005544d7ec0fe15965b130b`
    (81 jar entries; build + repro jars byte-identical)
  - `ExcelAutoSort-0.2.3-source.zip` — 328,378 B — SHA-256
    `0258cbc105bb2a5e2ad580cd8ca4eae0bfe190cbe47d45ee585f49d4ec5cfa7d`
    (42 files: src/ + tests/ + plugin.yml + config.yml +
    local-build.sh + menu/autosort.yml; timestamps pinned
    2026-11-14)
- **Build:** `local-build.sh` (version-parameterized) — YAML
  validation → JUnit → deterministic double-build (pinned
  2026-11-14 timestamps, sorted entries, no manifest),
  byte-identical, fingerprint gate on both Layer B homes.
  Toolchain and dependencies as in the 0.2.1 record
  (Temurin JDK 25 line, Paper API 26.3; compile-time libs in
  the build tree's `libs/`, provided at runtime by the
  server; see BUILD-RECORD.md / RECEIPTS.md).
- **Design of record:** `BUILD-RECORD.md` in this package
  (the run-362 build RESULT, copied verbatim) +
  `INSTALL-PACKET.md`. No separate 0.2.3 SPEC increment was
  authored — unlike 0.2.1, this increment's design lives in
  those two files; no SPEC.md is carried in this package.
- **Tests:** unit suite in `tests/` — 39 found / 39
  successful / 0 failed (35 pre-existing + 4 new toggle
  tests); local Paper rig PASS, SEVERE 0; code-trace walk
  20/20 (WALK-OUTPUT.txt). Tallies in `BUILD-RECORD.md`.
- **Source tree of record:**
  `~/workspace/board-r362/autosort-filter/build/ExcelAutoSort-0.2.3/`
  (a copy of the BB-04 0.2.2 candidate tree; the filed BB-04
  packet was never mutated — its jar re-hashed byte-identical
  in the build run).
- **Installed:** Nov 15, 2026 (see INSTALLED.txt +
  OUTCOME.txt in this package; landing verified PASS 20/20
  in board run 390).
