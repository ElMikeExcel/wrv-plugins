# ExcelDetails 0.2.1 — SOURCE

Excel — Wink Reyes Valley. Authors: Iris / Mike.

- **What:** 0.2.0's checkbox-mix combat meter, plus PARTY-SCOPED
  DISPLAY (Mike's ruling, Nov 9, 2026, after his live report that
  the meter showed players outside his party; Mike confirmed the
  either-party-counts default Nov 9, 2026, 5:19 PM CST). The
  encounter segment stays one shared, server-wide segment — same
  clock, same tracking, same overall totals — but each viewer's
  sidebar now shows only their own rows plus their party members'
  rows, where party = ExcelTogether party ∪ EliteMobs party
  (either system counts; solo = own rows only). Section totals
  recompute over the filtered rows, so share % is within-group.
  Pure filter: `PartyScope`; soft reflection seams to both party
  systems: `PartyLookup` (fail-soft to an empty member set per
  system; the scope floor is the viewer alone — a seam failure can
  only hide party rows, never widen the view). Everything else is
  0.2.0: checkbox mix, persistence, churn fix, placeholders,
  piggyback rendering. Full detail: BUILD-RECORD.md in this
  package.
- **Source:** this folder (`src/com/winkreyes/exceldetails/`),
  built by `local-build.sh` (deterministic double-build, JUnit
  first, fingerprint gate). Base: the shipped 0.2.0 tree, copied
  unchanged except for the 0.2.1 deltas; the unmodified copy was
  rebuilt first and reproduced the shipped 0.2.0 jar byte-for-byte
  (sha256 f216f81e…) before any edit was made.
- **Candidate jar:** `ExcelDetails-0.2.1.jar` — 41,116 B, sha256
  `7c5d56f3a7e70f147b14101b15d224144ae732735e7b5159efb5aba9d074251c`
  (full hash re-verified by the board supervisor at package
  staging, Nov 9, 2026; repro jar byte-identical; also recorded
  in `~/workspace/details-meter-20261109/build-0.2.1/BUILD-RECORD.md`).
- **Build ID:** EXC-DETAILS-0.2.1-WRV-20261109-01-5c2e8a
  (FINGERPRINT-STANDARD Layer B, verified in-jar by the build).
  Unit tests: 35/35 per BUILD-RECORD.md.
- **Status:** BUILT at desk, NOT installed at staging time. The
  MANIFEST gated PENDING entry was filed Nov 9, 2026 (board
  supervisor run 110); install is a parent hand window on the
  emptiness gate (first window Nov 9, 2026, 5:27 PM CST stopped
  at 2/10 online). Records package staged Nov 9, 2026 by the
  board supervisor (run 111) from
  `~/workspace/details-meter-20261109/build-0.2.1/`.
