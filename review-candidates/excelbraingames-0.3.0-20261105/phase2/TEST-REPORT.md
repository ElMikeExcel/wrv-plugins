# ExcelBrainGames 0.2.0 — TEST REPORT (Phase 2: Sudoku)

Built Nov 5, 2026 on the live Phase 1 framework (0.1.0). Build folder:
`~/workspace/brain-games/build-phase2/`. Standard: same as Phase 1 —
unit + property suites in the build gate, a live rig pass on Paper 26.3
with the real Vault/EssentialsX economy, deterministic double assembly.

## Suites (build gate, all in build.sh)

| Suite | Checks | Failures |
|---|---|---|
| WordSearchTest (Phase 1, regression) | 20,416 | 0 |
| MemoryTest (Phase 1, regression) | 3,039 | 0 |
| SudokuTest (new) | 553 | 0 |
| SudokuPropertyTest (new) | 15,006 | 0 |
| LedgerTest (Phase 1, regression) | 55 | 0 |
| PropertyTest (Phase 1, regression) | 499,811 | 0 |
| **Total** | **538,880** | **0** |

Deterministic build: two assemblies of `ExcelBrainGames-0.2.0.jar`
byte-identical — 84,155 bytes, SHA-256
`36e9519345088c3dd34f75e68486a2b466cf90375debd671c37cf995a3d1b6b6`.
SHA256SUMS + FILE-LIST regenerated and verified (`sha256sum -c` clean).

## SudokuTest (unit/logic)

- Generated shapes at all four sizes: solution is a valid complete grid
  (independent row/col/box permutation check), givens inside the tier
  band, givens match the solution, the starting board is exactly the
  givens, and the production counter proves uniqueness.
- Placement rules: given cells locked (place + erase both refuse),
  player cells place/overwrite/erase, out-of-range rejected.
- Check button: a planted wrong value is the only cell flagged; clean
  board flags nothing; adult cap refuses the 4th use at max 3; the kid
  tier's uses are unlimited (cost waived in settlement).
- Deduction math: 10% of payout per use, floored at zero — $250 with 2
  checks pays $200; $180 with 3 checks pays $126; the Medium rig case
  ($180 − 10%, then +20% par bonus) = $194.40.
- Kid-tier rule on Sudoku values through the shared Phase 1 path:
  adult on Kid 4×4 earns $12.50 (half of $25), kid-flagged earns $25,
  6×6 is not a kid tier.
- Completion: a full-but-wrong board does NOT complete; fixing every
  cell to the solution does.
- Solver sanity: the production solver re-solves a generated puzzle to
  exactly its stored solution; an empty 4×4 counts ≥2 solutions; a
  contradictory grid counts 0.
- Serialization: a mid-round Medium (placements incl. a deliberate
  error, 1 check used) survives the DataStore map round-trip exactly —
  board, given mask, solution, checks, flags; picture mode survives.

## SudokuPropertyTest (500 seeds × all 5 shipped tiers)

Tier bands are read out of the shipped config.yml, not restated in the
test. For every one of the 2,500 deals:

- givens land inside the config band;
- an **independent solver** (set-based candidates, most-constrained-cell
  recursion — shares no code with the production bitmask solver) proves
  the puzzle has **exactly one** solution, and it is the stored one;
- solving by placing the solution completes the round; spoiling one
  cell blocks completion.

Slowest single generation across all 2,500 deals: **44 ms** (kid4 and
hard); 6×6 slowest 2 ms. Generation can never stall a tap.

## Rig pass (live Paper 26.3, Vault + EssentialsX 2.22.0)

Rig folder: `build-phase2/rig/` (driver `BrainRig.java`, runner
`drive.py`, transcripts `rig/evidence/boot-a.log` + `boot-b.log`).
A fake NMS player plays through the plugin's real GUI with genuine
InventoryClickEvents; balances are read from the real Essentials
economy. NOTE: the scratch server runs the Paper 26.3 server jar
extracted from build 214's paperclip (the sandbox cannot download the
Mojang jar — the extraction was seeded from
`~/workspace/paper-26.3-migration/rig/`); the plugin itself is compiled
against the same Paper API 26.3 build 231 as Phase 1 (hash-pinned in
build.sh). Live server is 26.3-235; no API surface Sudoku touches
differs between those builds.

Verdicts (Nov 5, 2026): **ALL PASS, SEVERE 0 on both boots.**

Boot A — picker opens on Sudoku as a live door; Kid 4×4 starts
(givens in band) and a non-kid player solving it through real taps is
paid **$22.50 = $12.50 adult half + $10 day-1 streak**; 9×9 Medium
starts (givens in the 30–35 band), one correct placement + one Check
(checksUsed = 1, clean board) then a full solve through the paged
views and banked palette pays **$194.40 = ($180 − $18 check) × 1.2 par
bonus**; a 6×6 is seeded with 3 placements (filled = givens + 3).
Console admin exercised: status shows all rounds, `reset … sudoku`
surface present via reload report (5 Sudoku tiers).

Boot B — the seeded 6×6 resumes from disk intact (filled = givens + 3,
same tier), the resume prompt opens the board, and completing it pays
the full **$60.00** against the new boot — exactly the save/resume
contract of SPEC §9.5.

## Documented deviations (Phase 1 style)

1. **9×9 banked palette (SPEC §4.6, recorded in the spec itself).** The
   full tappable set — 45 visible cells + 9 digits + eraser + check +
   page toggle = 57 — cannot fit 54 slots alongside the standard New /
   Close buttons. As built: the palette row (slots 45–53) carries five
   digits of the current bank (1–5 / 6–9), a bank switch, the eraser,
   the check button and the page toggle — palette present in both
   views as specced; Close on 9×9 is the native GUI close (the round
   auto-saves; net effect identical); New Game is reached via
   picker → Sudoku → resume prompt. 4×4 and 6×6 keep the full standard
   control row. Mike's Bedrock phone test of 9×9 paging + the bank
   switch is an owed player proof in the install packet.
2. **Givens bands, not single targets (SPEC §4.6).** The 9×9 tiers ship
   as bands bracketing the spec's "~40 / ~32 / ~26" (38–44 / 30–35 /
   24–29): the digger draws a target inside the band and re-deals (max
   8) rather than stalling a tap on an unlucky dig. Kid 4×4 (8–10) and
   6×6 (18–22) are the spec's exact bands.

## Console admin (Phase 2 delta)

No new commands. `admin reset` now accepts `sudoku`; `admin status`
shows the Sudoku round (tier, mode, filled count, checks used);
`admin reload` reports the Sudoku tier count. `admin resetday`,
`setkid`, and the shared ledger already covered Sudoku state through
the Phase 1 paths — the reset/status gaps are the ones that existed,
and both are closed here rather than silently.
