# TEST RESULTS — ExcelWandBars 0.3.18 (Nov 14, 2026)

Base gate (pre-change proof): the unmodified 0.3.17 sources, copied
from `wandbars-verify-race-build/build-0.3.17`, compiled with the
house toolchain (Temurin JDK 25.0.5+8, `work/cp.txt`) and repacked,
came out BYTE-IDENTICAL to the shipped 0.3.17 jar: 553,827 B,
SHA-256 `949e9cd48790cd10bb2861ce32f4cc3442410aee219e91bbc48f6cb526330c9b`
(`work/basecheck/`, cmp clean). Only then were the 0.3.18 changes
applied.

## Suites

Plain-main house suites, compiled against each candidate's classes
and run per suite (`run-tests.sh`; full logs `work/test-run-0318.log`
for build A, `work/test-run-0318-b.log` for build B — identical
outcomes). The new suite additionally runs with the Mockito
javaagent (static `FileChannel.open` stub), as in ExcelShops 1.3.9.

**24 suites / 679 checks / 0 failed — on BOTH builds.**

Carried from 0.3.17, unchanged test files (669 checks):
ApplyCoalescePlanTest 19 · ArrangePlanTest 25 · BarLoadoutNewTest 35 ·
BarPositionTest 35 · BookHoldPlanTest 51 · CooldownDisplayPlanTest 20 ·
CooldownTagPlanTest 61 · DragWritePlanTest 38 · DragWriteTwoTapTest 13 ·
ExitSweepPlanTest 23 · FormContrastTest 70 · HoldReadPlanTest 11 ·
JoinGuardPlanTest 14 · JoinSweepPlanTest 8 · PagerDefault0312Test 27 ·
PagerPlanTest 29 · QuitClosePlanTest 8 · ShapeAPageCapTest 26 ·
SlotGuardPlanTest 28 · SpellbookNewTest 13 · SpellbookPager039Test 54 ·
UnarmedDragPlanTest 14 · VerifyRetryPlanTest 47

New (10 checks): `DirectorySyncBestEffortTest` —
for each of the 3 directory-force sites
(AuthenticatedWindow.commit, BoundedPractice.persist,
PracticeIdentityJournal.persist):
- directory opens throw `AccessDeniedException` → save succeeds (3)
- directory opens throw `UnsupportedOperationException` → save succeeds (3)
- no refusal (Linux behaviour) → save succeeds (3)
plus 1 reopen-cleanly check on the refused AuthenticatedWindow file.

## Discrimination

The new suite run against the unmodified 0.3.17 (base-gate) classes
FAILS (exit 1) at the first refused scenario with the exact diagnosed
production symptom (`work/discrim-base.log`):

```
java.io.IOException: durable issuance uncertain; close/reconcile
Caused by: java.nio.file.AccessDeniedException: /tmp/wandbars-dirfsync-…/ad-aw
```

## Jar

candidate-a and candidate-b byte-identical (repack.py from the
shipped 0.3.16 jar): 554,089 B, SHA-256
`cadb16a6e70ebcb4236302dfa6c2b67ae23a0ba1b03ac277f57ead9a9d6e1f11`.
Exactly 6 entries differ from the shipped 0.3.17 jar: the 3 fixed
classes, BuildInfo, plugin.yml, excel-build.txt.

NOT used as evidence: any mirror rig — the mirror re-run is a
separate lane (see BUILD-RECORD §5).
