# RECEIPTS — ExcelWeekendXP 1.2.1 (W7, run 619, Nov 19, 2026)

## Live-premise receipts (G1)

- Live jar `plugins/ExcelWeekendXP-1.2.0.jar` = 19,252 B / SHA-256
  `b54b3c465b5609f2b7a5df78e0498e1141d4aab6e0aed0bfd9371b0a6d609db8` — same-day
  (Nov 19, 2026) live pull by the all-plugin audit, 21:39 CT; audit record
  `~/workspace/plugin-audit-20261119/CUSTOM-AUDIT.md:429-430` and `:555`
  ("byte-identical to `~/workspace/weekendxp-1.2.0/ExcelWeekendXP-1.2.0.jar`").
  W7 re-verified the same bytes this run: the base-gate rebuild repackaged with
  the jar-embedded plugin.yml is byte-identical (`cmp` clean) to the build-of-record
  copy `~/workspace/weekendxp-1.2.0/build2/ExcelWeekendXP-1.2.0.jar`.
  NOTE: W7 did not run its own fresh rclone pull; the gate hash above is the
  audit's same-day pull, and the failsafe re-verifies the live-jar gate at install
  (STOP on mismatch) per the packet.
- Live mcMMO `mcMMO-2.3.003-WRV-XP-2-ExcelShare-1.jar` = 3,721,966 B / SHA-256
  `404025cc63f5733fa135dd9303783dc9812248ce6ad9bb0d408643c8879c8501` — audit §5
  (same Nov 19 pull); W7 verified the same hash on
  `~/workspace/mcmmo-xpfix-build-20261115/mcMMO.jar` this run and used that copy
  in the rig. The F-1 defect (bridge API absent) was verified by W5 against
  `~/workspace/mcmmo-xpfix-build-20261115/repo/` (zero hits for all three method
  names); W7 verified the no-double-count side: zero files in that source tree
  reference `PlayerExpChangeEvent`, and skill XP flows through
  `McMMOPlayer.addXp` (line 1310).

## Candidate receipts

- `ExcelWeekendXP-1.2.1.jar`: 15,148 B / SHA-256
  `b4725c555bbf218135d4c3b6eb40ddcdbecb05e46c48e2dec1e49cb1e1d791e6`
  (build 1 and build 2, byte-identical; see `SHA256SUMS`).
- `ExcelWeekendXP-1.2.1-source.zip`: 33,236 B / SHA-256
  `38cc2f3bd47a61120141f8b5cf4d9a09503ef0e2b2fa736b5f352cf56d406955`.
- Tests: 25/25 pass on both builds (7 containers, 0 failed).
- Rig boot log: `rig/logs/latest.log`, SHA-256
  `223e939757955e26efc9f27ba3412e538d27fc8b14a2c9bb258ca86d8c3cfc25`;
  key lines 137 (mcMMO enabling), 147-149 (WeekendXP enabling + weekday 1.0x
  verified line), 163 (`Done (36.756s)!`); SEVERE 0.
