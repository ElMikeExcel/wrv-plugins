# ExcelMakersMark 0.1.6 fsync fix — test results

**Date:** Nov 14, 2026 · Harness: `build-tests.sh` in this folder (the
tree's own `TestRunner`, JUnit platform, package `com.winkreyes.makersmark`).

## Environment note (why the harness looks like this)

The tree's canonical build (`build-offline.sh`) compiles the whole tree
against the composed Magic jar from the original offline environment;
that jar is absent from this VM, and 7 sources (bridge, registrations,
main, wrv-magic helpers) cannot compile without it. No recorded test
touches those classes: the recorded tests exercise only `core/*`,
`Accomplishments`, `Marks`, and (new) `history/*`. The harness therefore
compiles exactly that closure plus the recorder classes, using a
harness-only compile stub for the main plugin class
(`test-stubs/…/ExcelMakersMark.java`, mirrors the real class's config
defaults; never shipped, never in the tree). Classpath = the house lib
set (excelshops/build-libs) + Paper API 26.3 + stock Magic 11.2.5 +
PlaceholderAPI 2.12.3 + JUnit console standalone 1.11.4 (+ slf4j/log4j
for the JUnit vintage discovery on this host). Full-tree suite in the
original offline env remains available to the supervisor if wanted; the
closure below covers every recorded test class.

## Base proof (unmodified tree)

`gate/base-suite-output.txt` — **40 tests found, 40 successful, 0 failed**
(6 recorded test classes: CoreTest, MilestoneColorsTest,
TransferEvidenceTest, ColorCreditTest, FinderDataTest,
AccomplishmentsTest). Plus Gate B: unmodified `HistoryLedger.java`
recompiles byte-identical to all 10 shipped jar entries (BUILD-RECORD).

## Fixed tree

`gate/fixed-suite-output.txt` — **44 tests found, 44 successful,
0 failed**: the same 40 recorded tests, unmodified, plus the 4 new
tests below.

## New coverage — `HistoryLedgerFsyncTest` (tests/com/winkreyes/makersmark/history/)

1. `directoryForceOnExistingDirectoryReturnsNormally` — the seam succeeds
   on a real directory (on Windows pre-fix this very call threw
   AccessDeniedException).
2. `directoryForceFailureIsSwallowedMissingDirectory` — a missing
   directory (NoSuchFileException) must not propagate.
3. `directoryForceFailureIsSwallowedThroughRegularFile` — a path through
   a regular file (NotDirectoryException) must not propagate.
4. `ledgerSaveReloadAndCheckpointSucceed` — end-to-end: open (constructor
   directory sync) → acceptAward → prepare → commit → checkpoint (both
   publication directory syncs) → close → reopen → state and COMMITTED
   status replayed intact.

Tests 2–3 simulate the directory-force failure with the FileSystemException
siblings that occur on Linux; the exact Windows AccessDeniedException
cannot occur on this host (directory channels open fine here) — that
path's proof is the mirror rig boot, recorded as owed in BUILD-RECORD.

## Mutation check (the new tests pin the fix)

The new test class was also run against a scratch build of the ORIGINAL
HistoryLedger with only the test-seam visibility widened (original
throwing body kept): **2 tests fail** (tests 2 and 3 — the exception
propagates), 2 pass (tests 1 and 4). Against the fixed class: 4/4 pass.

## Candidate-jar check

The 4 new tests were additionally run with the **candidate jar itself**
on the classpath (not the harness compile): **4/4 successful** — the
shipped artifact carries the fix.
