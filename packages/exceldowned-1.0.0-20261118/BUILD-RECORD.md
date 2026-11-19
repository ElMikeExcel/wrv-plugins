# ExcelDowned 1.0.0 — BUILD RECORD (Revive system)

- Assembled at records close-out, Nov 18, 2026, from the build
  reports of record: board run 509 W5 (shop charge leg, base
  1.0.0 tree), board run 510 W5 (mirror rig) and board run 510
  W7 (F1 fix build). Filing: ~/workspace/downed-100-filing/
  (INSTALL-PACKET.md + RECEIPTS.md + FILING-RECORD.md).
- Source of record (hash-verified at this close-out): the
  F1-fix tree ~/workspace/board-r510/f1-fix/build-1.0.0/.
  Its jar is byte-identical to the staged + installed jar.
  The pre-fix tree (~/workspace/downed-016-build-20261117/
  work/build-1.0.0/, jar ed157aee...) is the base, NOT the
  shipped source.

## Base gate

Unmodified rebuild of the run-509 candidate tree in the F1
copy reproduced the candidate EXACT: 139,752 B /
ed157aeea4286d50adb0b34395173c746275e98ef8697c4c4c236ca6b2a4ba93,
tests 70/70, fingerprint gate PASS (W7 §2).

## F1 fix (the only delta on the shipped jar)

One production file: RevivePurchaseJournal.java — the
directory fsync in the journal-creation branch is now
best-effort (AccessDeniedException |
UnsupportedOperationException swallowed, in the ExcelShops
1.3.9 syncDirectory shape); the journal file's own
force(true) stays unconditional. Fixes the first-boot
"purchase journal unavailable" WARN seen on the Windows
mirror rig (run 510 W5 finding F1). Diffs: F1-source.patch
+ F1-test.patch in this package. New pinning test
journalDirectorySyncBestEffort (suite 70 -> 71).

## Candidate

- ExcelDowned-1.0.0.jar — 140,683 B, SHA-256
  fcfa96d2036926b0eee5eaf338196ceee55680ca56e9df415563e35b1990e57c
- Two independent local-build.sh runs: jar + repro
  byte-identical in every run; tests 71/71 both runs;
  fingerprint gate PASS, build
  EXC-DOWNED-1.0.0-WRV-20261117-01-d0wn3d.
- Config candidate: 4,916 B /
  94eda94a6ff9b9c23d11936315703199fdf8aabd165283266a035d4cbc87a3bf
  — pure superset of the live 0.1.5 config (one addition
  hunk, zero live lines changed; RECEIPTS.md diff).

## Feature summary (what 1.0.0 adds over 0.1.5)

- Revive Potion: passive carry item, sold ONLY in the Excel
  Shop for $5,000 (config price is the single truth); the
  buy verb is 'downed revivepotion buy' in ExcelDowned
  (D109); walk-up Revive pop-up consumes 1 potion, revives
  at 50% HP; per-player use cooldown 600 s.
- Cleric Raise Ally: Cleric class active + EliteMobs level
  10, 85 GRACE via ClassResourceController.trySpend (D107),
  cooldown 600 s, success-only.
- All tunables config-editable.
