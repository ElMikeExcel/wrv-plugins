# ExcelBrainGames 0.3.3 — SPEC DELTA (Sudoku visual box view for Bedrock)

Against: SPEC.md (Brain Games, settled Nov 4, 2026) and the live 0.3.2
build. Commissioned: Mike's Nov 5–7, 2026 verdict on the 0.3.1 chat
text grid — "helpful but still very confusing, borderline unplayable" —
and his design approval in his own words on Nov 7, 2026, 7:44 PM CST
("Build the Sudoku box view"), given after he rejected a non-visual
form-based variant the same evening ("So it basically won't be
visual? Sounds lame."). Playability was confirmed with him before the
approval (7:06 PM): on cell selection you see the box (visual) + the
full row + the full column (context line) = the complete Sudoku
constraint set; digit entry via a 3×3 digit picker; the one honest
tradeoff is slower whole-board scanning (the box picker shows overall
progress, and the full chat board stays one tap away).

## 1. The defect

On Bedrock phones, Geyser presents the 54-slot inventory GUI as a
linear stream wrapped ~5 wide, so Sudoku's 9×9 board cannot render
as a square and taps require counting stream positions. The 0.3.1
chat grid mitigated reading the board but left PLAYING it as
counting. A 9×9 square cannot be rendered at wrap width ~5 —
arithmetic, not effort — so 0.3.3 pages the presentation instead.

## 2. The fix — one board, viewer-keyed presentation paging

The key architectural difference from Word Search (0.3.2): Sudoku's
underlying board is the SAME 9×9 on both platforms. The box view is
a paging of the presentation, not a different board shape, so the
presentation keys off the VIEWER's platform and the round carries no
presentation state at all.

- **New pure logic class `games/sudoku/SudokuBoxView`** — no Bukkit
  imports, unit-tested directly. Box structure comes from the round's
  own boxRows×boxCols; three slot maps, all on the phone's stream
  stride (DISPLAY_WIDTH = 5): the box view (cell at localRow*5 +
  localCol, so the wrap IS the square), the box picker (box at
  boxRow*5 + boxCol — itself a grid of boxes), and the digit picker
  (digits in rows of three on the same stride). Box completion
  (finished ⇔ all nine cells non-zero AND equal to the solution) and
  the context lines live here too; the row line is built with
  SudokuTextGrid's exact cell formatting, byte-identical to the
  grid's own row line.
- **Bedrock presentation (Views/GuiListener)** — opening Sudoku as a
  Bedrock viewer lands on the box picker: one entry per box, a
  finished box shown as a glinting lime pane ("Finished — every
  square right!"), a full-but-wrong box as a yellow pane, anything
  else as paper with its filled count. Opening a box shows its cells
  as digit tiles at the stream offsets above. Tapping a cell prints
  the context line in chat (full row + full column) on EVERY tap;
  a non-given cell then opens the 3×3 digit-tile picker with an
  erase tile (same place/erase semantics as Java — the picker calls
  the round's own place/erase), while a given cell just reports
  itself and stays. The full-board chat print (SudokuTextGrid) stays
  one tap away from both the picker and the box view ("Show board in
  chat"). The check flow is shared: the Java palette's check action
  was extracted into one method both presentations call (identical
  cap logic, sounds, messages, and 80-tick wrong-cell highlight), so
  the two presentations cannot drift apart on rules.
- **Viewer-keyed, decided once at open.** `Views.sudokuBoard`
  branches on the existing `platformOf(player)`: Floodgate-known →
  BEDROCK (box view), anything else → JAVA (the full-board view).
  Unknown platform → Java presentation, the safe default. Because
  the board is identical, cross-platform resume is inherently
  unscrambled — and is proven in tests rather than assumed
  (SudokuBoxViewTest asserts the reloaded round's board, solution,
  givens, every slot map, every context line, and completion state
  are identical, and that it solves through the same calls).
- **Decisions taken under the brief's delegation:** (a) opening
  always lands on the box picker — the simpler consistent behavior;
  the last-viewed box is never persisted (GUI holder state is
  transient by design, like the existing sudokuPage/bank state);
  (b) the box view applies to ALL Sudoku sizes on Bedrock, with the
  box shape being the round's own (kid 2×2, six 2×3, nines 3×3);
  (c) no automatic chat-grid print on Bedrock open — the picker IS
  the landing surface, and the grid is one tap away (Java open still
  prints it, unchanged).

## 3. What did NOT change

- **Java presentation:** the full 9×9 board exactly as today — the
  Java code path is byte-identical, including the chat-grid print on
  open.
- **The Sudoku generator, checker, tiers, givens, payouts, mistake
  rules, the $1,500/day cap** — untouched; the carried suite passes
  at exactly its 0.3.2 counts with no carried test file modified.
- **Save format: NO new key at all** — stronger than the brief's
  allowance (an appended, old-jar-ignored key). Presentation state
  lives only in the transient GUI holder. A 0.3.3 save is
  byte-for-byte a 0.3.2 save (the new suite asserts the exact 0.3.2
  key set, in order), so rollback carries no save caveat of any kind.
- **config.yml:** byte-identical to 0.3.2's — the install is a jar
  swap only, with NO config merge (the packet says so plainly).
- **The tile packs** (hashes reproduce 0.3.2's in build.sh), **Word
  Search and every other game**, and the compass menu entry:
  untouched.

## 4. Proof (see TEST-REPORT.md for the full table)

- The full carried suite passes at exactly its 0.3.2 counts
  (1,869,695 checks across all suites at baseline; carried portion
  unchanged) — the behavior-unchanged proof for everything outside
  the Sudoku Bedrock presentation.
- **SudokuBoxViewTest** (new): (a) box-view slot mapping roundtrips
  for every box and cell at all three sizes, with display position ==
  local position; (b) the display-shape behavior gate — box, picker,
  and digit-picker streams rendered at wrap width 5 hold their
  shapes, and the SAME cells on the legacy 9-wide Java map do NOT
  (negative control, the harness reproduces the defect class);
  (c) digit-picker mapping plus place/erase through the round's own
  rules (givens locked, out-of-range refused); (d) box-picker
  completion states match board state exactly (blank breaks it,
  wrong digit breaks it, other boxes unaffected); (e) context-line
  correctness — the row line equals SudokuTextGrid's own row line
  byte-for-byte and the column line is rebuilt independently from
  the board; (f) cross-platform resume identity, including the exact
  0.3.2 save key set; (g) unknown platform → Java presentation.
- **Rig:** the built jar boots on the exact live Paper build
  (26.3-235) twice — without Floodgate (`platform-detection=
  unavailable (Java presentation for all)`) and with the live
  Floodgate 2.2.5-b141 + Geyser present (`platform-detection=
  floodgate`, bind line naming the box view). Boot logs in `rig/`.

## 5. Known limits / residuals

- **Whole-board scanning on Bedrock is slower than Java's single
  screen** — the tradeoff named to Mike before approval and accepted:
  the box picker shows overall progress at a glance, the context
  line carries the row/column constraints on every tap, and the full
  chat board is one tap away.
- **Real-client rendering still cannot be rig-proven.** The rig
  proves boot, binding, and zero SEVERE; what a phone actually draws
  is settled by the packet's player proof (Mike on Bedrock plays
  Sudoku — box renders as a square, taps need no counting). Same
  residual class as 0.3.2's Word Search proof.
- Opening the same round from the other platform shows THAT
  platform's presentation of the identical board — by design; the
  board itself never changes shape, so nothing can scramble.
