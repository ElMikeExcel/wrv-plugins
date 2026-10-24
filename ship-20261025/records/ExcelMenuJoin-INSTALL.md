# ExcelMenuJoin 1.4.2 (staged, NOT installed)

Rank: before-next-seal (player-facing: the menu compass ignores taps in the air).

## What changes
- onCompassUse checks `useItemInHand() == DENY` instead of `isCancelled()`.
  Bukkit fires every RIGHT_CLICK_AIR with the interacted-block result already DENY,
  so `isCancelled()` is true for every air tap and 1.3.0 to 1.4.1 dropped them.
- ExcelShops' compass shortcut cancels with setCancelled(true), which also denies the
  item use, so shop-sign taps are still respected (checked in WRVShops 1.3.1 source).
- New pure helper CompassTap + CompassTapTest. plugin.yml version 1.4.2. Nothing else.

## Artifact
- build/ExcelMenuJoin-1.4.2.jar  sha256 877bf1956ed1c4178b7d390df4719eb9f326305b640ad8656bc5c1637229d976  (9,704 B, reproducible: two builds identical)
- Diff: ExcelMenuJoin-1.4.2.diff (against the Drive source ExcelMenuJoin-1.4.1-source.zip)

## Checks
1. Build + tests: fix-tests.log, 21 passed 0 failed (1.4.1 baseline 19/19).
   Rig (box rig port 25911, rig-only /menu probe plugin counting dispatches):
   - 1.4.1: join auto-open 1, AIR taps x3 -> 0 menu opens.
   - 1.4.2: join auto-open 1, AIR taps x3 -> 3 menu opens.
   - Block tap on grass: 0 in both builds. The test bot sends no position/look, so this
     row is a rig limit, not evidence either way; the block path is unchanged code.
   Logs: ../tests/MJ.log, ../rig/logs-mj1.4.1.log, ../rig/logs-mj1.4.2.log. Rig stopped,
   MenuJoin and the probe removed from the rig afterwards.
2. Review pass: diff read line by line; ExcelShops interaction order checked
   (compassShortcut LOWEST setCancelled -> item DENY -> menu stays shut).

## Install record (when Mike approves; not done)
- Confirm which MenuJoin is live first (site text says 1.4.0; 1.4.1 may be installed).
- Move the live ExcelMenuJoin-1.4.x.jar to ExcelMenuJoin-1.4.x.jar.bak-<date> (keep it;
  never overwrite an existing .bak). Copy in ExcelMenuJoin-1.4.2.jar, verify sha256, restart.
- No config change. Rollback: restore the .bak jar.
