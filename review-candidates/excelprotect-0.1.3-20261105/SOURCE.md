# ExcelProtect 0.1.3 — source provenance

- **Plugin:** ExcelProtect 0.1.3 — original house code (Wink Reyes Valley),
  Mike's IP, MIT (LICENSE in the plugin lineage since 0.1.0).
- **Base:** ExcelProtect 0.1.2 sources (`~/workspace/protect-damage-build/`),
  LIVE Nov 5, 2026; jar of record 138,039 B, SHA-256
  `26b7f5ba1b01e37843647244e025da30ec51db6019395c4bfbc38f9428863393`.
- **This build:** `~/workspace/protect-dealt-build/` — full source in `src/`,
  tests in `tests/`, build via `local-build.sh` (deterministic: sorted
  entries, timestamps pinned 2026-11-05T00:00:00Z, `--no-manifest`).
- **Jar of record:** `ExcelProtect-0.1.3.jar` — 165,366 B, SHA-256
  `f0ebd67e3d8794def0e83e32def3eb86ff5693d976c11ccc5bf003a650bb9177`;
  double-built byte-identical (`ExcelProtect-0.1.3-repro.jar` alongside).
- **New files over 0.1.2:** `DealtIncidents.java`, `DealtWatch.java`,
  `DealtReport.java`, `HealingIncidents.java`, `HealingWatch.java`,
  `HealingReport.java`. **Modified:** `CombatCore.java` (peekClaim),
  `ExcelProtect.java` (wiring, awake lines, dealt/healing commands,
  version), `Settings.java` (two toggles), `Exporter.java` (STREAMS),
  `config.yml`, `plugin.yml`, `local-build.sh` (jar name).
- **Compile/probe dependencies (unchanged from 0.1.2, check
  DEPENDENCIES.md + DEPENDENCIES.sha256):** Paper API 26.3 build 231,
  GriefPrevention 16.18.7, Vault 2.0.0, mcMMO (libs/), JUnit 5 console
  standalone 1.11.4 for tests. EliteMobs is a RUNTIME reflective probe
  only — no EM code is compiled in or redistributed; Magic likewise is
  consumed through its Bukkit event surface only.
- **Rig (not shipped):** Paper 26.3 sandbox in `rig/server` with the real
  EliteMobs 10.9.12 jar + Magic 11.2.5 jar (both Mike's licensed server
  copies, evidence use only) and the rig-only driver in `rig/driver/`.
