# ExcelDowned 1.0.4 — wand-owner guard (Nov 23, 2026)

## The defect

A player who logs out while DOWNED and returns after the 5-minute
bleed-out window lost their Magic wand permanently and silently
(proven live Nov 23, 2026 on MikeExcel; full evidence in
~/workspace/wand-restore-20261123/RECORD.md):

1. While downed, ExcelDowned sets the player's display/list name to
   "✝ DOWNED <name>".
2. Magic wands bound to their owner's display name derive their
   owner string from that display name (`Wand.takeOwnership` in
   Magic 11.2.5 reads `player.getDisplayName()`, color-stripped).
   When Magic saved the mage at logout, the wand's owner had become
   "✝ DOWNED MikeExcel".
3. At the next login the wand failed Magic's owner validation and
   was deleted before the bleed-out death fired — no drop, no
   lost-wand registration, no log line, no recovery path. The wand
   carries keep + undroppable marks precisely so it survives death;
   it is the only item class whose validity depends on an owner
   string, so wands were uniquely vulnerable.

The bleed-out death and its normal loot drop are documented design
and are NOT changed. Only the wand's silent deletion is the defect.

## The fix (this plugin's half)

The mark is presentation; it must never cross a handoff as identity.

- `DownedListener.onQuitUnmark` — PlayerQuitEvent at
  EventPriority.LOWEST: if the quitting player is downed, restore
  their real display/list name BEFORE any other plugin's logout
  handling can read or save it. The downed session persists exactly
  as before (wall clock rules); only the visible name is handed
  back early. The saved originals are kept, so a later revive or
  finalize restores through the existing path unchanged.
- `DownedListener.onJoinUnmark` — PlayerJoinEvent at LOWEST: clears
  any stale mark before all other join handling. A genuinely live
  session re-marks in the existing NORMAL join handler — unchanged.
- The mark's composition moves into `DownedMark` (one home, unit-
  pinned to the incident string "✝ DOWNED MikeExcel").
- Toggle: `wand-owner-guard.unmark-on-quit` (default true).

## The other half

ExcelMagicGate 0.2.1 repairs, at join, any wand whose owner already
carries the mark (re-stamped by Magic's own takeOwnership). The two
halves are independent: either alone prevents the observed loss
path; together they also heal wands contaminated before the fix.

## Not changed

Downed entry, bleed-out timing/persistence, damage absorption,
revive paths (spell / potion / cleric / admin), give-up, death and
drop behavior, records. The mark still shows, identically, the
whole time a player is online and downed.
