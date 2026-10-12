# Player Map deployment: October 12, 2026

## Installed artifact and provenance

**WRVPlayerMap 0.2.0 is installed on Wink Reyes Valley.** The approved artifact was added during a normal stop/start on October 12, 2026. Its tested source remains [`a92ac702452a684000f860e1667e384d763961e2`](https://github.com/ElMikeExcel/wrv-plugins/commit/a92ac702452a684000f860e1667e384d763961e2), the ordinary-map revision in [draft PR #3](https://github.com/ElMikeExcel/wrv-plugins/pull/3).

- Artifact: `WRVPlayerMap-0.2.0.jar`
- Size: 35,580 bytes
- SHA-256 verified on host: `382e40bdb3eae37d2c5dc6b6344d57749701f4b589bcdfd77919aac2750b9dc9`
- Loaded version: `0.2.0`

This update records deployment facts only. It does not change code or binaries, merge the draft review, or install the separate shop candidate. The existing exact-Paper and automated test evidence remains build evidence, not human client acceptance.

## Completed server checks

Times below are UTC, with Central daylight time (America/Chicago, UTC−5) in parentheses.

- **14:54:53 (09:54:53 CDT):** normal server start initiated.
- **14:55:07 (09:55:07 CDT):** clean plugin enable. Its log reported: “Native player-map enhancement enabled. Only newly activated ordinary maps and their unlocked zoom descendants are enrolled.”
- **14:55:10 (09:55:10 CDT):** startup reached `Done`; the server was Online with 28 plugins loaded.
- Runtime version `0.2.0` and `/playermap` command registration were verified. Command registration is not a visual map test.
- **14:57:27 (09:57:27 CDT):** generated default configuration checked, including `update-interval-ticks: 20` (one second at normal tick rate).
- No map-specific warning, linkage failure, or registry error was observed. Before human map enrollment, no `maps.yml` registry had been created, consistent with the implementation's lazy creation. This is not a successful enrollment test.
- The existing XP pair also reloaded successfully; its weekday vanilla bridge factor was rechecked at `1.0` on this restart.

No earlier WRVPlayerMap installation was present in the preflight. Existing world/map data was not overwritten or migrated, and existing maps were not bulk-enrolled by the installation.

## Human report: basic checks passed, October 12

A player report relayed by the server owner on October 12, 2026 confirms:

- **Test 1: passed, player-reported.** A map was created and was working.
- **Test 2: passed, player-reported.** The player's own nameplate followed movement, and local terrain and the surrounding area appeared on the map.
- **Test 3: blocked during the test.** No other players were online, so other-player labels and off-map edge arrows were not tested. This check awaits a second player.

This is human-reported evidence, not an independently observed client session. The report did not specify Java or Bedrock, device, or client version. It does **not** establish cross-platform acceptance or a full gameplay pass.

## Remaining acceptance checks

- Complete the two-player test for other-player labels, positions, and distant edge arrows.
- Confirm readable names/arrows and holding behavior on identified Java and Bedrock clients (main hand on phones).
- Share and copy it; zoom with paper; lock with glass; verify the locked copy stays plain and native exploration remains correct.
- Check portals/dimensions, overlapping players, crowding, long names, visibility rules, and reconnect/restart behavior.
- Check normal inventory/cartography clicks, existing treasure/art maps, and the unlocked held/framed-copy caveat.
- Observe actual Geyser/Floodgate compatibility and family-session performance.

Normal instructions and limitations are in the [Player Map README](README.md) and the [existing server website](https://elmikeexcel.github.io/winkreyesvalley/#panel-player-map). Update this record when actual acceptance evidence arrives; startup success alone must not be presented as a human pass.
