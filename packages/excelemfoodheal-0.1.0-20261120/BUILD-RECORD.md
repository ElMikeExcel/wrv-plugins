# ExcelEMFoodHeal 0.1.0 — BUILD RECORD (food heal-over-time in EliteMobs worlds)

**Task:** build the D162 companion plugin: food grants a %-of-max-health
heal-over-time in EliteMobs worlds, because EliteMobs' Advanced Combat
System (ACS) exposes no balance settings in 10.9.14 (research:
`~/workspace/em-healing-research-20261120/RESEARCH.md`).
**Date:** 2026-11-20. **Builder:** build worker (parent-seat dispatch).
**Status:** COMPLETE — build only; not filed, not installed.

## Why this exists (one paragraph)

In EM worlds ACS already converts food to health at a FLAT 1 HP per
saturation point (steak ≈ 12.8 HP) against pools of ~50–148 HP, and its
out-of-combat trickle is 1%/s after 20 s out of combat. Neither number is
configurable in alpha. This plugin adds the missing % layer on top —
it does NOT suppress or alter ACS's own flat food heal, which still
happens exactly as before; the two stack (documented, deliberate).

## Design constants (D162 — all config-driven, these are the defaults)

| Constant | Config key | Default |
|---|---|---|
| Instant heal on eat | `instant-percent` | 1.0 (% of max health) |
| HoT delivery rate | `rate-percent-per-second` | 0.5 (% of max health / s) |
| Budget formula | `total-percent = clamp(saturation × saturation-multiplier, min-total-percent, max-total-percent)` | multiplier 0.75, min 1.0, max 10.0 |
| Combat window | `combat-window-seconds` | 20 (mirrors ACS) |
| Budget expiry | `budget-expiry-seconds` | 90 (from the meal) |
| Tick period | fixed | 20 ticks (1 s) |
| Debug logging | `debug` | **true** (see decision below) |
| EM world name list | `worlds` | `[em_primis, em_adventurers_guild]` |

Budget semantics:
- The budget TOTAL includes the instant 1%: eating grants 1% instantly,
  the remaining (total − 1)% is delivered at 0.5%/s, one tick per second,
  only while the player is out of combat.
- While in combat the remaining budget WAITS (does not drain, is not
  lost). Undelivered budget expires 90 s after the meal.
- Re-eating REPLACES the active budget (never stacks).
- If the player is at full health, ticks deliver 0 and the budget waits
  (same as combat pause; still subject to the 90 s expiry).
- Never heal above max health (per-tick clamp to missing health).
- Remainder is tracked in %-of-max units; each tick's HP is computed
  from the player's CURRENT max health, so an armor swap mid-HoT keeps
  the % bookkeeping exact.
- Max health = live `Attribute.MAX_HEALTH` value (ACS's +30 included).
- Self-only. No cleanse, shield, absorption, DR, or revive — healing
  only. Server-side mechanic: identical for Java and Bedrock, no UI,
  no commands in v1.

Per-food results (vanilla saturation × 0.75, clamped to [1.0, 10.0]):

| Food | Saturation | Total budget | Instant | HoT remainder | HoT duration |
|---|---|---|---|---|---|
| Golden carrot | 14.4 | 10.0% (clamped) | 1.0% | 9.0% | 18.0 s |
| Cooked porkchop / steak | 12.8 | 9.6% | 1.0% | 8.6% | 17.2 s |
| Rabbit stew | 12.0 | 9.0% | 1.0% | 8.0% | 16.0 s |
| Golden / enchanted apple | 9.6 | 7.2% | 1.0% | 6.2% | 12.4 s |
| Cooked mutton / cooked salmon | 9.6 | 7.2% | 1.0% | 6.2% | 12.4 s |
| Suspicious stew / mushroom stew / beetroot soup | 7.2 | 5.4% | 1.0% | 4.4% | 8.8 s |
| Cooked chicken | 7.2 | 5.4% | 1.0% | 4.4% | 8.8 s |
| Bread / baked potato / cooked cod / cooked rabbit | 6.0 | 4.5% | 1.0% | 3.5% | 7.0 s |
| Pumpkin pie | 4.8 | 3.6% | 1.0% | 2.6% | 5.2 s |
| Carrot | 3.6 | 2.7% | 1.0% | 1.7% | 3.4 s |
| Spider eye | 3.2 | 2.4% | 1.0% | 1.4% | 2.8 s |
| Apple / chorus fruit | 2.4 | 1.8% | 1.0% | 0.8% | 1.6 s |
| Raw meats/fish (2.4–3.0 sat band) | ~1.2–3.0 | 1.0–2.25% | 1.0% | 0–1.25% | 0–2.5 s |
| Melon slice / honey bottle | 1.2 | 1.0% (floor) | 1.0% | 0% | — |
| Cookie / berries / dried kelp / cake-tier | 0.4–0.6 | 1.0% (floor) | 1.0% | 0% | — |

Class bands land as D162 specified: steak-class 9.6% (~8–10), bread-class
4.5% (~4–5), snack-class 1.0–2.7% (~2–3 at the carrot end; the floor
foods deliver the instant 1% only — documented, deliberate: trash food
should not sustain).

Saturation source: a built-in vanilla saturation table keyed by
Material (Bukkit exposes no saturation getter for plain items), with a
config `food-saturation` override map (empty by default). Foods not in
the table (modded/custom items) get no budget. Cake is eaten from a
placed block and never fires PlayerItemConsumeEvent — out of scope for
v1 (ACS's flat cake heal still applies). Milk/potions are not edible
Materials and are ignored.

## EM-world detection (method chosen, and why)

The pinned EliteMobs jar (10.9.14, file `EliteMobs-10.9.10.jar`,
SHA-256 `4a6bc4a7…`, audit pull 20261120-062811) exposes a public
registry predicate:

    com.magmaguy.elitemobs.dungeons.EliteMobsWorld
        public static boolean isEliteMobsWorld(java.util.UUID worldUUID)

EliteMobs itself uses this predicate for world-scoped behavior (e.g.
its dungeon food-regeneration class references it). ACS's own internal
mechanics check (`AdvancedCombatModule.mechanicsActive`) routes
through per-player control-mode plumbing, not a public world
predicate, so `EliteMobsWorld.isEliteMobsWorld` is the right public
API. Gate rule (OR of two signals):

    isEmWorld(world) = EliteMobsWorld.isEliteMobsWorld(world.getUID())
                       || worlds-list.contains(world.getName())

The API signal covers every EM-registered world automatically —
including instanced dungeon copies while they exist (exactly where
food sustain matters). The name list (default `em_primis`,
`em_adventurers_guild`) is the belt-and-braces layer for the two
permanent WRV worlds in case a permanent world is ever not in the
registry. Overworld/nether/end satisfy neither signal. The EliteMobs
call is guarded (plugin present + try/catch on linkage errors) so a
missing EliteMobs degrades the gate to the name list alone, and the
plugin still loads (softdepend).

## Combat tracking

Per-player last-combat timestamp, set on EntityDamageByEntityEvent
when the player is the victim (damage taken from an entity) or the
damager (damage dealt; projectile shooters resolved to the player).
In combat = now − lastCombat < 20 s. This mirrors ACS's own 20 s
combat-exit rule with our own tracking, as D162 specifies (EM exposes
no public combat-state getter for this).

## Debug decision

`debug` defaults to **true**, following the TrainingWheels precedent
(debug on until first proof): the post-install proof for this plugin
IS a log read, so FOODHEAL lines must exist from the first live meal.
Lines: meal start (player, food, total %, instant HP), pause and
resume transitions, completion, expiry. The install packet tells the
supervisor lane to flip `debug: false` after the proof lands.

## Structure

- `com.winkreyes.excelemfoodheal.ExcelEMFoodHeal` — Bukkit glue:
  config, listeners (consume + combat), 1 s tick task, health writes.
- `com.winkreyes.excelemfoodheal.HealEngine` — pure state machine
  (budgets, combat marks, pause/resume/expiry decisions) with an
  injected clock; the Bukkit layer reports HP actually delivered so
  the engine decrements by delivered % of current max. Unit-tested
  without Bukkit.
- `com.winkreyes.excelemfoodheal.FoodTable` — vanilla saturation
  table + budget formula (pure, unit-tested).

## Phases

- [x] P0 base verification: EM jar copied from the audit pull and
      re-hashed — 14,563,211 B / SHA-256 `4a6bc4a74e…` = the pin.
      `EliteMobsWorld.isEliteMobsWorld(UUID)` verified on these bytes
      via javap; ACS's own mechanics check routes through per-player
      control-mode plumbing (not a public world predicate), so the
      registry predicate is the right public API.
- [x] P1 source: `ExcelEMFoodHeal` (Bukkit glue) + `HealEngine`
      (pure state machine, injected clock) + `FoodTable` (vanilla
      saturation table + formula) + `WorldGate` (pure OR decision) +
      plugin.yml + config.yml. Per-food table corrected during P1
      against the vanilla saturation data (cooked salmon 9.6 sat →
      7.2%; baked potato 6.0 sat → 4.5%).
- [x] P2 unit tests: **137/137 pass** (FoodTableTest: saturation
      values, steak/bread/snack class bands, ceiling + floor clamps;
      HealEngineTest: delivery rate + completion, combat pause/resume
      with no drain, window refresh on re-hit, replace-not-stack with
      expiry-clock restart, expiry at 90 s incl. mid-pause, full-
      health zero-delivery wait, instant-only floor meal, forget;
      WorldGateTest: OR truth table). One classpath note: paper-api
      signatures need the JetBrains annotations + Guava jars at
      compile time (javac 25 CompletionFailure without them) —
      run-tests.sh carries the full set.
- [x] P3 deterministic double-build: both builds 15,564 B, SHA-256
      `8c7f1cecb1e6a911fdb6d8c1156d5ca6765dfeca9ef646e393c5bb46c1a59d05`,
      `cmp` byte-identical.
- [x] P4 rig proof (Paper 26.2-123 + real EliteMobs 10.9.14;
      evidence `rig/runtime.log`, driver in `rig/driver/`). Boot:
      EliteMobs enabling precedes ExcelEMFoodHeal, the exact enable
      line logged, SEVERE 0. All 10 driver cases PASS:
      API (registry predicate callable on the pinned jar; both rig
      worlds report false as expected — the rig's `em_primis` is a
      driver-made flat world, so the gate exercised the config-list
      leg of the OR, the documented fallback);
      S3 overworld no-op (steak at 40 HP reads 40.0 immediately and
      after 4 s, no FOODHEAL line emitted for the meal);
      S1-instant (50 HP → 51.0 exactly);
      S1 completion (FOODHEAL meal line: budget 9.60%, instant
      1.00 HP, remaining 8.60%; complete line logged; health read
      59.5 at the completion poll vs 59.6 expected — the final
      0.1% tick lands inside the poll window, the complete line is
      the completion witness);
      S2-pause (FOODHEAL pause at remaining=7.10% — 9.6 − 1.0
      instant − 3 ticks × 0.5 = 7.1 exactly; health frozen at 42.5
      at hit+1 s and hit+6 s);
      S2-resume (FOODHEAL resume exactly 20 s after the hit,
      remainder still 7.10% — nothing drained during the pause;
      budget completed at 49.5 vs 49.6 expected);
      S4 clamp (bread at 99 HP samples exactly 100.0 / 100.0);
      S5 replace-not-stack (steak then bread 2.2 s later finishes
      at 46.5, inside the 45.4–47.0 band; stacking would approach
      54.1). Expiry (90 s) is unit-proven on the injected clock,
      including mid-pause; it was not re-run in rig real time.
- [x] P5 deliverables: jar (15,564 B, SHA-256 in P3 above),
      source zip (16,436 B / SHA-256
      `2535d7aa2fcd40e41b21f5c36e5b45b7e5ed542c8aff57774e4fd8099df0a635`),
      SHA256SUMS (verified `sha256sum -c` clean), INSTALL-PACKET.md
      finalized (5,508 B / max line 118 / non-ASCII 0), RECEIPTS.md.
      Build only — nothing filed, nothing installed.

## Log

- 2026-11-20 — record created (write-first). Research base read;
  EliteMobs world-registry API verified on the pinned jar via javap
  (`EliteMobsWorld.isEliteMobsWorld(UUID)`); design constants and the
  per-food table fixed above before any code.
- 2026-11-20 — P0/P1/P2/P3 complete (see phases). Rig assembled from
  the WeekendXP 1.3.0 rig skeleton (Paper 26.2-123 + the pinned
  EliteMobs jar and its live-shaped data folder), other plugins
  stripped; driver `EmFoodRig` modeled on the TrainingWheels NMS
  fake-player pattern and compiled against the rig's patched server
  jar (`versions/26.2/paper-26.2.jar`) + rig libraries.
- 2026-11-20 — P4 rig run complete: done-marker, exit 0, SEVERE 0,
  all 10 RIGRESULT cases PASS (detail in the P4 phase line).
  P5 deliverables written and hash-verified. BUILD COMPLETE.
