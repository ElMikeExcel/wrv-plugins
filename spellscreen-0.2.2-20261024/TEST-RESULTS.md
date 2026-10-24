# ExcelWandBars 0.2.2: test results (box test rig only, never live)

Rig: /workspace/ss022-rig (Paper, Magic (elmakers) 11.2.5, port 25941). EliteMobs (MagmaGuy) not touched.
Jar tested: ExcelWandBars-0.2.2.jar sha256 7d550148663a9721915f48ba146d7c226588cd8f3bd3a31fb0b7c30e8b7ddf2d
(rebuilt from src at 3:40 PM CDT: byte-identical). Rollback target: 0.2.1 jar d25cf1e8...ecd26 (same as PR #62).

## NOT tested / not fully verified (read first)
* Real Bedrock device through real Geyser: not tested. Bedrock was a simulated player
  (`/wandbars simbedrock`) plus the real Cumulus form classes from the floodgate jar.
* Progress line skipped during an ExcelAttunement stance: unit tests only. On the rig the bot hit
  did not start a stance, so that rig check is marked PASS but says "NOT REACHED" (both rounds).
* ExcelTips with a real Bedrock player: on the rig ExcelTips sees the simulated player as Java
  (floodgate does not know it), so ExcelTips' Bedrock wording was not seen on the rig.
* Suggested tips.yml compass rewording (TIPS-INTEGRATION.md): not tested, not applied.
* Install path on the live panel: not run (staged only).

## Invalid runs (not counted)
* Anything from 2:57 to 2:58 PM CDT (rig console input cut off by another job, log flooded):
  tests/run-dbg*.log. Earlier rounds a to g were on older builds. Round h (3:04 PM) had a reload
  phase timeout, so it is not counted as a full pass.

## Unit tests
* 229 passed, 0 failed (3:25 PM and again 3:40 PM CDT). Includes "no click word in any Bedrock line".

## Full rig rounds (each: 0.2.1 baseline -> 0.2.2 -> restart -> magic load -> rank-up with a path -> ExcelTips present -> rollback to 0.2.1)
| phase | round i (3:24-3:37 PM) | round j (3:37-3:49 PM) |
|---|---|---|
| pre (0.2.1 baseline) | 3/3 | 3/3 |
| main (0.2.2) | 52/52 (1 = stance NOT REACHED) | 52/52 (same) |
| restart | 6/6 | 6/6 |
| reload (magic load, Bedrock rejoin) | 8/8 | 8/8 |
| rankup (rig wand with a Magic path) | 3/3 | 3/3 |
| tips (ExcelTips 0.1.0 present, no duplicate) | 5/5 | 5/5 |
| rollback to 0.2.1 | 6/6 | 6/6 |
| total | 83/83 | 83/83 |

* Boots: no SEVERE, no ExcelWandBars or Magic errors. Only rig-environment lines (EssentialsX
  version warning, ExcelMenu spawnshop menus missing on the rig, mcMMO bridge warning, Magic
  resource-pack check timeout with no network); the same lines appear on the 0.2.1 boots.
* After each round Magic spells.yml was restored byte-identical (rig rank-up edit undone).

## Rollback test (0.2.2 -> 0.2.1), both rounds
* 5-row chest again, slots 0-44 kept, the row-6 spell is still on the wand (Magic page 2),
  0.2.1 /spellbook works with no rank header, spellscreen-tips.yml is left and ignored.
