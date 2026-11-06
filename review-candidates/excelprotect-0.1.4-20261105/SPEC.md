# ExcelProtect 0.1.4 — SPEC: "Where" (location timelines + playtime/where + all-stream inspect)

**Basis:** the total-observability coverage map
(`~/workspace/protect-coverage-audit/COVERAGE-MAP.md`), Mike's charter of
Nov 5, 2026 ("monitor everything and be able to see everything and be
asked about basically anything thats going on" — proactive, not
reactive). The map's staged plan names 0.1.4 "Where" as the first
increment after 0.1.3: highest investigative value per line of code.
Its query-surface-gap section names the other two defects this build
closes: `inspect player` scanned only 4 of 15 streams, and no playtime
answer existed anywhere despite the session records existing since 0.1.0.

0.1.4 is cumulative: it contains 0.1.2 (damage taken) and 0.1.3 (damage
dealt + healing) whole, and adds ONE new stream, TWO new queries, and
the inspect extension. Same house discipline throughout: daily JSONL
per stream, one async writer, 30-day retention + gzip + aggregates,
MONITOR-priority listeners, no IO on the event thread.

## §1 The location timeline — `locations-YYYY-MM-DD.jsonl`

One stream that rebuilds "where was X, in order" without merging other
streams at read time. Two record shapes:

**`location_sample`** — one position dot per online player per sample
interval. Envelope carries world + x/y/z (the sample position); body
carries `player`, `player_uuid`, `platform`, `gamemode`, `health`
(0.1 precision), `food`. Samples are single records, never merged.

**`location_anchor`** — the events that EXPLAIN movement, typed in the
`anchor` field so a timeline sorts in order:
- `join` / `quit` — at the join/quit location.
- `teleport` — any `PlayerTeleportEvent` cause; envelope = FROM
  location (the players stream's convention), body adds `cause`,
  `to_world`, `to_x/to_y/to_z`.
- `world_change` — `PlayerChangedWorldEvent`; body adds `from_world`,
  envelope = the new location.
- `respawn` — `PlayerRespawnEvent`; envelope = the respawn location,
  body adds `is_bed_spawn`, `is_anchor_spawn`.
- `gamemode` — `PlayerGameModeChangeEvent`; body adds
  `gamemode_from`, `gamemode_to`.

Every record (samples included) carries the player's gamemode at that
moment. Anchors are recorded by LocationWatch itself — the locations
stream is self-contained even if the players family's own toggles
(joins/leaves/teleports) are off; those streams keep their own records
unchanged (the duplication is deliberate: one stream, one timeline).

**Sampling, NOT movement tracking.** No `PlayerMoveEvent` hook exists
in this build. Mike's Nov 1 decision excluded per-step movement
records; the coverage map's reconciliation — a dot per interval plus
an anchor for every jump — is what ships. The sampler is stateless:
each tick reads the live online list, so a quit needs no cleanup and
offline players leave no records. Quit closes a player's sampling by
construction.

**Interval:** `locations.sample-seconds`, default **60**, clamped
5–3600 in code. The clamp floor is the volume guard: at 60 s and ≤10
players the ceiling is ~14,400 lines/day and realistic play is
2–5k/day (coverage map math, restated in TEST-REPORT with the rig's
measured rate); at 5 s it would be 12× that for almost no gain.

## §2 Playtime — a query, not a capture

The players stream has kept `player_join` / `player_leave` arcs since
0.1.0. Map row 30's gap was the missing QUERY. `/excelprotect playtime`
adds no records; `PlaytimeReport` derives the answer from those arcs:

- arcs pair join → next leave, sorted by `ts_ms`, exact name/uuid match;
- a join while an arc is open (leave never recorded — crash/kick edge)
  closes the open arc AT that join rather than double-counting;
- a leave with no open arc is ignored (its join predates the record);
- an arc still open counts to query-time ONLY when the player is
  actually online at query time (the command layer checks and passes
  `now`, else −1 and the arc is dropped);
- totals count each arc's OVERLAP with the window, so an arc that
  started before the window counts from the window start — which is
  why the scan carries no time filter (pairing needs the early join).

Form: `/excelprotect playtime <player> [days]` — days = Chicago
calendar days including today (default 1, clamped 1–30 = retention).
Answer: total + session count + longest session, plus the live line
("online now — current session … since HH:MM:SS") when applicable.
Self-logged (`playtime_query`), any sender.

## §3 Where — the route

`/excelprotect where <player> [last-hours]` (hours as 0.1.2: default
24, clamp 0.1–720) renders the locations stream for one player as a
route, `WhereReport`:

- records sort by `ts_ms`; at equal timestamps anchors precede samples
  (the anchor explains the position the sample confirms);
- the first record is the `start` step; the last is always shown
  (tagged `latest` when it earned no step of its own);
- anchors are always steps — they are the story;
- samples become steps only on real travel: world change, or ≥32
  blocks (3D) from the last SHOWN point — standing around makes
  samples, not noise;
- steps cap at 15 in the command; omitted middle steps are COUNTED in
  the answer ("… N intermediate step(s) omitted — full route via
  export"), never silently dropped.

Header line reports records / anchors / samples / steps so the reader
knows exactly what the route was distilled from. Self-logged
(`where_query`), any sender. With `log.locations` off, the command
answers the OFF line naming the config key (0.1.2 form).

## §4 Inspect player — all streams, in sections

0.1.1's form scanned containers/items/deaths/blocks only — "what has X
been doing?" returned chests and deaths, not fights, movement,
commands or money (coverage map, query-surface gap). 0.1.4 scans
**every stream in `Exporter.STREAMS`** for the player and answers in
per-stream sections (`InspectReport.buildSections`), in a fixed order:
players, locations, combat, damage, dealt, healing, economy,
containers, items, blocks, entities, deaths, snapshots, audit, admin,
system. Each section: latest `shown` of `total`, capped by the `[n]`
argument (now a PER-SECTION cap; default 10, clamp 1–25 — a deliberate
meaning change, since a single global cap would let one loud stream
starve the other fourteen), and every capped section ends with
"…and N more — full detail via /excelprotect export stream=… player=…".

Summaries now exist for every record type a player can appear in:
joins/leaves/teleports/commands/chat/deaths (players), location
anchors (locations; samples are COUNTED in the section with a pointer
to `/excelprotect where`, never itemized — routes are §3's job), the
three incident families (reusing the 0.1.2/0.1.3 summarizers verbatim),
combat_damage / combat_heal / fight_summary (combat), money /
service_charge (economy), entity death/spawn/change/tame (entities),
xp/enchant/ender-chest (audit), inventory + mcMMO snapshots
(snapshots — COMPACT count lines only: reason + item count / power
level; the fingerprint payload itself is never dumped, preserving the
0.1.1 rule's spirit while making the stream askable), lost-and-found
recoveries (deaths), and admin actions. Unknown/future types stay
unreportable rather than dumping raw JSON.

Two carried tests encoded the old 4-stream scope (snapshot types
asserted unreportable); both were updated in place with the
supersession marked — the assertions now pin the compact shapes.

**Companion fix (Exporter):** the `player=` matcher now also checks
`attacker`, `attacker_uuid`, `victim_uuid`, `target_uuid`,
`healer_uuid` — its javadoc always promised "any actor field", but
combat-stream lines name their actors in exactly those fields, so a
player scan silently missed the combat stream. Broader matching is
the documented intent; every `player=` consumer (export, rollback,
all query forms) inherits it.

## §5 Config + lines

- `log.locations` — new toggle, default **true** (charter: track
  everything). OFF = LocationWatch is never constructed: no samples,
  no anchors, no records; `where` answers OFF; `playtime` is
  unaffected (it reads the players stream).
- `locations.sample-seconds` — default 60, clamp 5–3600.
- Shed order gains `locations` after `players` (it sheds early under
  overload, like the other high-volume streams).
- The shipped config documents both keys; **the live config is NOT
  replaced at install** — absent keys default to ON/60s, the desired
  state.
- Awake line: `[ExcelProtect] locations: ON sample=60s
  anchors=join,quit,teleport,world,respawn,gamemode` (OFF variant
  names the key). READY/status report `version=0.1.4`.

## §6 What 0.1.4 deliberately is not

- No per-step movement (see §1) — settled, not a gap.
- No cross-stream timeline merge in `inspect` — sections are the
  honest intermediate step; the map's merged-timeline and Guide//ask
  items remain later increments, unchanged by this build.
- Respawn/gamemode records live as anchor types on `locations`
  (the map assigned them to this increment's family), not as new
  streams.
