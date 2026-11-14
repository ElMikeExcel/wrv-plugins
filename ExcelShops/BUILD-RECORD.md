# BUILD RECORD — ExcelShops 1.3.9 (Windows-safe directory sync)

Desk build, FSYNC FIX lane 3 of 4 (backlog P-FSYNC-1), Nov 14, 2026.
Desk side only: no install, no MANIFEST edit, no live-server contact,
no publishing was done or attempted.

**Deliverable:** `ExcelShops-1.3.9.jar` — **266,677 B** — SHA-256
`9473f1ed369a09512b689eed5a23c588fb3e9c20e8c251558a1ac56ed7ea8e73`
— 108 classes. Deterministic: two independent builds packaged
byte-identical (see §4). Tests **433/433** on both builds (430 carried
+ 3 new; see TEST-RESULTS.md).

**Why:** the Nov 14, 2026 mirror diagnosis
(`~/workspace/mirror-storage-diagnosis-20261114/DIAGNOSIS.md`) proved
the ExcelShops checkout store's durable fence fsyncs the containing
**directory** (`FileChannel.open(directory)` + `force()`). That open
throws `AccessDeniedException` under Windows Java NIO, so on the PC
test-server mirror ExcelShops drops to protection-only mode at enable
("ExcelShops entered protection-only mode; purchases/setup
unavailable") even though its data check reads the store fine
(EXISTING_TARGET — the data was never corrupt). The live Linux server
is unaffected. Mike chose the plugin code fix (Nov 14, 2026).

## 1. Source tree identification (verified current)

- Tree used: `~/workspace/shops138-rebase-20261111/tree-1.3.8` — the
  shipped 1.3.8 source, copied verbatim into this folder's
  `tree-1.3.9/` before any edit. The prior build folder is untouched.
- Currency evidence: `shops138-rebase-20261111/BUILD-RECORD-1.3.8.md`
  (266,417 B / SHA-256 `b8a1b8fd…`) and `INSTALLED.txt` in the same
  folder (1.3.8 installed live Nov 11, 2026; live boot shows
  "Enabling ExcelShops v1.3.8"). The Nov 13 golden-shovel packet was a
  catalog data change only and itself cites "ExcelShops 1.3.8 (live
  since Nov 11)" and this tree. Diagnosis §3 confirms live = 1.3.8.
  No newer complete tree exists on disk.

## 2. Base gate — PASSED

The unmodified copied tree was built first (`work/out-base`) and
packaged with the original `package-1.3.8.py`:
**266,417 B, SHA-256
`b8a1b8fd9a56f420accdb066a3ae3e9c6989638668cadd2f5497a52117a4adb9`** —
byte-identical (`cmp` clean) to the shipped
`shops138-rebase-20261111/ExcelShops-1.3.8.jar`. Base test run:
430/430. All edits below were made only after this gate passed.

## 3. The change (only this)

Every directory-force site now treats a **platform refusal** as
best-effort: if opening/forcing the directory throws
`AccessDeniedException` (the Windows refusal, proven by the diagnosis
probe) or `UnsupportedOperationException`, the step completes on the
file-level fsync, which stays mandatory and unchanged. Any other
`IOException` from the directory force still propagates exactly as
before, so genuine durability failures on Linux keep their existing
semantics (FileJournal recovery flagging, LegacyDataMigration's final
barrier). Linux behavior is otherwise unchanged: the force still runs
and still succeeds there.

Sites fixed (4, in 3 files):

1. `checkout/FileJournal.java` — `syncDirectory()` (constructor fence
   + clean-close path). Also replaced its now-false comment
   ("Refuse to start when the filesystem cannot provide required
   directory durability.") with the best-effort rationale. This is a
   comment change only; **no log line or player-visible text changed
   anywhere in this build.**
2. `checkout/GlobalSessionGuard.java` — `syncDirectory()` now
   delegates to a new `syncDirectory(Path)` carrying the guarded body.
3. `checkout/GlobalSessionGuard.java` — `archiveRecoveryEvidence()`:
   the inline `FileChannel.open(audit, READ)` + `force(true)` block
   replaced by `syncDirectory(audit)`.
4. `LegacyDataMigration.java` — `syncDirectory(Path)` (keeps its
   `NOFOLLOW_LINKS` open option). Its final-barrier catch in
   `migrate()` is untouched and still fires for any non-refusal
   IOException, exactly as designed.

Site found already tolerant (no change):
`store/RegistryFile.java` `write()` already wraps its directory force
in `catch (UnsupportedOperationException | IOException ignored)` with
the comment "Linux supports directory fsync; some other providers do
not." — the idiom this fix mirrors. A full-tree sweep for
`.force(` / directory `FileChannel.open` found no other sites;
`PurchaseLog` and `DataFileLock` open files only.

Not touched (per task limits): offers, prices, catalog, transaction
logic, all messages. Source diff vs tree-1.3.8 is exactly: the 3 files
above, `plugin.yml`, `pom.xml`, one version assertion in
`tests/.../AdminCommandTest.java`, and the new test file (§5).
Class-level delta vs the base build: only the 3 fixed files' classes
(10 class files incl. nested) + `plugin.yml`; the other 97 classes
are byte-identical to the base-gate build; 108 classes both sides.

**Version:** 1.3.8 → **1.3.9** (`plugin.yml` version + description
append noting the fix, `pom.xml`, AdminCommandTest version
assertion).

## 4. Build & determinism

Harness: the fleet `build.sh` copied from the 1.3.8 folder (JDK 25,
`--release 21`, same classpath). Packager: `package-1.3.9.py`
(1.3.8's deterministic packager; stamp 2026-11-14, same manifest
bytes). Two independent builds (`work/out-139-a`, `work/out-139-b`)
each ran the full suite green and packaged to the identical SHA-256
above. **Deterministic verdict: DETERMINISTIC.**

Source zip: `ExcelShops-1.3.9-source.zip` — 75 files (the tree-1.3.9
project under an `ExcelShops/` prefix, fixed timestamps) —
193,405 B — SHA-256 in `SHA256SUMS`.

## 5. Tests (detail in TEST-RESULTS.md)

430 carried tests + 3 new in
`tests/com/winkreyes/shops/checkout/DirectorySyncBestEffortTest.java`,
which simulates the exact Windows refusal (Mockito static mock:
`FileChannel.open` on any directory throws `AccessDeniedException`;
file opens run real). The 3 scenarios — FileJournal open/clean-close,
GlobalSessionGuard recordReady/closeClean, and a full
LegacyDataMigration migration — **all pass on 1.3.9 and all fail on
the unmodified 1.3.8 classes** with the simulated exception
(discrimination verified by running the new test class against the
base-gate classes: 0 passed / 3 failed).

**Rig verification still owed (supervisor's rig step):** boot 1.3.9
on the PC test-server mirror and confirm ExcelShops reaches
"ExcelShops purchases available." instead of protection-only mode,
with a clean boot log. The unit simulation proves the code path; the
mirror boot is the end-to-end proof on real Windows NIO.

## 6. Scope limits honoured

Desk build only. No install, no MANIFEST edit or filing, no panel,
no live-server contact, no sends, no publishing. The candidate waits
for the supervisor's normal build → rig → install discipline.
