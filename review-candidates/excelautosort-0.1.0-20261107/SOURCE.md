# SOURCE — ExcelAutoSort 0.1.0

- **Plugin:** ExcelAutoSort 0.1.0 (package `com.winkreyes.excelautosort`)
- **Authors:** Iris / Mike (plugin.yml `authors: [Iris, Mike]`)
- **Ownership:** 100% house IP. Written from scratch for Wink Reyes
  Valley against the behavioral contract of the incumbent set it
  replaces (nothub AutoSort 0.1.17's live behavior + ExcelAutoSortBridge
  0.1.0's placeholder semantics, both inventoried in SPEC §1). No
  upstream code is included; the sort comparator's *ordering* matches
  the incumbent's published behavior (registry order → key → meta
  string) because players' muscle memory is the spec, not because
  code was carried over. Upstream SortTest ordering cases were
  re-expressed as data in the house unit suite.
- **Build ID:** `EXC-AUTOSORT-0.1.0-WRV-20261106-04-f96f93`
  (see FINGERPRINTS.md; seq -01 was a superseded pre-release build).
- **Artifacts:**
  - `ExcelAutoSort-0.1.0.jar` — 104,136 B — SHA-256
    `00d01234b0a7d06b5c01dce3805f9f5736f0cf2f026512234e29c29c868dc36d`
  - `ExcelAutoSort-0.1.0-source.zip` — 61,659 B — SHA-256
    `4081e1d1cf0f2ea97be121a6b38731f26263c4062d516e7439f74393f4d6d6d7`
    (src/ + tests/ + plugin.yml + config.yml + local-build.sh)
- **Build:** `local-build.sh` — YAML validation → JUnit → deterministic
  double-build (pinned 2026-11-06 timestamps, sorted entries, no
  manifest), byte-identical, fingerprint gate on both Layer B homes.
  Toolchain: Temurin JDK 25.0.4.1 (`~/workspace/.jdk25`), Paper API
  26.3 build 231. Dependencies (compile-time, provided at runtime by
  the server): PlaceholderAPI 2.12.3, GriefPrevention 16.18.7,
  ExcelProtect 0.1.7 (API only — the audit sink binds reflectively
  through an isolation gateway and the plugin runs unchanged with
  Protect absent), Floodgate (Cumulus forms, softdepend).
- **Spec:** `SPEC.md` in this folder (includes the §15 Max
  Organization design for 0.2.0 — spec only, not in this build).
- **Tests:** unit suite in `tests/`; acceptance rig in `rig/`
  (local only, not published). Tallies in `BUILD-RECORD.md`.
