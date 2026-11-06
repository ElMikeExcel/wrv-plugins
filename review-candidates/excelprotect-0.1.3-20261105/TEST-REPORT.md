# ExcelProtect 0.1.3 — TEST REPORT

Build Nov 5, 2026. Jar `ExcelProtect-0.1.3.jar`, 165,366 B, SHA-256
`f0ebd67e3d8794def0e83e32def3eb86ff5693d976c11ccc5bf003a650bb9177`.

## Unit suite — 37 tests, 0 failures

`local-build.sh` runs JUnit first (console standalone 1.11.4); the jar is
only produced on a green suite. **26 carried from 0.1.2 stay green**; 11 new:

| Test | Proves |
|---|---|
| dealtMergeOneVictimHealthArc | 2 hits inside the window merge to ONE incident; total/max, health start/end/min; report builder reads it back |
| dealtSplitsOnVictimCauseAndDelivery | different victim, and projectile delivery to the SAME victim, split into separate incidents; projectile field renders |
| dealtEliteVictimSerializationShapes | elite victim → `elite:true` + `elite_level:25`; probe-unavailable → `elite_probe:"unavailable"` and no elite claim; report elite sums |
| dealtVictimDeathKillingBlowAttribution | two attackers on one victim: both close `victim_death`, `died:true` + health 0 on exactly the killer's incident |
| dealtSpellDeliveryRecorded | spell-claimed hits carry `spell` and render `[spell: …]` |
| healingSelfMergeWithCapDelta | self merge; worked cap case: 4+4 healed at 19.5/20 → total_healed 8, total_effective 0.5, arc 19.5→20 |
| healingOtherPlayerMergeAndDirections | SPELL heal merge; healer-side (done) and target-side (received) report directions both read the same record |
| healingSplitsOnHealerAndSourceAndUnattributed | healer/source splits; unattributed CUSTOM counts separately in the report |
| newStreamTogglesDefaultOnAndHonorOff | both toggles default ON, honor OFF; STREAMS carries dealt + healing |
| peekClaimDoesNotConsume | peek ×2 leaves the claim; consume still returns it (the §4 ordering contract) |
| dealtAndHealingQueryFormsFromLedgerFiles | real ledger day files on disk → Exporter player-filter scan finds both streams' incidents |

Determinism: built twice via `local-build.sh`; both jars byte-identical
(SHA-256 above; repro jar kept alongside).

## Rig — Paper 26.3 + REAL EliteMobs 10.9.12 + REAL Magic 11.2.5, one boot

Sandbox pattern as 0.1.2 run 4 (fake NMS players RigPlayer + RigHealer,
roofed sky platform y=110, 20-tick hit spacing). Evidence in `rig/evidence/`
(boot-0.1.3.log, dealt/healing/combat day files). Boot lines verbatim:

```
[ExcelProtect] combat: Magic CastEvent feed hooked (spell attribution on)
[ExcelProtect] damage incidents: ON gap=2000ms near-death<=30% elitemobs=hooked
[ExcelProtect] damage dealt: ON gap=2000ms elitemobs=hooked
[ExcelProtect] healing incidents: ON gap=2000ms
[ExcelProtect] READY version=0.1.3 … retention-days=30
```
Done (47.602s); **SEVERE 0**; status `version=0.1.3 … dropped-total=0`.

| Branch | Evidence (stream records, exact) |
|---|---|
| Dealt merge (normal mob) | 3 scripted melee hits → ONE incident: hit_count 3, total 17.71, health 200→182.29 (arc exact) |
| Dealt projectile split | one real arrow (shooter = player) → separate PROJECTILE incident, `projectile:minecraft:arrow`, 1.92, 182.29→180.37 |
| Dealt ELITE victim | genuine EliteEntity (EntityTracker-confirmed, level 25) → ONE incident `elite:true, elite_level:25`, 2 hits, total 1.02 = the EM-scaled final damage (scripted 10/hit), health 70→68.98 exact |
| Victim death | 30-HP zombie killed inside the window → incident closed `victim_death`, `died:true`, health 30→0, 3 hits, 35.42 |
| Self-heal | player splashes Instant Health II on himself ×2 → ONE self incident: 2 events, total_healed 16, **total_effective 14**, health 6→20 (second splash capped — cap-delta on real events) |
| Player→player heal (SPELL) | seeded Magic claim for RigHealer (see limits) + real CUSTOM regain events ×2 → ONE incident: healer RigHealer, `source:SPELL, spell:heal`, 2 events, 8/8, health 10→18 — the 2nd event merged via the attribution cache AFTER CombatCore consumed the claim |
| Splash boundary (honest) | RigHealer's splash onto RigPlayer recorded as RigPlayer SELF (8/8, 8→16) — the event names no thrower; documented vanilla limit behaving exactly as SPEC §3 states. The same splashes DAMAGING undead produced correctly-attributed dealt incidents for both throwers, incl. one against the elite |
| Cap at full health | RigHealer caught by splashes at 20 HP: total_healed 14 and 7 with total_effective **0** |
| Console | `dealt RigPlayer` → "1282.87 total (3.02 to elites), 15 incident(s)" + per-victim + ELITE Lv25 line incl. EM's custom name `"Lvl 25 Elite Zombie"`; `healing RigPlayer` / `healing RigHealer` split self/done/received correctly; `dealt Ghost` → clean no-records line; `damage RigPlayer` (0.1.2 regression) → clean no-records (nobody hurt the fake players this run) |

## Volume estimate (measured, not modeled)

Busy-fight phase: **240 scripted hits at 4 hits/s for ~60 s**, rotating
6 victims (each re-hit every 1.5 s, inside the window) → **6 dealt
records. 40 hits per record (40:1 coalescing).** Whole run: 264
player→mob hits of every kind → 20 dealt records (the one-off splashes
and the single arrow are inherently one-engagement records).

Live-server translation: a player in continuous combat produces roughly
one dealt record per victim-engagement — a busy boss fight with adds is
tens of records per player, not hundreds; a quiet day is near zero. At
~370 B/record the stream is a few hundred KB on a heavy day, inside the
existing 30-day gzip/retention path. Shed counters cover both new
streams (`dealt, healing` sit after `damage` in the shed order).

## Honest limits

- **Fake-player potion EFFECTS do not tick** (run 1 evidence: a
  REGENERATION effect left health pinned at exactly 10.0 and produced
  zero events). Self-healing is therefore proven via instant-health
  application — a real event with a real delta — and NOT via a regen
  effect. On real players, REGEN/SATIATED regain events are bog-standard
  Bukkit and flow through the same code path; the post-install proofs
  include a food/potion self-heal in play.
- **The player→player SPELL heal's CastEvent was synthetic at one hop:**
  the claim was seeded into CombatCore by rig reflection, then REAL
  `EntityRegainHealthEvent`s were fired through the plugin manager. The
  CastEvent→claim hop itself is the 0.1.1-proven Magic feed (hooked live
  in this same boot — see the boot line), and Magic's heal spells emit
  CUSTOM regains natively. A fully-live cast proof rides the
  post-install player proofs (INSTALL-PACKET).
- EliteMobs globally rescales damage on this rig (scripted 6.0 melee →
  4.92–5.90 final; scripted 10 → 0.51 against the level-25 elite). The
  records store FINAL event damage, so every total equals the victim's
  actual health delta — verified arithmetically on the normal, arrow,
  elite, and death arcs above.
- Victim health arcs are event-time reads: healing or environmental
  damage BETWEEN a player's hits moves `victim_health_end` accordingly
  (run 1's unroofed zombies burned in daylight and drifted; run 3's
  roofed platform removed the drift). The record never invents samples.
- Splash-potion healing of another player records as self-healing
  (vanilla names no thrower) — SPEC §3; proven live above.
