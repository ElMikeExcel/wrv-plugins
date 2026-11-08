# ExcelBrainGames 0.3.2 — SPEC DELTA (Word Search Bedrock geometry)

Against: SPEC.md (Brain Games, settled Nov 4, 2026), the live 0.3.1 build,
and the investigation report `~/workspace/brain-games/WORDSEARCH-LAYOUT-INVESTIGATION.md`
(Nov 7, 2026), which is this build's spec.
Commissioned: Mike's report from in-game Bedrock play, Nov 7, 2026, 5:18 PM CST,
and his design ruling the same day: **same word lists and tiers on both
platforms; on Bedrock's 5-wide board, horizontal (E/W) runs cap at 5 letters;
longer words place vertical or diagonal. Java unchanged.**

## 1. The defect (player-proof FAILURE against 0.3.1, Bedrock Word Search)

Evidence: Mike's screenshot, Nov 7, 2026 (in `~/workspace/user/files/`):
the letter tiles render (0.3.1 works), but the logical board does not
survive the trip. The 6×6 kid board he was dealt reads as two clean rows
of letters, then fragments — FISH and CAT findable, the other three words
unfindable by eye. The investigation decoded his board exactly (kid tier,
5 words) and convicted the mechanism from the source: there is no Bedrock
renderer; the 54-slot inventory is filled on the 9-wide Java map and
Geyser reflows it ~5-wide on a phone. Slot order survives; 2D adjacency
does not. Words running S or crossing a display-row seam scatter. Taps
still map back to the correct slots — the checker was never broken; the
board a phone player SEES is not the board that was dealt.

Per-game verdicts from the investigation (unchanged by this build except
Word Search): Sudoku affected but already mitigated by the 0.3.1 chat
square — **explicitly out of scope here**; Memory Match, Quick Math,
Word Scramble, Riddle of the Day not affected. The letter-tile packs are
exonerated and are **not touched** (this build's pack hashes reproduce
0.3.1's exactly, asserted in build.sh output and the record).

## 2. The fix (the investigation's Option A — platform-geometry rounds)

One logical round, one checker, one save format — but the board's slot
mapping is per-geometry, and the geometry is a property of the ROUND:

- **New pure type `core/WsGeometry`** — JAVA (guiCols 9) and BEDROCK
  (guiCols 5): `slotOf` / `cellAt` / `controlVoids`, computed through the
  map itself. Under JAVA the map is the legacy rule stated once —
  unit-proven cell-for-cell against the 0.3.1 formula, including the
  control-void mask — so Java rendering is bit-for-bit the old behavior.
- **The round records its geometry.** `WordSearchRound` carries it from
  generation, persists it as a `geometry` key appended LAST in the save
  map (pre-0.3.2 key order untouched), and defaults a missing or garbage
  value to JAVA. `Views.renderWs` renders through the ROUND's map, never
  the viewer's platform: a round started on one platform resumes
  unscrambled on the other, and a 0.3.1 save loads and renders exactly
  as it did under 0.3.1. A 0.3.2 Bedrock save read by a rolled-back
  0.3.1 jar ignores the unknown key (the round then renders on the Java
  map until it is finished or discarded — the rollback note in the
  install packet states this plainly).
- **Bedrock boards are 5-column contiguous streams** (cell at slot
  r*5+c = the cell index itself), so the phone's Geyser wrap IS the
  board. Shapes live in config (`games.wordsearch.bedrock-tiers`):
  Kid 5×8, Standard 5×10, Challenge 5×10 — all ≤ 50 cells, ending before
  the Words / New Game / Close controls at slots 51/52/53, so Bedrock
  boards have no control voids. A shape entry overrides ONLY label +
  cols + rows; word count, length band, directions, payout, par and kid
  flag are inherited from the tier — same lists, same tiers, both
  platforms, enforced structurally in `GameConfig` (there is nothing
  else a shape entry CAN change). The tier picker names the board the
  player will actually be dealt.
- **The length ruling lives in the generator.** Under BEDROCK geometry,
  a word longer than the stream width (5) is placed only in directions
  with a row step (S, N, diagonals) — enforced in one helper
  (`dirsFor`) applied per word in every placement path (deal, completion
  attacks, swap attacks), so no path can sneak a long word sideways.
  Java generation is unfiltered (proven: long horizontal placements
  still occur on Java challenge boards).
- **Platform detection is a Floodgate softdep.** `plugin.yml` gains
  `softdepend: [floodgate]` (Vault stays the only hard depend). One new
  class (`bedrock/BedrockHook`) is the only Floodgate touchpoint, bound
  in `onEnable` only when the floodgate plugin is present AND enabled —
  the fleet pattern copied from ExcelAutoSort. Detection keys off the
  round STARTER at deal time. **Unknown platform → Java geometry** (safe
  default): hook absent, hook failed, or Floodgate doesn't know the
  player all deal the board 0.3.1 dealt everyone. The enable log line
  states which world the server is in (`platform-detection=floodgate`
  or `unavailable (Java geometry for all)`), and each round-start log
  line records its geometry and board shape.

## 3. What did NOT change

- The checker (two-tap, placements-only validation), payouts, the
  $1,500/day cap, streaks, themes, word lists, kid rules — untouched.
  `GameService.completeWordSearch` and everything downstream of a round
  are unmodified; the payout lookup still reads the (Java) tier, whose
  money fields the Bedrock shape inherits identically.
- Save format semantics: same keys, same order, one key appended.
- The tile packs: byte-identical to 0.3.1 (hashes in BUILD-RECORD.md).
  No server.properties change, no Geyser config change, no new pack
  push — Bedrock players keep the pack they already have.
- Sudoku and every other game: no code path touched. Sudoku's geometry
  rework remains an elective, separate build (its chat square stands).
- The compass menu entry (`menu-entry/braingames.yml`): unchanged.

## 4. Proof (see TEST-REPORT.md for the full table)

- The full carried suite passes at exactly its 0.3.1 counts (622,227
  checks) plus TileTest (14,473) — the behavior-unchanged proof for
  everything outside Word Search geometry.
- **GeometryTest** (new): slot-map equivalence (Java == legacy formula,
  cell for cell, voids included); Bedrock stream identity; the length
  ruling under generation incl. the Java control; cross-geometry resume
  (round-trip both geometries, legacy-save default, garbage-value
  default, found-set survival, resumed round solves); unknown-platform
  parse defaults; and the display-traceability harness with its
  negative control.
- **BedrockPropertyTest** (new): the saturation proof at the project's
  standard count — 500 SplitMix-mixed seeds per Bedrock tier × theme
  over the shipped lists and shipped config: full word count placed in
  every round, ruling holds in every placement, every word traceable in
  the 5-wide stream in its placed direction, every round solved
  end-to-end through the real checker.
- **Behavior gate:** GeometryTest's display harness IS the property
  Mike hit, checked directly: a rendered Bedrock round's every word is
  findable in stream order per its direction; the negative control
  (a 0.3.1-style Java round wrapped at phone width) breaks the same
  property in the same harness, so the gate can see the defect class.
- **Rig:** the built jar boots on the exact live Paper build
  (26.3-235) twice — once without Floodgate (bind correctly absent,
  `platform-detection=unavailable`) and once with the live Floodgate
  2.2.5-b141 + Geyser present (bind succeeds,
  `platform-detection=floodgate`). Boot logs in `rig/`.

## 5. Known limits / residuals

- **Real-client rendering still cannot be rig-proven.** The rig proves
  boot, binding, and zero SEVERE; what a phone actually draws is settled
  by the packet's player proof (Mike on Bedrock plays Word Search —
  words findable by eye in the stream). This is the same residual class
  as 0.3.1's pack proofs, and the packet names it.
- A round's geometry is fixed at deal time by design. The same ACCOUNT
  is effectively single-platform on this server (Bedrock accounts have
  their own Floodgate UUIDs), so a player never watches their own round
  change shape; a Java-geometry round opened on a phone still shows the
  0.3.1 wrap (that is the rollback-compatible behavior, and new rounds
  dealt on the phone are stream boards).
- The Bedrock pack-declined rendering question from 0.3.1 §8 is
  unchanged and still open; tiles stamp the same way in both geometries.
