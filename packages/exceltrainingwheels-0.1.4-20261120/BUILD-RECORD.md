# ExcelTrainingWheels 0.1.4 — BUILD RECORD (elite animal-base shield hole)

**Task:** close the animal-base elite hole in the kid shield (decision D157,
superseded by D158 — cover-everything shield rule).
**Date:** 2026-11-20. **Builder:** build worker (parent-seat dispatch).
**Status:** COMPLETE — jar built, 303/303 tests, deterministic double-build
byte-identical, rig proof 10/10 PASS (elite case via the stated fallback),
packet written. Filing/install are the supervisor chain's (out of scope here).

## The bug (verified live, Nov 20, 2026)

Evidence of record: `~/workspace/trainingwheels-livecheck-20261120/` (live config
pull, today's logs, EM file census).

- Rylie (`.RylieDSunflower`, Floodgate UUID
  `00000000-0000-0000-0009-01f05d0d739d`) is protected at factor 0.25; live
  config, identity, plugin health, and EliteMobs compensation (live seal-5
  dealt x2.35, elite factor 0.1064) all verified CORRECT.
- The shield's hostile gate in `onShieldDamage` is `damager instanceof Enemy`
  (Bukkit). A rabbit implements `Animals`, never `Enemy` — so EliteMobs'
  Primis rabbit bosses (`custombosses/primis/primis_crazed_rabbit.yml` L1
  dmgMult 0.2, `primis_crazed_rabbit_large.yml`,
  `primis_crazed_rabbit_farmstead.yml`, `mobproperties/elite_killer_bunny.yml`)
  hit her completely unshielded. She took a full 0.8 in em_primis on
  2026-11-20; debug was on and ZERO shield lines logged all session — the
  gate-failure fingerprint. The plugin's `isElite` check exists but is only
  consulted inside the scaling block, downstream of the failed gate.
- Live jar FILE is named `ExcelTrainingWheels-0.1.7.jar` but the running code
  reports internal version 0.1.3. This build is a delta on the 0.1.3 source of
  record; the new jar's filename and internal version AGREE (0.1.4).

## Source of record

- Base: `~/workspace/exceltrainingwheels/src-0.1.3/ExcelTrainingWheels/`
  (0.1.3 candidate jar 39,077 B, SHA-256
  `81e76017ef14040845c050b908192bc319d08b642cc8ee37041620d5fc7ee189` per
  `build-0.1.3/SOURCE.md`; the 0.1.3 tree itself was clean-room-verified
  against the shipped 0.1.2 jar at its build time).
- Working copy: `src-0.1.4/` in this folder (build/ output dirs stripped).

## The fix (scope SUPERSEDED — D158, Mike's direct, Nov 20, 2026, 10:22 AM CT)

The original D157 elite-bypass scope was stopped before any code was written.
Mike's words: "No. Just cover everything. All her incoming damage is reduced
by .25 except for fall damage and dying from suffocation and stuff. Basically
anything that hits her should be reduced."

NEW SHIELD RULE for protected players — scale ALL incoming damage by the
player's factor (0.25 for Rylie), EXCEPT this environmental exclusion set
(Bukkit DamageCause): FALL, SUFFOCATION, DROWNING, STARVATION, FIRE,
FIRE_TICK, LAVA, CONTACT, HOT_FLOOR, FREEZE, CRAMMING, VOID, FLY_INTO_WALL.

- Everything else scales — including causes previously excluded by design
  (MAGIC / witch splash, POISON, WITHER, THORNS, BLOCK_EXPLOSION,
  ENTITY_EXPLOSION, LIGHTNING, DRAGON_BREATH, SONIC_BOOM, FALLING_BLOCK,
  ENTITY_SWEEP_ATTACK) and damage from ANY damager type: Enemy or not, elite
  or not, tamed animals, other players (PvP included).
- The hostile / `instanceof Enemy` test is REMOVED from the shield path
  entirely. Governing principle for any cause outside the named set: if an
  entity inflicted it, scale it; if the world inflicted it, don't. (Applied
  to the unlisted Bukkit tail: WORLD_BORDER, CAMPFIRE, DRYOUT, MELTING are
  world/block-inflicted → excluded; CUSTOM and KILL are not environmental →
  covered. Each tail call is documented in ShieldRule's javadoc.)
- COMPENSATION STAYS: an EliteMobs-elite damager keeps the existing
  compensation (effective factor = base / EliteMobs' live dealt multiplier),
  so she takes a quarter of the UNSCALED hit. The raid-buff and BossTrial
  pressure divisions — the same class of mechanism serving the same absolute
  line — also stay; they engage only on their existing damager shapes.
  Non-elite and non-entity damage takes the flat factor. Armor reduces
  further afterwards, as vanilla does — that ordering is untouched.
- Source-less causes (POISON / WITHER ticks and any other plain
  EntityDamageEvent) are now reached via a second handler for non-by-entity
  damage events; by-entity events remain owned by the existing handler (no
  double-scaling). Named consideration: the BossTrial pressure pulse was
  recorded in 0.1.3 as never reaching TW as an event (BossTrial 0.3.7 honors
  the factor on its own side), so no double-dip is introduced by covering
  MAGIC here.
- UNTOUCHED: door hold, iron siege, commands, factor semantics, config
  shape. The exclusion set lives in CODE (ShieldRule), not config — it is
  Mike's design law, not a per-server tuning knob, and the config shape
  stays byte-identical.
- Prior shield tests asserting animal / MAGIC / non-boss-cause non-scaling
  are SUPERSEDED by D158 and are rewritten; door-hold / siege / seam tests
  pass unmodified.
- Version 0.1.4: jar filename and plugin.yml version agree.

## Phases

- [x] P0 base verification: unmodified copy (= 0.1.3 source) — 280/280 unit
      checks pass; rebuilt jar SHA-256 `81e76017ef14040845…` = the 0.1.3
      candidate of record, byte-identical. Source of record confirmed.
- [x] P1 fix applied (D158): ShieldRule rewritten (cause-only admission,
      17-cause environmental exclusion set in code); glue hostile gate
      removed, full Bukkit cause mapping, unattributed-events handler added;
      compensation seams untouched; plugin.yml version 0.1.4 + description
      updated to the widened rule.
- [x] P2 unit tests reworked to D158: 303/303 pass (246 carried checks in
      the door/siege/seam suites unmodified + 57 in the rewritten
      ShieldRuleTest). Superseded 0.1.3 admission checks removed and listed
      in the test file's header.
- [x] P3 deterministic double-build: both builds 41,632 B, SHA-256
      `d60416f3de432bc2c8ac618b425510565681d3a8b29222f222fd20bec165abd0`,
      `cmp` byte-identical. Jar filename == internal version (0.1.4).
- [x] P4 rig proof (Paper 26.3-235 + EliteMobs 10.9.8 + TW 0.1.4;
      evidence `rig/RIG-RUN3-runtime.log`, runner done-marker, exit 0,
      SEVERE 0): RIGRESULT 10/10 PASS — elite rabbit composition via the
      fallback below (4.7 -> 0.5, ratio exactly 0.25/2.35; the literal
      live incident 0.8 -> 0.0851), plain rabbit 4.0 -> 1.0, killer bunny
      8.0 -> 2.0, zombie control 8.0 -> 2.0, witch MAGIC 6.0 -> 1.5,
      unattributed POISON 1.0 -> 0.25, FALL 10.0 -> 10.0 unscaled with
      NO SHIELD line, tamed wolf 8.0 -> 2.0.
      RIG LIMIT, stated plainly: a full EliteMobs CustomBossEntity spawn
      was NOT achieved on the rig. The rig's EliteMobs custom-boss
      registry was empty (reflective dump of CustomBossesConfig:
      "EMREGISTRY size=0"), so neither the rig fixture modeled on
      primis_crazed_rabbit.yml nor EliteMobs' own test_boss.yml could
      spawn (18 spawn attempts, both filename forms, ~90 s). The elite
      case was therefore proven against a real rabbit-base entity
      carrying EliteMobs' real elite tag (PersistentTagger.tagElite,
      verified isEliteEntity=true — the exact tag TW's seam reads),
      through the real Bukkit event pipeline on the live rig boot. The
      genuine-boss link is covered by the packet's post-install proof.
- [x] P5 deliverables: jar (41,632 B / d60416f3…), source zip (51,590 B /
      959eb9ed…), SHA256SUMS (self-checking), INSTALL-PACKET.md (lint
      4,918 B / max line 99 / non-ASCII 0), RECEIPTS.md.

## Log

- 2026-11-20 — record created; source of record located
  (`src-0.1.3`), working copy made (`src-0.1.4/`).
- 2026-11-20 — scope superseded by D158 before any code was written;
  record scope section rewritten (see above).
- 2026-11-20 — P0/P1/P2/P3 complete (see phases). One build-path fix
  along the way: the copied tree sits one directory level shallower
  than the 0.1.3 lineage layout, so build.sh's output path was
  corrected from `../../` to `../` (jar bytes unaffected — the
  mis-pathed first build hashes identically to the final one).
- 2026-11-20 — rig run 1 timed out at the runner level (the console
  command was fed without a trailing newline, so it never executed;
  server itself booted clean, SEVERE 0). Run 2 (newline fixed):
  R2–R8 PASS, R1 FAIL — no rabbit boss appeared. Diagnosis across
  runs 2–3: EliteMobs' custom-boss registry is empty on this rig
  (EMREGISTRY size=0) — loose custombosses files are not registered
  without EM's content-package system, so no boss file (fixture or
  EM's own test_boss.yml) can spawn here. Run 3 ran the fallback:
  elite case proven via PersistentTagger.tagElite on a killer-bunny
  base (R1F/R1Fb PASS, exact compensated ratio); all other cases
  PASS again. Full SHIELD lines are quoted in RECEIPTS.md.
- 2026-11-20 — P5 complete. Deliverables in this folder; SHA256SUMS
  verified with `sha256sum -c` as the final step.
