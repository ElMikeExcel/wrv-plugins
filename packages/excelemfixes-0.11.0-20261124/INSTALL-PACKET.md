# INSTALL PACKET — ExcelEMFixes 0.11.0 (arcane-bolt correction)

**What this is:** a correction release for ONE module of the pack
(arcane-bolt), driven by upstream EliteMobs issue #243. MagmaGuy
replied that EliteMobs applies Spell Echo to ability damage itself,
so the module's buff re-application would count every buff twice.
Measured on our live EliteMobs build (10.9.14) he is right — and
the measurement also found the module's carrier-projectile trigger
never fires under EliteMobs' modeled-projectile path
(FreeMinecraftModels), so the module has been silently inert on
live. 0.11.0 fixes both: the buff half is removed, and the trigger
now pairs EliteMobs' own ability events. Full evidence:
~/workspace/arcane-bolt-spellecho-review-20261124/RECORD.md

## Changes (arcane-bolt module only; all other modules untouched)

- Compensation formula: `extra = dealt x (scale - 1)` — the buff
  factor is gone. `dealt` is the final damage on EliteMobs' own
  `EliteMobDamagedByPlayerEvent`, i.e. it already includes
  EliteMobs' own Spell Echo application.
- Trigger: `ClassAbilityAffectEliteEvent` (DAMAGE) marks the
  (caster, elite) pair; the matching custom-damage
  `EliteMobDamagedByPlayerEvent` inside a 10-tick window supplies
  the hit. The ProjectileHitEvent carrier watch and the
  TimedCombatModifiers reflection bridge are removed.
- Config: `buff-application-enabled` is deprecated + ignored (a
  one-time warning logs if an old config enables it). WRV keeps
  `ability-damage-scale: 1.5`. Module docs (config comments +
  README) rewritten with the reason, per the module workflow rule.

## Artifacts

- Jar: `ExcelEMFixes-0.11.0.jar` — 456,032 B — SHA-256
  `98269ab667d0a5fb4e85cd38b7715f855e0e890ed858fa1ed062fff0fcc200f2`
  (deterministic build: two builds byte-identical)
- Deploy config: `deploy-config.yml` — 61,436 B — SHA-256
  `f076a3517a4ad38935ea7c55bbf5f612be3d51633cf814eabe4e43f56d80edd3`
  — the live config (5ab26780…) with ONLY the arcane-bolt section
  changed (comments + `buff-application-enabled: false`; scale
  stays 1.5). Diff-verified: no other section differs.

## Provenance gates (STOP and re-base if any fail)

- Live jar `plugins/ExcelEMFixes-0.10.0.jar` must be 461,849 B /
  SHA-256 `22224de8…` (full hash in the 0.10.0 install record).
- Live config `plugins/ExcelEMFixes/config.yml` must hash
  `5ab26780eb195bf67c502fdf7732e6f2273e41ce13929714a0c9b0cd1134cfac`
  (61,011 B). If it differs, re-merge the arcane-bolt section from
  this packet's deploy config into the live file instead of
  placing the deploy config.

## Placement

1. Snapshot first (standard window discipline).
2. Jar: MOVE `ExcelEMFixes-0.10.0.jar` to
   `ExcelEMFixes-0.10.0.jar.bak-20261124-pre0110` (move, never copy
   — the 0.10.0 window's duplicate-jar lesson), place
   `ExcelEMFixes-0.11.0.jar`.
3. Config: back up live config to
   `config.yml.bak-20261124-pre0110`, place deploy config.
4. No other files. No data folders touched.

## Acceptance at install

- Boot log: `ExcelEMFixes 0.11.0 ready` with 19 modules and
  `Arcane Bolt Balance ready - ability damage scale 1.5 (gear-gap
  scale only; EliteMobs applies timed buffs like Spell Echo
  itself).`
- No deprecation warning about `buff-application-enabled` (the
  deploy config sets it false).
- `emfixes reload` shows ZERO "reload failed" lines.
- SEVERE 0.

## Proof

- Unit suites: 1029 checks green (the one intermittent failure
  seen across runs is the pre-existing skill-nudge tie-break
  flake, unrelated; all arcane checks pass every run).
- Rig proof on the live configuration (Paper 26.1.2 + EliteMobs
  10.9.14 + FreeMinecraftModels 2.15.0, real cast path):
  control cast EM 34.88 + module 17.44 = 52.32 (x1.5 exactly);
  Spell Echo cast EM 40.50 (echo applied by EliteMobs itself) +
  module 20.25 = 60.74 (x1.5 exactly — the old formula would have
  added 29.61). Lines in
  ~/workspace/arcane-bolt-spellecho-review-20261124/evidence/.
- Proof owed after install (player-ride): Jester casts Arcane
  Volley in Primis — abilities should now land visibly harder
  (+50%); optionally flip `arcane-bolt.debug-log: true` for one
  session to watch the ARCANE-BOLT lines, then flip back.

## Rollback

Restore the .bak jar + config backup, restart. No data migration
exists in this release; rollback is a pure file swap.

## Surfaces owed in the same window

See SURFACES.md in this packet (What's New entry + website
plugin-row version bump). Guide is hibernated (no Guide surface).
Upstream reply to MagmaGuy is drafted
(~/workspace/arcane-bolt-spellecho-review-20261124/DRAFT-REPLY-243.md)
but is NOT part of this install — Mike reviews and posts it
separately.
