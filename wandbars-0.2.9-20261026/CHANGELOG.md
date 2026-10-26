# ExcelWandBars 0.2.9 (staged Oct 26, 2026, not installed)

0.2.8 (`bc9fd7f6`, live since Oct 26, 2026 12:13 PM CDT) plus a saved spell bar per player.
Jar sha256 `8f9de5c2c9f218bb8352e58006f56c65701fb7466d1eb4df335d6364d5006862` (deterministic build, date 2026-10-26).
Restart needed (jar). Config edits after that: `/wandbars reload`.

## Why
Tap bar put every spell the wand knows on the hotbar, 7 per page, crouch to page. Seal stages grow the Master wand
from 17 spells to about 124, which is about 18 pages of crouching. Design approved by Mike (voice, 1:04 PM CDT).

## What's new
- **Own spell bar per player.** Each player has a saved loadout: 14 slots by default (2 pages of 7 in Tap bar,
  or 8 + 6 in Sneak bars). `bar-loadout.slots` changes it (1 to 64). Only those spells are on the hotbar pages;
  every other spell the wand knows stays in the Spellbook (kept in the wand's storage, never lost).
- **New spells place themselves.** When a player's wand learns spells (rank-up by casting, or a new wand with more
  spells, for example after a seal break), each new spell goes into the first empty slot, in the wand's own spell
  order. Chat: `New spell: X added to your bar`, or `New spell: X is in your Spellbook` when the bar is full.
  At most 3 named lines (`max-new-spell-lines`), then `N more new spells. See /spellbook` and
  `Change your bar: /spellbar` (clickable on Java). `new-spell-lines: false` = placed quietly.
- **Change it yourself, same on Java and Bedrock.**
  - Spell Book: new button `My spell bar (change it)` (Bedrock form button, Java clickable chat line).
  - The bar screen lists slots `1: Heal` ... `14: (empty)`. Tap a slot: move it, take it off, or pick a spell for it.
  - Spell info screen (tap a spell in the Spell Book): `Put on my bar` / `Move on my bar` / `Take off my bar`, then
    pick a slot. Putting a spell into a used slot sends the old one to the Spellbook; putting a spell already on the
    bar into another used slot swaps the two.
  - `/spellbar` (alias `/mybar`, permission `excelwandbars.spells`):
    `/spellbar`, `slot <n>`, `put <spell> [n]`, `remove <n>`, `swap <a> <b>`.
  - Bedrock uses Geyser/Floodgate forms like the other menus (form lines at most 38 characters); if a form cannot
    be sent, the same menu comes as chat.
- **Existing players.** On first load the loadout is seeded from the current wand's spell order (first 14), silently.
  Bar positions stay exactly where 0.2.8 had them (slots 1-7 page 1, 8-14 page 2). A wand with more than 14 spells
  (today: a Seal 1 Master wand has 17) shows 2 pages instead of 3; the rest sit in the Spellbook.
- **Unknown spells are skipped.** A slot whose spell the held wand does not know shows empty and can never cast. The
  slot stays saved, so the spell comes back if that wand (or another one) knows it.
- **Saved per player UUID** in `plugins/ExcelWandBars/loadouts.yml` (atomic save about 1 sec after a change, plus on
  reload and shutdown). Survives relog and restart. Loadouts are kept while the player is on Classic or Cycle
  (updated quietly) and used as soon as they pick Tap bar or Sneak bars.
- **Fallback.** `bar-loadout.enabled: false` + `/wandbars reload` = exact 0.2.8 behaviour (every spell in pages).
  The saved file is left alone; turning it back on uses it again.
- **Text.** One-page crouch line: `Page 1/1: all N of your spells are on the bar` or
  `Page 1/1: X of your Y spells. More: /spellbar`. Tap bar how-to and tip now say "your spell bar fills the hotbar".
- **Admin.** `/wandbars loadout <player> [show|reseed|save|do <spellbar args>]`; `/wandbars` status and
  `wandbars style <p>` show the loadout state. Rank-up now re-applies the bar 1 sec after the cheer.

## Not changed
Rank-up by casting, SP, daily pace, bonuses, cheer, mana bar, wand slot 9, tap-wand toggle (Bedrock), Tap to pick,
press-and-hold reads, spell info screens, Classic and Cycle styles. Magic `wands.yml`, `spells.yml`, ExcelMagicGate
stages and every other plugin.

## Config (appended by `tools/add-loadout-config.py`; a missing section = ON with these defaults)
```yaml
bar-loadout:
  enabled: true
  slots: 14
  new-spell-lines: true
  max-new-spell-lines: 3
```

## Files
New: `core/BarLoadout.java` (pure loadout logic), `core/LoadoutText.java` (all player text), `LoadoutHook.java`
(storage, sync, menus, `/spellbar`), `tests/LoadoutTest.java`.
Changed: `StyleHook.java` (layout from the loadout, `applyNow`/`applyLater`), `ExcelWandBars.java` (wiring, reload,
commands), `MagicHook.java` (Spell Book button, spell info actions, one-page line), `RankUpHook.java` (re-apply after
rank-up), `core/ControlsText.java`, `core/TipGate.java`, `plugin.yml`, `config.yml`, `build.sh`.
Full diff: `ExcelWandBars-0.2.8-to-0.2.9.diff`.

## Tests
Unit 1202 passed, 0 failed (0.2.8: 1105). Private rig (Paper 26.3, Magic 11.2.5, Geyser/Floodgate): see TEST-RESULTS.md.
