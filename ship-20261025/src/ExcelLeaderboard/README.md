# ExcelLeaderboard 1.3.0

One `/leaderboard` command shows the boards three ways, depending on the
player:

- **Bedrock players** (through Geyser/Floodgate) get native form menus:
  one compact button per board ("Top Miner — Name · 12,340"), and a board
  form with the full top 10 plus the viewer's own rank pinned at the
  bottom. Buttons: "← All boards" and "Close".
- **Java players** get a clickable chat menu: one line per board, click
  to open that board (`/leaderboard board <board>`), with a clickable
  "« Back to boards" line.
- **Chest menu fallback**: if a form cannot be sent (or forms are off),
  the player gets the chest menu instead. Its main screen uses Mike's
  spacing rule: a 54-slot chest with the nine boards in slots
  10, 12, 14, 16, 19, 21, 23, 25, 31 (a blank slot between every icon,
  the 9th board centered).

Categories: Top Miner, Top Builder, Most Playtime, Top Slayer, Top
Traveler, Most Deaths (vanilla statistics, offline players included),
plus Richest (Vault), mcMMO Power, Jobs Levels, Elite Coins, and Land
Claimed (GriefPrevention) behind soft dependencies that hide a category
when the exact installed plugin's API does not verify.

Land Claimed (1.3.0): ranked by blocks claimed (the summed area of a
player's top-level GriefPrevention claims in claims-enabled worlds;
subdivisions never double-count). Every row shows a second number, the
blocks that player can still claim (accrued + bonus - claimed, clamped
at zero for display): chat/form rows read "#1 Name · 1,234 · 876 left",
the viewer's own rank line carries the same pair, and the chest cards
show it in the lore line. Display decision: ONE claims board, not two:
claimed is the ranking, still-claimable rides along as the row detail.
The chest fallback's main grid still shows at most nine boards, so with
every board available Land Claimed (eleventh in menu order) appears in
the chat and form presentations and not the fallback grid.

Build Mode rule (Mike, Oct 14, 2026): blocks broken in an ExcelBuildMode
session do not count toward Top Miner (the plugin watches ExcelBuildMode's
active-index and keeps a per-player adjustment ledger); blocks placed do
count toward Top Builder.

Build: ./build.sh (compiles against the exact Paper 26.2-123 API, runs
the test suites, emits target/ExcelLeaderboard-1.3.0.jar).

## Version history

- 1.3.0 (Oct 18, 2026): Land Claimed board (WRV-080, Mike's request).
  New soft dependency on GriefPrevention, reached reflectively like the
  other boards: GriefPrevention.instance -> dataStore -> getPlayerData
  for every known player (offline included; GP loads from its flatfile
  store), with per-claim world gating via GP's own
  claimsEnabledForWorld. Signatures verified against the installed
  GriefPrevention 16.18.7 jar by javap and guarded at build time by
  LiveApiSignatureTests (now 15 assertions). New pure core
  ClaimBlocksMath + ClaimBlocksMathTests (18 assertions over fake-GP
  fixtures: multi-world claims, disabled-world claim excluded,
  subdivisions excluded, bonus blocks, zero claims, brand-new player,
  over-drawn) and ClaimsPresentationTests (13) pinning the exact row
  strings. Snapshot gained a per-category details map; boards without
  details render byte-identically to 1.2. Full suite: 188 assertions,
  all passing; rebuild reproduces the jar SHA-256 exactly; scratch-rig
  boot (Paper 26.2-123 + GriefPrevention 16.18.7 with real GP data +
  ExcelLeaderboard 1.3.0) clean, zero SEVERE, probe line "Land Claimed
  category available (GriefPrevention)" at first refresh. Status: built,
  tested, rig-booted. NOT installed live yet.

- 1.2 (Oct 15, 2026): three presentations. Bedrock forms copied from
  ExcelShops 1.1's proven pattern: 100% reflection against the
  Geyser/Floodgate classloaders (no compile-time Geyser dependency),
  Bedrock detection via GeyserApi.connectionByUuid with a Floodgate
  isFloodgatePlayer fallback, SimpleForm via org.geysermc.cumulus, a
  one-shot response gate, and callbacks that hop to the main thread
  before touching plugin state. If a form send fails, that player falls
  back to the chest menu. Java players get the new clickable chat menu
  as the default (set `bedrock-forms: false` in config.yml to turn
  Bedrock forms off; Bedrock players then get the chest fallback).
  Chest main menu respaced to the 54-slot layout above, driven by one
  slot constant shared by the opener and the click handler. New test
  suites: LayoutTests (42), ChatMenuTests (31), FormContentTests (28).
  Status: built and automated-tested locally; local test-server boot
  clean. NOT yet live. Bedrock form rendering on a real phone and Java
  chat clicks in game are still unchecked.
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
- Geyser-Spigot and Floodgate: optional. When both are installed,
  Bedrock players get the native form menus described above. Without
  them, Bedrock players get the chest fallback menu. No board depends
  on them.
- ExcelBuildMode: optional companion. While a player is in a Build Mode
  session, blocks they break do not count toward Top Miner; blocks they
  place still count toward Builder.

Each optional board is probed against the actually installed plugin at
runtime. If a plugin is missing or its API does not match, that board
simply does not appear in the menu. Nothing else is affected.

## Test status (1.2, Oct 15, 2026)

Automated suites, all passing: BoardMathTests 25, LedgerTests 10,
VaultModeTests 6, LiveApiSignatureTests 5, LayoutTests 42,
ChatMenuTests 31, FormContentTests 28. Local test-server boot: clean
enable as v1.2, no ExcelLeaderboard errors (that local runtime has no
Vault/mcMMO/Jobs/EliteMobs, so the board verdict lines do not appear
there; they were proven live under 1.1). Not yet proven: a real Bedrock
form rendering on a phone, and Java chat-menu clicks in game.
