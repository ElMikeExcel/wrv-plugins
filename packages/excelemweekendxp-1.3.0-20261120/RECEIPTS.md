# RECEIPTS — ExcelWeekendXP 1.3.0 (D155, Nov 20, 2026)

## Live-premise receipts (G1) — fresh pull, Fri Nov 20, 2026, 15:08 UTC (9:08 AM CST)

Route: Homebase (ssh mike-pc) -> rclone copyto from
`shockbyte:1. Michael's Server/plugins/` -> scp to
`~/workspace/weekendxp-130-build-20261120/live-pull/`.

- Live jar `plugins/ExcelWeekendXP-1.2.1.jar` = 15,148 B / SHA-256
  `b4725c555bbf218135d4c3b6eb40ddcdbecb05e46c48e2dec1e49cb1e1d791e6`
  (pulled copy: `live-pull/ExcelWeekendXP-1.2.1.jar`). Byte-identical to the
  1.2.1 build-of-record hash — the live-premise gate for this packet.
- Live jar `plugins/EliteMobs-10.9.10.jar` (file name lags; its plugin.yml
  declares version 10.9.14) = 14,563,211 B / SHA-256
  `4a6bc4a74e382942e39961e9a25a231deb46cea34379d40bc63ae9a9328c44bd`
  (pulled copy: `live-pull/EliteMobs-10.9.10.jar`). API verified on this
  exact jar via `javap`: `EliteSkillXpGainEvent` (Cancellable; getPlayer,
  getSkillType, getEliteEntity, getReason, getOriginalXp, getXp,
  setXp(long)) and `EliteSkillXpGainEvent$Reason` with the single value
  COMBAT.
- Cross-check: both pulled jars are byte-identical (same SHA-256) to the
  independent plugin-audit pull of the same morning,
  `~/workspace/plugin-audit-2026-11-20-rerun/pull/20261120-062811/plugins/`.

## Candidate receipts

- `ExcelWeekendXP-1.3.0.jar`: 18,941 B / SHA-256
  `a9f161355907033c50d20246ed47cd8360641631f7033bd21dcbb84e9de11b96`
  (build 1 and build 2, byte-identical, `cmp` clean; see `SHA256SUMS`).
- `ExcelWeekendXP-1.3.0-source.zip`: 16,945 B / SHA-256
  `6f1737d27bebfc1cd75dee673f9a50aac46436526389da7b7d8fbe08557c2c7a`.
- Tests: 38/38 pass on both builds (8 containers, 0 failed) — 25 pre-1.3.0
  tests unmodified + 13 new (EmSkillXpMultiplierTest 5, WeekendScheduleTest
  three-leg 6, ExcelWeekendXPTest wiring 2).
- Compile classpath EliteMobs jar: `libs/EliteMobs-10.9.10.jar`, a copy of
  the Step-0 live pull (14,563,211 B / `4a6bc4a7...`).

## Rig receipts

- Rig mcMMO `mcMMO-2.3.003-WRV-XP-2-ExcelShare-1.jar` = 3,721,966 B /
  SHA-256 `404025cc63f5733fa135dd9303783dc9812248ce6ad9bb0d408643c8879c8501`
  (hash-verified in `rig/plugins/`).
- Rig candidate + EliteMobs jars hash-verified in `rig/plugins/` before
  boot 1: `a9f16135...` and `4a6bc4a7...` respectively.
- Boot 1 log (EliteMobs present): `rig/logs/boot1-with-em.log`, SHA-256
  `50095af0691373f38dd48ec7eb73b282a8ff3be8c47449a3c95c090158a138f8`;
  key lines 143 (EliteMobs enabling), 162-163 (WeekendXP enabling +
  "EliteMobs detected"), 166 (Weekend 1.2x verified, all three legs),
  181 (`Done (41.809s)!`); SEVERE 0.
- Boot 2 log (EliteMobs absent): `rig/logs/boot2-no-em.log`, SHA-256
  `683b0818429c62235c019cba6b4e520e4dd3ec1a7b2f66ae37d4e130ec6481fb`;
  key lines 143 (enabling), 144 (absent-path info line), 147 (Weekend 1.2x
  verified, two legs), 162 (`Done (40.241s)!`); SEVERE 0.
- Both boots stopped cleanly via console `stop`, JVM exit code 0.
