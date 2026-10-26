# ExcelWandBars 0.3.0: organized Spell Book (staged Oct 26, 2026, not installed)

0.2.9 (staged, `7634a3ab`, draft PR #88) plus an organized Spell Book. Nothing else changes.
Jar sha256 `9dff01536f882fbdeda0f4d2911269d8402daf1cbbeada3cc19a580a945db4e5` (deterministic build, date 2026-10-26).
Restart needed (jar). Config and spellbook.yml edits after that: `/wandbars reload`.
Design approved by Mike on Oct 26, 2026 (2:04 to 2:17 PM CDT): `spells-by-seal4-20261026/PLAN.md`.

## Why
By Seal 4 a Master wand knows 104 spells. The 0.2.9 Spell Book is one long list (one Bedrock form with 100+ buttons,
or 100+ Java chat lines). Mike: "When all the abilities are released we should be able to see them all in the spell
book nice and organized."

## What's new
- **Groups.** 8 groups: Healing, Defense, Damage, Movement, Summon, Building, World, Utility, read from a new
  `plugins/ExcelWandBars/spellbook.yml` (written from the jar on first boot, reloadable). A spell on the rank wands
  that is in no group shows under `Other` and the console warns on boot and on reload, naming it. Disposal is not
  listed (removed from the plan).
- **Home screen** (`/spellbook`, the Spellbook button, the Bedrock Classic spell list): wand name and spell count,
  how many are on the bar and in the book, rank and SP lines as before, one button per group with its count,
  `All spells A to Z`, `Put spells on my bar`, wand controls, tip OK. Groups with nothing to show are hidden.
- **Group pages.** Sorted by rank band (the lowest rank wand that has the spell), then name. Spells on the bar show
  `bar N`. Java: chat pages of 10 with `[< Prev] [Groups] [Next >]`. Bedrock: forms of 12 with `Next page`,
  `Previous page`, `Back to groups`, `Close`. Tapping a spell opens the same spell info screen as 0.2.9; its back
  button now reads `Back to <Group>` when you came from that group.
- **Locked spells, no spoilers.** Spells a higher wand rank gets show gray: `Arrow Rain: unlocks at Apprentice`
  (Java) or `Arrow Rain - unlocks at Apprentice` / `Arrow Rain (Apprentice)` (Bedrock). Tapping one shows a short
  page: `Unlocks at Apprentice rank. Cast spells to earn SP and rank up your wand.` Only spells already on the rank
  wands are listed, so only spells from seals that are broken. Group buttons: `Damage (23, +7 locked)`, or
  `World (6 locked)` when none are known yet (Java `[Damage 23 +7]`, `[World +6]`).
- **Bar picker uses the groups.** `Pick a spell for this slot` now asks for a group first (or All A to Z), then a
  paged list. Spells already on the bar show `(slot N)` and swap places, as in 0.2.9.
- **Commands.** `/spellbook group <name> [page]`, `/spellbook all [page]`, `/spellbook locked <spell>`,
  `/spellbar pick <slot> <group|all> [page]`. Admin: `/wandbars groups [player]` (group counts, read only).
  `/wandbars reload` reloads spellbook.yml and prints the group count. A broken spellbook.yml falls back to the jar
  copy with a console warning (players see the default groups).
- **Switches** (`config.yml`, `spell-book:`; missing = true, so the live config needs no edit):
  `groups: false` = the exact 0.2.9 flat Spell Book and picker. `show-locked: false` = no gray spells.
- Bedrock text: every form line 38 characters or less, no em dashes, no "click". Java/Bedrock parity on every screen.

## Not changed
0.2.9 own spell bar (slots, seeding, sync, `/spellbar` slot/put/remove/swap), spell info screens, rank-up by
casting, SP, wand slot 9, Tap bar / Tap to pick / Classic / Cycle styles, Magic `wands.yml` / `spells.yml`,
ExcelMagicGate stages, every other plugin.

## Files
New: `core/SpellbookGroups.java` (pure: parse spellbook.yml, lookups, Other), `core/SpellbookText.java` (pure:
counts, sorting, paging, every player line), `spellbook.yml`, `tests/SpellbookTest.java`,
`tests/data/spells-by-seal.csv`.
Changed: `MagicHook.java` (home, group, locked screens; back links), `LoadoutHook.java` (grouped picker),
`ExcelWandBars.java` (load/reload/check, `groups` admin, routing), `RankUpHook.java` (`ladderOrder()`),
`config.yml`, `plugin.yml` (0.3.0), `build.sh`, `tests/TestMain.java`.
Full diff: `ExcelWandBars-0.2.9-to-0.3.0.diff`.

## Tests
Unit 1319 passed, 0 failed (0.2.9: 1227). Private rig at Seal 1 and at a simulated Seal 4 full set, Java and Bedrock
form paths: see TEST-RESULTS.md. Not tested on a real Bedrock phone yet.
