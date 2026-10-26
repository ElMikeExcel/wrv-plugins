# TEST RESULTS: ExcelWandBars 0.2.9 (Oct 26, 2026)

**Current build: 21-slot default** (Mike, 2:02 PM CDT). Jar `7634a3ab0b7f431092915451a51e762e63751349f904205dd1cdc27e7070cfbf`.
Unit tests 1227 passed, 0 failed (`evidence/unit-tests-21.log`). Slot-count rig cases re-run on this exact jar, see
"Re-run with 21 slots" at the end. The tables below the first one are the original 14-slot run (`8f9de5c2`); every
case that does not depend on slot count is unchanged code.

## Unit tests
`src/run-tests.sh`: **1202 passed, 0 failed** (0.2.8 had 1105; `LoadoutTest` adds 97). Log: `evidence/unit-tests.log`.
Covers: seed (first N, order kept), sync (first empty slot, full bar -> Spellbook, seen list, re-learned spells not
re-added), put/move/swap/remove/resize, layout (unknown spells skipped, pages from the last filled slot, parking from
pages*8), placement order, every player line and button <= 38 characters and free of em dashes, pages line for 1/2/3+
pages, existing 0.2.8 controls and tip text tests.

## Private rig (`wb029-rig`, Paper 26.3, Magic 11.2.5, Geyser + Floodgate, copy of live plugins and configs)
Test accounts only (offline-mode bots). Bedrock form path driven with the rig-only `simbedrock` / `formtap` admin
helpers (same Floodgate form objects and callbacks; the form itself falls back to chat because the bot is Java).

| Case | Result |
|---|---|
| A. Existing 0.2.8 player, Seal 1 Master wand (17 spells), first load on 0.2.9 | PASS: seeded first 14 silently, global slots identical to 0.2.8 (1-7, 8-14), 3 spells to the Spellbook, 3 pages -> 2 |
| B. New player, Beginner wand | PASS: 6/14 seeded, 1 page |
| C. Seal stage 2 file swap + `magic load` | PASS (as expected): held wands do not change, so nothing is added; new spells arrive with rank-up or a new wand |
| D. Rank-up by casting under stage 2 (+21 spells) | PASS: 8 placed into empty slots 8-14 in wand order, 13 to Spellbook; 3 named lines + `18 more new spells. See /spellbook` + `Change your bar: /spellbar`; rank cheer and title intact |
| E. Next rank-up with a full bar (+7) | PASS: all 7 to Spellbook, capped lines |
| F. Java `/spellbar` put, swap, remove, move, unknown spell | PASS: right messages, bar re-applied at once |
| G. Bedrock forms: Spell Book button, bar form, slot form, pick spell, spell info Put/Move/Take off + slot picker | PASS: callbacks change the bar; displaced spell goes to the Spellbook |
| H. Relog | PASS: same loadout |
| I. Stage 5 Master wand (105 spells) | PASS: 14 on bar, 91 in Spellbook, 2 pages, no lines (new wand = seeded) |
| K. Full rig restart (twice: run 2 and run 3) | PASS: loadouts identical after each restart |
| K2. Wand knows fewer spells than the saved bar (stage files rolled back on the rig) | PASS: unknown slots show empty, never cast; slots stay saved |
| L. Wand slot 9, Tap bar opener, Java Spell Book `[My spell bar (change it)]` | PASS |
| M. Second wand given while holding the first | PASS: loadout unchanged |
| N. Classic player ranks up, then switches to Tap bar | PASS: loadout updated quietly, used on switch |
| O. `enabled: false` + reload, then back on | PASS: off = every spell in pages (27 spells, 4 pages, same as 0.2.8); on = own bar again |
| P. `slots: 21` + reload, then back to 14 | PASS: 3 pages, menu text names 3 pages; back to 14 = slot 21 spell goes to the Spellbook, nothing lost |
| Q. One-page crouch line | Unit tested only (the test bot cannot send a crouch on this rig) |
| Mana bar, SP line, tap-wand toggle text, spell info screens | PASS: unchanged output in the same runs |

Fix found by the rig: the bar menu said "the next 7 are page 2" even with 21 slots; now names every page
(`Slots 1 to 7 are page 1, 8 to 14 page 2, 15 to 21 page 3.`). Rebuilt, retested (rig run 3).

Not tested: a real Bedrock phone (the rig cannot accept a real Bedrock client from the box). Magic's own messages
(`You have been upgraded to 2 hotbars`, the active spell line) are Magic's and unchanged.

Evidence: `evidence/rig1.js|log`, `rig2.js|log` (contains NUL bytes; use `grep -a`), `rig3.js|log`, boot logs.

## Re-run with 21 slots (Oct 26, 2026, 2:05 to 2:09 PM CDT, jar `7634a3ab`, rig boot clean, known rig noise only)
Evidence: `evidence/rig4.js|log`, `evidence/rig5.js|log`, `evidence/rig-boot-run4-21slots.log`. Rig stopped after.
Boot line: `Own spell bar per player ON (21 slots, new spell lines ON, max 3, 0 player(s) saved).` with NO
`bar-loadout:` section in the rig config (code default), then the staged section appended (same result).

| Case | Result |
|---|---|
| A21. Existing player, Seal 1 Master wand (17 spells), first load | PASS: seeded 17/21 silently, 0 in Spellbook, 3 pages; global slots identical to 0.2.8 (0-6, 8-14, 16-18) |
| Page text (Java `/spellbar`, Bedrock bar form via simbedrock) | PASS: title `Your spell bar (17 of 21)`, `Slots 1 to 7 are page 1, 8 to 14 page 2, 15 to 21 page 3.`, buttons `1:` to `21: (empty)` (all <= 38) |
| B21. Brand new account, Beginner wand | PASS: 6/21 seeded, 1 page |
| D21. Rank-up by casting under stage 2 (+21) | PASS: 15 placed into slots 7-21 in wand order, 6 to Spellbook; 3 named lines + `18 more new spells. See /spellbook` + `Change your bar: /spellbar` |
| E21. Next rank-up, full 21 bar (+7) | PASS: all 7 to Spellbook, capped lines |
| I21. Stage 5 Master wand (105 spells) | PASS: 21 on bar, 84 in Spellbook, 3 pages, no lines on first seed |
| A21b. 17-spell player's wand replaced by a 105-spell wand (rig death/respawn at stage 5) | PASS: 4 empty slots 18-21 filled, rest to Spellbook, capped lines |
| S1. Staged `bar-loadout` section (slots 21) + reload | PASS: `ON (21 slots ...)`, loadouts unchanged |
| O21. Staged off block + reload, then on again | PASS: off = every spell in pages (76 spells, 11 pages, as 0.2.8); on = own 21-slot bar back, 3 pages |

Unit tests added for 21 (25 checks): default constant, 17-spell seed = 0.2.8 packed layout, 3 pages, 0 in book,
rank-up fill 18-21, full bar, 105 spells = 21 + 84 parked from slot 24, Sneak bars 8 + 8 + 5, 21-slot button and
title lengths, bundled `config.yml` says `slots: 21`.
