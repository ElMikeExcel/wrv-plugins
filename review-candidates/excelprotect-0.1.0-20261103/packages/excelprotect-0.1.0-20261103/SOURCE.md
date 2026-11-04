# ExcelProtect — SOURCE.md

## Current build: 0.1.0 (2026-11-01) — BUILT, NOT INSTALLED (shadow-install packet)

- Project folder: `~/workspace/excelprotect/` · Spec: `~/workspace/excelprotect/SPEC.md` (v2 + Amendment 3)
- Working tree: `~/workspace/excelprotect/src/ExcelProtect/` (src/, tests/, plugin.yml, config.yml, local-build.sh, libs/)
- Candidate: `~/workspace/excelprotect/build-0.1.0/ExcelProtect-0.1.0.jar`, SHA-256 `648d5aa2e77186249874efd0d431a87de04914f55322bf55390a7b59b4f87594` (repro pair byte-identical)
- Build: `bash local-build.sh` (JDK 25 default `~/workspace/.jdk25`; YAML-guards plugin.yml/config.yml; runs the 11-test JUnit suite; deterministic jar — entries touched 2026-11-01, sorted, no manifest).

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

`src/com/winkreyes/excelprotect/`: ExcelProtect (wiring + commands) · Ledger (writer/rotation/aggregates/shed) · Json · DurableWriter · DamageLedger · ItemFact (fingerprint + payload + rid) · Settings · Blocks · Containers · Entities · ItemEvents · Players · CombatCore · EconomyWatch · Recovery (L&F + marks/state) · Rollback · Exporter · Importer · Watch · Adapters · McmmoAdapter · GpAdapter · api/ (RecordEvent, EconomyEvent, FightTableEvent, ExcelProtectAPI).
Tests: `tests/com/winkreyes/excelprotect/ProtectTest.java` (11 tests).
Rig: `build-0.1.0/rig/` (ProtectRigDriver source + servers + evidence-A/A2/B).
