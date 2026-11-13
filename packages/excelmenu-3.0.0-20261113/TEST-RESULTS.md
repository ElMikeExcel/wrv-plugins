# TEST RESULTS — ExcelMenu 3.0.0

Desk build, Nov 13, 2026 (board supervisor run 281 re-dispatch;
full account in BUILD-REPORT.md; raw log = build.log in the
build folder, SHA-256
7355217b3d1f981825d4860c362a3481dd7d021e1546c13b7fe5a6e20406777e).

## Unit suite — OK (163 tests)

`build.sh` run: JUnit 4.13.2, suite `OK (163 tests)` =
128 carried from 2.0.1 + 35 new. Compile: 1 warning (the
standing Sound.valueOf deprecation in ActionDispatcher) — no
errors.

- Carried 128: all green after three enumerated intentional
  deltas, each the magiclevel retirement (DESIGN §7 R1):
  FormModelTest flat sequence 28 → 27 buttons and out-of-window
  26 → 25; ChestModelTest Java slot map compacts one slot from
  spellbook on (32 total), out-of-window 31 → 30;
  GoldenHarnessTest acceptance D10 removed (it existed solely
  to classify the magiclevel row; the harness's
  no-stale-acceptances rule requires its removal — annotated
  in the test's javadoc). Golden harness otherwise unchanged:
  D1–D9, D11 occurred; 108 viewer states × 2 platforms, 5,526
  rows compared. Catalogue harness C1–C8 unchanged, 56 rows.
- New suites (35): PinsModelTest (8 — picker pin order/labels/
  targets, group exclusion + Back last, flat dedupe,
  guard-failed pin still targets its entry, empty-pins = 2.0.1
  shape, commands_menu scope, group-order override);
  LayoutLoaderPinsTest (6 — parse/order, absent = empty,
  duplicate refused, >4 refused, non-list refused, blank
  refused); OrderMergeTest (8 — §3.3 properties incl.
  Bedrock-only group order and idempotence); PrefsCodecTest
  (7 — round-trips full/partial/empty, corrupt, wrong types,
  version 99, unknown ids survive verbatim);
  MenuEditorCoreTest (6 — moves, boundary notes, unknown id,
  insert=self no-op, and the 500-trial property that
  insert-move ≡ the Up/Down step sequence).

## Determinism

- Double-build: two consecutive clean build.sh runs produced
  byte-identical jars (306607ba… both times, cmp clean).
- Clean-room (a): a fresh copy of the tree built in /tmp —
  byte-identical. Clean-room (b): the shipped sources zip
  unpacked to a fresh directory and built — byte-identical
  jar, OK (163 tests). The sources zip is therefore a proven
  reproduction of the jar.
- Base gate: the 2.0.1 base jar was first reproduced
  byte-identically from its own sources zip (287,906 B /
  c6da29ba…, suite OK (128 tests)) before any 3.0.0 editing.

## Rig results

Filing Gate 2 (R2 desk rig + R3 PC mirror rig) — GATE 2 PASS;
the full record is RIG-REPORT.md in this package (harness
26 PASS / 0 FAIL; every 3.0.0 boot 93/2/13, bridge PRESENT,
SEVERE 0; magiclevel absent after restart). Client-tap cases
2, 3, 7, 8, 9, 16 were NOT EXERCISED on either rig — they are
DESIGN §7's five post-install player proofs, owed by Mike
after the Nov 13, 2026 install (INSTALLED.txt / OUTCOME.txt).
