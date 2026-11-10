# SOURCE — ExcelAutoSort 0.2.1

- **Plugin:** ExcelAutoSort 0.2.1 (package `com.winkreyes.excelautosort`)
- **Authors:** Iris / Mike (plugin.yml `authors: [Iris, Mike]`)
- **Ownership:** 100% house IP. The 0.2.1 increment (SPEC §16 in
  this package's `SPEC.md`) was written in-house on top of the
  0.2.0 tree: the per-player admin surface for Max Organization
  (status / revoke / grant) in the existing command, service, core,
  store, and Protect-event classes — no new files, no upstream code.
  The target-resolution rule and the grant|revoke symmetry are the
  house ExcelCosmetics admin pattern, re-expressed in this plugin's
  own code (no Cosmetics code copied).
- **Build ID:** `EXC-AUTOSORT-0.2.1-WRV-20261109-01-c36331`
  (see FINGERPRINTS.md).
- **Artifacts:**
  - `ExcelAutoSort-0.2.1.jar` — 159,240 B — SHA-256
    `48a5c0fed431fc9853e0e92ff199b24887ab18485a5febb4132735759617e266`
  - `ExcelAutoSort-0.2.1-source.zip` — 94,174 B — SHA-256
    `99f8371e49db29fb0f8c9810238307a67b5847cffc7d74dc774eaa3a784c26c0`
    (40 files: src/ + tests/ + plugin.yml + config.yml +
    local-build.sh; timestamps pinned 2026-11-09)
- **Build:** `local-build.sh` (version-parameterized) — YAML
  validation → JUnit → deterministic double-build (pinned
  2026-11-09 timestamps, sorted entries, no manifest),
  byte-identical, fingerprint gate on both Layer B homes.
  Toolchain: Temurin JDK 25.0.4.1 (`~/workspace/.jdk25`), Paper API
  26.3 build 231. Dependencies unchanged from 0.2.0 (compile-time,
  provided at runtime by the server): PlaceholderAPI 2.12.3,
  GriefPrevention 16.18.7 (the ExcelClaims facade on the live
  server), Vault 2.0.0 API, ExcelProtect 0.1.7 (API only — the audit
  sink binds reflectively through an isolation gateway and the
  plugin runs unchanged with Protect absent), Floodgate (Cumulus
  forms, softdepend).
- **Spec:** `SPEC.md` in this package (§16 is the 0.2.1 design of
  record; §§1–15 are the carried 0.1.0/0.2.0 design, unchanged).
- **Tests:** unit suite in `tests/`; acceptance rig in the build
  folder's `rig/` (local only, not published). Tallies in
  `BUILD-RECORD.md`.
- **Source tree of record:** `~/workspace/autosort-plugin/build-0.2.1/`
  (the 0.2.0 tree at `~/workspace/autosort-plugin/` is untouched by
  this build — its jar still hashes to the 0.2.0 record value).
