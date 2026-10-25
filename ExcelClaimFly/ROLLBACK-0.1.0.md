# ROLLBACK: ExcelClaimFly 0.1.0 (Build Flight pass)

Roll back in the reverse order of install (D, C, B, A). Each part can be undone alone. Parts B, C and D need no restart.

## Part D: What's New
Restore plugins/ExcelWhatsNew/changelog.yml from changelog.yml.bak-20261025-buildflight, then `whatsnew reload`.
Expect the previous count and newest id (3 entries, newest 2026-10-25-bedrock-port, if the port packet was the base).

## Part C: tip
Restore plugins/ExcelTips/tips.yml from tips.yml.bak-20261025-buildflight, then `tips reload` (expect 23 tips on the port-warning base).
Quick alternative: set `enabled: false` on the build-flight block, then `tips reload`.

## Part B: shop button
Restore both menu files from their .bak-20261025-buildflight copies, then `excelmenu reload`.
Check the self-check lists spawnshop and spawnshop_travel_animals by name, and that Travel & Animals shows 5 products again.
Do this BEFORE Part A, or the button would run a command that no longer exists.

## Part A: plugin
Soft off, no restart: in plugins/ExcelClaimFly/config.yml set `enabled: false`, then `buildfly reload`. New purchases stop.
Only combat: set `combat.pause-flight: false`, then `buildfly reload`.
Full removal:
1. `list` -> 0 players (two reads). Do Part B rollback first.
2. Rename plugins/ExcelClaimFly-0.1.0.jar to ExcelClaimFly-0.1.0.jar.disabled-YYYYMMDD (never delete).
3. Panel Shut Down, then Start. Leave the plugins/ExcelClaimFly folder (state.yml, purchases.log) in place as the record.
4. Open passes: `lp search excelclaimfly.pass` lists holders. The node does nothing without the plugin and runs out by itself
   (30 to 120 min). Remove early with `lp user <uuid> permission unsettemp excelclaimfly.pass` (Bedrock names need the floodgate UUID).
   If a pass is cut short, refund in-game money from purchases.log (`eco give <player> 1000` per unused pass). Keep player names out of the records.
5. On shutdown the plugin turns flight off for everyone online. A player who logged off while flying should rejoin in survival with flight off
   (vanilla resets may-fly for survival on join). This was NOT tested without the plugin. If anyone can still fly after removal: `fly <player> off` (EssentialsX).
