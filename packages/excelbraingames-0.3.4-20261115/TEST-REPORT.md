# ExcelBrainGames 0.3.4 — TEST REPORT

Build: EXC-BRAINGAMES-0.3.4-WRV-20261114-01-43bf6f. Every suite below
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
| SudokuBoxViewTest (carried) | 55,020 | 0 |
| BedrockPropertyTest (carried) | 1,223,793 | 0 |
| TileTest (carried) | 14,473 | 0 |
| **BackNavTest (NEW)** | **26** | **0** |
| **Total** | **1,924,741** | **0** |

The carried suites pass at EXACTLY their 0.3.3 counts (1,924,715) —
the behavior-unchanged proof for the generators, checkers, payouts,
cap, saves, Word Search geometry, the Sudoku box view and the text
trio. No carried test file was modified.

## Baseline (before any 0.3.4 change)

The 0.3.3 tree was copied to build-0.3.4/ and the copy verified
(src + tests diff-identical; the copied jar re-hashed to the 0.3.3
jar of record, 135,705 B / 64e93e17…). The same-day G1 pull proved
those bytes are also the LIVE jar (see BUILD-RECORD.md), so the
0.3.4 delta was applied to a tree that is simultaneously the audited
tree and the live build.

## BackNavTest — what the new checks pin

The Bukkit GUIs cannot be instantiated off-server, so the suite pins
the navigation contract where it lives — the renderers' slot→action
registrations in Views.java and the dispatch branches in
GuiListener.java, read from the build tree (-Dbuild), in the same
spirit as SudokuBoxViewTest's display-shape gate:

- Board control slots unchanged: SLOT_WORDS 51 / SLOT_NEW 52 /
  SLOT_CLOSE 53.
- Picker: exactly one `back:servermenu` registration, at slot 26,
  labelled "Back to Server Menu"; its handler branch closes the
  picker and runs performCommand("menu") — the ExcelMenu opener.
- Boards: `boardBackItem()` is defined once and placed by exactly the
  four renderers (WS, Memory, shared Sudoku controls, Sudoku 9x9
  palette); the three shared-control boards register
  `back:picker` at SLOT_CLOSE; NO renderer still emits `ctl:close`;
  total `back:picker` registrations = 8 (4 carried: theme, tier,
  resume, riddle — plus the 4 new).
- Sudoku 9x9 decision: Back at slot 51; Eraser registered at slot 49
  (the high bank); bank switch, check, page toggle and show-board
  actions all still present.
- The `back` handler saves player data BEFORE reopening the picker,
  and never nulls any round field (no discard on the Back path).
- Question card: exactly one `qback` registration, at slot 18;
  Finish now / Save & stop / Close registrations all survive; the
  `qback` handler clears typed-answer mode, saves, and reopens the
  picker, and contains no completion/banking call and no round
  nulling.

Build history, stated plainly: the first build.sh run failed in the
NEW suite only — one BackNavTest check asserted the back branch
contains no "= null" substring, which also matches the legitimate
`pd.wordSearch != null` guard in the board sub-branch. The check was
rewritten to look for actual round-field discard assignments; no
plugin source changed, so the fingerprint and build id were
unaffected. The two subsequent full runs are the artifact of record.

## Desk walk — the 13 BB-02 rows against the built contract

Both platforms share the same inventory GUIs (audit-established);
Sudoku splits by viewer platform (Java full board / Bedrock box
picker). "Mid-round Back" = start a round, make a move, tap Back:
the handler saves explicitly, the picker reopens showing the round
in progress, and Resume re-enters the same round.

| # | Screen | Platform | Back (slot) | Lands on | Mid-round save |
|---|---|---|---|---|---|
| 1 | Game picker | Java | 26, Back to Server Menu | ExcelMenu Server Menu (via /menu) | n/a (no round) |
| 2 | Game picker | Bedrock | 26, Back to Server Menu | ExcelMenu Server Menu (via /menu) | n/a |
| 3 | Word Search board | Java | 53, Back | Game picker | save + resume ✓ |
| 4 | Word Search board | Bedrock | 53, Back | Game picker | save + resume ✓ |
| 5 | Memory Match board | Java | 53, Back | Game picker | save + resume ✓ |
| 6 | Memory Match board | Bedrock | 53, Back | Game picker | save + resume ✓ |
| 7 | Sudoku full board 4x4/6x6 | Java | 53, Back | Game picker | save + resume ✓ |
| 8 | Sudoku full board 9x9 | Java | 51, Back | Game picker | save + resume ✓ |
| 9 | Sudoku box picker | Bedrock | 53, Back | Game picker | save + resume ✓ |
| 10 | Quick Math card | Java | 18, Back | Game picker | save + resume ✓ |
| 11 | Quick Math card | Bedrock | 18, Back | Game picker | save + resume ✓ |
| 12 | Word Scramble card | Java | 18, Back | Game picker | save, NOT banked ✓ |
| 13 | Word Scramble card | Bedrock | 18, Back | Game picker | save, NOT banked ✓ |

Rows 7+8 are the audit's two separate Java table rows for the Sudoku
full board (4x4/6x6 and 9x9). The Sudoku full-board Bedrock row and
the box-picker Java row are platform-not-served rows (audit "Not
defects") and are correctly outside the 13.

## Rig

Two boots on Paper 26.3-235 (logs in
`~/workspace/back-button-wave1-r354/BB-02/rig/`): without Floodgate —
Done (56.647s), SEVERE 0, `platform-detection=unavailable`; with
Geyser + Floodgate — Done (33.976s), SEVERE 0, `floodgate detected`,
`platform-detection=floodgate`. Both print the 0.3.4 build id in the
enable line.

## Not proven here (named, not hidden)

Real-client rendering and tap walks on Java and Bedrock clients —
the mirror walk in the full sense — cannot run in this desk lane
(no Minecraft client). They are the install packet's player proof:
Mike's spot taps (batch acceptance) covering each fixed screen and a
mid-round Back per board. The contract, slots, handlers, save
semantics and boots above are the desk-side proof those taps verify.
