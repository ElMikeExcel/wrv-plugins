# ExcelEliteLadder 0.1.1: Seal 5 spawn 0.12 (patch, STAGED, not live)

Built Sun Oct 25, 2026, about 11:45 to 11:55 PM CDT on the box. Deterministic build: two clean
builds give the same jar sha (f7ec4c1ff86487e80a992ce1a982151ec8900ceab482b8ca46c94b42bdf71521).
Live is 0.1.0 (ee98604a..., installed Oct 25, 2026, about 11:18 PM CDT, holding step S1).

## Why
Mike picked the Seal 5 elite spawn rate on Oct 25, 2026, 11:40 PM CDT: 0.12 (12 of every 100 wild
spawns) instead of 0.11. Damage stays the Seal 4 values (toPlayer 2.20, toElite 0.45) and the cap
stays 120. The ladder table is built into the jar on purpose (not editable in config), so the
change needs a new jar.

## What changed
- `LadderTable`: S5 row spawn 0.11 -> 0.12. No other row, value, or behavior changed.
- `plugin.yml` version 0.1.1; `build.sh` output name 0.1.1.
- `config.yml` (the default the jar creates on a fresh install): header and the S5 comment line now
  say 0.1.1 and 0.12. Comment only. An existing plugins/ExcelEliteLadder/config.yml is never
  overwritten, so on the live server its comment keeps saying 0.11; that is harmless (the
  ladder is read from the jar, not the config).
- `tests/fixtures/S5-norm-MobCombatSettings.yml`: now the staged S5-recommended file with
  line 6 = 0.12 (sha 01dcf1dc9aea67f23366d28a945b5e75fafdaf0d5abc3dfee56c6a737d29cbe9).
- Tests: 6 new checks (S5 spawn 0.12; S5 damage equals S4; S5 cap 120; a 0.1.0-era S5 file with
  spawn 0.11 is off the ladder in 0.1.1, and the refusal names only the spawn line).

## The ladder (built into the jar)
| step | spawn | cap | toElite (L109) | toPlayer (L112) | dungeon toElite/toPlayer (L91/L94) |
|---|---|---|---|---|---|
| S1 | 0.07 | 120 | 0.55 | 1.75 | 0.5 / 2.0 |
| S2 | 0.08 | 120 | 0.50 | 1.90 | 0.5 / 2.0 |
| S3 | 0.09 | 120 | 0.50 | 2.05 | 0.5 / 2.0 |
| S4 | 0.10 | 120 | 0.45 | 2.20 | 0.5 / 2.0 |
| S5 | **0.12** (was 0.11) | 120 | 0.45 | 2.20 | 0.5 / 2.0 |

## Shas
- S5 after apply (EliteMobs number form): 49763c616018e01f744dd6b201757f1a8aafe7671cf8a7af1ddbd610f2998f13
  (was 89fd7435... with 0.11). S1 to S4 EM-form shas are unchanged (57ad4ab2, 9b4c3f95, 983fa757, 95aface7).

## Upgrade notes
- State carries over: plugins/ExcelEliteLadder/state.yml (applied step, last seals, latch),
  backups/ and apply-log.txt are read as before. Nothing is re-applied on upgrade.
- Install 0.1.1 before Seal 5 breaks. If 0.1.0 has already applied S5 (spawn 0.11), 0.1.1 shows
  the live file as OFF LADDER (spawn 0.11, want 0.12) and changes nothing (no step-down or
  sideways edits by design); the fix is then a one-line manual edit plus `em reload` (INSTALL-0.1.1.md).

## Tests
- Plain-JVM suite: 63 passed, 0 failed (`./run-tests.sh`).
- Box rig (Paper 26.3, EliteMobs 10.9.10, ExcelGuildGate 0.3.3, ExcelTrainingWheels 0.1.6),
  Oct 25, 2026, 11:49 to 11:52 PM CDT, see RIG-TEST-RESULTS-0.1.1.md.
