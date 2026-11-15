# ExcelSummons 0.1.10 - BUILD RECORD (BB-11 main-page Back)

Built Sat Nov 14, 2026 (desk build, board-manager run 356,
unit BB-11). Base: LIVE ExcelSummons 0.1.9 seq-02 tree
(~/workspace/summons-cap-build-20261114/), confirmed by a
fresh G1 pull (jar EXACT vs the seq-02 candidate of
record) and a byte-identical base-gate rebuild in
BB-11/base-gate/ (107/107 tests) BEFORE any edit.

- Candidate: ExcelSummons-0.1.10.jar - 102,597 B /
  97e442165b755fb7fac325e712f634c2607490bf7f0de0dd83b28510036ff7d0
- Build ID: EXC-SUMMONS-0.1.10-WRV-20261114-01-bb1110
  (fingerprint gate PASS; plugin.yml declares 0.1.10)
- Tests: 109/109 (107 carried + 2 new BackNav tests)
- Determinism: internal double-build byte-identical +
  a second full build run reproduced the same hash.
- Scope diff vs live tree: src BuildInfo / MenuModel /
  SummonsMenus only (+ tests, version knobs,
  DESIGN-0.1.10.md). Config byte-identical.
- Rig: local Paper 26.2-123 + Magic 11.2.5, candidate
  in place - Enabling v0.1.10, Done (37.656s),
  SEVERE 0, clean disable. Log BB-11/rig/boot-0110.log.
- NOT filed, NOT installed (supervisor files).
