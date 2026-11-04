# ExcelWandBars 0.3.7 — source record

Plugin: ExcelWandBars  
Version: 0.3.7  
Record assembled: Nov 3, 2026 (records catch-up pass)  
Local build folder: `wandbars-0.3.7`

## What this is

0.3.7 makes plain drag-and-drop a real spell-bar write path (Mike's live verdict on 0.3.6, Nov 1, 2026, 7:25 PM CST: Arrange works, but dragging the Ward spell onto a hotbar slot with the wand in hand did not stay, and neither did dragging Wolf from the double-tap all-spells view — dragging a spell onto the bar must just work, his original Oct 31 expectation), and it fixes the spell position labels to name the bar AND the slot (Mike, same evening, 7:29 PM CST: "bar 17" for Cure is a flattened index, not a bar; Mend is Bar 1 · Slot 1).

## Built artifact (not included in this record — text only)

- Jar: `ExcelWandBars-0.3.7.jar` — 499456 bytes, SHA-256 `fc77e22f40e4719da86ea639efb91a0e46ddbf3ee02db37b12bf09ae5d770c59`

## Install / live status

Shipped live before the 0.3.8 pager rework (jar verified byte-identical to the rebuilt tree, per the 0.3.8 build record).

## Record contents

- `src/` — the version's source tree (text files only; binaries and build outputs excluded, see FILE-LIST.txt)
- `plugin.yml`, `config.yml` — as shipped in this version (when the version ships a config)
- `TEST-RESULTS.txt` — the build folder's recorded test evidence
- `DEPENDENCIES.md` / `DEPENDENCIES.sha256` — upstream dependencies
- `FUNCTIONAL-CHANGE.diff` — change vs the previous version's tree, or a statement where no clean baseline exists
- `SANITIZATION.diff` — sensitive-content scan result
- `SOURCE-PROVENANCE.json` — provenance + per-file SHA-256 manifest
- `FILE-LIST.txt`, `SHA256SUMS` — inventory and checksums
