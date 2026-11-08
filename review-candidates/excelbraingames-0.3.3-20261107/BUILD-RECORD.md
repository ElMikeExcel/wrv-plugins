# ExcelBrainGames 0.3.3 — BUILD RECORD

**Build ID:** EXC-BRAINGAMES-0.3.3-WRV-20261107-01-3bfc61
**Built:** Nov 7, 2026 (America/Chicago), from `~/workspace/brain-games/build-0.3.3/`
**Jar:** `ExcelBrainGames-0.3.3.jar` — 135,705 bytes,
SHA-256 `64e93e17f4d3fdd14acd9284b874902001188f9eaedf3687d83ef328589d6de1`
**Commission:** Mike's Nov 5–7, 2026 verdict on the 0.3.1 Sudoku chat
grid ("helpful but still very confusing, borderline unplayable") and
his design approval in his own words, Nov 7, 2026, 7:44 PM CST.
Spec: `SPEC-DELTA.md` in this folder.
**Status at build close:** NOT installed. Install packet filed as a
gated MANIFEST entry (see INSTALL-PACKET.md); this build does not
touch the panel or the running server.

## What this build is

The Sudoku visual box view for Bedrock. Sudoku's board is the SAME
9×9 on both platforms, so — the mirror image of Word Search 0.3.2 —
the presentation keys off the VIEWER's platform and the round
records nothing: a Bedrock viewer opening Sudoku lands on a box
picker (one entry per box, finished boxes marked glinting lime,
full-but-wrong boxes yellow), opens one box at a time as a true
visual square (its cells sit at stream offsets on the phone's
5-wide wrap — the same stream trick as the 0.3.2 Word Search board),
taps a cell to get a context line in chat (that cell's full row +
full column, in SudokuTextGrid's own formatting) and, for a
non-given cell, a 3×3 digit-tile picker with erase. The full-board
chat print stays one tap away. Unknown platform → the Java
full-board view. Java presentation, the generator, the checker,
tiers, givens, payouts, mistake rules, the $1,500/day cap, the tile
packs, Word Search and every other game are unchanged — and the
save format gains NO key at all: a 0.3.3 save is byte-for-byte a
0.3.2 save (asserted in the new suite).

Authorship: `authors: [Iris, Mike]` per the settled credit rule.

## Baseline verification (0.3.2 tree → this build's starting point)

The build-0.3.2 tree was copied to build-0.3.3/ (src/tests
diff-verified identical after the copy) and re-proven BEFORE any
change, in a scratch copy of the tree:

- Full suite at exactly 0.3.2's recorded counts: 1,869,695 checks,
  0 failures.
- Packs reproducible at 0.3.2's hashes (java 05942eed… /
  bedrock 486bcad6…).
- The rebuilt jar's SHA-256 equals the 0.3.2 jar of record exactly
  (`8ef0465010bc3046efe18a7e2ada658bd730d44d6e74a76dc83a2a60de124511`,
  130,850 B) — the tree copied is the tree that shipped.

## Source delta vs the 0.3.2 tree (diff-verified)

- NEW `games/sudoku/SudokuBoxView.java` — pure logic, no Bukkit:
  box structure from the round's own boxRows×boxCols; the box-view,
  box-picker, and digit-picker slot maps on the stream stride
  (DISPLAY_WIDTH = 5); boxFilled / boxFinished (finished ⇔ all nine
  cells non-zero and equal to the solution); rowLine / colLine
  context lines built with SudokuTextGrid's exact cell formatting.
- `gui/BrainHolder.java` — three new views (SUDOKU_BOXES,
  SUDOKU_BOX, SUDOKU_DIGITS) and two transient fields (sudokuBox,
  sudokuCell). Presentation state lives only here, never in a save.
- `gui/Views.java` — `sudokuBoard` branches on the viewer's platform
  (Bedrock → the picker; the Java path is byte-identical, chat-grid
  print included); new renderers for the picker, the box view, and
  the digit picker; `printSudokuContext` (context line on a cell
  tap); the picture legend extracted verbatim from printSudokuGrid
  and shared by both prints.
- `gui/GuiListener.java` — dispatch + handlers for the three new
  views; the Java palette's check action extracted into one shared
  `sudokuCheck` both presentations call (same cap logic, sounds,
  messages, 80-tick highlight — the rules cannot drift apart).
- `ExcelBrainGames.java` — javadoc; the enable line's
  platform-detection wording generalized ("Java presentation for
  all"); both Floodgate bind log lines now name the box view.
- `bedrock/BedrockHook.java` — javadoc only (a second platform
  decision now reads the same hook).
- `core/BuildInfo.java` — the 0.3.3 build id (fingerprint carrier).
- `plugin.yml` — version 0.3.3 + one appended description sentence.
- Tests: NEW `SudokuBoxViewTest.java`. No carried test file modified
  (diff-verified). `SudokuRound.java` and `SudokuTextGrid.java` are
  NOT touched; `config.yml`, the word lists, and the menu entry are
  byte-identical to 0.3.2's (diff-verified; config SHA-256
  68eb3db30f1288279e8427a3372247a4a24a05dc8964748b35385ec42e4723de
  on both trees).
- `build.sh` — 0.3.3 header + descriptor gate, the new suite, jar
  name. Build inputs unchanged from 0.3.2.

## Build inputs (all pinned in build.sh)

- Paper API 26.3 build 231 — SHA-256 f0df9e7f… (as 0.3.2).
- Vault 2.0.0 — SHA-256 8b2ce431… (as 0.3.2).
- Floodgate API jar (compile-only) — SHA-256 21570aff… (as 0.3.2).
- JDK: ~/workspace/.jdk25 (javac --release 21), as 0.3.2.

## Tests

1,924,715 checks, 0 failures — see TEST-REPORT.md. Carried suite at
EXACTLY its 0.3.2 counts (1,869,695): the behavior-unchanged proof.
New: SudokuBoxViewTest (55,020) — slot-map roundtrips, the
display-shape behavior gate at wrap width 5 with the legacy-map
negative control, digit entry, box completion states, context-line
equality against the grid's own lines, cross-platform resume
identity including the exact 0.3.2 save key set, and the
unknown-platform default.

Build history, stated plainly: the first 0.3.3 script run failed in
the NEW suite's negative control — the harness wrote legacy-map
slots past the 54-slot inventory (the Java board pages at 54) and
threw. The fix touched only the new test file (off-page cells are
absent from the stream, as they are in the real GUI); no plugin
source changed, so the fingerprint and build id were unaffected.
The two subsequent full runs are the artifact of record.

## Determinism

build.sh ran to completion twice end-to-end (plus its internal
double assembly and double pack generation on every run): both runs
produced the identical jar (SHA-256 above) and identical suite
counts. Packs on every run: java 05942eed… / bedrock 486bcad6… —
0.3.2's recorded hashes, unchanged.

## Rig (Paper 26.3-235, the exact live build; logs in rig/)

- **Rig A (no Floodgate present)** — `rig/boot.log`: boot Done
  (25.771s), SEVERE 0; bind-absent line reads `floodgate not present
  — Word Search deals the Java geometry to everyone and Sudoku
  shows the full board.`; enable line ends `platform-detection=
  unavailable (Java presentation for all), build=EXC-BRAINGAMES-
  0.3.3-WRV-20261107-01-3bfc61`. The unknown-platform default, proven
  at plugin level.
- **Rig B (live platform pair present)** — `rig/boot-floodgate.log`:
  Geyser 2.12.1 + Floodgate 2.2.5-b141 (the live builds) added to
  the same rig: boot Done (29.109s), SEVERE 0; log carries
  `floodgate detected — Bedrock players get the 5-wide Word Search
  board and the Sudoku box view.` and the enable line reads
  `platform-detection=floodgate`. The bind path, proven against the
  live Floodgate build.
- Rig noise is the sandbox's offline Mojang services (SSL discovery
  / version-fetch failures, and in Rig B Geyser's own discovery
  shutdown for the same reason; economy=NONE is a rig artifact — no
  economy provider in the rig). None of it is attributable to this
  plugin; both logs show zero SEVERE and no ExcelBrainGames ERROR.

## Records

Package staged at
`~/workspace/records-catchup-20261103/packages/excelbraingames-0.3.3-20261107/`
with PUSH-NOTES.md (draft PR + Drive legs are the parent/board-manager's).
FILE-LIST.txt + SHA256SUMS in this folder cover the build tree
(excluding work/ and the rig server internals; SHA256SUMS hashes
every file FILE-LIST.txt names).
