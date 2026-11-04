# ExcelTrainingWheels — Changelog

## 0.1.3 (2026-11-02) — BossTrial compensation seam + skull-explosion arm

Restores the Training Wheels Protocol's absolute line against seal bosses (spec of record: `~/workspace/seal-audit/TW-PRESSURE-CHECK-20261102.md`, Nov 2, 2026): a protected player takes their factor × a mob's NORMAL, UNSCALED damage in every world at every seal state. Two gaps broke that line against BossTrial bosses; both are closed here. Pairs with ExcelBossTrial 0.3.7 (pressure-pulse coverage + the `outgoingScaleFor` API); degrades gracefully without it.

### Added

- **BossTrial compensation seam** (third compensation seam, beside the EliteMobs ladder division and the raid-buff division, mirroring `RaidDivision`'s shape). New pure class `core/BossDivision`. When an admitted damage event's damager IS a seal boss (the boss LivingEntity itself — melee, sweep, the elder guardian's laser, the warden's sonic boom), the glue reads the boss's current outgoing scale through a reflective call to `ExcelBossTrialAPI.outgoingScaleFor(LivingEntity)` and divides the effective shield factor by it:
  - `take = vanilla × scale × (factor / scale) = factor × vanilla` at every ramp point.
  - Worked values at factor 0.25: ÷ 2.5 at Wither/Warden ramp cap → effective factor 0.1 (was a 0.625× vanilla take under 0.1.2); ÷ 1.25 at fight start → 0.2; ÷ 1.0 with no open encounter → 0.25, bit-exact.
  - The scale is sanitized (`BossDivision.sanitize`): anything below 1.0, NaN, or infinite collapses to 1.0, so a bad read can never strengthen or weaken the shield beyond the flat factor.
  - **Fail-soft:** BossTrial absent, older than 0.3.7, API class relocated beyond the two known names, or the call throwing — all read as 1.0 and the shield behaves exactly as 0.1.2. TrainingWheels keeps working standalone (modularity law). The enable log states which state the seam bound in (`BOUND` / `UNBOUND`).
  - Resolution order: `com.winkreyes.excelbosstrial.api.ExcelBossTrialAPI`, then `com.winkreyes.excelbosstrial.api.BossTrialAPI`.
- **Skull-explosion arm** in `ShieldRule`: `ENTITY_EXPLOSION` is now admitted when the damager is a projectile whose shooter is a hostile mob (mirrors the existing PROJECTILE arm) — Wither skull blasts are shielded at the flat factor. The creeper arm is unchanged, and skull events are **never divided** by the pressure scale: no BossTrial scaler touches them on any path (the pressure handler requires a LivingEntity damager), so dividing would push the take below the guaranteed factor.
- **Boss-attributed admissions** (evaluated per the task's scope item 3, included where attribution is clean):
  - `SONIC_BOOM` admitted when the damager is the boss itself → divided like any direct boss hit (the pressure handler scales it; the API's number is the scaler that touched it).
  - `DRAGON_BREATH` admitted when the AreaEffectCloud's source is the Ender Dragon → flat factor, **not divided** (see the axis note below).
  - EnderDragonPart contact: the glue resolves a part damager to its parent dragon and admits it → flat factor, **not divided**.
- Config key **`compensate.bosstrial`** (default `true`), alongside the existing seam toggles' pattern. `false` restores the pre-0.1.3 proportional behavior against seal bosses. The key also defaults to `true` in code, so an unedited live config gets the seam.
- Debug SHIELD line now carries the boss term (`boss x<scale> <shape>`) when a boss-attributed event is scaled.
- `plugin.yml`: version 0.1.3; `softdepend` gains `ExcelBossTrial`.

### The dragon phase-2 axis — named residual, by contract

The 0.3.7 API contract reports the PRESSURE scale only (chapter outgoing × ramp). The dragon chapter's native phase-2 ×1.5 buff is a separate axis the API explicitly does not report ("a dragon reads its ramp only"). Dividing the dragon's part / fireball / breath shapes — or the phase-2 contact buff stacked on its direct hits — by the pressure number would be wrong in both directions: below the guaranteed factor in phase 1, coincidentally right only where ramp happened to equal 1.5. Those shapes are therefore admitted at the flat factor and never divided:

- Exact factor × vanilla everywhere except dragon phase 2.
- During dragon phase 2 only: 0.375× vanilla (0.25 × 1.5) on the phase-2-axis shapes and on the dragon's direct hits (whose pressure division remains exact).
- Closing this residual needs a future phase-aware BossTrial getter, not division by the wrong axis. It is recorded here and in TEST-REPORT.md, not hidden.

### Not covered (unchanged, stated plainly)

- **Wither effect ticks** (wither status damage): plain `EntityDamageEvent` with no damager entity — unattributable by construction. Remains uncovered; not forced.
- **Pressure pulse:** source-less MAGIC damage. Coverage for the pulse is delivered on the BossTrial side by 0.3.7 (the pulse honors the protected player's factor via its own seam), not by TrainingWheels — TW never sees the event.

### Tests

- 280/280 unit checks pass (225 carried from 0.1.2 green + 55 new/changed pinning the seam math at scales 1.0 / 1.25 / 1.5 / 2.0 / 2.5, the sanitizer, the shape-division table, the skull arm, and the boss-attribution admissions; EliteMobs and RaidBuff seam suites regression-green).
- Build is deterministic: two consecutive builds byte-identical (SHA-256 `81e76017ef14040845c050b908192bc319d08b642cc8ee37041620d5fc7ee189`).
- Rig pass on Paper 26.3-235 + Geyser against a contract stub of the 0.3.7 API (the real 0.3.7 jar was still scaffold-stage at build time): see TEST-REPORT.md for per-case verdicts, including the real elder-guardian laser composition proof.

## 0.1.2 and earlier

See the 0.1.2 tree's records (`~/workspace/exceltrainingwheels/src-0.1.2/`) — EliteMobs ladder compensation (0.1.1), raid-buff division, copper door tier, iron-door siege, door hold.
