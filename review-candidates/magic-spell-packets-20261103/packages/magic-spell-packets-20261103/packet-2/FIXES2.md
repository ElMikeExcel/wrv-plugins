# FIXES2 — Spell fix packet #2 (Jester-reports pass)

Built Nov 2, 2026 from the evidence investigation
`~/workspace/spell-audit/JESTER-REPORTS-20261102.md`, under Mike's directive
"just get everything fixed, use your best judgment." This packet chains on
top of the QUEUED logic-fix packet (`../install-20261102/`, "packet #1"):
its bases are packet #1's staged outputs, and it installs only after
packet #1 (see INSTALL-PACKET2.md ordering gate).

Root cause RC-1 (investigation §header): Magic's `bowpull` parameter
variable resolves to `Mage.getLastBowPull()`, which is **0 for every wand
cast** (only a real bow release sets it). Every bowpull-scaled parameter
therefore evaluated to its zero-draw value in real play: projectile speed
0 (arrows dropped at the caster's feet), damage 0, spread max, repeat
counts 0. This packet converts each bowpull expression to the constant a
full draw (bowpull = 1.0) produces, so a wand cast behaves like a
full-draw shot.

## Bases (gate-verified before editing; re-verified after — untouched)

| staged base (packet #1 output) | sha256 |
|---|---|
| `install-20261102/stage-s2/spells.yml` | `0ec3def9ef38331d032dd39dc393609f1dac9442860f3402459b56dc791c4653` |
| `install-20261102/stage-s3/spells.yml` | `66986716c0bd69325d480f3613e11fead7f8d55459bb2116ac55d0db86631f31` |
| `install-20261102/stage-s4/spells.yml` | `a019fa6c447b99d2ecf991a55ab06f872150f3aecda364fc87c79ddacad53daa` |
| `install-20261102/stage-s5/spells.yml` | `6b317053ee78f9365ba055af3d314c4621ff4168a961c6b57dea601f32d5842b` |

Method: block-scoped scripted edits (each replacement asserted to occur
exactly once inside its named spell block; the script aborts on any
surprise). No hand edits.

## Bowpull census — every reference found

`grep -n bowpull` on the four bases: **stage-s2: 0 lines** (the whole
bow family, lightning, and push are absent from the stage-2 file — its
only arrow-adjacent content is the `arrow` spell's constant rank table
and chainlightning icons). **stage-s3 / s4 / s5: 13 lines each**, all in
four single blocks. None of the four spells has rank variants (`|2`…
keys do not exist for them), so there are no inheriting overrides.

| spell | bowpull lines (per file, s3/s4/s5) |
|---|---|
| `arrow_ice` | `speed: bowpull * 3`, `spread: 1 - bowpull`, `arrow_damage: bowpull`, `radius: 1 + 3 * bowpull` |
| `arrow_light` | `repeat: bowpull * 5` (inside the damage Repeat), `gravity: (1 - bowpull) * 0.5` |
| `arrow_shock` | `speed: bowpull * 3`, `spread: 1 - bowpull`, `arrow_damage: bowpull`, `repeat: bowpull * 5 + 10` |
| `arrow_regular` | `speed: bowpull * 3`, `spread: 1 - bowpull`, `arrow_damage: 2 * bowpull` |

**End state: `bowpull` grep (case-insensitive) = 0 in all four outputs.**
No disabled-spell contexts contain bowpull (the census above is the whole
file, disabled or not), so there are no exceptions to name.

## Per-spell changes (identical in s3, s4, s5; s2 pass-through)

### arrow_ice — straight full-draw evaluation (bowpull = 1)
| parameter | old | new | math |
|---|---|---|---|
| `speed` | `bowpull * 3` | `3` | 1 × 3 |
| `spread` | `1 - bowpull` | `0` | 1 − 1 |
| `arrow_damage` | `bowpull` | `1` | 1 |
| `radius` (freeze sphere) | `1 + 3 * bowpull` | `4` | 1 + 3 |

Untouched: the action tree's fixed `Damage` 1, `tick_increase: 500`,
`costs: arrow: 1` (item cost is this family's design).

### arrow_shock — straight full-draw evaluation
| parameter | old | new | math |
|---|---|---|---|
| `speed` | `bowpull * 3` | `3` | 1 × 3 |
| `spread` | `1 - bowpull` | `0` | 1 − 1 |
| `arrow_damage` | `bowpull` | `1` | 1 |
| `repeat` | `bowpull * 5 + 10` | `15` | 5 + 10 |

The Repeat wraps a parameterless `Damage` action (engine default damage
for this class, 5/tick per the investigation's mechanism read), so the
full-draw payload is up to 15 ticks ≈ 75 damage over ~4.5 s on one
entity, plus Disarm + 4 s potion effect. That is the stock spell's own
full-draw behaviour — this packet restores it, it does not invent it.
Untouched: `costs: arrow: 1`, `keep_in_inventory: true`, effects.

### arrow_regular — straight full-draw evaluation; item cost kept
| parameter | old | new | math |
|---|---|---|---|
| `speed` | `bowpull * 3` | `3` | 1 × 3 |
| `spread` | `1 - bowpull` | `0` | 1 − 1 |
| `arrow_damage` | `2 * bowpull` | `2` | 2 × 1 |

`costs: arrow: 1` stays — the physical-arrow cost is this spell's design
(investigation item 7: the family is item-cost by stock design, and
Jester's "zero mana, no cooldown" report was config-accurate, not a
leak).

### arrow_light — JUDGMENT CALLS (two; flagged per brief)
The investigation flags this spell's mechanism as degenerate for a wand
spell, so it is converted to the family's normal shape rather than
evaluated raw:

1. **Damage mechanism: `Repeat(bowpull * 5)` of `Damage` → a single
   `Damage` on hit.** Raw full-draw evaluation would be Repeat 5: up to
   5 × `entity_damage: 5` = **25 damage to one target inside ~1.2 s**
   (player targets: 5 × 3 = 15) — the family's hardest single-target
   burst, on a spell whose identity is utility (block-breaking bolt:
   `block_hit_count: 4`, `target_breakables: 1`, `brush: air`). The
   family's normal shape is one Damage application per hit at
   `entity_damage` (ice's AoE payload, shock's per-tick payload, the
   `arrow` spell's AoE). The conversion keeps the spell's own stock
   timing scaffolding — `Delay 200` → `Asynchronous`
   (`requires_entity_target: true`) — and replaces only the Repeat
   wrapper and its inner `Delay 300` with the single `Damage`, which
   reads the block's existing `entity_damage: 5` / `player_damage: 3`
   parameters. Per-hit damage is now exactly family-consistent, and the
   bolt can still hit up to `entity_hit_count: 5` entities per cast.
   If the burst is ever wanted back it is a one-line config change
   (wrap the Damage in `Repeat` with `repeat: 5`) — noted here so the
   choice is reversible and visible.
2. **Gravity: `(1 - bowpull) * 0.5` → `0`.** Full-draw evaluation is
   arithmetically 0, and it is also the only sane wand value:
   `CustomProjectileAction` multiplies gravity by velocity
   (investigation item 2), so the old at-rest value 0.5 acted as
   effective gravity **50** against velocity 100 — the bolt nosed into
   the ground at the caster's feet and its `ModifyBlock` broke the
   ground there (Jester's vB report, verbatim). `gravity: 0` = flat
   flight, in line with the family's other CustomProjectile bolts.
   Untouched: `velocity: 100`, `range: 80`, hit counts, `ModifyBlock`
   block-breaking (that part is the spell's utility identity, not the
   bug), potion effect, `costs: arrow: 1`.

### lightning (+ ranks) — Spread scatter removed (investigation item 9)
The `radius` parameter is consumed by the `Spread` action, which
re-centres the strike by a random offset of up to ±`radius` blocks while
the damage AoE is radius 3 — the spell was built to miss what you aimed
at, and read in play as "must be on the same block."

| block | parameter | old | new |
|---|---|---|---|
| `lightning` | `radius` | `8` | `0` |
| `lightning|2` | `radius` | `6` | `0` |
| `lightning|3` | — | (no `radius` override; inherits base) | inherits `0` |

At `radius: 0` the Spread offset is zero — the strike lands where
targeted (the AoE `radius: 3` action parameters are a different,
untouched `radius`). Untouched: `damage: 10`, AoE radius 3, mana 80,
cooldowns 45000 / 40000, `range: 32`, the Repeat-2 shape at |3.

### push family — real shove, same progression shape (investigation item 10)
`VelocityAction` sets velocity = direction × `living_entity_speed`.
Old values (0.5 base / 0.75 top) impart roughly a third of a vanilla
knockback swing, delivered in pulses 250 ms apart — a nudge. New values
keep the family's rank spacing (≈+0.25–0.30 per rank, as before) on a
real-shove band:

| block | `living_entity_speed` old | new |
|---|---|---|
| `push` | 0.5 | **1.2** |
| `push|2` | 0.6 | **1.45** |
| `push|3` | 0.7 | **1.7** |
| `push|4` | 0.75 | **2.0** |

Untouched: everything else — ranges (8/10/12/16), target counts,
`repeat` 3/4, `delay: 250`, mana 20, cooldown 5000, `item_speed: 0.1`,
and push|4's `alternate_down` self-centred variant (its 0.2 upward pop
and radius-12 speed-1 shove are a different cast shape, not the cone).
Piglin brutes keep their vanilla knockback resistance (0.5 — they halve
any shove): a brute resisting more than a zombie is correct and stays;
the proof target is a zombie.

## Explicitly NOT changed

- **Magic arrow (`arrow` spell): NO change in this packet.** Its
  pass-through / splash behaviour is NEEDS-RIG (investigation item 5,
  RC-2: whether ExcelAttunement's projectile tracker zeroes spell-arrow
  damage) and is owned by the parallel rig build. (The "fire arrow" in
  Jester's vA draft is this spell mislabelled — investigation item 3:
  no `arrow_fire` key exists anywhere; nothing separate to fix.)
- **Packet #1's values** — wolf, familiar, cat, missile family, `arrow`
  mana, stormshackle, and the rest of packet #1's retune are
  byte-identical here outside the blocks above (the diff proves it).
- **`repairitem` / `repairequipment`** — untouched (no repair hunks in
  the diff).
- **Every `pvp_restricted` flag** — untouched (packet #1's standing
  exclusion carries over).
- **holylight** (item 8: config-correct self-centred support burst;
  the report is the expected experience of the config, no defect to
  fix), **wolf spell stats** (item 11: health/damage are class-fixed
  in `WolfSpell`, not parameterizable — an Excel-layer decision, not a
  spells.yml fix), **familiar cast path** (item 6 open rig question),
  **mcMMO/Jobs loops** (items 12/13 — settled by live config reads
  owned outside this packet).

## Outputs

| staged file | sha256 | bytes |
|---|---|---|
| `install-20261102-packet2/stage-s2/spells.yml` | `0ec3def9ef38331d032dd39dc393609f1dac9442860f3402459b56dc791c4653` | 86,716 (pass-through — identical to base) |
| `install-20261102-packet2/stage-s3/spells.yml` | `c49829004d786580107825b0959f1374c9938ca81157243fd560608cd0cf47e6` | 140,111 |
| `install-20261102-packet2/stage-s4/spells.yml` | `936af53b2c23ee726eceeced71e8ec4f89c38328c79592f9d826e9d08f698c77` | 178,352 |
| `install-20261102-packet2/stage-s5/spells.yml` | `bda9559f7c7ac991c8a887290f958af6f8dd00b3fd8d8df387e4c12b3cf13663` | 178,350 |

## Verification (all run at build)

- **Base gates:** all four base hashes matched the packet-#1 record
  before editing; bases re-hashed after the work — unchanged.
- **YAML:** all four outputs parse; top-level key counts 106 / 168 /
  209 / 209 (unchanged from bases — no spell added or removed).
- **Diff scope:** stage-s2: 0 lines. stage-s3/s4/s5 (identical shape):
  24 lines removed / 19 added. Reconciliation: 18 one-for-one parameter
  lines + the arrow_light chunk (11 lines → 6; diff aligns the 5
  scaffolding lines common to both, showing 6 removed / 1 added there).
  Every changed line sits inside `arrow_ice`, `arrow_light`,
  `arrow_shock`, `arrow_regular`, `lightning`, `lightning|2`, `push`,
  `push|2`, `push|3`, or `push|4` — the full s3 diff was read line by
  line at build.
- **Greps:** `bowpull` (case-insensitive) = **0** in all four outputs;
  `disable_mana_regeneration` = **0** in all four outputs.
- **Harness** (`spell_logic_tests.py`, Mike's Nov 2 gate): findings on
  the outputs are **identical to the packet-#1 after-state** on all
  four files — s2 113 (C0/H6/M33/L74), s3 212 (C0/H7/M74/L131),
  s4 258 (C0/H8/M81/L169), s5 258 (C0/H8/M81/L169). For s3 and s5 the
  full harness outputs were diffed before/after: byte-identical apart
  from the file-path header — no new findings, none resolved (this
  packet changes no mana/cooldown economics, so an identical economics
  report is the expected result).
