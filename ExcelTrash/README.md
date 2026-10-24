# ExcelTrash 0.2.0
Original Wink Reyes Valley plugin (MIT). Upgrade of ExcelTrash 0.1.0: same plugin name, same jar name pattern,
same `/trash` + `/bin` commands, same compass "Trash Bin" entry (`[player] trash`), same `/trash undo`.

## Screens (every action is one tap: Java click, Bedrock touch, controller A)
**Inventory trash** (`/trash`, `/bin`, compass Trash Bin): 4 rows. Rows 1-3 = red side (27 slots). Row 4:
slot 27 chest button ("Trash from a chest"), 31 lime dye "Recover last trash" (shows time left), 33 red concrete
"OK, delete the red side", 35 barrier "Cancel". Move items from your inventory onto the red side (tap item, tap slot;
Java shift-click also works), press OK.

**Chest trash** (`/trash chest`, or the chest button while looking at a container): raycast 5 blocks to a chest,
trapped chest, barrel, shulker box, hopper, dispenser or dropper. 6 rows: rows 1-3 = live picture of the real chest
(double chest = 2 pages, arrows at 27/35, page info at 28), row 4 buttons (29 "Trash from my inventory", 31 Recover,
33 OK, 34 Cancel), rows 5-6 = red side (18 slots). Tap a chest item: the server moves that exact stack out of the
real chest onto the red side in the same tick. Tap a red-side item, then an empty chest slot: it goes back in.
No shulker box into a shulker box.

**OK** deletes everything on the red side. All OK presses in one screen are one batch. **Recover** / `/trash undo`
returns the last batch once, within 5 minutes (persisted, downtime does not count). Chest items go back into their
chest if it still exists, has room, is not in someone else's Trash, and you may still open it; the rest goes to you,
overflow drops at your feet. A second undo does nothing. A new screen's OK replaces the old batch.

**Close / Cancel without OK** gives everything back (chest items to the chest, yours to you). Config
`close-action: delete` restores the 0.1.0 "close = delete" behavior.

## Safety
- Access: `/trash chest` fires a real PlayerInteractEvent (right-click block, empty hand) first, so GriefPrevention
  (and any protection plugin) decides; denied = refused. If another plugin answers the tap with its own screen
  (e.g. an ExcelShops shop chest), Trash backs off. Lockable (vanilla lock) chests are refused.
- Lock while in Trash: other players' opens are cancelled, hoppers in/out frozen, block break / explosions /
  entity change / burn cancelled for that chest (both halves of a double chest). Refused if someone else already
  has it open or in Trash.
- Every 10 ticks: chest re-fetched (same type, same size, chunk loaded, player within 8 blocks), picture redrawn;
  if the chest is gone the screen closes and red-side items go to the player (nothing copied from the picture).
- Clicks on the picture are always cancelled and done server-side; double-click collect, number keys, drags into
  the picture are cancelled. Buttons debounce 400 ms. Mode switch needs an empty red side.
- Death with items on the red side: chest items go home, yours drop with your death drops (or stay with keepInventory).
- Server stop: players are kicked first, which closes the screen and gives items back before saving.
- Hard crash (kill -9 / power loss) while items sit on the red side: those items live only in memory until OK, so
  the usual "last autosave" rules apply. No dupe path, but a loss is possible. Pressing OK writes the batch to disk.

## Files
- `plugins/ExcelTrash/batches.dat` v2 (`#v2` header). 0.1.0 files migrate on first start (a pending 0.1.0 recover
  still works). 0.1.0 reading a v2 file sees every batch as expired: rollback can lose a pending recover, never dupe.
- `plugins/ExcelTrash/trash.log`: `TRASH ... source=inventory|chest:world,x,y,z fromchest=N batch=ID` and
  `UNDO ... dropped=N tochest=N ... batch=ID` (same first fields as 0.1.0; ExcelAuditLog parses them unchanged).
- `config.yml`: `close-action: return|delete`.
- Admin: `/trash peek <player>` (exceltrash.admin, op) prints the pending batch and screen state.
- Event for other plugins: `com.winkreyes.trash.TrashOpenEvent` (getPlayer, getMode = inventory|chest).
  Screen holder class stays `ExcelTrash$TrashHolder` so the ExcelTips `holder-contains: [TrashHolder]` trigger works.
  No tip is built in (ExcelTips owns tips).

Build: `bash local-build.sh` (JDK 25, --release 21, Paper 26.3 API, JUnit 5). 13/13 tests.
Rig tests: `rig-tests/` (mineflayer through ViaVersion on a Paper 26.3 test server, plus a tiny test-only probe plugin).
