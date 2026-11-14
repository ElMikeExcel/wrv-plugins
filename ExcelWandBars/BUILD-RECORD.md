# BUILD RECORD — ExcelWandBars 0.3.18 (Windows-safe directory sync)

Desk build, backlog P-WANDBARS-FSYNC-1, Nov 14, 2026 (board supervisor
run 329, follow-on lane). Desk side only: no install, no MANIFEST edit,
no live-server contact, no PC/mirror work, no publishing.

**Deliverable:** `ExcelWandBars-0.3.18.jar` — **554,089 B** — SHA-256
`cadb16a6e70ebcb4236302dfa6c2b67ae23a0ba1b03ac277f57ead9a9d6e1f11`
— build ID `EXC-WANDBARS-0.3.18-WRV-20261114-01-e17db9`
(suffix = first 6 hex of SHA-256 of "EXC-WANDBARS-0.3.18-dirfsync").
Deterministic: two independent builds byte-identical (see §4).
Tests **679/679** on both builds (669 carried + 10 new; TEST-RESULTS.md).

**Why:** the mirror follow-on diagnosis
(`~/workspace/fsync-mirror-followon-20261114/FOLLOWON.md` §W) proved
`wrv.magic.retention.AuthenticatedWindow.commit()` performs an
unguarded directory fsync after its atomic move. On Windows Java NIO
the directory force throws `AccessDeniedException` AFTER the move
lands; the catch-all wraps it as "durable issuance uncertain;
close/reconcile", PracticeBootstrap fences managed scopes and disables
the plugin on the mirror. Reproduced in isolation on the PC's JDK 25.
The live Linux server is unaffected (directory fsync succeeds there).

## 1. Source tree identification (verified current)

- Base tree: the shipped **0.3.17** sources in
  `~/workspace/wandbars-verify-race-build/build-0.3.17/work/` (the 96
  files listed in its `work/sources.txt`), copied verbatim into this
  folder's `tree-0.3.18/` before any edit. The prior build folder is
  untouched. Currency evidence: `build-0.3.17/BUILD-NOTES.md` +
  `SOURCE.md` (0.3.17 jar 553,827 B / `949e9cd4…`, installed packet
  `packet-0317-20261112`) and the follow-on's §W1 (throw site identical
  across every WandBars tree on disk).

## 2. Base gate — PASSED

The unmodified copied tree was compiled first (`work/out-base`,
Temurin JDK 25.0.5+8, `work/cp.txt`, no `--release` — the shipped
classes are class-file v69, Geyser hooks pinned at v65 by repack.py)
and repacked with `work/repack.py` from the shipped 0.3.16 jar plus
the 0.3.17 resources (`work/shipped-0317`):
**553,827 B, SHA-256
`949e9cd48790cd10bb2861ce32f4cc3442410aee219e91bbc48f6cb526330c9b`** —
byte-identical (`cmp` clean) to the shipped 0.3.17 jar
(`work/basecheck/rebuilt-0.3.17.jar` vs `shipped-0.3.17.jar`), with
exactly the 10 changed-vs-0.3.16 entries BUILD-NOTES records.
All edits below were made only after this gate passed.

## 3. The change (only this)

Every directory-force site now treats a **platform refusal** as
best-effort, mirroring the sibling fsync fixes (e.g. ExcelShops 1.3.9
`GlobalSessionGuard.syncDirectory` / `FileJournal.syncDirectory`):
the directory force is wrapped in
`catch (AccessDeniedException | UnsupportedOperationException)` around
the directory force ONLY. The file-level fsync (before the atomic
move) stays mandatory and unchanged; any other `IOException` from the
directory force still propagates exactly as before (AuthenticatedWindow
still wraps it as "durable issuance uncertain", BoundedPractice still
locks, PracticeIdentityJournal still marks uncertain), so genuine
durability failures on Linux keep their existing semantics. Linux
behaviour is otherwise unchanged: the force still runs and still
succeeds there.

**Sweep (jar-wide, whole 0.3.17 tree):** grep for `FileChannel.open`
on directories / `.force(` patterns over all 96 sources found
**3 directory-force sites** — the follow-on named only the first;
the other two are the same post-move pattern in sibling stores:

1. `src/wrv/magic/retention/AuthenticatedWindow.java` — `commit()`
   (the diagnosed site).
2. `src/wrv/magic/retention/BoundedPractice.java` — `persist()`
   (found by this sweep; would have failed identically on Windows
   at the first credited practice use).
3. `src/com/winkreyes/excelwandbars/PracticeIdentityJournal.java` —
   `persist()` (found by this sweep; same failure at the first
   practice-identity write).

No other directory-force sites exist: the remaining `.force(` calls
are file channels (mandatory, untouched) and the remaining
`FileChannel.open` calls open `.lock` files. Per-site diffs are in
`diffs/`.

**Version:** 0.3.17 → **0.3.18** (`BuildInfo.java`,
`work/shipped-0318/plugin.yml` version + one appended description
sentence with no ": " — YAML parse-verified locally and inside the
rebuilt jar — and `work/shipped-0318/excel-build.txt`).
`config.yml` / `spellbook.yml` are byte-identical to 0.3.17's.

**Class-level delta vs the shipped 0.3.17 jar: exactly 6 entries** —
the 3 fixed classes, `BuildInfo.class`, `plugin.yml`,
`excel-build.txt`. The other 245 entries are byte-identical.

## 4. Build & determinism

Harness: `build.sh` in this folder (JDK 25, `javac -proc:none` over
`work/cp.txt`, repack via the 0.3.17 `work/repack.py` from the shipped
0.3.16 jar — the 0.3.12–0.3.17 method). Two independent builds
(`work/out-0318-a`, `work/out-0318-b`) each packaged to the identical
SHA-256 above. **Deterministic verdict: DETERMINISTIC.**

Source zip: `ExcelWandBars-0.3.18-src.zip` — 96 files (the
`tree-0.3.18/src/` tree exactly as compiled, fixed 2026-01-01
timestamps, sorted) — 285,206 B — SHA-256 in `SHA256SUMS`.
Tests live in `tree-0.3.18/tests/` (not in the source zip, matching
the 0.3.17 source-zip shape).

## 5. Tests (detail in TEST-RESULTS.md)

669 carried checks (23 suites, unchanged files) + 10 new in
`tree-0.3.18/tests/DirectorySyncBestEffortTest.java`, which simulates
the exact Windows refusal (Mockito static mock: `FileChannel.open`
on any directory throws; file opens run real) for BOTH refusal types
(`AccessDeniedException`, `UnsupportedOperationException`) across
ALL 3 fixed sites, plus a no-refusal control per site and a reopen
check. **All pass on 0.3.18; on the unmodified 0.3.17 classes the new
suite fails immediately** with the exact production symptom
(`IOException: durable issuance uncertain; close/reconcile`, caused
by `AccessDeniedException` — `work/discrim-base.log`).

**Rig verification still owed (separate lane, per task):** boot
0.3.18 on the PC test-server mirror and confirm practice issuance
completes, no "durable issuance uncertain" ERROR, plugin stays
enabled. The unit simulation proves the code path; the mirror boot
is the end-to-end proof on real Windows NIO.

## 6. Scope limits honoured

Desk build only. No install, no MANIFEST edit or filing, no panel,
no live-server contact, no PC/mirror work, no sends, no publishing,
no backlog/state edits. The candidate waits for the supervisor's
normal build → rig → install discipline.
