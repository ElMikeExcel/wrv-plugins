# ExcelProtect 0.1.2 — changelog (2026-11-05)

**Why:** ExcelProtect's goal is to track everything, but nothing anywhere
recorded damage TAKEN by players — this morning's elite-tuning question (and
the Kailey lava near-miss investigation) had to be answered from design
numbers because no per-incident damage evidence existed. 0.1.2 closes that
gap.

## Added

- **Damage incidents** — new daily stream `damage-YYYY-MM-DD.jsonl`. One
  record per incident: a player's continuous exposure to one source (same
  attacker identity/type + same cause) within the coalesce window merges into
  a single `damage_incident` record with start/end, tick count, total and
  max-single-hit **final** damage, the player's health arc
  (start/end/min/max), start location, and close reason
  (`window`/`death`/`quit`/`shutdown`).
- **EliteMobs attribution** — mob attackers are probed reflectively through
  EliteMobs 10.9.12's own API (`EntityTracker.isEliteMob`,
  `getEliteMobEntity().getLevel()` — verified against the real jar): records
  carry `elite:true` + `elite_level`. Probe states are explicit (elite /
  probed-not-elite / API-unavailable); soft dependency — Protect runs
  identically with EliteMobs absent.
- **Projectile resolution** — arrow/trident/etc. hits attribute to the shooter
  and keep their projectile type on the record.
- **Environmental incidents are first-class** — LAVA, FIRE, FIRE_TICK, FALL,
  DROWNING, CONTACT, HOT_FLOOR… produce records with no attacker (the lava
  case that motivated this build).
- **Death + near-death flags** — `died:true` on the incident active at death
  (the `deaths` stream snapshot at the same moment is the link; nothing is
  duplicated), `near_death:true` when the player survives at ≤ 30% of max
  health (configurable).
- **Console query** — `excelprotect damage <player> [last-hours]`: recent
  incidents with time, source (elite + level), totals, ticks, health drop,
  NEAR DEATH / DIED marks. Self-logged like the other queries.
- **New awake line** — `[ExcelProtect] damage incidents: ON gap=2000ms
  near-death<=30% elitemobs=hooked` (or the OFF form when disabled).
- **Config keys** (all defaulted; live configs keep working untouched):
  `log.combat.damage-incidents` (true), `combat.damage-incident-gap-ms`
  (2000, clamped 250–30000), `combat.near-death-health-fraction` (0.30,
  clamped 0.01–1.0). `damage` joins the retention path (30 days, gzipped,
  aggregated) and the disk-pressure shed order automatically.

## Unchanged

- Every 0.1.1 stream, toggle, command and behavior. The `combat` stream's
  per-event `combat_damage` rows stay as they are; incidents are a rollup
  beside them, not a replacement.
- Listener cost: one MONITOR handler on player damage, map operations only;
  all IO rides the existing async writer. Nothing synchronous on the main
  thread beyond arithmetic.

## Verification

- 26/26 unit checks (16 carried from 0.1.1 + 10 new), deterministic
  double-build byte-identical (138,039 B, SHA-256
  `26b7f5ba1b01e37843647244e025da30ec51db6019395c4bfbc38f9428863393`).
- Local Paper 26.3 rig with the real EliteMobs 10.9.12 jar: scripted melee
  coalescing + source split, lava, fall, near-death and death incidents all
  produced the spec'd records (see TEST-REPORT.md; run 1's scenario-control
  failure is documented there too — the plugin recorded every event that
  actually fired, correctly, in both runs).
