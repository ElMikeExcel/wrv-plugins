# ExcelWandBars 0.3.5 — source record

Plugin: ExcelWandBars  
Version: 0.3.5  
Record assembled: Nov 3, 2026 (records catch-up pass)  
Local build folder: `wandbars-ux-build`

## What this is

Version **0.3.5** of ExcelWandBars (Wink Reyes Valley, family Minecraft server): the four WandBars UX features built as one combined release, per `~/workspace/wandbars-drag-remove/DESIGN.md`:

## Built artifact (not included in this record — text only)

- Jar: `ExcelWandBars-0.3.5.jar` — 479992 bytes, SHA-256 `3a5c0d0db3493375759f11688d2356ddd52bed8d0a7fffb8472e5a52763cd622`

## Install / live status

Installed live Oct 30, 2026 (boot 6:04:36 PM CDT) under the live filename plugins/ExcelWandBars-0.3.4.jar — the version lives in plugin.yml, not the filename. Superseded by the 0.3.6/0.3.7 line.

## Record contents

- `src/` — the version's source tree (text files only; binaries and build outputs excluded, see FILE-LIST.txt)
- `plugin.yml`, `config.yml` — as shipped in this version (when the version ships a config)
- `TEST-RESULTS.txt` — the build folder's recorded test evidence
- `DEPENDENCIES.md` / `DEPENDENCIES.sha256` — upstream dependencies
- `FUNCTIONAL-CHANGE.diff` — change vs the previous version's tree, or a statement where no clean baseline exists
- `SANITIZATION.diff` — sensitive-content scan result
- `SOURCE-PROVENANCE.json` — provenance + per-file SHA-256 manifest
- `FILE-LIST.txt`, `SHA256SUMS` — inventory and checksums


---

**Packaging note (records catch-up, Nov 3, 2026):** this record's `src/` is the 0.3.5 candidate working tree (`wandbars-ux-build/work/src`) identified by the build folder's SOURCE.md; `work/shipped/plugin.yml` reads version 0.3.5 and the candidate jar hashes to the recorded value. One completeness caveat, stated plainly: the shipped jar is a repack over the shipped 0.3.4 fake-tap jar — 26 shipped classes (FirstObservationFloor x11, `wrv/magic/eligibility` x15) have no source in any local tree and are not represented in `src/` (they carried over byte-for-byte and this release did not touch them), and the 5 Geyser hook classes' shipped bytes were pinned from the base jar rather than recompiled. `src/` covers everything 0.3.5 changed, but cannot by itself reproduce the shipped jar from scratch. FUNCTIONAL-CHANGE.diff is taken against `work/base-src`, the build's own base tree, verified before the change to reproduce the shipped 0.3.4 jar byte-for-byte for every class this release modifies (see SOURCE.md and BASE-VERIFICATION.md).
