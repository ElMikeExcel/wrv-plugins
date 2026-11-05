# INSTALL PACKET — ExcelBrainGames 0.3.0 (Phase 3: the text trio)

**Status: PENDING — gated jar swap (jar + config + one new wordlist
file). NOT a text entry.** Installs only through the standing gates:
server verified empty (`list` = 0 twice), WINDOW-IN-PROGRESS marker
discipline, BACKUP-FIRST with the cap-at-6 prune rule. Do NOT install
from this packet outside a window.

**Sequencing:** 0.3.0 builds on 0.2.0 (Sudoku). If 0.2.0 has not
installed yet when a window opens, install 0.3.0 INSTEAD of 0.2.0 —
it is cumulative (every Phase 1 + 2 + 3 surface in one jar) and the
0.2.0 packet's swap steps apply with this packet's hashes. Never
install 0.2.0 after 0.3.0.

## What it is

- **ExcelBrainGames 0.3.0** — completes the six-game Brain Games set:
  **Quick Math** (10 questions; Kid $25 tap-choice, Standard $50,
  Challenge $90 typed; payout = tier × right/10 floored to dollars),
  **Word Scramble** (5 words from the shipped lists; Kid $50 3-choice
  tap with retries, Standard $100 typed at $20/word, Hint reveals the
  first letter and halves the word, finish-now banks partial credit),
  **Riddle of the Day** ($100 flat, one riddle per server day for
  everyone, 3 attempts, hint −$25, fuzzy answers from the config
  list of 16 family-safe riddles).
- All three run on ONE shared question-loop module; typed answers are
  captured from chat (never broadcast) on both platforms — documented
  deviation from the spec's Bedrock-form wording, SPEC §8.3.
- Same shared ledger throughout: $1,500/day cap, streak bonus,
  kid-tier half rule on both kid text tiers — Phase 1 code paths.
- The picker has no dead buttons after this install: all six games
  are live doors from the compass Brain Games button.
- Evidence: `~/workspace/brain-games/build-phase3/TEST-REPORT.md` —
  622,227 checks, 0 failures (incl. 500 seeds × every text tier with
  prompt re-parse evaluation, and 500 seeds × 5 Sudoku tiers carried
  from Phase 2); live rig pass on Paper 26.3 with real
  Vault/Essentials payouts (details in the report). SEVERE 0.

## Gates (verify before swapping)

| Artifact | Bytes | SHA-256 |
|---|---|---|
| `ExcelBrainGames-0.3.0.jar` (this folder) | 118,307 | `acfba81f800cf67e9bdf56e9eae016ec2d6ce8c42fce7c8558e9f3e694345b63` |
| Live jar being replaced (`ExcelBrainGames-0.1.0.jar` or the 0.2.0 jar under its live name) | 69,650 / 84,155 | `b23f916f…` (0.1.0) / `36e95193…` (0.2.0) |

- BACKUP-FIRST: fresh Shockbyte backup before the swap; prune oldest
  manual backups first if needed to keep the post-creation total ≤ 6
  (never the Daily Backup's newest copy, never the two newest manual
  restore points); name any prunees in the install report.
- Server verified empty (`list` = 0 twice) before Shut Down.
- No other panel actor: check for an in-flight failsafe run before
  opening the window; create the WINDOW-IN-PROGRESS marker at start,
  delete at finish.

## Swap steps

1. Upload `ExcelBrainGames-0.3.0.jar` to `plugins/`; delete the old
   BrainGames jar (0.1.0 or 0.2.0 name).
2. Merge the config: replace `plugins/ExcelBrainGames/config.yml`
   with this build's `config.yml` **after diffing against live** —
   the delta from 0.2.0 is exactly: the new `games.quickmath`,
   `games.scramble`, and `games.riddle` sections plus three message
   lines (`picker-quickmath`, `picker-scramble`, `picker-riddle`).
   (From 0.1.0, add the Phase 2 `games.sudoku` section +
   `picker-sudoku` line as well.) Live kids list and every other
   setting must carry over unchanged.
3. Wordlists: ADD `wordlists/riddles.yml` (16 riddles) to
   `plugins/ExcelBrainGames/wordlists/`; all other list files are
   unchanged. (The jar also bundles it and self-extracts on first
   boot if the folder copy is missed — the folder copy is the
   config-first source of truth, so place it explicitly.)
4. Menu entry: `menu-entry/braingames.yml` lore now names all six
   games — replace the file in `plugins/ExcelMenu/entries/` and
   `/excelmenu reload`. **Registry count unchanged (85 entries).**
5. Start the server; run the boot verification below.

## Boot verification (all must hold)

- Boot `Done` line present; plugin count unchanged (78).
- ExcelBrainGames enable line reads:
  `ExcelBrainGames enabled: games=[word-search, memory-match, sudoku, quick-math, word-scramble, riddle], kid-tiers=ENABLED, daily-cap=$1,500.00, wordlists=10 files / 6 themes, riddles=16, economy=EssentialsX Economy`
- `excelmenu reload` reports the 85-entry registry loading clean.
- Guild line still `LOCKED 3 of 5`; no new SEVERE attributable to the
  swap (SEVERE 0).
- Console smoke: `games admin riddle` prints today's riddle WITH its
  answer (console-only); `games admin setriddle 0` then
  `games admin riddle` shows the forced riddle; `games admin reload`
  reports 3 Quick Math tiers, 2 Scramble tiers, 16 riddles.

## Player-data note

New data sections (`quickmath`, `scramble`, `riddle`) and one new
global file (`plugins/ExcelBrainGames/riddle-state.yml`, created on
first boot) are additive. 0.2.0/0.1.0 ignore the new sections on
rollback; an in-progress text round simply waits for 0.3.0 to
return. No migration in either direction.

## Same-pass surfaces (owed with this install, per the standing rule)

- Guide knowledge + intents: REPLACE the "coming soon" teaching with
  the text trio's real chunks (how each game plays and pays, the
  chat-answer flow, the riddle's once-a-day + hint cost) — website
  canon first (index.html + ai-knowledge.json together), Guide
  knowledge regenerated from it, live-edit reload.
- What's New: website section + in-game ExcelWhatsNew changelog
  entry, self-contained how-tos for all three games.
- Broadcaster tips: check rotation for a text-games tip (27 tips at
  packet time).
- Records: package + Drive deposit + draft PR in wrv-plugins for
  0.3.0, per the standing records workflow.
- Route Sheet WRV-196 + daily log updated at install.

## Rollback

Restore the pre-swap jar (0.2.0 or 0.1.0) and the pre-swap config.yml
from the window backup. `riddles.yml` and `riddle-state.yml` may
stay in place — older jars ignore both.

## Player proofs owed after install

- **Mike (Bedrock phone):** one Quick Math Standard round answered
  in chat (the deviation surface — confirm answers are captured, not
  broadcast, and the flow feels right on the phone); one Riddle
  answer attempt in chat.
- **Jester (Java):** one Scramble Standard round incl. a Hint; one
  Quick Math Challenge round.
- **Rylie:** Quick Math Kid by taps (3-choice) and Scramble Kid by
  taps — full kid payouts on her flagged account.
- Guide /ask retest: "how do I play quick math", "how does the
  riddle of the day work", "what does a scramble hint do" answered
  from the new knowledge.
