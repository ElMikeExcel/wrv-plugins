# ExcelBrainGames 0.3.1 "Letter Tiles" — SPEC DELTA

Against: SPEC.md (Brain Games, settled Nov 4, 2026) and the live 0.3.0 build.
Commissioned: Mike's decision Nov 5, 2026, 10:47 AM CST ("Build a letter-tile
resource pack so the grid itself shows letters"), after his own mobile Bedrock
play test proved 0.3.0's grid games unplayable; extended twice the same morning
(10:48 AM scope additions; ~10:52 AM Sudoku/Quick Math screenshot evidence).

**This release is presentation-layer only.** Game logic, payouts, saves, caps,
and round generation are byte-for-byte behavior-identical to 0.3.0 — proven by
the full carried suite passing with exactly its 0.3.0 check counts (622,227).
An add-on build later the same day closed the one residual §8 had flagged
(the Quick Math kid question print); §8 carries the mechanism, and the jar
of record is the add-on build's (see BUILD-RECORD.md).

## 1. The defect (player-proof FAILURES against 0.3.0, Bedrock usability)

Evidence: Mike's screenshots, Nov 5, 2026 (in `~/workspace/user/files/`).

- **Word Search** — `13580_29_x6d4.png`: grid letters live only in item names;
  mobile Bedrock has no hover; tapping a cell to read it SELECTS it. Unplayable.
- **Sudoku** — `13582_30_n86w.png` (read firsthand): Geyser reflows the
  54-slot chest GUI to ~5 columns × ~11 rows on his phone. The Kid 4×4 board's
  panes, palette papers (their stack-count digits DO show), eraser, counters
  and close button scatter across the wrap. No square is readable. **Letter
  tiles alone do not fix this** — the layout itself is gone.
- **Quick Math (kid card)** — `13581_31_9ryd.png` (read firsthand): three
  identical emerald items; the answer values live only in their names; a tap
  is an answer. Blind guessing.

## 2. Mechanism decision: `minecraft:item_model`, not CustomModelData

- Paper API 26.3 build 231 (the live server's API line; jar pinned by SHA-256
  `f0df9e7f…` in build.sh) exposes `ItemMeta.setItemModel(NamespacedKey)` —
  verified in the API jar before the build started.
- Geyser's current docs (geysermc.org/wiki/geyser/custom-items/, read Nov 5,
  2026) define the v2 mapping type `"definition"` keyed on exactly this
  component: `{"type":"definition","model":"<ns>:<path>","bedrock_identifier":…}`
  under `format_version: 2`, grouped per Java base item. CustomModelData is
  the pre-1.21.4 `legacy` route — deprecated upstream; not used.
- The view stamps `braingames:<key>` onto the SAME items 0.3.0 already
  renders (same materials, names, lore, amounts). **Fallback law:** a client
  without the pack ignores the component and sees exactly 0.3.0.

## 3. Tile key space (one class: `core/TileKeys.java` — 205 keys)

| Family | Keys | Base items (as stamped in Views) |
|---|---|---|
| Word Search | `ws/<a–z>_normal` (26) | paper |
| | `ws/<a–z>_selected` (26) | yellow_stained_glass_pane (first tap) |
| | `ws/<a–z>_found` (26) | lime_stained_glass_pane |
| Sudoku | `sudoku/<1–9>_given` (9) | light_blue_stained_glass_pane |
| | `sudoku/<1–9>_placed` (9) | paper (board cells AND palette; palette keeps its stack-count digit too) |
| | `sudoku/<1–9>_wrong` (9) | red_stained_glass_pane (Check flash) |
| Quick Math | `number/<0–99>` (100) | emerald (kid choice cards only) |

State sets mirror the 0.3.0 code exactly — no invented states. Sudoku blanks
carry no tile (a blank has no digit to show; the cell is a plain light-gray
pane as in 0.3.0). The Sudoku palette's "selected" indication stays the
existing enchantment glint (unchanged 0.3.0 behavior).

**Sudoku picture mode:** placed cells are real picture items — already
visible, no tile (same verdict class as Memory Match). Given and wrong-flash
cells are PANES in both modes, named after the picture — so in picture mode
they carry the digit tile of the picture's palette number, and the chat-grid
legend (§5) ties number ↔ picture. One consistent reading across the board,
the palette, and the chat square.

**Quick Math number range (Extension 1 defense):** per-value tiles for
0–99. By construction, kid answers are ≤ 20 (a+b ≤ 20, a−b ≥ 0) and
distractors are the answer ±(1..10)×(1..2) with negatives rejected — design
bound 40. TileTest sampled 2,000 generated kid rounds: observed max **40**.
A label outside 0–99 (never produced by the kid generator) simply doesn't
stamp and renders as 0.3.0. Standard/Challenge Quick Math is typed — untouched.

## 4. Word Scramble kid choices (Extension 1): chat print, not tiles

Whole words can't be tiles. When a Scramble **kid** choice card renders a
word prompt for the first time, the view prints ONE chat line — the scrambled
word plus the three choices numbered 1–3 **in card order** — so a mobile
player reads before tapping. Once-per-prompt is enforced by
`core/ChoicePrintTracker` on the (transient, never persisted) view holder:
re-renders (retries, click feedback) stay silent; the next word prints again.
The scrambled word is included because it, too, lives only in an item name on
the card. Standard Scramble is typed — untouched. No other behavior change.

## 5. Sudoku squareness (Extension 2): the chat square

Tiles fix WHAT a cell says; the reflow destroys WHERE cells are. So Sudoku
also renders its board as bordered text in chat — `games/sudoku/SudokuTextGrid.java`
(pure logic, unit-tested cell-by-cell at 4×4, 6×6, and 9×9):

- Printed **at every board opening** and on demand from a new **"Show board
  in chat"** book control (slot 44 — filler in all three layouts; all sizes).
- True square: box borders from the round's own box dimensions, rows and
  columns numbered so a chat cell maps straight back to the GUI tap surface.
- Givens are `[bracketed]`, placed digits plain, blanks dotted — readable at
  a glance, no color dependence.
- Picture rounds print their palette numbers plus a legend line
  (`1 = Apple   2 = Boat …`).
- The GUI remains the only tap surface; Java clients get the identical print
  (parity). Limits, honestly: chat scrolls — the print is a snapshot at open
  time (the Show-board button re-prints the current state); proportional
  chat fonts can soften column alignment on some clients, though Minecraft's
  digit glyphs are fixed-width, which is what the grid is made of.

## 6. Games considered, no change (record of verdicts)

- **Memory Match — OUT OF SCOPE (confirmed):** card faces are real item
  pictures, already visible on every client. Nothing to tile.
- **Riddle — OUT OF SCOPE (confirmed):** pure text/chat already.
- **Word Search chat print — not added:** the grid is 9-wide like Sudoku,
  but Word Search's defect was letter visibility, which the tiles fix
  directly; its cells must be read in place to circle words, and a chat
  snapshot would go stale on every selection. Revisit only if Mike's
  post-0.3.1 Bedrock proof says the reflow still beats him there.

## 7. Pack engineering

- ONE generator (`packs/generate_tiles.py`) draws every tile from a defined
  hand-set 5×7 pixel font with PIL — **no AI image generation** (it garbles
  letters) — 64×64 PNGs, state-color backgrounds, WCAG contrast ≥ 4.0 for
  every tile (validator-enforced; worst tile 4.40). The same PNG bytes ship
  in both packs. Three tiles were eyeballed post-build (Q, 42, 7): exact.
- **Java pack:** `pack.mcmeta` pack_format **97** (Paper 26.3-235
  version.json: pack_version resource 97.1), item definitions
  (`assets/braingames/items/<key>.json`, type `minecraft:model`) → models
  (`parent: minecraft:item/generated`) → textures. Served via
  server.properties `resource-pack` + `resource-pack-sha1`;
  `require-resource-pack` stays **false** (fallback law).
- **Bedrock pack (Geyser):** manifest format_version 2 with FIXED UUIDs
  (stable across rebuilds — bumping the version updates the pack instead of
  duplicating it), `textures/item_texture.json`, PNGs under
  `textures/items/`. Geyser serves it from its `packs` folder on join.
- **Geyser mappings:** `format_version: 2`; one `definition` per
  base-item+model pair (205). **Docs ambiguity, mitigated:** the wiki derives
  the default icon shorthand by replacing `:`→`.` and `/`→`_` in the bedrock
  identifier, while its item_texture.json examples key textures by the full
  identifier. Every definition therefore sets `bedrock_options.icon`
  explicitly AND item_texture.json carries BOTH key forms pointing at the
  same texture — either reading resolves.
- Geyser requires `enable-custom-content: true` and a restart to load
  mappings + pack (wiki, "Loading mappings and resource packs").

## 8. Known limits / residuals

- **Real-client rendering cannot be rig-proven.** The rig proves boot,
  enable, and zero SEVERE on the exact live Paper build. What Bedrock and
  Java clients actually draw is settled only by the packet's player proofs.
- **Bedrock pack DECLINED is an open question:** a Bedrock player who
  declines Geyser's pack prompt may see a placeholder instead of today's
  vanilla pane for stamped cells. Named as an explicit player proof in the
  install packet; if it renders badly, the mitigation is a follow-up build,
  not a silent hope.
- **Quick Math kid QUESTION text — CLOSED by the 0.3.1 add-on build (Nov 5,
  2026, later the same day).** The question ("7 + 5 = ?") lived in the
  prompt paper's name; Extension 1 tiled the answer OPTIONS only. The
  add-on closes it with the Scramble pattern (§4): in
  `Views.fillQuestion`'s choices branch, a kid choice round now prints the
  question ONCE per question to chat — `Quick Math — question N of 10:
  <question exactly as the card states it>` — gated by the same
  holder-scoped `ChoicePrintTracker`, keyed `qm|<index>|<question>|<choices>`
  (the index makes an identical question later in the round print again as
  the NEW question it is; re-renders/reopens within the same card session
  stay silent). **Which modes needed it, code-verified:** ONLY the choice
  rounds (kid tier with 3-choice on — the only mode whose prompt never
  leaves the card). Typed rounds (Standard, Challenge, and kid with
  choices config off) did NOT need it: `GuiListener.openQuestion` and
  `TextInputListener.answerQuickMath` already deliver every typed question
  via `promptInChat` at card open and after each answer — and typed rounds
  carry no choices, so the new print structurally cannot fire there (no
  double-print). Presentation only, again: no logic, payout, save, or cap
  change; the carried suite still passes at exactly 622,227 checks.
- Sudoku palette selection on Bedrock still relies on glint translation
  (unchanged from 0.3.0; the palette digit itself is now tiled + counted).
