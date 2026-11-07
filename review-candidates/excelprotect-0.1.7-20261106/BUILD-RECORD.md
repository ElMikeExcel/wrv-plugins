# ExcelProtect 0.1.7 "Parity" — BUILD RECORD

Built Nov 6, 2026 (Iris, subagent session a90bc09f) from the shipped
0.1.6 tree (`~/workspace/protect-recovery-build/`, jar of record
234,451 B, SHA-256 `097a4cbc836ef7252db3510770673c7173531eb900c5500d4d6d26c39566b028`).
Purpose: the two Mike-decided retirement unblocks of Nov 6, 2026 —
(1) "Build the importer fix first, then import clean" (DeathRecord
retirement, backlog Item 13a) and (2) "parity into 0.1.7, then
Telemetry retires" (Item 13b). Nothing else: the broader coverage-map
"Elites & entities" recording expansion is explicitly NOT in this
build.

**Jar of record:** `ExcelProtect-0.1.7.jar` — **246,038 B**, SHA-256
`d7c852b5db14ea52d75a5532c582bb6ad2912c21c601715923c3e9dfb1e83124`.
Deterministic double-build byte-identical (`ExcelProtect-0.1.7-repro.jar`
alongside, same hash). Fingerprint (Layer B, injected at build time,
gate-verified post-build): `EXC-PROTECT-0.1.7-WRV-20261106-01-c017f1`.
plugin.yml: version 0.1.7, **authors: [Iris, Mike]**. Config: NO changes
(no new keys — a pure jar swap behind 0.1.6).

**Tests:** 86/86 green — the full carried 0.1.6 suite (77) plus 9 new
0.1.7 tests (importer end-to-end on a synthetic DeathRecord fixture set
in the real 0.3.0 format, LF-state delta advancement, delta-book math,
Recovery merge monotonicity, markLineRestored/recoverable id parity,
elite-cache merge rules, origin taxonomy, damage-share math, kill-parity
JSON shapes incl. Telemetry's share rounding).

## Feature 1 — DeathRecord importer fix — SHIPPED

Evidence base: `~/workspace/protect-rollout-item13a/` (CENSUS-20261106:
77 deaths, 2,197 burns; **R-burns = 4** — Kailey's already-restored
netherite sword/shovel/boots/helmet, the re-offer hazard; LF-state = 1
file: free-used false, 3 paid recoveries; format audit EVIDENCE.md F1/F2).

- **(a) Restored marks honored.** Burns: the per-item `restored-times`
  in DeathRecord's burn file now drives an emitted `source_restored` /
  `source_restored_times` stamp AND a pre-mark in Recovery's restore
  marks via the new `Recovery.markLineRestored(line)`, which computes
  the entry id from the emitted line itself (`Ledger.emit` now returns
  the rendered line) exactly as `recoverable()` computes it — so the
  imported entry is never offered by `/lostfound` and never
  double-restorable, by construction, not by parallel bookkeeping.
  Deaths: per-item restored marks EXCLUDE the item from the imported
  snapshot (staff restore gives inventory+drops wholesale — inclusion
  would double-give), counted as `source_restored_items`; a death whose
  every item was restored is marked `death:<death_id>` (the same
  once-only mark `Rollback.restoreDeath` checks); `xp-restored-times`
  > 0 omits `xp_level` from the snapshot (restoreDeath's reader treats
  absent as skip) and stamps `xp_restored_in_source`.
- **(b) Lost & Found state migration.** `lostfound/players/<uuid>.yml`
  merges into Protect's per-player state through the new
  `ImportedLfState` delta book (`imported-lf-state.json`): state files
  are MUTABLE (DeathRecord keeps writing them until it retires), so
  they are deliberately NOT path-marked like record files; each import
  applies only the delta (recoveries grown since last applied, free
  flag at most once), and `Recovery.applyImportedLfState` is monotonic
  — never un-uses a free recovery, never lowers a count, never rewinds
  `last_recovery_epoch`. Protect-era recoveries recorded between
  imports are never subtracted (unit-pinned).
- **(c) Idempotency.** Record files keep the 0.1.0 path marks; the
  delta book makes state re-imports exact no-ops. Rig-proven: second
  import of the fixture folder = 8 skipped, 0 errors, state unchanged.
- The console reply and the `type=import` system record now carry
  `restored_burns`, `restored_items`, `lf_state` alongside the original
  deaths/burns/skipped/errors. `Importer.locOf` is fail-soft on an
  unresolvable world (costs the location pin, never the record).
- NOT touched (audit-scoped out): F3 packed-string enchants in
  fingerprints (payloads stay byte-exact; fingerprints are a search
  aid), F4 world-name location pins (records still land; the pin is
  best-effort), F5 drops/inventory folding (verified parity with
  DeathRecord's own restore).

## Feature 2 — Telemetry kill-field parity — SHIPPED

Evidence base: `~/workspace/protect-rollout-item13b/` (TELEMETRY-
CONSUMER-AUDIT + EVIDENCE F3 field diff). All fields land on
`entity_death` records (Entities.onDeath), additive only. Assembly is
split: the pure half (`Entities.killParity`) is unit-tested; the
entity-bound reads are guarded so a parity field can never cost the
death line.

**The elite signal needed one real design decision.** Telemetry does
NOT probe elite status at death — it caches from damage events,
because EliteMobs unregisters a mob from its EntityTracker during its
own death handling. Rig run 1 proved the point on this very build: a
level-5 elite killed in the rig read `elite=false` from an at-death
probe despite `EntityTracker.isEliteMob` returning true at conversion.
0.1.7 therefore caches the victim's elite level on the damage book at
damage time (`CombatCore.onDamage` → `Book.noteElite`, merge rules
unit-pinned: elite sticks, NOT_ELITE yields to a later elite, UNKNOWN
never overwrites) and `onDeath` prefers the cached value, falling back
to the at-death probe for deaths with no recorded damage. Same
EntityTracker mechanism Telemetry reads (Protect's existing
`EliteProbe`, the one the incident streams use).

### Telemetry → Protect field-mapping table (gate G3 reference)

| Telemetry `kill` line | Protect 0.1.7 `entity_death` | Status |
|---|---|---|
| `victim.type` | `entity` | present since 0.1.0 |
| `victim.uuid` | `entity_uuid` | present since 0.1.0 |
| `victim.name` | `name` | present since 0.1.0 |
| `victim.elite` | `elite` | **NEW** — same EntityTracker signal; damage-time cached probe (Telemetry caches identically) |
| `victim.elite_level` | `elite_level` | **NEW** — emitted with `elite:true`; probe-unavailable ⇒ `elite_probe:"unavailable"` replaces both (Protect incident-stream convention) |
| `killer.kind` / `killer.uuid` / `killer.name` | `killer.kind` / `killer.uuid` / `killer.name` | present (Protect attributes via its damage book; extra `killer.final_cause` has no Telemetry counterpart) |
| `weapon{id, enchants, name}` | `weapon` (ItemFact JSON: `id, count, damage?, name?, enchants{}` — a superset) | **NEW** — same signal: killer player's main hand at death time |
| `fight_s` | `fight_s` | present since 0.1.0 |
| `damage.total` / `by_attacker` / `by_cause` | same keys inside `damage` | present since 0.1.0 |
| `damage.fall_share` | `damage.fall_share` | **NEW** — `FALL / total` from the book's by-cause totals, Telemetry's definition + rounding (round1(share·1000)/1000) |
| `damage.explosion_share` | `damage.explosion_share` | **NEW** — `(BLOCK_EXPLOSION + ENTITY_EXPLOSION) / total`, same definition + rounding |
| `origin.class` | `origin_class` | **NEW** — `RgOrigin`, a field-for-field port of Telemetry's RewardGuardOrigin taxonomy |
| `origin.raw` | `origin` | mapped — Protect's pre-existing field stores the Bukkit spawn reason LOWERCASED (its 0.1.0 convention, unchanged to protect existing consumers); G3 should compare `origin_class`/`rg_denied` strictly and `origin` case-insensitively |
| `origin.rg_denied` | `rg_denied` (top level; emitted only when true, as Telemetry emits it only when true) | **NEW** — same PDC keys (`excelrewardguard:origin_v1` / `denied_v1`) |
| `fall_softened` | `fall_softened` | **NEW** — raw fall share ≥ 0.5, always present (false with no damage book, as Telemetry) |
| `flags[]` — `explosion_share_high` (elite kills, share ≥ 0.60) | `flags[]` — same object `{type, explosion_share, threshold}` | **NEW** — Telemetry's `IntegrityFlags.forEliteKill`, same threshold and rounding |
| `flags[]` — `implausibly_fast_kill` | — | **ABSENT, by scope**: Telemetry computes it only for BOSS kills (`forBossKill`) from a per-boss-kind floor table; `entity_death` is the all-entities record and Protect holds no boss-floor table. Not approximated. Boss-fight records are BossTrial/fight_summary territory, not this stream. |

Signal-availability note: every field above is populated from a signal
Protect genuinely reads (EliteMobs tracker, RewardGuard PDC, Bukkit
damage causes, the killer's live inventory). No field is approximated;
the only ABSENT row is the boss-only flag, reason stated.

## Rig (Paper 26.3 + WorldEdit 7.4.6 + real EliteMobs 10.9.12)

Driver `ProtectRigElites` re-runs every 0.1.6 case (A–F) and adds
G (importer round-trip) and H (kill parity). Three runs:

- **Run 1** (0.1.7 jar before the elite-cache fix, 0.1.6's reused rig
  world): G passed end-to-end on the first attempt (4/4 restored-burn
  marks, fully-restored death marked, LF state exact, import #2
  all-skipped). H2 exposed the at-death-probe defect described above
  (elite read `elite=false` at death despite the tracker confirming
  level 5 at conversion) — the catch that produced the damage-time
  cache. A/D/F "failures" were traced to the reused rig world
  (leftover restored stands from the 0.1.6 runs, F's WorldEdit target
  blocks already stone_bricks so the hook's no-op silence was correct
  behavior) and H1 to scenario math (an overkill finisher inflating
  the damage total — Bukkit records full event damage, not the
  HP-clamped remainder; Telemetry accounts identically).
- **Run 2** (elite-cache fix in, fresh world): H2 PASS —
  `elite=true, elite_level=5`, weapon + origin_class + shares on the
  line. A/D/F clean. H1 lost a daylight race (zombie burned:
  FALL 17 + FIRE_TICK 3 = exactly its 20 HP, killer=environment —
  the parity fields themselves were all correct on that line).
- **Run 3** (night-time H cases, widened wolf search): **50 PASS /
  1 FAIL, SEVERE 0 total / 0 attributable.** H1 PASS (fall_share 0.88,
  fall_softened=true, killer=player, weapon=minecraft:iron_sword,
  elite=false, origin_class=other). H2 PASS again. G PASS in full:
  import #1 = 2 deaths / 6 burns / 0 skipped / 0 errors,
  restored_burns=4, restored_items=2, lf_state=1; import #2 = 8
  skipped / 0 errors; the mixed death's snapshot holds 2 items (the
  restored boots excluded) with xp_level intact; the fully-restored
  death carries no xp_level and an empty inventory.
- The single FAIL is `D_entity_wolf_restored`, the documented driver
  proximity artifact (the 0.1.6 rig hit the same case on its run 4):
  the restore itself is proven three ways in the run's own records —
  the console reply (`restored minecraft:wolf 'RigWolf' at
  -2,110,-3`), the system stream's entity_restore commit, and the
  admin stream. The recorded death position (-2,110,-3, beside the
  owner's standing spot, not the spawn point 14 blocks away) shows
  the tamed wolf had teleported to its owner before dying; neither
  the death-point nor the owner-point search found the animal 80
  ticks after its restore — vanilla pet AI, not a restore defect.
  Evidence: `rig-evidence/` (run 3, incl. restore-marks.json,
  lostfound-players.json, imported-lf-state.json,
  imported-sources.json).

## Files

- **New:** `RgOrigin.java` (origin taxonomy port), `ImportedLfState.java`
  (LF-state delta book).
- **Modified:** `Importer.java` (restored marks + state migration +
  extended Result/system line + fail-soft locOf), `Recovery.java`
  (`markLineRestored`, `applyImportedLfState`), `Ledger.java` (`emit`
  returns the rendered line; all existing callers source-compatible),
  `Entities.java` (kill parity assembly + `killParity` pure half),
  `DamageLedger.java` (book shares + elite cache + accessor),
  `CombatCore.java` (damage-time elite probe into the book),
  `ExcelProtect.java` (importer wiring, extended import reply, 0.1.7
  boot lines, status version), `plugin.yml` (0.1.7 + description),
  `local-build.sh` (jar name + fingerprint).
- **Observed, deliberately untouched:** `Ledger.FILE_HEADER` still
  stamps version "0.1.5" — the 0.1.6 build left it as-is (the header is
  the stream-format provenance line, and the true build identity rides
  `excel-build.txt`); changing it mid-lineage is a records decision,
  not a build detail.
