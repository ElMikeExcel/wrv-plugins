# TEST RESULTS — ExcelShops 1.3.9 (Windows-safe directory sync)

Nov 14, 2026 · Harness: fleet `build.sh` (JDK 25, `--release 21`),
JUnit platform console, Mockito 5.15.2 (already on the test classpath).

## Suite totals

| Run | Classes under test | Tests | Result |
|---|---|---|---|
| Base gate — unmodified tree-1.3.8 copy (`work/out-base`) | 1.3.8 | 430 | **430/430 pass**; packaged jar byte-identical to shipped 1.3.8 (`b8a1b8fd…`) |
| 1.3.9 build A (`work/out-139-a`) | 1.3.9 | 433 | **433/433 pass** (JUnit summaries in `work/out-139-a/junit.txt`) |
| 1.3.9 build B (`work/out-139-b`) | 1.3.9 | 433 | **433/433 pass** (`work/out-139-b/junit.txt`); jar byte-identical to build A |

## New coverage — DirectorySyncBestEffortTest (3 tests, all new)

File: `tree-1.3.9/tests/com/winkreyes/shops/checkout/DirectorySyncBestEffortTest.java`

Simulation: a Mockito static mock of `FileChannel` (real-method
default) whose `open(Path, OpenOption...)` throws
`java.nio.file.AccessDeniedException` whenever the path is a
directory — byte-for-byte the refusal the diagnosis probe observed on
Windows (JDK 25, Win 11) — while every file open executes the real
method. (Matcher note: the stub must use the `OpenOption[]` array
form; a single-component matcher silently misses multi-option calls
such as LegacyDataMigration's `READ + NOFOLLOW_LINKS` open. Verified
with a standalone probe before the suite run.)

Scenarios, each exercising a store save whose directory force fails:

1. `fileJournalOpensAndCleanClosesWhenDirectorySyncRefused` — the
   checkout store (the mirror's failing fence): FileJournal
   constructor + clean close succeed; session marker still removed;
   a reopen shows no false recovery.
2. `globalSessionGuardRecordsReadyAndClosesCleanWhenDirectorySyncRefused`
   — guard assess → CLEAN_START, `recordReady` writes the READY
   envelope (atomic move + directory sync refused), `closeClean`
   removes it.
3. `legacyMigrationCompletesWhenDirectorySyncRefused` — a full legacy
   migration (the existing suite's populate fixture) completes with
   outcome MIGRATED and byte-equal contents; source untouched.

## Discrimination check (the simulation bites)

The new test class was compiled and run against the **unmodified
1.3.8 classes** (`work/out-base/classes`): **0 passed / 3 failed**,
each failing with the simulated `AccessDeniedException` (FileJournal
failures trace to `FileJournal.syncDirectory (FileJournal.java:313)`
from the constructor — the exact mirror failure point). Raw output:
`work/discrim/junit-base.txt`. Against 1.3.9 classes: 3/3 pass as part
of the 433 above.

## Not covered here (owed at rig)

A real Windows JVM end-to-end: the unit simulation reproduces the
exception class and call shape proven by the diagnosis probe, but the
acceptance proof is the PC-mirror boot of 1.3.9 reaching
"ExcelShops purchases available." — the supervisor's rig step.
