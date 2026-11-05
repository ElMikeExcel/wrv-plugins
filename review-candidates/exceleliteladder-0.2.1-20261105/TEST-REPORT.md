# ExcelEliteLadder 0.2.1 — test report (Nov 5, 2026)

## Artifact

- `ExcelEliteLadder-0.2.1.jar` — **47,232 B**, SHA-256
  `d15c5df2cbb2e8c7b6b15ba6b7543b1b0bac59394463416f4a43cc2593f03dd4`
- Deterministic: build.sh builds twice and `cmp`s them — byte-identical.
- Compile classpath gains the EliteMobs 10.9.12 jar (compile-only; the
  elite-speed listener compiles against its API event classes). The
  product jar packages ONLY our own classes (entry list verified — no
  `com/magmaguy` entries); EliteMobs stays a runtime softdepend.

## Build-time confirmations (from SPEED-NOTE §3, against the real jar)

1. **Accessor:** `EliteEntity.isNaturalEntity()` exists, public, exact
   name (javap on the rig's EliteMobs-10.9.12.jar). Semantics are
   bytecode-exact: the EliteEntity constructor and the normalized-actor
   path both compute it as
   `spawnReason == CreatureSpawnEvent.SpawnReason.NATURAL`.
2. **Registry lookup: NONE CLEAN.** No EliteEntityManager class, no
   static registry field/method on EliteEntity, MetadataHandler is
   plugin-state bookkeeping, and `EliteMindService` (api/mind) only
   accepts an EliteEntity you already hold — it never resolves one from
   a Bukkit Entity. **Trigger 3 (EntitiesLoadEvent catch-up) is NOT
   implemented**, per the spec's own fallback; the omission is recorded
   in CHANGELOG-0.2.1.md. Triggers 1 (spawn) + 2 (combat re-assert) ship.

## Plain-JVM suites — 272 passed, 0 failed (`./run-tests.sh`)

- **Carried suite, re-run against the retuned table:** fixtures S1–S5
  regenerated to the retuned -norm files; fixture matching, peak/no-op
  semantics, patch bytes in EM number form, reader refusals, StepPlan,
  FileApplier apply/no-op/refusal/fault-restore — all green. The
  pre-retune 0.2.0 S4/S5 files (0.11/150/0.40/2.35) are now proven
  OFF-ladder, and `differences` names exactly the three retuned peak
  lines (cap unchanged).
- **Shipped-table equality (67 checks):** the bundled `ladder.yml`,
  parsed with SnakeYAML, equals `LadderTable.builtin()` — the retuned
  values — on all 5 steps × all 6 MobCombat keys; the
  `defaultSectionMap` round-trip equals it too; `matchStep` parity on
  all 7 staged fixtures.
- **Malformed battery (25 cases × 3 = 75 checks):** unchanged from
  0.2.0, still all refused + defect-named + last-good-kept (two battery
  inputs were re-pointed at the retuned curve so they still violate:
  the S4 dealt-decrease case now uses 1.5 against S3's 1.85; the shipped
  text NaN probe now targets step-1's `spawn: 0.10`).
- **Reload flip:** peak spawn edited 0.17 → 0.20 (S4+S5 together) is
  accepted and provably live against a patched MobCombat file; a
  subsequent broken edit is refused and the flipped instance kept.
- **Elite-speed suite (39 checks, NEW):** parse/defaults (absent keys →
  on, amp 0, permanent, no warnings); explicit values kept (incl. EM's
  own amp 1 / 100000-tick shape); clamps with exactly one warning each
  (amp −3 → 0, amp 500 → 127, amp 1.5 → default, amp as text → default,
  duration 0 / −7 → permanent, duration past int range → permanent,
  enabled as text → default); labels (Speed I/II/V/X, plain past X);
  describe/bootText forms; the `shouldBuff` truth table (only
  on + EM-present + natural passes); the hidden-effect spec
  (ambient/particles/icon all false — the exact constants the glue hands
  to `new PotionEffect(...)`).
  - Note: the Bukkit-side construction cannot run on a plain JVM —
    `PotionEffectType` static init requires a live registry (probed:
    `IllegalStateException: No RegistryAccess implementation found`).
    The parameter VALUES are therefore pinned in the pure core class
    and asserted here; the construction line itself is exercised on
    the rig boots (listener loads and registers against the real EM).

## Rig — Paper 26.3-235, EliteMobs 10.9.12, ExcelGuildGate 0.4.0 (real jars)

Copied 0.2.0 rig (`rig/server/`), evidence in `rig/evidence/`, driven by
`rig/drive-0.2.1.py` (console only; no players on a rig — see the speed
proof note for why that matters).

- **R1 — fresh install** (`r1-0.2.1-fresh-boot.log`): no plugin folder,
  EM file at the NEW S3-norm, 3 seals broken. ladder.yml GENERATED;
  enable line `…applied step none (no verified baseline), ladder table:
  ladder.yml (config) — default generated on first boot, …, elite speed:
  ON (amplifier 0, duration permanent)`; `Baseline adopted: live
  MobCombatSettings.yml equals ladder S3-norm (spawn 0.14, cap 120,
  toElite 0.4, toPlayer 1.85, …)`; status shows all five retuned rows,
  `Live file: S3-norm`, `Elite speed: on (Speed I, permanent)`;
  `eliteladder reload` accepted (`Ladder table reloaded … (S5: spawn
  0.17, cap 150, toElite 0.35, toPlayer 1.94, …)`). Done (37.956s).
  **SEVERE 0.** The generated ladder.yml was later verified byte-equal
  to the bundled resource (after R5's boot; cmp clean).
- **R4 — the exclusion gate, proven on a real server**
  (`r4-0.2.1-speedproof.log`): after EM's own "fully initialized" line,
  12 persistent invulnerable zombies summoned; EM converted 2 into
  "Lvl 0 Elite Zombie" (name read back via data get). A direct
  `data get entity @e[name="Lvl 0 Elite Zombie",limit=1] ActiveEffects`
  returned **"Found no elements matching ActiveEffects"** — zero potion
  effects on the converted elite ~15 s after conversion, and zero
  `Elite speed applied` lines. Correct by design: /summon conversions
  carry SpawnReason CUSTOM, and EM's constructor sets
  `isNaturalEntity = (reason == NATURAL)` — the gate refused them,
  exactly the custom/non-natural exclusion Mike specified.
- **R5 — speed toggle off** (`r5-0.2.1-speedoff.log`): config pre-placed
  with `elite-speed.enabled: false` → enable line `…, elite speed:
  OFF`, status `Elite speed: off`, applied step S3 carried over.
  **SEVERE 0.**
- **Diagnostic runs R2/R3** (retained in the build folder's
  rig/evidence): R2 raced EM's async initialization and hit the
  zero-player hard-despawn; R3 established that EM converts only after
  its full initialization and that `execute as … run data get` swallows
  its own feedback. Both findings shaped R4's procedure.

## The one proof that is NOT rig-shaped — stated plainly

The POSITIVE spawn application (a natural elite actually receiving the
buff) cannot be produced on a playerless rig: monster spawns with
SpawnReason NATURAL require the spawn cycle around a player, and every
playerless stand-in (summon, spawner, eggs) carries a non-NATURAL reason
that EM itself classifies as not-natural. What IS proven: the event
classes and accessor against the real jar (javap); the natural-semantics
rule from EM's own constructor bytecode; the gate's exclusion on a live
server (R4); the effect parameters pinned at unit level (39 checks);
boot health with the listener registered against the real EM (R1/R5).
The positive landing is therefore the bundle packet's post-install
player proof: the first natural elite after install is named in the log
by an `Elite speed applied (spawn)` line and visibly moves ~20% faster.
Trigger 2 (combat re-assert) is likewise unprovable without a player
(combat needs a player target); its code path is the same `apply()` the
spawn trigger uses, gated by the same predicate the suite pins.

## Deviations / notes

1. Trigger 3 (chunk-load catch-up) not implemented — no clean EliteEntity
   registry lookup exists in 10.9.12 (above). Spec-anticipated fallback.
2. TestMain/LadderConfigSuite historical blocks were re-pointed from the
   0.1.2-era values to the pre-retune 0.2.0 values (the meaningful
   "previous table" for 0.2.1); old-S0/old-S1 pre-ladder fixtures are
   untouched.
3. The plugin's baseline-refusal hint now cites the new S1-norm file
   sha (`ff017c5d…`, the regenerated fixture) in place of `57ad4ab2…`.
