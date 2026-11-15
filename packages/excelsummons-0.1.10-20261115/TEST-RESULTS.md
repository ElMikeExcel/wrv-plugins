# ExcelSummons 0.1.10 — test results

Authored at records assembly from BUILD-RECORD.md tallies (no standalone TEST-RESULTS.md existed in the build folder; prep §5 item 1).

Build: `ExcelSummons/local-build.sh`, run in `~/workspace/back-button-wave3-r356/BB-11/build-0.1.10/` on 2026-11-14.

## Base gate (before any edit)

The copied LIVE 0.1.9 seq-02 tree was built UNMODIFIED in `BB-11/base-gate/` first:
- 107 tests found, 107 successful, 0 failed.
- Jar SHA-256 `ead3beec553da8e37d255971c8e8e5f96a707835c0a8811a90df115759e6df46` — byte-identical to the live 0.1.9 seq-02 candidate of record. Clean-room base reproduction CONFIRMED before editing.

## 0.1.10 run

- 109 tests found, 109 successful, 0 failed (107 carried + 2 new BackNav tests for the main-page Back fix, Java + Bedrock).
- Candidate: ExcelSummons-0.1.10.jar — 102,597 B / SHA-256 `97e442165b755fb7fac325e712f634c2607490bf7f0de0dd83b28510036ff7d0`.
- Build ID: EXC-SUMMONS-0.1.10-WRV-20261114-01-bb1110 (fingerprint gate PASS; plugin.yml declares 0.1.10).
- Determinism: internal double-build byte-identical + a second full build run reproduced the same hash. No separate -repro.jar file was kept; determinism rests on this record plus the three byte-identical jar copies in the build folder.
- Scope diff vs live tree: src BuildInfo / MenuModel / SummonsMenus only (+ tests, version knobs, DESIGN-0.1.10.md). Config byte-identical.

## Rig

Local Paper 26.2-123 + Magic 11.2.5, candidate in place — Enabling v0.1.10, Done (37.656s), SEVERE 0, clean disable. Log: BB-11/rig/boot-0110.log (29,297 B / 0aab9093796a07ced363a6ead61fa5ba3eb7b1374734b4f7469e3ea5d5d835d9). Rig verdict: PASS.

## Install verification

INSTALLED Nov 15, 2026 (wrv-install-failsafe 08:03 CST window): placed jar pull-back EXACT pre/post boot, boot Done (23.715s), Enabling v0.1.10 x1, build line x1, registry 98, SEVERE 0, guild OPEN 5/5 unchanged. SCRIPT-OUTPUT.txt: 20/20 verbatim blocks, all RESULT: PASS.

## NOT covered here (owed, by design)

Player proof still owed: mirror walk of the main page on Java + Bedrock (Back lands on the Server Menu) + Mike's Java spot taps. Per Nov 15 rule Kailey is NOT a server tester; the Bedrock half stays open ownerless / Mike-opportunistic.
