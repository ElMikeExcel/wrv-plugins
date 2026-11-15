# ExcelBrainGames 0.3.4 — SPEC DELTA (Back-button pass, audit BB-02)

Against: SPEC.md (Brain Games, settled Nov 4, 2026), the live 0.3.3
build, and the Back-button audit of Nov 13, 2026
(`~/workspace/back-button-audit-20261113/AUDIT.md` + FIXBATCH-SPLIT-R348.md).

## The delta

Navigation only. Every ExcelBrainGames screen now carries a Back
control per Mike's standing rule (Nov 13, 2026):

- The game picker — opened from the Server Menu — gains "Back to
  Server Menu" (slot 26), wired to the ExcelMenu `/menu` opener.
  Before 0.3.4 it had no Back and no Close at all.
- Every board's exit control (slot 53) is now Back: it saves the
  in-progress round and reopens the game picker. Boards: Word Search,
  Memory Match, Sudoku full board (4x4/6x6), Sudoku box picker
  (Bedrock) — and, through the same shared renderer, the Bedrock
  single-box view.
- The Sudoku 9x9 board, which had neither Back nor Close, gains Back
  at slot 51; its Eraser moves onto the high palette bank (slot 49,
  beside digits 6-9). Decision + reversibility: BUILD-RECORD.md.
- The shared Quick Math / Word Scramble question card gains Back at
  slot 18: saves the round (never banks a Scramble round) and
  reopens the picker.

## What does NOT change

Game logic, generators, tiers, givens, payouts, mistake rules, the
$1,500/day cap, the tile packs, config, word lists, the menu entry,
and the save format (no new key — a 0.3.4 save is byte-for-byte a
0.3.3 save). ESC / native close behavior is untouched everywhere:
it bare-closes with the round saved, as before.
