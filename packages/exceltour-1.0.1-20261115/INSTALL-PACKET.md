# INSTALL PACKET - ExcelTour 1.0.1 (BB-09 back-button fixes)
Packet: ~/workspace/back-button-wave2-r355/BB-09/INSTALL-PACKET.md
Lint: 4963 B / max line 66 / non-ASCII 0 (targets: <= ~12 KB,
lines < ~300, minimal non-ASCII - PASS; measured before the
lint line itself was finalized, delta < 40 B)
Unit: BB-09, one plugin, one install unit. Rows: 3 (0 FIX-1 /
3 FIX-2) from back-button-audit-20261113 AUDIT.md (ExcelTour
1.0.0) via FIXBATCH-SPLIT-R348.md section 2.

## Live-premise receipts (G1) - GATE OPEN, READ FIRST

- Claim: live runs ExcelTour 1.0.0 as plugins/ExcelTour-1.0.0.jar.
- Receipt: NOT OBTAINED. Homebase probes failed all attempts on
  Nov 14, 2026 (3-probe/~2-min pattern completed, plus 2 extra;
  relay reset, exit 255 - full log in RECEIPTS.md). No FALLBACK
  (panel) receipt was taken (no panel route in the desk lane).
- STOP: do not file or install until a byte-exact G1 receipt
  (Homebase retry, or labelled FALLBACK (panel) download +
  SHA-256) confirms the live jar. If live differs from 1.0.0,
  re-base this unit and re-check the 3 rows first.
- Supporting (NOT a live premise): the audited 1.0.0 tree
  rebuilds byte-identically to the jar of record, 50,656 B /
  4af7c455d98ead43603f75fe007444983c75345d70f0109c9149dc785c78dd72
  (base-gate receipt in RECEIPTS.md).

## Candidate

- Artifact: ~/workspace/back-button-wave2-r355/BB-09/
  build-1.0.1/ExcelTour-1.0.1.jar
- 50,908 B / SHA-256
  ab94d4a12b33b8acb45b60599ef3b27eac4f0b86805456930719ba90e2c0e317
- Build: EXC-TOUR-1.0.1-WRV-20261114-01-bb09; deterministic
  double-build byte-identical across two full build.sh runs.
- Tests: 59/59 (carried TourTest 42/42 at exactly the 1.0.0
  count; new BackNavTest 17/17 incl. the mid-tour Back
  progress-survival walk).
- Rig: local Paper 26.3 rig (Geyser + floodgate co-loaded):
  Done, SEVERE 0, enable line prints the 1.0.1 build ID;
  TourRig exercise 4 PASS / 1 FAIL (arrival-completion step;
  control-boot classification in BB-09-RESULT.md). Mirror walk
  NOT run (Homebase down) - owed, see Proof owed.

## Live-jar gate (STOP on mismatch)

- Once the G1 receipt exists: live plugins/ExcelTour-1.0.0.jar
  must equal that receipt's size + SHA-256 at install time.
  Any mismatch = STOP, re-base, do not install this candidate.

## Scope

- Changes: Guide view (TourMenu inventory, Java + Bedrock
  fallback) slot 26 Close barrier becomes "Back to Server
  Menu" (ARROW) in all states, wired to the ExcelMenu /menu
  opener; Bedrock tour step form gains a last button "Back to
  Server Menu" (action backmenu, pure navigation - no engine
  call, no save, tour progress untouched mid-tour).
- Does NOT change: core tour engine, triggers, routes, config,
  player progress data format, commands, the Java action-bar +
  chat step presentation (audit Not-a-defect row), or any other
  plugin. No config or data migration; existing
  plugins/ExcelTour/ data carries over untouched.

## Gates (standard, at install)

- Marker discipline: WINDOW-IN-PROGRESS marker, re-checked
  before Shut Down and before Start; foreign marker < 3 h old
  = coordination abort.
- BACKUP FIRST + cap-at-6 (name prunees; never the Daily
  newest or the two newest manual restore points).
- Emptiness: list = 0 twice before the swap.
- Swap: replace plugins/ExcelTour-1.0.0.jar with the candidate
  (name: ExcelTour-1.0.1.jar); keep the 1.0.0 jar as the
  in-window rollback until boot verify passes.

## Boot verify (by script output)

- grep the boot log for: "Enabling ExcelTour v1.0.1" and
  "ExcelTour v1.0.1 enabled: 1 route(s) loaded
  [guild-first-visit] (build EXC-TOUR-1.0.1-WRV-20261114-01-bb09)"
- /tour version prints the same build ID.
- SEVERE count attributable to ExcelTour = 0 (grep -c SEVERE).
- Guild state unchanged: guild LOCKED 4 of 5 (ExcelGuildGate
  line in the same boot log).
- Marker on install: BB-09/build-1.0.1/INSTALLED.txt

## Same-pass surfaces (drafts staged in BB-09/surfaces/)

- Guide knowledge note: GUIDE-NOTE-DRAFT.md (staged).
- What's New, website + in-game changelog: WHATSNEW-DRAFT.md
  (staged; both texts).
- Broadcaster tips check: NO CHANGE (no tip names the Guide
  view's Close behaviour).
- In-game menus check: NO CHANGE (ExcelMenu's Guide entry still
  opens /tour menu; the change is inside ExcelTour's own view).

## Proof owed after install

- Mike (Java): open the compass Guide mid-tour, tap slot-26
  "Back to Server Menu" - Server Menu opens; re-open the Guide:
  same step shown (tour not advanced, stopped or restarted).
- Kailey (Bedrock): mid-tour step form, tap "Back to Server
  Menu" - Server Menu opens; re-open /tour: same step. Also
  spot-check the Bedrock Guide fallback view's Back.
- Mirror walk (Homebase test-server mirror): the same three
  walks on the mirror before or with the player proof, once
  the Homebase route is back.
- Acceptance end for the batch: Mike's spot taps (backlog
  line 466); a proof is discharged only by the tester's own
  report/screenshot.
