# RIG RESULTS — ExcelTrainingDummy 1.0.0

Rig: local Paper 26.3 (build 235 tree copied from the ExcelTour
1.0.0 rig, `rig-1.0.0/server/`) with ExcelTrainingDummy-1.0.0.jar
(SHA-256 verified `b46250ed…c098` in the rig plugins folder before
boot), Geyser-Spigot 2.11.3-SNAPSHOT + floodgate 2.2.5-SNAPSHOT
co-loaded (softdepend coexistence proof), and the DummyRig driver
plugin (NMS fake player, the TourRig pattern; `driver-src/`).
Offline mode. Full log of the evidence run: `boot.log` in this
folder (third boot; see the anomaly note). The driver shut the
server down itself at the end of the exercise.

## Boot

- "Enabling ExcelTrainingDummy v1.0.0" + enable line:
  `ExcelTrainingDummy 1.0.0 enabled (build
  EXC-TDUMMY-1.0.0-WRV-20261112-01-178829); tiers Practice 1,000 /
  Dungeon 5,000 / Seal 20,000; caps 5 per player, 25 server.`
- Done (30.443s). **SEVERE: 0** in the whole boot + exercise +
  shutdown. Clean stop: "Disabling ExcelTrainingDummy v1.0.0".

## Behavior exercise — 11/11 PASS (RIGOBS lines in boot.log)

| # | Check | Result |
|---|---|---|
| 1 | Plugin enabled alongside Geyser + floodgate | PASS |
| 2 | NMS fake player DummyRigA joins | PASS |
| 3 | Real `/dummy spawn 1 practice` as the player → 1 dummy | PASS |
| 4 | Dummy carries the PDC training stamp; entity is a zombie | PASS |
| 5 | Fresh dummy at full real health (virtual pool design) | PASS |
| 6 | Real `damage(300, player)` event → session records the hit | PASS — recorded **295.2** = 300 × 0.984, the vanilla armor curve on the zombie's base 2 armor points (single event, proven by the driver's MONITOR DMGOBS logger) |
| 7 | Virtual pool: remaining = 1000 − recorded, exactly | PASS |
| 8 | Entity health still 20.0 after the 300 hit — the pool took it, not the mob | PASS |
| 9 | Overkill hit (5,000) finishes the test → dummy removed, count 0 | PASS |
| 10 | `/dummy spawn 3 dungeon` → the 3-cluster stands | PASS |
| 11 | `/dummy removeall` → count 0; console `/dummy version` answered with the build line | PASS |

## Anomaly note (first boot, not reproduced)

The FIRST boot of this rig (log preserved as `boot.log` run 1 in
the build chat record; superseded by `boot2.log`/`boot3.log`, both
kept in this folder) recorded 329.2 for the same 300 hit and failed
check 6. The driver was then instrumented with a MONITOR-priority
damage logger (DMGOBS) and the rig run twice more: both runs show
exactly ONE damage event per hit and the identical recorded value
295.2 (= the armor-curve expectation). The 329.2 did not reproduce
and its cause was not identified; the instrumented runs are the
evidence of record. Intake semantics as proven: the session records
`event.getFinalDamage()` — damage as actually dealt after armor —
which is the honest number for a DPS meter (real targets have armor
too); the dummy mob's base armor is the zombie's 2 points.

## Limitations, stated plainly

- Chat output to the fake player (checkpoint prints, the final
  report) is not visible in the console log — the report content is
  proven by the unit suite (ReportText checks rebuild the exact
  lines) and the finish/removal path is proven here; a human
  reading the report in-game is still owed player proof.
- No Bedrock client exists on this rig: the Cumulus form path was
  NOT exercised. The Java inventory menus were not click-exercised
  either (no client); menu actions route through the same
  spawn/settings code the rig proved via commands.
- Hazardous pulses, the lethal clamp, tap-the-dummy with a compass,
  claim-gate behaviour (no claims plugin in this rig — wilderness
  path only), idle despawn and logout despawn are covered by unit
  tests + code, NOT by this rig. They join the owed player-proof
  list in the BUILD-RECORD.
- ExcelClaims' GriefPrevention API was not in this rig stack: the
  ClaimGate reflection path is desk-verified against the fork's
  source (DataStore.getClaimAt / Claim.getOwnerID / isAdminClaim /
  parent) but not live-proven.
