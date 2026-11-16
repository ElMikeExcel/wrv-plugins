# ExcelSummons 0.1.13 — test results

Authored at records assembly from BUILD-RECORD.md tallies (board run 394, worker W7) — no standalone TEST-RESULTS.md existed in the build folder; no re-run was needed or performed.

Build: `ExcelSummons/local-build.sh`, run in `~/workspace/board-r394/summons-mana-build/` on 2026-11-15.

## Base gate (before any edit)

The copied 0.1.12 tree was built UNMODIFIED in `base-gate/` first:
- 123 tests found, 123 successful, 0 failed.
- Jar SHA-256 `a21c584fd9982ec5ce83b652b19b6c9536af31baecdb92251ab42afcf1d81c83` — byte-identical to the 0.1.12 candidate. Clean-room base reproduction CONFIRMED before editing.

## 0.1.13 run

```
[       128 tests found           ]
[       128 tests successful      ]
[         0 tests failed          ]

97b67906b7aad6b82c548c9ab025618fa5d7c93151331c029eb21cd824ddc1f5  ExcelSummons-0.1.13.jar
double-build: byte-identical (in both full runs)
fingerprint gate: PASS (excel-build.txt + BuildInfo fragments, build EXC-SUMMONS-0.1.13-WRV-20261115-01-mana25)
```

128 = 123 carried tests re-pinned to the +25% table, plus 5 new coherence cases: single holds + adopted 0, 4th-wolf PreCast refusal, grandfathered wolves re-derivation, grandfathered elite pair re-derivation, emitter numbers at the new percents. A second full build run reproduced `97b67906…` exactly.

## Rig

NOT RUN by the build worker (BUILD-RECORD.md, owed-at-build residual). No rig PASS is claimed here. The live install boot verification below is the boot evidence of record for this version.

## Install verification (of record, not a test re-run)

INSTALLED Nov 15, 2026 (wrv-install-failsafe 18:33 CST window): at-window G1 pulls all EXACT, placed pull-backs EXACT, boot Done (23.617s) 18:47:16 CST, Enabling ExcelSummons v0.1.13 x1, build line x1, Protect audit door bound x1, SEVERE 0, guild OPEN 5/5 unchanged; SCRIPT-OUTPUT.txt 18/18 verbatim blocks PASS. See OUTCOME.txt + SCRIPT-OUTPUT.txt in this package.

## NOT covered here (owed, player lane)

Player-flow proofs remain the packet's post-install proof owed: Mike Guardian ~44% hold + Ghast-alongside ceiling refusal; Jester-lane wolf pack 3-hold / 4th-refused / dismiss-recast; Archer+Bruiser+Wolf = 75%; Protect audit reserve_ceiling spot-check; adopted pet holds 0. They need real clients and were not run by the desk build or this records leg.
