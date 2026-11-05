# ExcelProtect 0.1.2 — TEST REPORT (2026-11-05)

## Unit tests — 26/26 pass, 0 failures

`bash local-build.sh` runs the full JUnit suite before packaging (16 checks
carried unchanged from 0.1.1 + 10 new for this build):

- `DamageIncidentsTest` — coalescer merge (3 hits inside the window = 1
  incident, exact totals/max/health arc), window edge (a hit 1 ms past the gap
  closes + splits), source split (interleaved second attacker keeps its own
  incident), cause split, death flags latest incident + closes all
  (`died`/`near_death` never coexist), near-death threshold math, elite
  serialization shapes (elite / not-elite / probe-unavailable /
  environment-has-no-elite-keys), projectile + non-living-entity descriptors.
- `DamageSettingsTest` — shipped defaults (toggle on, 2000 ms, 0.30) and the
  gap/fraction clamps (250 / 30000 / 0.01 / 1.0).
- `DamageReportTest` — line forms: elite (`zombie ELITE Lv25`), projectile
  (`skeleton (arrow)`), DIED beats NEAR DEATH, empty state, latest-N window
  keeps the true total.
- `DamageStreamTest` — `damage` follows the housekeeping path (day-past files
  gzip, past-retention files delete with per-day+type aggregates kept, kinds
  include `damage_incident`) and the query round-trips real day files
  (recency window, per-player filter).

## Deterministic build

Two clean `local-build.sh` runs produced byte-identical jars:
**138,039 B, SHA-256 `26b7f5ba1b01e37843647244e025da30ec51db6019395c4bfbc38f9428863393`**
(`ExcelProtect-0.1.2.jar` and `ExcelProtect-0.1.2-repro.jar` in the build folder).

## Rig — local Paper 26.3 sandbox, real EliteMobs 10.9.12 jar alongside

Driver: an NMS fake player + scripted damage on a lit sky platform
(`rig/driver/`, `rig/drive.py`); transcripts + damage files under
`rig/evidence/run1..run4/`, canonical set at `rig/evidence/` (run 4).
Every boot printed the new awake line with the probe resolved against the
real EM jar:

```
[ExcelProtect] damage incidents: ON gap=2000ms near-death<=30% elitemobs=hooked
[ExcelProtect] READY version=0.1.2 … retention-days=30
```

### Run 4 (canonical) — every branch in one boot, SEVERE 0

Scripted: zombie melee ×3 → skeleton arrow (real falling-arrow impact) → one
more zombie hit; lava ×3; one fall; a genuine EliteMobs elite (made through
EM's own public `EliteEntity` constructor, level 25 — `EntityTracker`
independently confirmed it) landing one hit; then a 5 HP near-death hit and a
killing hit inside the same window. The six records written to
`damage-2026-11-05.jsonl`, verbatim:

```json
{"ts":"2026-11-05T08:58:17.080-06:00","ts_ms":1793890697080,"world":"world","x":0,"y":111,"z":0,"type":"damage_incident","player":"RigPlayer","player_uuid":"14d09c01-016e-31d0-ae70-da2e4ca07e3e","cause":"PROJECTILE","attacker_kind":"mob","attacker_entity":"minecraft:skeleton","attacker_uuid":"2faedd6a-15db-4d0c-8225-bb26b4ad9c6e","projectile":"minecraft:arrow","elite":false,"start_ts":"2026-11-05T08:58:14.136-06:00","start_ts_ms":1793890694136,"end_ts_ms":1793890694136,"duration_ms":0,"tick_count":1,"total_damage":2,"max_single_hit":2,"health_start":8,"health_end":6,"health_min":6,"max_health":20,"near_death":true,"died":false,"closed":"window"}
{"ts":"2026-11-05T08:58:17.082-06:00","ts_ms":1793890697082,"world":"world","x":0,"y":111,"z":0,"type":"damage_incident","player":"RigPlayer","player_uuid":"14d09c01-016e-31d0-ae70-da2e4ca07e3e","cause":"ENTITY_ATTACK","attacker_kind":"mob","attacker_entity":"minecraft:zombie","attacker_uuid":"98b166e8-3076-4b32-80ab-66a3caa8b045","elite":false,"start_ts":"2026-11-05T08:58:11.187-06:00","start_ts_ms":1793890691187,"end_ts_ms":1793890695034,"duration_ms":3847,"tick_count":4,"total_damage":16,"max_single_hit":4,"health_start":20,"health_end":2,"health_min":2,"max_health":20,"near_death":true,"died":false,"closed":"window"}
{"ts":"2026-11-05T08:58:29.033-06:00","ts_ms":1793890709033,"world":"world","x":0,"y":111,"z":0,"type":"damage_incident","player":"RigPlayer","player_uuid":"14d09c01-016e-31d0-ae70-da2e4ca07e3e","cause":"LAVA","attacker_kind":"environment","start_ts":"2026-11-05T08:58:24.086-06:00","start_ts_ms":1793890704086,"end_ts_ms":1793890706034,"duration_ms":1948,"tick_count":3,"total_damage":6,"max_single_hit":2,"health_start":20,"health_end":14,"health_min":14,"max_health":20,"near_death":false,"died":false,"closed":"window"}
{"ts":"2026-11-05T08:58:37.033-06:00","ts_ms":1793890717033,"world":"world","x":0,"y":111,"z":0,"type":"damage_incident","player":"RigPlayer","player_uuid":"14d09c01-016e-31d0-ae70-da2e4ca07e3e","cause":"FALL","attacker_kind":"environment","start_ts":"2026-11-05T08:58:34.034-06:00","start_ts_ms":1793890714034,"end_ts_ms":1793890714034,"duration_ms":0,"tick_count":1,"total_damage":6,"max_single_hit":6,"health_start":20,"health_end":14,"health_min":14,"max_health":20,"near_death":false,"died":false,"closed":"window"}
{"ts":"2026-11-05T08:58:45.033-06:00","ts_ms":1793890725033,"world":"world","x":0,"y":111,"z":0,"type":"damage_incident","player":"RigPlayer","player_uuid":"14d09c01-016e-31d0-ae70-da2e4ca07e3e","cause":"ENTITY_ATTACK","attacker_kind":"mob","attacker_entity":"minecraft:zombie","attacker_uuid":"836d8aeb-66fa-43f4-8dbf-0a1c20c331c5","elite":true,"elite_level":25,"start_ts":"2026-11-05T08:58:42.571-06:00","start_ts_ms":1793890722571,"end_ts_ms":1793890722571,"duration_ms":0,"tick_count":1,"total_damage":19,"max_single_hit":19,"health_start":20,"health_end":1,"health_min":1,"max_health":20,"near_death":true,"died":false,"closed":"window"}
{"ts":"2026-11-05T08:58:50.565-06:00","ts_ms":1793890730565,"world":"world","x":0,"y":111,"z":0,"type":"damage_incident","player":"RigPlayer","player_uuid":"14d09c01-016e-31d0-ae70-da2e4ca07e3e","cause":"ENTITY_ATTACK","attacker_kind":"mob","attacker_entity":"minecraft:zombie","attacker_uuid":"98b166e8-3076-4b32-80ab-66a3caa8b045","elite":false,"start_ts":"2026-11-05T08:58:49.534-06:00","start_ts_ms":1793890729534,"end_ts_ms":1793890730533,"duration_ms":999,"tick_count":2,"total_damage":13,"max_single_hit":10,"health_start":5,"health_end":0,"health_min":0,"max_health":20,"near_death":false,"died":true,"closed":"death"}
```

Checks against the script:
- **Merge + split + merge**: the zombie incident holds exactly the 4 zombie
  hits (16 total) across the interleaved arrow; the arrow is its own
  projectile record resolved to the skeleton (health arcs interlock exactly:
  20 →(3 zombie hits) 8 →(arrow 2) 6 →(4th zombie hit) 2).
- **Lava**: 3 ticks merged, kind environment, no attacker/elite keys. **Fall**:
  single tick. Both close by window.
- **Elite**: `elite:true, elite_level:25`. Note the recorded damage is **19,
  not the scripted 5.0** — EliteMobs replaces its elites' outgoing damage
  with its own scaled value on the real event path, and Protect recorded the
  FINAL damage, which is exactly the number tuning needs.
- **Death**: killing hit inside the window merged (2 ticks, 13 total),
  `died:true`, `near_death` correctly false, closed `death`.
- **Console query** (same boot): all six lines rendered correctly, incl.
  `zombie ELITE Lv25 — 19 dmg`, `skeleton (arrow)`, `DIED`/`NEAR DEATH`
  marks; the `1h` window form, the unknown-player form ("no damage incidents
  recorded"), and the bare-command usage line all answered as spec'd.

### Runs 1–3 (why they exist — reported honestly)

- **Run 1** lost scenario control, not plugin correctness: EliteMobs'
  first-boot world generation starved ticks so the spaced scripted hits
  landed in one burst and hurt-immunity ate them, and natural cave zombies
  at the y=-59 world spawn killed the victim mid-run. Every event that
  actually fired was recorded correctly (the combat stream cross-checks the
  damage records event-for-event), including a 3-tick natural-zombie merge
  across interleaved attackers and a death closing all open incidents.
- **Run 2** (sky platform + settle): all non-elite branches clean in one
  boot — the numbers quoted during the build. Its one gap: an arrow applied
  via `damage(amount, arrowEntity)` fires cause `CUSTOM` with no damager on
  Paper, so the projectile branch fell back to the unit fixture. Run 4's
  real falling-arrow impact closed that gap on the live path.
- **Run 3** proved the elite branch end-to-end first (2 hits × 19 = 38,
  `died:true`) — and showed a second scripted elite hit kills the fake
  player and eats later phases, which is why run 4 scripts one elite hit.
- The `/em spawn elite zombie 25` command (syntax verified from the jar's
  own command class) produced no elite for the NMS fake player in runs 1–2 —
  an EliteMobs-side quirk with fake players, not a Protect input: the
  constructor path above creates the elite through EM's own machinery and
  `EntityTracker.isEliteMob` — the exact API Protect probes — confirms it.

### Not rig-proven (stated plainly)

- Bedrock-player damage (the listener is platform-agnostic Bukkit; no
  platform-specific path exists in the code).
- `elite_probe:"unavailable"` in a live boot (EM present but API moved) —
  covered by unit fixture only; the probe degrades to omitting elite keys,
  never to inventing them.
- Quit/shutdown close reasons — unit-covered; the rig's stops exercised
  shutdown flush through the shared writer (dropped-total=0 at status).
