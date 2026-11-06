# ExcelProtect 0.1.5 — source provenance

- **Plugin:** ExcelProtect 0.1.5 "Clarity" — original house code (Wink
  Reyes Valley), Mike's IP, MIT (LICENSE in the plugin lineage since
  0.1.0).
- **Base:** ExcelProtect 0.1.4 sources (`~/workspace/protect-where-build/`),
  LIVE on the server at 0.1.5 build time; jar of record 184,594 B,
  SHA-256 `2b471e7b45f9dd029f79570eee3bb24d99da3f830ca17c097c3e942dda9d3b4c`.
  0.1.5 is cumulative: it contains 0.1.2 + 0.1.3 + 0.1.4 whole.
- **This build:** `~/workspace/protect-clarity-build/` — full source in
  `src/`, tests in `tests/`, build via `local-build.sh` (deterministic:
  sorted entries, timestamps pinned 2026-11-06T00:00:00Z,
  `--no-manifest`; the Layer B fingerprint marker `excel-build.txt` is
  injected at build time with the pinned build ID
  `EXC-PROTECT-0.1.5-WRV-20261106-01-c01500` and gate-verified
  post-build).
- **Jar of record:** `ExcelProtect-0.1.5.jar` — size + SHA-256 in
  BUILD-RECORD.md; double-built byte-identical
  (`ExcelProtect-0.1.5-repro.jar` alongside).
- **New files over 0.1.4:** `Chains.java` (chain registry: Sig,
  delivery classification, conservative exact/probable matching),
  `Timelines.java` (per-player timeline projection — derived,
  rebuildable; streams stay canonical), `ItemChainReport.java`
  (`/excelprotect item` chain-of-custody rendering), `LiveInventory.java`
  (LIVE inventory/ender summary for inspect). **Modified:**
  `ExcelProtect.java` (0.1.5 wiring: chains, timelines, item/timelines
  commands, LIVE inspect read, player-first query paths),
  `Adapters.java` (chain minting + next-tick delivery resolution;
  shop_purchase chain minting when fields resolve), `ItemEvents.java` /
  `Containers.java` (chain stamping), `InspectReport.java` (0.1.4
  sections preserved; drop/resolution pairing with UNRECOVERED flags;
  trade/delivery/jobs summaries), `Ledger.java` (timeline projection
  hook; provenance file_header on fresh day files — closes the
  null-header gap), `Settings.java` (`log.player-timelines` default ON,
  `clarity.chain-expiry-seconds` 1800, `clarity.ground-radius` 5),
  `plugin.yml` (0.1.5 + website line), `config.yml` (timelines + clarity
  keys), `local-build.sh` (jar name, fingerprint injection + gate).
  **Tests:** 17 new; one carried ledger test updated with an explicit
  0.1.5 SUPERSESSION marker (the file_header line).
- **Compile/probe dependencies (unchanged from 0.1.3, check
  DEPENDENCIES.md + DEPENDENCIES.sha256):** Paper API 26.3 build 231,
  GriefPrevention 16.18.7, Vault 2.0.0, mcMMO (libs/), JUnit 5 console
  standalone 1.11.4 for tests. EliteMobs is a RUNTIME reflective probe
  only — no EM code is compiled in or redistributed; Magic likewise is
  consumed through its Bukkit event surface only.
- **Rig (not shipped):** Paper 26.3 sandbox in `rig/server` with the real
  EliteMobs 10.9.12 jar + Magic 11.2.5 jar (both Mike's licensed server
  copies, evidence use only) and the rig-only driver in `rig/` (fake NMS
  player; movement driven by teleports — see TEST-REPORT honest limits).
