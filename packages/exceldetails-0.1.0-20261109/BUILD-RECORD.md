# ExcelDetails 0.1.0 — BUILD RECORD (Nov 9, 2026)

Design: `~/workspace/details-meter-20261109/build-0.1.0/DESIGN.md`.
Commission: Mike Reyes, Nov 8, 2026, ~11:22 PM CST (backlog Item 40).
Placement verification:
`~/workspace/details-meter-20261109/DETAILS-PLACEMENT-DESK-VERIFICATION.md`.

**Status: BUILT + RIG-PROVEN at desk. NOT installed, NOT filed in
the MANIFEST — the board supervisor files the entry after verifying
this landing; install is a parent hand window (PACKET-DRAFT.md).**

## Candidate

- `ExcelDetails/ExcelDetails-0.1.0.jar` — 28,362 B,
  sha256 `f73e97600ec98863600b2d0b57ce6af4b1cbed943289071343714cae84fa310b`
- Deterministic double-build byte-identical; fingerprint gate PASS
  (build EXC-DETAILS-0.1.0-WRV-20261109-01-a47f0c, Layer B in-jar:
  excel-build.txt + BuildInfo fragments).
- Unit tests: **13/13** (segment opens on first hit, quiet-timeout
  close + overall merge + last-segment readability, healing tracked
  separately, manual clear discards and restarts the clock, ranking,
  non-positive amounts never open a segment, piggyback budget
  15 − foreign lines, line shedding footer→lowest rows with header
  last, own-row retention under the cut, compact/percent/per-second/
  clock formatting, window parse + cycle, prefs default ON).

## Rig (Paper 26.3-235, NMS fake players, fresh world)

Run log: `rig/rig-console.log` — **13 PASS / 0 FAIL**:

- plugin-enabled / intake — scripted damage lands in the tracker
  (alpha damage done, bravo damage taken incl. zombie source).
- standalone-board — the meter assigns its own board outside a party.
- standalone-lines-live — the sidebar row shows the viewer's name
  with the tracker's exact compact amount ("RigAlpha 4.4 (100%)").
- standalone-lines-update — after a second hit the same row moves
  (4.4 → 11.8) on the next refresh: LIVE updating proven, not
  end-of-fight.
- piggyback-board-kept — against a simulated EM-managed board
  (sidebar objective, 3 member lines at scores 0..2), the renderer
  never replaces the player's board.
- piggyback-negative-scores — 3 meter lines written, all at
  negative scores (min −3), directly under EM's lines.
- piggyback-combined-cap — combined box = 6 lines ≤ 15.
- piggyback-em-lines-untouched — EM rows keep scores 0..2 and their
  exact prefix text after meter writes.
- piggyback-header-present / piggyback-taken-window — the Details!
  header renders in the shared box; `/details window taken` shows
  Bravo's Damage Taken row with his exact total (the Warden-fight
  window shape).
- toggle-off-strips-meter-only — `/details off` removes every meter
  entry, EM lines and board assignment untouched.
- segment-active — the shared segment is live at rig end.

Sandbox note: the rig log also carries authlib/Paper version-fetch
SSL errors — the sandbox cannot reach Mojang services; they are
unrelated to the plugin (offline mode, cached server jar).

## Known v1 limits (stated, not hidden)

- Healing attribution: `EntityRegainHealthEvent` names no healer,
  so v1 credits healing that lands on a player to that player
  (DESIGN §4). True Magic-healer attribution is a v1.1 candidate.
  Damage done + damage taken — Mike's stated minimum — are exact.
- The real EliteMobs 10.9.12 PartySidebar was not in the rig; the
  piggyback proof ran against a faithful simulation of its verified
  shape (bytecode verdict in the placement doc). First live proof
  rides the install window's player checks (PACKET-DRAFT.md).
- Segments are live instruments: toggles/windows persist across
  relogs (data.yml); segment numbers do not survive a restart.
