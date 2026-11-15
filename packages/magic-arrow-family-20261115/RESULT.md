# RESULT — R8 Arrow family balance fix (board run 360, wave 2, worker R8)

## Premise verdict: HOLDS — built

Verify-first checks, all against current on-disk / live state:
- **Owner plugin:** the arrow family lives in the Magic plugin's
  spells.yml (elBukkit Magic 11.2.5; `class: Projectile` + real
  TippedArrow). No custom WRV plugin owns arrows. The same spell
  blocks are delivered to 5 destinations: live plugins/Magic/spells.yml
  + the ExcelMagicGate stage 2/3/4/5 template spells.yml files.
- **Current values (G1 live pull, not lineage):** Homebase probes
  3/3 PASS (2026-11-14 21:23:17 / 21:24:03 / 21:24:50 CST, host
  DESKTOP-IL05DQR); rclone pull 21:25–21:27 CST. Live values are
  EXACTLY the Nov 9 investigation's: arrow_damage 4/6/6/6/7/7 at
  speeds 3/3.5/4/4.5/5/5 with payloads 10/10/12/14/15/16; ice 8,
  shock 8, regular 9 (all @ speed 3); arrowrain 5 @ speed 1.
  Live file == stage4 template byte-identical (stage 4 of 5 seals).
- **No fix already landed:** no arrow-balance build folder or
  MANIFEST entry exists; live bytes still carry the inflated values.
  (The Nov 1 "Magic Arrow fix" was a different defect — spawn
  origin/spread — and the Nov 2 packet-#2 bowpull constants for
  ice/shock/regular are NOT in live bytes; the current lineage
  carries 8/8/9.) Jester's Nov 10 test (42 direct / 16 indirect)
  confirms the overshoot is live in play, ~58/cast at arrow|6 vs
  the config's balanced 23.
- Diagnosis of record: ~/workspace/spell-audit/
  ARROW-DOUBLING-INVESTIGATION-20261109.md (arrow_damage is vanilla
  BASE damage; vanilla multiplies by impact velocity).

## The build (config-only — no jar)

`arrow_damage_new = arrow_damage_old / speed` (2 dp), applied to
the real-arrow blocks only, per file from its own values:
- Ladder live/s3/s4/s5: 4→1.33, 6→1.71, 6→1.5, 6→1.33, 7→1.4, 7→1.4
- Ladder stage2 (own values): 5→1.67, 6→1.71, 6→1.5, 7→1.56, 7→1.4, 7→1.4
- arrow_ice 8→2.67, arrow_shock 8→2.67, arrow_regular 9→3.0
- arrowrain untouched (speed 1: value already true; identity asserted)
- Speeds, payloads, mana, cooldowns, spread, all other spells:
  byte-identical (diff-proved: live 9, stage2 6, stage3 9, stage4 9,
  stage5 9 changed lines — 42 total, every one an arrow_damage line).

## Candidate artifacts (build/staged/, SHA256SUMS in build/)

| File | Size B | SHA-256 |
|---|---|---|
| live-spells.yml | 188,297 | 3572fc5ef5978314bc39a5261318be2c4f5eb0803eeec26f183f5ee3d5510f36 |
| stage2-spells.yml | 89,223 | 9498948ba474c1868c4ef54a4339604f19b703bc9e6ee1c9f509656de99e4389 |
| stage3-spells.yml | 145,696 | 123334d4ce535d9b1ec59a629ee1f6ad93da5c2cc58ee49479b2cf4294ec1406 |
| stage4-spells.yml | 188,297 | 3572fc5ef5978314bc39a5261318be2c4f5eb0803eeec26f183f5ee3d5510f36 |
| stage5-spells.yml | 188,297 | 96c61a50a75bc41d1906680bedff2b80a09ffece85be7ab97548bf36ac5a8152 |

(No jar in this packet — config-only spells delta; the staged
spells.yml files are the candidates. Live == stage4 staged, correct
at stage 4.)

## Tests

- **YAML validity:** all 5 staged files parse; key counts identical
  to bases (222 / 109 / 176 / 222 / 222).
- **Spell logic-test harness** (Mike's Nov 2 gate,
  spell-audit/logic-tests/spell_logic_tests.py): staged live findings
  IDENTICAL to base live — 278 findings (CRITICAL 0, HIGH 6,
  MEDIUM 83, LOW 189), 280 finding rows each, diff = path header only.
  Staged stage2/3/5 also run (632 rows, evidence file).
- **Mana-tier sustainability:** mana + cooldowns untouched, so the
  sustainability profile at every pool tier (60/100/150/200 →
  regen 5/15/25/35 per s) is the config's existing profile by
  construction — harness check-1 output for the arrow family is
  unchanged (base arrow drain 26.7/s, sustainable only at pool 200,
  pre-existing LOW; item-cost family zero-mana shape is the known
  stock shape, paced by 1-arrow item cost + 1000 ms cooldown).
- **Damage model test** (build/model_test.py, scripted): PASS —
  39 spell-file rows; post-fix physical = ceil(speed × new) matches
  the intended arrow part within 1 HP everywhere (ice/shock land 9
  vs intent 8 via ceil on 8.01 — noted); arrow|6 = 23/cast,
  20.0 DPS = Power V bow parity (Nov 8 audit top-tier target),
  vs ~58/cast / 44–60 DPS pre-fix.
- **Local rig boot** (Paper + Magic 11.2.5, staged live file):
  "Loaded 591 spells" (rig book count), Done (39.282s),
  **SEVERE 0**, zero warnings naming arrow or spells.
  Log: build/evidence/rig-boot.log.

## Packet + lint

- Packet draft: build/INSTALL-PACKET.md — **Lint: 5625 B /
  max line 84 / non-ASCII 22** (targets ≤ ~12 KB, < 300, minimal —
  PASS). NOT filed into MANIFEST.md (per brief — supervisor files
  on verification; install stays parent-gated).
- Receipts: build/RECEIPTS.md (G1 pull receipts with sizes, hashes,
  timestamps; candidate hashes; evidence index).
- Build folder: ~/workspace/board-r360/r8-arrow-balance/build/
  (build.py, model_test.py, staged/, diffs/, evidence/, SHA256SUMS,
  INSTALL-PACKET.md, RECEIPTS.md). G1 pulls in ../g1-pull/.

## G1 status: OBTAINED (not owed)

Homebase route passed 3/3 probes; all live premises in the packet
cite the same-day pull receipts in RECEIPTS.md. No fallback used.
PC-side staging dir (muse-tools/r8-pull) deleted after scp.

## Notes for the supervisor / parent

- Stage-flip caution is written into the packet gate: if tonight's
  post-Warden stage-5 flip lands before this install, live gates
  against the stage5 receipt (66ebb58c…), not stage4's.
- Observation, NOT changed (scope discipline): the audit's ladder
  targets assumed CD 1500 ms at top tier; the shipped cooldowns run
  1150–1500 ms. This fix restores config intent only; any cooldown
  retune is a separate decision.
- Proof owed after install: Jester recast set (named in packet).
