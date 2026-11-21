# D164 Shared-Loot Fix — Packet Prep Record

Created: 2026-11-20 (packet prep worker; prepare + packet only — the supervisor chain files and installs)

## Background
- ExcelSharedDungeonLoot works. The Wolf Den test "failure" was the EliteMobs Lua power
  `hyper_loot` on dungeon bosses: on death it spawns physical, owner-locked loot showers
  per damager over a 10% damage gate, bypassing the shared loot table entirely.
- Verified background: ~/workspace/sharedloot-investigation-20261120/RECORD.md
- D164 ruling: remove the `hyper_loot` power from the Primis DUNGEON boss files so loot
  flows loot table -> vote table -> plugin, and flip the plugin's debug on until a retest passes.

## Plan
1. Pull FRESH copies of the four boss files from live (Homebase route, rclone shockbyte:,
   EliteMobs custombosses folder); record size + sha256; confirm each lists hyper_loot
   (quote the powers block). Stop on any file that does not carry it.
   - primis_feral_alpha_wolf.yml
   - primis_gladius_champion_p2.yml
   - primis_mountain_pass_guardian_p1.yml
   - primis_mountain_pass_guardian_p2.yml
2. Prepare edited copies: remove ONLY the hyper_loot power entry; diff-prove exactly that.
3. Pull live ExcelSharedDungeonLoot config.yml; prepare debug: false -> true, diff-proven.
4. Determine apply path (EM reload vs restart) from the pinned EM 10.9.14 jar/docs, with evidence.
5. Write INSTALL-PACKET.md: live premise hashes, swap list + per-file backups (.bak-20261120),
   apply path, post-install proof (clean boot/reload; Wolf Den retest by Marvin + Jester after
   their lockout reset: no floor shower, shared items to BOTH per kill in plugin log lines,
   /sharedloot status counters incrementing; future dungeon imports screened for
   hyper_loot/bonus_loot powers).

## Findings

### Step 1 — Fresh pulls (2026-11-20 ~11:07 AM CT, Homebase rclone copyto + scp)
Remote root: `shockbyte:1. Michael's Server/plugins/EliteMobs/custombosses/primis/`
Saved to `originals/` in this folder.

| File | Size | SHA-256 (live, fresh) | hyper_loot line |
|---|---|---|---|
| primis_feral_alpha_wolf.yml | 2,519 B | b6b7c78f496f0f516c742064007d736408966415189b3b0a4813f1794404e5fe | line 23 |
| primis_gladius_champion_p2.yml | 2,044 B | 438a7162e81d73fdd9a0555e1c90040c8420f0113b3816f57e98e4fe8c12ac22 | line 21 |
| primis_mountain_pass_guardian_p1.yml | 3,559 B | 6fbe915546072cbd2f0c97d62113240198d13b07343e85467d0fea2adc7f6bce | line 15 |
| primis_mountain_pass_guardian_p2.yml | 10,645 B | b0b485d91ab8746c1163c964dad441d3999f275586f82dea7535119534ccb436 | line 13 |

All four carry the power — no discrepancy, no stop needed. Powers blocks verbatim:

primis_feral_alpha_wolf.yml (lines 20-25):
```
powers:
- primis_sleep.yml
- ground_pound.yml
- hyper_loot.yml
- primis_fire_circle.yml
- primis_howl.yml
```
primis_gladius_champion_p2.yml (lines 20-25):
```
powers:
- hyper_loot.yml
- primis_dome_of_darkness.yml
- primis_fire_columns.yml
- primis_push_dome.yml
- gold_explosion.yml
```
primis_mountain_pass_guardian_p1.yml (lines 13-17):
```
powers:
- invulnerability_knockback.yml
- hyper_loot.yml
- photon_ray.yml
- death_slice.yml
```
primis_mountain_pass_guardian_p2.yml (lines 11-16):
```
powers:
- invulnerability_knockback.yml
- hyper_loot.yml
- photon_ray.yml
- death_slice.yml
- primis_electrical_discharge.yml
```
No `bonus_loot` in any of the four files.

### Step 2 — Edited copies (`edited/`, diffs in `diffs/`)
Each edited file removes exactly one line, `- hyper_loot.yml` (17 bytes); unified diffs show
a single deletion hunk per file, no other change. Edited files re-parsed as YAML: powers lists
intact minus hyper_loot; zero remaining `hyper_loot` occurrences.

| File | Edited size | SHA-256 (edited) |
|---|---|---|
| primis_feral_alpha_wolf.yml | 2,502 B | b155d316ac5e28967749efbd63f53d856b5309bee00ccbabd3086a789be2f881 |
| primis_gladius_champion_p2.yml | 2,027 B | e696e4ce79d07eb4aa2bfc3bbecbb41733babeeda68e04e1a12f06ce1de07a21 |
| primis_mountain_pass_guardian_p1.yml | 3,542 B | d7a0760930828a9a7a24591a73fa07419eef1cd6473d1b81c4aaa5362d803772 |
| primis_mountain_pass_guardian_p2.yml | 10,628 B | 3c6d8d94f5eff2ecc5c2d9e64b349c8d04f834467548373f163bf2ae35f97658 |

### Step 3 — Plugin config flip
Live `plugins/ExcelSharedDungeonLoot/config.yml` pulled fresh: 387 B,
sha256 b6c704c46505cfe40385f23fb99e00775933b3e82dfdb6e8ec7af73959062eb3.
Edited copy (`edited/sharedloot-config.yml`): exactly one line changed,
`debug: false` -> `debug: true` (386 B, sha256 3bd626d5cc6eba075c55519146fe2f9dc791309017aca2d2afb1d879b4c7f380).
Diff saved as `diffs/sharedloot-config.yml.diff`.

### Step 4 — Apply path (evidence from the pinned jars, audit pull 20261120-062811)
- EliteMobs 10.9.14: `/em reload` EXISTS (plugin.yml permission `elitemobs.reload`,
  "Allows admins to reload EliteMobs"; `com.magmaguy.elitemobs.commands.ReloadCommand`).
  Bytecode: it runs `AdvancedCombatRuntime.prepareForSoftReload()` then a full
  `onDisable()` -> `onLoad()` -> `onEnable()` cycle. The enable-time initialization
  constructs `CustomBossesConfig` and calls `CustomBossesConfig.initializeBosses()` —
  the routine that loads the custombosses folder. So `/em reload` DOES re-read the four
  boss files; already-spawned bosses keep their loaded powers, new spawns/instances
  pick up the edit.
- ExcelSharedDungeonLoot 0.1.0: NO reload path. Its plugin.yml declares only
  `/sharedloot status`; source reads `debug` once into a field in `onEnable()`.
  The config flip therefore needs a plugin reload or a server restart.
- CONCLUSION: one server restart is the apply path — it is REQUIRED for the debug flip
  and it covers the boss files in the same window. (`/em reload` alone would fix the
  boss files but would leave debug off.)

### Step 5 — Packet
INSTALL-PACKET.md written in this folder. Nothing filed, nothing installed by this worker.

