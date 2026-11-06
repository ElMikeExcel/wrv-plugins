# ExcelProtect 0.1.4 — changelog

Released as a build Nov 5, 2026 (install gated; see INSTALL-PACKET.md).
The coverage map's first increment after 0.1.3 — "Where": location
timelines, the playtime and where queries, and inspect across all
streams. Mike's charter: monitor everything, be askable about
basically anything; proactive, not reactive.

## Added

- **Location TIMELINE stream** (`locations-YYYY-MM-DD.jsonl`) —
  `LocationWatch`. One `location_sample` per online player per sample
  interval (world + coords in the envelope; gamemode + health/food in
  the body) plus typed `location_anchor` records — join, quit,
  teleport (cause + destination), world_change, respawn (bed/anchor
  flags), gamemode (from → to) — so any player's route rebuilds in
  order from one stream. SAMPLING, not movement tracking: no
  move-event hook exists; this is the settled reconciliation with
  the Nov 1 no-per-step decision (dot per interval + anchor per jump).
- **`/excelprotect where <player> [last-hours]`** — the timeline as a
  readable route (start → anchors + real travel → latest), with
  record/anchor/sample/step counts and omitted steps counted, never
  silent. Self-logging, any sender, OFF-aware.
- **`/excelprotect playtime <player> [days]`** — total time online
  over the last N Chicago calendar days (default today), derived from
  the join/leave arcs the players stream has always kept; open arcs
  count to now only while the player is actually online. No new
  capture — the gap was the query (coverage map row 30).
- **Config:** `log.locations` (default ON) and
  `locations.sample-seconds` (default 60, clamped 5–3600);
  `locations` joins the writer shed order after `players`.

## Changed

- **`inspect player` now covers ALL streams, in sections.** 0.1.1
  scanned containers/items/deaths/blocks only. Now every stream is
  scanned and answered per-stream (players, locations, combat,
  damage, dealt, healing, economy, containers, items, blocks,
  entities, deaths, snapshots, audit, admin, system), each section
  capped by [n] (now a per-section cap) with a "…and N more" tail.
  Summaries exist for every player-bearing record type, reusing the
  incident summarizers verbatim; location samples are counted with a
  pointer to `where`, never itemized; snapshot records summarize as
  compact count lines (payloads still never dumped).
- **Exporter `player=` matching** now includes attacker / victim /
  target / healer name+uuid fields — its documented "any actor
  field" intent; combat-stream lines were previously unmatchable by
  player scans.

## Notes

- Two carried unit tests encoded the pre-0.1.4 inspect scope
  (snapshot types asserted unreportable); updated in place with the
  supersession marked, pinning the new compact shapes instead.
- Everything 0.1.3 does is carried unchanged. Version reported as
  0.1.4 in the READY line, status, and plugin.yml.
