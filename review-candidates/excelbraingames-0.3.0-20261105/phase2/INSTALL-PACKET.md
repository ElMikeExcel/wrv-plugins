# INSTALL PACKET — ExcelBrainGames 0.2.0 (Phase 2: Sudoku)

**Status: PENDING — gated jar swap (jar + config change). NOT a text
entry.** Installs only through the standing gates: server verified
empty (`list` = 0 twice), WINDOW-IN-PROGRESS marker discipline,
BACKUP-FIRST with the cap-at-6 prune rule. Do NOT install from this
packet outside a window.

## What it is

- **ExcelBrainGames 0.2.0** — adds **Sudoku** to the LIVE 0.1.0 Brain
  Games framework: unique-solution generated puzzles (no library), five
  tiers (Kid 4×4 $25 picture-or-number mode, 6×6 $60, 9×9 Easy $120,
  Medium $180, Hard $250), palette-first placement, eraser, Check
  button (adult tiers 10% of payout per use, max 3; kid tier free +
  unlimited), +20% par-time bonus on Medium/Hard, 9×9 two-view paging
  with a banked palette (documented deviation, SPEC §4.6).
- Same shared ledger: $1,500/day cap, streak bonus, kid-tier half rule
  (adults earn $12.50 on Kid 4×4) — Phase 1 code paths, not forks.
- Evidence: `~/workspace/brain-games/build-phase2/TEST-REPORT.md` —
  538,880 checks, 0 failures (incl. 500 seeds × 5 tiers proven unique
  by an independent solver); live rig pass on Paper 26.3 with real
  Vault/Essentials payouts (kid-tier half through the real GUI, a
  Medium completion with one Check deduction + par bonus, a 6×6 round
  resumed across a restart). SEVERE 0.

## Gates (verify before swapping)

| Artifact | Bytes | SHA-256 |
|---|---|---|
| `ExcelBrainGames-0.2.0.jar` (this folder) | 84,155 | `36e9519345088c3dd34f75e68486a2b466cf90375debd671c37cf995a3d1b6b6` |
| Live jar being replaced (`ExcelBrainGames-0.1.0.jar`) | 69,650 | `b23f916f5c1d3a2b8764ffc93d6351b780ba4ae6d37afa773a4249da385cf04` |

- BACKUP-FIRST: fresh Shockbyte backup before the swap; prune oldest
  manual backups first if needed to keep the post-creation total ≤ 6
  (never the Daily Backup's newest copy, never the two newest manual
  restore points); name any prunees in the install report.
- Server verified empty (`list` = 0 twice) before Shut Down.
- No other panel actor: check for an in-flight failsafe run before
  opening the window; create the WINDOW-IN-PROGRESS marker at start,
  delete at finish.

## Swap steps

1. Upload `ExcelBrainGames-0.2.0.jar` to `plugins/`; delete
   `plugins/ExcelBrainGames-0.1.0.jar`.
2. Merge the config: replace `plugins/ExcelBrainGames/config.yml` with
   this build's `config.yml` **after diffing against live** — the delta
   is exactly: the new `games.sudoku` section (tiers, check cost,
   picture symbols) and one message line (`picker-sudoku`). Live kids
   list and every other setting must carry over unchanged.
3. Wordlists: unchanged (no new files).
4. Menu entry: `menu-entry/braingames.yml` lore line changes to
   "Word Search, Memory Match, Sudoku" — replace the file in
   `plugins/ExcelMenu/entries/` and `/excelmenu reload`. **Registry
   count unchanged (85 entries)** — this is a lore refresh on the
   existing door, not a new entry.
5. Start the server; run the boot verification below.

## Boot verification (all must hold)

- Boot `Done` line present; plugin count unchanged (78).
- ExcelBrainGames enable line reads:
  `ExcelBrainGames enabled: games=[word-search, memory-match, sudoku], kid-tiers=ENABLED, daily-cap=$1,500.00, wordlists=9 files / 6 themes, economy=EssentialsX Economy`
- `excelmenu reload` reports the 85-entry registry loading clean.
- Guild line still `LOCKED 3 of 5`; no new SEVERE attributable to the
  swap (SEVERE 0).
- Console smoke: `games admin status <any known player>` runs and
  prints the Sudoku line; `games admin reload` reports 5 Sudoku tiers.

## Same-pass surfaces (owed with this install, per the standing rule)

- Guide knowledge + intents: add the Sudoku chunk set (games list,
  tiers, check cost, picture mode, 9×9 paging + bank switch) to
  ai-knowledge.json + index.html (website canon first), regenerate the
  Guide knowledge from it, live-edit reload.
- What's New: website section + in-game ExcelWhatsNew changelog entry,
  self-contained how-to (how Sudoku pays, what Check costs, how the
  9×9 views/bank work).
- Broadcaster tips: check rotation for a Sudoku tip (27 tips at packet
  time).
- Records: package + Drive deposit + draft PR in wrv-plugins for
  0.2.0, per the standing records workflow.
- Route Sheet WRV-196 + daily log updated at install.

## Rollback

Restore `ExcelBrainGames-0.1.0.jar` and the pre-swap config.yml from
the window backup. Player data is forward-compatible-safe: 0.1.0
ignores the `sudoku:` section in data files (unknown keys are not read;
an in-progress Sudoku round simply waits for 0.2.0 to return). No data
migration exists in either direction.

## Player proofs owed after install

- **Mike (Bedrock phone):** one Kid 4×4 in picture mode; one 9×9 —
  specifically the **paging + bank switch** feel (the documented
  deviation surface); one Check use on an adult tier.
- **Jester (Java):** one 9×9 Medium or Hard completion; Check wrong-
  cell flash reads clearly.
- **Rylie:** Kid 4×4 picture mode round (kid-flagged full $25).
- Guide /ask retest: "how do I play sudoku", "what does the check
  button cost", "how does the 9x9 board fit" answered from the new
  knowledge.
