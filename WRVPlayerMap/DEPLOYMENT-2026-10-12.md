# Player Map deployment: October 12, 2026

## Current installation: ExcelPlayerMap 0.2.1

**ExcelPlayerMap 0.2.1 is installed**, replacing WRVPlayerMap 0.2.0 in a normal restart on October 12, 2026. Tested source and migration evidence remain pinned at [`2043759e2ddc31af7d73568a83e3635b6811a192`](https://github.com/ElMikeExcel/wrv-plugins/commit/2043759e2ddc31af7d73568a83e3635b6811a192).

- Artifact: `ExcelPlayerMap-0.2.1.jar`
- SHA-256 verified on host: `b0194ef2612f3bddd6ca698dab8462a518677f9ab5bb3191e858e75c6b570527`
- Loaded plugin/version: `ExcelPlayerMap 0.2.1`

Times are UTC, followed by America/Chicago daylight time (UTC−5).

- **17:10:29 (12:10:29 CDT):** normal server start.
- **17:10:44 (12:10:44 CDT):** safe legacy-storage migration logged. The original WRVPlayerMap folder was retained unchanged; the copied registry text matched the original and preserved enrolled native map IDs. The destination migration marker was present.
- **17:10:47 (12:10:47 CDT):** startup reached `Done`.
- **17:11:22 (12:11:22 CDT):** ExcelPlayerMap 0.2.1 runtime and all 28 plugins verified; server Online.
- **17:14:03 (12:14:03 CDT):** final checks completed. The previous `update-interval-ticks: 20` setting was preserved, and no new map/XP errors were observed.

The installed update uses compact five-pixel names, glyph-shaped shadows and no solid label/arrow/footer backing rectangles. `/playermap` and existing permissions remain compatible; `/excelplayermap` is an alias. Existing enrolled map items and identities are preserved; creating replacement maps is unnecessary.

**Human acceptance remains pending:** reopen an existing enrolled map and check compact names/clear terrain on an identified Java or Bedrock client. The migration checks establish preserved storage and successful startup, not client rendering. Earlier human reports below concern 0.2.0; they do not establish a pass for 0.2.1. Automated and isolated exact-Paper evidence remains build evidence. This documentation update changes no code/binaries and does not merge the draft PR.

## Earlier WRVPlayerMap 0.2.0 installation

**WRVPlayerMap 0.2.0 was installed earlier on Wink Reyes Valley, then superseded by 0.2.1 above.** The approved artifact was added during a normal stop/start on October 12, 2026. Its tested source remains [`a92ac702452a684000f860e1667e384d763961e2`](https://github.com/ElMikeExcel/wrv-plugins/commit/a92ac702452a684000f860e1667e384d763961e2), the ordinary-map revision in [draft PR #3](https://github.com/ElMikeExcel/wrv-plugins/pull/3).

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

## Earlier 0.2.0 human reports, October 12

A player report relayed by the server owner on October 12, 2026 confirms:

- **Test 1: passed, player-reported.** A map was created and was working.
- **Test 2: passed, player-reported.** The player's own nameplate followed movement, and local terrain and the surrounding area appeared on the map.
- **Test 3: blocked during the test.** No other players were online, so other-player labels and off-map edge arrows were not tested. This check awaits a second player.

A later screenshot showed two visible player names/markers and terrain, but also oversized labels and solid dark backing rectangles. That visual defect motivated the installed 0.2.1 update above. Off-map edge arrows still lack a human check.

This is human-reported evidence, not an independently observed client session. The report did not specify Java or Bedrock, device, or client version. It does **not** establish cross-platform acceptance or a full gameplay pass.

## Remaining acceptance checks

- Check the new compact labels on existing enrolled maps, including two-player positions and distant edge arrows.
- Confirm readable names/arrows and holding behavior on identified Java and Bedrock clients (main hand on phones).
- Share and copy it; zoom with paper; lock with glass; verify the locked copy stays plain and native exploration remains correct.
- Check portals/dimensions, overlapping players, crowding, long names, visibility rules, and reconnect/restart behavior.
- Check normal inventory/cartography clicks, existing treasure/art maps, and the unlocked held/framed-copy caveat.
- Observe actual Geyser/Floodgate compatibility and family-session performance.

Normal instructions and limitations are in the [Player Map README](README.md) and the [existing server website](https://elmikeexcel.github.io/winkreyesvalley/#panel-player-map). Update this record when actual acceptance evidence arrives; startup success alone must not be presented as a human pass.
