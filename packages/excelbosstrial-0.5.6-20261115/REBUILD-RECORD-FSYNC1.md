# REBUILD RECORD — ExcelBossTrial 0.5.6, Windows-safe sync fix (P-FSYNC-1, lane 1 of 4)

Built Nov 14, 2026 (parent-dispatched desk lane). Commission:
Mike's decision Nov 14, 2026 (~1:00 AM CST) choosing the plugin
code fix over a WSL2 rig move; backlog P-FSYNC-1. Desk only:
no installs, no MANIFEST edits, no live-server contact, no
sends. The rebuilt candidate stays HELD under the original
release condition — it installs only after the Nov 14 Warden
attempt, in the supervisor's filing/install flow, superseding
the pre-fix 0.5.6 candidate.

## ⚠️ Brief-vs-disk discrepancy (surfaced, resolved on evidence)

The dispatch brief named the source as
`~/workspace/bosstrial-056-build-20261113/` with a candidate
of 132,474 B / SHA-256 `4c3b9191…`. **Neither exists on disk:**
a full-workspace search found no such folder, no file
mentioning `4c3b9191` in any .md/.txt, and no jar of 132,474 B
anywhere. (The backlog's P-FSYNC-1 entry repeats the same
wrong folder path — worth correcting there too.) The 0.5.6
build of record is `~/workspace/bosstrial-056-build/`, whose
BUILD-RECORD.md, INSTALL-PACKET.md (paperwork-fixed Nov 13),
SOURCE.md and SHA256SUMS all agree, and whose candidate this
lane re-verified with `sha256sum -c SHA256SUMS` = 6/6 OK
before and after the work:

- **Base candidate:** `ExcelBossTrial-0.5.6.jar` — 469,839 B /
  sha256 `5cd9f76b729dd53567ab7d7b7ae97e70a326e6a1784f34283282793782c0409c`
- Build ID: `EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56` (unchanged)

The lane proceeded against that verified base; the mandated
base gate below was run against `5cd9f76b…`.

## Diagnosis (of record)

`~/workspace/mirror-storage-diagnosis-20261114/DIAGNOSIS.md`:
BossTrial's `AtomicStateFile` ends its durable save by
fsync-ing the containing directory
(`FileChannel.open(dir)` + `force()`). Legal on Linux (the
live server — unaffected); on Windows Java NIO the open
itself throws `AccessDeniedException`, so on the PC
test-server mirror every state save failed AFTER the write
landed, and the plugin locked its state writes
(self-protection) — the Nov 12 + Nov 13 mirror boots' ERROR
`Could not durably save state.yml; writes locked pending
restart/inspection`.

## Base gate (house pattern — PASSED before any edit)

The `bosstrial/` tree was copied verbatim into this folder
(`diff -r` clean at copy time; the original folder was never
modified — closing `sha256sum -c` there is still 6/6 OK).
`package.sh` on the UNMODIFIED copy (JDK 25,
`~/workspace/.jdk25`, classpath per the original build's
`work/classpath.txt`) produced:

- `work/base-gate.jar` — **469,839 B / sha256
  `5cd9f76b729dd53567ab7d7b7ae97e70a326e6a1784f34283282793782c0409c`**,
  byte-identical to the candidate of record.
- Suite green during that build: **2,765 checks / 98
  harnesses** (`work/tally.py`, copied from the original
  build) — exactly the original build record's tally.

## The change (only this)

`src/com/winkreyes/excelbosstrial/core/AtomicStateFile.java`
— full diff in `ATOMICSTATEFILE.diff` beside this record.
Code shape:

- The post-move directory fsync is extracted into
  `private static void forceDirectory(Path)` (the original
  open+force, verbatim) behind a new package-private
  `@FunctionalInterface interface DirectoryForce` seam. The
  public `write(...)` entry points delegate to a
  package-private 4-arg `write` whose last parameter is the
  `DirectoryForce` (production passes
  `AtomicStateFile::forceDirectory`) — the same injection
  shape this class already uses for its `Probe`.
- In the write path, the directory-force call is wrapped:
  `try { directoryForce.force(parent); } catch
  (AccessDeniedException …) { /* file fsync already carried
  this write */ }` — and only that exception is excused.
- Nothing else moves: temp-file write + `force(true)` stays
  mandatory and uncaught; the atomic move stays mandatory;
  probe phases (`before-write`, `before-move`, `after-move`,
  `after-force`) unchanged, with `after-force` still reached
  after an excused force and still skipped when a genuine
  failure propagates.
- The class doc comment is rewritten to state the new
  contract truthfully (best-effort ONLY for the platform
  refusal; any other directory-force failure still fails the
  write). **No message texts changed** — no player-facing or
  log text exists in this class; the host's degraded-state
  log line (`Could not durably save state.yml; writes locked
  pending restart/inspection`) is untouched and simply stops
  triggering for the Windows refusal, because the write now
  succeeds.

Semantics: on Linux nothing changes (the directory force
still runs; its failures behave exactly as before). On
Windows the write succeeds on the strength of the
temp-file fsync + atomic move, which the diagnosis's PC
probe proved work there.

Tree bookkeeping refreshed in place: `bosstrial/SOURCE.md`
(fsyc-rebuild section appended) and
`bosstrial/TEST-RESULTS.txt` (rewritten for this rebuild).
Total tree diff vs the original tree = exactly 4 paths:
`AtomicStateFile.java` (modified), `AtomicStateFileTest.java`
(new), `SOURCE.md`, `TEST-RESULTS.txt`.

## Candidate jar (FINAL for this lane)

- `~/workspace/bosstrial-056-fsync-rebuild-20261114/ExcelBossTrial-0.5.6.jar`
  — **470,592 B / sha256
  `eb11a6e2f2c74436a505a7199c2f5a686fe45742f00257549d61530f712bd265`**
- Version stays **0.5.6** and the build ID stays
  `EXC-BOSSTRIAL-0.5.6-WRV-20261111-01-3c9e56` (0.5.6 never
  shipped; this record carries the distinction, per the
  house pattern for rebuilt unshipped candidates).
- Deterministic: `package.sh` run three times (pkg1, pkg2,
  and a final build from the deliverable tree after the doc
  refresh) — all three jars byte-identical at `eb11a6e2…`.
- Jar-entry diff vs the pre-fix candidate: 241 → 242
  entries; changed = `AtomicStateFile.class` only; added =
  `AtomicStateFile$DirectoryForce.class`; none removed.

## Tests

See `TEST-RESULTS.md`. Headline: full suite **2,775 checks /
99 harnesses, 0 failures** on both deterministic builds
(base 2,765 / 98 + new `AtomicStateFileTest` 10 / 1). The
new harness injects the exact Windows failure
(`AccessDeniedException` from the directory force) through
the production write path's seam — a natural reproduction is
impossible on the Linux build host, since Linux directory
channels open and force fine — and pins that a genuine
(non-platform) directory-force `IOException` still fails the
write. The mirror boot itself remains the end-to-end Windows
proof, owed in the P-FSYNC-1 acceptance (mirror boots with
zero degradation ERRORs once all four fixed jars are in).

## Observation for the supervisor (out of this lane's scope)

Two OTHER directory-force sites exist in this same jar,
outside the diagnosis's named BossTrial touch point and
outside this lane's "only this" change; neither fires at
boot (they fire on in-play writes), so neither affects the
mirror-boot acceptance — but on a Windows rig both would
fail the same way during play:

- `com.winkreyes.teamrewards.marker.MarkerRegistry` — dir
  force after the atomic move, wrapped so any failure marks
  persistence failed (`marker persistence uncertain`).
- `wrv.rewards.TeamRewardLedger` — dir force after the
  atomic move inside its save try.

If Mike wants the whole jar Windows-clean rather than the
diagnosed state path only, these two are a small follow-up
in the same shape.

## Filing note

The HELD 0.5.6 install packet in the original build folder
cites the pre-fix jar (`5cd9f76b…`). At filing/install time
the supervisor refreshes the packet's jar facts to this
rebuild (`eb11a6e2…`, 470,592 B); everything else in the
packet (config merge, counter seeding, boot lines, rollback)
is unaffected by this change.
