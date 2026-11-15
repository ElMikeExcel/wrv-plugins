# RECEIPTS — R8 arrow family balance fix (board run 360, worker R8)

## G1 live-premise receipts (Homebase route, probes PASS)

Homebase probe pattern (3 attempts over ~2 min, ssh mike-pc):
2026-11-14 21:23:17 / 21:24:03 / 21:24:50 CST — 3/3 PASS
(HOMEBASE_OK, host DESKTOP-IL05DQR). No fallback used.

Pull method: rclone on Homebase, remote `shockbyte:` root
"1. Michael's Server/", copied to PC staging then scp to this VM.
Pull window: 2026-11-14 21:25–21:27 CST. Local copies in ../g1-pull/.

| Destination file | Size B | SHA-256 |
|---|---|---|
| plugins/Magic/spells.yml (live) | 188,274 | 75877195b4991f48dd976698fbb53dca3800633dd8230d65a13022bfa2258abf |
| ExcelMagicGate stage2 spells.yml | 89,208 | 9be7eda6444471b526c0b20b28ee38a1efa0f25a0b474ff003cd21e9d78f97a5 |
| ExcelMagicGate stage3 spells.yml | 145,673 | 682ab22f57b99e78b6c668e92d1fa8f43893569bf92bae34d8602fde2fb08be7 |
| ExcelMagicGate stage4 spells.yml | 188,274 | 75877195b4991f48dd976698fbb53dca3800633dd8230d65a13022bfa2258abf |
| ExcelMagicGate stage5 spells.yml | 188,274 | 66ebb58cabb58b30a5cc611cf2b376b8c449c4511179c60805e769d33fbe44a7 |

Live == stage4 byte-identical (correct: guild is at stage 4, 4 of 5 seals).

## Live arrow-family census (from the pulled bytes, script-extracted)

Live / stage3 / stage4 / stage5 (identical values):
arrow ad 4 @ speed 3, payload 10, cd 1500, mana 40;
arrow|2 ad 6 @ 3.5, payload 10, mana 40;
arrow|3 ad 6 @ 4, payload 12, cd 1400;
arrow|4 ad 6 @ 4.5, payload 14, cd 1350;
arrow|5 ad 7 @ 5, payload 15, cd 1250;
arrow|6 ad 7 @ 5, payload 16, cd 1150, mana 40;
arrow_ice ad 8 @ 3, cd 1000; arrow_shock ad 8 @ 3, cd 1000;
arrow_regular ad 9 @ 3, cd 1000; arrowrain ad 5 @ 1, mana 30, cd 10000.

Stage2 differs (its own retune values): arrow ad 5 @ 3, payload 13,
cd 2000, mana 25; |2 ad 6 @ 3.5 payload 13; |3 ad 6 @ 4 payload 14;
|4 ad 7 @ 4.5 payload 14; |5 ad 7 @ 5 payload 15; |6 ad 7 @ 5 payload 16.
Stage2 file has no ice/shock/regular/arrowrain blocks.

## Candidate (staged) hashes

See build/SHA256SUMS. Sizes: live 188,297 B; stage2 89,223 B;
stage3 145,696 B; stage4 188,297 B; stage5 188,297 B.
Staged live == staged stage4 byte-identical (correct at stage 4).

## Evidence files (build/evidence/)

- harness-base-live.txt / harness-staged-live.txt — spell logic-test
  harness on base vs staged live file: findings identical apart from
  the path header line (278 findings; CRITICAL 0, HIGH 6, MEDIUM 83,
  LOW 189 in both; 280 finding rows each).
- harness-staged-s235.txt — harness on staged stage2/3/5 (632 rows).
- model-test.txt — scripted post-fix damage model: PASS (see RESULT.md).
- rig-boot.log — local Paper rig boot with staged live spells.yml:
  "Loaded 591 spells" (rig book), Done (39.282s), SEVERE 0, zero
  warnings naming arrow or spells.
- diffs in build/diffs/: live 9 lines, stage2 6, stage3 9, stage4 9,
  stage5 9 — every changed line is an `arrow_damage:` line inside an
  in-scope arrow block; nothing else differs.
