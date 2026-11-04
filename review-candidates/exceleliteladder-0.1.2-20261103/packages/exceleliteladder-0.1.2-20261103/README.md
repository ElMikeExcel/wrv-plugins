# ExcelEliteLadder 0.1.2 — source record

Plugin: ExcelEliteLadder  
Version: 0.1.2  
Record assembled: Nov 3, 2026 (records catch-up pass)  
Local build folder: `excel-eliteladder-0.1.2/build-0.1.2`

## What this is

Corrects the mob-ladder table (Mike's decision, Nov 1, 2026): the EliteMobs overworld ladder tops out at seal 4, compressed — the S4 row takes the full end-state values (spawn 0.11, level cap 150, damage taken by elites x0.40, damage dealt to players x2.35) and the S5 row is made identical to S4, so a seal-5 break is a safe no-op (seal 5 hands the group to the EliteMobs guild world instead). Change scope: LadderTable S4/S5 rows, version stamps, and test fixtures only — every other source file is byte-identical to the 0.1.1 tree. Deterministic build: two clean builds produce the identical jar.

## Built artifact (not included in this record — text only)

- Jar: `ExcelEliteLadder-0.1.2.jar` — 31403 bytes, SHA-256 `7ea61276a543820351cae016c8ece4f927b8d430ae4cf71a4987b6d4cecbd7fb`

## Install / live status

Installed live Nov 1, 2026 (Mike-directed push window). Boot verified: 'ExcelEliteLadder enabled: applied step S3, watching ExcelGuildGate seals every 30s'; SEVERE 0; guild LOCKED 3 of 5. Supersedes 0.1.1 (recorded in PR #70).

## Record contents

- `src/` — the version's source tree (text files only; binaries and build outputs excluded, see FILE-LIST.txt)
- `plugin.yml`, `config.yml` — as shipped in this version (when the version ships a config)
- `TEST-RESULTS.txt` — the build folder's recorded test evidence
- `DEPENDENCIES.md` / `DEPENDENCIES.sha256` — upstream dependencies
- `FUNCTIONAL-CHANGE.diff` — change vs the previous version's tree, or a statement where no clean baseline exists
- `SANITIZATION.diff` — sensitive-content scan result
- `SOURCE-PROVENANCE.json` — provenance + per-file SHA-256 manifest
- `FILE-LIST.txt`, `SHA256SUMS` — inventory and checksums
