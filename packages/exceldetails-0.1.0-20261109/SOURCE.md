# ExcelDetails 0.1.0 — SOURCE

Excel — Wink Reyes Valley. Authors: Iris / Mike.

- **What:** a live damage meter in the Details! mold — Damage Done,
  Damage Taken, and Healing Done sidebar windows over a shared
  encounter segment (auto open/close, manual `/details clear`),
  per-player toggle default ON. In an EliteMobs party it piggybacks
  EM's managed sidebar at negative scores under the combined
  15-line cap; outside a party it owns its board alone. Design:
  `~/workspace/details-meter-20261109/build-0.1.0/DESIGN.md`.
  Placement law:
  `~/workspace/details-meter-20261109/DETAILS-PLACEMENT-DESK-VERIFICATION.md`.
- **Source:** this folder (`src/com/winkreyes/exceldetails/`), built
  by `local-build.sh` (deterministic double-build, JUnit first,
  fingerprint gate). Tests: 13/13 (`DetailsTest` — segment lifecycle,
  overall merge, manual clear, ranking, piggyback budget math, line
  shedding incl. own-row retention, Details-style formatting).
- **Candidate jar:** `ExcelDetails-0.1.0.jar`, sha256 recorded in
  `~/workspace/details-meter-20261109/build-0.1.0/BUILD-RECORD.md`.
- **Build ID:** EXC-DETAILS-0.1.0-WRV-20261109-01-a47f0c
  (FINGERPRINT-STANDARD Layer B, verified in-jar by the build).
- **Desk rig:** `~/workspace/details-meter-20261109/build-0.1.0/rig/`
  — Paper 26.3 + NMS fake players; 13/13 RIGOBS PASS, results in
  BUILD-RECORD.md.
- **Status:** BUILT at desk, NOT installed. Install packet draft:
  `~/workspace/details-meter-20261109/build-0.1.0/PACKET-DRAFT.md`
  (the board supervisor files the MANIFEST entry; install is a
  parent hand window).
