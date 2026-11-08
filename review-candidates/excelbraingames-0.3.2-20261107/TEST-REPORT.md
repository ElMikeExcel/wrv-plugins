# ExcelBrainGames 0.3.2 — TEST REPORT

Build: EXC-BRAINGAMES-0.3.2-WRV-20261107-01-b87f7f. Every suite below
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
| **GeometryTest (NEW)** | **9,202** | **0** |
| **BedrockPropertyTest (NEW)** | **1,223,793** | **0** |
| TileTest (carried) | 14,473 | 0 |
| **Total** | **1,869,695** | **0** |

The carried suites pass at EXACTLY their 0.3.1 counts (622,227
without TileTest) — the behavior-unchanged proof for the checker,
payouts, cap, saves, Sudoku, and the text trio. No carried test file
was modified; the no-geometry `generate()` signature they exercise
delegates to the JAVA path.

## Saturation proof (the generator under Mike's ruling)

BedrockPropertyTest, shipped word lists + shipped config shapes,
500 SplitMix-mixed seeds per tier × theme (PropertyTest's convention
and count), every round asserted at FULL tier word count:

| Bedrock tier (shape) | Themes | Full count |
|---|---|---|
| Kid 5x8, 5 words | animals, food, minecraft | 500/500 each |
| Standard 5x10, 8 words | all six | 500/500 each |
| Challenge 5x10, 10 words | all six | 500/500 each |

15/15 combos, 7,500 rounds, zero shortfalls — the narrow board with
the length ruling saturates as reliably as the Java board does under
PropertyTest. (Kid pools exist only for the three kid themes, as in
PropertyTest.) Before the suites were finalized, an independent
probe harness ran the same matrix against the raw generator and
agreed (500/500 everywhere, zero ruling violations).

## GeometryTest — what the new checks pin

- **Java map == legacy map:** for both Java shapes, every cell's
  slotOf equals the 0.3.1 formula (r*9+c), cellAt inverts it at every
  slot, and controlVoids matches the legacy control rule cell for
  cell. Java rendering cannot drift.
- **Bedrock stream map:** slotOf is the identity on both Bedrock
  shapes; tail slots (board end → 53) are never cells; no control
  voids anywhere on a Bedrock board.
- **Length ruling:** over generated Bedrock standard + challenge
  rounds, no placement with dr == 0 is longer than 5; long words DO
  place off-axis (counted, > 0); the Java control run over the same
  seeds still produces long horizontal placements (the ruling is
  geometry-gated, not global). Saturation asserted at the standard
  shape on the synthetic pool (300/300); at the challenge shape the
  synthetic pool (nine of 18 band words are ruling-restricted
  7-letters) deals 8–9 of 10 — the generator's documented shortfall
  shape for letter-heavy undersized pools — so challenge saturation
  is proven where it matters, over the shipped pools
  (BedrockPropertyTest, above), and GeometryTest asserts the
  near-full floor there instead.
- **Cross-geometry resume:** a Bedrock round round-trips through
  toMap/fromMap with geometry, board, placements, and found-set
  intact and SOLVES on the reloaded copy; a Java round round-trips
  as JAVA; a pre-0.3.2 save map (geometry key removed) loads as JAVA
  with its board intact; a garbage geometry value loads as JAVA.
- **Unknown-platform defaults:** parse(null / empty / junk) = JAVA,
  case/space-tolerant for real values. The plugin-level half (no
  Floodgate → JAVA for everyone) is rig-proven (below).

## Behavior gate — display traceability (the property Mike hit)

GeometryTest renders rounds into a simulated display (the
inventory's slot sequence wrapped at a display width) and requires
every placed word's letters to step through consecutive display
positions in the placement's own direction, spelling the word:

- Bedrock rounds at phone width 5 under the stream map: **0 breaks**
  across 360 rounds (all three Bedrock tiers).
- Java rounds at width 9 under the Java map: **0 breaks** (0.3.1
  behavior preserved).
- **Negative control:** the same Java rounds wrapped at phone width
  5 break the property (hundreds of broken placements) — the harness
  reproduces Mike's exact failure class, so the gate is not vacuous.
- BedrockPropertyTest repeats the stream-traceability assertion
  inside all 7,500 shipped-pool rounds, letter by letter, and solves
  every round end-to-end through the real two-tap checker.

## Rig

Two boots on Paper 26.3-235 (logs in `rig/`): without Floodgate —
Done, SEVERE 0, `platform-detection=unavailable`; with the live
Geyser 2.12.1 + Floodgate 2.2.5-b141 — Done, SEVERE 0, bind line
printed, `platform-detection=floodgate`. Details in BUILD-RECORD.md.

## Not proven here (named, not hidden)

Real-client rendering (what a phone draws from the stream layout) is
settled by the install packet's player proof — Mike on Bedrock plays
Word Search — the same residual class as 0.3.1's pack proofs. The
0.3.1 pack-declined question stays open and unchanged.
