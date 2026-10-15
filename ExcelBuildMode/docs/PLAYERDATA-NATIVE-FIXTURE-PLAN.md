# Targeted exact-Paper native save-failure extension

Plan only. These scenarios have not run. Do not edit or start a live server; the parent coordinates the one-JVM slot and the existing bounded offline driver. All test UUIDs are invented fixture identities. Do not modify the main probe until this extension is assigned.

## Common fixture

Use the final candidate helper and Paper 26.2 build123/revision5001879 in a disposable copied world. Retain stdin, a bounded timeout, the owned process ID, `-XX:-UsePerfData`, fixed low heap/CPU limits and loopback-only binding. Provision the test-only recovery/index directories and private cohort/eligibility replicas for the fake test players; no real user UUID/default permission grants.

Resolve playerdata through the runtime's actual `playerIo.getPlayerDir()` accessor. Seed distinguishable inventory/armor/offhand/ender contents, XP and unrelated PDC. Save once with native saving enabled and verify matching UUID/unmarked Survival. Use no real Jobs/mcMMO/EliteMobs coverage claims: the existing handshake fixtures only test helper behavior.

Keep the original `SpigotConfig.disablePlayerDataSaving` value and restore it in `finally`. Any file fault is confined to the disposable server's verified native player directory and restored in the same controlled main-thread test step. No permission/security setting changes or production filesystem targets are needed.

## A. Native saving explicitly disabled before entry

1. Record the saved native file bytes and exact starting inventory/XP.
2. Set `SpigotConfig.disablePlayerDataSaving=true` in the fixture, then invoke Build entry.
3. The actual `CraftPlayer.saveData()` should return normally through the native early-return branch; its lack of exception must not acknowledge the active marker.
4. Assert the saved file is unchanged, no Creative transition occurred, no Build inventory clearing occurred, recovery/index remain active after their preparation, and the authoritative reward guard is retained.
5. Restore the native saving flag. Complete the documented recovery path; assert exact Survival state, own marker absence acknowledged in native data, completed records and guard release.

The helper may immediately attempt safe recovery after entry failure; count the native save calls and assert the safety outcome rather than requiring one specific user-facing phase name.

## B. Native saving disabled during restored-marker acknowledgment

1. Enter Build successfully with verified active marker and introduce distinguishable Creative contents.
2. Disable native saving, then request normal exit.
3. Inventory can be restored in memory, but native data still carries the active phase. Require failure of the exact `v1:restored:<same session>` acknowledgment before journal/index completion or guard release.
4. Verify both native marker and immutable journal, no Creative items became available, and new origins remain denied.
5. Re-enable saving and retry; the same Survival snapshot restores safely, the restored acknowledgment succeeds, completion follows, and only the own marker is cleared.

## C. Native saving disabled only for marker removal

Add a fixture-only per-save-call switch around the real `super.saveData()` invocation, toggling the native flag only on the clear-marker save call and restoring it immediately afterward. This exercises the real native early return, not just a mocked exception.

- Restored marker acknowledgment succeeds first; journal/index may then complete.
- Removal save returns void but leaves the native restored marker present.
- `forceVerifiedMarker(... expected=null, requireSurvival=true)` must fail; the current-lifetime guard must remain.
- Verify the loaded marker is restored by adapter cleanup and the player is held for the explicitly reviewed post-commit recovery policy. Do not treat this as permission to silently replay a completed snapshot or delete the remaining marker.

## D. Native save catches an actual filesystem failure

In a single controlled main-thread callback, temporarily move the disposable native player directory to a uniquely named sibling and put a regular file at its former directory path. Verify the path identity before this operation. This makes the real `Files.createTempFile(playerDir, ...)` fail inside `PlayerDataStorage.save()`, which catches/logs the exception and returns void. Restore the directory in `finally` before other test stages. Do not do this against shared cached or production storage.

Invoke the active-marker save/entry attempt during that controlled fault. Require the native `Failed to save player data` log and a normal return from `saveData`, followed by failed helper proof and no Creative mutation/guard release. After restoration, verify the original record is unchanged and recovery can follow its existing safe path. Abort the test if directory restoration cannot be verified; never continue with a guessed replacement path.

## Required artifact evidence

Save final helper/probe/runtime hashes, per-scenario assertion logs, native saving flag before/after, native file digests/marker observations, journal/index states, main-thread mutation ordering and clean process exit. This verifies the silent-save paths; it is not a real client test, power-loss test or proof against external concurrent filesystem writers.
