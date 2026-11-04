# ExcelWandBars 0.3.10 — source record

Plugin: ExcelWandBars  
Version: 0.3.10  
Record assembled: Nov 3, 2026 (records catch-up pass)  
Local build folder: `wandbars-ux-build/build-0.3.10`

## What this is

- **Base:** the shipped ExcelWandBars 0.3.9 source tree (`../build-0.3.9/work/src`), copied unmodified into `work/src`. Base rebuild gate: the unmodified tree compiled and repacked byte-identical to the shipped 0.3.9 candidate before any change — SHA-256 `6652e6bebb067ae10b423bcf6e730e4a16ec28f3aaee92f90b1f1057154fd70d` (520,282 B; reference jar kept in this folder as `ref-0.3.9.jar`, alongside `base-0.3.6.jar`). - **Candidate:** `ExcelWandBars-0.3.10.jar` — SHA-256 `0afbf993282df6447533e3b3b753eca3c96fb0a4dcf741078b7c7b90973a8461`, 529,841 B, 233 entries, built twice byte-identical. - **Toolchain:** Temurin/JDK 25 (`~/workspace/.jdk25/bin`), `javac --release 25` over `work/cp.txt` + `work/sources.txt` into `work/classes`; repack via `work/repack.py base-0.3.6.jar work/classes work/shipped <out>` (0.3.6 base jar, 5 Geyser hook classes pinned to shipped bytes, sorted entries, 2026-01-01 t

## Built artifact (not included in this record — text only)

- Jar: `ExcelWandBars-0.3.10.jar` — 529841 bytes, SHA-256 `0afbf993282df6447533e3b3b753eca3c96fb0a4dcf741078b7c7b90973a8461`

## Install / live status

Installed live Nov 3, 2026, 11:37-11:53 AM CST. Current live version.

## Record contents

- `src/` — the version's source tree (text files only; binaries and build outputs excluded, see FILE-LIST.txt)
- `plugin.yml`, `config.yml` — as shipped in this version (when the version ships a config)
- `TEST-RESULTS.txt` — the build folder's recorded test evidence
- `DEPENDENCIES.md` / `DEPENDENCIES.sha256` — upstream dependencies
- `FUNCTIONAL-CHANGE.diff` — change vs the previous version's tree, or a statement where no clean baseline exists
- `SANITIZATION.diff` — sensitive-content scan result
- `SOURCE-PROVENANCE.json` — provenance + per-file SHA-256 manifest
- `FILE-LIST.txt`, `SHA256SUMS` — inventory and checksums
