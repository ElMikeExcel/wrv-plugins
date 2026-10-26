# ExcelTrash 0.3.0 (Oct 26, 2026). Upgrade of 0.2.0 (live since Oct 24, 6:42 PM CDT, sha a7c00ba7)

Why: Mike, Oct 26: "Trash gave no confirmation for the delete and it should." (Bedrock screenshots), then
"you can't open a chest and have the trash open at the same time", and the chest feature "isn't explained anywhere".
Plus audit finding A2 (Oct 26): items in the trash slots were lost when a player with keep-inventory died with Trash open.

## Two-tap OK (Java and Bedrock the same)
- First OK deletes nothing. The OK slot turns into TNT named "Tap again to delete N items",
  lore "Not deleted yet. Goes back to normal in 5 seconds.", click sound.
- Second tap within 5 s deletes. If the item count changed between taps, it re-arms with the new count instead.
- After 5 s with no second tap it goes back to the normal red concrete "OK: delete the trash slots"
  (lore "Tap once to see how many items, tap again to delete.").
- After a delete: chat "Deleted N items. Changed your mind? Tap Recover or type /trash undo within 5 minutes.",
  action bar "Deleted N items. /trash undo within 5 min", lava-hiss sound, and the OK slot shows lime
  "Deleted N items" for 3 s. Bedrock hides chat while a screen is open, so the result is always on the button.
- OK with nothing in the trash slots: gray "Nothing to delete: trash slots are empty" and chat
  "Nothing was deleted. The trash slots are empty."
- `close-action: delete` (config, off on WRV) still deletes on close without a confirm, as configured.

## Trash from a chest, explained in the screen
- Slot 27 names what you look at (within 5 blocks): "Trash from the Chest you're looking at" (Barrel, Shulker Box, ...).
  Not looking at one: "Trash from a chest: tap, then tap the chest".
- Tapping it with nothing in view closes Trash and arms a 30 s pick: title "Tap a chest", subtitle
  "the one you want to trash from (30 s)", action bar, and chat "Now tap (right-click) the chest, barrel or
  shulker box you want to trash from. You have 30 seconds." The next tap on a container opens it in chest trash
  mode instead of the plain chest. `/trash chest` with nothing in view does the same.
- Claims and permissions: unchanged access check (a real PlayerInteractEvent so GriefPrevention decides; vanilla
  locks, other viewers and ExcelShops shop chests are refused as before). The pick does not bypass any of it.
- Chest view title "Trash from Chest: tap items to trash"; info slot 28 "Help: tap Chest items above to move
  them down, tap OK twice". Same two-tap OK and same chat message.
- No sneak+right-click shortcut: it would clash with AutoSort, ExcelShops shop chests and claim tools.

## Help
- Help book (slot 29) "Help: put items in the top slots, tap OK twice"; tap = 4 chat lines. `/trash help` (tab-complete).
- One-time chat hint on the first /trash per player (plugins/ExcelTrash/hinted.txt):
  "Trash tip: put items in the top slots, then tap OK twice. Oops? /trash undo within 5 minutes." /
  "To trash from a chest: tap the chest button in Trash, then tap the chest. Or look at it and type /trash chest."
- Cancel is an oak door "Cancel: close, give it all back" (the old barrier looked like Geyser's padding on Bedrock).
- "red side" is now "trash slots" everywhere. Mode switch with items in the trash slots is refused with
  "Empty the trash slots first (OK twice, or take them back)".

## Death fix (audit A2)
- 0.2.0 added trash-slot items to the death drops at LOWEST; a keep-inventory plugin (Essentials keepinv and the like)
  then set keepInventory and clear the drops at HIGHEST, so the items were lost.
- 0.3.0 reads the final result at MONITOR: inventory kept = our stacks are taken out of the drops (by identity,
  no dupe) and put back in the inventory; what does not fit is given on respawn (overflow at the feet). Chat:
  "You died with N items in the trash slots. Nothing was deleted: they stay with you." Normal death = they drop
  with the death drops as before. Chest items still go home first. A cancelled death gives them straight back.
- Limit: overflow waiting for respawn lives in memory; a server stop between death and respawn would lose only
  that overflow (needs a keep-inventory death with a full bag, and a restart before respawning).

## Unchanged
/trash, /bin, /trash undo, Recover (5 min, one time, batches.dat v2), trash.log format (ExcelAuditLog reads it),
compass Trash Bin button, TrashOpenEvent, holder class name TrashHolder (ExcelTips triggers), config.yml keys,
permissions. No new dependencies. Restart needed (no reload command; Paper has no plugin reload).

## Build and tests
JDK 25 --release 21, Paper 26.3 API, deterministic jar. 24/24 unit tests (Confirm 5, DeathReturn 6, older 13).
Rig (Paper 26.3-214 + Geyser-era plugin set, mineflayer through ViaVersion): see TEST-RESULTS.md.
