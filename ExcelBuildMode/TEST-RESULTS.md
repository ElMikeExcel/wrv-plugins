# TEST RESULTS — ExcelBuildMode 1.3.5 (Windows-safe directory sync)

All runs via `bash build.sh` (compiles, runs every suite, jars with the
pinned date) against Paper 26.2 build 123 / JDK 25, per the 1.3.4 line.

## Base gate — unmodified 1.3.4 tree, before any edit

The copied tree in this folder was built untouched first:

- Exit 0; all eight suites PASS at exactly the 1.3.4 recorded totals
  (5,710 / 5,283 / 84 / 145 / 338 / 18 / 82 / 25 = 11,685 assertions).
- Jar reproduced at 99,073 B, SHA-256
  `39d86f3bc76ee7f28ff5bd428e1c8add9d07d5f9bedda5d1a84551c36f7464d3`
  — byte-identical to the on-record 1.3.4 candidate in
  `~/workspace/buildmode-134-build/SHA256SUMS` and to the jar its
  INSTALLED.txt records as installed live on Nov 10, 2026.

## 1.3.5 runs — three builds, one hash

Run 1 (in place), run 2 (in place, determinism), run 3 (clean-room:
fresh copy of src + build.sh + README + docs, no `target/`). All three
exit 0, all nine suites PASS with identical totals, and all three jars
are byte-identical: 99,879 B, SHA-256
`aa2d35fd4c2fb2a1839c33d2153f96be52c06c02e5e75ec144d27f1911ab8a66`.

| Suite | Assertions | Note |
|---|---|---|
| CoreTests | 5,710 | carried, unmodified, at 1.3.4 total |
| RewardTests | 5,283 | carried, unmodified, at 1.3.4 total |
| RecoverySafetyTests | 84 | carried, unmodified, at 1.3.4 total |
| PlayerDataProofTests | 145 | carried, unmodified (PlayerDataProof untouched) |
| PilotGateTests | 338 | carried, unmodified, at 1.3.4 total |
| BuildReturnTests | 18 | carried, unmodified, at 1.3.4 total |
| AdminTests | 82 | carried; version pin moved 1.3.4 → 1.3.5 only |
| ReviewedTests | 25 | carried; version pin moved 1.3.4 → 1.3.5 only |
| **DirectorySyncTests** | **14** | **NEW** |
| **Total** | **11,699** | |

## DirectorySyncTests (14) — what the new coverage proves

- **The failure is real (2):** the unguarded idiom — `FileChannel.open`
  on a path that cannot be opened, then `force(true)` — throws
  `IOException` (the class of the Windows `AccessDeniedException`),
  and the failed probe creates nothing.
- **The tolerance holds (4):** `DirectorySync.bestEffort` returns
  normally on exactly that unopenable path, tolerates it repeatedly,
  returns normally on a real directory, and creates nothing as a side
  effect.
- **Saves land through the tolerant force (8):** ReviewHoldStore hold +
  read-back from a new instance (2); BuildReturnStore record round-trip
  + clear (2); RecoveryJournal begin → active with snapshot equality →
  complete → inactive (2); ActiveSessionIndex begin → tombstone (2).
  Every store save path that previously ended in a fatal directory
  force now completes on its mandatory file-level fsync alone when the
  directory force is unavailable.

## Not proven here (owed at the rig, supervisor's step)

The Windows half cannot execute on this Linux VM: a mirror boot with
1.3.5 — with `review-holds\` and `build-return\` provisioned as the
live installation provisions them — must show zero ExcelBuildMode
storage ERRORs and Build entry enabled. The diagnosis's PC probe
already proved the platform behavior the fix targets (directory open
refused, file fsync fine); the mirror boot is the end-to-end check.
