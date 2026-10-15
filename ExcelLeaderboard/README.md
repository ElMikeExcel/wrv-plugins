# ExcelLeaderboard 1.1

One `/leaderboard` command opens a WealthBoard-style menu: tap a category
for the top 10 plus your own rank. Categories: Top Miner, Top Builder,
Most Playtime, Top Slayer, Top Traveler, Most Deaths (vanilla statistics,
offline players included), plus Richest (Vault), mcMMO Power, Jobs Level,
and Elite Coins behind soft dependencies that hide a category when the
exact installed plugin's API does not verify.

Build Mode rule (Mike, Oct 14, 2026): blocks broken in an ExcelBuildMode
session do not count toward Top Miner (the plugin watches ExcelBuildMode's
active-index and keeps a per-player adjustment ledger); blocks placed do
count toward Top Builder.

Build: ./build.sh (compiles against the exact Paper 26.2-123 API, runs the
test suites, emits target/ExcelLeaderboard-1.1.jar).

## Version history

- 1.1 (Oct 14, 2026): probe fix. At 1.0's first live boot, the mcMMO Power
  and Jobs Levels categories hid themselves because the reflective probes
  named API entry points that do not exist in the installed builds
  (verified by javap on the actual jars: mcMMO 2.3.003's offline power
  read is ExperienceAPI.getPowerLevelOffline(UUID); Jobs 5.2.6.3 has no
  JobsAPI class, the path is Jobs.getPlayerManager() ->
  PlayerManager.getJobsPlayer(UUID) -> JobsPlayer.getTotalLevels()).
  1.1 targets the verified signatures, and a new LiveApiSignatureTests
  suite checks every probe target against the real installed jars at
  build time so this class of mistake fails the build instead of the
  boot. Jobs probe proven live against the real jars on a test server;
  mcMMO probe signature-proven against the real jar.
- 1.0 (Oct 14, 2026): first release, installed live the same evening.

## Dependencies (what this plugin relies on)

- Paper (or a compatible fork) 1.21+. Required.
- Vault: optional. Enables the Richest board (reads balances through
  Vault's economy service).
- mcMMO: optional. Enables the mcMMO Power board (total of a player's
  skill levels, read through mcMMO's API).
- Jobs Reborn: optional. Enables the Jobs Levels board (total job level
  across a player's jobs, read through the Jobs API).
- EliteMobs: optional. Enables the Elite Coins board (read through the
  EliteMobs Economy API).
- ExcelBuildMode: optional companion. While a player is in a Build Mode
  session, blocks they break do not count toward Top Miner; blocks they
  place still count toward Builder.

Each optional board is probed against the actually installed plugin at
runtime. If a plugin is missing or its API does not match, that board
simply does not appear in the menu. Nothing else is affected.
