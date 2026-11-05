# ExcelBrainGames 0.1.0 — CHANGELOG

First release, built Nov 4, 2026 (America/Chicago). **Installed live
Nov 5, 2026, 12:05–12:23 AM CST** (see INSTALLED.txt). Phase 1 of the
Brain Games set (WRV-196): the framework plus Word Search and Memory
Match. Sudoku follows in Phase 2; Quick Math, Word Scramble and Riddle
of the Day in Phase 3.

## What players get

- **`/games`** (aliases `/brain`, `/puzzles`) — and the **Brain Games
  button in the compass / Server Menu** — opens the game picker. Every
  player-facing action is tap-reachable from the button on Java and
  Bedrock alike; Sudoku and the Phase 3 games show as "coming soon"
  notices, no dead buttons.
- **Word Search** — a fresh generated puzzle every round. Tap the first
  letter of a word, then the last letter to circle it; found words stay
  lit green and strike off the list. Themes: Animals, Food, Minecraft,
  Ocean, School, Space (plus kid themes). Tiers: Kid 6×6 ($40),
  Standard 9×6 ($90), Challenge 9×6 ($150, diagonals and backwards
  words allowed).
- **Memory Match** — flip two cards, match the pairs. Tiers pay
  $30 / $55 / $100 / $160 (6 / 8 / 12 / 18 pairs).
- **Save / resume** — one round in progress per game; it saves itself
  on every move and is still there after you close the menu, log out,
  or the server restarts. Starting a new round over a saved one asks
  twice (armed discard).
- **Money rules** — all Brain Games share one **$1,500/day cap per
  player**; each puzzle pays once; the first finished game of a day
  earns a streak bonus ($10 × streak days, up to $50) that counts
  toward the cap.
- **Kid tiers** — ON from launch (word lists hand-checked). A
  kid-flagged player earns the full kid payout; everyone else earns
  **half** on kid tiers (Mike's Q6 call) — parents can play alongside
  the kids without farming the easy boards.

## For staff

- **Console admin** (Mike's fleet rule — every plugin ships a console
  way to help a player): `/games admin status|reset|resetday|setkid|
  reload`, plus `riddle`/`setkid`-family Phase 3 stubs answering in
  words. Players need `braingames.admin` (default op); playing needs
  `braingames.play` (default true).

## Under the hood

- Config-first: payouts, tier shapes, the cap, themes, symbols, kid
  flags and messages all live in `config.yml` + `wordlists/`; tuning
  is an edit + `/games admin reload`, never a rebuild.
- Ledger is day-keyed on America/Chicago with a 35-day prune; payouts
  go through Vault (EssentialsX Economy on live).
