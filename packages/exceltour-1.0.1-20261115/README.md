# ExcelTour 1.0.1 — records package (BB-09)

Plugin: ExcelTour 1.0.1, build EXC-TOUR-1.0.1-WRV-20261114-01-bb09.
Unit: BB-09 back-button fixes, 3 rows (0 FIX-1 / 3 FIX-2), from
back-button-audit-20261113 AUDIT.md (ExcelTour 1.0.0) via
FIXBATCH-SPLIT-R348.md section 2.
Status: Installed Nov 15, 2026, by wrv-install-failsafe run
scheduled 2026-11-15 05:33 CST (see INSTALLED.txt / OUTCOME.txt).
This folder is the wrv-plugins records package, assembled
2026-11-15 (board run 409, worker W10) and published
2026-11-15 (run 409, worker W11) as an OPEN DRAFT PR — see
RECORDS-STAGING.md for the publication shape and the
G2 duplicate-guard result, and the publication RECORD.md
(board-r409/tour-records/) for the PR number + claim line.

## What 1.0.1 changes (from INSTALL-PACKET.md)

- Guide view (TourMenu inventory, Java + Bedrock fallback):
  slot 26 Close barrier becomes "Back to Server Menu" (ARROW)
  in all states, wired to the ExcelMenu /menu opener.
- Bedrock tour step form gains a last button "Back to Server
  Menu" (action backmenu, pure navigation — no engine call,
  no save, tour progress untouched mid-tour).
- Does NOT change: core tour engine, triggers, routes, config,
  player progress data format, commands, or the Java action-bar
  + chat step presentation (audit Not-a-defect row). No config
  or data migration; existing plugins/ExcelTour/ data carries
  over untouched.

## Artifacts

- ExcelTour-1.0.1.jar — 50,908 B — SHA-256
  ab94d4a12b33b8acb45b60599ef3b27eac4f0b86805456930719ba90e2c0e317
  Deterministic double-build byte-identical (BUILD-RECORD.md).
  Placed live as plugins/ExcelTour-1.0.1.jar; pull-back EXACT
  (OUTCOME.txt). Rollback jar retained at install as
  plugins/ExcelTour-1.0.0.jar.bak-20261115-pre101 (50,656 B /
  4af7c455d98ead43603f75fe007444983c75345d70f0109c9149dc785c78dd72).
- ExcelTour-1.0.1-source.zip — 39,081 B — SHA-256
  b695249069e1ed29804bf7a48ee11c9e24afe06fa10133e91af32cb6b8a4c3e6
  31 entries (source tree + tests + plugin.yml + config.yml +
  routes + build.sh; 108,355 B uncompressed).

## Tests and install evidence

- Tests: 59/59 — carried TourTest 42/42 at exactly the 1.0.0
  count; new BackNavTest 17/17 incl. the mid-tour Back
  progress-survival walk (TEST-REPORT.md).
- Rig: local Paper 26.3 rig (Geyser + floodgate co-loaded):
  Done, SEVERE 0, enable line prints the 1.0.1 build ID;
  TourRig exercise 4 PASS / 1 FAIL, with the identical 4/1 on
  the unmodified 1.0.0 control — a rig/environment property,
  not a delta regression (TEST-REPORT.md).
- Install: SCRIPT-OUTPUT.txt — 17 blocks, all RESULT: PASS;
  boot "Enabling ExcelTour v1.0.1" x1, enabled line exact,
  /tour version prints the 1.0.1 build ID, SEVERE 0.
  Guild state unchanged: ExcelGuildGate 5 of 5 seals broken,
  guild OPEN, state.yml byte-identical pre/post (OUTCOME.txt
  notes the packet's stale "LOCKED 4 of 5" premise).

## Contents

README.md (this file), SOURCE.md (provenance), RECORDS-STAGING.md
(publication shape + G2 claim line), FILE-LIST.txt, SHA256SUMS,
the source zip above, and byte-copies of the build /
packet / install record: BUILD-RECORD.md, TEST-REPORT.md,
FINGERPRINTS.md, SPEC-DELTA.md, INSTALL-PACKET.md, RECEIPTS.md,
SRC-DIFF-1.0.0-to-1.0.1.txt, INSTALLED.txt, OUTCOME.txt,
SCRIPT-OUTPUT.txt. The jar above is NOT in this GitHub package
(BB-01 precedent, applied at publication): the Drive deposit
carries it, hash-referenced in SOURCE.md and in Artifacts.

## Still owed (separate legs, do not gate this package)

- Same-pass surfaces: Guide knowledge + What's New BOTH
  surfaces — drafts staged, filing is its own leg (board run
  409 W6 prep: ~/workspace/board-r409/bb09-surfaces/PREP.md).
  Tips + in-game menus checked NO CHANGE by the packet.
- Player proof: Mike (Java) mid-tour Guide Back tap; Bedrock
  step-form Back tap (defaults to Mike himself per Nov 15 rule
  — Kailey is not a server tester). Mirror walk owed.
