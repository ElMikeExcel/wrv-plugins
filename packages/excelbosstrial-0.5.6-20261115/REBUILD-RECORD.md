# REBUILD RECORD — ExcelBossTrial 0.5.6, fsync follow-up: MarkerRegistry + TeamRewardLedger (P-FSYNC-1)

Built Nov 14, 2026 (parent-dispatched desk lane). Commission:
close the two directory-force sites the first BossTrial
fsync lane flagged in its REBUILD-RECORD "Observation for
the supervisor" — `com.winkreyes.teamrewards.marker.MarkerRegistry`
and `wrv.rewards.TeamRewardLedger` carry the identical
Windows-hostile idiom (post-move `FileChannel.open(directory)`
+ `force()`) that the mirror diagnosis
(`~/workspace/mirror-storage-diagnosis-20261114/DIAGNOSIS.md`)
proved degrades BossTrial on the PC test-server mirror.
Neither site fires at boot (they fire on in-play writes:
personal-chest marker mutations and team-reward ledger
saves), so neither affected the mirror-boot acceptance —
but the rig's play proofs would have hit both. Mike's
standing pattern: finish the full sweep, close the class,
not the specimen. Desk only: no installs, no MANIFEST
edits, no live-server contact, no sends. The rebuilt
candidate stays HELD under the original release condition
— it installs only after the Nov 14 Warden attempt, in the
supervisor's filing/install flow.

## Base

- Source: the `bosstrial/` tree copied verbatim from
  `~/workspace/bosstrial-056-fsync-rebuild-20261114/` (the
  first fsync rebuild; that folder was never modified —
  the closing tree diff was taken against it, see below).
- Base candidate: `ExcelBossTrial-0.5.6.jar` — 470,592 B /
  sha256
  `eb11a6e2f2c74436a505a7199c2f5a686fe45742f00257549d61530f712bd265`
  (the AtomicStateFile-fixed 0.5.6; build ID
  `EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56`, unchanged).

## Base gate (house pattern — PASSED before any edit)

`package.sh` on the UNMODIFIED copy (JDK 25,
`~/workspace/.jdk25`, classpath per the original build's
`work/classpath.txt`) produced:

- `work/base-gate.jar` — **470,592 B / sha256
  `eb11a6e2f2c74436a505a7199c2f5a686fe45742f00257549d61530f712bd265`**,
  byte-identical to the base candidate.
- Suite green during that build: **2,775 checks / 99
  harnesses** (`work/tally.py`), 0 FAIL lines — exactly the
  first fsync rebuild's tally.

## The change (only this)

Same fix shape as `AtomicStateFile` (see
`ATOMICSTATEFILE.diff`, carried over from the first fsync
rebuild): the post-move directory force is extracted behind
a package-private `@FunctionalInterface DirectoryForce`
seam per class, the production default is the original
open+force as a private static `forceDirectory`, and the
call site excuses ONLY `AccessDeniedException` — the exact
refusal Windows NIO throws opening a directory channel —
on the strength of the mandatory temp-file fsync. Any
other directory-force failure propagates exactly as
before. Full diffs in `MARKERREGISTRY.diff` and
`TEAMREWARDLEDGER.diff` beside this record.

- `src/com/winkreyes/teamrewards/marker/MarkerRegistry.java`
  — `mutate()`: the inline directory open+force after the
  atomic move (and after `writeProbe.afterMove()`) becomes
  `try { directoryForce.force(parent); } catch
  (AccessDeniedException …) { /* excused */ }`. Seam sits
  beside the class's existing `WriteProbe` seam as a
  package-private field (field injection, matching that
  class's style). A genuine failure still lands in
  mutate's existing handler: `failed=true` + `IOException`
  "marker persistence uncertain" — message text unchanged.
  Class doc gains the durability contract sentence.
- `src/wrv/rewards/TeamRewardLedger.java` — `persist()`:
  the inline directory open+force after the atomic move
  becomes the same wrapped seam call. Seam is a
  package-private field declared with the state fields.
  A genuine failure still propagates out of `persist()`
  into `change()`'s existing handler (ledger marked
  failed, operation rethrown) — unchanged. Class doc
  gains the durability contract sentence.

**No message texts changed** in either class — the only
text edits are the two class-doc durability sentences,
which are source comments, not runtime output.

Semantics: on Linux nothing changes (each directory force
still runs; its failures behave exactly as before). On
Windows the marker/ledger writes succeed on the strength
of the temp-file fsync + atomic move, which the
diagnosis's PC probe proved work there.

Tree bookkeeping refreshed in place: `bosstrial/SOURCE.md`
(FSYNC FOLLOW-UP REBUILD section appended) and
`bosstrial/TEST-RESULTS.txt` (rewritten for this rebuild).
Total tree diff vs the first fsync rebuild's tree =
exactly 6 paths: `MarkerRegistry.java` (modified),
`TeamRewardLedger.java` (modified),
`MarkerRegistryDirectoryForceTest.java` (new),
`TeamRewardLedgerDirectoryForceTest.java` (new),
`SOURCE.md`, `TEST-RESULTS.txt`.

A closing sweep of the whole source tree confirms the
class is closed: the only remaining `FileChannel.open`
calls on a directory are the three sanctioned
`forceDirectory` implementations (AtomicStateFile,
MarkerRegistry, TeamRewardLedger), each invoked only
through its best-effort seam. (The `.lock`-file opens are
regular files and are unaffected on Windows.)

## Candidate jar (FINAL — the 0.5.6 of record)

- `~/workspace/bosstrial-056-fsync2-rebuild-20261114/ExcelBossTrial-0.5.6.jar`
  — **471,888 B / sha256
  `005bd005ff015dc7b7c1f9e271a3606e3601a9e2732cfc5ce603d8caea53449a`**
- **SUPERSESSION: this candidate supersedes `eb11a6e2…`
  (470,592 B) as the 0.5.6 of record.** Version stays
  **0.5.6** and the build ID stays
  `EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56` (0.5.6
  never shipped; this record carries the distinction, per
  the house pattern for rebuilt unshipped candidates).
- Deterministic: `package.sh` run three times (pkg1, pkg2,
  and a final build from the deliverable tree after the
  doc refresh) — all three jars byte-identical at
  `005bd005…`.
- Jar-entry diff vs `eb11a6e2…`: 242 → 244 entries;
  changed = `MarkerRegistry.class` and
  `TeamRewardLedger.class` only; added =
  `MarkerRegistry$DirectoryForce.class` and
  `TeamRewardLedger$DirectoryForce.class`; none removed.

## Tests

See `TEST-RESULTS.md`. Headline: full suite **2,792 checks
/ 101 harnesses, 0 failures** on all three fixed builds
(base 2,775 / 99 + new `MarkerRegistryDirectoryForceTest`
8 / 1 + new `TeamRewardLedgerDirectoryForceTest` 9 / 1).
Both new harnesses inject the exact Windows failure
(`AccessDeniedException` from the directory force) through
their class's production save path seam — a natural
reproduction is impossible on the Linux build host — and
pin that a genuine (non-platform) directory-force
`IOException` still fails the write with each class's
pre-existing failure semantics. The mirror rig's in-play
proofs remain the end-to-end Windows evidence, owed in
the P-FSYNC-1 acceptance.

## Filing note (facts the supervisor must use)

The HELD 0.5.6 install packet in the original build folder
(`~/workspace/bosstrial-056-build/`) cites the pre-fix jar
(`5cd9f76b…`); the first fsync rebuild already superseded
that with `eb11a6e2…`. **At filing/install time the
supervisor refreshes the packet's jar facts to THIS
candidate: `ExcelBossTrial-0.5.6.jar` — 471,888 B / sha256
`005bd005ff015dc7b7c1f9e271a3606e3601a9e2732cfc5ce603d8caea53449a`.**
Everything else in the packet (config merge, counter
seeding, boot lines, rollback) is unaffected by either
fsync change.
