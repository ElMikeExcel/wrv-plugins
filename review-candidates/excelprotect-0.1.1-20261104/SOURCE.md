# ExcelProtect — SOURCE.md (build 0.1.1)

## Current build: 0.1.1 (2026-11-04) — BUILT, NOT INSTALLED (filed PENDING)

- Project folder: `~/workspace/excelprotect/` · Spec: `~/workspace/excelprotect/SPEC.md` (v2 + Amendment 3)
- Working tree: `~/workspace/excelprotect/src/ExcelProtect/` (src/, tests/, plugin.yml, config.yml, local-build.sh, libs/)
- Candidate: `~/workspace/excelprotect/build-0.1.1/ExcelProtect-0.1.1.jar`, SHA-256 `b24c0ef2b16eae8cc381a721ce2d4b252904dd0dcecbe40a8ab5f80130d1312b` (122,081 B; repro pair byte-identical)
- Build: `bash local-build.sh` in the working tree (JDK 25 default `~/workspace/.jdk25`; YAML-guards plugin.yml/config.yml; runs the 16-test JUnit suite; deterministic jar — entries touched 2026-11-04, sorted, no manifest).
- Delta vs 0.1.0: new `InspectReport.java` (pure report logic) + `inspect player` / `inspect at` handlers and usage/version strings in `ExcelProtect.java`; plugin.yml version/usage. Everything else byte-identical source to the 0.1.0 tree (whose candidate `648d5aa2…` / 115,549 B is the shadow-live jar this build replaces).

## Identity + lineage

ExcelProtect is a FRESH plugin (package `com.winkreyes.excelprotect`) — the canonical artifact per the 2026-11-01 identity ruling. Its design descends from: ExcelTelemetry's kill schema (fight-window attribution), ExcelDeathRecord's capture/restore model (serializeAsBytes payloads, L&F semantics, PDC owner stamping), and ExcelAuditLog's JSONL "Black Box" build of the same evening (writer durability, exporter, equipment snapshots) — that build is **history**, not the shipped artifact, and its CoreProtect bridge is cut (CoreProtect is replaced, never depended on). The fold map governing parity is `~/workspace/plugin-audit/AUDIT-20261101.md`.

## Compile dependencies (`src/ExcelProtect/libs/`, compile-only, never shipped)

| Jar | SHA-256 | Note |
|---|---|---|
| GriefPrevention-16.18.7.jar | `45e9907c61222e559ed2e099ffee0ede706a21243a31d7db549baf678708fdf0` | the live GP jar; claim events + getClaimAt |
| Vault-2.0.0.jar | `8b2ce431c88741fcae6d4de6032ab2ab2ab0cca6aa86e8af375bdfe654321822a68` | economy provider API |
| mcMMO.jar | `7b077675af937b6c3b491661fa40d08ab96e39479c1e49a4f47ca426b0176054` | level-up event + ExperienceAPI (adapter class loads only when mcMMO is enabled) |

Paper API: `~/workspace/magic-los-fix/cp/paper-api-26.3.build.231-stable.jar` (+ excelshops build-libs, bungeecord-chat, jetbrains annotations per local-build.sh). Magic/Jobs are hooked reflectively by class name — no compile deps, fail-soft.

## Layout

`src/com/winkreyes/excelprotect/`: ExcelProtect (wiring + commands) · InspectReport (0.1.1 text inspect reports) · Ledger (writer/rotation/aggregates/shed) · Json · DurableWriter · DamageLedger · ItemFact (fingerprint + payload + rid) · Settings · Blocks · Containers · Entities · ItemEvents · Players · CombatCore · EconomyWatch · Recovery (L&F + marks/state) · Rollback · Exporter · Importer · Watch · Adapters · McmmoAdapter · GpAdapter · api/ (RecordEvent, EconomyEvent, FightTableEvent, ExcelProtectAPI).
Tests: `tests/com/winkreyes/excelprotect/ProtectTest.java` (16 tests: the 11 from 0.1.0 unchanged + 5 inspect-report tests).
Rig: `build-0.1.1/rig/` (console-driven Paper 26.3 sandbox, seeded ledger day files, transcript in `rig/evidence/`).
