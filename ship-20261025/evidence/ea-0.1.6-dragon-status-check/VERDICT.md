# ExcelAttunement 0.1.6: "world_the_end: dragon ALIVE (zone locked)" after the Oct 24 11:37 AM CDT restart

Verdict (Sat Oct 24, 2026, ~11:55 AM CDT): NOT A BUG. No 0.1.7 built or staged.

## How 0.1.6 decides ALIVE (ArenaLocks.readDragonState, cached per tick)
ALIVE if ANY: (1) a loaded, non-dead EnderDragon entity; (2) DragonBattle.getEnderDragon() alive;
(3) getRespawnPhase() != NONE; (4) getBossBar().isVisible(); (5) !hasBeenPreviouslyKilled().
A signal that throws = UNKNOWN = locked (fail-safe).

## Why live says ALIVE
- Live has never killed its dragon: post-boot log shows GuildGate "✘ The Ender Dragon", seals 1 of 5 (raid only);
  the dragon fight is planned for tonight. Signal 5 (never won) holds ALIVE: the designed behaviour
  ("never fought = locked until the first kill", INSTALL-RECORD of 0.1.6).
- BossTrial "completed battles=1" is NOT a kill: DragonChapter.finishBattle increments it whenever a trial
  battle ends, including when players leave range ("Trial battle ... ended during PHASE1" WARN).
  phase=NONE is the in-memory phase, reset on every restart.

## Restart test (rig, Paper 26.3, EA 0.1.6 441d3f70..., BossTrial 0.2.7 + staged config), 11:46-11:50 AM CDT
| phase | result |
|---|---|
| spawn (fresh End, never killed) | 2/2 PASS, ALIVE |
| restart, empty, never killed | ALIVE (bar=true, prevKilled=false, no loaded entity); plain entity test = "Test failed" (false negative); forceload test = "Test passed. Count: 1"; still ALIVE after unforceload. 3/4 in log: the 1 FAIL is the harness regex ("passed, count" vs "passed. Count"), the output itself is correct; regex fixed in t_restart.js |
| kill (via BossTrial phases) | 3/3 PASS, NONE right after and with End empty |
| restart, empty, killed | 4/4 PASS: NONE early and after the 300-tick window (bar=false, prevKilled=true); forceload test "Test failed"; still NONE |
=> 0.1.6 does NOT report a false ALIVE after a real kill + restart.

## Read-only console check for live (temporary forceload scoped to the End; live has 0 End forceloads)
1. execute in minecraft:the_end run forceload add -128 -128 127 127
2. wait ~10 s
3. execute in minecraft:the_end if entity @e[type=minecraft:ender_dragon]
   "Test passed. Count: 1" = dragon alive in the unloaded End. "Test failed" = no dragon.
4. execute in minecraft:the_end run forceload remove all
Without step 1 the test gives a false "Test failed" (proved on the rig).
Simpler signal: after the first real kill, /attunement status must read NONE (zone open), also after restarts.

## Rig state after the test
BossTrial 0.2.6 jar + config restored; the rig End from before the test was put back
(test End kept at /workspace/ea017-check/rig-end-after-test). Rig Attunement config = rig-config-arena-full.yml.
