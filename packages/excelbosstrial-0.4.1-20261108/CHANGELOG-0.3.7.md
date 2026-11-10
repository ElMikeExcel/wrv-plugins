# ExcelBossTrial 0.3.7 — CHANGELOG

Release date: built 2026-11-03 (pending install; rides the
emergency boss-fight window as the train's final BossTrial step,
immediately before ExcelTrainingWheels 0.1.3). Base: ExcelBossTrial
0.3.6 (FROZEN candidate jar, 368,678 bytes, SHA-256
`aa9571f7c43592d8216b42775c35895dc50d733b2011aabdfe486348efaf8f6e`),
rebuilt byte-identical from its tree before any change — see
`base-verify/BASE-PROOF.md` in the build folder.

0.3.7 is **RYLIE'S SHIELD** (authority: the TW×BossTrial check,
`~/workspace/seal-audit/TW-PRESSURE-CHECK-20261102.md`). The check
found that BossTrial's pressure pulse — the periodic MAGIC hit a
seal boss deals to everyone fighting it — is source-less damage:
it has no damager entity, so TrainingWheels never sees it, and the
Training Wheels Protocol's absolute line (a protected player takes
one quarter of a normal unscaled mob's damage, everywhere, at
every seal state) silently did not cover it. A protected player
ate the full pulse — 2.0 HP every 20 seconds, 4.0 at the attrition
ramp's cap — while every direct boss hit was quartered correctly.
0.3.7 closes that gap and publishes the scale the companion fix
(ExcelTrainingWheels 0.1.3) needs to hold the same line on direct
boss hits at every ramp point. No fight math changes for anyone
else: pools, caps, ramp, verdicts, rewards, feedback — identical.

## What changes

- **The pressure pulse honors the shield.** Before a pulse lands
  on a player, BossTrial now asks TrainingWheels for that player's
  shield factor through a reflective seam (`TwShieldSeam`) and
  scales the pulse by it: a 0.25-protected player takes a quarter
  pulse (2.0 → 0.5 at rest, 4.0 → 1.0 at the ramp cap). The
  milestone surge rides the same single call site, so it is
  covered by construction. Per-chapter config key
  `<chapter>.pressure.pulse-respect-training-wheels` (default
  true) can retire the behavior per chapter without a rebuild.
  Fail-soft throughout: no TrainingWheels, a changed
  TrainingWheels, or a throwing lookup all degrade to the
  pre-0.3.7 full pulse — never an error, never a stalled fight.
- **A public outgoing-scale API.** `ExcelBossTrialAPI.
  outgoingScaleFor(LivingEntity)` returns the scale the pressure
  handler applies to that boss's hits right now (chapter outgoing
  × current ramp; 1.0 for anything that is not a seal boss in an
  applicable state; never throws). It is extracted from the same
  method the handler uses, so the API and the fight can never
  drift. ExcelTrainingWheels 0.1.3 consumes it to divide a
  protected player's takes back to the unscaled line.
- **Boot honesty for the seam.** The boot log carries one line —
  `pulse shield: ExcelTrainingWheels seam BOUND (registry|API)` or
  the UNBOUND variants — so a boot record shows whether the shield
  is live. Resolution distinguishes "not yet" from "never":
  TrainingWheels 0.1.3 softdepends on BossTrial while BossTrial
  softdepends on it, and the 0.3.7 rig caught Paper enabling
  BossTrial first under that cycle; a partner that is simply not
  enabled yet is re-checked on later calls instead of latching
  the shield off for the whole boot, and the late bind logs its
  own line when it lands.

ExcelBossTrial - a Wink Reyes Valley Excel plugin.
