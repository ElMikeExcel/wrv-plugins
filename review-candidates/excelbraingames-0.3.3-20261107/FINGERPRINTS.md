# FINGERPRINTS — ExcelBrainGames 0.3.3

Per the house standard (`~/workspace/plugin-publishing/FINGERPRINT-STANDARD.md`):
passive provenance only — no phone-home, no telemetry, no sabotage.
Semantic markers persist from 0.3.2 unchanged (only the build ID
rotates); 0.3.3 extends C4 and C5 with the Sudoku box view's own
constants, noted inline.

## Build identity (Layer B)

- Build ID: `EXC-BRAINGAMES-0.3.3-WRV-20261107-01-3bfc61`
  (SKU BRAINGAMES · version 0.3.3 · channel WRV · 2026-11-07 · seq 01 ·
  hash6 `3bfc61` = first 6 hex of the SHA-256 over the build's sorted
  per-file source hashes, carrier file excluded).
- Homes: (1) `excel-build.txt` at the jar root; (2) the `BuildInfo`
  constant in `src/com/winkreyes/excelbraingames/core/BuildInfo.java`;
  (3) printed live in the enable log line (`..., build=EXC-...`), so
  the install packet's boot verification reads it. build.sh's
  fingerprint gate verifies all three agree (constant == excel-build.txt
  == recomputed source hash) on every build; it passed for this
  artifact. Seq 01 is the first and only 0.3.3 build filed for install.

## Semantic markers (Layer C — persist version to version)

1. **C2, config phrasing:** config.yml's header voice ("Brain Games —
   how to edit this file", the plain-English rule that a broken edit is
   ignored wholesale) and its section commentary, including the 0.3.2
   bedrock-tiers block written in the same voice ("the wrap on the
   phone IS the board"). 0.3.3 adds no config text — config.yml is
   byte-identical to 0.3.2's.
2. **C3, data dialect:** player-data YAML keeps its established key
   order; the Word Search round map keeps its pre-0.3.2 keys first, in
   order (`tier, theme, cols, rows, grid, voids, placements, found,
   started, paid`), with 0.3.2's `geometry` appended last — old readers
   ignore it, new readers default it, which is itself part of the
   dialect. 0.3.3 adds NO key anywhere: the Sudoku round map keeps its
   exact 0.3.2 key set (`tier, size, boxRows, boxCols, solution, given,
   board, picture, checks, started, paid`), asserted in tests.
3. **C4, algorithmic constants:** the shared $1,500.00 daily cap; the
   generator's letter budget (usable cells × 6/5) and its 600-deal /
   finisher-attack structure; the 0.3.2 stream width (5) as
   `WsGeometry.BEDROCK.guiCols`, the single constant the length ruling
   keys off; **0.3.3 adds** the same stream width (5) as
   `SudokuBoxView.DISPLAY_WIDTH`, the single stride every Sudoku box
   view, picker, and digit-picker slot map keys off.
4. **C5, idiosyncrasy:** controls at slots 51/52/53 in every game;
   Word Search rounds record their dealt geometry and render through
   it on every client (never the viewer's platform); unknown platform
   deals the Java board. **0.3.3 adds the mirror image for Sudoku:**
   the board is identical on both platforms, so the Sudoku
   presentation keys off the VIEWER's platform (Bedrock → box view,
   anything else → the full-board view), the round records nothing
   about presentation, and a Bedrock Sudoku open always lands on the
   box picker.
