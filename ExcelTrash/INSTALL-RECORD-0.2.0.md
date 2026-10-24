# ExcelTrash 0.2.0: install (NOT DONE, needs Mike's go and an empty-server restart)

Upgrade of the live ExcelTrash 0.1.0. Same plugin name (ExcelTrash), one jar, same /trash and /bin, same compass
Trash Bin button (server_menu slot 26 runs `[player] trash`, no menu edit needed), same /trash undo.

1. Before: note any player who trashed something in the last 5 minutes (their recover carries over; see step 4).
2. Stop the server (Paper plugins need a restart; no /reload).
3. In `/plugins/`: delete `ExcelTrash-0.1.0.jar`, upload `ExcelTrash-0.2.0.jar`
   (sha256 in SHA256SUMS). Keep the `plugins/ExcelTrash/` folder (batches.dat + trash.log).
4. Start. Console should show:
   `ExcelTrash 0.2.0 enabled; undo window 5 minutes; chest mode on; close-action=return`
   batches.dat is migrated to v2 on first start (a pending 0.1.0 recover still works, tested).
   `plugins/ExcelTrash/config.yml` is created with `close-action: return`.
5. Quick check in game (Java and a Bedrock phone/controller if possible):
   - Compass > Trash Bin: title "Trash: put items here, press OK".
   - Put a dirt stack on the red side, OK, Recover: it comes back. /trash undo again: "Nothing to recover".
   - Look at your own chest, /trash chest: tap an item, it moves to the red side; Cancel: it goes back.
6. Same restart, optional: ExcelTips `trash` tip wording (see TIPS-NOTE.md). Website/Guide text: WEBSITE-DRAFT.md.

Nothing else changes: no new dependencies (GriefPrevention is a soft-depend), ExcelBedrockMenu untouched
(ExcelMenu renders the Bedrock form from the same menu file), ExcelAuditLog reads the new trash.log lines unchanged.

Decision for Mike: closing the screen without OK now GIVES ITEMS BACK (OK is the only delete). If you want the 0.1.0
"close = delete" feel, set `close-action: delete` in config.yml before the restart (tested both ways).

# ExcelTrash 0.2.0 -> 0.1.0 rollback

1. Stop the server.
2. In `/plugins/`: delete `ExcelTrash-0.2.0.jar`, put back `ExcelTrash-0.1.0.jar`
   (sha256 9d5c6a30d8c325e9e905c2c781ce41817209262f661ba281e9eeca6e4be6570f, copy in /workspace/batch1023/upload/).
3. Start. Console: `ExcelTrash 0.1.0 enabled; undo window 5 minutes`.

Effect: 0.1.0 reads the v2 batches.dat and treats every 0.2.0 batch as expired, so a recover from the last 5
minutes before the rollback is LOST, never duplicated (tested on the rig: /trash undo says "Nothing to recover").
config.yml is ignored by 0.1.0 (can stay). trash.log keeps going. The compass button keeps working (/trash).
If you also undid the ExcelTips wording change, put the 0.1.0 wording back.

## Artifacts
```
a7c00ba76e8db0e521992097159811b9456dd1b6bd4f509d96559cf551abeb1e  ExcelTrash-0.2.0.jar
9846f8b33f9979660ea32647d57e1c5784e3ab33cca0e7ceecbabe7896e672d7  ExcelTrash-0.2.0-source.zip
```
