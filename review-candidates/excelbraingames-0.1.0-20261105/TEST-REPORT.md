# ExcelBrainGames 0.1.0 — Phase 1 TEST REPORT

**Built Nov 4, 2026 (America/Chicago). Status: BUILT + TESTED + RIG-PROVEN, NOT INSTALLED.**
Install is gated (see INSTALL-PACKET.md): Mike's Bedrock phone test + the kid-list
hand check come first. Nothing was filed in the install MANIFEST.

## Candidate

- File: `ExcelBrainGames-0.1.0.jar` — **69,087 B**, SHA-256 **`19de1e8dc32db57b9eb08b6e77d06c386c28610fa81acaecef6ba28cac046d69`**
- Reproducibility: `build.sh` assembles the jar twice per run and hash-asserts;
  a second full `build.sh` run reproduced the identical SHA-256
  (`ExcelBrainGames-0.1.0-repro.jar` alongside is that second build's output).
- Toolchain: JDK 25 (`~/workspace/.jdk25`), `javac --release 21`, Paper API
  26.3 build 231 (`paper-api-26.3.build.231-stable.jar`, SHA-256 `f0df9e7f…`),
  Vault 2.0.0 compile input (SHA-256 `8b2ce431…`, compile-only, never shipped).
  Live server is Paper 26.3-235; plugin.yml api-version 1.21, `depend: [Vault]`.
- Contents: 31 classes + plugin.yml + config.yml + 9 bundled word-list files
  (6 live themes + 3 draft kid lists under `wordlists/draft/`, never loaded).

## Scope built (SPEC §11 Phase 1)

Framework: `/games` (+ `/brain`, `/puzzles`), game picker GUI, per-player
sessions with save/resume (auto-save on every move; survives GUI close,
logout, restart), the shared **$1,500/day cap + streak ledger** (America/Chicago
day boundary), Vault payouts, console admin (`status / reset / resetday /
setkid / reload`, plus `riddle`/`setriddle` answering with the Phase 3 notice),
and the ExcelMenu registry entry (`menu-entry/braingames.yml`, placed at
install). Games: **Word Search** and **Memory Match**, full tier sets.
Phase 2/3 seams: Sudoku + text games show as "coming soon" in the picker;
`games/QuestionLoop.java` ships as the question-loop stub (interface only).
**Kid tiers ship DISABLED** (`games.kid-tiers-enabled: false`) behind the
kid-list hand-check gate; Rylie (`.RylieDSunflower`,
`00000000-0000-0000-0009-01f05d0d739d`) is kid-flagged in the shipped config.

**Q6 amendment (Mike, Nov 4, 2026):** adults on kid tiers earn **half** the
kid payout, rounded **down to the cent** — not the spec's original $0
practice mode. Implemented in `core/Payouts.adjust` + ledger flow; proven in
unit tests and end-to-end in the rig (below).

## Unit / logic tests — 23,467 checks, 0 failures

Run by `build.sh` on every build (plain main-method harnesses, fleet style):

- **WordSearchTest — 20,373 checks.** The two-tap checker accepts a placed
  word in **all 8 directions**, tapped forward AND in reverse tap order;
  tier direction legality (Kid E/S only, Standard 4-way, Challenge 8-way)
  over 200 seeds/tier; invalid selections (straight non-word → NO_MATCH,
  knight move → NOT_A_LINE); tap-same-cell cancel; re-tapping a found word →
  ALREADY_FOUND with no state change; GUI control cells are void and never
  crossed by a placement (200 seeds); save/resume serialization round-trip.
- **MemoryTest — 3,039 checks.** Deck integrity (each symbol exactly twice)
  for 6/8/12/18 pairs × 250 seeds; flip/match/ignore rules; miss flow (both
  cards stay up, flips blocked, resolveMiss flips them back); perfect game =
  exactly 2 flips per pair; serialization round-trip.
- **LedgerTest — 55 checks.** Cap: exact-landing, partial payment at the
  boundary ($1,495 earned + $90 round → pays exactly $5.00, flagged capped,
  total never exceeds cap); streak steps $10→$20→$30→$40→$50→$50 across six
  consecutive days; gap resets streak to 1; no bonus on a day's second
  completion; **kid-half: $40 → $20.00 for adults, full $40 for kid-flagged,
  odd cents round DOWN (2501¢ → 1250¢), and the streak bonus is not halved**;
  speed/par +20% math; resetday; 35-day prune; money formatting.

## Generator property tests — 502,678 checks, 0 failures (PropertyTest)

Run against the **shipped** config + word lists. **500 seeded boards per
tier per theme** (Standard ×6 themes, Challenge ×6, Kid ×3 draft themes):

- every board places the tier's **full word count**;
- every placed word reads back off the grid at its stored placement and is
  FOUND by tapping its endpoints through the real checker — a full solve
  completes every board;
- every placement is in-bounds, direction-legal for the tier, and never
  crosses a void cell;
- **accidental-word probes:** wherever a listed pool word happened to be
  formed by filler at a non-placement position, tapping it was rejected
  (NO_MATCH) — the checker honors stored placements only, per SPEC §3.1.
  Probes executed: Standard 80 boards, Challenge 4, Kid 239 (a probe fires
  only when an accidental word actually occurs on that board).

**Generator design (why it works):** attachment construction (each word
anchors onto a placed word's letter, crossword-style) + max-overlap scan
fallback with a must-cross rule + a per-deal **letter budget** (usable
cells × 1.2) + completion and swap attacks for the last word. Measured
generation latency over the 500-seed Challenge probe: **avg 7–19 ms per
board per theme, worst 146 ms**.

**DEVIATION from SPEC §3.2 (flagged): Challenge = 10 words, not 12.**
Twelve words of 5–10 letters total ~65–72 letters; on the 9×6 board minus
the 3 control cells (48 usable) that needs ~20 shared letters — saturation
testing across every strategy above topped out at 8–11 words (12 succeeded
on ~2% of deals even with all-short words). Ten words places at full count
on 500/500 boards for all six shipped themes. Payout unchanged ($150).
Config carries a comment recording this; SPEC §3.2 carries the deviation
note. Also content-side: the shipped lists were deepened with short words
(ocean +5, school +5, space +3, food +6) after the first property runs
showed thin pools failing the count.

## Rig — Paper 26.3, two boots, 24/24 verdicts PASS, SEVERE 0

Scratch server cloned from the fleet rig (Paper 26.3), plugins:
ExcelBrainGames 0.1.0 (the candidate jar, hash-verified), Vault 2.0.0,
EssentialsX 2.22.0 (economy provider), BrainRig driver. A fake player
(NMS ServerPlayer on an embedded channel — the fleet rig pattern) played
**real rounds through the plugin's GUI**: every tap is a genuine
InventoryClickEvent fired into the plugin manager; payouts verified against
the real Essentials balance. Transcripts: `rig/evidence/boot-a.log`,
`rig/evidence/boot-b.log`. Driver source: `rig/driver/`.

Rig-only config differences (the exact post-gate state the phone test will
use): `games.kid-tiers-enabled: true` and the draft kid-animals list
pre-placed into `wordlists/`. Everything else is the shipped config.

Boot A verdicts (17): plugin + economy hookup; picker opens; **kid-tier
Word Search as a non-kid player paid $30.00 = $20.00 half payout + $10.00
day-1 streak bonus (Q6 amendment end-to-end)**; round cleared on completion;
Standard Word Search paid **$108.00** ($90 + 20% speed bonus); **pays-once**
— re-settling the same completed round object is refused and the balance
does not move; Memory Match deliberate miss (both cards show, flips block,
pair flips back), solve in 18 flips paid **$66.00** ($55 + 20% par bonus);
Challenge round seeded with exactly 1 of 10 words found.
Console admin from the same boot (orchestrator-sent, replies in the log):
bare `games` prints usage (never a bare refusal); `status` shows **Today:
$204.00 of $1,500.00 cap** (= 30 + 108 + 66); `setkid true/false` flips the
flag in config and status reflects it; `resetday` zeroes the day (status
then $0.00); `reset … memory` discards; `riddle` answers with the Phase 3
notice; `reload` reports 7 word-list files / 3 + 4 tiers.

Boot B verdicts (7): after a full server restart the seeded Challenge round
**resumed intact** (found = 1/10, same board), the resume prompt opened from
the picker, Resume returned to the board, and solving the remaining words
paid **$180.00** ($150 + 20% speed bonus; elapsed 74 s including the
restart) against the completion day's ledger, exactly as specced.
Plugin enable line in both boots, no WARN/ERROR attributable to
ExcelBrainGames; **SEVERE 0 in both boots**.

Rig simplifications (stated, per brief): the fake player does not fire a
manual PlayerJoinEvent (the driver's name-index entry is created by its
/games use instead — admin name resolution also falls back to Bukkit's
offline-player cache); the driver aims taps using round state read through
the plugin's public API (it knows where the words are — tap *accuracy* is
not a rig question, it's the phone test's).

## What remains for Mike's phone test (the install gate)

1. **Bedrock phone, Word Search**: two-tap selection lands on the intended
   letters (no off-by-one), found highlighting reads clearly at phone size,
   the Words list view and controls (slots 51–53) are comfortable.
2. **Bedrock phone, Memory Match**: card flips feel right, the ~1 s miss
   pause reads well, symbols are distinguishable at phone size.
3. A Java client pass of the same two games (rendering parity spot-check).
4. The compass **Brain Games door** in the real ExcelMenu (the entry file
   ships with this build; the rig has no ExcelMenu, so the door itself is
   proven at install, not here).
5. Kid tiers on Rylie's account — only after the hand-check gate: she is
   the audience the picture of "kid-readable" has to satisfy.

## Files

Jar + repro jar, full source (`src/`), tests (`tests/`), `build.sh`,
`plugin.yml`, `config.yml`, `wordlists/` (incl. `draft/` kid lists),
`menu-entry/braingames.yml`, rig driver + evidence, `INSTALL-PACKET.md`,
`FILE-LIST.txt` + `SHA256SUMS` (verified).

---

## ADDENDUM — Ship-prep, Nov 4, 2026 (late evening, America/Chicago)

Mike approved the draft kid lists as-is ("that list is fine"), directed
more Minecraft words for the kid AND adult lists, and said "yes ship it."
Changes after the report above:

**Word lists.**
- `kid-minecraft.yml`: 14 → 28 words (added LAVA, SAND, DIRT, GOLD, IRON,
  WOOL, CLAY, SNOW, CAVE, WOLF, TORCH, WHEAT, STONE, APPLE — all 3–5
  letters, the kid tier's length band).
- `minecraft.yml` (adult): 24 → 72 words (standard 12 → 40, challenge
  12 → 32), lengths obeying the tier rules (Standard 4–8, Challenge
  5–10; the Challenge pool is challenge + standard combined).
- All three kid lists promoted `wordlists/draft/` → `wordlists/`; file
  headers record Mike's approval (Nov 4, 2026). Shipped config now
  carries `games.kid-tiers-enabled: true` — **kid tiers are live at
  launch**; Rylie remains kid-flagged. `ExcelBrainGames.BUNDLED_WORDLISTS`
  updated to the promoted paths (class javadoc updated to match).

**Generator bug found + fixed (latent; exposed by the expansion).**
`dealSubsetBudget` and `dealSubset` sorted candidates with a comparator
that re-rolled `rng.nextInt(3)` on every comparison — a sort-contract
violation that stays silent on small lists but throws
`IllegalArgumentException` ("Comparison method violates its general
contract!") from TimSort once a candidate list reaches ~32. The largest
pre-expansion pool had 24 candidates; the expanded Challenge minecraft
pool has 69 length-eligible candidates, and the property suite caught it
on the first re-run. Fix: jitter keys are drawn once per word into a map
before sorting — same shortest-first-with-jitter distribution, consistent
comparator. No other generator logic touched.

**Re-test on the final content.** Full `build.sh` suite re-run:
WordSearchTest 20,416; MemoryTest 3,039; LedgerTest 55; PropertyTest
499,811 — **total 523,321 checks, 0 failures**. The property suite now
loads the kid pools from `wordlists/` (the promoted location) and covers
the expanded minecraft theme at all three tiers, 500 seeds each, full
deal count on every board (Standard 8, Challenge 10, Kid 5).
Accidental-word probes executed: standard 119 boards, challenge 1,
kid 232. (Check totals differ slightly from the original run's 526,145 —
counts vary with pool contents; the 500-seed × 15 tier/theme matrix and
the 0-failure result are the constants.)

**Final candidate (supersedes the header figures above).**
`ExcelBrainGames-0.1.0.jar` — **69,650 B**, SHA-256
**`b23f916fe9f15f58f28f991ada9b13f5a758b609f212eb623c9b87f2ab5e86ef`**.
build.sh hash-asserts two assemblies per run; a second full run
reproduced the identical hash (`ExcelBrainGames-0.1.0-repro.jar`
alongside). The jar changed because config.yml + word lists are bundled
resources, plus the comparator fix.

**Smoke boot of the final jar — Paper 26.3-235 (the live build).**
Fresh data folder, Vault present, no economy provider: boot reached
Done; enable line verbatim — `ExcelBrainGames 0.1.0 enabled:
games=[word-search, memory-match], kid-tiers=ENABLED, daily-cap=$1,500.00,
wordlists=9 files / 6 themes, economy=NONE (ledger-only payouts!)` —
NONE only because the smoke box runs no economy provider (the
EssentialsX hookup was proven in the original two-boot rig and is
present on live). Extraction produced exactly the 9 word lists with NO
`draft/` folder, and the extracted config carries
`kid-tiers-enabled: true`. Console smoke: `games` printed the usage
block; `games admin status RigPlayer` answered in words. SEVERE 0
attributable. Log: `rig/evidence/smoke-20261104.log`. The full gameplay
rig was not re-run — its scratch server had been cleaned from /tmp, and
no game logic changed after it; the deltas are content (property-tested
in place), config, extraction paths (smoke-proven here), and the
comparator fix (covered by the suites).
