# ExcelAutoSort 0.2.2 — records package staging notes

Prepped Nov 15, 2026 (worker W2, board supervisor run 392).
PREP ONLY — nothing was pushed or uploaded by this worker.
For publication as an OPEN DRAFT record PR in wrv-plugins at
`packages/excelautosort-0.2.2-20261115/` — never merged.
Expected branch: `record/excelautosort-0.2.2-20261115`.
Publication claim: wrv-plugins PR #___ (duplicate check run
2026-11-15T17:50:43Z, result NO MATCH for
'excelautosort-0.2.2') — PR number filled in at publish time
in the publish record, not in this file. Broad guard same
run (fragment 'excelautosort', exit 3): #230 (0.2.3), #174
(0.2.1), #153 (0.2.0), #151 (0.1.0), #132 (Bridge 0.1.0) —
no 0.2.2.

## Why this package exists (the gap)

Every other installed AutoSort jar version has its records:
0.1.0 PR #151, 0.2.0 PR #153, 0.2.1 PR #174, 0.2.3 PR #230
(run 391). 0.2.2 (BB-04 back-button fixes) was INSTALLED
Nov 15, 2026 (MANIFEST tombstone; full record in
MANIFEST-ARCHIVE.md) but never got a record PR or a Drive
deposit — flagged as BB-04's own separate leg in run 391
(W4 READINESS §2b; W10 PREP §1b). Drive check this prep
(2026-11-15T17:50:54Z, files list q=name contains
'ExcelAutoSort-0.2.2'): 8 AutoSort files returned (0.2.3
trio, 0.2.1 trio, 0.2.0 pair) — ZERO named 0.2.2. The gap
is real on both legs.

## Package

- File count: 54 files total on disk, including this
  RECORDS-STAGING.md, FILE-LIST.txt, and SHA256SUMS.
- SHA256SUMS: 53 entries — every file in the package except
  SHA256SUMS itself.
- Verification: `sha256sum -c SHA256SUMS` — see the prep
  record (PREP.md in the deliverable folder) for the
  verified count.
- Jar: NOT included in the package (following the BossTrial
  0.5.6 / AutoSort prior-record template; no .jar in the
  GitHub-bound file set). Candidate jar of record stays in
  the packet folder: ExcelAutoSort-0.2.2.jar — 160,197 B /
  sha256
  8b3bf5ab3b07119909e8f371361a21f32235b17039eb0f53b987f514fa18ad28,
  build EXC-AUTOSORT-0.2.2-WRV-20261114-01-8fdbf2.
  INSTALLED Nov 15, 2026 (see INSTALLED.txt + OUTCOME.txt).

## What was copied from where

Template: the prior AutoSort records packages (0.2.3 =
`packages/excelautosort-0.2.3-20261115/`, PR #230; 0.2.1
PR #174; 0.2.0 PR #153; 0.1.0 PR #151) — source tree
flattened at the package root, plus records overlays.

- Source tree (40 files): copied verbatim from
  `~/workspace/back-button-wave1-r354/BB-04/build/ExcelAutoSort-0.2.2/`
  — config.yml, plugin.yml, local-build.sh, src/ (36 .java),
  tests/ (1 .java). This is exactly the 40-file set inside
  ExcelAutoSort-0.2.2-source.zip (100,207 B /
  c2ff6c453264c810e5fc340ade2fac0a32a940f30b958fa0427565733ca288fe):
  every zip file entry was hash-compared against the build
  tree this prep — 40 checked, 0 missing, 0 mismatched.
  Closing `diff -r` of src/ and tests/ against the build
  tree was clean. Build-tree jars and `libs/` are NOT in
  the package. NOTE: unlike 0.2.3 there is no `menu/`
  folder — the ExcelMenu entry move was a 0.2.3 change;
  0.2.2 edited no ExcelMenu files.
- Build / install evidence (copied verbatim):
  - BUILD-RECORD.md from the build run's RESULT
    (back-button-wave1-r354/BB-04-RESULT.md — 35/35 tests,
    rig PASS SEVERE 0, walk 25/25, fingerprint PASS).
  - INSTALL-PACKET.md, RECEIPTS.md, INSTALLED.txt,
    OUTCOME.txt, SCRIPT-OUTPUT.txt from
    back-button-wave1-r354/BB-04/packet/ (SCRIPT-OUTPUT:
    18 verbatim install script blocks, all PASS).
  - WALK-OUTPUT.txt from back-button-wave1-r354/BB-04/rig/.
- Authored for this package: SOURCE.md, FINGERPRINTS.md,
  SOURCE-PROVENANCE.json, PUSH-NOTES.md, this
  RECORDS-STAGING.md.
- FILE-LIST.txt and SHA256SUMS were generated locally for
  this package.
- Deviations from the 0.2.3 shape, stated explicitly:
  (1) no SPEC.md and no MANIFEST-ENTRY-DRAFT.md (no 0.2.2
  SPEC increment was authored; install already DONE);
  (2) SCRIPT-OUTPUT.txt IS carried as a seventh evidence
  copy (the 0.2.3 package's install script output stayed
  in its install folder; BB-04's sits in the packet folder
  and is the install's primary verification bundle);
  (3) no `menu/` source file (see above).

## Tests

Unit suite: 35 found / 35 successful / 0 failed. Local
Paper rig: PASS, SEVERE 0. Code-trace walk: 25/25.
Install verification (failsafe, Nov 15, 2026): placed jar
+ merged config pull-backs EXACT, boot Done, SEVERE 0,
SCRIPT-OUTPUT 18/18 PASS.

## Still owed (outside this package)

- Publication: the supervisor's later leg — Homebase
  native-git lane, new OPEN DRAFT wrv-plugins PR (never
  merged), claim line above with the PR number filled in.
- Drive deposit of the jar / source zip / SOURCE note:
  local staging folder prepared at
  `~/workspace/autosort022-drive/`; the Drive upload itself
  is the supervisor's leg on the proven connector route
  (run-391 drive-route verdict).
- Player-facing surfaces for 0.2.2 (BB-04 What's New seq 3
  set) are already staged separately
  (~/workspace/board-r374/bb04-surfaces/); their filing is
  sequence-gated and does not gate this records leg.
  Player proof (Mike's spot taps) is a player-lane item.
