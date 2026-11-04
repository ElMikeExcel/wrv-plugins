# ExcelWandBars 0.3.9 — source record

Plugin: ExcelWandBars  
Version: 0.3.9  
Record assembled: Nov 3, 2026 (records catch-up pass)  
Local build folder: `wandbars-ux-build/build-0.3.9`

## What this is

- **Base:** the shipped ExcelWandBars 0.3.8 source tree (`../build-0.3.8/work/src`), copied unmodified into `work/src`. Base rebuild gate: the unmodified tree compiled and repacked byte-identical to the shipped 0.3.8 candidate before any change — SHA-256 `57b86601a8eafc87e16630cd32e725e9c3d329dd6d5e2cae8976008c0e5a37db` (511,977 B; reference jar kept in this folder as `ExcelWandBars-0.3.8.jar`, alongside `base-0.3.6.jar` and `base-0.3.7-shipped.jar`). - **Candidate:** `ExcelWandBars-0.3.9.jar` — SHA-256 `6652e6bebb067ae10b423bcf6e730e4a16ec28f3aaee92f90b1f1057154fd70d`, 520,282 B, 231 entries, built twice byte-identical. - **Toolchain:** Temurin/JDK 25 (`~/workspace/.jdk25/bin`), `javac --release 25` over `work/cp.txt` + `work/sources.txt` into `work/classes`; repack via `work/repack.py base-0.3.6.jar work/classes work/shipped <out>` (0.3.6 base jar, 5 Geyser hook classes pinned to shipp

## Built artifact (not included in this record — text only)

- Jar: `ExcelWandBars-0.3.9.jar` — 520282 bytes, SHA-256 `6652e6bebb067ae10b423bcf6e730e4a16ec28f3aaee92f90b1f1057154fd70d`

## Install / live status

Installed live Nov 3, 2026 (early morning). Player proof Nov 3: empty-slot moves work; occupied-slot replace still fails on Bedrock — not fixed by this version.

## Record contents

- `src/` — the version's source tree (text files only; binaries and build outputs excluded, see FILE-LIST.txt)
- `plugin.yml`, `config.yml` — as shipped in this version (when the version ships a config)
- `TEST-RESULTS.txt` — the build folder's recorded test evidence
- `DEPENDENCIES.md` / `DEPENDENCIES.sha256` — upstream dependencies
- `FUNCTIONAL-CHANGE.diff` — change vs the previous version's tree, or a statement where no clean baseline exists
- `SANITIZATION.diff` — sensitive-content scan result
- `SOURCE-PROVENANCE.json` — provenance + per-file SHA-256 manifest
- `FILE-LIST.txt`, `SHA256SUMS` — inventory and checksums
