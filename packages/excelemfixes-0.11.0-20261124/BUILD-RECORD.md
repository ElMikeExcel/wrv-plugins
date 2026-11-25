# Arcane Bolt / Spell Echo review — ExcelEMFixes vs MagmaGuy's correction on EliteMobs issue #243

Lane: `~/workspace/arcane-bolt-spellecho-review-20261124/`
Date: Nov 24, 2026. Live server untouched throughout (all measurement
on a local rig; one read-only config pull via the Panel API).
Nothing installed. Nothing posted.

## Verdict

**(A) — the double-apply defect is real in the module's code on our
live EliteMobs build (10.9.14), and MagmaGuy's correction holds.**
EliteMobs applies Spell Echo to ability damage itself, so the
module's buff factor re-applies a buff EliteMobs already applied.

**With one measured caveat that changes the live-risk picture:** on
the live server's actual configuration, the module never fires at
all — its trigger (a Bukkit `ProjectileHitEvent` on the ability's
carrier projectile) is never raised under EliteMobs'
modeled-projectile path (FreeMinecraftModels, which the mage class
requires and live runs). So no player has ever received the doubled
buff: the module has been **inert, not harmful**, since install.
The defect is latent — it would go live on any setup where the
carrier produces the vanilla hit event — and the module's intended
half (the gear-gap scale) has likewise never been delivered.

**Loot/threat sub-verdict: MagmaGuy's point stands.** The module's
top-up is a direct health/absorption adjustment; it fires no damage
event, so that share earns no damage credit (no loot share, no
threat) in EliteMobs' accounting. The corrected build keeps the
direct adjustment (dealing it as event damage would re-enter
EliteMobs' weapon formula and timed modifiers and distort it) and
documents the limitation in the module's config comments and README.

**Action taken:** corrected build **ExcelEMFixes 0.11.0** (buff
factor removed; trigger rebased onto EliteMobs' own ability events
so the module actually works on the live configuration), full test
suite, rig proof on the live configuration, **filed as a PENDING
install packet — not installed.** Draft reply to MagmaGuy saved
for Mike's review — **not posted.**

## 1. The thread (issue #243, MagmaGuy/EliteMobs)

Full texts: `evidence/issue-243-body.md`, `evidence/magmaguy-reply-243.md`.

Our issue (posted Nov 22, 2026) claimed:

> "Class-ability damage never enters the damage event pipeline:
> `AbilityEffects.apply()` computes the amount with
> `AdvancedDamageScaling.classAbility(...)` and deals it through
> `ClassAbilityDamage.apply()`, which calls the NMS adapter's
> `damageWithoutCooldown(...)` directly. No
> `EntityDamageByEntityEvent` and no `EliteMobDamagedByPlayerEvent`
> is ever fired for ability damage."

> "Spell Echo's STRENGTH effect is stored via
> `TimedCombatModifiers.modifyOutgoing(...)` … and those modifiers
> are applied in exactly one place:
> `TimedCombatModifiers.onPlayerDamagesElite(EliteMobDamagedByPlayerEvent)`.
> Ability damage never fires that event, so ANY- and SPELL-domain
> outgoing modifiers structurally cannot touch ability damage."

> "Side note: `ClassAbilityAffectEliteEvent` looks like the intended
> integration point for this, but nothing in 10.9.10 ever fires it."

And described the shipped module: `extra = dealt x (scale x
buffFactor - 1)`, buffFactor read from live TimedCombatModifiers
state, dealt measured by per-tick HP snapshots after a carrier
`ProjectileHitEvent`.

MagmaGuy's reply (Nov 24, 2026 06:12:51 UTC; headed "Semi-automated
AI reply — Replying on behalf of MagmaGuy based on his
instructions"), tested on 10.9.15 / Spigot 26.3 / real client:

> "Ability damage does go through the damage events. One Arcane
> Bolt at a level 30 elite fired one `EntityDamageByEntityEvent`
> and one `EliteMobDamagedByPlayerEvent` with `isCustomDamage()`
> true, and removed 60.2 health. `damageWithoutCooldown` only skips
> the 10-tick hit cooldown, so volley bolts and zone ticks are not
> swallowed."

> "Spell Echo applies to abilities. Arcane Volley hit for 35.85
> without it and 41.26 with it, 1.15x. It is small and lasts about
> 4 seconds, which makes it easy to miss."

> "`ClassAbilityAffectEliteEvent` fires before each effect, damage
> included, and can be cancelled."

> "So your module currently applies Spell Echo twice, about 1.32x;
> I would drop the buff factor. The direct health top-up also skips
> damage credit, so that share earns no loot or threat."

> "You are right about the part that matters. Ability damage takes
> nothing from the weapon's level or enchantments, so gear
> progression leaves abilities behind weapon hits. Scaling
> abilities with gear is being designed now. There is no date yet."

## 2. The module as live (ExcelEMFixes 0.10.0)

Source: `build-root/src/src/excelemfixes/arcane/` (copy of the
0.10.0 tree). As designed in Nov 2026:

- Trigger: `ProjectileHitEvent` (MONITOR) where
  `ClassAbilityProjectileCarrier.isCarrier(projectile)` and the hit
  entity is a tracked elite; the hit is settled next tick against
  per-tick effective-HP snapshots.
- `extra = dealt x (abilityDamageScale x buffFactor - 1)`, buffFactor
  = caster's strongest unexpired ANY/SPELL modifier read by
  reflection from EliteMobs' live TimedCombatModifiers (fails safe
  to 1.0). WRV config: scale **1.5**, buff application **on**.
- Top-up via setAbsorptionAmount/setHealth — no event, no XP.

## 3. How the key question was settled (10.9.14, not 10.9.15)

Provenance: the EM jar used everywhere below is SHA-256
`4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd`
— the Nov 22 rig copy, the Nov 24 04:09 live pull, and this lane's
rig are byte-identical; plugin.yml reports internal version
**10.9.14**. The verdict applies to the live server.

### 3a. Bytecode chain (javap, JDK 25)

- `NMSAdapter.damageWithoutCooldown` builds a vanilla DamageSource
  and calls `LivingEntity.hurtServer(...)` — the standard NMS
  damage entry point.
- Paper's `LivingEntity.hurtServer` (verified in BOTH the 26.2 and
  26.1.2 server jars) calls `handleEntityDamage(...)` — which
  constructs and fires the Bukkit damage event — then
  `computeAmountFromEntityDamageEvent` → `actuallyHurt`. An NMS
  hurtServer call does **not** bypass events on Paper. The Nov 22
  premise ("direct NMS call ⇒ no event") was a decompile misread.
- `ClassAbilityDamage` wraps the call in
  `CombatDamageContext.runClassAbilityDamage(domain, …)`;
  `EliteMobDamagedByPlayerEventFilter` builds the EM event from the
  Bukkit event; `TimedCombatModifiers.onPlayerDamagesElite` reads
  the class-ability domain from that context and calls
  `event.applyClassAbilityDamageMultiplier(abilityId, multiplier)`
  — EM 10.9.14 is built to apply timed outgoing modifiers to
  ability damage.
- One correction to this lane's own interim note: a bytecode census
  suggested `ClassAbilityAffectEliteEvent` is never fired in
  10.9.14 (only two referencing classes). **Wrong** — it is fired
  from inside `EliteAbilityTargetAuthorization`, and the rig
  measurement below caught it firing on every cast. MagmaGuy is
  right about this event too, on 10.9.14 already.

### 3b. Rig measurement (the decisive evidence)

Rig: Paper 26.1.2 (bot protocol limit; hurtServer verified
identical in behaviour to 26.2's), EM 10.9.14 (live jar), FMM
2.15.0 (live build, pulled from the Nov 24 audit pull), a
Mineflayer bot driven through EM's own test commands
(`/em skill setAll`, `/em class test set … mage`) and the real
cast entry point (`ClassAbilityInput.useAbility` — the same call
the gesture router makes). Full lines:
`evidence/measurements-summary.txt`.

Phase 1 — EliteMobs alone, mage 40 vs a level-30 elite:

- Baseline volley hit: `EliteMobDamagedByPlayerEvent damage=33.37
  custom=true`, matching `EntityDamageByEntityEvent` and an HP
  delta of exactly 33.37. `ClassAbilityAffectEliteEvent` fired.
- With Spell Echo active (live modifier read back from EM's state:
  `multiplier=1.154, abilityId=mage.utility, domain=ANY`): the same
  cast dealt **38.509**, with `classAbilitySources=[mage.utility]`
  and `classAbilityBonusDamage=5.139` on the event. 38.509 / 33.370
  = **1.1540** — EliteMobs applied the buff itself, on 10.9.14.
- Note: a driver-level `AbilityEffects.apply()` cast with the same
  modifier active dealt exactly 33.37 (bonus 0.0) — the buff
  application is primed by the real cast path (engine/input
  layer), not by the damage step alone. Real players cast through
  the engine path, so the application is what players get.

Phase 2 — ExcelEMFixes 0.10.0 on the rig (arcane-bolt only, scale
1.5, buff on, debug on), mage 60:

- Control cast: EM damage 34.88, HP delta exactly 34.88 — **no
  top-up, no module debug line.**
- Echo cast: EM damage **40.496** (sources=[mage.utility], bonus
  5.616 — EM's own application again), HP delta exactly 40.496 —
  module still silent.
- Across every rig cast, `ProjectileHitEvent` count for ability
  carriers: **0**. Control check: a summoned snowball hitting the
  same elite DID produce a ProjectileHitEvent in the same probe,
  so the listener works — under the modeled-impact path the event
  simply never fires. (Without FMM the ACS control layer itself
  won't enable — `controlsEnabled` requires the FMM F-layer — so
  there is no real-cast configuration in which the old trigger
  fires: with FMM the events don't come, without FMM the casts
  don't happen.)

Arithmetic of the latent defect, at WRV's scale 1.5 with echo
1.154: intended total = dealt × 1.5 where dealt already includes
the buff; the old formula adds dealt × (1.5 × 1.154 − 1) = dealt ×
0.731 instead of dealt × 0.5 — the buff counted twice, exactly as
MagmaGuy calculated (his ~1.32× is the buff squared at scale 1.0).

## 4. The corrected build — ExcelEMFixes 0.11.0

Build tree: `build-root/` in this lane (0.10.0 tree + the changes
below; nothing else in the pack changed).

- `CompensationMath.extraDamage(dealt, scale)` — the buff term is
  deleted: `extra = dealt × (scale − 1)`, 0 when scale ≤ 1.
  `BuffEntry`/`strongestBuff` removed.
- `ArcaneBoltModule` rewritten trigger: `ClassAbilityAffectEliteEvent`
  (DAMAGE effect) marks (caster, elite) in the new
  `AbilityHitPairing` (10-tick window, not consumed — a volley's
  steps all belong to the cast); `EliteMobDamagedByPlayerEvent`
  (MONITOR, `isCustomDamage()` required — weapon hits can never
  qualify) supplies `dealt` from the event's final damage; the
  top-up lands next tick via the same absorption/health split and
  per-hit cap as before. The carrier listener, the HP-snapshot
  machinery, and the EmInternals reflection bridge are removed
  (`EmInternals.java` deleted).
- `buff-application-enabled`: deprecated + ignored; a one-time
  warning logs if an old config enables it.
- Docs per the module workflow rule: the module header, the config
  section (shipped + deploy + packet merged config), and the README
  section now carry the reason — the original player feedback
  (abilities falling behind gear), the upstream correction, the
  trigger finding, and the top-up credit limitation.
- Version stamps: plugin.yml + `ExcelEMFixes.VERSION` + build.sh
  → 0.11.0.

Tests: full pack suites — **1029 checks, 0 failed** on clean runs;
the only intermittent failure across repeated runs is the
pre-existing skill-nudge tie-break flake ("armor named first on
the tie"), unrelated to this module. All arcane checks pass every
run, including new ones pinning the corrected behavior (an
echo-inclusive `dealt` of 38.51 at scale 1.5 yields exactly 19.255
— the scale's share only) and the pairing window semantics.
Deterministic build: two builds byte-identical.

- Jar: `ExcelEMFixes-0.11.0.jar` — 456,032 B — SHA-256
  `98269ab667d0a5fb4e85cd38b7715f855e0e890ed858fa1ed062fff0fcc200f2`
- Packet deploy config: live config (61,011 B / `5ab26780…`,
  pulled read-only via the Panel API for this filing) with ONLY
  the arcane-bolt section changed — 61,436 B — SHA-256
  `f076a3517a4ad38935ea7c55bbf5f612be3d51633cf814eabe4e43f56d80edd3`
  (diff-verified against live).

Rig proof of the corrected build (same rig, live configuration,
mage 60):

- Control: EM 34.88, module `ARCANE-BOLT dealt=34.88 scale=1.50
  extra=17.44` next tick — total 52.32 = ×1.5 exactly.
- Echo: EM 40.50 (EliteMobs' own buff application, unchanged),
  module `extra=20.25` — total 60.74 = ×1.5 exactly. The old
  formula would have added 29.61 (total ≈ 70.1).

## 5. Filing (D212) — filed, NOT installed

- Packet: `packet/INSTALL-PACKET.md` (+ jar, deploy config,
  SURFACES.md with the What's New entry + website row note).
- MANIFEST: pointer-stub PENDING entry added under
  `## PENDING entries` in `~/workspace/wrv-install-queue/MANIFEST.md`
  (Nov 24, 2026). Provenance gates name the live 0.10.0 jar
  (461,849 B / `22224de8…`) and live config (`5ab26780…`); the jar
  swap is specified as a MOVE per the 0.10.0 duplicate-jar lesson.
- The upstream reply is a draft only (DRAFT-REPLY-243.md) — Mike
  reviews before anything is posted.

## 6. Residue and rig notes

- The measurement rig lives at `rig261/` in this lane (a COPY of
  the Nov 22 rig, re-based to Paper 26.1.2 with a fresh world);
  the original Nov 22 rig at `~/workspace/arcane-layer-20261122/`
  was never modified. The 26.2 copy (`rig/`) is also preserved.
  Both rigs are stopped; the bot is stopped. Rig worlds contain
  test husks/pads from the measurement — disposable with the lane
  folder, nothing on the Homebase mirror or live was touched.
- The ViaProxy route was not usable from this sandbox (the
  sandbox's network policy intercepts the extra local TCP hop);
  the Paper 26.1.2 downgrade of the rig copy sidestepped it
  without changing the EM jar or the FMM build under test, and
  Paper's hurtServer event behaviour was verified identical in
  the 26.1.2 and 26.2 jars.
- One read-only live pull was made: `plugins/ExcelEMFixes/config.yml`
  via the Panel file API (fleet token), saved as
  `live-config-0.10.0.yml` — hash matches the 0.10.0 install
  record exactly.

## 7. What this means for players (for the wake-up rundown)

- Today on live: mage abilities deal raw EliteMobs damage — Spell
  Echo works (EliteMobs applies it), gear scaling does not (the
  real, upstream-agreed gap). Nobody was ever over-buffed by our
  module; it never fired.
- After 0.11.0 installs: abilities gain the +50% WRV gear-gap
  scale (config knob, shipped default 1.0 for other servers), and
  Spell Echo keeps working exactly once — through EliteMobs
  itself.
- When upstream ships gear scaling for abilities, the module's
  remaining half can be retired the same day, as the original
  issue promised.
