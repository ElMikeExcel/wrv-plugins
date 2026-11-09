# BUILD RECORD — ExcelBossTrial 0.5.3 (Item 39 Warden retune filing: R2 + R1)

Built Nov 9, 2026 (desk lane). The approved Warden retune
candidate from the Item 39 test rounds
(`~/workspace/warden-retune-20261109/TEST-ROUNDS.md`, rounds 1–2;
Mike approved filing R2+R1 as-is with the step-4 flags noted,
Nov 9, 2026) composed onto the shipped ExcelBossTrial 0.5.2
line. 0.5.2 did NOT contain R1 or R2; this build is exactly
R2 + R1 on top of it and nothing else.

## Candidate jar (FINAL)

- `~/workspace/warden-retune-20261109/build-053/ExcelBossTrial-0.5.3.jar`
  — **440,242 B / sha256
  09c5eb0e4e0860855c6a28e07d703a6627a778bbca5c6a9dacf431c7a0cf35e8**
- Build ID: **EXC-BOSSTRIAL-0.5.3-WRV-20261109-01-35bd2b**
  (fresh tag; neither 0.5.2's 13447a nor 0.5.1's a3aa16 reused).

## Base proof

- Tree copied tree-identical from the shipped 0.5.2 build tree
  (`~/workspace/bosstrial-052-build/bosstrial/`).
- Pristine `package.sh` rebuild of the untouched copy is
  byte-identical to the shipped 0.5.2 jar: 438,791 B / sha256
  1a866a54840dc510ad78cdbd384b75d49f82a25808a74724a0b4fc1e66f0944b
  (`work/base-pristine.jar`, log `work/base-build.log`), suite
  at 0 failures before any 0.5.3 edit.

## The change (complete list — tree diff vs the 0.5.2 tree)

**R1 — the sonic-boom pressure exemption** (ported from the
retune test build `build-r1/`, TEST-ROUNDS round 2; same shape,
same insertion points):

- NEW `core/SonicBoomRule.java` — pure predicate: exempt ⟺
  cause SONIC_BOOM **and** kind WARDEN **and** the chapter key
  off. Chapter-local by construction.
- `core/BossTrialConfig.java` — `Pressure` record gains a 9th
  field `applyToSonicBoom`; `pressureFrom` reads
  `<chapter>.pressure.apply-to-sonic-boom`.
- `ExcelBossTrial.java` — `onBossPressureDamage` returns early
  (event damage untouched) when `SonicBoomRule.isExempt(...)`,
  after the elder reflect branch and before the scale is
  applied; `loadSettings` flat-key list gains
  `<chapter>.pressure.apply-to-sonic-boom` for all four chapters.

**R2 — the pressure values**, shipped as the bundled config's
warden defaults: `warden.pressure.outgoing-multiplier`
1.25 → **1.0**, `warden.pressure.ramp-max` 2.0 → **1.5**
(`ramp-per-minute` 0.06 unchanged; pool/caps/pulse untouched;
no other chapter's values touched).

**The shipped default of the new key is FALSE** — in the
bundled config (all four chapter blocks carry
`apply-to-sonic-boom: false`; inert outside the warden, the
only sonic-boom source) **and** in the code fallback
(`Pressure.settled(...)` and `Pressure.OFF` carry false), so a
config predating the key runs the retune. Setting the warden
key `true` restores the pre-0.5.3 pressure-scaled boom.
**Named deviation from the R1 test build:** the test build's
settled default was TRUE (so its rig configs changed nothing
by accident); the shipped filing deliberately reverses it,
because 0.5.3 IS the retune. `Pressure.settled(...)`'s
multiplier/ramp strengths are unchanged (the R2 values ship
through the config layer, and the install merge sets them
explicitly in the live file).

**Version surfaces:** `plugin.yml` version 0.5.3 + the 0.5.3
description sentence; `core/BuildInfo.java` fragments rotated
(PART_B `0.5.3-WRV-`, PART_C `20261109-01-35bd2b`, VERSION
0.5.3); `excel-build.txt` carries the new build ID.

**Tests:** NEW `SonicBoomRuleTest` ported from the test build
(24 checks — truth table unchanged; the default expectations
flipped to the shipped FALSE default; the candidate-neighbor
checks kept). `PluginYmlParseTest` re-pinned: version 0.5.3,
+1 description pin, +4 chapter pins for the new key, +2 pins
for the shipped R2 warden defaults (37 → 44 checks).

## Tests

- Full suite via `package.sh` (both builds): **0 failures**.
  Tally by the 0.5.2 lane's own method (`tally.py` over the
  package log): **2,435 checks / 85 harnesses**, vs the 0.5.2
  log's 2,404 / 84 with the same tool — delta +31 checks /
  +1 harness = SonicBoomRuleTest 24 + PluginYmlParseTest +7,
  exactly the new pins. (The 0.5.2 BUILD-RECORD's 27,557
  headline counts assertions inside harnesses whose log lines
  only summarize; the relative tally here is apples-to-apples.)
- SonicBoomRuleTest: **24 passed, 0 failed** (both builds).

## Deterministic double-build

`package.sh` twice → byte-identical: `work/pkg1.jar` =
`work/pkg2.jar` = the candidate above (440,242 B / 09c5eb0e…).
Jar contents verified: 230 entries (0.5.2: 229; +1 =
`core/SonicBoomRule.class`); all 0.5.2 feature classes present
(BossVoice, VoiceBook, VoiceSettings, FeedbackDisplay,
SprinkleCadence, FlipEffects, BuildInfo); `excel-build.txt`
at the jar root carries the 0.5.3 build ID; `BuildInfo.class`
carries the 35bd2b fragment and NO 13447a; bundled `plugin.yml`
reads version 0.5.3; bundled `config.yml` carries the warden
candidate block (outgoing 1.0 / ramp-max 1.5 /
apply-to-sonic-boom false).

## Fingerprint verification (release gate)

Layer B homes all carry the new ID, verified in the built
jar: (1) `excel-build.txt` at the jar root; (2) the
fragment-assembled constant in `core/BuildInfo.class`
(35bd2b present, 13447a absent); (3) the boot surface line
`Build: EXC-BOSSTRIAL-0.5.3-WRV-20261109-01-35bd2b`, observed
in the rig boot below. Layer C semantic markers carried
unchanged; 0.5.3 adds one config key
(`<chapter>.pressure.apply-to-sonic-boom`) under the standing
naming texture.

## Rig re-verification (Step 3 boom/melee vs the NEW build)

The retune rounds' own harness, re-run against the SHIPPED
0.5.3 jar: a copy of the Item 39 rig (`build-053/rig-053/`,
Paper 26.3-235) restored to the round-2 part-1 plugin set
(BossTrial **0.5.3** swapped in; GuildGate 0.4.0; Protect
0.1.8; RigDriver0211; WardenProbe-R2 `7556a1ea…`; Magic and
WardenRigExt removed — neither existed in the part-1 set),
BossTrial state reset to config-only, rig config = the
round-2 candidate config (`9223c0dd…`: R2 values + warden
`apply-to-sonic-boom: false`), script =
`script-round2-backup.txt` verbatim. Log
`rig-053/round-053.log` (69,273 B / sha256 08ef0e1b696d9051
49a81670642ac4bfc7bb965ce906a96bc69f9452901a0454); runner
exited / exit 0 / **SEVERE 0**. Gear basis: BRACKET FALLBACK,
as every round (tank Prot IV netherite, others Prot IV
diamond; beacons none).

Boot: `Enabling ExcelBossTrial v0.5.3`; `Build:
EXC-BOSSTRIAL-0.5.3-WRV-20261109-01-35bd2b`; damage-feedback
line carries `display scale x1000.0
(feedback.display-scale)`; boss voice line names all four
chapters ARMED (10/7/7/8 clips) — the 0.5.2 surfaces execute
unchanged on the new jar.

Step 3 readings (constructed-event instrument through the
real 0.5.3 handler, cross-validated in the rounds against
natural booms; post-mitigation via round 1's validated
textbook factors — the boom is armor-ignoring, read
directly):

| Point | Boom | Melee (event base) | Melee post-mit, Prot IV netherite | Round-2 reference |
|---|---|---|---|---|
| Minute 0 (~11–14 s) | **15.0000** / **15.0000** (both victims) | 45.5284 / 45.6184 (scale ×1.012–1.014) | ≈ **9.2** | boom 15.0000 |
| Mid-ramp (~4.1 min) | **15.0000** | **56.1484** (×1.24774) | ≈ **13.1** | boom 15.0000; melee 56.1482 |
| Ramp cap (~9.2 min, past the 8.33 min cap) | **15.0000** / **15.0000** | **67.5000** ×3 (both victims) (×1.5000) | **17.98** | boom 15.0000; melee 67.5000 |

Every SONIC_BOOM event in the log (5 of 5) reads base =
final = 15.0000 — no boom was scaled at any fight point.
**Step 3 verdict on the shipped 0.5.3 jar: PASS** — the
round-2 result reproduces exactly (mid melee within 0.0002
of the test build's reading, a sub-second clock offset; cap
values identical to the fourth decimal). No hit ≥ 20 HP at
any point on the tank bracket. The encounter closed by the
scripted stop (fight record: pool 16,800 at the script's 3
damagers, duration 531 s, verdict SERVER_STOP — the part-1
shape, as in round 2).

Steps 2 and 4 were NOT re-run against 0.5.3: their round-2
verdicts (step 2 PASS, 695 s; step 4 MIXED, flagged) were
measured against the R1 test build whose handler output this
run proves identical to 0.5.3's at every Step 3 point, and
the candidate's config values are the same by construction.
