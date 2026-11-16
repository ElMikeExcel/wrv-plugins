# SOURCE — ExcelTour 1.0.1 (BB-09 back-button fixes)

Plugin: ExcelTour 1.0.1 — back-button fixes, 3 rows (BB-09):
Guide view slot 26 becomes "Back to Server Menu" on Java and
on the Bedrock fallback (one inventory, one code path), and
the Bedrock tour step form gains "Back to Server Menu" last.
Authority: board-manager run 355 unit BB-09 (back-button
wave 2); installed by wrv-install-failsafe.
Build folder of record: ~/workspace/back-button-wave2-r355/BB-09/
Tests: 59/59 (TourTest 42/42 carried, BackNavTest 17/17 new) —
see TEST-REPORT.md in this package.

Source zip: ExcelTour-1.0.1-source.zip — 39,081 B / SHA-256
b695249069e1ed29804bf7a48ee11c9e24afe06fa10133e91af32cb6b8a4c3e6
(31 entries: ExcelTour/src, ExcelTour/tests, plugin.yml,
config.yml, routes/guild-first-visit.yml, build.sh;
108,355 B uncompressed). The source tree is carried inside
the zip only — it is not unpacked in this package.

Built jar (NOT in this GitHub records package — BB-01
precedent applied by the run-409 W11 publication leg: the
Drive deposit carries the jar, hash-referenced here):
ExcelTour-1.0.1.jar — 50,908 B / SHA-256
ab94d4a12b33b8acb45b60599ef3b27eac4f0b86805456930719ba90e2c0e317
Build ID EXC-TOUR-1.0.1-WRV-20261114-01-bb09; deterministic
double-build byte-identical; base gate: the unmodified audited
1.0.0 tree rebuilds byte-identically to the jar of record
(50,656 B / 4af7c455d98ead43603f75fe007444983c75345d70f0109c9149dc785c78dd72),
and the G1 live pull at install matched that hash EXACT
(RECEIPTS.md addendum; SCRIPT-OUTPUT.txt G1 block PASS).

Delta (4 files; full diff in SRC-DIFF-1.0.0-to-1.0.1.txt):
TourMenu.java (slot-26 backItem in all three states),
ExcelTour.java (openServerMenu + backmenu case, pure
navigation), Presentation.java (step form Back last),
BuildInfo.java / plugin.yml (version 1.0.1). See
BUILD-RECORD.md and SPEC-DELTA.md.

Install: INSTALLED Nov 15, 2026, by wrv-install-failsafe run
scheduled 05:33 CST — see INSTALLED.txt, OUTCOME.txt,
SCRIPT-OUTPUT.txt in this package (17/17 script blocks PASS,
boot Done, SEVERE 0, placed pull-back EXACT, guild state
byte-identical). Packet: INSTALL-PACKET.md + RECEIPTS.md.
