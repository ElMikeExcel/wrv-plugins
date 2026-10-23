# ExcelSealLog 1.0.0

Read-only console logger for the five guild seal fights, so the seal watcher can report starts, progress and ends.
Every line is prefixed `[SealLog]` and uses `EVENT key=value ...` (no spaces in values, players comma-separated, `-` = none).

| Event | When |
|---|---|
| `FIGHT_START seal=dragon` | a player enters the End while a dragon is alive (later arrivals log `PLAYER_JOINED`) |
| `BOSS_SPAWN seal=wither` | a Wither spawns (`reason=BUILD_WITHER` for a player build) |
| `BOSS_EMERGE seal=warden` | a Warden spawns/emerges |
| `FIGHT_START seal=<boss> trigger=first_player_damage` | first player hit on a boss not already tracked (Elder Guardian start) |
| `PLAYER_JOINED` | a new player damages a tracked boss |
| `BOSS_HEALTH milestone=75%/50%/25%` | boss health crosses a milestone (once each) |
| `BOSS_DEATH killer=<name>` | boss dies, with killer and players involved |
| `RAID_START village=world@x,y,z` / `RAID_WAVE wave=n/total` / `RAID_WON` / `RAID_LOST` / `RAID_ENDED reason=` | raid lifecycle |

All listeners are MONITOR priority and never cancel or change anything. No commands, no config, no permissions, no files written.

## Alternatives checked first (Oct 23, 2026)
- Installed: ExcelGuildGate only logs `Seal broken` and admin grants; CoreProtect logs kills to its database, not fight lifecycle. Nothing else installed covers this.
- FinalBlow (SpigotMC): broadcasts the final blow on Dragon/Wither only; built for 1.15.
- RaidLogs (SpigotMC/GitHub): raid start/stop to a separate raids.log; no bosses, no waves.
- AdvLoc (Hangar): generic 150+ event logger, floods the console, last pinned to 1.20.6.
- EnderDragon (Hangar): dragon customization plugin that changes gameplay; 1.20.4 max.

## Build
`./build.sh` (JDK 25, `--release 21`, Paper API 26.2.133 libs; override `JDK`, `LIBS`, `JUNIT`). Deterministic jar: SHA-256 `e75de2643cf0e5f1e34902574f2bc3732144356ae6609dae1c15b231b61eb0be`, 8,420 B.

## Tests
- 4/4 JUnit (health milestone logic).
- Scratch Paper 26.2 server: enabled clean, zero SEVERE. Console summons showed BOSS_SPAWN, BOSS_HEALTH 75/50/25, BOSS_DEATH for the Wither, BOSS_EMERGE/BOSS_DEATH for the Warden, and correctly no fight start for non-player damage on an Elder Guardian. Player-only paths (End entry, player damage, raids) need a real client and were not run.

## Install (empty server only, Mike's go)
1. Upload `ExcelSealLog-1.0.0.jar` to `/plugins/`. 2. Restart. 3. Console shows `[SealLog] READY version=1.0.0`.
Rollback: delete the jar and restart.
