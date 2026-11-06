# ExcelProtect 0.1.4 — TEST REPORT

Build Nov 5, 2026. Jar `ExcelProtect-0.1.4.jar`, 184,594 B, SHA-256
`2b471e7b45f9dd029f79570eee3bb24d99da3f830ca17c097c3e942dda9d3b4c`.

## Unit suite — 49 tests, 0 failures

`local-build.sh` runs JUnit first (console standalone 1.11.4); the jar is
only produced on a green suite. **37 carried from 0.1.3 stay green** (two
inspect tests updated with explicit 0.1.4 SUPERSESSION markers — they
encoded the pre-0.1.4 inspect scope: snapshot sections are now compact
count lines instead of absent, and the sort/limit fixture's total moved
3 → 4 with the locations section added). 12 new:

| Test | Proves |
|---|---|
| locationSampleSchedulingMath | sampler period math: first sample one interval after join, ticks on the configured period, no drift accumulation |
| locationsToggleDefaultOnAndSampleSecondsClamps | `log.locations` defaults ON; `locations.sample-seconds` default 60, clamped 5–3600 |
| locationAnchorSummaryShapes | anchor records serialize per type (join/quit/teleport w/ cause, world_change from→to, respawn, gamemode from→to) |
| whereRouteOrderingAnchorsBeforeSamplesAtEqualTs | route sort: ts ascending, anchors before samples at equal timestamps |
| whereRouteRendersAnchorsAndTravelOnly | samples become steps only on world change or ≥32 blocks from the last shown point; stationary samples collapse |
| whereRouteCapKeepsStartAndLatest | 15-step cap keeps the start step and always tags the final point "latest"; hidden steps are counted, not silent |
| playtimePairsArcsAndClampsToWindow | join/leave arcs pair; totals clamp to the requested day window |
| playtimeOpenArcClosesAtNowOnlyWhenTold | an arc open at query time counts to now ONLY when the caller confirms the player is online |
| playtimeMissingLeaveAndOrphans | join-while-open closes the prior arc at that join; orphan leaves ignored; no NaN/negative durations |
| playtimeParseDaysAndFmtDuration | days arg default 1, clamp 1–30; duration formatting |
| inspectSectionsCoverAllStreamsWithMoreTail | inspect sections every stream that has records, fixed order, per-section cap with "…and N more" tails; location samples counted with a `where` pointer, never itemized |
| locationsQueryFormFromLedgerFiles | real ledger day files on disk → `where`/`playtime` read the locations + players streams back correctly |

Determinism: built twice via `local-build.sh`; both jars byte-identical
(SHA-256 above; repro jar kept alongside).

## Rig — Paper 26.3 + REAL EliteMobs 10.9.12 + REAL Magic 11.2.5, two runs

Sandbox pattern as prior Protect rigs (fake NMS player RigPlayer; rig
config `locations.sample-seconds: 5`). Evidence in `rig/evidence/`
(boot-0.1.4.log, boot2-toggle-off.log, locations/players/combat/dealt/
healing day files). Boot lines verbatim:

```
[ExcelProtect] locations: ON sample=5s anchors=join,quit,teleport,world,respawn,gamemode
[ExcelProtect] READY version=0.1.4 dir=plugins/ExcelProtect gp=off vault=off mcmmo=off retention-days=30
[ExcelProtect] version=0.1.4 gp=off vault=off mcmmo=off dropped-total=0
```

**SEVERE 0** in both boots; writer status `locations: pending=0 dropped=0 failed=0`.

| Branch | Evidence |
|---|---|
| Samples tick at the configured interval | Run 2: 23 samples at a 5 s rig interval — gaps median 5.0 s, min 5.0, max 11.3; 95% ≤9 s (the one 11.3 s gap coincides with world creation / respawn work on the main thread) |
| Anchors recorded, typed, in order | Run 2: 11 anchors — join ×1, teleport ×5, world_change ×2 (both directions via a created `rigsecond` world), gamemode ×2 (creative→survival, survival→creative), respawn ×1 |
| Teleport anchor lands between samples | Verified directly from the stream file: 4 of 5 teleport anchors have samples on BOTH sides in timestamp order. The exception is the setup teleport 290 ms after join — before the first 5 s sample exists — which is correct by construction |
| Respawn anchor | PROVEN in run 2 (run 1's driver invoked the wrong NMS signature; Paper 26.3's is the 4-arg `PlayerList.respawn(ServerPlayer, boolean, Entity.RemovalReason, PlayerRespawnEvent.RespawnReason)`, verified via javap; driver fixed). Stream shows `anchor respawn @ world -6,116,4` after the scripted death |
| `where` renders the route | `RigPlayer — where, last 24h: 27 record(s) (11 anchors, 16 samples), 15 step(s)` — start join → teleport → moved → teleport → world change → gamemode ×2 → teleport → respawn → latest, in order (full block in boot-0.1.4.log) |
| `playtime` matches session arcs | `6m total over 3 session(s), longest 2m` (identical for 1-day and 7-day windows). The evidence players file accumulated 3 `player_join` arcs across the two rig runs (see limits): arcs 1–2 closed by the next join, arc 3 open-but-online at query time — the derivation behaved exactly per design on real data |
| Inspect across streams | 7 sections with records — players (16), locations (11 anchors + 19 samples counted, not itemized), combat (11), dealt (4), healing (2), deaths (2), snapshots (7) — each capped at 3 with "…and N more" tails. ≥5 required: PASS |
| Toggle OFF boot | Boot 2 with `log.locations: false`: awake line `locations: OFF (config log.locations=false)`; `where` refused with the OFF message; ZERO locations files written. PASS |
| Console hygiene | bare `where`/`playtime` → usage lines; unknown player → clean no-records lines; both commands self-log like the 0.1.2 queries |

## Volume estimate (measured + modeled)

Rig-measured: the sampler ticks at exactly the configured period
(median gap = interval, above) — one record per online player per
interval, ~170 B/record. Model at production defaults (60 s):

- **Ceiling:** 10 players × 24 h × 60 samples/h = 14,400 lines/day ≈ 2.4 MB/day raw.
- **Realistic family load:** ≤6 players, evening-weighted ≈ 2,000–5,000 lines/day ≈ 0.3–0.9 MB/day.
- Anchors are event-rate (teleports dominate), noise next to samples.

Well inside the existing 30-day gzip/retention path; `locations` sits in
the writer shed order right after `players`, so under disk pressure it
sheds before the forensic streams.

## Honest limits

- **Fake players don't move organically.** All rig movement was driven
  by teleports (stated in the driver); sample *content* on a walking
  player (the ≥32-block travel-step rule) is unit-proven, and the
  post-install proofs include a real player's where-route across a play
  session.
- **No quit evidence at server shutdown — for ANY plugin.** Paper
  disables plugins before disconnecting players at stop, so neither the
  locations `quit` anchor nor the players-stream `player_leave` fires on
  a shutdown kick. Runtime quits DO record (same listeners, standard
  Bukkit events). Consequence: a session that ends by server stop stays
  "open" in the arcs; playtime handles it by the join-closes-prior rule
  and the online-only open-arc rule, so totals stay sane — but a
  stop-ended session's tail is invisible until the player's next join.
  Inherited house behavior (identical in 0.1.1–0.1.3), not a 0.1.4
  regression.
- **Playtime rig figure spans two runs.** The driver's inter-run cleanup
  missed the players day-file, so the 6m/3-session answer aggregates
  both rig runs' joins. The derivation was verified arithmetically
  against the arcs actually in the file, and the precise pairing/
  clamping cases are unit-tested; the number is a consistency proof,
  not a stopwatch proof.
- **No `damage` section in the rig inspect** because this rig's data
  holds no damage incidents — the driver set the fake player's health
  directly to stage the healing branch rather than landing mob hits.
  Damage capture itself is 0.1.2-proven and unchanged; the inspect
  section renderer is shared code, unit-covered with combat-family
  fixtures.
- One 11.3 s sample gap (run 2 max) during world creation: sampling is
  a main-thread scheduled task, so a long tick stretches that one
  interval. Samples are timestamped at capture, so routes stay truthful.
