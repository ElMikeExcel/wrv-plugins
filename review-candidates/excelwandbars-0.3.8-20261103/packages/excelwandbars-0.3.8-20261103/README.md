# ExcelWandBars 0.3.8 — source record

Plugin: ExcelWandBars  
Version: 0.3.8  
Record assembled: Nov 3, 2026 (records catch-up pass)  
Local build folder: `wandbars-ux-build/build-0.3.8`

## What this is

- **Base:** ExcelWandBars 0.3.7 source tree (`~/workspace/wandbars-0.3.7/work/`), itself verified byte-identical to the shipped 0.3.7 jar (`fc77e22f40e4719da86ea639efb91a0e46ddbf3ee02db37b12bf09ae5d770c59`, 499,456 B) from the 0.3.6 base jar (`8335db03f015adc4a6a2464eee268fe7306f09d1abe96dec137f6474ba42a98f`) before any 0.3.8 change was made. Base verification repeated in this build folder: the unmodified tree was rebuilt and repacked and matched the shipped hash exactly. - **This folder:** `work/` is the full 0.3.8 source tree (the 0.3.7 tree plus the delta below); `base-0.3.6.jar` and `base-0.3.7-shipped.jar` are the reference jars; `tests/` holds the pure suites (the nine 0.3.7 suites copied unchanged, plus three new 0.3.8 suites); `rig/` holds Probe038, its prep script and evidence. - **Delta vs 0.3.7 (12 changed + 5 added jar entries, 0 removed):** - NEW `PagerHook.java` — pager but

## Built artifact (not included in this record — text only)

- Jar: `ExcelWandBars-0.3.8.jar` — 511977 bytes, SHA-256 `57b86601a8eafc87e16630cd32e725e9c3d329dd6d5e2cae8976008c0e5a37db`

## Install / live status

Installed live Nov 2, 2026.

## Record contents

- `src/` — the version's source tree (text files only; binaries and build outputs excluded, see FILE-LIST.txt)
- `plugin.yml`, `config.yml` — as shipped in this version (when the version ships a config)
- `TEST-RESULTS.txt` — the build folder's recorded test evidence
- `DEPENDENCIES.md` / `DEPENDENCIES.sha256` — upstream dependencies
- `FUNCTIONAL-CHANGE.diff` — change vs the previous version's tree, or a statement where no clean baseline exists
- `SANITIZATION.diff` — sensitive-content scan result
- `SOURCE-PROVENANCE.json` — provenance + per-file SHA-256 manifest
- `FILE-LIST.txt`, `SHA256SUMS` — inventory and checksums
