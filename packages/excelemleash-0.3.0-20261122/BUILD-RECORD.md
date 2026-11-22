# ExcelEMLeash 0.3.0 — BUILD RECORD (D183: MMO-style leash, 96 blocks from spawn)

**Task:** P-LEASH-BOSS-AGGRO-181 as re-scoped by D183 (Mike, Nov 21,
2026, 7:16 PM CT — tapped "96 blocks from spawn"). D183 supersedes
D182 (aggro reset removed entirely; live as 0.2.0 since 6:25 PM
tonight).
**Date:** 2026-11-21. **Builder:** build worker (parent-seat dispatch).
**Scope of THIS pass:** build + rig proof + MANIFEST filing ONLY.
No live install, no live-server edits or restarts, no player contact,
no GitHub posting. The failsafe installs at the first verified-empty
moment under the player-fix push rule.
**Status:** COMPLETE — built, harness 31/31, rig suite fully PASS
(SEVERE 0), packet filed in the MANIFEST. Awaiting failsafe install
at the first verified-empty moment.

**Final artifact:** ExcelEMLeash-0.3.0.jar — 13,655 B / SHA-256
`2b04e90f651767047279ec1a53181916bac0e43cd6371f919c6675986356033e`
(deterministic: two consecutive builds byte-identical).

## Behavior spec (D183, exactly as approved)

- Each elite has a home = its spawn point. While its distance from
  home is <= the leash range (config `leash.range`, default **96**),
  aggro behaves normally — no deaggro ever; a camp fight can never
  trip the leash.
- Pulled MORE than the range from home: the elite deaggros (Bukkit
  target cleared + `EliteEntity.clearDamagers()` — the same two calls
  0.1.0 used), heads home, and heals to full when it arrives home.
- Damaged by a player while heading home: it re-aggroes the damager
  immediately and the heal does NOT happen. It only heals if it makes
  it home untouched. (The free-fight fix — non-negotiable.)
- Aggro still ends on death (either side) or the player leaving the
  world/dimension.
- Applies to all elites uniformly (scope note below); the range is a
  config value, default 96.
- Interplay: the primis_sleep wake-on-damage patch went live tonight
  (19:13) — a boss asleep at home now wakes on damage; the
  return-home state must not fight that.

## Why the return is plugin-run, not EM-native (bytecode + investigation evidence)

Setting EM's `leashRadius` to 96 and letting stock EM run the return
was considered and REJECTED — it cannot meet the spec:

1. EM's native leash is TWO returns: a **soft return at radius/2**
   (investigation RECORD.md, Nov 20: walks home mid-fight at speed
   1.2, `setReturnDuringCombat(true)`) and a hard return at the full
   radius. At radius 96 the boss would start walking home at **48
   blocks** — inside the zone where the spec says aggro must behave
   normally with no deaggro ever.
2. A boss on a native return is **damage-invulnerable**
   (`Navigation.protectReturningBoss` cancels all damage; live
   evidence: Investigation B's MID-RETURN-HIT sample, cancelled=true).
   Spec case (b) — damage mid-return re-aggroes — can never occur on
   the native path.
3. Stock EM **never clears the target** on return (zero `setTarget`
   in every leash-path class) — the original Jester loop. The deaggro
   half of the spec is ours by definition (it was 0.1.0's Leg A).

So 0.3.0 keeps 0.2.0's suppression of the native leash (every loaded
boss's in-memory `leashRadius` set to 0 — no soft return, no
invulnerable return, no native heal timing) and implements the whole
D183 behavior in this plugin: distance poll vs spawn point, deaggro
via the two EM-API calls, pathfinder walk home with a teleport
fallback (mirroring EM's own teleport-on-fail), heal on arrival.

### The heal-timing problem, and its two-layer fix

EM full-heals a boss when combat ends
(`regenerateCustomBossHealthOnCombatEnd: true`, live) — if that heal
fired the moment we clear damagers at return START, the boss would
heal before walking home and "no heal if damaged mid-return" would
be unenforceable. Two layers prevent it:

1. **Heal-event cancel (primary):** `EliteEntity.fullHeal()` fires
   `EliteMobHealEvent` FIRST and returns without healing when the
   event is cancelled (bytecode-verified on the pinned 10.9.14 jar:
   `fullHeal()` constructs the event, calls it via EventCaller,
   checks `isCancelled()` and returns; only then sets health — and
   also calls `clearDamagers()`). While an elite is in our RETURNING
   state, this plugin cancels its `EliteMobHealEvent`s, so no EM
   heal (combat-end or otherwise) can land mid-return.
2. **Health clamp (backstop):** the poll remembers the elite's
   health when the return started; if its health is ever above that
   value while RETURNING (a heal path that bypasses the event), the
   poll sets it back. The boss can only ever LOSE health mid-return.

On arrival home the RETURNING mark is removed FIRST and only then
`elite.fullHeal()` is called — the stock heal event fires
uncancelled, so pack behaviors keyed to it (primis_sleep's
sleep-on-heal at home) proceed exactly as stock, at home, after the
heal. That is the sleep interplay handled: we never cancel a heal
for a boss that is home, and we never set AI or asleep state
ourselves; a sleeping boss damaged at home wakes via tonight's
primis_sleep patch and stock targeting, with no return state
involved (it is not RETURNING — it arrived).

### Scope note — "all elites"

EM's leash system only ever governs **regional bosses**
(`RegionalBossEntity`): they alone have a placed spawn point and
leash AI (investigation RECORD.md). 0.1.0's clear and both versions'
radii legs likewise only ever affected regional bosses. D183's
"applies to all elites uniformly" is implemented on that same
domain: EVERY regional boss, no boss-tier carve-outs (the D181 axis),
no per-boss exceptions. Natural/dungeon elites were never leashed
by EM and are untouched, exactly as in 0.1.0/0.2.0.

## Design (0.3.0)

- **Suppression leg (carried from 0.2.0, unchanged):**
  `LeashRemovalPolicy` + the bridge's registry pass set every loaded
  boss's in-memory `leashRadius` to 0 on enable (+20t) and on a
  5-minute self-heal pass. Renamed in prose only: with D183 this is
  native-leash suppression so EM's half-radius soft return and
  invulnerable return can never engage underneath ours.
- **Return engine (new):** a poll task every 10 ticks iterates
  `EntityTracker.getEliteMobEntities()` (EM's live registry map),
  regional bosses only:
  - Home: recorded from `EliteEntity.getSpawnLocation()` on
    `EliteMobSpawnEvent`, and lazily on first poll sight.
  - Combat test: `elite.isInCombat() || elite.hasDamagers() ||
    Mob.getTarget() != null`.
  - Trigger (`ReturnPolicy.shouldTriggerReturn`): not already
    returning, in combat, distance-from-home > range → deaggro
    (target null + `clearDamagers()`), mark RETURNING with
    home/health/start/progress stamps, path home.
  - While RETURNING: stray targets are cleared each poll (nothing
    may re-aggro it except player damage, below); the pathfinder is
    re-issued `moveTo(home, 2.0)` (EM's hard-return speed); the
    health clamp applies; if no progress for 5 s or the return runs
    60 s, teleport home (EM's teleport-on-fail precedent) and run
    the arrival sequence.
  - Arrival (`ReturnPolicy.hasArrived`: distance <= 2): unmark,
    then `elite.fullHeal()` (stock heal; also re-clears damagers).
  - Player damage (Bukkit `EntityDamageByEntityEvent`, MONITOR,
    ignoreCancelled — only damage that actually lands counts;
    damager resolved directly or via projectile shooter): if the
    victim is RETURNING, unmark and `Mob.setTarget(damager)` —
    instant re-aggro, no heal (the arrival heal simply never runs,
    and any EM combat-end heal from that point is a NEW fight's
    business, not the return's).
  - Heal guard: `EliteMobHealEvent` (HIGH) cancelled while the
    elite is RETURNING.
  - Cleanup: `EntityDeathEvent` drops home + return state; the poll
    sweeps state for entities EM's tracker no longer holds. A
    cross-world elite (target followed through a portal) has its
    return state dropped — dimension change ends the episode, per
    spec.
- **Pure policies (unit-tested on the bare JVM):**
  `LeashRemovalPolicy` (carried, unchanged) + new `ReturnPolicy`
  (trigger / arrival / stuck-fallback / damage decisions).
- **Config:** new `leash:` section — `leash.enabled` (default true
  when absent, so a jar swap onto the live 0.2.0 config still runs
  the leash), `leash.range` (default 96). The 0.2.0
  `leash-removal.enabled` key is still honored as the master switch
  if present and false. The install packet places the new config.yml
  so the live folder matches the shipped shape.
- **Version:** 0.2.0 → **0.3.0** (plugin.yml + ready log line).

## Base gate (established pattern)

Rebuild 0.2.0 from the copied tree BEFORE changes and require
byte-identity with the shipped jar (11,438 B lineage for 0.1.0;
0.2.0: 7,356 B / SHA-256 `7f72e672…`).
Result: **PASS** (2026-11-21) — copied tree rebuilt to 7,356 B /
SHA-256 `7f72e67259552f1a5d188ad0fcfc1fe7ae95e8a675c8b274f1c7ba96be6fe838`
(byte-identical), pristine suites 14/14 before any change.

## Rig plan (scratch only — no live contact)

Local scratch rig copied from the 0.2.0 tree (Paper 26.2-123 + real
pinned EM 10.9.14), new driver with a fake player (the technique
Investigation B proved on its mirror rigs today: a real CraftPlayer
establishes EM combat — aggro map, inCombat — and can damage and be
targeted). Cases:

- REG / R1 / R2 / R3: carried-forward registry cases (suppression
  leg): registry loads; fixtures' radii all 0 in memory; files
  untouched; simulated EM reload re-suppressed.
- (a) PULL-96: combat at home; player walks the boss out past 96
  from spawn → deaggro fires (target + damagers cleared), boss
  returns home, health full on arrival (boss pre-damaged so the
  heal is observable).
- (b) MID-RETURN HIT: return in progress; player damages the boss →
  target = that player immediately, return state gone, health NOT
  healed (stays at damaged value).
- (c) CAMP FIGHT (Marvin regression): fight played out inside 96 of
  spawn (pull to ~40, fight, re-pull) → no return ever triggers,
  target/damagers never cleared by us.
- (d) DEATH + WORLD-LEAVE: boss killed mid-combat → state dropped,
  no errors; player teleports to another world mid-combat → boss's
  aggro ends (target gone; no return triggered at <= 96, combat
  exits on EM's schedule).
- SEVERE 0 across the run.

## Log

- 2026-11-21 ~19:20 CT — record created (write-first). D183 spec,
  D181/D182 log entries, both prior build records, the Nov 20
  investigation record, and Investigation B's rig results read.
  EM bytecode verified this pass: `fullHeal()` heal-event-cancel
  semantics; `EliteMobHealEvent` is Cancellable;
  `EntityTracker.getEliteMobEntities()` map API;
  `EliteEntity.getSpawnLocation()/fullHeal()/isInCombat()/
  hasDamagers()` surface. Native-leash rejection rationale recorded
  above. Base gate PASS (above).
- 2026-11-21 evening — 0.3.0 built (first candidate 13,410 B /
  `2efef451…`); harness 31/31 (14 carried LeashRemovalPolicy + 17
  new ReturnPolicy). Rig iterations burned several runs on driver
  choreography, each diagnosed from logs: the fake player must join
  BEFORE EM's async init completes and must be DAMAGEABLE (an
  invulnerable player is not attackable, so no boss ever acquires
  it — the one clean early pull had a damageable bot); the arena
  needs a roof (undead fixture burns) and width (a chase drifted
  off the south edge); EM regional bosses are singletons that
  respawn, so the driver resets (teleport home) instead of
  removing between cases; every engage phase runs point-blank.
- 2026-11-21 ~21:2x CT — RIG FOUND A REAL PLUGIN BUG (case (b)):
  mid-return damage registered in EM (damager present, health
  dropped) but the return never broke. Cause: the state maps are
  keyed by the EntityTracker map key — EM's persistent elite UUID,
  which for regional bosses survives respawns and is NOT the living
  entity's Bukkit UUID. `pollLeashes()` used the tracker key
  (correct), but `onDamage` / `onDeath` / `onHeal` / `onEliteSpawn`
  looked state up by the raw Bukkit UUID and silently missed every
  time: the damage-break never fired and the heal-event guard was
  dead code (the health clamp backstop masked the second half).
  Fix: a `trackerKey(...)` resolver in EmLeashBridge (direct key
  hit, else identity scan of the tracker map) used by all four
  event handlers. Rebuilt: 13,655 B / `2b04e90f…`, harness still
  31/31, deterministic across two builds.
- 2026-11-21 ~21:30 CT — FINAL RIG RUN (preserved:
  `rig-runtime-final.log` in this folder): **10/10 PASS, SEVERE 0.**
  REG, R1-all-unleashed, R2-files-untouched, R3-reapply,
  FAKEPLAYER, **A-pull96** (trigger at 96.5–96.9 in every observed
  cycle; damagers cleared; walked home; healed to full on arrival),
  **B-midhit** (plugin line: "LEASH return broken: ZOMBIE damaged
  by LeashRigBot mid-return — re-aggroed, no heal"; hp 17.3 vs
  return-start cap 17.5; boss re-engaged at 76.7 blocks and fought),
  **C-camp** (10 samples over 200t at ~40 blocks: no return, no
  aggro loss, health stayed damaged — Marvin regression closed),
  **D-death** (killed mid-combat; respawned at home, aggro ended,
  no return state), **D-worldleave** (bot left the world; EM combat
  cleared on its own schedule; no return triggered; boss stayed in
  its world). The heal-event guard also fired live in the run
  ("LEASH heal held: heal cancelled for a returning elite").
- Known EM-stock behavior observed, NOT a plugin defect: during a
  long chase EM's damager tracking can expire (boss keeps chasing
  on target alone and EM combat-heals it on combat exit). The
  trigger accepts target-only combat, and case (b)'s no-heal rule
  is judged against the return-start health for exactly this
  reason. Also observed: a boss that completes a return while its
  attacker stays in follow range may re-aggro and run out again —
  stock EM targeting, unchanged by this plugin in either direction.
- Filing: INSTALL-PACKET.md + RECEIPTS.md in this folder; MANIFEST
  pointer stub filed Nov 21, 2026. G1 live receipt pulled fresh
  this run (Nov 21, 21:31 CT): live ExcelEMLeash-0.2.0.jar 7,356 B
  / `7f72e672…` — matches the expected live jar exactly.
