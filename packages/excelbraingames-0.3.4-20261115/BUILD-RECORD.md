# ExcelBrainGames 0.3.4 — BUILD RECORD

**Build ID:** EXC-BRAINGAMES-0.3.4-WRV-20261114-01-43bf6f
**Built:** Nov 14, 2026 (America/Chicago), from `~/workspace/brain-games/build-0.3.4/`
**Jar:** `ExcelBrainGames-0.3.4.jar` — 136,140 bytes,
SHA-256 `52d498e720b00a65cd9de5878e046d870d1fe140db399055338a91d46dc329d3`
**Commission:** the Back-button audit (Nov 13, 2026) and its fix-batch
split (FIXBATCH-SPLIT-R348.md §2/§5) — unit BB-02: ExcelBrainGames'
13 rows (11 FIX-1 + 2 FIX-2). Board supervisor run 354 dispatch.
**Status at build close:** NOT installed. Packet draft staged in
`~/workspace/back-button-wave1-r354/BB-02/` (INSTALL-PACKET.md +
RECEIPTS.md + MANIFEST-ENTRY-DRAFT.md); this build does not touch
the panel, the MANIFEST, or the running server.

## What this build is

The Back-button pass. Standing rule (Mike, Nov 13, 2026): every menu
and submenu ALWAYS carries a Back button. ExcelBrainGames' gaps were
the audit's worst single screen (the game picker, with no Back and no
Close at all) plus every board and the shared question card:

- **Game picker (FIX-2, Java + Bedrock):** new "Back to Server Menu"
  at slot 26 (ARROW), running the ExcelMenu `/menu` opener — the same
  opener the fleet's other Back-to-Menu controls use (ExcelMenu
  registers `menu`, verified in its 3.0.1 plugin.yml).
- **Word Search board, Memory Match board (FIX-1, both platforms),
  Sudoku 4x4/6x6 full board and the Bedrock Sudoku box picker
  (FIX-1):** the slot-53 control becomes Back. It performs the old
  Close's save (the listener now saves player data explicitly on the
  Back path) and reopens the game picker. A round is never discarded
  by going back; ESC still bare-closes, saved, exactly as before.
  Slot 53 is the only possible home on the largest boards — a Word
  Search 9x6 board fills slots 0-50 with cells and 51/52 already
  carry Words/New — so Back takes the Close slot rather than a cell.
- **Sudoku 9x9 full board (FIX-1, Java):** the board had no Back and
  not even a Close (the palette takes slots 51-53). SLOT DECISION:
  slot 51 becomes Back; the Eraser rides the high palette bank
  (slots 45-49 = digits 6-9 + Eraser, one bank switch away, like a
  tenth palette entry). No cell, digit, check, page or show-board
  control moves or is lost. Reversible in one method
  (renderSudokuPalette9): restore the Eraser to slot 51.
- **Quick Math + Word Scramble question cards (FIX-1, both
  platforms):** new Back at slot 18 (bottom-left, free in both card
  variants). It clears typed-answer mode and saves the round exactly
  like Close, then reopens the picker. It deliberately does NOT bank
  a Scramble round — banking stays on Finish now; per-word earnings
  already banked live on the saved round either way.
- Incidental, from the shared renderer: the Bedrock single-box view
  (SUDOKU_BOX, already carrying "All boxes") gains the same slot-53
  Back to the picker via renderSudokuControls. Not an audit row.

Presentation and routing only: game logic, the generators, tiers,
payouts, the $1,500/day cap, the tile packs and the save format are
unchanged — a 0.3.4 save is byte-for-byte a 0.3.3 save (no new key;
the carried suites assert the 0.3.3 save key sets at their exact
0.3.3 counts).

## G1 base-gate (live premise, same day)

Fresh Homebase pull, Nov 14, 2026, 16:55 CST (probe PASS attempt 1,
DESKTOP-IL05DQR; rclone on the PC from the Shockbyte remote; no
fallback): live `plugins/ExcelBrainGames-0.3.3.jar` = 135,705 B /
SHA-256 `64e93e17f4d3fdd14acd9284b874902001188f9eaedf3687d83ef328589d6de1`
— EXACTLY the audited 0.3.3 jar of record, and the plugins census
shows exactly one active BrainGames jar (older jars are .bak
held-asides). The build therefore bases on the audited tree with no
re-base. Receipts: `~/workspace/back-button-wave1-r354/BB-02/RECEIPTS.md`.

## Baseline verification (0.3.3 tree → this build's starting point)

The build-0.3.3 tree was copied to build-0.3.4/ (src and tests
diff-verified identical after the copy). The copied tree's jar is the
0.3.3 jar of record (135,705 B / 64e93e17…, re-hashed this run), and
the G1 pull above proves the same bytes are the live jar.

## Source delta vs the 0.3.3 tree

- `gui/Views.java` — picker slot-26 Back-to-Server-Menu; a shared
  `boardBackItem()` placed at slot 53 by renderWs, renderMem and
  renderSudokuControls (replacing the Close item + `ctl:close`
  action); renderSudokuPalette9 reworked (slot 51 Back, Eraser on
  the high bank at slot 49); question card slot-18 Back (`qback`);
  javadoc for the pass + the 9x9 slot decision.
- `gui/GuiListener.java` — the `back` dispatch gains an explicit
  save on the picker branch and a `servermenu` branch (close +
  performCommand("menu")); new `qback` branch (clearInput + save +
  picker). The `ctl:close` handler remains, now unemitted.
- `ExcelBrainGames.java` — javadoc paragraph for 0.3.4.
- `core/BuildInfo.java` — the 0.3.4 build id (fingerprint carrier).
- `plugin.yml` — version 0.3.4 + one appended description sentence.
- Tests: NEW `BackNavTest.java` (26 checks). No carried test file
  modified (the one test fix during this build touched only the new
  file — see TEST-REPORT.md). `config.yml`, the word lists, the menu
  entry and the tile packs are byte-identical to 0.3.3's.
- `build.sh` — 0.3.4 header + descriptor gate, the new suite, jar name.

## Build inputs (all pinned in build.sh, unchanged from 0.3.3)

- Paper API 26.3 build 231 — SHA-256 f0df9e7f…
- Vault 2.0.0 — SHA-256 8b2ce431…
- Floodgate API jar (compile-only) — SHA-256 21570aff…
- JDK: ~/workspace/.jdk25 (javac --release 21).

## Tests

1,924,741 checks, 0 failures — see TEST-REPORT.md. The carried suite
passes at EXACTLY its 0.3.3 counts (1,924,715): the
behavior-unchanged proof. New: BackNavTest (26) — the navigation
contract pinned in the renderers + listener.

## Determinism

build.sh ran to completion twice end-to-end (plus its internal
double assembly and double pack generation on every run): both runs
produced the identical jar (SHA-256 above). Packs on every run:
java 05942eed… / bedrock 486bcad6… — 0.3.3's recorded hashes,
unchanged.

## Rig

Two boots on Paper 26.3-235 from scratch copies of the 0.3.3 rig
servers (logs in `~/workspace/back-button-wave1-r354/BB-02/rig/`):
without Floodgate — Done (56.647s), SEVERE 0, enable line
`platform-detection=unavailable (Java presentation for all)`; with
the live Geyser + Floodgate — Done (33.976s), SEVERE 0, bind line
`floodgate detected`, enable line `platform-detection=floodgate`.
Both end with the build id above.

## Not proven here (named, not hidden)

Real-client tap walks (Java + Bedrock) of the fixed screens —
including a mid-round Back on each board — are the install packet's
player proof (Mike's / Kailey's spot taps, the backlog acceptance for
the whole batch). The desk evidence for the walk is the BackNavTest
contract (slot, action, handler, landing, save semantics per screen)
plus the two rig boots; no Minecraft client exists in this lane.
