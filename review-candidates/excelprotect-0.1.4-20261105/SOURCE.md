# ExcelProtect 0.1.4 — source provenance

- **Plugin:** ExcelProtect 0.1.4 — original house code (Wink Reyes Valley),
  Mike's IP, MIT (LICENSE in the plugin lineage since 0.1.0).
- **Base:** ExcelProtect 0.1.3 sources (`~/workspace/protect-dealt-build/`),
  built Nov 5, 2026 and filed PENDING in the install MANIFEST (not yet
  installed at 0.1.4 build time); jar of record 165,366 B, SHA-256
  `f0ebd67e3d8794def0e83e32def3eb86ff5693d976c11ccc5bf003a650bb9177`.
  0.1.4 is cumulative: it contains 0.1.2 + 0.1.3 whole.
- **This build:** `~/workspace/protect-where-build/` — full source in `src/`,
  tests in `tests/`, build via `local-build.sh` (deterministic: sorted
  entries, timestamps pinned 2026-11-05T00:00:00Z, `--no-manifest`).
- **Jar of record:** `ExcelProtect-0.1.4.jar` — 184,594 B, SHA-256
  `2b471e7b45f9dd029f79570eee3bb24d99da3f830ca17c097c3e942dda9d3b4c`;
  double-built byte-identical (`ExcelProtect-0.1.4-repro.jar` alongside).
- **New files over 0.1.3:** `LocationWatch.java` (locations stream: 60 s
  position samples + join/quit/teleport/world-change/respawn/gamemode
  anchors; sampling only — no PlayerMoveEvent hook, per Mike's Nov 1,
  2026 decision), `WhereReport.java` (`/excelprotect where` route
  rendering), `PlaytimeReport.java` (`/excelprotect playtime` derived
  from the players stream's `player_join`/`player_leave` arcs — no new
  capture). **Modified:** `ExcelProtect.java` (LocationWatch wiring,
  where/playtime commands, locations awake line, version 0.1.4),
  `Settings.java` (`log.locations` default ON, `locations.sample-seconds`
  default 60, clamp 5–3600), `Exporter.java` (STREAMS gains `locations`;
  `player=` matcher extended to attacker/victim/healer UUID fields),
  `InspectReport.java` (inspect now sections ALL streams in fixed order
  with per-section caps; snapshot payloads summarized, never dumped),
  `config.yml` (locations keys + shed order), `plugin.yml` (0.1.4),
  `local-build.sh` (jar name). **Tests:** 12 new; two carried inspect
  tests updated with explicit 0.1.4 SUPERSESSION markers (they encoded
  the pre-0.1.4 inspect scope).
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
