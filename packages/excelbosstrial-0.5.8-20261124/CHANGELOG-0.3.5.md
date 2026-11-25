# ExcelBossTrial 0.3.5 — changelog

Released as a build on 2026-11-02 (not yet installed). A single-purpose
delta on 0.3.4: guardian spike reflect is retired for tracked seal
elders. Everything else — fights, verdicts, rewards, the 0.3.4
ExcelProtect emitter — is byte-for-byte the 0.3.4 behavior.

## Why

Mike's playstyle-parity ruling (Nov 2, 2026): melee, ranged, and magic
must all stay viable in seal fights; a mechanic that taxes one style
out of the fight is a defect even when the tuning matches the design.
Vanilla elder guardians return 3 HP (Hard) on every melee hit while
their spikes are out. Against a normal 80 HP elder that is trivial;
against BossTrial's governed pools (7,800 solo, 10,900 duo) it lands
on the order of a thousand times per elder, on melee only — and the
attrition pressure ramp scaled it like boss offense, doubling it by
about minute 17 of a long fight. Jester's live feel report the same
night (thorns "hella annoying" at ~1 governed damage per hit dealt)
is the player-side confirmation. Feasibility and design of record:
`~/workspace/seal-audit/REFLECT-FIX-FEASIBILITY-20261102.md`.

## What changed

- **New config key (elder chapter only):**
  `elder-guardian.reflect.multiplier`. Shipped value **0.0** — spike
  reflect from a tracked seal elder during an encounter is cancelled
  outright (Mike's decision, Nov 2, 2026: full cancel, not a scale).
  1.0 restores exact vanilla; 0.25 restores a quarter sting; values
  above 1 are accepted deliberately. The value is a config key with
  the settled value as its code default — never hardcoded
  (adjustability law).
- **Interception:** the existing HIGH-priority boss→player handler
  (`onBossPressureDamage`) judges spike reflect FIRST, through the
  new pure core class `core/ReflectRule`, and applies the chapter
  multiplier INSTEAD of the pressure scale. Reflect is never ramped,
  at any fight length — at multiplier 0.0 the result is 0 regardless
  of ramp ordering. At a scaled value of 0 the event is *cancelled*,
  not zeroed, so the MONITOR fight recorder skips it and reflect
  leaves the fight table's damage-taken column entirely (a melee
  champion's participation evidence comes from their dealt records,
  which are untouched). At intermediate values the scaled reflect
  still records as damage taken, truthfully.
- **Identification is exact.** Paper assigns guardian spikes and the
  Thorns enchantment the same damage cause (THORNS); the pair
  (cause == THORNS, damager resolves to an elder guardian with an
  open encounter ledger) can only be spikes — seal elders wear no
  armor, and a player's own Thorns armor fires in the opposite
  direction, which this handler never sees. Proven on the rig: the
  probe logged `cause=THORNS, damager=ELDER_GUARDIAN, damage=3.0`
  on every spike hit.
- **Scope — what is NOT touched:** vanilla guardians and wild elder
  guardians outside an encounter, elder sprinkle adds (regular
  GUARDIAN elites), player Thorns enchantment in either direction,
  EliteMobs mobs, and the wither/warden/dragon chapters (they have
  no spikes; their `reflect(kind)` reads a neutral 1.0 and their
  handlers never consult it). The reflect branch deliberately runs
  ahead of the pressure-enabled gate: retiring reflect must not
  depend on the attrition layer being switched on.
- **Config plumbing:** `BossTrialConfig` gains a `Reflect` record
  (`elderReflect`, parsed by `reflectFrom` beside `pressureFrom`; a
  config missing the block runs the settled default) and a new
  canonical constructor (the 0.3.0 shape delegates at the settled
  default). The adapter's flat-key whitelist gains
  `elder-guardian.reflect.multiplier`.

## Numbers

- Vanilla spike: 3.0 HP raw on Hard → ≈0.67 HP through the rig's
  netherite set. At 0.0: 0 (event cancelled). At 1.0: unchanged
  vanilla — including NO ramp, where 0.3.4 would have doubled it
  late-fight.
- `ReflectRule` clamps: negative or NaN multiplier reads as 0 (a
  broken value retires reflect rather than restoring the tax);
  NaN/negative raw damage reads as 0; scaled output is always
  finite and ≥ 0, so the fight table can never gain a negative or
  NaN row from reflect.

## Tests

- New `ReflectRuleTest` (31 checks): multiplier math at 0.0 / 0.25 /
  1.0 / 2.0, clamps, cancel semantics, ramp independence (0 × ramp
  stays 0; 1.0 yields vanilla, not the ramped value), the full
  identification-predicate matrix (untracked elder, other chapters,
  player damager, non-THORNS cause), and the TrainingWheels
  composition — Rylie's 0.25 factor composed with reflect in either
  order at multiplier 0 is 0, no special-casing.
- New `ReflectConfigTest` (9 checks): settled default 0.0, flat-key
  parse, missing-block default, per-kind accessor neutrality.
- Full suite: 26,949 checks, 0 failures (0.3.4: 26,909; +40 new).
  Built twice, byte-identical.
- Rig (Paper 26.3-235 + Geyser pattern as 0.3.4, two boots): at
  multiplier 0.0, 11/11 probed spike hits from a tracked elder were
  cancelled and the player took zero reflect; at 1.0, 5/5 landed at
  exact vanilla values; a wild plain guardian's reflect landed
  untouched in both boots; the 0.3.4 Protect `boss_state`
  fight_started emissions still landed in ExcelProtect's store;
  SEVERE 0 in both boots.

## Notes

- First candidate jar (`e963f717…`) failed to load on the rig: a
  colon-space inside this release's plugin.yml description text
  made the YAML unparseable and Paper skipped the jar. The
  description was rephrased, the jar rebuilt (`3364c152…`), and
  plugin.yml/config.yml are now parse-verified inside the built
  jar as part of this build's record. No other content changed
  between the two candidates.
- Installs BEHIND 0.3.4 (the queue order is surfaces → 0.3.4 →
  BedrockMenu 1.5.29 → Menu Phase 2 → 0.3.5): the merged staging
  config here already contains the 0.3.4 `protect-emit` block.
- Player proof owed after install: in the next elder fight, a melee
  champion takes no spike reflect across the whole fight (and, if
  anyone wears Thorns armor, the elder still takes its enchantment
  reflect exactly as vanilla).
