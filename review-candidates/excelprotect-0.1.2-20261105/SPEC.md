# ExcelProtect 0.1.2 — SPEC: damage-incident logging

**Commission:** Mike, Nov 5, 2026 — "We need to add damage incidents to the
ExcelProtect. Remember the goal is to be able to track everything."
**Trigger:** the Nov 5, 2026 elite-tuning question. Kailey's lava near-miss
that morning produced NO usable record anywhere: ExcelTelemetry logs kills
only (no damage-taken field), Protect's combat stream holds per-event
`combat_damage` lines and fight rollups but nothing that answers "what hit
this player, how hard, for how long, and how close to death did it take
them" as one incident. Elite damage tuning (the ladder's scaled-to-player
multiplier) has no per-hit evidence base until this exists.
**Base:** ExcelProtect 0.1.1 (LIVE Nov 4, 2026), source byte-identical
between `~/workspace/excelprotect/src/ExcelProtect/` and the record package
`~/workspace/records-catchup-20261103/packages/excelprotect-0.1.1-20261104/`
(diff-verified at build start; the tree's own 0.1.1 jar re-hashes to the
installed `b24c0ef2…`).

## 1. What an incident is

One **continuous exposure**: consecutive damage ticks to the same player
from the same source (same resolved attacker identity + same damage cause)
separated by no more than the coalesce window merge into ONE record. A new
source, a new cause, or a gap longer than the window starts a new incident.
An incident closes when (a) the window expires (sweep), (b) a hit from a
different source arrives — only the expired ones close, see §3, (c) the
player dies, (d) the player quits, or (e) the plugin disables. Incidents
are about damage TAKEN BY PLAYERS only (the tuning question); damage dealt
by players to mobs stays in the combat stream's per-event records and fight
summaries, untouched.

## 2. The record — new stream `damage`, file `damage-YYYY-MM-DD.jsonl`

Same data dir, same envelope dialect as every Protect stream (`ts`/`ts_ms`
Chicago ISO + epoch ms, written by the same async `Ledger`/`DurableWriter`,
so drops are counted, never silent). The envelope `ts` is the CLOSE time;
the body carries the incident's own span. Envelope location = the incident's
start location (world/x/y/z + claim stamp, house style).

Body fields (`type` = `damage_incident`):

| field | meaning |
|---|---|
| `player`, `player_uuid` | victim |
| `cause` | Bukkit `DamageCause` name (`ENTITY_ATTACK`, `LAVA`, `FALL`, …) |
| `attacker_kind` | `mob` / `player` / `environment` / `projectile` (unresolved owner) / `entity` (non-living damager: primed TNT, falling block) |
| `attacker` | player-attacker name (kind=player only) |
| `attacker_entity` | entity-type key, e.g. `minecraft:zombie` (kind=mob, and projectile owner) |
| `attacker_uuid` | attacker entity uuid when there is one |
| `projectile` | projectile type key when the hit arrived by projectile (owner resolved into the attacker fields) |
| `elite`, `elite_level` | mob attackers only, per §4: `elite:false` when the probe ran and the mob is not an elite (incl. EliteMobs absent — no elites can exist); both omitted + `elite_probe:"unavailable"` when EM is present but its API could not be resolved |
| `start_ts`, `start_ts_ms`, `end_ts_ms`, `duration_ms` | the exposure span |
| `tick_count` | damage ticks merged into this incident |
| `total_damage` | sum of FINAL damage (post-armor `getFinalDamage()`), 2-dp |
| `max_single_hit` | largest single final-damage tick |
| `health_start` | victim health (red hearts) immediately before the first tick |
| `health_end` | victim health immediately after the last tick (0 when `died`) |
| `health_min` | lowest post-tick health inside the incident |
| `max_health` | victim max health at the first tick |
| `near_death` | true when the player SURVIVED with `health_end` ≤ threshold × `max_health` (§5) |
| `died` | true on the most recently active incident when death closed the player's incidents |
| `closed` | `window` / `death` / `quit` / `shutdown` |

Health semantics, stated once: values are `getHealth()` red hearts at event
time (absorption excluded); post-tick health = `max(0, health_before −
final_damage)` per tick, so heals landing between ticks are picked up by
the next tick's `health_before` — `health_end` is honestly "health right
after the last recorded hit," not a sampled-later value.

### Example records (the three shapes this build must produce)

Elite melee (a level-17 elite zombie works a player over for ~4 s):

```json
{"ts":"2026-11-05T21:14:09.412-06:00","ts_ms":1793930049412,"world":"world","x":-1180.4,"y":64,"z":-290.15,"type":"damage_incident","player":"Jester_Turtle","player_uuid":"…","cause":"ENTITY_ATTACK","attacker_kind":"mob","attacker_entity":"minecraft:zombie","attacker_uuid":"…","elite":true,"elite_level":17,"start_ts":"2026-11-05T21:14:05.020-06:00","start_ts_ms":1793930045020,"end_ts_ms":179393004
...[truncated 8473 chars]