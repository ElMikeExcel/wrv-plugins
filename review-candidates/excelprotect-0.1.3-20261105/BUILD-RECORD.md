# ExcelProtect 0.1.3 — BUILD RECORD

- **Date:** Nov 5, 2026. **Builder:** Iris (subagent build task).
- **Commission:** Mike, Nov 5, 2026, 12:25 PM CST — damage dealt to mobs +
  self/other healing tracking (quote in SPEC.md). Base: ExcelProtect
  0.1.2 LIVE (jar 138,039 B, SHA-256 `26b7f5ba…`, sources in
  `~/workspace/protect-damage-build/`).
- **Deliverable:** `ExcelProtect-0.1.3.jar` — **165,366 B**, SHA-256
  `f0ebd67e3d8794def0e83e32def3eb86ff5693d976c11ccc5bf003a650bb9177`.
  Deterministic double-build byte-identical; repro jar
  `ExcelProtect-0.1.3-repro.jar` alongside.
- **Build:** `local-build.sh` (carried from 0.1.2, jar name updated) —
  YAML sanity parse, JUnit first (37 tests, 0 failures), compile against
  Paper API 26.3 build 231 + libs/ (GriefPrevention 16.18.7, Vault 2.0.0,
  mcMMO), deterministic jar (sorted entries, pinned timestamps
  2026-11-05T00:00:00Z, `--no-manifest`).
- **What changed vs 0.1.2:** +6 source files (DealtIncidents, DealtWatch,
  DealtReport, HealingIncidents, HealingWatch, HealingReport); edits to
  CombatCore (peekClaim), ExcelProtect (register watches BEFORE combat,
  awake lines, dealt/healing commands, 0.1.3), Settings (2 toggles),
  Exporter (STREAMS + dealt/healing), config.yml (toggles + shed order),
  plugin.yml (0.1.3). Design + boundaries: SPEC.md. Evidence:
  TEST-REPORT.md + `rig/evidence/`.
- **Install state at build time:** NOT installed. MANIFEST entry is the
  parent's to file; INSTALL-PACKET.md carries the gate (live 0.1.2 jar
  must read 138,039 B / `26b7f5ba…` or STOP).
- **Records:** package staged at
  `~/workspace/records-catchup-20261103/packages/excelprotect-0.1.3-20261105/`
  (parent publishes: Drive deposit + draft PR).
