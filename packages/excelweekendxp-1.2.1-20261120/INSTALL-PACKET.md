# INSTALL-PACKET - ExcelWeekendXP 1.2.1 (F-1 fix, decision D146)

Lint: 3765 B / max line 125 / non-ASCII 0 (filled after lint count; targets <= ~12 KB, lines < ~300 chars, minimal non-ASCII)

## PENDING - ExcelWeekendXP 1.2.1 (Nov 19, 2026)

- **Packet:** `~/workspace/weekendxp-121-build-20261119/INSTALL-PACKET.md`
- **Live-premise receipts (G1):** live jar `plugins/ExcelWeekendXP-1.2.0.jar` =
  19,252 B / SHA-256 `b54b3c465b5609f2b7a5df78e0498e1141d4aab6e0aed0bfd9371b0a6d609db8`
  per the all-plugin audit's same-day (Nov 19, 2026, 21:39 CT) live pull
  (`~/workspace/plugin-audit-20261119/CUSTOM-AUDIT.md:429-430` and `:555`).
  Full receipts: `~/workspace/weekendxp-121-build-20261119/RECEIPTS.md`.
  NOTE: W7 did not run its own fresh rclone pull; the failsafe re-verifies
  the live-jar gate at install (STOP on mismatch).
- **Candidate:** `~/workspace/weekendxp-121-build-20261119/ExcelWeekendXP-1.2.1.jar`
  - 15,148 B / SHA-256 `b4725c555bbf218135d4c3b6eb40ddcdbecb05e46c48e2dec1e49cb1e1d791e6`;
  deterministic double-build byte-identical; tests 25/25; rig boots clean
  (weekday 1.0x observed, SEVERE 0). Build record:
  `~/workspace/weekendxp-121-build-20261119/BUILD-RECORD.md`.
- **Live-jar gate (STOP on mismatch):** live file `plugins/ExcelWeekendXP-1.2.0.jar`
  = 19,252 B / `b54b3c46...` - sourced from the live-premise receipt above.
- **Staged artifacts:** `~/workspace/weekendxp-121-build-20261119/` (SHA256SUMS in
  the build folder) - candidate jar + source zip + build record + this packet.
- **Scope:** WeekendXP owns the vanilla (green-bar) XP multiplier itself via a
  `PlayerExpChangeEvent` listener (1.2x Fri-Sun, 1.0x weekdays, >=1.0 guard kept);
  the reflective mcMMO bridge (`VanillaXpBridge`) is deleted. The `mcmmo:xprate`
  leg is untouched. mcMMO is NOT rebuilt. No config, data, or earned files change.
  Rollback: restore the 1.2.0 jar (19,252 B / `b54b3c46...`) and restart.
- **Gates:** marker discipline with re-checks before Shut Down and before Start;
  BACKUP FIRST + cap-at-6 (name prunees; never the Daily newest or the two newest
  manuals); emptiness `list` = 0/10 twice.
- **Boot verify:** `[ExcelWeekendXP] Enabling ExcelWeekendXP v1.2.1`; on a weekday
  `[ExcelWeekendXP] Weekday detected: dispatched mcMMO XP-rate reset command;
  verified vanilla XP event multiplier 1.0x.` (on Fri-Sun the 1.2x line instead);
  `[ExcelWeekendXP] ExcelWeekendXP scheduler enabled: America/Chicago, every 30
  minutes.`; `Done` line; SEVERE 0 attributable to ExcelWeekendXP; no
  `schedule incomplete` / `greenbar` / `bridge` warnings. Marker:
  `~/workspace/weekendxp-121-build-20261119/INSTALLED.txt` (written at install).
- **Same-pass surfaces:** no player-facing text changes in 1.2.1 (no Guide, website,
  What's New, tips, or menu updates owed); the fix restores the already-documented
  weekend 1.2x vanilla bonus. Record the fix in the 1.2.1 record PR + website
  changelog at close-out (supervisor lane).
- **Proof owed after install:** weekend 1.2x engagement on live (Fri-Sun): a player
  gains vanilla XP and the green-bar gain is 1.2x; weekday reset to 1.0x; no
  `schedule incomplete` WARN across one boot + one 30-minute cycle. Tester: Marvin
  or Jester (Mike does not test); Java + Bedrock both. A proof is only discharged
  by the tester's own report.

## Verification by script output

- `sha256sum ~/workspace/weekendxp-121-build-20261119/ExcelWeekendXP-1.2.1.jar`
  -> `b4725c55...` (must match the Candidate line above).
- `grep -c SEVERE <rig-or-live latest.log>` -> 0 attributable to ExcelWeekendXP.
- `grep -ci "schedule incomplete\|greenbar\|bridge" <latest.log>` -> 0 lines naming
  ExcelWeekendXP.
- `unzip -l <candidate jar>` -> lists `VanillaXpMultiplier.class`, no
  `VanillaXpBridge.class`.
