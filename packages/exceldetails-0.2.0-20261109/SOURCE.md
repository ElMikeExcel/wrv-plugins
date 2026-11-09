# ExcelDetails 0.2.0 — SOURCE

Excel — Wink Reyes Valley. Authors: Iris / Mike.

- **What:** 0.1.0's live Details!-style combat meter, with the display
  model replaced by Mike's checkbox mix (Nov 9, 2026): damage done /
  damage taken / healing done are three independent per-player
  booleans (defaults ON / OFF / OFF), rendered all at once as stacked
  sections in one sidebar box under the hard 15-line combined ceiling
  (config max-rows is per-section). `/details show <metric>` toggles
  one checkbox; `window`/`next` remain as exclusive quick paths.
  PlaceholderAPI expansion ships the menu bridge (%details_show_*% →
  ON/OFF). Also in 0.2.0: the empty-segment churn fix (micro segments
  merge silently, never announce) and overall-total persistence in
  data.yml. Design: `~/workspace/details-meter-20261109/build-0.2.0/DESIGN.md`.
  Placement law:
  `~/workspace/details-meter-20261109/DETAILS-PLACEMENT-DESK-VERIFICATION.md`.
- **Source:** this folder (`src/com/winkreyes/exceldetails/`), built
  by `local-build.sh` (deterministic double-build, JUnit first,
  fingerprint gate). Base: the shipped 0.1.0 tree, copied unchanged
  except for the 0.2.0 deltas.
- **Candidate jar:** `ExcelDetails-0.2.0.jar` — 37,130 B, sha256
  `f216f81eefd8d09cc6abfc14efbd2b884803f6c3717d9d9946766b836cd538cb`
  (full hash re-verified by the board supervisor at package staging,
  Nov 9, 2026; also recorded in
  `~/workspace/details-meter-20261109/build-0.2.0/BUILD-RECORD.md`).
- **Build ID:** EXC-DETAILS-0.2.0-WRV-20261109-01-c84f19
  (FINGERPRINT-STANDARD Layer B, verified in-jar by the build).
  Unit tests: 28/28 per BUILD-RECORD.md.
- **Status:** INSTALLED Nov 9, 2026, parent hand window
  12:30–1:14 PM CST (see INSTALLED.txt in this package: jar panel
  checksum exact, boot READY version=0.2.0, Done 20.135s, SEVERE 0;
  Details Settings menu definition placed — the registry-side menu
  entry is a parent-lane follow-up named in INSTALLED.txt).
  Records package staged Nov 9, 2026 by the board supervisor
  (run 107) from `~/workspace/details-meter-20261109/build-0.2.0/`.
