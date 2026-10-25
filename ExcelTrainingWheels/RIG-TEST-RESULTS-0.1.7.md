# ExcelTrainingWheels 0.1.7 Night Hearing: rig test results

Jar under test: ExcelTrainingWheels-0.1.7.jar, sha256 6e38061e06b6d212e5ad17ccd4213ed2afdd9fb32433d15018f5704ddec35cae
(the exact staged jar; rebuilt again on the box Oct 25 4:15 PM CDT, byte-identical).
Unit tests: 497 passed, 0 failed (unit-tests.txt).

Rig: local Paper 26.3 build 214 (26.3-214-main@e65c076), JDK 25.0.4.1, flat world, offline mode,
difficulty Hard, plugins: ExcelGuildGate 0.3.3, ExcelTips 0.1.0 (same jar sha as live, 3bf8d0cd),
GriefPrevention 16.18.7, floodgate, ViaVersion/ViaBackwards (for the test client), spark.
tips.yml on the rig = the staged tips.yml minus the ClaimFly entry (Night Hearing entry identical).
Seals were simulated with "guildgate grant/revoke" plus "tw reload", as in the 0.1.6 rig.
Two offline test players: NhA (unprotected) and NhB (on the TW protected list, factor 0.50).
Zombies were summoned with slowness 255 where a fixed distance mattered; huts are 5x5 stone with one door.
Test window: Oct 25, 2026, 3:21 PM to 4:24 PM CDT. Rig stopped after the run.

## Boot
| Check | Result |
|---|---|
| Boot clean, 0 SEVERE / Exception lines in the whole session log | PASS (0) |
| Plugin enables ("Enabling ExcelTrainingWheels v0.1.7") | PASS |
| Config loads: night hearing line "ON from seal 2 (seal now 2, ARMED), night 13000-23000, overworld, range 16.0 (sneak 8.0), footsteps OFF, types [ZOMBIE, HUSK, ZOMBIE_VILLAGER], max 3/noise 6/player, cooldown 20s, give up 60s, bed ALWAYS OPENS, steer to doors ON (12.0 blocks), wood fallback off Hard ON (12s)" | PASS |
| New WARN lines | Only expected rig noise: EliteMobs not on the rig (2 TW lines, same as 0.1.6), offline mode, floodgate language, "floating too long" kicks of the test client |

## Behavior
| # | Case | Result | Evidence |
|---|---|---|---|
| T1 | Seal 0, night, chest noise: nobody hears | PASS | final.txt, final2.txt |
| T1b | Seal 1, night: nobody hears | PASS | final.txt, final2.txt |
| T2 | Seal 2, noon: nobody hears | PASS | final.txt, final2.txt |
| T3a | Seal 2, night: one chest noise pulls 3 of 4 idle zombies (max-per-noise 3) | PASS | final2.txt (log count "pulled 3"; final.txt counted targets and missed door-steered zombies, measurement fixed) |
| T3b | Zombie 30 blocks away (range 16) not pulled | PASS | final.txt |
| T4 | 20 s per-player cooldown: second noise 3 s later pulls nobody | PASS | final.txt, final2.txt |
| T5a | Sneaking: zombies ~11 blocks away do not hear (range 8.0) | PASS | final.txt ("container_open (range 8.0) - no idle zombie in range") |
| T5b | Standing: the same zombies hear (range 16) | PASS | final2.txt ("pulled 2") |
| T15 | max-per-player 6: three noises pull 3, 3, then 0 | PASS | final2.txt |
| T16 | Types: husk and zombie villager pulled; drowned and skeleton not | PASS | final2.txt |
| T13 | Hitting a mob is a noise (idle zombies pulled by a melee hit) | PASS | final5.txt |
| T3c | Hard, oak door: heard zombies sent to the door, door falls in ~13 s | PASS | final.txt |
| T6 | Copper door falls to the 45 s siege (46.4 s) | PASS | final.txt |
| T7 / T7b | Iron door falls to the 60 s siege (62.9 s); no give-up while working it | PASS | final.txt |
| T8 | Protected player opens a chest: nobody hears, no line names them, door stands | PASS | final.txt |
| T10 | No door at all: hunt holds ~60 s, then gives up | PASS | final.txt |
| T11 / T11b | Normal difficulty, oak door: TW wood fallback brings it down (~13-14 s) | PASS | final.txt |
| T14 | steer-to-doors false: vanilla walk-to-the-wall, iron door untouched 30 s | PASS | final.txt |
| T9 | Seal 2 night, zombies at the door: bed opens despite monsters, hunters let go | PASS | final.txt |
| T9d | 10 s later nobody hunts the sleeper, door stands | PASS | final.txt |
| T9a | Control, seal 1: vanilla bed refuses (monsters nearby) | PASS | final.txt |
| T12 | Seal revoked back to 1: hearing off again | PASS | final.txt, final2.txt |
| T17a | Tip: seal 1 night, no Night Hearing tip | PASS | final6.txt (/tips status) |
| T17b | Tip: seal 2 night, tip shows | PASS | final6.txt (shown 1 of 3) |
| T17c | Tip: same night, after a noise and 15 s more, still once | PASS | final6.txt |
| T17d | Tip: next day (noon), no new tip | PASS | final6.txt |
| T17e | Tip: next night, shown once more (ExcelTips caps total at 3) | PASS | final6.txt (shown 2 of 3) |
| T18a | Console /tw list and /tw reload work (reload prints the night hearing state) | PASS | final5.txt |
| T18b | Non-op player cannot run /tw | PASS | final5.txt |

Every behavior case passed at least once on the exact staged jar. Earlier attempts that failed are kept
in rig-logs/ for the record; each failure was a test-harness problem, not the plugin:
- final.txt T3a/T5b: the harness counted zombies with a target, but a zombie steered to a door has
  no target while it walks; part 2 counts from the console "pulled N" line instead.
- final2.txt T5a: the harness also required a log line that only appears with debug wording; the
  part 1 run of the same case passed with the "range 8.0 - no idle zombie" line.
- T13 earlier tries: the hut was built before its chunk loaded, so zombies saw the player and were
  not idle. Rebuilt after the teleport, it passed.
- The test client is kicked for "floating too long" every ~10-30 s (a mineflayer client with physics
  off on Paper 26.3); the harness rejoins and marks any case where a kick hit mid-test as INVALID.

## Untestable or not tested on the rig
- Bedrock (Floodgate) players and the loading-screen grace interaction: no Bedrock client on the box. UNTESTED.
- EliteMobs elites never hear: EliteMobs is not on the rig. Covered by unit tests only. UNTESTED on rig.
- ExcelMobVeil veiled players: MobVeil not on the rig. Unit tests only. UNTESTED on rig.
- Footsteps silent: the test client cannot walk (physics off), so no step events were made. The rig
  did log 111 movement "bounce" (landing) vibrations from the client, and none of them pulled a zombie
  (only container_open x21 and hit x1 pulled). Footstep keys are covered by unit tests. PARTLY TESTED.
- Shooting, block place/break, note block/jukebox, door/trapdoor noise keys: only container_open and
  hit were driven by the test client. Unit tests cover the key list.
- Dawn release, excluded "em_" worlds, the 3-of-3 ExcelTips cap: unit tests only.
- Real players, real terrain and live performance: needs the live server after install.
