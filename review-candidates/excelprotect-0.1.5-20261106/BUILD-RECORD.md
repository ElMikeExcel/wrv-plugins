# ExcelProtect 0.1.5 — BUILD RECORD

- **Date:** Nov 6, 2026. **Builder:** Iris (subagent build task).
- **Commission:** Mike, Nov 5, 2026, 11:05 PM CST — the clarity pass
  pulled AHEAD of the Elites build after the mending-book
  investigation (Protect's records were complete but unreadable as a
  story; the investigator concluded wrong twice from complete data).
  Scope addition Nov 5, ~11:10 PM CST: player-first timelines
  (feature 6). Base: ExcelProtect 0.1.4 sources in
  `~/workspace/protect-where-build/` — 0.1.4 LIVE at build time (jar
  184,594 B, SHA-256 `2b471e7b…`). The staged line renumbers: Elites
  → 0.1.6, Machines & making → 0.1.7, server self-knowledge → 0.1.8,
  casts & economy causes → 0.1.9 (coverage map updated in the same
  pass: `~/workspace/protect-coverage-audit/COVERAGE-MAP.md`).
- **Deliverable:** `ExcelProtect-0.1.5.jar` — **225,181 B**, SHA-256
  `c9b5dc1f58cd67de7db60e67e82b8e1cd8cb83b6ed0207ae9597bca0fa3ed5cb`.
  Deterministic double-build byte-identical; repro jar
  `ExcelProtect-0.1.5-repro.jar` alongside. Fingerprint gate PASS:
  the jar carries `excel-build.txt` =
  `EXC-PROTECT-0.1.5-WRV-20261106-01-c01500` (FINGERPRINT-STANDARD
  Layer B, build-time injected with a pinned build ID so the
  double-build stays byte-identical).
- **Build:** `local-build.sh` (carried from 0.1.4; jar name, pinned
  dates 2026-11-06, fingerprint injection + gate added) — YAML sanity
  parse, JUnit first (66 tests, 0 failures), compile against Paper
  API 26.3 build 231 + libs/ (GriefPrevention 16.18.7, Vault 2.0.0,
  mcMMO), deterministic jar (sorted entries, pinned timestamps,
  `--no-manifest`).
- **What changed vs 0.1.4:** +4 source files (Chains, Timelines,
  ItemChainReport, LiveInventory); edits to ExcelProtect (0.1.5
  wiring, item/timelines commands, LIVE inspect read, player-first
  query paths, clarity + timelines awake lines), Adapters (chain
  minting + next-tick delivery resolution; shop_purchase minting when
  fields resolve), ItemEvents + Containers (chain stamping),
  InspectReport (0.1.4 sections preserved; drop/resolution pairing
  with UNRECOVERED flags; trade/delivery/jobs summaries), Ledger
  (timeline projection hook; provenance `file_header` on fresh day
  files — closes the queued null-header bake fix), Settings
  (`log.player-timelines` ON, `clarity.chain-expiry-seconds` 1800,
  `clarity.ground-radius` 5), config.yml (timelines + clarity keys),
  plugin.yml (0.1.5 + `website:` line — closes that bake fix too),
  local-build.sh. Design + boundaries: SPEC.md. Evidence:
  TEST-REPORT.md + `rig/evidence/`.
- **Design notes:** (1) delivery is a linked follow-up record, never
  an in-place edit — the streams stay append-only; (2) the chain
  matcher is state-filtered and labels ambiguity `probable` rather
  than guessing; (3) timelines are a PROJECTION — canonical streams
  untouched, projection failure-isolated, rebuildable by console
  command; queries read player-first per day and fall back per day,
  so a pre-0.1.5 day (or the toggle off) behaves exactly like 0.1.4;
  (4) `fight_summary` is deliberately not projected per-player.
- **Sales-aware engineering (standing practice, Nov 5, 2026):**
  evidence filed at
  `~/workspace/plugin-publishing/PROTECT-CLARITY-EVIDENCE-20261106.md`
  — structural differences vs the category leader (CoreProtect) named
  internally, with the rig's measured query-path facts and caveats.
- **Install state at build time:** NOT installed. Filed PENDING in
  the install MANIFEST by this lane; INSTALL-PACKET.md carries the
  gate (live `ExcelProtect-0.1.4.jar` must read 184,594 B /
  `2b471e7b…` or STOP — never skip a version).
- **Records:** package staged at
  `~/workspace/records-catchup-20261103/packages/excelprotect-0.1.5-20261106/`
  (parent publishes: Drive deposit + draft PR).
