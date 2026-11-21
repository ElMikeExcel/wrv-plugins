# ExcelEMLeash 0.1.0 — BUILD RECORD (EliteMobs leash fix: aggro-clear + plugin-managed radii)

**Task:** build the D167 companion plugin: ONE plugin with two legs fixing
EliteMobs' regional-boss leash loop (investigation:
`~/workspace/em-leash-investigation-20261120/RECORD.md`).
**Date:** 2026-11-20. **Builder:** build worker (parent-seat dispatch).
**Status:** BUILD COMPLETE (P0–P5) — build only; not filed, not
installed, no live boss file edited. Deliverable jar:
`ExcelEMLeash-0.1.0.jar`, 11,438 B, SHA-256
`9da74f34ec97da8227b3531e76e4fe7e921abcdf2325742ead08cf2d29dbc90b`
(double-build byte-identical; see Results).

## Why this exists (one paragraph)

EliteMobs' regional-boss leash walks/teleports a boss home at its
`leashRadius` (soft return at half radius, hard return at full) and heals
it to full on return end — but nothing in the leash path ever clears the
boss's target, so it charges straight back out and the fight loops
(Jester's reports, Nov 18 + Nov 20). EM exposes no drop-aggro config.
Separately, Primis leash radii are often fight-breaking tight (50 bosses
at 20 — soft return at 10 blocks; tail down to 2), and Mike directed the
radius fix be plugin-managed (a floor + per-boss overrides in OUR config)
so it's easy to set in the future and a content-pack update can't
silently undo it. `regenerateCustomBossHealthOnCombatEnd: true` stays
untouched (decided against changing it).

## Design (D167)

### Leg A — aggro-clear
- Listen for EM's `WanderBackToPointEndEvent` (a real Bukkit event EM
  fires when a leash return completes).
- On the event: clear the boss's Bukkit target (`Mob.setTarget(null)`)
  plus any additional EM combat/aggro state the pinned API exposes for
  that entity that is safely clearable (exact set determined in P0 from
  the pinned jar; documented in this record).
- Scope guard: act only on the event's own scope (regional bosses);
  never dungeon/natural elites.
- Result: the boss stays home, healed per EM's design, until genuinely
  re-pulled.

### Leg B — radii management (plugin-managed, NO hand-edits)
- Our `config.yml`: `minimum-radius: 35` (floor), per-boss
  `overrides:` map (boss filename -> radius), per-leg `enabled` toggles.
- Effective radius for a boss = override if set, else
  max(file radius, floor) — applied ONLY to bosses that HAVE a leash
  radius; bosses with none stay unleashed (no invented leashes).
  Values above the floor are never lowered except by explicit override.
- Application route, preference order: (1) RUNTIME — mutate EM's loaded
  boss configuration objects (or the object the spawned entity reads)
  after EM finishes loading and after any EM reload; (2) MANAGED-FILE —
  only if no runtime route exists: the plugin writes floored values
  into the custombosses files itself at boot (each file backed up once
  to `.bak-emleash` before its first write, every change logged) and
  triggers EM's reload path; the reason runtime failed is documented
  with bytecode evidence.
- Either way the floor re-applies every boot/reload; a content-pack
  update cannot silently undo it.
- Logging: a `LEASH` summary line at apply time (bosses seen, how many
  raised, floor used) + per-boss lines at debug.

### Config shape (planned)
```yaml
aggro-clear:
  enabled: true
radii:
  enabled: true
  minimum-radius: 35
  overrides: {}        # "primis_feral_wolf.yml": 40
debug: true            # TrainingWheels/FoodHeal precedent: on until first proof
```

## P0 questions to answer from the pinned jar (10.9.14, sha256 4a6bc4a7…)
1. `WanderBackToPointEndEvent`: package, constructor, getters (what
   entity/boss handle does it carry?).
2. `RegionalBossEntity` / `EliteEntity`: target/combat/damager state —
   what can be cleared safely via public API.
3. `CustomBossesConfigFields.leashRadius`: field type, getter/setter;
   is the fields object the live one the spawn path reads, and is it
   reachable from a static registry (`CustomBossesConfig`)?
4. EM lifecycle: when are custom boss configs loaded relative to plugin
   enable? Any load/reload event? Reload command path?
5. Route decision (runtime vs managed-file) from 3+4, with evidence.

## P0 findings (pinned jar, javap evidence in `build/javap/`)

### Leg A — event + clear set
- Event: `com.magmaguy.elitemobs.easyminecraftgoals.events.WanderBackToPointEndEvent`
  extends Bukkit `Event` with a public HandlerList; getters
  `getLivingEntity()`, `isHardObjective()`. EM's own
  `Navigation.endLeashReturn` listens to it (fullHeal + follow-range
  restore) — the event is the native-path return-completion signal.
  The fallback spirit-walk path (`RegionalBossEntity.checkLeash` ->
  `SpiritWalkSupport`) does NOT reference this event (grep count 0);
  that path only engages when EM's native navigation is unavailable,
  which is not our platform (investigation: native path active on
  Paper, no "Leash task errored!" warnings). Leg A covers the native
  path; the fallback gap is a documented limit, same class as the
  sibling rig limits.
- Entity resolution: `EntityTracker.getEliteMobEntity(Entity)` ->
  `EliteEntity` — the exact static call EM itself makes in
  `endLeashReturn`.
- Clear set (mirrors EM's own combat reset, nothing beyond it):
  1. `Mob.setTarget(null)` — the Bukkit target (the actual loop driver).
  2. `EliteEntity.clearDamagers()` — EM's purpose-built reset bundle:
     bytecode-verified to clear the damagers map, the aggro (threat)
     map, skillDamageContributions, contributionSessions, and to call
     `clearForcedTarget()` (the forced-target lease). EM calls
     `clearDamagers()` from three internal EliteEntity sites and from
     CustomBossEntity — it is EM's own "forget this fight" method.
- Deliberately NOT cleared (EM keeps them, by design): health (EM
  full-heals on return end), the `inCombat` flag + combat watchdog
  (EM's own state machine exits combat on its schedule — stock
  post-disengage behavior; forcing `setInCombat(false)` from our
  handler would fire `EliteMobExitCombatEvent` side effects from the
  wrong trigger), spawn point, powers, follow-range attribute (EM
  restores it in its own `endLeashReturn`).
- Scope guard: act only when the tracker resolves an `EliteEntity`
  that is a `RegionalBossEntity`; anything else is a no-op.

### Leg B — RUNTIME route confirmed (preference 1 exists)
- Registry: `CustomBossesConfig.getCustomBosses()` returns the static
  live `HashMap<String, CustomBossesConfigFields>`; each fields object
  exposes `getFilename()` and `isRegionalBoss()`.
- Setter: `CustomBossesConfigFields.setLeashRadius(double)` —
  bytecode is a bare `putfield`, NO file persistence. (The confusingly
  named `runtimeSetLeashRadius(double)` is the one that calls
  `persistValue(...)`; it is deliberately NOT used.)
- Read path: `RegionalBossEntity`'s constructor captures
  `fields.getLeashRadius()` into its own final field, and
  `Navigation`'s leash maps key off the entity value — so mutated
  fields govern every future (re)spawn. Already-spawned bosses keep
  their captured radius until they respawn (documented; the install
  is a restart, and the floor is applied at enable, before any
  post-install spawn reads the fields).
- Lifecycle: `CustomBossesConfig.initializeBosses()` is invoked from
  the EliteMobs main class in its enable chain; with
  `softdepend: [EliteMobs]` our `onEnable` runs after EM's, so the
  registry is populated. NO EM load/reload event exists in 10.9.14
  (jar event census: the only relevant events are the two
  WanderBackToPoint events) — so re-application after an EM reload /
  pack update / runtime registration is via our own repeating
  self-heal task (5 min) that writes only when a value differs from
  its computed target, plus a first apply 20 ticks after enable as
  belt-and-braces. The LEASH summary logs on the first apply and on
  any re-apply that changed something.
- Note: EM's framework owns the boss files and may persist field
  values on its own schedule (e.g. `RegionalBossEntity.setLeashRadius(int)`
  marks files out of sync for that purpose). If EM ever writes our
  floored in-memory values into a file, the written value is a valid
  `leashRadius` and the floor re-applies on top regardless. Our plugin
  itself never writes boss files — the managed-file route is NOT
  needed and not used.

## Phases
- [x] P0 jar inspection + route decision (evidence in this record).
- [x] P1 source: plugin main, Leg A listener, Leg B applier (pure
      resolution core separated for unit tests), plugin.yml, config.yml.
- [x] P2 unit tests: floor/override resolution (below floor raised,
      above untouched, override wins both ways, no-radius untouched);
      Leg A targeting decisions. **30 passed, 0 failed.**
- [x] P3 deterministic double-build, byte-identical jar (both builds
      and the pre-existing on-disk jar hash identically — see Results).
- [x] P4 mirror-rig proof (sibling pattern): rig ran to completion —
      **8 PASS / 1 FAIL**, the FAIL being the known EM-spawn wall on
      scratch rigs, not a plugin defect; limits stated in Results.
- [x] P5 deliverables: jar, source zip, SHA256SUMS, INSTALL-PACKET.md,
      this record completed. Build only — nothing filed or installed.

## Results (P1–P5, completed by the finisher pass, 2026-11-20)

### P2 — unit tests (run by finisher)
`./run-tests.sh` → **TOTAL: 30 passed, 0 failed.**
- RadiusPolicyTest, 24 checks: below floor raised to floor (incl.
  radius 2 and just-below), at/above floor untouched, no-radius boss
  untouched by the floor, override raises above floor, override lowers
  below floor AND file value, override between file value and floor
  wins, override leashes an unleashed boss, override 0 unleashes,
  override equal to current stays, floor is config-driven (floor 40
  raises 20; floor 10 leaves 20).
- AggroClearPolicyTest, 6 checks: enabled+resolved+regional → CLEAR;
  not regional → SKIP_NOT_REGIONAL; unresolved → SKIP_NO_ELITE
  (unresolved wins over the regional flag); disabled →
  SKIP_LEG_DISABLED (disabled wins over unresolved).

### P3 — double-build (run by finisher)
`build.sh` twice from clean (`rm -rf build/classes` each run; jar
entries stamped 2026-11-20T00:00:00-06:00): both builds produced
**11,438 B / SHA-256
`9da74f34ec97da8227b3531e76e4fe7e921abcdf2325742ead08cf2d29dbc90b`**,
`cmp` byte-identical — and identical to the jar already on disk from
the original worker's 11:33 build. No stale-jar discrepancy: the
on-disk jar was built from the same source that is in `src/` now.

### P4 — rig verdict (from the interrupted run's own artifacts)
The original worker's rig run in fact ran to completion before the
worker ended: `rig/runtime.log` (Paper 26.2-123 + real pinned EM
10.9.14) reaches `Done (79.674s)!`, prints all 9 RIGRESULT lines and
`RIGALL DONE`, and stops cleanly. **SEVERE count: 0** (the ERROR lines
present are sandbox-network SSL noise — authlib yggdrasil key fetch
and Paper's version fetcher; no plugin errors). ExcelEMLeash enabled
(`ExcelEMLeash 0.1.0 ready — aggro-clear on, radii floor 35.0.`) and
the Leg B summary line fired. No re-run was needed: the state was
complete and unambiguous, and the one failing case fails
deterministically for the rig reason below.

Rig results — **8 PASS / 1 FAIL**:
- REG PASS — EM's boss registry booted EMPTY on the scratch rig (the
  sibling wall: with the mob-properties registry empty, EM's own
  validation disables every boss); the driver completed EM's
  initialization with EM's own public calls
  (`MobPropertiesConfig.initializeConfigs`,
  `EliteMobProperties.initializeEliteMobValues`,
  `new CustomBossesConfig()`), yielding the real registry: 17 bosses,
  all 4 `leashrig_*` test files present.
- B1-floor PASS — `leashrig_wolf.yml` in-memory leashRadius 20 → 35.
- B1-file PASS — the boss file on disk still carries
  `leashRadius: 20` (runtime route confirmed end-to-end: the plugin
  never writes boss files).
- B2-override-down PASS — `leashrig_big.yml` 46 → 25 (override).
- B3-override-up PASS — `leashrig_ovr.yml` 20 → 50 (override).
- B4-no-leash PASS — `leashrig_free.yml` stays 0 (floor invents no
  leash for an unleashed boss).
- B5-reapply PASS — after a simulated EM reload (fields object reset
  to the file value 20), the plugin's apply restored 35 — the
  self-heal/re-apply leg works.
- A1-plain-mob PASS — fabricated `WanderBackToPointEndEvent` on a
  non-EM zombie through the real Bukkit pipeline: no throw, target
  kept, plugin logged `SKIP_NO_ELITE`.
- A2-regional FAIL — **rig limit, not a plugin defect** (see below).

LEASH lines captured (live-format evidence for the packet):
`LEASH radii applied: bosses seen 17, with leash 3, floor 35: raised 1, overrides in force 2, values changed 3`
(first apply) and `… values changed 1` (B5 re-apply); per-boss debug
`[debug] LEASH leashrig_wolf.yml: leashRadius 20 -> 35 (floor)`.

### P4 limits, stated plainly (the A2 wall)
Leg A's CLEAR branch was **not** exercised end-to-end on the rig,
for the same class of reason TrainingWheels could not spawn a full
EM custom boss on its rig:
1. A real `RegionalBossEntity` object was created via EM's own API,
   but `boss.spawn(...)` threw inside EM's custom-model path
   (`CustomModel.modelPlugin` null — FreeMinecraftModels is absent
   from the scratch rig), so no boss entity materialized.
2. The driver's fallback — binding a plain zombie to the real boss
   object through EM's tracker — was refused by EM itself:
   `EntityTracker.registerEliteMob` threw `IllegalArgumentException:
   Elite actor must be initialized before registration`. The tracker
   therefore never resolved the entity (`resolves=false`), and the
   shipped handler did exactly what its scope guard requires: logged
   `SKIP_NO_ELITE` and touched nothing (target/damagers/aggro/forced
   target all kept — visible in the FAIL line's detail).
3. Contributing rig artifact: the driver's fake embedded-channel
   player also broke EM's own post-initialization (`EliteMobs
   initialization failed! … packet_handler` — EM cannot inject a
   packet handler into a fake connection). Environment, not plugin.
What IS proven for Leg A: the real event class flows through the
real pipeline to the shipped handler; unresolved and non-regional
entities are skipped untouched (A1 + both `SKIP_NO_ELITE` log lines);
the CLEAR/SKIP decision itself is unit-tested (AggroClearPolicyTest:
enabled+resolved+regional → CLEAR); and the clear set is EM's own
reset bundle, bytecode-verified in P0 (`Mob.setTarget(null)` +
`EliteEntity.clearDamagers()`). The live post-install proof in
INSTALL-PACKET.md is the end-to-end Leg A test — the same posture
the sibling builds took (mirror proof accepted; player-flow proof
after release).
Leg B, by contrast, was proven end-to-end against EM's REAL loaded
registry objects — floor, both override directions, no-leash
untouched, file untouched, reload survival.

### P5 — deliverables
- `ExcelEMLeash-0.1.0.jar` — 11,438 B / `9da74f34…` (above).
- `ExcelEMLeash-0.1.0-source.zip` — the `src/` tree + build/test
  scripts under a `src-0.1.0/` root (sibling convention); hash in
  SHA256SUMS.
- `INSTALL-PACKET.md` — finalized against the final jar (real
  hash/size, real LEASH line formats, runtime-route rollback).
- `SHA256SUMS` — jar + source zip + INSTALL-PACKET.md; verified with
  `sha256sum -c`.

## Log
- 2026-11-20 — record created (write-first). Investigation record read;
  pinned EM jar copied from the audit pull and re-hashed:
  14,563,211 B / SHA-256 `4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd` = the pin.
- 2026-11-20 — P0 complete (findings above); P1 source written
  (ExcelEMLeash, EmLeashBridge, RadiusPolicy, AggroClearPolicy,
  plugin.yml, config.yml); jar built 11:33 (11,438 B / `9da74f34…`);
  INSTALL-PACKET.md drafted 11:21; rig booted ~11:49 and ran its
  cases through 11:51. Original worker ended there, mid-close-out.
- 2026-11-20 — FINISHER pass: tests re-run (30/30); double-build
  verified byte-identical and matching the on-disk jar; rig verdict
  read from the completed run's artifacts (8 PASS / 1 FAIL, A2 =
  rig wall, limits documented); INSTALL-PACKET.md finalized;
  source zip + SHA256SUMS produced and verified. BUILD COMPLETE.
