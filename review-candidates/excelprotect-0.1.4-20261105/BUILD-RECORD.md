# ExcelProtect 0.1.4 — BUILD RECORD

- **Date:** Nov 5, 2026. **Builder:** Iris (subagent build task).
- **Commission:** the coverage audit's first staged increment after
  0.1.3 (`~/workspace/protect-coverage-audit/COVERAGE-MAP.md`, "0.1.4
  Where"), under Mike's ExcelProtect charter (Nov 5, 2026, 12:26 PM
  CST): total observability, proactive not reactive. Base: ExcelProtect
  0.1.3 sources in `~/workspace/protect-dealt-build/` (jar 165,366 B,
  SHA-256 `f0ebd67e…`, filed PENDING, not yet installed at build time).
- **Deliverable:** `ExcelProtect-0.1.4.jar` — **184,594 B**, SHA-256
  `2b471e7b45f9dd029f79570eee3bb24d99da3f830ca17c097c3e942dda9d3b4c`.
  Deterministic double-build byte-identical; repro jar
  `ExcelProtect-0.1.4-repro.jar` alongside.
- **Build:** `local-build.sh` (carried from 0.1.3, jar name updated) —
  YAML sanity parse, JUnit first (49 tests, 0 failures), compile against
  Paper API 26.3 build 231 + libs/ (GriefPrevention 16.18.7, Vault
  2.0.0, mcMMO), deterministic jar (sorted entries, pinned timestamps
  2026-11-05T00:00:00Z, `--no-manifest`).
- **What changed vs 0.1.3:** +3 source files (LocationWatch,
  WhereReport, PlaytimeReport); edits to ExcelProtect (wiring, where/
  playtime commands, locations awake line, 0.1.4), Settings
  (`log.locations`, `locations.sample-seconds`), Exporter (STREAMS +
  `locations`; `player=` matcher extended to combat UUID fields),
  InspectReport (sections across ALL streams, per-section caps,
  snapshot payloads summarized), config.yml (locations keys + shed
  order), plugin.yml (0.1.4). Design + boundaries: SPEC.md. Evidence:
  TEST-REPORT.md + `rig/evidence/`.
- **Design note:** playtime adds NO capture — it derives from the
  players stream's existing `player_join`/`player_leave` arcs
  (PlaytimeReport). Location sampling is the settled reconciliation
  with Mike's Nov 1, 2026 decision: a dot a minute + event anchors,
  never a PlayerMoveEvent hook.
- **Install state at build time:** NOT installed. MANIFEST entry is the
  parent's to file; INSTALL-PACKET.md carries the gate (live
  `ExcelProtect-0.1.3.jar` must read 165,366 B / `f0ebd67e…` or STOP —
  0.1.3 installs ahead of this in queue order; a window that finds
  0.1.2 still live STOPs, it does not skip a version).
- **Records:** package staged at
  `~/workspace/records-catchup-20261103/packages/excelprotect-0.1.4-20261105/`
  (parent publishes: Drive deposit + draft PR).
