# WINDOW RECORD - P-SEALFREEZE-1 (EM wipe + ExcelSealFreeze 0.1.0)
# Installer: wrv-install-failsafe, run scheduled 2026-11-14 04:03 CST
# Lane: PC LANE (probe PASS attempt 1: hostname DESKTOP-IL05DQR +
# rclone lsd shockbyte: -> "1. Michael's Server"; no fallback)

## Scan / gates at open
- Heading inventory: 217 headings; only real PENDING =
  EM wipe + ExcelSealFreeze 0.1.0 (P-SEALFREEZE-1). Other two
  PENDING headings are the template line and the section header.
- No foreign WINDOW-IN-PROGRESS.txt at scan. Own marker created
  2026-11-14 04:05:45 CST. Marker re-checked before Start
  04:16 CST: only own marker present.
- Server arrival state: OFFLINE (panel, 0/10; stopped
  2026-11-14 03:54:14 CST in the maintenance window;
  latest.log shutdown sequence complete). Slot 4 emptiness:
  server fully stopped = no players possible; RCON list does
  not exist against a stopped server (per filing G1 note).

## Step 0 - fresh re-pulls (PC route, 04:06 CST) - ALL EXACT vs filing
- player_data.db 28672 B / b9d251a0e61e5ecc7d1bea552f1fa86ffbae1b4941826c56df9f5c8074b4da4f
  schema 3 tables, PlayerData 31 cols / 11 rows, ClassProgress
  0 rows, ClassProfile 11 rows, integrity ok, verification set
  NULL x11 each, SkillBonusSelections '{}' x11, sums
  Deaths 18 / GamblingDebt 0.0 / GamblingDebtCents 0.
- ExcelGuildGate/state.yml 1015 B / 79e4470255edc5522db555e6ee935c301a5b74776fed45f3aa1f45220879b4c0
  unlocked: false; 4 of 5 broken (warden false).
- EconomySettings.yml 9545 B / dbec1c1e80557898eb8c70186be62fec0dcd536da9b58daf4c67dc0420ffba93
  useVault: true (line 13); enableCurrencyShower: true (line 15).
- EliteMobs/config.yml 8419 B / 8e4d72bfaaa12f9f4d4a04866317eee4077382cccc28fb1b11a8344ac319ff26
  0 hits for either rider key.
- Plugins listing: ExcelSealFreeze 0 hits (jar + folder ABSENT,
  lsf exit 3); live jars EliteMobs-10.9.10.jar,
  ExcelGuildGate-0.3.4.jar.
- EM data dir: player_data.db + resource_pack_status.yml only;
  no -wal / -shm.
- Candidate jar local: 20456 B / d3610948d7a3d1bcdd150d1a61c9e27743d7dfdb7cb86cad23a434383c7ee925 (matches packet).

## Step 1 - window backup (panel, 04:08-04:12 CST)
- Prunee (cap-at-6): "TravelGreen pre-install 1113"
  (oldest manual; Daily newest + two newest manuals protected).
- Created + verified completed: "SealFreeze pre-install 1114"
  11/14/2026 4:08 AM, 7.5 GB, Manual. Total backups after: 6.

## Step 2 - stopped-server DB backup (04:13 CST)
- Stopped data-dir listing before copy: player_data.db +
  resource_pack_status.yml only; no -wal / -shm.
- Server-side copy: player_data.db.prewipe-20261114-0413
- Pulled prewipe copy: 28672 B / b9d251a0... (full hash above)
- Fresh stopped live pull: 28672 B / b9d251a0... - EQUAL.

## Step 3 - pre-wipe snapshot (stopped DB) - PASS
- Per-player + sums identical to filing snapshot (Score 3338,
  Kills 316, HLK 77, Deaths 18, CurrencyV2 7.0, Cents 700,
  SkillXP ARMOR 4121 / SWORDS 2340 / AXES 4 / BOWS 1549 /
  TRIDENTS 43 / MACES 25, rest 0). Verification set NULL x11,
  SkillBonus '{}' x11, ClassProgress 0 rows, integrity ok.
- File: step3-snapshot-stopped.txt

## Step 4 - working copy wipe - PASS before swap
- 18-column UPDATE on wipework copy only: 11 rows updated;
  in-transaction V2 = 0; committed only after that check.
- V1-V6 on wipework: V1 11; V2 0; V3 (18, 0.0, 0);
  V4 preserve dump SHA 4c9c18675b84b475b6ed9730ca9e88c0668d383c7dd19e8abd80d69ca926dc3e
  (identical to pre-wipe); V5 progress 0 + profile dump SHA
  feca53ea8f59651698d317d92d4516523c5f3a162f155d79c5ecfa0f6bc62e3a
  (identical); V6 ok.
- Wipework: 28672 B / 541cebeb2d72db1e316fb9dbb0fd6c8b6b5ef6b1ffca03ecf3c45e1785edbe3e
  PC-staged hash matched local hash.

## Step 5 - swap - PASS
- Pre-swap safety copy on server:
  player_data.db.preswap-20261114-0413 (pulled: b9d251a0...).
- Wipework pushed to live player_data.db; fresh live pull:
  28672 B / 541cebeb... = wipework hash EXACT.
- V1-V6 re-run on the now-live pull: all PASS (same values).
- Post-swap data dir: player_data.db, .preswap-20261114-0413,
  .prewipe-20261114-0413, resource_pack_status.yml; no -wal/-shm.

## Step 6 - jar placement - PASS
- Placed plugins/ExcelSealFreeze-0.1.0.jar; pull-back:
  20456 B / d3610948... = candidate hash EXACT.

## Step 7 - D50 rider - PASS (pre-start)
- In-place backup on server:
  EconomySettings.yml.bak-20261114-sealfreeze
  (pull-back 9545 B / dbec1c1e... = original EXACT).
- Edited live file pushed; pull-back 9546 B / ea7ca609ec447998b050c4954e87ff497fd288773b4169935db4e67823b147f5;
  diff vs original = line 15 only:
  enableCurrencyShower: true -> false; useVault: true intact.

## Step 8 - boot + arm
- PENDING boot handoff / log verification (V7), whitelist
  check, /sealfreeze arm + status (V8), post-start rider
  grep (V8R).

## End state
- Inside maintenance window, arrival OFFLINE: after arm +
  verification, return server to Offline via panel Shut Down
  (5 AM job owns the morning up), unless .MikeExcel is online.

## Step 8 - boot + arm - PASS (V7, V8, V8R)
- Panel Start 04:16:41 CST; Online 04:17:12; Done (22.524s)!
- Boot log (panel console + pulled latest.log, SEVERE count 0):
  ExcelSealFreeze enabling; "ExcelSealFreeze enabled.
  Armed=false, sealUnlocked=false, sealsBroken=4.";
  ExcelGuildGate "4 of 5 seals broken, guild LOCKED";
  EliteMobs "Opened database successfully" +
  "EliteMobs fully initialized!", no database ERROR.
  Two Error-level lines are pre-existing environment notices
  (terminal features; Essentials unsupported version) - not
  attributable to this install.
- Window check: server.properties white-list=true (line-only
  read on the PC; temp copy deleted immediately).
- RCON list: 0 of 10 online.
- /sealfreeze arm -> "Arming: snapshotting the EM progression
  baseline..."; log: "ExcelSealFreeze ARMED by Rcon: baseline
  snapshot of 11 players written."
- /sealfreeze status -> armed: true; seal unlocked: false,
  seals broken: 4; baseline players: 11; last error: none.
- First clamp cycle: rowsRead=11 rowsClamped=0
  columnsClamped=0, last error: none.
- baseline.txt: 11 players, every value 0 (wiped zeros).
  state.yml: armed: true (persisted for the next boot).
- V8R: post-start live EconomySettings.yml pull-back
  9546 B / ea7ca609... - enableCurrencyShower: false,
  useVault: true. Post-start live DB pull identical to
  wipework (541cebeb...); V1-V6 re-run PASS.

## End state - DONE
- Marker re-checked before Shut Down (04:27 CST): own only.
- Panel Shut Down 04:27:53 CST; panel verified Offline,
  0/10, "Server has stopped." 04:27:55. Arrival was OFFLINE
  in the maintenance window, so the server is returned DOWN;
  the 5 AM job owns the morning up.
- Same-pass surfaces: none owed per packet (no player-facing
  change; Guide / website / What's New / tips / menus
  checked in the packet - no change).
