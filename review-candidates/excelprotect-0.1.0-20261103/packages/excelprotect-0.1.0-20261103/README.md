# ExcelProtect 0.1.0 — source record

Plugin: ExcelProtect  
Version: 0.1.0  
Record assembled: Nov 3, 2026 (records catch-up pass)  
Local build folder: `excelprotect/build-0.1.0`

## What this is

- Project folder: `~/workspace/excelprotect/` · Spec: `~/workspace/excelprotect/SPEC.md` (v2 + Amendment 3) - Working tree: `~/workspace/excelprotect/src/ExcelProtect/` (src/, tests/, plugin.yml, config.yml, local-build.sh, libs/) - Candidate: `~/workspace/excelprotect/build-0.1.0/ExcelProtect-0.1.0.jar`, SHA-256 `648d5aa2e77186249874efd0d431a87de04914f55322bf55390a7b59b4f87594` (repro pair byte-identical) - Build: `bash local-build.sh` (JDK 25 default `~/workspace/.jdk25`; YAML-guards plugin.yml/config.yml; runs the 11-test JUnit suite; deterministic jar — entries touched 2026-11-01, sorted, no manifest).

## Built artifact (not included in this record — text only)

- Jar: `ExcelProtect-0.1.0.jar` — 115549 bytes, SHA-256 `648d5aa2e77186249874efd0d431a87de04914f55322bf55390a7b59b4f87594`

## Install / live status

Shadow-installed Nov 2, 2026, 4:05–4:12 AM CST alongside CoreProtect/DeathRecord/Telemetry/AuditLog; adapters GP/Vault/mcMMO on; importer off. Nothing retires until the importer check and a week of volume data.

## Record contents

- `src/` — the version's source tree (text files only; binaries and build outputs excluded, see FILE-LIST.txt)
- `plugin.yml`, `config.yml` — as shipped in this version (when the version ships a config)
- `TEST-RESULTS.txt` — the build folder's recorded test evidence
- `DEPENDENCIES.md` / `DEPENDENCIES.sha256` — upstream dependencies
- `FUNCTIONAL-CHANGE.diff` — change vs the previous version's tree, or a statement where no clean baseline exists
- `SANITIZATION.diff` — sensitive-content scan result
- `SOURCE-PROVENANCE.json` — provenance + per-file SHA-256 manifest
- `FILE-LIST.txt`, `SHA256SUMS` — inventory and checksums
