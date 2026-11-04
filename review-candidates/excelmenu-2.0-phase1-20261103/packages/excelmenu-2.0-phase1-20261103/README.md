# ExcelMenu 2.0 Phase 1 — source record

Plugin: ExcelMenu  
Version: 2.0 Phase 1  
Record assembled: Nov 3, 2026 (records catch-up pass)  
Local build folder: `menu-2.0/phase-1`

## What this is

Artifact: `ExcelMenu-2.0.0.jar` (this folder) SHA-256: `1419bdb9ddd4b97b32f3fef466e63c305a6c6cf94e443416c8abd513adde0fd3` Size: 223,472 bytes. Three consecutive `./build.sh` runs byte-identical (reproducible-build verified 2026-10-31).

## Built artifact (not included in this record — text only)

- Jar: `ExcelMenu-2.0.0.jar` — 224150 bytes, SHA-256 `6350714b541f000d5fd2ebd3c6e82785ed207fa9f466c2aedd64f2a9b6d58a13`

## Install / live status

Phase 1 installed live Nov 2, 2026 (see INSTALLED.txt in the build folder); superseded the same night by the Phase 2 unification swap.

## Record contents

- `src/` — the version's source tree (text files only; binaries and build outputs excluded, see FILE-LIST.txt)
- `plugin.yml`, `config.yml` — as shipped in this version (when the version ships a config)
- `TEST-RESULTS.txt` — the build folder's recorded test evidence
- `DEPENDENCIES.md` / `DEPENDENCIES.sha256` — upstream dependencies
- `FUNCTIONAL-CHANGE.diff` — change vs the previous version's tree, or a statement where no clean baseline exists
- `SANITIZATION.diff` — sensitive-content scan result
- `SOURCE-PROVENANCE.json` — provenance + per-file SHA-256 manifest
- `FILE-LIST.txt`, `SHA256SUMS` — inventory and checksums
