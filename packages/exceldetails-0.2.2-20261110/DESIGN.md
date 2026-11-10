# ExcelDetails 0.2.2 — DESIGN (boss-scale match + EliteMobs prep)

Commission: Mike, Nov 9, 2026 (MEMORY.md, DETAILS SCALING +
EM-READY): "Details should scale. And it should be prepared for
elite mobs world." Two halves:

  (1) Boss segments scale by BossTrial's display-scale (x1000)
      so meter numbers match the boss pop-ups.
  (2) Prepare the meter for the EliteMobs world: EM's
      simulated-damage numbers must not render as meaningless
      millions in the meter (the EM audit's T4 concern).

Everything below is grounded in the shipped sources on disk —
ExcelBossTrial 0.5.3 (records package
`~/workspace/records-catchup-20261103/packages/excelbosstrial-0.5.3-20261109/`)
and EliteMobs 10.9.12 (`~/workspace/elitemobs-10.9.12/EliteMobs-10.9.12.jar`,
javap + `~/workspace/em-world-magic-prep/EM-MAGIC-RESEARCH.md`).

## 1. Boss scaling — the evidence chain

What BossTrial 0.5.3 actually does to a hit, in event order:

1. **Governor (HIGH).** `ExcelBossTrial.onBossDamageGovernor`:
   `BossGovernor.govern(rawFinal, …)` applies CAPS ONLY (per-event
   cap, per-tick explosion budget — `core/BossGovernor.java` is
   pure cap math, no intake inside). The governed result is
   stashed (`GovernorStash.put(event, governed.damage)`) and the
   EVENT is then scaled by the chapter's intake factor:
   `event.setDamage(event.getDamage() * (scaled / rawFinal))`
   with `scaled = governed.damage * intakeF`,
   `intakeF = IntakeFactor.factor(entityCap=1024, pool) =
   min(1, 1024/pool)` (`core/IntakeFactor.java`). So after HIGH,
   `getFinalDamage()` = governed × intakeF — ENTITY space.
2. **Record (MONITOR).** `onBossDamageRecord` reads
   `recordedDamage = GovernorStash.consume(event)` — the
   PRE-intake governed value — else `event.getFinalDamage()`.
   The ledger (`EncounterLedger.recordDealt`) therefore stores
   capped RAW damage: a wooden sword credits ~4, diamond ~7,
   netherite ~8 (the 0.3.6 feel-patch numbers). BossTrial calls
   this space "pool space".
3. **Pop-ups (display only).** Every player-facing pool figure is
   `DisplayScale.apply(displayScale, ledgerValue)`:
   - action bar `{last}` / `{total}` —
     `FeedbackDisplay` lines 207–208 (credited hit + running
     `ledger.dealtBy(player)`);
   - boss-bar title `{remaining} / {total}` — lines 308–309;
   - post-fight stats pop-up + `/bosstrial stats` table —
     `FightStatsFormat` (every damage/healing/pool figure).
   `displayScale` comes from BossTrial's own config key
   **`feedback.display-scale`** (shipped value **1000**;
   `ExcelBossTrial` reads `getDouble("feedback.display-scale",
   DisplayScale.DEFAULT_SCALE)`; `DisplayScale.sanitized`:
   NaN/inf/≤0 → 1000, clamped to [1, 100000]).

**Consequence for the meter.** ExcelDetails listens at MONITOR
and reads `event.getFinalDamage()` — for a governed boss that is
ledger × intakeF. Today the meter prints that raw (a wooden-sword
wither hit reads "0.2"). The pop-up for the same hit reads
ledger × 1000 = 4,000. The exact recovery is therefore:

    meter amount = finalDamage / intakeF × displayScale

- Sub-cap chapters (f = 1 — today's elder guardians and dragon,
  per IntakeFactor's own header) reduce to finalDamage × 1000:
  exact by construction.
- Over-cap chapters (the wither: pool 21,900 at 4 champions,
  f ≈ 0.0468) need the LIVE factor, because the pool grows with
  participants mid-fight (per-extra-damager) — no config constant
  can supply it. 0.2.2 reads it per hit through a soft reflection
  seam into BossTrial's live encounter state (below); fail-soft
  is f = 1 (the meter then reads low by f on over-cap bosses
  only — never high, never junk).

**A trap avoided.** `DisplayScale.poolHit()` / `displayedHit()`
multiply raw damage by the intake factor, and that class's
javadoc works an example where a wooden sword renders "187".
Grep over the whole 0.5.3 source tree shows those two helpers
are called by NO production surface — every shipped pop-up uses
`DisplayScale.apply(scale, ledgerValue)` on the pre-intake
ledger value. Building the meter to the javadoc example instead
of the shipped wiring would have reproduced the wrong number
(187 where the game shows 4,000). This design follows the
shipped wiring.

### 1a. What scales, what never scales

- **Damage DONE to a seal boss scales** (victim type ∈ {WITHER,
  ELDER_GUARDIAN, WARDEN, ENDER_DRAGON} — the exact type set of
  BossTrial's own `Attribution.kindOf`). This is the damage the
  pop-ups display.
- **Damage TAKEN never scales.** Boss outgoing damage is real HP
  (pressure-scaled by BossTrial before the event; the final
  damage IS the HP lost) and has no pop-up counterpart. Printing
  a 2.0 wither pulse as "2,000 taken" would manufacture the
  meaningless-numbers failure in reverse.
- **Healing never scales.** Heals land on players in real HP;
  BossTrial's fight-record healing is effective (real) healing.
- Damage to non-boss targets in the same segment stays unscaled
  (sprinkle adds, trash): only the damage BossTrial itself
  displays gets BossTrial's display treatment. In a seal fight
  the boss rows dominate the segment, which is representative —
  boss damage IS the fight.

### 1b. The seams (BossScaleLookup — soft, house pattern)

- **Display scale:** read from the LIVE BossTrial plugin's own
  config object — `Bukkit.getPluginManager().getPlugin(
  "ExcelBossTrial").getConfig().getDouble(
  "feedback.display-scale", 1000)` — public Bukkit API, no
  reflection, always the live value (a BossTrial config reload
  is picked up within the 5 s cache TTL). Sanitized with a
  mirror of `DisplayScale.sanitized`. ExcelDetails does NOT
  hardcode 1000 as its source of truth; 1000 appears only as
  the fail-soft default, which is BossTrial's own settled
  default (`DisplayScale.DEFAULT_SCALE`).
- **Intake factor:** reflection, resolved lazily, handles
  cached, outcome logged once (BOUND / UNBOUND), mirroring
  PartyLookup's discipline: BossTrial instance field `cfg`
  (`BossTrialConfig`, whose `intakeEntityCap` is a public
  field) + field `encounters` (`Map<UUID, EncounterLedger>`)
  → `EncounterLedger.maxHealth()` (public) →
  `BossScale.intakeFactor(cap, pool)`. Every failure fails
  soft to f = 1.
- **Master gate:** scaling applies only while the ExcelBossTrial
  plugin is present (a vanilla wither fought with BossTrial
  absent keeps true-HP numbers) and the Details config switch
  `boss-display-scale` is on (shipped on).

### 1c. Known edges (stated, not hidden)

- **The killing blow.** BossTrial excludes the verdict blow
  from its fight record (PM-2: the governor passes it unscaled
  and the record handler skips verdict-pending encounters).
  The meter counts it, scaled at f = the encounter's last
  factor. One hit of divergence in segment totals vs the
  post-fight pop-up — accepted; the meter is a live fight
  display, not the verdict record.
- **Chapter toggles.** Detection keys off boss TYPE + BossTrial
  present (exactly BossTrial's own kindOf test). A chapter
  disabled in BossTrial's config would still scale in the
  meter. All chapters ship enabled; the install rig checks the
  boot state.
- **Boss adds that are also EM elites** route to the EM bucket
  (§2 wins over §1): EM-world content belongs to EM's number
  space, never to the boss segment.

## 2. EliteMobs prep — the evidence

From the 10.9.12 jar (javap) + EM-MAGIC-RESEARCH §4:

- **Two combat systems coexist.** Scaled combat (natural
  elites): EM *simulates* the fight at the player's level —
  "damage numbers are simulated and grow huge — the config
  itself warns of scaling player damage into the millions at
  high levels. Real entity HP stays within vanilla attribute
  limits; the big numbers are the simulation layer."
  Normalized combat (regional/dungeon bosses): real HP pools,
  hard-capped at 2048 by the server max-health attribute EM
  raises.
- **EM health is decoupled from Bukkit.** `EliteEntity` carries
  its own `health` / `maxHealth` doubles plus an
  `unsyncedLivingEntity` beside the visible `livingEntity` —
  the Bukkit entity is partly a shell for EM's own bookkeeping.
- **EM mutates Bukkit damage events in its pipeline**
  (`EliteMobGenericDamagedHandler` rewrites the underlying
  event's BASE damage modifier from its API event;
  `EliteDamageEvent.getDamage()` carries EM-space damage).
- **A reliable detection seam exists:**
  `com.magmaguy.elitemobs.entitytracker.EntityTracker` —
  public static `isEliteMob(Entity)` /
  `getEliteMobEntity(Entity)` (javap-verified against the
  shipped jar).
- **NOT verified at desk** (stated plainly): the exact
  `getFinalDamage()` a MONITOR listener sees for a high-level
  scaled-combat hit. EM internals give no desk-provable answer,
  and the EM world does not open until Nov 15 — so 0.2.2's
  handling is built to be correct under EITHER answer.

### 2a. The decision — separately bucket, never render (yet)

Damage DONE to an EM-tracked elite goes to a separate
**EM bucket** inside the segment instead of the ranked rows:

- Bucket events keep the segment clock alive (they `touch`
  like any tracked event) and accumulate per player
  (done/taken + hits), so nothing is silently dropped.
- The bucket is NEVER ranked, NEVER rendered in the sidebar,
  and NEVER merged into the overall totals at segment close —
  overall totals stay true-HP forever. On close it is archived
  as the last segment's EM totals; on clear/reset it is
  discarded with the segment. `/details status` gains one line
  naming the bucket when it is non-empty, so the handling is
  observable in the install rig.
- **Second layer (seam-independent):** any damage event —
  done or taken, elite or not — whose amount exceeds
  **100,000** is routed to the bucket as simulated. No real
  single event on this server can reach that floor: the
  largest real HP pool anywhere is 2048 (EM's raised cap;
  the platform cap is 1024) and real overkill is bounded by
  real weapon damage. 100,000 sits ~50× above the largest
  possible real pool while sitting far below EM's simulated
  millions — if EM ever writes a simulated value into a
  Bukkit event the meter sees, the clamp catches it even
  with the seam down.
- **Damage TAKEN from an elite stays in the normal meter.**
  Elite→player damage is real HP: player pools are real
  (20–~218 with EM armor skill), players actually die from
  it, and Damage Taken is a headline metric in EM content.
  The 100k clamp still guards it. (Bucketing taken would
  empty a headline section in exactly the world this build
  prepares for, to guard a failure mode that cannot occur:
  a player cannot lose simulated HP.)
- **Healing is untouched** (real HP on real players).
- Elite detection keys off the VICTIM for damage-done (the
  target being simulated is what makes the number simulated).
  World-name checks (`em_` prefix) are deliberately NOT a
  bucketing signal: EM worlds contain non-elite trash whose
  damage is real, and elites (sprinkle adds) appear in normal
  worlds where bucketing is equally correct.
- Uniform over scaled/normalized: normalized-boss damage is
  real HP and COULD be metered truthfully, but distinguishing
  the two per elite at desk means guessing at EM internals
  (`EliteEntity.isNaturalEntity` is the lead for a later
  refinement). Uniform bucketing is predictable, satisfies
  Mike's rule with certainty, and loses nothing yet — the EM
  world has not opened; EM already shows its own popups and
  health displays there. Rendering the bucket as its own
  section is explicitly a POST-ENTRY decision (Nov 15+), not
  0.2.2 scope; the data is preserved per segment meanwhile.
- Config switch `elite-bucket` (shipped on); seam fail-soft =
  not-elite (the clamp still guards).

## 3. Not changed

Checkbox mix, party scoping (0.2.1), segment lifecycle, churn
fix, persistence format (data.yml untouched — the EM bucket is
session-scoped like the by-target maps; a jar swap either
direction needs no data migration), placeholders, commands
(one additive status line aside), piggyback rendering. The
bundled config.yml gains exactly two keys (`boss-display-scale`,
`elite-bucket`), both with code defaults equal to the shipped
values, so an unmerged live config behaves identically.
