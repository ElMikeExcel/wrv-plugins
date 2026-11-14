# RECEIPTS - P-SEALFREEZE-1 filing packet (Nov 14, 2026)

Desk-side evidence only. Every hash below was re-measured in this
filing pass (sha256sum + wc -c, run 329 worker). No live contact,
no PC route, no writes to any source file. Live-state values are
NOT in this file: filing-day live premises live only in
G1-CHECKLIST.md, blank until the window fills them.

## Candidate artifact (the freeze enforcer)

- ExcelSealFreeze-0.1.0.jar
  Path: ~/workspace/sealfreeze-20261114/ExcelSealFreeze-0.1.0.jar
  Size: 20456 B
  SHA-256: d3610948d7a3d1bcdd150d1a61c9e27743d7dfdb7cb86cad23a434383c7ee925
  Build ID: EXC-SEALFREEZE-0.1.0-WRV-20261114-01
  Deterministic: two consecutive build.sh runs byte-identical
  (BUILD-RECORD.md). Tests: 25/25 PASS (TEST-RESULTS.txt,
  1fc760ca722fc27ebe162de8d4cc24393021a41904c1eb86ec8984cb8eb4148f).
  Build folder SHA256SUMS file:
  a5f434adea610f7b15e1db80d97f07760b4b0aeb5ce80e05c626101e2adcbc65
  (its jar line matches the jar hash above exactly).

## Desk source files (inputs to this packet)

- sealfreeze-prep-20261114/PACKET-DRAFT.md - 8710 B -
  65888dea6bbb35ac5c6caad7ec7257de4d683a1dd5e4f6e3d807855d4587e964
- sealfreeze-prep-20261114/FREEZE-DESIGN.md - 8196 B -
  4a7c22a6c5a862cbafb68a14b97d4e4d77a250eaf13523a1dc490ad7b540d69e
- sealfreeze-prep-20261114/RECEIPTS.md - 4747 B -
  0fa81d94f5f30423bf2de46ff6f1013f6a8163979566dfb9bc2eb3345edfb6e7
- sealfreeze-20261114/WIPE-PACKET.md - 9729 B -
  14674e917a433b1e7ea6e731a9e16873d4978130845a96893f572414e149b1fa
- sealfreeze-20261114/BUILD-RECORD.md - 4326 B -
  0ef53b7bb2bbc57f105def139cea926cb12493cbafb9dfecadee1e2781f67d5b
- sealfreeze-20261114/FREEZE-SCOPING.md - 7853 B -
  6d92fe02f2cf04921f7985257947b5e63c17ca55f964d2e1d5ea35502c815379
- sealfreeze-20261114/RIG-ACCEPTANCE-PLAN.md - 5444 B -
  b323dd68c125af30f614919a7250072d8296fe120e5e2cd00968f4fe20ee4ccf

## Desk DB evidence base (NOT a live premise)

- Copy: ~/workspace/g4-pull-bm326/em_player_data.db
  Size: 28672 B
  SHA-256: b9d251a0e61e5ecc7d1bea552f1fa86ffbae1b4941826c56df9f5c8074b4da4f
  Origin: PC-route pull 2026-11-14 00:07-00:08 CST (G4 pull).
  Status in this packet: desk evidence base for schema + snapshot
  values ONLY. It is never a filing-day premise (G1 rule). The
  window re-pulls fresh and fills G1-CHECKLIST.md.

## Schema of record (from the desk evidence base, re-dumped in prep)

- Exactly 3 tables. PlayerData: 11 rows, 31 columns.
  PlayerClassProgress: 0 rows, 5 columns.
  PlayerClassProfile: 11 rows, 5 columns.
- Wipe set (18 columns, zeroed): CurrencyV2, CurrencyCents, Score,
  Kills, HighestLevelKilled, QuestsCompleted, DungeonsCompleted,
  SkillXP_ARMOR, SkillXP_SWORDS, SkillXP_AXES, SkillXP_BOWS,
  SkillXP_CROSSBOWS, SkillXP_TRIDENTS, SkillXP_HOES, SkillXP_MACES,
  SkillXP_SPEARS, SkillXP_STAVES, SkillXP_WANDS.
- Preserve set (8 columns, never written): PlayerUUID, DisplayName,
  Deaths, GamblingDebt, GamblingDebtCents, BackTeleportLocation,
  UseBookMenus, DismissEMStatusScreenMessage.
- Filing-day verification set (5 columns, checked, never written
  by this packet): QuestStatus, PlayerQuestCooldowns,
  DungeonBossLockouts, QuestLockouts (desk snapshot: NULL x11
  each), SkillBonusSelections (desk snapshot: '{}' x11).

## Pre-wipe snapshot in the desk evidence base (filing day re-bases)

Per player (DisplayName, HighestLevelKilled, Score, Kills,
CurrencyV2, CurrencyCents, Deaths):
- GumGumJetPistol: 23, 858, 75, 7.0, 700, 3
- Jester_Turtle: 22, 3674, 220, 0.0, 0, 1
- .coolgirlkail: 16, -988, 10, 0.0, 0, 11
- .MikeExcel: 12, 86, 9, 0.0, 0, 0
- FADEOUTMinecraft: 2, 4, 1, 0.0, 0, 0
- oklajoma: 2, 4, 1, 0.0, 0, 0
- .FoamyDragon9514: 0, 0, 0, 0.0, 0, 0
- .KWSunflower: 0, 0, 0, 0.0, 0, 0
- .RylieDSunflower: 0, 0, 0, 0.0, 0, 0
- MikeExcel: 0, 0, 0, 0.0, 0, 0
- Racimbio: 0, -300, 0, 0.0, 0, 3
Column sums: Score 3338; Kills 316; HighestLevelKilled 77;
Deaths 18; CurrencyV2 7.0; CurrencyCents 700; GamblingDebt 0.0;
GamblingDebtCents 0; QuestsCompleted 0; DungeonsCompleted 0.
SkillXP sums: ARMOR 4121, SWORDS 2340, AXES 4, BOWS 1549,
CROSSBOWS 0, TRIDENTS 43, HOES 0, MACES 25, SPEARS 0, STAVES 0,
WANDS 0.

## Scope-delta reconciliation (on the record)

- The build lane's WIPE-PACKET.md UPDATE lists 16 columns and its
  header says 17; both omit QuestsCompleted and
  DungeonsCompleted. Desk snapshot: both are 0 on all 11 rows.
  This packet's UPDATE carries the prep lane's full 18-column
  set (Mike's scope of record in PACKET-DRAFT.md) so the
  post-wipe state of those two columns is proven by the write
  and by V2, not assumed. The enforcer's clamp set is 16 columns
  (the 18 minus QuestsCompleted / DungeonsCompleted, which do
  not accrue through play); that is the built, tested behavior
  and is unchanged.
- WIPE-PACKET.md asserts a 4-of-5 seal state at boot. The prep
  evidence base does NOT prove the live seal count (state.yml
  was not in the G4 pull). Per G1, this packet assumes no count:
  the count is a filing-day pull (G1-CHECKLIST.md slot 2) and
  boot verify compares against that pulled value.

## Filing-day G1 receipts (run 330, Nov 14, 2026) - NEW

Probe (PC route, retry pattern): attempt 1 PASS
2026-11-14 02:42:32 CST - ssh mike-pc hostname ->
DESKTOP-IL05DQR; attempts 2 (02:42:53) + 3 (02:43:14) also
PASS. Last successful probe: 2026-11-14 02:43:14 CST.
Pulls completed 02:44-02:45 CST on the same route. No
fallback used. No wipe, no install, no live write: rclone
copyto / lsf read-only pulls via the PC only. Live server
OFFLINE for the nightly maintenance window at pull time.
PC temp staging deleted after copy-down (Test-Path False).

Pulled copies (folder g1-pull-20261114/ in this folder):
- player_data.db (live plugins/EliteMobs/data/player_data.db)
  28672 B -
  b9d251a0e61e5ecc7d1bea552f1fa86ffbae1b4941826c56df9f5c8074b4da4f
  NOTE: byte-identical to the G4 desk copy (00:07 CST pull).
- guildgate_state.yml (live
  plugins/ExcelGuildGate/state.yml) - 1015 B -
  79e4470255edc5522db555e6ee935c301a5b74776fed45f3aa1f45220879b4c0
- em_EconomySettings.yml (live
  plugins/EliteMobs/EconomySettings.yml) - 9545 B -
  dbec1c1e80557898eb8c70186be62fec0dcd536da9b58daf4c67dc0420ffba93
- em_config.yml (live plugins/EliteMobs/config.yml) -
  8419 B -
  8e4d72bfaaa12f9f4d4a04866317eee4077382cccc28fb1b11a8344ac319ff26
- listing_plugins.txt (rclone lsf plugins) - 16709 B -
  75c741afa4cc79c4712d876827a49a2190ae442b9c3b58942ad31b88a16484a7
- listing_em_data.txt (rclone lsf EliteMobs/data) - 45 B -
  9622e636b16492b36bfb693c9a659bea275ac9869cb4b95c81994fe320
- discovery.txt (full listing session capture) - 21154 B -
  8482afe4493e4ae3c52a46897c61769490a8acc40773fd0607a1edc3f811367b

DB script output (python3 sqlite3, local pulled copy):
3 tables (PlayerClassProfile, PlayerClassProgress,
PlayerData); PlayerData 31 cols / 11 rows;
PlayerClassProgress 5 cols / 0 rows; PlayerClassProfile
5 cols / 11 rows; PRAGMA integrity_check -> ok;
QuestStatus / PlayerQuestCooldowns / DungeonBossLockouts /
QuestLockouts NULL x11 each; SkillBonusSelections '{}' x11;
SUM(Deaths)=18, SUM(GamblingDebt)=0.0,
SUM(GamblingDebtCents)=0. Top HLK: GumGumJetPistol 23,
Jester_Turtle 22, .coolgirlkail 16, .MikeExcel 12,
FADEOUTMinecraft 2, oklajoma 2 - matches the desk snapshot.

GuildGate script output (pulled state.yml): unlocked:
false; broken 4 of 5 - ender_dragon, wither,
elder_guardian, raid true; warden false.

Economy script output (grep, pulled files):
EconomySettings.yml line 13 useVault: true; line 15
enableCurrencyShower: true (filing-day value for D50).
em_config.yml: 0 hits for either key.

Jar census (listing_plugins.txt): ExcelSealFreeze hits 0 -
jar ABSENT, data folder ABSENT (rclone lsf
plugins/ExcelSealFreeze -> directory not found, exit 3,
02:44:49 CST). Live EliteMobs jar: EliteMobs-10.9.10.jar.
Live ExcelGuildGate jar: ExcelGuildGate-0.3.4.jar.
Data-dir listing: only player_data.db +
resource_pack_status.yml - no -wal / -shm at pull time.

Slots filled at filing: G1-CHECKLIST.md slots 1, 2, 3
complete (all premise fields) + new slot 6 (D50 rider).
Not filled, by design: slot 4 emptiness pair + marker
fields and slot 5 backup/prewipe/post-swap fields - all
window-only steps; slot 4 list checks recorded in the
checklist as NOT RUN AT FILING (server offline; no RCON
list against a stopped server). Window re-verifies
slots 1-3 + WAL at its own stop before any write.
