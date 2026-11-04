# ExcelProtect 0.1.1 — CHANGELOG

Small follow-up build, 2026-11-04 (America/Chicago). **Not yet installed** — filed PENDING in the install queue; 0.1.0 remains shadow-live until the failsafe lands this in a gated window.

## The one change: console-usable inspect queries

0.1.0's `/excelprotect inspect` only toggled click-to-inspect mode on the sender, so from the console it answered "players only" — the one console-admin gap found in ExcelTogether 0.5.3's fleet coverage audit (`~/workspace/party-mesh/build-0.5.3/COVERAGE.md`). Mike's standing rule (Nov 3, 2026): every plugin ships console-runnable admin commands for support operations — "We should always have a way to help the players."

0.1.1 adds two query forms to `inspect`, routed before the toggle and usable by **any sender** (console included; the command's existing `excelprotect.admin` permission behavior for players is unchanged):

- `/excelprotect inspect player <name> [n]` — that player's latest recorded activity (containers, items, deaths, blocks streams), latest first, default 10 lines, max 25. Answers "no recorded activity for <name>" when there is nothing.
- `/excelprotect inspect at <world> <x> <y> <z> [n]` — recorded history at one block/container position (blocks + containers streams, exact position), headed by the same one-line summary click-inspect would show for that block when the world is loaded. Answers "no recorded events at …" when there is nothing.

Reports are one compact line per event (time, kind, item fingerprints, actor, location), assembled from the same ledger records `export` reads — and they never print item payloads (fingerprints only). Every query is written to the admin stream like the other staff actions (`inspect_player` / `inspect_at`).

Bare `/excelprotect inspect` keeps its exact 0.1.0 behavior for players (click-mode toggle). From the console, bare `inspect` now prints the query-form usage instead of just "players only".

## Nothing else changed

No recording, rollback, recovery, importer, API, config, or data-format changes. The 0.1.0 data folder and day files are read as-is. New code is one class (`InspectReport` — pure summary/sort logic) plus the two command handlers in the main class.
