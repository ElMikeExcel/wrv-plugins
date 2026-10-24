# ExcelLeaderboard 1.3.1 (staged, NOT installed)

Rank: before-next-seal (server load grows with every player who has ever joined).

## What changes
- 1.3.0 asks Paper for each offline player's statistics once per block and item material.
  On Paper 26.x every such call re-reads and re-parses that player's stats JSON from disk.
- 1.3.1 caches the six vanilla totals per offline player, keyed on the stats file's mtime and size.
  Online players are never cached. A missing file or folder means no caching (1.3.0 behavior).
- The stats folder is found from the main world folder, walking up to three parents.
  On 26.x the world folder is world/dimensions/minecraft/overworld and stats live in world/players/stats.
  The first 1.3.1 rig run showed NO gain because of this layout; it was fixed and re-proved (below).
- SoftAdapters probes also catch LinkageError. Before, a NoClassDefFoundError inside a probe
  (seen on the rig: Jobs present without Vault) killed that refresh and logged a stack trace.
- New core/VanillaStatsCache + StatsCacheTests (13 assertions). plugin.yml version 1.3.1.

## Artifact
- build/ExcelLeaderboard-1.3.1.jar  sha256 3b2c1f1c11faf9e1bdadd77fc25e5f9c905f3eadb2a472d01da6919e978060ee  (57897 B, reproducible: two builds identical)
- Diff: ExcelLeaderboard-1.3.1.diff (against the Drive source 1.3.0). No names in it.
- Hygiene: src/test/.../ClaimsPresentationTests.java (pre-existing in 1.3.0) used family first names as sample
  players. fix/ uses neutral names (original in pre-scrub-bak/); excluded from the diff, ship it whole from fix/.
  Test-only, so the jar is unchanged; all suites still pass (fix-tests-round2.log).

## Checks
1. Build + tests: fix-tests.log, all suites pass incl. StatsCacheTests 13/13 (1.3.0 baseline all pass).
   Rig (box rig port 25911, Paper 26.3, 12 seeded offline players, cache-seconds 10, 60 s window):
   | build | read syscalls | bytes read | CPU |
   |---|---|---|---|
   | no plugin (baseline) | 1,396 to 1,406 | 0.77 to 0.95 MB | 1.54 to 1.89 s |
   | 1.3.0 | 575,541 | 88.7 MB | 11.24 s |
   | 1.3.1 final | 1,588 | 0.98 MB | 1.87 s |
   Board output (miner, playtime, traveler, deaths) identical between 1.3.0 and 1.3.1.
   Log: "offline stats cache reads .../world/players/stats"; no exception.
   Logs: ../tests/LB.log, ../rig/logs-lb-1.3.0.log, ../rig/logs-lb-1.3.1-final.log. Rig stopped, plugin removed.
2. Review pass: diff read line by line. Cache methods are synchronized (async refresh); one slot per
   ever-joined player (same order as the existing loop); a wrong or missing folder only disables caching.
   Residual, unchanged: plugin APIs (Vault, mcMMO, Jobs, GriefPrevention) are still called from the async refresh.

## Install record (when Mike approves; not done)
- Confirm the live version first (expected 1.3.0).
- Move the live ExcelLeaderboard-1.3.0.jar to ExcelLeaderboard-1.3.0.jar.bak-<date> (keep it; never
  overwrite an existing .bak, add -2 if the name exists). Copy in 1.3.1, verify sha256, restart.
- No config change. After restart check the log line "offline stats cache reads <world>/players/stats".
- Rollback: restore the .bak jar.
