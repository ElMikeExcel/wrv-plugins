# ExcelProtect 0.1.2 — BUILD RECORD

Date: 2026-11-05 · Builder: Iris subagent (desk build; install is the failsafe lane's)
Spec: `SPEC.md` (design-first, in this folder) · Tests/rig: `TEST-REPORT.md`

## Artifact

| Field | Value |
|---|---|
| Jar | `ExcelProtect-0.1.2.jar` |
| Size | **138,039 B** |
| SHA-256 | **`26b7f5ba1b01e37843647244e025da30ec51db6019395c4bfbc38f9428863393`** |
| Determinism | Two clean `local-build.sh` runs byte-identical (`ExcelProtect-0.1.2-repro.jar` = same hash) |
| plugin.yml | name ExcelProtect, version **0.1.2**, main `com.winkreyes.excelprotect.ExcelProtect`, api-version '1.21', softdepend [GriefPrevention, Vault, mcMMO, **EliteMobs**] |

## Base

ExcelProtect **0.1.1** — the live version's own record package
(`~/workspace/records-catchup-20261103/packages/excelprotect-0.1.1-20261104/`).
The workspace tree `~/workspace/excelprotect/src/ExcelProtect/` was diff-verified
byte-identical to that package (src/, plugin.yml, config.yml, tests/) before
copying, and the 0.1.1 sources rebuild to the exact live jar
(122,081 B, `b24c0ef2b16eae8cc381a721ce2d4b252904dd0dcecbe40a8ab5f80130d1312b`)
under the same script — the base is proven, not assumed.

## Delta from 0.1.1 (files)

- NEW `src/com/winkreyes/excelprotect/DamageIncidents.java` — pure incident coalescer
  (merge math, close reasons, death/near-death flags, elite record shapes).
- NEW `src/com/winkreyes/excelprotect/EliteProbe.java` — reflective EliteMobs probe.
  Mechanism verified from the real jar
  (`~/workspace/elitemobs-10.9.12/EliteMobs-10.9.12.jar`, `javap`):
  `EntityTracker.isEliteMob(Entity)` / `getEliteMobEntity(Entity)` →
  `EliteEntity.getLevel()`. No PDC/metadata key is used (none documented in
  the jar; EM's metadata store is internal). Rig boot against the same jar
  printed `elitemobs=hooked`.
- NEW `src/com/winkreyes/excelprotect/DamageWatch.java` — Bukkit glue: MONITOR
  `EntityDamageEvent`/`EntityDamageByEntityEvent` (player victims only),
  MONITOR `PlayerDeathEvent`/`PlayerQuitEvent`, 20-tick sweep reuse of
  `combat::tick`, emits via `Ledger.writer("damage")`.
- NEW `src/com/winkreyes/excelprotect/DamageReport.java` — pure query formatter.
- `Settings.java` — `logDamageIncidents`, `damageIncidentGapMs`,
  `nearDeathFraction` with spec clamps.
- `Exporter.java` — `damage` added to `STREAMS` (query surface).
- `ExcelProtect.java` — wiring (watch enable alongside combat, sweep in the
  housekeeping loop, closeAll on disable), the damage-incidents awake line,
  `damage` subcommand + handler, version stamps 0.1.1 → 0.1.2.
- `plugin.yml` — version 0.1.2, softdepend + EliteMobs, usage line gains
  `damage <player> [hours]`.
- `config.yml` — `log.combat.damage-incidents: true`; new CONFIG section
  `combat.damage-incident-gap-ms: 2000`, `combat.near-death-health-fraction: 0.30`;
  shed-order list gains `damage` after `combat`. All 0.1.1 keys untouched.
- `local-build.sh` — output jar name + pinned jar-entry date only.
- `tests/` — `DamageIncidentsTest`, `DamageSettingsTest`, `DamageReportTest`,
  `DamageStreamTest` added; the 16 carried 0.1.1 tests unchanged.

## Toolchain (same as 0.1.1)

JDK 25 (`~/workspace/.jdk25`), JUnit 5 console, deterministic jar settings
(pinned entry timestamps, sorted entries, fixed manifest). Compile/runtime
dependency hashes: `DEPENDENCIES.sha256` in this folder (GriefPrevention
16.18.7, Vault 2.0.0, mcMMO, Paper API 26.3 build 231, JUnit — binaries live
in the build workspace `libs/` + the excelshops build-libs set, not in the
record package).
