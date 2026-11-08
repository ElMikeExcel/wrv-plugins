# ExcelBrainGames 0.3.3 — TEST REPORT

Build: EXC-BRAINGAMES-0.3.3-WRV-20261107-01-3bfc61. Every suite below
ran inside build.sh on both full script runs, with identical counts.

## Totals

| Suite | Checks | Failures |
|---|---|---|
| WordSearchTest (carried) | 20,416 | 0 |
| MemoryTest (carried) | 3,039 | 0 |
| SudokuTest (carried) | 553 | 0 |
| SudokuPropertyTest (carried) | 15,006 | 0 |
| TextTest (carried) | 103 | 0 |
| TextPropertyTest (carried) | 83,244 | 0 |
| LedgerTest (carried) | 55 | 0 |
| PropertyTest (carried) | 499,811 | 0 |
| GeometryTest (carried) | 9,202 | 0 |
| **SudokuBoxViewTest (NEW)** | **55,020** | **0** |
| BedrockPropertyTest (carried) | 1,223,793 | 0 |
| TileTest (carried) | 14,473 | 0 |
| **Total** | **1,924,715** | **0** |

The carried suites pass at EXACTLY their 0.3.2 counts (1,869,695) —
the behavior-unchanged proof for the generator, checker, payouts,
cap, saves, Word Search, and the text trio. No carried test file was
modified.

## Baseline (before any 0.3.3 change)

The 0.3.2 tree was copied to build-0.3.3/ and re-proven untouched in
a scratch copy first: full suite at exactly the recorded counts
(1,869,695 checks, 0 failures), packs reproducible at 0.3.2's hashes
(java 05942eed… / bedrock 486bcad6…), and the rebuilt jar's SHA-256
equal to the 0.3.2 jar of record (`8ef0465010bc…`, 130,850 B). The
0.3.3 delta was then applied to the verified tree.

## SudokuBoxViewTest — what the new checks pin

All three shipped tier shapes (kid 4×4 / 2×2 boxes, 6×6 / 2×3,
9×9 / 3×3) throughout.

- **(a) Slot mapping:** for every box and every cell, slotOfCell's
  display position (slot / 5, slot % 5) IS the cell's local position
  inside the box; cellAtSlot inverts slotOfCell exactly and returns
  -1 exactly off the box's cells (and for -1 / 54). The box picker
  and digit picker maps round-trip the same way, with no two entries
  ever sharing a slot.
- **(b) Display-shape behavior gate:** each box view, the box
  picker, and the digit picker are rendered into a simulated 54-slot
  stream wrapped at phone width 5, over generated rounds at all
  sizes: the box's cells occupy exactly the box-shaped block at
  display rows 0..boxRows-1, cols 0..boxCols-1, in local row-major
  order, with NO marker anywhere else in the stream. **Negative
  control:** the SAME cells laid out on the legacy Java map (global
  row×9 + col) fail the same shape check at width 5 for every box —
  the harness reproduces the exact defect class Mike hit (bottom
  boxes of a 9×9 fall off the Java board's 54-slot page entirely),
  so the gate is not vacuous.
- **(c) Digit entry:** every digit 1..size maps through the picker
  to its tile slot and back; placing each digit into an empty square
  through the round's own place() holds the digit; erase clears it;
  givens refuse placement and erase; digits 0 and size+1 and indexes
  -1 / size² are refused; placing the solution digits through the
  same calls solves the board.
- **(d) Box completion states:** on a crafted solved board every box
  is finished with a full filled count; zeroing one cell or writing
  one wrong digit un-finishes exactly that box and no other; a board
  with only one box filled shows exactly that box finished; on fresh
  generated rounds boxFinished equals a direct board-vs-solution
  comparison for every box.
- **(e) Context lines:** for every cell of generated rounds (with
  played moves on the board), rowLine is byte-identical to the row
  line SudokuTextGrid itself renders for that row (extracted from
  SudokuTextGrid.lines), and colLine equals a column string rebuilt
  independently from the board's values, top to bottom, with the
  grid's own cell formatting and box-boundary separators.
- **(f) Cross-platform resume:** rounds played on "one platform"
  (correct placements + a placed-and-erased mistake) round-trip
  through toMap/fromMap with tier, size, box shape, solution, board,
  givens, checks, start time, and picture flag identical; every
  presentation function (boxOf, slotOfCell, slotOfBox, boxFinished,
  rowLine, colLine for every cell) is identical on the reloaded
  round; the reloaded round then solves through the same place()
  calls. The save's key set is asserted to be EXACTLY the 0.3.2 set,
  in order (`tier, size, boxRows, boxCols, solution, given, board,
  picture, checks, started, paid`) — 0.3.3 adds no key.
- **(g) Viewer-platform decision:** useBoxView is true only for
  BEDROCK; JAVA, and parse(null / empty / junk) — the unknown
  platform — all take the Java full-board presentation.

## Rig

Two boots on Paper 26.3-235 (logs in `rig/`): without Floodgate —
Done (25.771s), SEVERE 0, bind-absent line printed, enable line
`platform-detection=unavailable (Java presentation for all)`; with
the live Geyser 2.12.1 + Floodgate 2.2.5-b141 — Done (29.109s),
SEVERE 0, bind line `floodgate detected — Bedrock players get the
5-wide Word Search board and the Sudoku box view.`, enable line
`platform-detection=floodgate`. Both end with the build id above.
Details in BUILD-RECORD.md.

## Not proven here (named, not hidden)

Real-client rendering (what a phone draws from the box-view stream
layout) is settled by the install packet's player proof — Mike on
Bedrock plays Sudoku: the box reads as a square, taps need no
counting, the context line shows the full row and column. The same
residual class as 0.3.2's Word Search proof. The 0.3.1 pack-declined
question stays open and unchanged.
