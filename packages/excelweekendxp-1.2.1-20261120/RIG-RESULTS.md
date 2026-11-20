# ExcelWeekendXP 1.2.1 — RIG RESULTS

- Mirror rig: copy of the excelbuildmode-pilot runtime server
  (Paper 26.2-123, Java 25.0.4.1) at rig/ in the build folder;
  plugins: rig originals + live patched mcMMO
  (404025cc...) + candidate ExcelWeekendXP-1.2.1.jar.
  Boot Nov 19, 2026, 22:28 CST; stopped cleanly 22:29 CST.
- Boot lines (rig/logs/latest.log:147-149): Enabling
  ExcelWeekendXP v1.2.1; "Weekday detected: dispatched mcMMO
  XP-rate reset command; verified vanilla XP event multiplier
  1.0x."; scheduler enabled America/Chicago every 30 minutes.
  Done (36.756s)! at line 163. SEVERE 0. Zero schedule
  incomplete / greenbar / bridge warnings.
- Honest limits (from BUILD-RECORD.md): rig day was Thursday,
  so only the weekday 1.0x path was observed on the rig; the
  weekend 1.2x engagement is proven at unit level and was
  then observed live at install (Friday boot line, see
  INSTALLED.txt). No player was on the rig, so no live
  PlayerExpChangeEvent fired there; scaling is proven by the
  VanillaXpMultiplierTest unit tests (5/5).
- Tests: 25/25 pass on both builds (VanillaXpMultiplierTest 5,
  WeekendScheduleTest 11, ExcelWeekendXPTest 4,
  LegacyDataMigrationTest 5).
- Live install boot verify (failsafe chain 0029): Done
  (23.677s) 1:09:14 AM Nov 20, 2026, Paper 26.3-256;
  "Weekend detected: dispatched mcMMO 1.2x XP-rate command;
  verified vanilla XP event multiplier 1.2x."; scheduler
  enabled; SEVERE 0; no schedule incomplete / greenbar /
  bridge warnings. Server left ONLINE, 0/10 players.

## Proof still owed (post-release, NOT gates)

- Player proof Fri-Sun: vanilla XP gain at 1.2x; weekday
  reset to 1.0x; no schedule incomplete WARN across one
  boot + one 30-minute cycle. Tester: Marvin or Jester.
  Java + Bedrock both.
Status until then: Installed, unproven by player proof.
