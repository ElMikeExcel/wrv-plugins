# Spell logic fixes — FIXES.md (built Nov 2, 2026)

Fixes for the findings in `~/workspace/spell-audit/LOGIC-TESTS-20261102.md`,
approved by Mike Nov 2, 2026 ("do what you think is best, go for all").
Config-only: every changed value is a plain `spells.yml` number (adjustability
law). No live changes were made by this pass — these are staged files plus a
queue packet.

## Bases — integrity gates (all PASS)

Bases are the ExcelTogether 0.5.2 staging files — the Window #2 files of
record. Verified by sha256 against the MANIFEST's recorded final hashes
before any edit; the first issue of this brief stopped at this gate because
its bases (the pre-merge Fling staging files) were stale, and using them
would have reverted the Repair Item 30-minute cooldown and the Repair
Equipment confirmation gate. Those two spells are NOT touched here.

| base | sha256 (verified) |
|---|---|
| `party-mesh/build-0.5.2/staging/upload-spells-s2.yml` | `91240009bf43e6a630382435465aa0aaab244a147274b5f41b70889ab18966a1` |
| `party-mesh/build-0.5.2/staging/upload-spells-s3.yml` | `29dfe0dc2d256908a37931bbdb491becdcbd005818772e716e2318aafdf07f9c` |
| `party-mesh/build-0.5.2/staging/upload-spells-s4.yml` | `4594b8823912160f36482821aee8a9978d3ba79943df1e323797f268bb09805f` |
| `party-mesh/build-0.5.2/staging/upload-spells-s5.yml` | `fa31fa208f34b23a1bb0740d03ff139e0b4cc01539e3236570de00f6fc48b741` |

## Principles applied

- **P1 (damage sustainability):** no damaging spell stays mana-free at max
  cast rate at its unlock pool tier. Tiers (pool → regen/s): 60→5, 100→15,
  150→25, 200→35. Sustained drain (mana ÷ cooldown) is tuned above the
  tier's regen so holding the button empties a full pool in ~15–45 s.
  Mana is raised in preference to lengthening cooldowns; the one cooldown
  change is the floor fix (no cast-spell rank keeps a cooldown < 1000 ms).
  Rank progression is preserved: no higher rank is more mana-efficient per
  cast than a lower one.
- **P3 (Storm Shackle upkeep):** upkeep targets = unlock-tier regen × 1.2 —
  6 / 18 / 30 / 42 mana per second for ranks 1–4 (net bleed ≈ 1 / 3 / 5 / 7
  per sec at the unlock tier → ~60 / 33 / 30 / 29 s of channel from a full
  pool). Mike's in-game verdict Nov 2 ("press the button, mana doesn't even
  go down") overrides the old "sustainable by design" note.
- **Summons:** where config cannot cap accumulation (jar-proven below), the
  levers are cooldown and a real mana cost, pacing accumulation to
  preparation speed instead of combat speed.

**Assumptions, stated:** (1) a spell's "unlock tier" is the pool tier the
harness/report assigns its finding (the lowest pool at which the old numbers
were self-funding). (2) `masterattack` and `sweep` exist only in the stage-4/5
templates, outside the report's stage-3 corpus; they are check-1 HIGH damage
spells of exactly the class the report ranks HIGH, in files this pass already
edits, so P1 was applied to them too rather than leaving two known-mana-free
damage spells live in later stages. (3) All values were identical in every
stage file where a key exists (verified per file before editing), so the
MagicGate stage-flip law is satisfied by applying the same retune per file.

## Per-spell changes (old → new) with tier math

Drain = mana ÷ cooldown(s). Net = drain − regen at the unlock tier.
T = pool ÷ net = seconds of button-holding to empty a full pool.

| spell (tiers) | files | unlock tier (pool/regen) | old mana / cd | new mana / cd | new drain | net | T |
|---|---|---|---|---|---|---|---|
| wolf | s3,s4,s5 | 100 / 15 (summon) | 50 / 5000 | **100 / 60000** | 1.7/s | — | brief-fixed: 1 wolf/min at a full unlock-pool cost |
| arrow, arrow\|2…\|6 | all | 100 / 15 | 20 / 2000 | **40** / 2000 | 20.0/s | 5.0 | 20 s |
| missile, missile\|2…\|4 | s3,s4,s5 | 100 / 15 | 15 / 1000 | **20** / 1000 | 20.0/s | 5.0 | 20 s |
| missile\|5 | s3,s4,s5 | 150 / 25 | 15 / 850 | **30 / 1000** | 30.0/s | 5.0 | 30 s |
| wrv_spark | all | 60 / 5 | 6 / 3000 | **20** / 3000 | 6.7/s | 1.7 | 36 s |
| wrv_spark\|2 | all | 60 / 5 | 7 / 3000 | **23** / 3000 | 7.7/s | 2.7 | 22 s |
| wrv_spark\|3 | all | 60 / 5 | 8 / 3000 | **26** / 3000 | 8.7/s | 3.7 | 16 s |
| dash | all | 100 / 15 | 40 / 5000 | **100** / 5000 | 20.0/s | 5.0 | 20 s |
| stream, stream\|2, stream\|3 | s3,s4,s5 | 100 / 15 | 30 / 5000 | **100** / 5000 (base block only; tiers inherit) | 20.0/s | 5.0 | 20 s |
| whip | s3,s4,s5 | 60 / 5 | 25 / 5000 | **40** / 5000 | 8.0/s | 3.0 | 20 s |
| sweep | s4,s5 | 100 / 15 | 50 / 5000 | **100** / 5000 | 20.0/s | 5.0 | 20 s |
| masterattack | s4,s5 | 60 / 5 | 8 / 2000 | **16** / 2000 | 8.0/s | 3.0 | 20 s |
| familiar | s3,s4,s5 | 60 / 5 (summon) | 20 / 30000 | **50 / 300000** | 0.17/s | — | 1 spawn / 5 min; cost = 83% of the unlock pool per cast |
| cat | s3,s4,s5 | 60 / 5 (summon) | 30 / 60000 | 30 / **300000** | 0.1/s | — | 1 cat / 5 min |
| stormshackle upkeep (active_costs) | all | r1 60/5, r2 100/15, r3 150/25, r4 200/35 | 2 / 1.5 / 1 / 0.5 per s | **6 / 18 / 30 / 42** per s | — | bleed 1/3/5/7 | channel ≈ 60/33/30/29 s from full |

Untouched inside edited blocks, by design: wolf `max_wolves: 5`; Shackle
activation mana (10/8/6/5), damage, slowness, range, targeting, and its
settled re-engage cooldown curve; every spell's damage/effects text.

## Wolf — honesty note (the loophole config cannot close)

The retune (mana 100, cooldown 60 s) does NOT fix the actual defect the
logic-test pass proved from the `WolfSpell` bytecode: `max_wolves: 5` caps
only the wolves the spell instance tracks in memory, so a relog orphans the
existing pack (they persist as ordinary tamed wolves) and grants a fresh
budget of 5. **No spells.yml value can close that** — it is class behaviour
in Magic 11.2.5. What the retune does is pace it: a wolf now costs a full
unlock-tier pool (100 mana) and one minute of cooldown, so pack-building is
a preparation activity (~1/min at real cost), not a mid-fight print button —
which matches the settled design line that wolves you raise and bring are
preparation, while printing fresh wolves mid-fight forever was the defect.
Closing the loophole itself needs a class-level fix (an Excel-side wolf
tracker or an upstream change), out of this packet's scope and flagged here
for a separate decision.

## Jar evidence — familiar and cat (Magic-11.2.5.jar, constant-pool + bytecode inspection)

Both spells are plain action spells (`class:` unset) whose cast is a single
`SpawnEntity` action, so the governing class is
`com.elmakers.mine.bukkit.action.builtin.SpawnEntityAction` (the jar's
builtin `FamiliarSpell` class is NOT what WRV's `familiar` uses).

- **No concurrent-cap parameter exists.** The complete parameter set
  SpawnEntityAction reads (constant pool of `prepare`/`getParameterNames`):
  type, entity_types, entity_parameters, reason/spawnReason, tamed, owned/
  set_owner, set_target, set_source, force, wait_for_death, repeat_random,
  on_block, allow_replacement, use_wand_name, parameterize_name, name, baby,
  dy_offset, speed, direction, villager_profession, skeleton_type,
  rabbit_type, horse_variant/style/color, color, loot, spawn/death action
  handlers. Nothing matching max/cap/limit/count is read. The class holds a
  single `entity: WeakReference<Entity>` — the last spawn only, for undo and
  wait-for-death — and never scans the world or counts prior spawns.
- **Consequence:** repeated `cat` casts accumulate permanent tamed cats
  uncapped (the report's check-2 LOW), and `familiar` accumulates its
  weighted-table spawns uncapped (check-2 MEDIUM). With no cap key to set,
  both take the cooldown route (300000 ms = 5 min), and familiar takes a
  real mana cost as well (50 = 83% of its pool-60 unlock tier). Familiar's
  `undo: 20000` only makes the cast undoable for 20 s; it is not a cap.
- **`loot: false` DOES suppress drops — settled from bytecode.** In
  `SpawnEntityAction.spawn`: `getfield loot; ifne skip` — when loot is
  false, the code runs
  `EntityMetadataUtils.setBoolean(entity, MagicMetaKeys.NO_DROPS, true)`.
  Familiar's spawned mobs therefore carry Magic's NO_DROPS metadata and
  drop nothing when killed. The spell's residual value is the living animal
  itself (breeding/food), not drops; the 5-minute cooldown + 50-mana cost
  now pace that stream.

## Storm Shackle mechanism note (brief-vs-file discrepancy, stated plainly)

The brief described Shackle's upkeep as living in "Repeat-loop Mana ACTION
amounts, not active_costs". The actual blocks contain **no Mana action
anywhere in the Repeat loop** (the loop is Retarget / PlayEffects / Damage /
PotionEffect / AreaOfEffect / Delay 250) — the upkeep is the spell-level
`active_costs: mana:` value, which is also exactly what the harness's check
5 measures. Bytecode confirms it is the real in-game mechanism:
`BaseSpell.checkActiveCosts` computes `activeCostScale = (now −
lastActiveCost) / 1000.0` (elapsed **seconds**) and applies each active cost
scaled by it, calling `deactivate()` when the mage cannot pay — so
`active_costs.mana: N` is N mana per second while the channel is up, and the
channel now self-ends when the bleed empties the pool. The edit was made
there (2/1.5/1/0.5 → 6/18/30/42). No `disable_mana_regeneration` was added
anywhere: grep count is **0** in all four output files (house regen law).

Harness check-5 proof (stage-3 file; identical in all files):

| rank | before | after | reading |
|---|---|---|---|
| stormshackle | upkeep 2 ≤ regen 5 at pool 60 | upkeep **6** ≤ regen 15 at pool **100** | above regen at unlock tier 60; flag's reference pool moved up a tier |
| stormshackle\|2 | upkeep 1.5 ≤ regen 5 at pool 60 | upkeep **18** ≤ regen 25 at pool **150** | above regen at unlock tier 100 |
| stormshackle\|3 | upkeep 1 ≤ regen 5 at pool 60 | upkeep **30** ≤ regen 35 at pool **200** | above regen at unlock tier 150 |
| stormshackle\|4 | upkeep 0.5 ≤ regen 5 at pool 60 | *no check-5 row* | 42 > 35 (max regen): no self-funding pool exists |

The residual MEDIUM rows for ranks 1–3 are the harness noting that one pool
tier *above* a rank's unlock tier, regen (which scales 5→15→25→35) overtakes
that rank's fixed upkeep. That is P3 working as specified — upkeep is pegged
to the unlock tier, and the rank ladder is the intended answer for higher
pools (a pool-150 player runs rank 3, which bleeds 5/s at their tier).

## Harness before / after (same bases, same harness)

| file | before (C/H/M/L) | after (C/H/M/L) |
|---|---|---|
| stage-s2 | 114 (0/13/27/74) | 113 (0/6/33/74) |
| stage-s3 (= live set) | 214 (0/22/64/128) | 212 (0/7/74/131) |
| stage-s4 | 260 (0/24/70/166) | 258 (0/8/81/169) |
| stage-s5 | 260 (0/24/70/166) | 258 (0/8/81/169) |

Zero CRITICAL in every file after. (The manual CRITICAL on wolf was an
upgrade of its old check-1 HIGH on bytecode evidence; the retuned wolf reads
check-1 LOW, and the loophole itself is documented above as not
config-fixable.)

**Remaining HIGH rows — each explained (7 per s3 file, 6 in s2, 8 in s4/s5):**

1. **stormshackle, \|2, \|3 — check 1 (activation model), all files.**
   Check 1 prices a toggle by its one-shot activation cost ÷ re-engage
   cooldown. A toggle cannot be "held": recasting cancels it, and its real
   sustain path is the channel, which check 5 (above) now proves bleeds
   above unlock-tier regen at every rank and force-deactivates when the
   pool empties. Clearing this row would require activation mana > 60 at
   rank 1 — an absurd toggle cost on a 60 pool — and the brief's P3
   confines the Shackle edit to upkeep. The spell as a whole is no longer
   mana-free at any tier.
2. **wrv_spark, \|2, \|3 — check 1, all files.** Now referenced at pool
   **100** (before: pool 60): at their pool-60 unlock tier the retune
   bleeds 1.7/2.7/3.7 per sec and empties the pool in 36/22/16 s — P1 met.
   The harness HIGH band (damage + free at pool ≤ 100 + cd ≤ 5000) can
   only be escaped with drain > 15/s, i.e. a > 45-mana starter spell that
   empties the *starting* pool in under 6 s — outside P1's 15–45 s window
   by construction.
3. **whip — check 1, s3/s4/s5.** Same shape: now referenced at pool 100;
   at its pool-60 unlock tier, drain 8 vs regen 5 empties in 20 s. Escaping
   the band needs mana > 75 — more than the entire unlock pool, i.e. the
   spell would be uncastable for the players it unlocks for.
4. **masterattack — check 1, s4/s5 only.** Identical pool-60 shape to whip
   (drain 8, 20 s to empty); same explanation.

Everything the report ranked HIGH that *could* be cleared inside P1's
window is cleared: arrow ×6, missile ×4, dash, stream ×3, sweep now read
MEDIUM at pool 150 (free only two tiers above unlock), missile|5 reads LOW
and its check-4 sub-second row is gone, wolf reads LOW.

Also remaining, deliberately untouched (not HIGH): the check-4 family
cooldown-shrink MEDIUMs for `fling` (25×) and `stormshackle` (16×) — both
are Mike-settled numbers (the Nov 1 Fling retune he approved; Shackle's
Oct 30 ratified re-engage curve); familiar's check-2 MEDIUM (no cap exists
to set — jar evidence above; pacing now 1 per 5 min at 83%-of-pool cost,
drops suppressed); cat's check-2 LOW (same, paced 1 per 5 min).

## Excluded — check 7 (pvp_restricted policy), separate decision

Named here so the exclusion is explicit, per the brief. Not one
`pvp_restricted` value was changed:

- **MEDIUM:** smite, smite|2, smite|3 (elemental; 5/8 damage peers
  restricted).
- **LOW:** the combat-category rows — arrow ×6 + arrow_ice, arrow_light,
  arrow_shock, chainlightning ×3, dash, frost ×4, masterattack, missile ×5,
  stream-adjacent none (stream IS restricted), stun ×3, sweep, whip,
  wrv_frostbite ×3, wrv_spark ×3 (3–4 of 35–42 damage peers restricted).

## Change scope (byte diff vs the verified bases)

Every edit was a single-line value replacement inside the target spell's
own block, applied by an assertion-guarded editor (each planned line's old
value verified before replacement; output line counts identical to base;
every changed line machine-checked to sit inside an in-scope block).

| file | lines changed | blocks touched |
|---|---|---|
| stage-s2 | 14 | arrow ×6, dash, wrv_spark ×3, stormshackle ×4 |
| stage-s3 | 27 | s2 set + missile ×5, stream base, whip, wolf, familiar, cat |
| stage-s4 | 29 | s3 set + sweep, masterattack |
| stage-s5 | 29 | s3 set + sweep, masterattack |

No other file types change. `repairitem` / `repairequipment` blocks are
byte-identical to the 0.5.2 bases.
