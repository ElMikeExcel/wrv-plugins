# ExcelBrainGames 0.3.0 — CHANGELOG

Cumulative release, built Nov 5, 2026 (America/Chicago). **Installed
live Nov 5, 2026, 5:34–5:53 AM CST** over 0.1.0 (see INSTALLED.txt).
0.3.0 completes the Brain Games set (WRV-196): it carries Phase 2
(Sudoku, built as 0.2.0 and superseded the same morning — never
installed standalone) and Phase 3 (Quick Math, Word Scramble, Riddle
of the Day) on top of the Phase 1 framework. Mike's direction, Nov 5,
2026: "Go full blast with brain game. Then we'll fix and edit as we
test later."

## New in Phase 2 — Sudoku

- **Sudoku** joins the `/games` picker as a live door. A generator
  deals a fresh puzzle with exactly one solution every round
  (independently solver-proven in tests, 500 seeds × every tier).
- Tiers: Kid 4×4 picture mode ($25; symbols instead of digits), 6×6
  ($60), 9×9 Easy ($120) / Medium ($180) / Hard ($250). Palette-first
  placement: pick a number, tap cells; givens are locked; an eraser
  clears your own cells.
- **Check button:** flags your wrong cells. Adults pay 10% of the
  payout per use, max 3 uses; kid tiers check free and unlimited.
- **Par bonus:** finishing a 9×9 Medium or Hard at or under par earns
  +20%.
- 9×9 plays in two views (rows 1–5 / 5–9) with a banked number
  palette — the menu grid is 9×6, so the board pages instead of
  shrinking.

## New in Phase 3 — the text trio (one shared question loop)

- **Quick Math** — 10 questions a round; payout is the tier amount ×
  (right answers / 10), rounded down. Kid tier ($25) answers by
  tapping one of three choices — no typing; Standard ($50) and
  Challenge ($90) type answers in chat (captured, never broadcast).
- **Word Scramble** — 5 words a round from the theme lists. Kid
  ($50): 3-choice tap, retries allowed, $10 a word. Standard ($100):
  typed, one try, $20 a word; a Hint halves that word's value.
  Finish-now banks exactly what you've earned.
- **Riddle of the Day** — one riddle a day for the whole server, 16
  in the shipped list. $100 flat, minus $25 for the hint (price
  floors at $25); fuzzy answers count (case, articles, plurals).
  Staff run it from the console: `/games admin riddle` /
  `setriddle`.
- All three save/resume like the grid games and pay through the same
  shared ledger: **$1,500/day cap per player across all games**, each
  round pays once, streak bonus on the first completion of a day,
  adults earn half on kid tiers.

## Completeness

- After 0.3.0 the picker has **no dead buttons and no "coming soon"
  tiles**: Word Search, Memory Match, Sudoku, Quick Math, Word
  Scramble and Riddle of the Day are all live, all tap-reachable
  from the compass Brain Games button on Java and Bedrock alike.
- Console admin now covers all six games (`status` / `reset` accept
  every game; `riddle` / `setriddle` are real, the Phase 1 stub
  message is gone).
- New shipped content: `wordlists/riddles.yml` (16 riddles). Config
  gains the quickmath and scramble tier blocks; everything else —
  payouts, cap, kid flags — is config-first as before, tunable with
  `/games admin reload`.
