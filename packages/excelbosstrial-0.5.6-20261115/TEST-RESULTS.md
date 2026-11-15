# TEST RESULTS — ExcelBossTrial 0.5.6 fsync follow-up rebuild (Nov 14, 2026)

Tally method: `work/tally.py` (copied from the original
0.5.6 build's work/ folder) over each `package.sh` log.
Every build below ran the FULL suite through `package.sh`
(build-test.sh compiles all source + all harnesses and runs
every main-method harness; a harness failure fails the
build). JDK 25 (`~/workspace/.jdk25`), `--release 21`.

## Runs

| Run | Tree | Jar | Size | Suite |
|---|---|---|---|---|
| base gate | unmodified copy of the first fsync rebuild's tree | `work/base-gate.jar` = `eb11a6e2…` | 470,592 B | 2,775 checks / 99 harnesses, 0 failures |
| pkg1 | fixed | `work/pkg1.jar` = `005bd005…` | 471,888 B | 2,792 checks / 101 harnesses, 0 failures |
| pkg2 | fixed | `work/pkg2.jar` = `005bd005…` | 471,888 B | 2,792 checks / 101 harnesses, 0 failures |
| pkg3 | fixed, final deliverable tree (post doc-refresh) | `work/pkg3.jar` = `005bd005…` | 471,888 B | 2,792 checks / 101 harnesses, 0 failures |

- Base gate jar byte-identical to the first fsync
  rebuild's candidate of record
  (`eb11a6e2f2c74436a505a7199c2f5a686fe45742f00257549d61530f712bd265`).
- pkg1 = pkg2 = pkg3 byte-identical (deterministic
  double-build, plus the deliverable-tree confirmation).
- `grep "^FAIL"` over the pkg1/pkg2/pkg3 logs: 0 lines.
- Delta vs base: +17 checks / +2 harnesses = the two new
  DirectoryForce harnesses below; every earlier harness
  (0.5.6 features, the AtomicStateFile fix's included)
  re-ran green.

## New harness — MarkerRegistryDirectoryForceTest (8 checks, all pass)

`tests/com/winkreyes/teamrewards/marker/MarkerRegistryDirectoryForceTest.java`,
same package as the class under test. Covers:

1. Public-API add with the real directory force: the
   marker lands and survives close + reopen. (2 checks)
2. **Windows failure mode**: `mutate()` is driven with a
   `DirectoryForce` that throws `AccessDeniedException` —
   the exact exception Windows NIO throws opening a
   directory channel. Asserts: the force was actually
   attempted; two adds and a remove all succeed; the
   registry stays writable throughout; after close +
   reopen, exactly the surviving marker is on disk (the
   excused writes are genuinely durable). (4 checks)
3. **Control / Linux semantics**: a `DirectoryForce`
   throwing a plain `IOException` still fails the mutation
   with the pre-existing `IOException("marker persistence
   uncertain", cause)` shape, and the registry is no
   longer writable afterwards. (2 checks)

## New harness — TeamRewardLedgerDirectoryForceTest (9 checks, all pass)

`tests/wrv/rewards/TeamRewardLedgerDirectoryForceTest.java`,
same package as the class under test. Covers:

1. Public-API saves with the real directory force:
   encounter + roster survive close + reopen. (2 checks)
2. **Windows failure mode**: `persist()` is driven with a
   `DirectoryForce` that throws `AccessDeniedException`.
   Asserts: the force was actually attempted; encounter
   open + qualify succeed and stay readable; a `win()`
   persists an OWED claim in memory; after close + reopen
   the WON outcome and the OWED claim are both durable on
   disk. (5 checks)
3. **Control / Linux semantics**: a `DirectoryForce`
   throwing a plain `IOException` still propagates out of
   the save unchanged, and the ledger then refuses further
   work (its persistence-uncertain self-protection keeps
   its trigger for genuine durability failures).
   (2 checks)

Why injection rather than a natural reproduction: the
build host is Linux, where opening a directory channel
succeeds — the Windows refusal cannot occur naturally
here. Each seam exercised is the production code path
itself (the public mutations delegate to it with the real
`forceDirectory`), so the only substituted element is the
final NIO call. The end-to-end Windows proof is the PC
mirror rig's in-play exercise of markers and team
rewards, owed under P-FSYNC-1's acceptance once the fixed
jars land there.

## Logs

`work/base-gate.log`, `work/pkg1.log`, `work/pkg2.log`,
`work/pkg3.log` in this folder.
