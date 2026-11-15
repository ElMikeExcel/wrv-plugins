# ExcelSummons 0.1.11 — test results

Authored at records assembly from BUILD-RECORD.md (RESULT.md of record, board run 364, worker W2) tallies — no standalone TEST-RESULTS.md existed in the build folder; no re-run was needed or performed.

Build: `ExcelSummons/local-build.sh` (JUnit runs FIRST, then the deterministic double-build + fingerprint gate), run in `~/workspace/board-r364/summons-build/` on 2026-11-14.

## Base gate (before any edit)

The copied BB-11 0.1.10 tree was built UNMODIFIED in `base-gate/` first:
- 109 tests found, 109 successful, 0 failed.
- Jar SHA-256 `97e442165b755fb7fac325e712f634c2607490bf7f0de0dd83b28510036ff7d0` — byte-identical to the filed BB-11 0.1.10 candidate. Clean-room base reproduction CONFIRMED before editing.

## 0.1.11 run

```
[       116 tests found           ]
[       116 tests successful      ]
[         0 tests failed          ]

3d9b22a0ed3878912832760bd9df9e1f9979be7339fd7b49f805610643f7661a  ExcelSummons-0.1.11.jar
3d9b22a0ed3878912832760bd9df9e1f9979be7339fd7b49f805610643f7661a  ExcelSummons-0.1.11-repro.jar
double-build: byte-identical
fingerprint gate: PASS (excel-build.txt + BuildInfo fragments, build EXC-SUMMONS-0.1.11-WRV-20261114-01-tune11)
```

116 = 109 carried tests re-pinned to the D64 hold table + version 0.1.11, plus 7 new tests: owner plate format, idempotence / parenthesised pet names, countdown strip, notice strings with and without the mana clause, summary aggregation, warning thresholds per duration row, D64 worked fields. A second full build run reproduced `3d9b22a0…` exactly.

## Rig

Local scratch rig (Paper 26.2-123 + Magic 11.2.5, candidate hash-verified in place, candidate config staged): PASS / SEVERE 0. `Enabling ExcelSummons v0.1.11`, enable line carries build `EXC-SUMMONS-0.1.11-WRV-20261114-01-tune11`, `Done (89.525s)`, SEVERE count 0, clean disable. Log of record: rig/boot-0111.log in the build folder (28,485 B / 6fb9e3e62de9bf5327e6c04e45cfcf32abb008e059cc5ed53631db837382e063).

## Install verification (of record, not a test re-run)

INSTALLED Nov 15, 2026 (wrv-install-failsafe 09:03 CST full-push chain, Install 5 of 13): at-window G1 pulls all EXACT, placed pull-backs EXACT, boot Done (23.361s), Enabling v0.1.11 x1, build line x1, registry 98, SEVERE 0, guild OPEN 5/5 unchanged; SCRIPT-OUTPUT.txt 20/20 verbatim blocks PASS. See OUTCOME.txt + SCRIPT-OUTPUT.txt in this package.

## NOT covered here (owed, player lane)

Player-flow proofs remain the packet's post-install proof owed: two-player nameplates (Java + Bedrock), Tier-1 Archer stopwatch (T-15 + countdown + EXPIRED line), one trigger per other despawn reason, Mike's glance (two Archers hold 40%). They need real clients and were not run by the desk build or this records leg.
