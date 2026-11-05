# ExcelBrainGames — SPEC

**Status:** Design spec, rebuilt Nov 4, 2026. The original Oct 24 plan folder (`~/workspace/batch-next/brain-games-plan/`) no longer exists on disk; this spec recreates it from Route Sheet WRV-196 and adds Word Search. Nothing is built yet. No jar, no install packet — this document is the build contract.

**Provenance / decisions already settled (do not relitigate in build):**
- Mike lifted the hold Nov 4, 2026 ("Fuck it lets do it") and named **Word Search** as his own game — it is a headline game, not an extra.
- Command: `/games` (aliases `/brain`, `/puzzles`). Compass **Brain Games** button via the ExcelMenu 2.0 registry (config-first entry, same pattern as every other menu door).
- **Tap-only input** for the grid games on Bedrock phones. Text games may use a form text field (Bedrock) / chat (Java) — see each game.
- Rewards are **Vault money**. **$1,500/day cap per player across all Brain Games combined.** Each generated puzzle pays **once**. Small **streak bonus**. **Anti-farm:** kid tiers pay in full only to kid-flagged players; everyone else earns half on kid tiers (Mike's decision, Nov 4, 2026 — see §9.3).
- Platform parity: Java + Bedrock get the same games, same payouts, same rules. Presentation adapts per platform (§2), function does not.
- Console-admin rule (fleet standard): every state-changing operation has a console-runnable admin command (§9).
- Third-party alternative already rejected Oct 24: Pocket Games (SpigotMC) — Sudoku broken on modern versions, no 26.x support, Dec 2025 dupe report. We build our own.

---

## 1. The six games

| # | Game | Type | Kid tier | Core machinery |
|---|------|------|----------|----------------|
| 1 | **Word Search** | Letter grid | Yes (6×6, short words) | Grid GUI + two-tap selection |
| 2 | **Sudoku** | Number grid | Yes (4×4 picture mode) | Grid GUI + palette-first placement |
| 3 | **Memory Match** | Card grid | Yes (6 pairs) | Grid GUI + flip state |
| 4 | **Quick Math** | Text questions | Yes (add/subtract) | Form/chat question loop |
| 5 | **Word Scramble** | Text / choice | Yes (3-choice tap) | Form/chat question loop |
| 6 | **Riddle of the Day** | Text riddle | Shared (family-safe list) | Form/chat, daily rotation |

Every game deals a **fresh round every time** — generators, not fixed content. Riddle of the Day is the one exception in cadence only: the *riddle* rotates daily (same riddle for everyone that day), but it is drawn from the list, not hand-posted.

---

## 2. Shared presentation model (how anything renders)

**Grid games (Word Search, Sudoku, Memory Match)** render in an **inventory GUI on both platforms**. Bedrock players see the Geyser-translated chest UI, which is a true 2D grid and tap = click — the same surface WandBars and the ExcelMenu inventories already prove on this server. Native Bedrock forms cannot do a 2D button grid (form buttons are a vertical list), so forms are **not** used for grids.

Hard consequence of the inventory surface: a chest GUI is **9 columns × 6 rows = 54 slots**. All grid sizes in this spec fit inside 54 slots, counting control buttons. Larger grids would need a scrolling viewport — explicitly out of v1 (Open Question 1).

**Menus** (game picker, tier picker, resume prompts) use the fleet pattern: Bedrock gets Floodgate forms, Java gets an inventory menu or chat list, both driven from the same registry/config definitions (ExcelMenu 2.0 pattern — content in config, not code).

**Text games (Quick Math, Word Scramble, Riddle)** use a Bedrock custom form with a text input (phone keyboard) on Bedrock, and chat input on Java. The kid tiers of Word Scramble stay tap-only via 3-choice buttons on both platforms.

**Sounds/feel:** small click on selection, a pleasant chime arpeggio on word/puzzle completion, a low buzz on a wrong selection. No particles — menus only.

---

## 3. Word Search (headline game — Mike's)

### 3.1 Round generation
- Player picks a **theme** (Animals, Food, Minecraft, Space, Ocean, School, Holidays, Sports — lists in config, §8.4) and a **tier**.
- The generator takes the tier's word count from the theme list, then places each word on the grid in a straight line in an allowed direction, retrying on collision conflicts (letters may share a cell only where they match). Placement gives up on a word after N tries and swaps in another word from the list, so a round always starts on time.
- Remaining cells are filled with random letters (uniform A–Z).
- The placed positions are stored with the session — the checker validates against **placements**, so stray accidental words in the filler neither count nor confuse scoring.

### 3.2 Tiers
| Tier | Grid | Words | Directions allowed | Word length | Payout |
|------|------|-------|--------------------|-------------|--------|
| Kid | 6×6 | 5 | Horizontal →, vertical ↓ only | 3–5 letters | $40 |
| Standard | 9×6 | 8 | →, ↓, ←, ↑ | 4–8 letters | $90 |
| Challenge | 9×6 | 12 | All 8 (incl. diagonals + reverse) | 5–10 letters | $150 |

> **Phase 1 build deviation (Nov 4, 2026):** Challenge ships with **10 words**, not 12. Generator saturation testing (attachment construction + crossing scan + letter budget, 500 seeds × 6 themes) showed 12 words of 5–10 letters cannot be placed reliably on a 9×6 board with the 3 GUI control cells voided (48 usable cells); 10 words places at full count on 500/500 boards per theme with the shipped lists. Payout unchanged ($150). Evidence: `build-phase1/TEST-REPORT.md`. Revisit only if the grid grows (viewport, §13).

Kid tier uses only kid-list themes (§8.4) and short words — bigger effective targets, no backwards/diagonal hunting.

### 3.3 Interaction — two-tap "circling"
This is the phone equivalent of circling a word with a pencil:
1. **Tap the first letter** of a word you spotted. The cell highlights (selected state) and the game prompts "now tap the last letter."
2. **Tap the last letter.** The plugin walks the straight line between the two taps (must be one of the tier's allowed directions and a straight row/column/diagonal).
   - **Match:** the word is struck off the word list, its cells turn to the **found highlight** (green-tinted) and stay highlighted, chime plays.
   - **No match:** soft buzz, selection clears, no penalty, no cost.
3. **Tap the first letter again** (or the Cancel control) to drop a pending selection.
4. The word list (words remaining vs found) is visible without leaving the grid: found words move to a "found" display; on the inventory GUI the bottom row holds **Words** (opens the list view), **New Game**, and **Close** controls. The list view shows found words struck through.

### 3.4 Win / timer / payout
- Win = all words found. There is **no lose state and no hard timer**. Elapsed time is tracked; finishing a Standard/Challenge round under the par time (config, default 6 min / 10 min) pays a **+20% speed bonus** (adult tiers only; kid tier has no clock pressure at all).
- Payout per the tier table, paid once, on completion only. Partial progress pays nothing — but the round is resumable (§7), so nothing is lost by closing mid-round.

---

## 4. Sudoku

### 4.1 Round generation
- Generator builds a complete valid grid by randomized backtracking, then removes cells while a solver confirms the puzzle still has a **unique solution** (removal stops at the tier's givens target or when no more cells can be removed without breaking uniqueness). Every round is a new puzzle; there is no puzzle library.
- 4×4 and 6×6 use the same engine with box shapes 2×2 and 2×3 respectively; uniqueness checking applies at all sizes.

### 4.2 Tiers
| Tier | Grid | Givens (approx.) | Payout |
|------|------|------------------|--------|
| Kid 4×4 | 4×4 | 8–10 of 16 | $25 |
| 6×6 | 6×6 | 18–22 of 36 | $60 |
| 9×9 Easy | 9×9 | ~40 of 81 | $120 |
| 9×9 Medium | 9×9 | ~32 of 81 | $180 |
| 9×9 Hard | 9×9 | ~26 of 81 | $250 |

### 4.3 Picture mode (kid 4×4)
The 4×4 tier offers **picture mode**: the digits 1–4 are replaced by four item icons (config list, default: apple, sunflower, heart-of-the-sea style distinct silhouettes — final icons chosen for instant recognition at phone size). Rules are identical (each picture once per row/column/box). Picture mode is the Rylie tier; number mode on 4×4 is also available.

### 4.4 Interaction — palette first (settled design)
1. **Pick a number (or picture) from the palette first**, then **tap cells** to place it — tap as many cells as you like with that number selected; switch numbers by tapping the palette again. This is the settled Oct 24 design and suits phones (no per-cell number-picker popups).
2. **Eraser** in the palette clears a player-placed cell. Given cells are locked (visually distinct, unchangeable, buzz if tapped with intent to edit).
3. **Check button** (adult tiers): highlights any wrong placements in red briefly — costs 10% of the payout per use (config `sudoku.check-cost-percent: 10`, max 3 uses). Kid tier: check is free and unlimited; a kid can't "lose" a Sudoku.
4. **9×9 paging:** 81 cells exceed the 54-slot GUI, so the 9×9 board uses **two views** — View A shows rows 1–5, View B shows rows 5–9 (row 5 shared as the seam), with a toggle control; the palette row is present in both views. The full board state is always one model; paging is display-only. (Open Question 6.)

### 4.5 Win / payout
- The plugin validates continuously against the solution: when the last empty cell is filled **and** the board is correct, the round completes and pays the tier amount (minus any check-button deductions on adult tiers). An incorrect full board does not complete — wrong cells simply aren't accepted as "done"; the player keeps editing. No lose state, no timer; par-time speed bonus (+20%) applies to 9×9 Medium/Hard only.

---

## 5. Memory Match

### 5.1 Generation
A deck of pair-symbols (item icons — distinct, kid-readable) is shuffled and dealt face-down. Fresh shuffle every round.

### 5.2 Tiers
| Tier | Pairs | Cards | Payout |
|------|-------|-------|--------|
| Kid | 6 | 12 | $30 |
| Standard | 8 | 16 | $55 |
| Big | 12 | 24 | $100 |
| Giant | 18 | 36 | $160 |

All fit the 54-slot GUI with room for controls.

### 5.3 Interaction
Tap a face-down card → it flips. Tap a second → if the pair matches, both stay face-up in the matched highlight; if not, both flip back after a short pause (~1 s). Standard memory rules; no other input.

### 5.4 Win / payout
Win = all pairs matched. Tracked stat: total flips. Finishing at or under the tier's par flip count pays **+20%** (all tiers — it's a skill bonus, not time pressure). Pays once on completion.

---

## 6. Quick Math

### 6.1 Generation
A round is **10 questions**, generated per tier:
| Tier | Content | Payout at 10/10 |
|------|---------|-----------------|
| Kid | Addition/subtraction within 20, no negatives | $25 |
| Standard | + multiplication, simple division, within 100 | $50 |
| Hard | Two-step problems, larger numbers | $90 |

### 6.2 Interaction
Question appears (form on Bedrock, chat prompt on Java) with a text answer field (Java: type the number in chat). Kid tier may optionally show 3-choice tap answers — default ON for kid tier (config), keeping Rylie tap-only.
- One attempt per question (a wrong answer shows the correct one and moves on — it's practice-shaped, not punitive).

### 6.3 Payout
Payout = tier amount × (correct ÷ 10), rounded down to whole dollars. Pays once when the round ends. No separate speed bonus (speed is implicit — rounds are short); no streak interaction beyond the shared one (§8.2).

---

## 7. Word Scramble

### 7.1 Generation
A round is **5 words** drawn from the theme list for the tier. Each word is letter-shuffled (guaranteed different from the original spelling; for short kid words, shuffle is checked so the scramble isn't trivially the answer).

### 7.2 Tiers & interaction
| Tier | Words | Input | Payout |
|------|-------|-------|--------|
| Kid | 3–4 letters, kid lists | **3-choice tap** (the word + 2 decoys) | $50/round |
| Standard | 5–8 letters | Type the word (form/chat) | $100/round |

### 7.3 Payout
Partial credit: **$10 per correct word (Kid), $20 per correct word (Standard)** — so a finished round of 5 pays the table amount, and an abandoned-after-3 round still banks what was earned when the player chooses "finish now." One attempt per word in Standard (a **Hint** control reveals the first letter and halves that word's value); Kid tier allows retries until correct (no penalty — it's for a 6-year-old).

---

## 8. Riddle of the Day

### 8.1 Rotation
One riddle per server day (America/Chicago), drawn without repeat from the riddle list (config) until the list cycles. **Same riddle for everyone that day.** Family-safe list; kid-suitable riddles flagged so the daily pick can prefer them on a config schedule (e.g., weekends).

### 8.2 Interaction & payout
- `/games` → Riddle shows today's riddle (form/chat). Player submits an answer (form text field / chat).
- **3 attempts** per player per day. A **Hint** control gives the stored hint and reduces the payout.
- Payout: **$100**, minus $25 per hint used (floor $25). Pays once per player per day, only on a correct answer.
- Answer checking is **fuzzy**: case-insensitive, ignores leading articles ("a", "the"), trims plurals where the riddle config lists accepted variants, and each riddle carries an explicit `answers:` list in config (the canonical answer plus accepted alternates). (Open Question 3.)

---

## 9. Shared systems

### 9.1 Menu & compass integration
- ExcelMenu registry entry `braingames` (config-first, per ExcelMenu 2.0): compass button "Brain Games" opens the game picker.
- Game picker → per-game tier picker → round starts (or **Resume** prompt if a saved round exists for that game, §9.5).
- All menu text lives in plugin config/lang, not code (fleet config-first pattern); menu entries follow the registry pattern so What's New / Guide / website surfaces can describe them at install time.

### 9.2 Daily cap & streaks
- **Cap:** $1,500 per player per day (America/Chicago day boundary) summed across **all** games including bonuses. A completion that would cross the cap pays only up to the cap and tells the player they've hit today's Brain Games limit. Admins can read cap state (§10); the cap is config (`economy.daily-cap: 1500`) but changing it is a Mike decision, not a default to drift.
- **Streak:** consecutive days with ≥1 paid completion. On the **first paid completion of a day**, add a streak bonus of **$10 × current streak length, max $50** (day 1 = $10 … day 5+ = $50). The bonus counts toward the cap.
- **Payout math sanity check** (why these numbers): a realistic family session is 2–4 rounds ≈ $100–$400. Reaching the $1,500 cap requires e.g. six Hard Sudokus (~2+ hours of real solving) or ~10 Challenge Word Searches — i.e., the cap binds only on deliberate grinding, never on family play. Kid payouts are small by design ($25–$50/round) so Rylie's earnings stay pocket-money scale even before the anti-farm rule.

### 9.3 Anti-farm (kid tiers)
- Players are flagged **kid** in config (`kids: [uuid-or-name…]`) — set by console admin command (§10), never self-service.
- Kid tiers (Word Search Kid, Sudoku 4×4, Memory Kid, Quick Math Kid, Scramble Kid) pay full table value **only to kid-flagged players**. Anyone else may play kid tiers and earns **exactly HALF the kid payout** for that tier (rounded down to the cent). **Mike's decision, Nov 4, 2026 — this supersedes the original $0-practice-mode recommendation in Open Question 6.** Kid tiers stay visible and playable for everyone; the half rate plus the shared daily cap keeps the farm dead (an adult grinding kid tiers earns half pocket-money rates against the same $1,500 cap) without locking adults out of playing alongside their kid.

### 9.4 Word lists (config-first + hand-check gate)
- All word content lives in **data files, not code**: `plugins/ExcelBrainGames/wordlists/<theme>.yml` — each entry: the word, tier tags (`kid`, `standard`, `challenge`), and for riddles a separate `riddles.yml` with `question`, `answers: [...]`, `hint`, `kid: true/false`.
- Lists are hot-reloadable via the admin reload command (§10); adding a theme = dropping in a file, no rebuild.
- **PRE-RELEASE GATE (blocking):** every list tagged `kid` must be **hand-checked by Mike (or his delegate) before the kid tier ships** — spelling level, no scary/violent/brand words, nothing a 6-year-old can't read. The build ships with kid lists in `wordlists/draft/` and the kid tiers stay **disabled in config** (`games.kid-tiers-enabled: false`) until the check is done and the files move to `wordlists/`. This gate is named in the install packet when the time comes. Draft lists in the build are *starting material for the check*, never silently final.

### 9.5 Sessions, save/resume, limits
- **One active round per game per player** (a player may have a Word Search *and* a Sudoku in progress simultaneously).
- Grid games **auto-save on every move**; closing the GUI (or logging out, or a server restart) preserves the round exactly. Reopening the game offers **Resume** or **New Game** (New Game asks for confirmation if a round is in progress, because the old round is discarded unpaid).
- Text-game rounds (Quick Math, Scramble) save at question boundaries.
- A saved round **never expires**; a round completed on a later day pays against **that day's** cap (Open Question 5).
- Riddle of the Day has no resume state beyond attempts-used + hints-used for the day (persisted; resets at day boundary).
- Data storage: per-player YAML under `plugins/ExcelBrainGames/data/<uuid>.yml` (active rounds serialized as grid state + placements/found flags; daily totals keyed by date; streak count + last-played date). Small data, human-readable, consistent with fleet plugin data habits. Daily totals prune entries older than 35 days on load.

### 9.6 Permissions
- `braingames.play` (default: true) — open the menu and play.
- `braingames.admin` (default: op) — admin commands; **console always allowed** regardless (fleet console-admin rule).

---

## 10. Admin console commands

All under `/braingames admin …`, runnable from console and by `braingames.admin` holders:

| Command | Effect |
|---------|--------|
| `admin status <player>` | Show active rounds per game, today's earnings vs cap, current streak |
| `admin reset <player> [game]` | Discard the player's active round (one game, or all) — unpaid |
| `admin resetday <player>` | Zero today's Brain Games earnings total (cap room restored) |
| `admin setkid <player> <true\|false>` | Set/clear the kid flag (anti-farm, §9.3) |
| `admin reload` | Reload config + all word lists from disk |
| `admin riddle` | Show today's riddle + answer (console/support use) |
| `admin setriddle <index>` | Force today's riddle to a list index (support/events) |

Player-facing support cases these exist for: a stuck/corrupt round, a kid flag on the wrong account, a disputed daily total, a riddle typo'd in config mid-day.

---

## 11. Build order (recommendation)

**Phase 1 — Framework + Word Search + Memory Match.**
The shared framework (menu/compass entry, session storage, cap/streak ledger, admin commands, config/lang) plus the two grid games that exercise it most simply. **Word Search first because it's Mike's game** and it proves the hardest interaction (two-tap selection in a translated inventory GUI). **Memory Match alongside** because its logic is trivial once grid rendering exists — it shakes down card-flip rendering and save/resume cheaply, and it's the most Rylie-playable game in the set.

**Phase 2 — Sudoku.**
The deep end: uniqueness-proving generator, palette-first placement, picture mode, and 9×9 two-view paging. Built after Phase 1 proves the Bedrock inventory-tap path in real hands, so Sudoku's UI risks land on proven machinery.

**Phase 3 — Text trio: Quick Math, Word Scramble, Riddle of the Day.**
All three share one form/chat question-loop module; once it exists they're content + small rule differences. Cheapest phase, fully independent of grid work.

Each phase installs separately through the normal gates (packet, backup-first, empty-server or Mike-directed window), with its own player proofs — the family never waits for all six to play the first two.

## 12. Test plan

**Unit/logic tests (build-time, blocking):**
- Sudoku generator: property test over ≥500 seeds per size — every generated puzzle has exactly one solution (verified by an independent solver), givens within tier bands.
- Word Search generator: over ≥500 rounds per tier — every listed word is findable at its stored placement, all placements in-bounds and direction-legal for the tier, collision letter-matching correct.
- Memory Match: deck integrity (each symbol exactly twice) over seeds.
- Economy: cap arithmetic (partial payout at cap boundary), streak bonus steps ($10→$50), anti-farm flag behavior, check-button deduction math, partial-credit scramble math.

**Rig tests (local Paper + Geyser, blocking):**
- Full round of each Phase game start-to-payout on Java and on a Bedrock client through Geyser.
- Save/resume across GUI close, logout, and server restart mid-round.
- Two players in different games simultaneously; one player with rounds in two games.
- Admin commands from **console** (status/reset/setkid/reload) — fleet console rule proof.

**Bedrock phone test gate (Mike, blocking for install — from WRV-196):**
- Mike plays Word Search on his phone: two-tap selection lands on the intended letters (no off-by-one taps), found highlighting reads clearly at phone size.
- Sudoku palette-first flow on phone; 9×9 paging toggle (Phase 2).
- Text-game form entry on phone (Phase 3).
- Kailey on iPad/controller spot-check if available (controller navigation of inventory grids is the known-awkward surface).

**Pre-release gates (blocking):**
1. **Kid word lists hand-checked** (§9.4) — kid tiers stay config-disabled until done.
2. **Mike's Bedrock phone pass** (above).
3. Payout/cap values in this spec confirmed unchanged at packet time (economy changes are Mike decisions).

## 13. Out of scope for v1
Leaderboards, head-to-head/multiplayer rounds, larger-than-9×6 Word Search grids (viewport), Sudoku pencil-notes mode, timed competitive modes, cross-day puzzle-of-the-day for grid games. All are addable later on the same framework; none block v1.
