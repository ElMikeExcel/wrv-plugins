# BUILD RECORD - ExcelTour 1.0.1 (BB-09)

Unit: BB-09 - ExcelTour back-button fixes, 3 rows (0 FIX-1 /
3 FIX-2), board-manager run 355. Audit: back-button-audit-20261113
AUDIT.md ExcelTour 1.0.0 rows + parts/D-wandbars-tour-qm-guild.md;
split: FIXBATCH-SPLIT-R348.md section 2 BB-09 + section 3 base-gate.

## Base gate (section 3)

- Copied tree: exceltour-build-20261111/ExcelTour copied to
  build-1.0.1/ExcelTour; src and tests diff-identical to the
  audited 1.0.0 tree before the delta (diff -r: identical).
- Byte-identical rebuild: the UNMODIFIED copy was built first with
  the original build.sh. Result: ExcelTour-1.0.0.jar 50,656 B /
  SHA-256 4af7c455d98ead43603f75fe007444983c75345d70f0109c9149dc785c78dd72
  - EXACT match to the audited jar of record. Suite 42/0.
  Base gate PASSES on disk lineage.
- G1 live pull: NOT OBTAINED this run - Homebase relay down for
  every probe (see ../RECEIPTS.md). The packet therefore carries
  the G1 gate as OPEN: the supervisor must obtain the byte-exact
  live receipt (Homebase retry or labelled FALLBACK (panel))
  before filing. No live hash is asserted anywhere in this unit.

## Build

- Version: 1.0.0 -> 1.0.1 (next patch). Build ID:
  EXC-TOUR-1.0.1-WRV-20261114-01-bb09 (BuildInfo, plugin.yml and
  excel-build.txt agree; fingerprint gate passed).
- Candidate: ExcelTour-1.0.1.jar - 50,908 B / SHA-256
  ab94d4a12b33b8acb45b60599ef3b27eac4f0b86805456930719ba90e2c0e317
- Determinism: build.sh internal double assembly byte-identical,
  and two full build.sh runs produced the identical SHA-256.
- Source zip: ExcelTour-1.0.1-source.zip - 39,081 B / SHA-256
  b695249069e1ed29804bf7a48ee11c9e24afe06fa10133e91af32cb6b8a4c3e6

## Delta (4 files; full diff in ../SRC-DIFF-1.0.0-to-1.0.1.txt)

- TourMenu.java: slot 26 in all three states (not started,
  completed, in progress incl. restart-confirm) changes from the
  Close barrier to backItem() - Material.ARROW, "Back to Server
  Menu", lore "Return to the Server Menu. / Leaving changes
  nothing - resume any time." Covers the Java Guide view AND the
  Bedrock fallback (same inventory).
- ExcelTour.java: new openServerMenu(Player) - closeInventory +
  performCommand("menu"), the ExcelMenu opener this view is
  reached by (plugin.yml already softdepends ExcelMenu). Slot-26
  clicks in all three states call it. New handleTourAction case
  "backmenu": pure navigation via openServerMenu; no engine call,
  no save.
- Presentation.java: Bedrock step form gains the final button
  "Back to Server Menu" (action "backmenu"), Back-last per the
  house form convention, after Close. All carried buttons kept.
- BuildInfo.java / plugin.yml: version 1.0.1.
- NOT changed: core engine (TourEngine, Progress, ProgressStore,
  Route/RouteParser/Step/Trigger), TourCommands, BedrockForms,
  PlatformDetect, config.yml, routes, and the Java step
  presentation (action bar + chat - an audit Not-a-defect row,
  out of scope). No other behaviour changes.

## Mid-tour Back safety (the BB-09 care point)

Back performs zero progress operations: the backmenu branch and
openServerMenu contain no engine call and no save (pinned by
BackNavTest source-contract checks). Progress is only ever
mutated by the carried actions (next/skip/repeat/restart/stop),
which Back does not invoke. BackNavTest's behavioral walk proves
the record survives: mid-tour state (step 3 of 5, one skip
recorded) is field-identical after a Back, across a save/load
round-trip, and the tour still advances normally afterwards;
not-started and completed states are likewise unchanged.
