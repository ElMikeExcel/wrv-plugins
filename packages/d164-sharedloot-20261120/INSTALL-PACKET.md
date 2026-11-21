# INSTALL PACKET — D164 Shared-Loot Fix (hyper_loot removal + plugin debug on)

Prepared: 2026-11-20 by the packet-prep worker. Prep record: `RECORD.md` in this folder.
Investigation of record: `~/workspace/sharedloot-investigation-20261120/RECORD.md`.
Staged files: `originals/` (fresh live copies), `edited/` (apply these), `diffs/` (proof).

## What this fixes
The Primis dungeon bosses below carry EliteMobs' Lua power `hyper_loot`, which on death
spawns physical, owner-locked loot showers per damager (10% damage gate), bypassing the
loot table -> vote table -> ExcelSharedDungeonLoot path entirely. Removing the power makes
their loot flow through the shared path like every other dungeon boss (D139/D164). The
bosses' loot tables already carry their full class-loot lists, so signature gear still
drops — shared. The same window flips ExcelSharedDungeonLoot's debug on until the retest
passes, so every stand-down reason is visible in the log during the proof.

## Live premise (pulled fresh 2026-11-20 ~11:07 AM CT via Homebase/rclone — re-verify at apply time; STOP if any hash differs)

Boss files, remote folder `plugins/EliteMobs/custombosses/primis/`:

| File | Live size | Live SHA-256 |
|---|---|---|
| primis_feral_alpha_wolf.yml | 2,519 B | b6b7c78f496f0f516c742064007d736408966415189b3b0a4813f1794404e5fe |
| primis_gladius_champion_p2.yml | 2,044 B | 438a7162e81d73fdd9a0555e1c90040c8420f0113b3816f57e98e4fe8c12ac22 |
| primis_mountain_pass_guardian_p1.yml | 3,559 B | 6fbe915546072cbd2f0c97d62113240198d13b07343e85467d0fea2adc7f6bce |
| primis_mountain_pass_guardian_p2.yml | 10,645 B | b0b485d91ab8746c1163c964dad441d3999f275586f82dea7535119534ccb436 |

Plugin config, remote path `plugins/ExcelSharedDungeonLoot/config.yml`:
387 B, SHA-256 b6c704c46505cfe40385f23fb99e00775933b3e82dfdb6e8ec7af73959062eb3 (debug: false).

## Swap list (5 files; content/config only — no jar changes)
For each file: back up the live file to `<filename>.bak-20261120` beside it FIRST, then
place the edited copy from `edited/` and verify the placed hash.

| Live path | Edited SHA-256 | Change |
|---|---|---|
| plugins/EliteMobs/custombosses/primis/primis_feral_alpha_wolf.yml | b155d316ac5e28967749efbd63f53d856b5309bee00ccbabd3086a789be2f881 | remove line `- hyper_loot.yml` only |
| plugins/EliteMobs/custombosses/primis/primis_gladius_champion_p2.yml | e696e4ce79d07eb4aa2bfc3bbecbb41733babeeda68e04e1a12f06ce1de07a21 | remove line `- hyper_loot.yml` only |
| plugins/EliteMobs/custombosses/primis/primis_mountain_pass_guardian_p1.yml | d7a0760930828a9a7a24591a73fa07419eef1cd6473d1b81c4aaa5362d803772 | remove line `- hyper_loot.yml` only |
| plugins/EliteMobs/custombosses/primis/primis_mountain_pass_guardian_p2.yml | 3c6d8d94f5eff2ecc5c2d9e64b349c8d04f834467548373f163bf2ae35f97658 | remove line `- hyper_loot.yml` only |
| plugins/ExcelSharedDungeonLoot/config.yml | 3bd626d5cc6eba075c55519146fe2f9dc791309017aca2d2afb1d879b4c7f380 | `debug: false` -> `debug: true` only |

Every diff is saved in `diffs/` and is exactly the stated change — no collateral edits.

## Apply path — ONE server restart (required)
- ExcelSharedDungeonLoot has no reload command (`/sharedloot status` only) and reads
  `debug` once in `onEnable()` — the config flip only takes effect on a restart (or a
  plugin-manager reload, not house practice).
- The boss files would also re-read under `/em reload` (EM 10.9.14 bytecode: reload =
  prepareForSoftReload + onDisable/onLoad/onEnable, and enable re-runs
  `CustomBossesConfig.initializeBosses()`), but since a restart is already required for
  the config, ride both halves in the one restart window under the normal install gates
  (verified-empty or the ordered weekend exception; one window backup per house rules).
- Note: only NEW boss spawns/instances pick up the power removal; any boss already
  spawned at swap time keeps its loaded powers. The retest uses a fresh instance.

## Post-install proof
1. Boot clean: server reaches Done with SEVERE 0; no new WARN/ERROR naming any of the
   four bosses; EliteMobs boots at 10.9.14; ExcelSharedDungeonLoot boot line reads
   `enabled (enabled=true, debug=true)` and its vote-suppression seam reports OK.
2. Retest (Marvin + Jester, Wolf Den, after their loot-lockout reset for this boss has
   landed): run the dungeon fresh and confirm ALL of:
   - NO physical loot shower on the floor from the Feral Alpha Wolf (nothing soulbound
     sitting at the corpse).
   - The plugin's INFO line per boss kill reads `Shared N item(s) x 2 player(s) = K
     clone(s)` — identical shared item(s) delivered into BOTH players' inventories,
     with `[debug]` lines showing snapshot/intercept detail.
   - `/sharedloot status` counters (snapshots / intercepts / suppressed / delivered)
     increment per kill versus the pre-retest read.
   - Both testers confirm in chat/DM they each received the shared item(s).
3. If the retest passes, a follow-up text edit flips `debug` back to false (same
   one-line shape; no restart urgency — it takes effect at the next restart).

## Surfaces / screening note (for the filing step)
- Player-facing effect: dungeon boss loot in the Primis dungeons now shares to the whole
  eligible party instead of showering per-damager floor loot. What's New / website copy
  rides the normal same-pass surfaces at install time.
- STANDING SCREENING RULE going forward: every future dungeon content import is checked
  for loot-generating Lua powers (`hyper_loot`, `bonus_loot`) in its boss files before
  install; a dungeon boss carrying one is flagged and the power removed or the import
  held — the D139 shared-loot promise is silently void otherwise. (The 12 other live
  boss files carrying these powers are overworld event bosses — outside D139 scope,
  untouched by this packet.)

## Rollback
Restore any file from its `.bak-20261120` copy and restart; the change is five text
files, fully reversible.
