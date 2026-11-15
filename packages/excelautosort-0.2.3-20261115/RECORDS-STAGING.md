# ExcelAutoSort 0.2.3 — records package staging notes

Prepped Nov 15, 2026 (worker W10, board supervisor run 391).
PREP ONLY — nothing was pushed or uploaded by this worker.
For publication as an OPEN DRAFT record PR in wrv-plugins at
`packages/excelautosort-0.2.3-20261115/` — never merged.
Expected branch: `record/excelautosort-0.2.3-20261115`.
Publication claim: wrv-plugins PR #___ (duplicate check run
2026-11-15T17:32:54Z, result NO MATCH for
'excelautosort-0.2.3') — PR number filled in at publish time
in the publish record, not in this file. (The readiness
audit's earlier guard run, 2026-11-15T17:26:54Z, also
returned NO MATCH.)

## Package

- File count: 55 files total on disk, including this
  RECORDS-STAGING.md, FILE-LIST.txt, and SHA256SUMS.
- SHA256SUMS: 54 entries — every file in the package except
  SHA256SUMS itself.
- Verification: `sha256sum -c SHA256SUMS` — see the prep
  record (PREP.md in the deliverable folder) for the
  verified count.
- Jar: NOT included in the package (following the BossTrial
  0.5.6 / AutoSort prior-record template; no .jar in the
  GitHub-bound file set). Candidate jar of record stays in
  the packet folder: ExcelAutoSort-0.2.3.jar — 161,791 B /
  sha256
  0457557e15c3de52033be300bc7f5bb32991268f3005544d7ec0fe15965b130b,
  build EXC-AUTOSORT-0.2.3-WRV-20261114-01-c41a07.
  INSTALLED Nov 15, 2026 (see INSTALLED.txt + OUTCOME.txt).

## What was copied from where

Template: the prior AutoSort records packages (0.2.1 =
`packages/excelautosort-0.2.1-20261109/`, PR #174; 0.2.0
PR #153; 0.1.0 PR #151) — source tree flattened at the
package root, plus records overlays.

- Source tree (42 files): copied verbatim from
  `~/workspace/board-r362/autosort-filter/build/ExcelAutoSort-0.2.3/`
  — config.yml, plugin.yml, local-build.sh,
  menu/autosort.yml, src/ (37 .java), tests/ (1 .java).
  This is exactly the 42-entry set inside
  ExcelAutoSort-0.2.3-source.zip (328,378 B /
  0258cbc105bb2a5e2ad580cd8ca4eae0bfe190cbe47d45ee585f49d4ec5cfa7d).
  Closing `diff -r` of src/ and tests/ against the build
  tree was clean. Build-tree jars and `libs/` are NOT in
  the package.
- Build / install evidence (copied verbatim):
  - BUILD-RECORD.md from the build run's RESULT.md
    (board-r362/autosort-filter/RESULT.md — 39/39 tests,
    rig PASS SEVERE 0, walk 20/20, fingerprint PASS).
  - INSTALL-PACKET.md from board-r362/autosort-filter/.
  - RECEIPTS.md + INSTALLED.txt from
    board-r362/autosort-filter/packet/.
  - WALK-OUTPUT.txt from board-r362/autosort-filter/.
  - OUTCOME.txt from
    board-r362/autosort-filter/install-20261115/ (failsafe
    install Nov 15, 2026; placed jar / config / menu entry
    pull-backs all EXACT).
- Authored for this package: SOURCE.md, FINGERPRINTS.md,
  SOURCE-PROVENANCE.json, PUSH-NOTES.md, this
  RECORDS-STAGING.md.
- FILE-LIST.txt and SHA256SUMS were generated locally for
  this package.
- Deviation from the 0.2.1 shape, stated explicitly: no
  SPEC.md and no MANIFEST-ENTRY-DRAFT.md are carried. No
  0.2.3 SPEC increment was authored (the design of record
  is BUILD-RECORD.md + INSTALL-PACKET.md), and the install
  is already DONE, so no manifest draft is owed.

## Tests

Unit suite: 39 found / 39 successful / 0 failed (35
pre-existing + 4 new toggle tests). Local Paper rig: PASS,
SEVERE 0. Code-trace walk: 20/20. Landing verification
(run 390): PASS 20/20, SEVERE 0.

## Still owed (outside this package)

- Publication: the supervisor's later leg — Homebase
  native-git lane, new OPEN DRAFT wrv-plugins PR (never
  merged), claim line above with the PR number filled in.
- Drive deposit of the jar / source zip / SOURCE note:
  local staging folder prepared at
  `~/workspace/autosort023-drive/`; the Drive upload itself
  rides the proven-route verdict (run-391 drive-route
  check), same gate as the BossTrial 0.5.6 deposit.
- ExcelAutoSort 0.2.2 (BB-04) records gap: NO record PR
  exists for 0.2.2 (broad guard: #174 0.2.1, #153 0.2.0,
  #151 0.1.0, #132 Bridge only). That is BB-04's own leg,
  flagged in the run-391 readiness audit — this 0.2.3
  package does not absorb or fix it.
- Player-facing surfaces for 0.2.3 (What's New BOTH,
  Guide) are a separate staging/filing leg (provisional
  seq 19); player proof (Mike's spot taps) is a player
  lane item. Neither gates this records leg.
