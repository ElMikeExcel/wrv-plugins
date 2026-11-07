# ExcelProtect 0.1.6 — source provenance

- **Plugin:** ExcelProtect 0.1.6 "Recovery" — original house code
  (Wink Reyes Valley), Mike's IP, MIT (LICENSE in the plugin lineage
  since 0.1.0). Authors: Iris / Mike.
- **Base:** ExcelProtect 0.1.5 sources (`~/workspace/protect-clarity-build/`),
  LIVE on the server at 0.1.6 build time; jar of record 225,181 B,
  SHA-256 `c9b5dc1f58cd67de7db60e67e82b8e1cd8cb83b6ed0207ae9597bca0fa3ed5cb`.
  0.1.6 is cumulative: it contains 0.1.2 + 0.1.3 + 0.1.4 + 0.1.5 whole.
- **This build:** `~/workspace/protect-recovery-build/` — full source
  in `src/`, tests in `tests/`, build via `local-build.sh`
  (deterministic: sorted entries, timestamps pinned
  2026-11-06T00:00:00Z, `--no-manifest`; the Layer B fingerprint
  marker `excel-build.txt` is injected at build time with the pinned
  build ID `EXC-PROTECT-0.1.6-WRV-20261106-01-c016rf` and
  gate-verified post-build).
- **Jar of record:** `ExcelProtect-0.1.6.jar` — 234,451 B, SHA-256
  `097a4cbc836ef7252db3510770673c7173531eb900c5500d4d6d26c39566b028`;
  double-built byte-identical (`ExcelProtect-0.1.6-repro.jar`
  alongside, same hash).
- **New files over 0.1.5:** `WorldEditHook.java` (EditSessionEvent
  extent-delegate recorder, fail-soft softdepend).
  **Modified:** `Exporter.java` (parseLocal accepts ISO-T / space /
  bare-date forms; `Query.error` records the first malformed known
  filter value), `Rollback.java` (container selection extracted as
  testable `chooseContainerLine`; container undo capture; entity
  rollback `restoreEntity` + shared equipment dressing; inventory
  rollback `restoreInventory` with slot-exact REPLACE),
  `ItemFact.java` (`slotListFromJson` — slot-preserving payload list;
  `listFromJson` refactored onto it, behavior unchanged),
  `ExcelProtect.java` (loud `Query.error` checks on export/rollback;
  container capture in rollback commit + container partition in
  undo; `restore-entity` / `restore-inventory` commands; WorldEdit
  hook wiring + boot/status lines; version strings),
  `Settings.java` (`log.blocks.worldedit`, default ON),
  `plugin.yml` (0.1.6, authors [Iris, Mike], WorldEdit softdepend,
  usage), `config.yml` (worldedit key), `local-build.sh` (jar name,
  fingerprint, WorldEdit compile deps).
  **Tests:** 11 new (77 total, all green).
- **Compile/probe dependencies (see DEPENDENCIES.md):** Paper API
  26.3 build 231, GriefPrevention 16.18.7, Vault 2.0.0, mcMMO
  (libs/), JUnit 5 console standalone 1.11.4 for tests — plus, new
  in 0.1.6, **WorldEdit core 7.2.7 + bukkit 7.2.0-SNAPSHOT API jars
  from the local Maven repo (compile-only; WorldEdit is a softdepend
  and no WorldEdit code is bundled or redistributed)**. EliteMobs is
  a RUNTIME reflective probe only; Magic is consumed through its
  Bukkit event surface only.
- **Rig (not shipped):** Paper 26.3 sandbox in `rig/server` with the
  genuine **WorldEdit 7.4.6** jar (Modrinth version YFnqiUlL,
  SHA-512 `1fe39751…b4152` — the exact build live on WRV, downloaded
  for evidence use only) and the rig-only driver in `rig/` (fake NMS
  player — see BUILD-RECORD rig honesty notes). A first assembly
  attempt from the local .m2 7.2.x jars could not load on MC 26.3
  (adapter clinit), which is why the genuine 7.4.6 artifact was used
  for the proof instead.
