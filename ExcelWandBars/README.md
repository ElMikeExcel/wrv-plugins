# ExcelWandBars 0.1.0

Separate helper plugin for Magic (elmakers) 11.2.5 on Paper 26.3. It does not
change Magic's files and does not touch EliteMobs (MagmaGuy).

## 1. Sneak flips the open spell bar
Magic 11.2.5 has wand actions only for left/right click, drop and swap (each
with a sneak variant). There is no plain "sneak" wand action and no command
that cycles the bar, so this plugin adds it through Magic's API
(`Wand.cycleHotbar()`).

* A sneak PRESS while holding an inventory-mode wand with its bar open flips
  1 -> 2 -> 3 -> 1. Releasing sneak does nothing, and the bar stays where it is,
  so you never have to keep sneaking.
* Ignored: bar closed, chest-mode wands, wands with one bar, in a vehicle,
  swimming, gliding, flying, and the EliteMobs worlds (`em_` prefix plus the
  listed worlds). A second press within 250 ms is ignored.
* Controller players who do not open the bar are never affected.
* After a flip one aqua action-bar line is sent once, e.g. `Bar 2/3: Boon, Feed`.
* Bedrock: if the touch sneak button is a toggle, one tap starts sneaking and
  flips; the next tap stands up (no flip).

## 2. /spellinfo (aliases /spellbook, /wandspells)
Readable spell info without casting (Magic's own `/spells` is an op-only list).
Bedrock gets a Geyser form (list, tap a spell for details); Java gets chat
lines with hover details. Values are read live from Magic's spell templates
for the rank on the wand: description, mana cost, per-second cost, cooldown,
range, duration, rank N of M and what the next rank needs.

## 3. Menu compass guard
While the Magic bar is open the real inventory is stored away, so a menu
compass given then (join, respawn or /compass) cannot see the existing one and
the two stack when the bar closes. A few ticks after the bar can close (and
after join and respawn), when no bar is open, compasses tagged
`menujoin:menu_compass` or `excelmenujoin:menu_compass` are trimmed to exactly
one. It never gives items.

## Commands and permissions
* `/spellinfo [spell]`: `excelwandbars.spells` (default true)
* `/wandbars [reload | flip <player> | info <player>]`: `excelwandbars.admin` (op)

## Build and test
`./build.sh` (JDK 25, `--release 21`), `./run-tests.sh` (66 unit checks,
including a real Cumulus SimpleForm build).
